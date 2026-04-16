package io.quintkard.quintkardapp.card;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import io.quintkard.quintkardapp.embedding.EmbeddingProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CardSearchRepositoryImpl implements CardSearchRepository {

    private static final int MIN_TEXT_PROBE_LIMIT = 100;
    private static final int MIN_SEMANTIC_PROBE_LIMIT = 200;
    private static final int RRF_K = 60;

    private static final RowMapper<CardSummaryProjection> CARD_SUMMARY_ROW_MAPPER = new RowMapper<>() {
        @Override
        public CardSummaryProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CardSummaryItem(
                    rs.getObject("id", UUID.class),
                    rs.getString("user_id"),
                    rs.getString("title"),
                    rs.getString("summary"),
                    CardType.valueOf(rs.getString("card_type")),
                    CardStatus.valueOf(rs.getString("status")),
                    CardPriority.valueOf(rs.getString("priority")),
                    rs.getObject("due_date", LocalDate.class),
                    rs.getObject("source_message_id", UUID.class),
                    toInstant(rs, "created_at"),
                    toInstant(rs, "updated_at")
            );
        }
    };

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingProperties embeddingProperties;

    public CardSearchRepositoryImpl(JdbcTemplate jdbcTemplate, EmbeddingProperties embeddingProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingProperties = embeddingProperties;
    }

    @Override
    public Slice<CardSummaryProjection> searchHybridSummaries(
            CardFilter filter,
            String embeddingModel,
            float[] queryEmbedding,
            Pageable pageable
    ) {
        String userId = filter.userId();
        CardStatus status = filter.status();
        CardType cardType = filter.cardType();
        Instant updatedAfter = filter.updatedAfter();
        Instant updatedBefore = filter.updatedBefore();
        String query = filter.query();

        int pageSize = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        int textProbeLimit = Math.max((pageable.getPageNumber() + 1) * pageSize * 5, MIN_TEXT_PROBE_LIMIT);
        int semanticProbeLimit = Math.max((pageable.getPageNumber() + 1) * pageSize * 20, MIN_SEMANTIC_PROBE_LIMIT);
        String queryVector = toVectorLiteral(queryEmbedding);
        String halfvecType = "halfvec(" + embeddingProperties.dimensions() + ")";
        String cardStatusClause = status == null ? "" : " and c.status = ?\n";
        String cardTypeClause = cardType == null ? "" : " and c.card_type = ?\n";
        String cardUpdatedAfterClause = updatedAfter == null ? "" : " and c.updated_at >= ?\n";
        String cardUpdatedBeforeClause = updatedBefore == null ? "" : " and c.updated_at <= ?\n";
        String sql = ("""
                with semantic_chunk_matches as (
                    select
                        ce.card_fk as card_id,
                        cast(ce.embedding_vector as %s) <=> cast(? as %s) as semantic_distance
                    from card_embeddings ce
                    join users u on u.id = ce.user_fk
                    where u.user_id = ?
                      and ce.embedding_model = ?
                    order by cast(ce.embedding_vector as %s) <=> cast(? as %s) asc
                    limit ?
                ),
                semantic_matches as (
                    select
                        card_id,
                        min(semantic_distance) as semantic_distance
                    from semantic_chunk_matches
                    group by card_id
                    having min(semantic_distance) <= ?
                ),
                semantic_filtered as (
                    select
                        sm.card_id,
                        sm.semantic_distance
                    from semantic_matches sm
                    join cards c on c.id = sm.card_id
                    where 1 = 1
                """ + cardStatusClause + cardTypeClause + cardUpdatedAfterClause + cardUpdatedBeforeClause + """
                ),
                semantic_ranked as (
                    select
                        card_id,
                        row_number() over (order by semantic_distance asc, card_id) as semantic_rank
                    from semantic_filtered
                ),
                text_matches as (
                    select
                        c.id as card_id,
                        ts_rank(
                            to_tsvector(
                                'english',
                                coalesce(c.title, '') || ' ' ||
                                coalesce(c.summary, '') || ' ' ||
                                coalesce(c.content, '')
                            ),
                            websearch_to_tsquery('english', ?)
                        ) as text_score
                    from cards c
                    join users u on u.id = c.user_fk
                    where u.user_id = ?
                """ + cardStatusClause + cardTypeClause + cardUpdatedAfterClause + cardUpdatedBeforeClause + """
                      and to_tsvector(
                            'english',
                            coalesce(c.title, '') || ' ' ||
                            coalesce(c.summary, '') || ' ' ||
                            coalesce(c.content, '')
                        ) @@ websearch_to_tsquery('english', ?)
                    order by text_score desc, c.updated_at desc
                    limit ?
                ),
                text_ranked as (
                    select
                        card_id,
                        row_number() over (order by text_score desc, card_id) as text_rank
                    from text_matches
                ),
                candidates as (
                    select card_id from semantic_ranked
                    union
                    select card_id from text_ranked
                )
                select
                    c.id,
                    u.user_id,
                    c.title,
                    c.summary,
                    c.card_type,
                    c.status,
                    c.priority,
                    c.due_date,
                    c.source_message_id,
                    c.created_at,
                    c.updated_at
                from candidates candidate
                join cards c on c.id = candidate.card_id
                join users u on u.id = c.user_fk
                left join text_ranked tr on tr.card_id = c.id
                left join semantic_ranked sr on sr.card_id = c.id
                order by
                    coalesce(1.0 / (? + tr.text_rank), 0.0) +
                    coalesce(1.0 / (? + sr.semantic_rank), 0.0) desc,
                    greatest(
                        coalesce(1.0 / (? + tr.text_rank), 0.0),
                        coalesce(1.0 / (? + sr.semantic_rank), 0.0)
                    ) desc,
                    c.updated_at desc
                limit ?
                offset ?
                """).formatted(halfvecType, halfvecType, halfvecType, halfvecType);

        List<Object> parameters = new ArrayList<>();
        parameters.add(queryVector);
        parameters.add(userId);
        parameters.add(embeddingModel);
        parameters.add(queryVector);
        parameters.add(semanticProbeLimit);
        parameters.add(embeddingProperties.maxSemanticDistance());
        if (status != null) {
            parameters.add(status.name());
        }
        if (cardType != null) {
            parameters.add(cardType.name());
        }
        if (updatedAfter != null) {
            parameters.add(Timestamp.from(updatedAfter));
        }
        if (updatedBefore != null) {
            parameters.add(Timestamp.from(updatedBefore));
        }
        parameters.add(query);
        parameters.add(userId);
        if (status != null) {
            parameters.add(status.name());
        }
        if (cardType != null) {
            parameters.add(cardType.name());
        }
        if (updatedAfter != null) {
            parameters.add(Timestamp.from(updatedAfter));
        }
        if (updatedBefore != null) {
            parameters.add(Timestamp.from(updatedBefore));
        }
        parameters.add(query);
        parameters.add(textProbeLimit);
        parameters.add(RRF_K);
        parameters.add(RRF_K);
        parameters.add(RRF_K);
        parameters.add(RRF_K);
        parameters.add(pageSize + 1);
        parameters.add(offset);

        List<CardSummaryProjection> rows = jdbcTemplate.query(sql, CARD_SUMMARY_ROW_MAPPER, parameters.toArray());

        boolean hasNext = rows.size() > pageSize;
        List<CardSummaryProjection> items = hasNext ? rows.subList(0, pageSize) : rows;
        return new SliceImpl<>(items, pageable, hasNext);
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        builder.append(']');
        return builder.toString();
    }

    private static Instant toInstant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
