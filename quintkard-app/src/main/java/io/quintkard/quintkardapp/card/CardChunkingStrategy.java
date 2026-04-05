package io.quintkard.quintkardapp.card;

import java.util.List;

public interface CardChunkingStrategy {

    List<TextChunk> chunk(Card card);
}
