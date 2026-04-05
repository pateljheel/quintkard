package io.quintkard.quintkardapp.aimodel;

import io.quintkard.quintkardapp.agenttool.AiTool;
import io.quintkard.quintkardapp.agenttool.AiToolExecutionRequest;
import io.quintkard.quintkardapp.agenttool.AiToolScopeResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SpringAiChatService implements AiChatService {

    private final ChatMemory chatMemory;
    private final ChatModel chatModel;
    private final AiToolScopeResolver toolScopeResolver;
    private final ObjectMapper objectMapper;

    public SpringAiChatService(
            ChatMemory chatMemory,
            ChatModel chatModel,
            AiToolScopeResolver toolScopeResolver,
            ObjectMapper objectMapper
    ) {
        this.chatMemory = chatMemory;
        this.chatModel = chatModel;
        this.toolScopeResolver = toolScopeResolver;
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

        return new Prompt(
                promptMessages,
                buildOptions(userId, model, temperature, toolScope, responseSchema, responseMimeType)
        );
    }

    private GoogleGenAiChatOptions buildOptions(
            String userId,
            String model,
            double temperature,
            AiToolScope toolScope,
            String responseSchema,
            String responseMimeType
    ) {
        GoogleGenAiChatOptions.Builder builder = GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .internalToolExecutionEnabled(false)
                .labels(Map.of("userId", userId));

        if (responseSchema != null) {
            builder.responseSchema(responseSchema);
        }

        if (responseMimeType != null) {
            builder.responseMimeType(responseMimeType);
        }

        if (toolScope != null && toolScope.allowedToolNames() != null && !toolScope.allowedToolNames().isEmpty()) {
            List<AiTool> tools = toolScopeResolver.resolveTools(userId, toolScope.allowedToolNames());
            Set<String> allowedToolNames = new LinkedHashSet<>();
            List<ToolCallback> toolCallbacks = new ArrayList<>();
            Map<String, Object> toolContext = new LinkedHashMap<>();

            for (AiTool tool : tools) {
                allowedToolNames.add(tool.name());
                toolCallbacks.add(FunctionToolCallback
                        .builder(tool.name(), (Map<String, Object> arguments) -> tool.execute(
                                new AiToolExecutionRequest(
                                        userId,
                                        null,
                                        arguments
                                )
                        ))
                        .description(tool.description())
                        .inputType(tool.inputType())
                        .build());
            }

            toolContext.put("userId", userId);
            builder.toolNames(allowedToolNames);
            builder.toolCallbacks(toolCallbacks);
            builder.toolContext(toolContext);
        }

        return builder.build();
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
                Object result = item.get("result");
                String serializedResult = objectMapper.writeValueAsString(result);
                responses.add(new ToolResponseMessage.ToolResponse(toolName, toolName, serializedResult));
            }
            return ToolResponseMessage.builder()
                    .responses(responses)
                    .build();
        } catch (Exception exception) {
            try {
                Map<String, Object> payload = objectMapper.readValue(content, Map.class);
                String toolName = String.valueOf(payload.getOrDefault("toolName", "tool"));
                Object result = payload.get("result");
                String serializedResult = objectMapper.writeValueAsString(result);
                return ToolResponseMessage.builder()
                        .responses(List.of(new ToolResponseMessage.ToolResponse(toolName, toolName, serializedResult)))
                        .build();
            } catch (Exception ignored) {
                return ToolResponseMessage.builder()
                        .responses(List.of(new ToolResponseMessage.ToolResponse("tool", "tool", content)))
                        .build();
            }
        }
    }
}
