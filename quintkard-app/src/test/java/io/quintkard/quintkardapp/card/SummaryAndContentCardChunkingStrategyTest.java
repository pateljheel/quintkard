package io.quintkard.quintkardapp.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quintkard.quintkardapp.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryAndContentCardChunkingStrategyTest {

    private final SummaryAndContentCardChunkingStrategy strategy = new SummaryAndContentCardChunkingStrategy();

    @Test
    void chunkCreatesSummaryAndContentChunks() {
        Card card = new Card(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "  Follow up  ",
                "  August invoice   review ",
                " First line.\n\nSecond line. ",
                CardType.FOLLOW_UP,
                CardStatus.OPEN,
                CardPriority.MEDIUM,
                null,
                null
        );

        List<TextChunk> chunks = strategy.chunk(card);

        assertEquals(2, chunks.size());
        assertEquals(new TextChunk(0, TextChunkType.SUMMARY, "Follow up August invoice review"), chunks.get(0));
        assertEquals(TextChunkType.CONTENT, chunks.get(1).chunkType());
        assertEquals("First line. Second line.", chunks.get(1).text());
    }

    @Test
    void chunkReturnsOnlySummaryWhenContentBlank() {
        Card card = new Card(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "Title",
                "Summary",
                "   ",
                CardType.TASK,
                CardStatus.OPEN,
                CardPriority.MEDIUM,
                null,
                null
        );

        List<TextChunk> chunks = strategy.chunk(card);

        assertEquals(1, chunks.size());
        assertEquals(TextChunkType.SUMMARY, chunks.getFirst().chunkType());
    }

    @Test
    void chunkReturnsOnlyContentWhenTitleAndSummaryBlank() {
        Card card = new Card(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "   ",
                null,
                "Actual content",
                CardType.TASK,
                CardStatus.OPEN,
                CardPriority.MEDIUM,
                null,
                null
        );

        List<TextChunk> chunks = strategy.chunk(card);

        assertEquals(1, chunks.size());
        assertEquals(TextChunkType.CONTENT, chunks.getFirst().chunkType());
        assertEquals("Actual content", chunks.getFirst().text());
    }

    @Test
    void chunkSplitsLongContentIntoOverlappingWindows() {
        String content = "A".repeat(900);
        Card card = new Card(
                new User("admin", "Admin", "admin@example.com", "hash", false),
                "Title",
                null,
                content,
                CardType.TASK,
                CardStatus.OPEN,
                CardPriority.MEDIUM,
                null,
                null
        );

        List<TextChunk> chunks = strategy.chunk(card);

        assertEquals(3, chunks.size());
        assertEquals(TextChunkType.SUMMARY, chunks.get(0).chunkType());
        assertEquals(TextChunkType.CONTENT, chunks.get(1).chunkType());
        assertEquals(TextChunkType.CONTENT, chunks.get(2).chunkType());
        assertEquals(800, chunks.get(1).text().length());
        assertTrue(chunks.get(2).text().length() > 100);
    }
}
