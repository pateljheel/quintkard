package io.quintkard.quintkardapp.agentexecution;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agenttool.AiTool;
import io.quintkard.quintkardapp.agenttool.AiToolExecutionRequest;
import io.quintkard.quintkardapp.agenttool.AiToolScopeResolver;
import io.quintkard.quintkardapp.aimodel.AiChatRequest;
import io.quintkard.quintkardapp.aimodel.AiChatResponse;
import io.quintkard.quintkardapp.aimodel.AiMessage;
import io.quintkard.quintkardapp.aimodel.AiMessageRole;
import io.quintkard.quintkardapp.aimodel.AiToolCall;
import io.quintkard.quintkardapp.aimodel.AiToolResult;
import io.quintkard.quintkardapp.aimodel.AiChatService;
import io.quintkard.quintkardapp.message.Message;
import io.quintkard.quintkardapp.messagepipeline.MessageProcessingContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class AgentLoopExecutorImpl implements AgentLoopExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AgentLoopExecutorImpl.class);

    private final AiChatService aiChatService;
    private final AiToolScopeResolver aiToolScopeResolver;
    private final ObjectMapper objectMapper;

    public AgentLoopExecutorImpl(
            AiChatService aiChatService,
            AiToolScopeResolver aiToolScopeResolver,
            ObjectMapper objectMapper
    ) {
        this.aiChatService = aiChatService;
        this.aiToolScopeResolver = aiToolScopeResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentExecutionResult execute(
            MessageProcessingContext context,
            AgentConfig agentConfig,
            AgentExecutionRequest request,
            AgentLoopPolicy loopPolicy
    ) {
        List<AiToolResult> toolResults = new ArrayList<>();
        Map<String, AiTool> allowedTools = resolveAllowedTools(context, request);
        List<AiMessage> turnMessages = buildInitialMessages(agentConfig, request.message());
        String lastResponseText = "";
        int toolCallCount = 0;

        for (int iteration = 1; iteration <= loopPolicy.maxIterations(); iteration++) {
            AiChatResponse response = aiChatService.chat(new AiChatRequest(
                    context.userId(),
                    agentConfig.getModel(),
                    agentConfig.getTemperature(),
                    turnMessages,
                    context.memoryScope(),
                    request.toolScope()
            ));

            lastResponseText = response.text();
            if (response.toolCalls().isEmpty() || response.finalResponse()) {
                return new AgentExecutionResult(
                        response.text(),
                        AgentExecutionStatus.SUCCESS,
                        iteration,
                        toolCallCount,
                        toolResults
                );
            }

            if (toolCallCount + response.toolCalls().size() > loopPolicy.maxToolCalls()) {
                return new AgentExecutionResult(
                        response.text(),
                        AgentExecutionStatus.MAX_TOOL_CALLS_REACHED,
                        iteration,
                        toolCallCount,
                        toolResults
                );
            }

            if (iteration == loopPolicy.maxIterations()) {
                return new AgentExecutionResult(
                        response.text(),
                        AgentExecutionStatus.MAX_ITERATIONS_REACHED,
                        iteration,
                        toolCallCount,
                        toolResults
                );
            }

            turnMessages = executeToolCalls(context, response.toolCalls(), allowedTools, toolResults);
            toolCallCount += response.toolCalls().size();
        }

        return new AgentExecutionResult(
                lastResponseText,
                AgentExecutionStatus.MAX_ITERATIONS_REACHED,
                loopPolicy.maxIterations(),
                toolCallCount,
                toolResults
        );
    }

    private Map<String, AiTool> resolveAllowedTools(MessageProcessingContext context, AgentExecutionRequest request) {
        Map<String, AiTool> allowedTools = new LinkedHashMap<>();
        if (request.toolScope() == null || request.toolScope().allowedToolNames() == null) {
            return allowedTools;
        }

        for (AiTool tool : aiToolScopeResolver.resolveTools(context.userId(), request.toolScope().allowedToolNames())) {
            allowedTools.put(tool.name(), tool);
        }
        return allowedTools;
    }

    private List<AiMessage> buildInitialMessages(AgentConfig agentConfig, Message message) {
        return List.of(
                new AiMessage(AiMessageRole.SYSTEM, agentConfig.getPrompt()),
                new AiMessage(AiMessageRole.USER, """
                        Message ID: %s
                        Source service: %s
                        Message type: %s
                        Summary: %s
                        Payload:
                        %s
                        """.formatted(
                        message.getId(),
                        message.getSourceService(),
                        message.getMessageType(),
                        nullToEmpty(message.getSummary()),
                        nullToEmpty(message.getPayload())
                ))
        );
    }

    private List<AiMessage> executeToolCalls(
            MessageProcessingContext context,
            List<AiToolCall> toolCalls,
            Map<String, AiTool> allowedTools,
            List<AiToolResult> toolResults
    ) {
        List<Map<String, Object>> toolResponseParts = new ArrayList<>();
        for (AiToolCall toolCall : toolCalls) {
            Object result = executeToolCall(context, toolCall, allowedTools);
            toolResults.add(new AiToolResult(toolCall.toolName(), result));
            toolResponseParts.add(Map.of(
                    "toolName", toolCall.toolName(),
                    "result", result
            ));
        }
        return List.of(new AiMessage(
                AiMessageRole.TOOL,
                serializeToolResponses(toolResponseParts)
        ));
    }

    private Object executeToolCall(
            MessageProcessingContext context,
            AiToolCall toolCall,
            Map<String, AiTool> allowedTools
    ) {
        logger.info(
                "Invoking tool {} for user {} message {} with arguments {}",
                toolCall.toolName(),
                context.userId(),
                context.messageId(),
                toLogJson(toolCall.arguments())
        );

        AiTool tool = allowedTools.get(toolCall.toolName());
        if (tool == null) {
            String error = "Tool not allowed: " + toolCall.toolName();
            logger.warn("{} for user {} message {}", error, context.userId(), context.messageId());
            return Map.of("error", error);
        }

        try {
            Object result = tool.execute(new AiToolExecutionRequest(
                    context.userId(),
                    context.memoryScope() == null ? null : context.memoryScope().conversationId(),
                    toolCall.arguments()
            ));
            logger.info(
                    "Tool {} completed for user {} message {} with result {}",
                    toolCall.toolName(),
                    context.userId(),
                    context.messageId(),
                    toLogJson(result)
            );
            return result;
        } catch (RuntimeException exception) {
            logger.warn(
                    "Tool {} failed for user {} message {} with arguments {}",
                    toolCall.toolName(),
                    context.userId(),
                    context.messageId(),
                    toLogJson(toolCall.arguments()),
                    exception
            );
            return Map.of(
                    "error", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()
            );
        }
    }

    private String serializeToolResponses(List<Map<String, Object>> toolResponses) {
        try {
            return objectMapper.writeValueAsString(toolResponses);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize tool responses", exception);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String toLogJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }
}
