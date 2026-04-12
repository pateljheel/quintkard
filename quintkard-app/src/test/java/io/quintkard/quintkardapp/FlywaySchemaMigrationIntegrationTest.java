package io.quintkard.quintkardapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class FlywaySchemaMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
    );

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        jdbcTemplate = new JdbcTemplate(dataSource);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void migrateCreatesSchemaAndIndexes() {
        assertEquals(1, count("select count(*) from flyway_schema_history where version = '1' and success = true"));
        assertEquals(1, count("select count(*) from information_schema.tables where table_name = 'users'"));
        assertEquals(1, count("select count(*) from information_schema.tables where table_name = 'cards'"));
        assertEquals(1, count("select count(*) from information_schema.tables where table_name = 'card_embeddings'"));
        assertEquals(1, count("select count(*) from information_schema.tables where table_name = 'messages'"));
        assertEquals(1, count("select count(*) from information_schema.tables where table_name = 'agent_configs'"));
        assertEquals(1, count("select count(*) from information_schema.tables where table_name = 'orchestrator_configs'"));
        assertEquals(1, count("select count(*) from information_schema.tables where table_name = 'orchestration_active_agents'"));
        assertEquals(1, count("select count(*) from pg_indexes where indexname = 'idx_cards_search'"));
        assertEquals(1, count("select count(*) from pg_indexes where indexname = 'idx_messages_search'"));
        assertEquals(1, count("select count(*) from pg_indexes where indexname = 'idx_messages_status_ingested_at'"));

        String dataType = jdbcTemplate.queryForObject(
                """
                select udt_name
                from information_schema.columns
                where table_name = 'card_embeddings'
                  and column_name = 'embedding_vector'
                """,
                String.class
        );
        assertEquals("vector", dataType);

        String extensionName = jdbcTemplate.queryForObject(
                "select extname from pg_extension where extname = 'vector'",
                String.class
        );
        assertEquals("vector", extensionName);
    }

    @Test
    void migrateIsRepeatableAgainstExistingSchema() {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword()
                )
                .cleanDisabled(false)
                .load();

        flyway.migrate();

        assertTrue(count("select count(*) from flyway_schema_history where version = '1' and success = true") >= 1);
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
