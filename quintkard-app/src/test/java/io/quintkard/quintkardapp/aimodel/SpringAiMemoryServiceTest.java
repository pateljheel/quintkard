package io.quintkard.quintkardapp.aimodel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;

class SpringAiMemoryServiceTest {

    private ChatMemoryRepository chatMemoryRepository;
    private SpringAiMemoryService springAiMemoryService;

    @BeforeEach
    void setUp() {
        chatMemoryRepository = mock(ChatMemoryRepository.class);
        springAiMemoryService = new SpringAiMemoryService(chatMemoryRepository);
    }

    @Test
    void ignoresNullMemoryScope() {
        springAiMemoryService.clear(null);

        verifyNoInteractions(chatMemoryRepository);
    }

    @Test
    void ignoresNullConversationId() {
        springAiMemoryService.clear(new AiMemoryScope(null));

        verifyNoInteractions(chatMemoryRepository);
    }

    @Test
    void ignoresBlankConversationId() {
        springAiMemoryService.clear(new AiMemoryScope("   "));

        verifyNoInteractions(chatMemoryRepository);
    }

    @Test
    void deletesConversationWhenConversationIdIsPresent() {
        springAiMemoryService.clear(new AiMemoryScope("msg:1:run:1"));

        verify(chatMemoryRepository).deleteByConversationId("msg:1:run:1");
    }
}
