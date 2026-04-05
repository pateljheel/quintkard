package io.quintkard.quintkardapp.card;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, UUID>, CardSearchRepository {

    Optional<Card> findByIdAndUser_UserId(UUID id, String userId);

    @Query("""
        select
            c.id as id,
            u.userId as userId,
            c.title as title,
            c.summary as summary,
            c.cardType as cardType,
            c.status as status,
            c.priority as priority,
            c.dueDate as dueDate,
            c.sourceMessageId as sourceMessageId,
            c.createdAt as createdAt,
            c.updatedAt as updatedAt
        from Card c
        join c.user u
        where u.userId = :userId
        order by c.updatedAt desc
        """)
    Slice<CardSummaryProjection> findSummariesByUserUserIdOrderByUpdatedAtDesc(
            @Param("userId") String userId,
            Pageable pageable
    );

    @Query("""
        select
            c.id as id,
            u.userId as userId,
            c.title as title,
            c.summary as summary,
            c.cardType as cardType,
            c.status as status,
            c.priority as priority,
            c.dueDate as dueDate,
            c.sourceMessageId as sourceMessageId,
            c.createdAt as createdAt,
            c.updatedAt as updatedAt
        from Card c
        join c.user u
        where u.userId = :userId
          and c.status = :status
        order by c.updatedAt desc
        """)
    Slice<CardSummaryProjection> findSummariesByUserUserIdAndStatusOrderByUpdatedAtDesc(
            @Param("userId") String userId,
            @Param("status") CardStatus status,
            Pageable pageable
    );

}
