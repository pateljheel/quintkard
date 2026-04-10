package io.quintkard.quintkardapp.aimodel;

import io.quintkard.quintkardapp.agenttool.AiTool;
import io.quintkard.quintkardapp.agenttool.AiToolExecutionRequest;
import io.quintkard.quintkardapp.agenttool.AiToolScopeResolver;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;

@Service
public class DefaultAiChatOptionsFactory implements AiChatOptionsFactory {

    private static final String USER_ID_CONTEXT_KEY = "userId";

    private final AiToolScopeResolver toolScopeResolver;

    public DefaultAiChatOptionsFactory(AiToolScopeResolver toolScopeResolver) {
        this.toolScopeResolver = toolScopeResolver;
    }

    @Override
    public ChatOptions build(
            AiProvider provider,
            String userId,
            String model,
            double temperature,
            AiToolScope toolScope,
            String responseSchema,
            String responseMimeType
    ) {
        ToolOptions toolOptions = resolveToolOptions(userId, toolScope);

        return switch (provider) {
            case GOOGLE_GENAI -> buildGoogleOptions(
                    userId,
                    model,
                    temperature,
                    toolOptions,
                    responseSchema,
                    responseMimeType
            );
            case OPENAI -> buildOpenAiOptions(
                    userId,
                    model,
                    temperature,
                    toolOptions,
                    responseSchema
            );
        };
    }

    private GoogleGenAiChatOptions buildGoogleOptions(
            String userId,
            String model,
            double temperature,
            ToolOptions toolOptions,
            String responseSchema,
            String responseMimeType
    ) {
        GoogleGenAiChatOptions.Builder builder = GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .internalToolExecutionEnabled(false)
                .labels(Map.of(USER_ID_CONTEXT_KEY, userId));

        if (responseSchema != null) {
            builder.responseSchema(responseSchema);
        }

        if (responseMimeType != null) {
            builder.responseMimeType(responseMimeType);
        }

        if (!toolOptions.allowedToolNames().isEmpty()) {
            builder.toolNames(toolOptions.allowedToolNames());
            builder.toolCallbacks(toolOptions.toolCallbacks());
            builder.toolContext(Map.of(USER_ID_CONTEXT_KEY, userId));
        }

        return builder.build();
    }

    private OpenAiChatOptions buildOpenAiOptions(
            String userId,
            String model,
            double temperature,
            ToolOptions toolOptions,
            String responseSchema
    ) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .internalToolExecutionEnabled(false)
                .user(userId);

        if (responseSchema != null) {
            builder.outputSchema(responseSchema);
        }

        if (!toolOptions.allowedToolNames().isEmpty()) {
            builder.toolNames(toolOptions.allowedToolNames());
            builder.toolCallbacks(toolOptions.toolCallbacks());
            builder.toolContext(Map.of(USER_ID_CONTEXT_KEY, userId));
        }

        return builder.build();
    }

    private ToolOptions resolveToolOptions(String userId, AiToolScope toolScope) {
        if (toolScope == null || toolScope.allowedToolNames() == null || toolScope.allowedToolNames().isEmpty()) {
            return new ToolOptions(Set.of(), List.of());
        }

        List<AiTool> tools = toolScopeResolver.resolveTools(userId, toolScope.allowedToolNames());
        Set<String> allowedToolNames = new LinkedHashSet<>();
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (AiTool tool : tools) {
            allowedToolNames.add(tool.name());
            toolCallbacks.add(FunctionToolCallback
                    .builder(tool.name(), (Map<String, Object> arguments) -> tool.execute(
                            new AiToolExecutionRequest(userId, null, arguments)
                    ))
                    .description(tool.description())
                    .inputType(tool.inputType())
                    .build());
        }

        return new ToolOptions(allowedToolNames, toolCallbacks);
    }

    private record ToolOptions(
            Set<String> allowedToolNames,
            List<ToolCallback> toolCallbacks
    ) {
    }
}
