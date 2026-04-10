package io.quintkard.quintkardapp.aimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiModelConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiModelConfiguration.class)
            .withPropertyValues(
                    "quintkard.ai.models[gemini-2.5-flash].provider=GOOGLE_GENAI",
                    "quintkard.ai.models[gpt-5.4-mini].provider=OPENAI"
            );

    @Test
    void registersSharedMemoryBeansAndBindsModelCatalog() {
        contextRunner.run(context -> {
            ChatMemoryRepository chatMemoryRepository = context.getBean(ChatMemoryRepository.class);
            ChatMemory chatMemory = context.getBean(ChatMemory.class);
            AiModelCatalogProperties catalogProperties = context.getBean(AiModelCatalogProperties.class);

            assertNotNull(chatMemoryRepository);
            assertNotNull(chatMemory);
            assertEquals(AiProvider.GOOGLE_GENAI,
                    catalogProperties.getModels().get("gemini-2.5-flash").provider());
            assertEquals(AiProvider.OPENAI,
                    catalogProperties.getModels().get("gpt-5.4-mini").provider());
        });
    }
}
