package io.quintkard.quintkardapp.aimodel;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.stereotype.Service;

@Service
public class SpringAiMemoryService implements AiMemoryService {

    private final ChatMemoryRepository chatMemoryRepository;

    public SpringAiMemoryService(ChatMemoryRepository chatMemoryRepository) {
        this.chatMemoryRepository = chatMemoryRepository;
    }

    @Override
    public void clear(AiMemoryScope memoryScope) {
        if (memoryScope == null || memoryScope.conversationId() == null || memoryScope.conversationId().isBlank()) {
            return;
        }
        chatMemoryRepository.deleteByConversationId(memoryScope.conversationId());
    }
}
