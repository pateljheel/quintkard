package io.quintkard.quintkardapp.message;

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
        order by m.ingestedAt desc
        """)
    Slice<MessageSummaryProjection> findSummariesByUserUserIdOrderByIngestedAtDesc(
            @Param("userId") String userId,
            Pageable pageable
    );

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
          and m.status = :status
        order by m.ingestedAt desc
        """)
    Slice<MessageSummaryProjection> findSummariesByUserUserIdAndStatusOrderByIngestedAtDesc(
            @Param("userId") String userId,
            @Param("status") MessageStatus status,
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
            @Param("query") String query,
            Pageable pageable
    );
}
