package io.quintkard.quintkardapp.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quintkard.quintkardapp.embedding.EmbeddingProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class CardSearchRepositoryImplIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
    );

    private JdbcTemplate jdbcTemplate;
    private CardSearchRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new CardSearchRepositoryImpl(
                jdbcTemplate,
                new EmbeddingProperties("gemini-embedding-001", "summary-and-content-v1", 16, 3, 0.45)
        );

        jdbcTemplate.execute("create extension if not exists vector");
        jdbcTemplate.execute("drop table if exists card_embeddings");
        jdbcTemplate.execute("drop table if exists cards");
        jdbcTemplate.execute("drop table if exists users");
        jdbcTemplate.execute("""
                create table users (
                    id bigint primary key,
                    user_id varchar(255) not null unique,
                    display_name varchar(255) not null,
                    email varchar(255) not null,
                    password_hash varchar(255) not null,
                    redaction_enabled boolean not null,
                    created_at timestamptz not null,
                    updated_at timestamptz not null
                )
                """);
        jdbcTemplate.execute("""
                create table cards (
                    id uuid primary key,
                    user_fk bigint not null references users(id),
                    title varchar(255) not null,
                    summary varchar(255),
                    content text not null,
                    card_type varchar(255) not null,
                    status varchar(255) not null,
                    priority varchar(255) not null,
                    due_date date,
                    source_message_id uuid,
                    created_at timestamptz not null,
                    updated_at timestamptz not null,
                    constraint uk_cards_id_user_fk unique (id, user_fk)
                )
                """);
        jdbcTemplate.execute("""
                create table card_embeddings (
                    id uuid primary key,
                    card_fk uuid not null,
                    user_fk bigint not null references users(id),
                    chunk_index integer not null,
                    chunk_text text not null,
                    chunk_type varchar(255) not null,
                    chunking_strategy varchar(255) not null,
                    embedding_model varchar(255) not null,
                    embedding_vector vector(3) not null,
                    created_at timestamptz not null,
                    updated_at timestamptz not null,
                    constraint fk_card_embeddings_card_owner foreign key (card_fk, user_fk) references cards(id, user_fk)
                )
                """);
    }

    @Test
    void searchHybridSummariesFiltersByUserAndStatus() {
        insertUser(1L, "admin");
        insertUser(2L, "other");
        UUID adminOpen = insertCard(1L, "Invoice follow up", "August invoice", "Need vendor response", CardStatus.OPEN, Instant.parse("2026-04-05T00:00:00Z"));
        insertEmbedding(adminOpen, 1L, "[1,0,0]");
        UUID adminDone = insertCard(1L, "Invoice archive", "Old invoice", "Already resolved", CardStatus.DONE, Instant.parse("2026-04-05T00:05:00Z"));
        insertEmbedding(adminDone, 1L, "[1,0,0]");
        UUID otherOpen = insertCard(2L, "Invoice for other user", "Other invoice", "Other user content", CardStatus.OPEN, Instant.parse("2026-04-05T00:10:00Z"));
        insertEmbedding(otherOpen, 2L, "[1,0,0]");

        Slice<CardSummaryProjection> result = repository.searchHybridSummaries(
                new CardFilter("admin", "invoice", CardStatus.OPEN, null, null, null),
                "gemini-embedding-001",
                new float[] {1f, 0f, 0f},
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getContent().size());
        assertEquals(adminOpen, result.getContent().getFirst().getId());
    }

    @Test
    void searchHybridSummariesIncludesSemanticOnlyMatchesWithinThreshold() {
        insertUser(1L, "admin");
        UUID textCard = insertCard(1L, "Invoice follow up", "Need action", "Vendor correction pending", CardStatus.OPEN, Instant.parse("2026-04-05T00:00:00Z"));
        insertEmbedding(textCard, 1L, "[0,1,0]");
        UUID semanticCard = insertCard(1L, "Account update", "Billing review", "Review customer account changes", CardStatus.OPEN, Instant.parse("2026-04-05T01:00:00Z"));
        insertEmbedding(semanticCard, 1L, "[1,0,0]");

        Slice<CardSummaryProjection> result = repository.searchHybridSummaries(
                new CardFilter("admin", "invoice", null, null, null, null),
                "gemini-embedding-001",
                new float[] {1f, 0f, 0f},
                PageRequest.of(0, 10)
        );

        List<UUID> ids = result.getContent().stream().map(CardSummaryProjection::getId).toList();
        assertEquals(2, ids.size());
        assertTrue(ids.contains(textCard));
        assertTrue(ids.contains(semanticCard));
    }

    @Test
    void searchHybridSummariesExcludesWeakSemanticMatchesOutsideThreshold() {
        insertUser(1L, "admin");
        UUID textCard = insertCard(1L, "Invoice follow up", "Need action", "Vendor correction pending", CardStatus.OPEN, Instant.parse("2026-04-05T00:00:00Z"));
        insertEmbedding(textCard, 1L, "[0,1,0]");
        UUID weakSemanticCard = insertCard(1L, "Account update", "Billing review", "Review customer account changes", CardStatus.OPEN, Instant.parse("2026-04-05T01:00:00Z"));
        insertEmbedding(weakSemanticCard, 1L, "[0,1,0]");

        Slice<CardSummaryProjection> result = repository.searchHybridSummaries(
                new CardFilter("admin", "invoice", null, null, null, null),
                "gemini-embedding-001",
                new float[] {1f, 0f, 0f},
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getContent().size());
        assertEquals(textCard, result.getContent().getFirst().getId());
    }

    @Test
    void searchHybridSummariesAppliesCardTypeAndUpdatedAfterFilters() {
        insertUser(1L, "admin");
        UUID matchingCard = insertCard(
                1L,
                "Invoice follow up",
                "Need action",
                "Vendor correction pending",
                CardType.FOLLOW_UP,
                CardStatus.OPEN,
                Instant.parse("2026-04-05T02:00:00Z")
        );
        insertEmbedding(matchingCard, 1L, "[1,0,0]");
        UUID wrongTypeCard = insertCard(
                1L,
                "Invoice note",
                "Need action",
                "Vendor correction pending",
                CardType.NOTE,
                CardStatus.OPEN,
                Instant.parse("2026-04-05T02:05:00Z")
        );
        insertEmbedding(wrongTypeCard, 1L, "[1,0,0]");
        UUID tooOldCard = insertCard(
                1L,
                "Invoice archive",
                "Need action",
                "Vendor correction pending",
                CardType.FOLLOW_UP,
                CardStatus.OPEN,
                Instant.parse("2026-04-05T00:05:00Z")
        );
        insertEmbedding(tooOldCard, 1L, "[1,0,0]");

        Slice<CardSummaryProjection> result = repository.searchHybridSummaries(
                new CardFilter(
                        "admin",
                        "invoice",
                        CardStatus.OPEN,
                        CardType.FOLLOW_UP,
                        Instant.parse("2026-04-05T01:00:00Z"),
                        Instant.parse("2026-04-05T03:00:00Z")
                ),
                "gemini-embedding-001",
                new float[] {1f, 0f, 0f},
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getContent().size());
        assertEquals(matchingCard, result.getContent().getFirst().getId());
    }

    private void insertUser(long id, String userId) {
        Timestamp now = Timestamp.from(Instant.parse("2026-04-05T00:00:00Z"));
        jdbcTemplate.update(
                """
                insert into users (id, user_id, display_name, email, password_hash, redaction_enabled, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                userId,
                userId,
                userId + "@example.com",
                "hash",
                false,
                now,
                now
        );
    }

    private UUID insertCard(long userFk, String title, String summary, String content, CardStatus status, Instant updatedAt) {
        return insertCard(userFk, title, summary, content, CardType.FOLLOW_UP, status, updatedAt);
    }

    private UUID insertCard(long userFk, String title, String summary, String content, CardType cardType, CardStatus status, Instant updatedAt) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into cards (id, user_fk, title, summary, content, card_type, status, priority, due_date, source_message_id, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                userFk,
                title,
                summary,
                content,
                cardType.name(),
                status.name(),
                CardPriority.MEDIUM.name(),
                null,
                null,
                Timestamp.from(Instant.parse("2026-04-05T00:00:00Z")),
                Timestamp.from(updatedAt)
        );
        return id;
    }

    private void insertEmbedding(UUID cardId, long userFk, String vectorLiteral) {
        Timestamp now = Timestamp.from(Instant.parse("2026-04-05T00:00:00Z"));
        jdbcTemplate.update(
                """
                insert into card_embeddings (id, card_fk, user_fk, chunk_index, chunk_text, chunk_type, chunking_strategy, embedding_model, embedding_vector, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?)
                """,
                UUID.randomUUID(),
                cardId,
                userFk,
                0,
                "chunk",
                "CONTENT",
                "summary-and-content-v1",
                "gemini-embedding-001",
                vectorLiteral,
                now,
                now
        );
    }
}
