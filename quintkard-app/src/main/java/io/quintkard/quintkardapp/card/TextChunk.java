package io.quintkard.quintkardapp.card;

public record TextChunk(
        int chunkIndex,
        TextChunkType chunkType,
        String text
) {
}
