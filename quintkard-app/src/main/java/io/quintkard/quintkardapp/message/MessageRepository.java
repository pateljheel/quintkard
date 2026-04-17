package io.quintkard.quintkardapp.message;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface MessageRepository extends Repository<Message, UUID>, JpaSpecificationExecutor<Message>, MessageSearchRepository {

    Message save(Message message);

    Iterable<Message> saveAll(Iterable<Message> messages);

    Page<Message> findAll(Specification<Message> specification, Pageable pageable);

    Optional<Message> findByIdAndUserUserId(UUID id, String userId);

    boolean existsByUser_UserIdAndExternalMessageId(String userId, String externalMessageId);

    void delete(Message message);
}
