package io.quintkard.quintkardapp.messagepipeline;

import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.message.MessageStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface InternalMessageQueueRepository extends Repository<Message, UUID> {

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

    Optional<Message> findById(UUID id);

    Iterable<Message> saveAll(Iterable<Message> messages);
}
