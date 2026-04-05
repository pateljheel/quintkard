package io.quintkard.quintkardapp.card;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SummaryAndContentCardChunkingStrategy implements CardChunkingStrategy {

    private static final int CONTENT_CHUNK_LENGTH = 800;
    private static final int CONTENT_CHUNK_OVERLAP = 120;

    @Override
    public List<TextChunk> chunk(Card card) {
        List<TextChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        String summaryChunk = normalize(joinNonBlank(
                card.getTitle(),
                card.getSummary()
        ));
        if (!summaryChunk.isBlank()) {
            chunks.add(new TextChunk(chunkIndex++, TextChunkType.SUMMARY, summaryChunk));
        }

        String content = normalize(card.getContent());
        if (content.isBlank()) {
            return chunks;
        }

        for (String part : splitIntoWindows(content, CONTENT_CHUNK_LENGTH, CONTENT_CHUNK_OVERLAP)) {
            chunks.add(new TextChunk(chunkIndex++, TextChunkType.CONTENT, part));
        }

        return chunks;
    }

    private List<String> splitIntoWindows(String content, int chunkLength, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + chunkLength);
            String window = content.substring(start, end).trim();
            if (!window.isBlank()) {
                chunks.add(window);
            }
            if (end >= content.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    private String joinNonBlank(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                parts.add(value.trim());
            }
        }
        return String.join("\n\n", parts);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
