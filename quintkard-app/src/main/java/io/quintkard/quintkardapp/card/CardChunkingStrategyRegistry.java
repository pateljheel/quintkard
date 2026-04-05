package io.quintkard.quintkardapp.card;

public interface CardChunkingStrategyRegistry {

    CardChunkingStrategy get(String strategyName);
}
