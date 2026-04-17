package io.quintkard.quintkardapp.aimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiModelConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiModelConfiguration.class);

    @Test
    void registersSharedMemoryBeansAndModelCatalog() {
        contextRunner.run(context -> {
            ChatMemoryRepository chatMemoryRepository = context.getBean(ChatMemoryRepository.class);
            ChatMemory chatMemory = context.getBean(ChatMemory.class);
            AiModelCatalog catalog = context.getBean(AiModelCatalog.class);

            assertNotNull(chatMemoryRepository);
            assertNotNull(chatMemory);
            assertEquals(AiProvider.GOOGLE_GENAI, catalog.getModel("gemini-2.5-flash").provider());
            assertEquals(AiProvider.OPENAI, catalog.getModel("gpt-5.4-mini").provider());
        });
    }
}
