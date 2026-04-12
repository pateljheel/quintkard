package io.quintkard.quintkardapp.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
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
class MessageSearchRepositoryImplIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
    );

    private JdbcTemplate jdbcTemplate;
    private MessageSearchRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new MessageSearchRepositoryImpl(jdbcTemplate);

        jdbcTemplate.execute("drop table if exists messages cascade");
        jdbcTemplate.execute("drop table if exists users cascade");

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
                create table messages (
                    id uuid primary key,
                    user_fk bigint not null references users(id),
                    source_service varchar(255) not null,
                    external_message_id varchar(255),
                    message_type varchar(255) not null,
                    status varchar(255) not null,
                    payload text not null,
                    summary varchar(280),
                    metadata_json jsonb,
                    details_json jsonb,
                    ingested_at timestamptz not null,
                    source_created_at timestamptz
                )
                """);
    }

    @Test
    void searchSummariesFiltersByUserAndStatus() {
        insertUser(1L, "admin");
        insertUser(2L, "other");
        UUID matchingMessage = insertMessage(
                1L,
                "gmail",
                "EMAIL",
                MessageStatus.PENDING,
                "Follow up on invoice from last August",
                Instant.parse("2026-04-05T12:00:00Z")
        );
        insertMessage(
                1L,
                "gmail",
                "EMAIL",
                MessageStatus.SUCCESS,
                "Follow up on invoice from last August",
                Instant.parse("2026-04-05T12:05:00Z")
        );
        insertMessage(
                2L,
                "gmail",
                "EMAIL",
                MessageStatus.PENDING,
                "Follow up on invoice from last August",
                Instant.parse("2026-04-05T12:10:00Z")
        );

        Slice<MessageSummaryProjection> result = repository.searchSummaries(
                new MessageFilter("admin", "invoice", MessageStatus.PENDING, null, null, null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getContent().size());
        assertEquals(matchingMessage, result.getContent().getFirst().getId());
    }

    @Test
    void searchSummariesAppliesStructuredFiltersAndPagination() {
        insertUser(1L, "admin");
        UUID firstMatch = insertMessage(
                1L,
                "gmail",
                "EMAIL",
                MessageStatus.PENDING,
                "Account update and invoice review",
                Instant.parse("2026-04-05T12:00:00Z")
        );
        UUID secondMatch = insertMessage(
                1L,
                "gmail",
                "EMAIL",
                MessageStatus.PENDING,
                "Invoice account update required",
                Instant.parse("2026-04-05T12:10:00Z")
        );
        insertMessage(
                1L,
                "slack",
                "CHANNEL_MESSAGE",
                MessageStatus.PENDING,
                "Invoice account update required",
                Instant.parse("2026-04-05T12:20:00Z")
        );
        insertMessage(
                1L,
                "gmail",
                "EMAIL",
                MessageStatus.PENDING,
                "Invoice account update required",
                Instant.parse("2026-04-05T11:00:00Z")
        );

        MessageFilter filter = new MessageFilter(
                "admin",
                "account update",
                MessageStatus.PENDING,
                "gmail",
                "EMAIL",
                Instant.parse("2026-04-05T11:30:00Z"),
                Instant.parse("2026-04-05T12:30:00Z")
        );

        Slice<MessageSummaryProjection> firstPage = repository.searchSummaries(filter, PageRequest.of(0, 1));
        Slice<MessageSummaryProjection> secondPage = repository.searchSummaries(filter, PageRequest.of(1, 1));

        assertEquals(1, firstPage.getContent().size());
        assertTrue(firstPage.hasNext());
        assertEquals(secondMatch, firstPage.getContent().getFirst().getId());

        assertEquals(1, secondPage.getContent().size());
        assertEquals(firstMatch, secondPage.getContent().getFirst().getId());
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

    private UUID insertMessage(
            long userFk,
            String sourceService,
            String messageType,
            MessageStatus status,
            String payload,
            Instant ingestedAt
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into messages (
                    id, user_fk, source_service, external_message_id, message_type, status,
                    payload, summary, metadata_json, details_json, ingested_at, source_created_at
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb), ?, ?)
                """,
                id,
                userFk,
                sourceService,
                "ext-" + id,
                messageType,
                status.name(),
                payload,
                payload.length() > 40 ? payload.substring(0, 40) : payload,
                "{}",
                "{}",
                Timestamp.from(ingestedAt),
                Timestamp.from(ingestedAt.minusSeconds(60))
        );
        return id;
    }
}
