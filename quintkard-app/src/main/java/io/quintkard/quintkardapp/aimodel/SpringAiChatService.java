package io.quintkard.quintkardapp.aimodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SpringAiChatService implements AiChatService {

    private final ChatMemory chatMemory;
    private final AiChatModelRegistry chatModelRegistry;
    private final AiChatOptionsFactory chatOptionsFactory;
    private final ObjectMapper objectMapper;

    public SpringAiChatService(
            ChatMemory chatMemory,
            AiChatModelRegistry chatModelRegistry,
            AiChatOptionsFactory chatOptionsFactory,
            ObjectMapper objectMapper
    ) {
        this.chatMemory = chatMemory;
        this.chatModelRegistry = chatModelRegistry;
        this.chatOptionsFactory = chatOptionsFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        Prompt prompt = buildPrompt(
                request.userId(),
                request.model(),
                request.temperature(),
                request.messages(),
                request.memoryScope(),
                request.toolScope(),
                null,
                null
        );

        ChatModel chatModel = chatModelRegistry.get(request.model());
        ChatResponse response = chatModel.call(prompt);
        AssistantMessage assistantMessage = response.getResult().getOutput();
        persistAssistantMessage(request.memoryScope(), assistantMessage);

        return new AiChatResponse(
                assistantMessage.getText(),
                assistantMessage.getToolCalls().stream()
                        .map(this::toToolCall)
                        .toList(),
                !assistantMessage.hasToolCalls()
        );
    }

    @Override
    public <T> T chatForObject(AiStructuredChatRequest<T> request) {
        BeanOutputConverter<T> outputConverter = new BeanOutputConverter<>(request.responseType());

        List<AiMessage> messages = appendStructuredOutputInstructions(request.messages(), outputConverter.getFormat());

        Prompt prompt = buildPrompt(
                request.userId(),
                request.model(),
                request.temperature(),
                messages,
                request.memoryScope(),
                request.toolScope(),
                outputConverter.getJsonSchema(),
                "application/json"
        );

        ChatModel chatModel = chatModelRegistry.get(request.model());
        ChatResponse response = chatModel.call(prompt);
        AssistantMessage assistantMessage = response.getResult().getOutput();
        persistAssistantMessage(request.memoryScope(), assistantMessage);

        return outputConverter.convert(assistantMessage.getText());
    }

    private List<AiMessage> appendStructuredOutputInstructions(List<AiMessage> requestMessages, String formatInstructions) {
        List<AiMessage> messages = new ArrayList<>(requestMessages);
        for (int i = 0; i < messages.size(); i++) {
            AiMessage message = messages.get(i);
            if (message.role() == AiMessageRole.SYSTEM) {
                messages.set(i, new AiMessage(
                        AiMessageRole.SYSTEM,
                        message.content() + "\n\n" + formatInstructions
                ));
                return messages;
            }
        }

        messages.add(0, new AiMessage(AiMessageRole.SYSTEM, formatInstructions));
        return messages;
    }

    private Prompt buildPrompt(
            String userId,
            String model,
            double temperature,
            List<AiMessage> requestMessages,
            AiMemoryScope memoryScope,
            AiToolScope toolScope,
            String responseSchema,
            String responseMimeType
    ) {
        List<Message> promptMessages = new ArrayList<>();
        if (memoryScope != null) {
            promptMessages.addAll(chatMemory.get(memoryScope.conversationId()));
        }

        List<Message> newMessages = requestMessages.stream()
                .map(this::toSpringMessage)
                .toList();
        promptMessages.addAll(newMessages);

        if (memoryScope != null && !newMessages.isEmpty()) {
            List<Message> memoryMessages = newMessages.stream()
                    .filter(message -> !(message instanceof SystemMessage))
                    .toList();
            if (!memoryMessages.isEmpty()) {
                chatMemory.add(memoryScope.conversationId(), memoryMessages);
            }
        }

        AiProvider provider = chatModelRegistry.providerFor(model);
        ChatOptions options = chatOptionsFactory.build(
                provider,
                userId,
                model,
                temperature,
                toolScope,
                responseSchema,
                responseMimeType
        );

        return new Prompt(promptMessages, options);
    }

    private Message toSpringMessage(AiMessage message) {
        return switch (message.role()) {
            case SYSTEM -> new SystemMessage(message.content());
            case USER -> new UserMessage(message.content());
            case ASSISTANT -> new AssistantMessage(message.content());
            case TOOL -> toToolResponseMessage(message.content());
        };
    }

    private AiToolCall toToolCall(AssistantMessage.ToolCall toolCall) {
        return new AiToolCall(
                toolCall.id(),
                toolCall.name(),
                parseArguments(toolCall.arguments())
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String arguments) {
        try {
            return objectMapper.readValue(arguments, Map.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse tool arguments", exception);
        }
    }

    private void persistAssistantMessage(AiMemoryScope memoryScope, AssistantMessage assistantMessage) {
        if (memoryScope != null) {
            chatMemory.add(memoryScope.conversationId(), assistantMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private ToolResponseMessage toToolResponseMessage(String content) {
        try {
            List<Map<String, Object>> payload = objectMapper.readValue(content, List.class);
            List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
            for (Map<String, Object> item : payload) {
                String toolName = String.valueOf(item.getOrDefault("toolName", "tool"));
                String toolCallId = String.valueOf(item.getOrDefault("toolCallId", toolName));
                Object result = item.get("result");
                String serializedResult = objectMapper.writeValueAsString(result);
                responses.add(new ToolResponseMessage.ToolResponse(toolCallId, toolName, serializedResult));
            }
            return ToolResponseMessage.builder()
                    .responses(responses)
                    .build();
        } catch (Exception exception) {
            try {
                Map<String, Object> payload = objectMapper.readValue(content, Map.class);
                String toolName = String.valueOf(payload.getOrDefault("toolName", "tool"));
                String toolCallId = String.valueOf(payload.getOrDefault("toolCallId", toolName));
                Object result = payload.get("result");
                String serializedResult = objectMapper.writeValueAsString(result);
                return ToolResponseMessage.builder()
                        .responses(List.of(new ToolResponseMessage.ToolResponse(toolCallId, toolName, serializedResult)))
                        .build();
            } catch (Exception ignored) {
                return ToolResponseMessage.builder()
                        .responses(List.of(new ToolResponseMessage.ToolResponse("tool", "tool", content)))
                        .build();
            }
        }
    }
}
