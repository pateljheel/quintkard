package io.quintkard.quintkardapp.message;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query(
        value = """
            select id
            from messages
            where status = :status
            order by ingested_at asc
            limit :batchSize
            for update skip locked
            """,
        nativeQuery = true
    )
    List<UUID> claimMessageIdsByStatus(@Param("status") String status, @Param("batchSize") int batchSize);

    boolean existsByStatus(MessageStatus status);

    List<Message> findAllByIdIn(List<UUID> ids);

    Optional<Message> findByIdAndUserUserId(UUID id, String userId);

    @Query("""
        select
            m.id as id,
            u.userId as userId,
            m.sourceService as sourceService,
            m.externalMessageId as externalMessageId,
            m.messageType as messageType,
            m.status as status,
            m.summary as summary,
            m.ingestedAt as ingestedAt,
            m.sourceCreatedAt as sourceCreatedAt
        from Message m
        join m.user u
        where u.userId = :userId
          and (:status is null or m.status = :status)
          and (:sourceService is null or m.sourceService = :sourceService)
          and (:messageType is null or m.messageType = :messageType)
          and m.ingestedAt >= coalesce(:ingestedAfter, m.ingestedAt)
          and m.ingestedAt <= coalesce(:ingestedBefore, m.ingestedAt)
        order by m.ingestedAt desc
        """)
    Slice<MessageSummaryProjection> findSummariesByFiltersOrderByIngestedAtDesc(
            @Param("userId") String userId,
            @Param("status") MessageStatus status,
            @Param("sourceService") String sourceService,
            @Param("messageType") String messageType,
            @Param("ingestedAfter") Instant ingestedAfter,
            @Param("ingestedBefore") Instant ingestedBefore,
            Pageable pageable
    );

    @Query(
        value = """
            select
                m.id as id,
                u.user_id as userId,
                m.source_service as sourceService,
                m.external_message_id as externalMessageId,
                m.message_type as messageType,
                m.status as status,
                m.summary as summary,
                m.ingested_at as ingestedAt,
                m.source_created_at as sourceCreatedAt
            from messages m
            join users u on u.id = m.user_fk
            where u.user_id = :userId
              and (:status is null or m.status = :status)
              and (:sourceService is null or m.source_service = :sourceService)
              and (:messageType is null or m.message_type = :messageType)
              and m.ingested_at >= coalesce(:ingestedAfter, m.ingested_at)
              and m.ingested_at <= coalesce(:ingestedBefore, m.ingested_at)
              and to_tsvector(
                    'english',
                    concat_ws(
                        ' ',
                        coalesce(m.payload, ''),
                        coalesce(m.source_service, ''),
                        coalesce(m.external_message_id, ''),
                        coalesce(m.message_type, '')
                    )
                  ) @@ websearch_to_tsquery('english', :query)
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
                    websearch_to_tsquery('english', :query)
                ) desc,
                m.ingested_at desc
            """,
        nativeQuery = true
    )
    Slice<MessageSummaryProjection> searchSummariesByUserId(
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("sourceService") String sourceService,
            @Param("messageType") String messageType,
            @Param("ingestedAfter") Instant ingestedAfter,
            @Param("ingestedBefore") Instant ingestedBefore,
            @Param("query") String query,
            Pageable pageable
    );
}
