package io.quintkard.quintkardapp.card;

import io.quintkard.quintkardapp.common.AuditableEntity;
import io.quintkard.quintkardapp.user.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cards")
public class Card extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_fk", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardPriority priority;

    @Column
    private LocalDate dueDate;

    @Column
    private UUID sourceMessageId;

    protected Card() {
    }

    public Card(
        User user,
        String title,
        String summary,
        String content,
        CardType cardType,
        CardStatus status,
        CardPriority priority,
        LocalDate dueDate,
        UUID sourceMessageId
    ) {
        this.user = user;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.cardType = cardType;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.sourceMessageId = sourceMessageId;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSummary() {
        return summary;
    }

    public CardType getCardType() {
        return cardType;
    }

    public CardStatus getStatus() {
        return status;
    }

    public CardPriority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public UUID getSourceMessageId() {
        return sourceMessageId;
    }

    public void update(
        String title,
        String summary,
        String content,
        CardType cardType,
        CardStatus status,
        CardPriority priority,
        LocalDate dueDate,
        UUID sourceMessageId
    ) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.cardType = cardType;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.sourceMessageId = sourceMessageId;
    }

    public void changeStatus(CardStatus status) {
        this.status = status;
    }
}
