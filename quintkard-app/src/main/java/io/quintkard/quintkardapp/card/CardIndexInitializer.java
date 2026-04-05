package io.quintkard.quintkardapp.card;

import io.quintkard.quintkardapp.embedding.EmbeddingProperties;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class CardIndexInitializer {

    @Bean
    ApplicationRunner ensureCardIndexes(JdbcTemplate jdbcTemplate, EmbeddingProperties embeddingProperties) {
        return args -> {
            jdbcTemplate.execute("""
                alter table card_embeddings
                alter column embedding_vector type vector(%d)
                using embedding_vector::vector(%d)
                """.formatted(embeddingProperties.dimensions(), embeddingProperties.dimensions()));

            jdbcTemplate.execute("""
                create index if not exists idx_cards_user_updated_at
                on cards (user_fk, updated_at desc)
                """);

            jdbcTemplate.execute("""
                create index if not exists idx_cards_user_status_updated_at
                on cards (user_fk, status, updated_at desc)
                """);

            jdbcTemplate.execute("""
                create index if not exists idx_cards_search
                on cards using gin (
                    to_tsvector(
                        'english',
                        coalesce(title, '') || ' ' ||
                        coalesce(summary, '') || ' ' ||
                        coalesce(content, '')
                    )
                )
                """);

            jdbcTemplate.execute("""
                create index if not exists idx_card_embeddings_card_model
                on card_embeddings (card_fk, embedding_model)
                """);
        };
    }
}
