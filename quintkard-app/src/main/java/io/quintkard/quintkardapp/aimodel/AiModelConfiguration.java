package io.quintkard.quintkardapp.aimodel;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(OpenAiChatConnectionProperties.class)
public class AiModelConfiguration {

    @Bean
    ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(100)
                .build();
    }

    @Bean
    AiModelCatalog aiModelCatalog() {
        return new AiModelCatalog();
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai", name = "api-key")
    @ConditionalOnMissingBean(OpenAiApi.class)
    OpenAiApi openAiApi(
            OpenAiChatConnectionProperties properties,
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder
    ) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("spring.ai.openai.api-key must not be blank");
        }

        return OpenAiApi.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai", name = "api-key")
    @ConditionalOnMissingBean(OpenAiChatModel.class)
    OpenAiChatModel openAiChatModel(
            OpenAiApi openAiApi,
            ToolCallingManager toolCallingManager,
            ObjectProvider<RetryTemplate> retryTemplateProvider,
            ObjectProvider<ObservationRegistry> observationRegistryProvider,
            ObjectProvider<ToolExecutionEligibilityPredicate> toolExecutionEligibilityPredicateProvider
    ) {
        OpenAiChatModel.Builder builder = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().build())
                .toolCallingManager(toolCallingManager)
                .retryTemplate(retryTemplateProvider.getIfAvailable(RetryTemplate::new))
                .observationRegistry(observationRegistryProvider.getIfAvailable(ObservationRegistry::create));

        ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate =
                toolExecutionEligibilityPredicateProvider.getIfAvailable();
        if (toolExecutionEligibilityPredicate != null) {
            builder.toolExecutionEligibilityPredicate(toolExecutionEligibilityPredicate);
        }

        return builder.build();
    }
}
