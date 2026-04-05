package io.quintkard.quintkardapp.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quintkard.embedding")
public record EmbeddingProperties(
        String model,
        String chunkingStrategy,
        int batchSize,
        int dimensions,
        double maxSemanticDistance
) {

    public EmbeddingProperties {
        model = (model == null || model.isBlank()) ? "default" : model.trim();
        chunkingStrategy = (chunkingStrategy == null || chunkingStrategy.isBlank())
                ? "summary-and-content-v1"
                : chunkingStrategy.trim();
        batchSize = batchSize <= 0 ? 16 : batchSize;
        dimensions = dimensions <= 0 ? 3072 : dimensions;
        maxSemanticDistance = maxSemanticDistance <= 0 ? 0.35 : maxSemanticDistance;
    }
}
