package io.quintkard.quintkardapp.message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID>, JpaSpecificationExecutor<Message>, MessageSearchRepository {

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
}
