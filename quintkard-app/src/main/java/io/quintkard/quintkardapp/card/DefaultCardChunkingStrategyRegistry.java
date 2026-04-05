package io.quintkard.quintkardapp.card;

import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Component;

@Component
public class DefaultCardChunkingStrategyRegistry implements CardChunkingStrategyRegistry {

    private final Map<String, CardChunkingStrategy> strategies;

    public DefaultCardChunkingStrategyRegistry(SummaryAndContentCardChunkingStrategy summaryAndContent) {
        this.strategies = Map.of(
                "summary-and-content-v1", summaryAndContent
        );
    }

    @Override
    public CardChunkingStrategy get(String strategyName) {
        CardChunkingStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw new NoSuchElementException("Unsupported chunking strategy: " + strategyName);
        }
        return strategy;
    }
}
