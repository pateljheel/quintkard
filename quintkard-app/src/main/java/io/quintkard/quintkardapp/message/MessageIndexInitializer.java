package io.quintkard.quintkardapp.message;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class MessageIndexInitializer {

    @Bean
    ApplicationRunner ensureMessageIndexes(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("""
                create index if not exists idx_messages_user_ingested_at
                on messages (user_fk, ingested_at desc)
                """);

            jdbcTemplate.execute("""
                create index if not exists idx_messages_user_status_ingested_at
                on messages (user_fk, status, ingested_at desc)
                """);

            jdbcTemplate.execute("""
                create index if not exists idx_messages_search
                on messages using gin (
                    to_tsvector(
                        'english',
                        coalesce(payload, '') || ' ' ||
                        coalesce(source_service, '') || ' ' ||
                        coalesce(external_message_id, '') || ' ' ||
                        coalesce(message_type, '')
                    )
                )
                """);
        };
    }
}
