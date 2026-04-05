package io.quintkard.quintkardapp.message;

import io.quintkard.quintkardapp.common.AuditableEntity;
import io.quintkard.quintkardapp.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "messages")
public class Message extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_fk", nullable = false)
    private User user;

    @Column(nullable = false)
    private String sourceService;

    @Column
    private String externalMessageId;

    @Column(nullable = false)
    private String messageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(length = 280)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details_json", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(nullable = false)
    private Instant ingestedAt;

    @Column
    private Instant sourceCreatedAt;

    protected Message() {
    }

    public Message(
            User user,
            String sourceService,
            String externalMessageId,
            String messageType,
            MessageStatus status,
            String payload,
            String summary,
            Map<String, Object> metadata,
            Map<String, Object> details,
            Instant ingestedAt,
            Instant sourceCreatedAt
    ) {
        this.user = user;
        this.sourceService = sourceService;
        this.externalMessageId = externalMessageId;
        this.messageType = messageType;
        this.status = status;
        this.payload = payload;
        this.summary = summary;
        this.metadata = metadata;
        this.details = details;
        this.ingestedAt = ingestedAt;
        this.sourceCreatedAt = sourceCreatedAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getSourceService() {
        return sourceService;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public String getPayload() {
        return payload;
    }

    public String getSummary() {
        return summary;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public Instant getSourceCreatedAt() {
        return sourceCreatedAt;
    }

    public void markProcessing() {
        this.status = MessageStatus.PROCESSING;
    }

    public void markPending() {
        this.status = MessageStatus.PENDING;
    }

    public void markFailed() {
        this.status = MessageStatus.FAILED;
    }

    public void markSuccess() {
        this.status = MessageStatus.SUCCESS;
    }

    public void updateDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            this.details = null;
            return;
        }
        this.details = new LinkedHashMap<>(details);
    }
}
