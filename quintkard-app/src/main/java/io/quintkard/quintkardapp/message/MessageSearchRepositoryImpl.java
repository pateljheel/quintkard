package io.quintkard.quintkardapp.message;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MessageSearchRepositoryImpl implements MessageSearchRepository {

    private static final RowMapper<MessageSummaryProjection> MESSAGE_SUMMARY_ROW_MAPPER = new RowMapper<>() {
        @Override
        public MessageSummaryProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MessageSummaryItem(
                    rs.getObject("id", UUID.class),
                    rs.getString("user_id"),
                    rs.getString("source_service"),
                    rs.getString("external_message_id"),
                    rs.getString("message_type"),
                    MessageStatus.valueOf(rs.getString("status")),
                    rs.getString("summary"),
                    toInstant(rs, "ingested_at"),
                    toInstant(rs, "source_created_at")
            );
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public MessageSearchRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Slice<MessageSummaryProjection> searchSummaries(MessageFilter filter, Pageable pageable) {
        String sourceServiceClause = filter.sourceService() == null ? "" : " and m.source_service = ?\n";
        String messageTypeClause = filter.messageType() == null ? "" : " and m.message_type = ?\n";
        String ingestedAfterClause = filter.ingestedAfter() == null ? "" : " and m.ingested_at >= ?\n";
        String ingestedBeforeClause = filter.ingestedBefore() == null ? "" : " and m.ingested_at <= ?\n";

        String sql = """
                select
                    m.id,
                    u.user_id,
                    m.source_service,
                    m.external_message_id,
                    m.message_type,
                    m.status,
                    m.summary,
                    m.ingested_at,
                    m.source_created_at
                from messages m
                join users u on u.id = m.user_fk
                where u.user_id = ?
                  and (? is null or m.status = ?)
                """ + sourceServiceClause + messageTypeClause + ingestedAfterClause + ingestedBeforeClause + """
                  and to_tsvector(
                        'english',
                        concat_ws(
                            ' ',
                            coalesce(m.payload, ''),
                            coalesce(m.source_service, ''),
                            coalesce(m.external_message_id, ''),
                            coalesce(m.message_type, '')
                        )
                    ) @@ websearch_to_tsquery('english', ?)
                order by
                    ts_rank(
                        to_tsvector(
                            'english',
                            concat_ws(
                                ' ',
                                coalesce(m.payload, ''),
                                coalesce(m.source_service, ''),
                                coalesce(m.external_message_id, ''),
                                coalesce(m.message_type, '')
                            )
                        ),
                        websearch_to_tsquery('english', ?)
                    ) desc,
                    m.ingested_at desc
                limit ?
                offset ?
                """;

        List<Object> parameters = new ArrayList<>();
        parameters.add(filter.userId());
        parameters.add(filter.status() == null ? null : filter.status().name());
        parameters.add(filter.status() == null ? null : filter.status().name());
        if (filter.sourceService() != null) {
            parameters.add(filter.sourceService());
        }
        if (filter.messageType() != null) {
            parameters.add(filter.messageType());
        }
        if (filter.ingestedAfter() != null) {
            parameters.add(java.sql.Timestamp.from(filter.ingestedAfter()));
        }
        if (filter.ingestedBefore() != null) {
            parameters.add(java.sql.Timestamp.from(filter.ingestedBefore()));
        }
        parameters.add(filter.query());
        parameters.add(filter.query());
        parameters.add(pageable.getPageSize() + 1);
        parameters.add(pageable.getOffset());

        List<MessageSummaryProjection> rows = jdbcTemplate.query(sql, MESSAGE_SUMMARY_ROW_MAPPER, parameters.toArray());
        boolean hasNext = rows.size() > pageable.getPageSize();
        List<MessageSummaryProjection> items = hasNext ? rows.subList(0, pageable.getPageSize()) : rows;
        return new SliceImpl<>(items, pageable, hasNext);
    }

    private static Instant toInstant(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
