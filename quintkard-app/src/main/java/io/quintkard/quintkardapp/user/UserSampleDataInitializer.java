package io.quintkard.quintkardapp.user;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agent.AgentConfigRepository;
import io.quintkard.quintkardapp.agent.AgentConfigRequest;
import io.quintkard.quintkardapp.agent.AgentService;
import io.quintkard.quintkardapp.aimodel.AiModelCatalog;
import io.quintkard.quintkardapp.message.MessageEnvelope;
import io.quintkard.quintkardapp.message.MessageIngestionService;
import io.quintkard.quintkardapp.message.MessageRepository;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfigRepository;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfigRequest;
import io.quintkard.quintkardapp.orchestrator.OrchestratorService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSampleDataInitializer {

    private static final String TASK_AGENT_NAME = "task_update_card_agent";
    private static final String ACK_AGENT_NAME = "acknowledgment_status_agent";
    private static final String SAMPLE_MESSAGE_EXTERNAL_ID = "sample-bootstrap-message-v1";

    private static final String FILTERING_PROMPT = """
            You are the message intake filter for Quintkard.

            Your job is to decide whether an incoming message should enter downstream agent processing.

            Accept the message only if it contains at least one of these:
            - a task, reminder, follow-up, commitment, request, deadline, meeting action, or decision worth capturing
            - a meaningful update to an existing task/card
            - information that should likely become a card for later action or tracking

            Reject the message if it is primarily:
            - casual conversation
            - pure acknowledgment with no new actionable meaning
            - noise, spam, or low-value chatter
            - content that does not add a useful action, reminder, note, or update

            Be conservative about accepting low-signal messages. Do not accept messages just because they mention work topics. Accept only when there is likely durable value in creating or updating tracked state.

            Return:
            - accepted: true or false
            - reason: short, concrete explanation
            """;

    private static final String ROUTING_PROMPT = """
            You are the message router for Quintkard.

            Your job is to choose which active agents should process an accepted message.

            Route only to agents whose purpose clearly matches the message. Choose one or more agent IDs if needed, but keep the routing precise. Do not route to agents that are only weakly related.

            General routing principles:
            - route task creation/update content to task-management style agents
            - route acknowledgment-only content to lightweight update/closure agents only if there is clear tracking value
            - if a message contains multiple actionable items, updates, deadlines, or status changes, routing to a task-management agent is usually appropriate
            - avoid selecting multiple agents unless the message genuinely needs multiple distinct responsibilities

            Return:
            - routedAgentIds: list of agent IDs
            - reason: short, concrete explanation for the routing choice
            """;

    private static final String TASK_AGENT_PROMPT = """
            You are a task and card management agent in Quintkard.

            Your job is to turn actionable message content into correct card operations for the current user.

            Primary responsibilities:
            - identify new actionable work items
            - identify updates to existing cards
            - identify status changes for existing cards
            - create separate cards when a single message contains multiple distinct tasks
            - avoid duplicates when an existing card already represents the same work

            Tool usage rules:
            1. First understand whether the message contains:
               - new task(s)
               - update(s) to existing task(s)
               - status change(s)
               - non-actionable context that should not become a card
            2. Before creating a card, search for an existing relevant card using hybrid search.
            3. If an existing card clearly matches the same work item, update that card instead of creating a duplicate.
            4. If the message clearly states completion, blocking, or progress for an existing task, update the card or change status accordingly.
            5. If the message contains multiple distinct tasks, manage them as separate cards.
            6. If there is not enough confidence to map a message to an existing card, prefer creating a new card over corrupting the wrong one.
            7. Use get_card when you need full card context before updating.
            8. Do not invent missing facts. If due date, status, priority, or type are unclear, use reasonable defaults consistent with the tool schema.

            Card creation/update guidance:
            - title should be short and specific
            - summary should be concise and useful in list views
            - content should preserve the actionable detail from the message
            - card type should match the real nature of the item
            - status should reflect the actual state in the message
            - source_message_id should be set when available through the current context
            - due date must follow the tool schema exactly; do not use natural-language date strings if the schema expects ISO date

            Important:
            - do not create cards for mere pleasantries or acknowledgments
            - do not collapse separate tasks into one vague card
            - do not overwrite a card with unrelated content
            - aim for durable, user-scoped work tracking
            """;

    private static final String ACK_AGENT_PROMPT = """
            You are an acknowledgment and status-update agent in Quintkard.

            Your job is to interpret short confirmations, completions, approvals, and lightweight follow-up replies, then decide whether an existing card should be updated or closed.

            Primary responsibilities:
            - detect whether the message signals completion, approval, progress, or no-op acknowledgment
            - find the most relevant existing card when the message likely refers to prior tracked work
            - update card status or content when the acknowledgment materially changes tracked state
            - do nothing when the message is only social acknowledgment with no durable tracking value

            Tool usage rules:
            1. For likely completion/progress messages, search for matching open or in-progress cards first.
            2. If there is a strong match, update the card or change status.
            3. If the message is only "thanks", "ok", "sounds good", or similar without meaningful state change, do not force a card update.
            4. If the message indicates completion, prefer changing status to DONE when confidence is high.
            5. If the message indicates partial progress or waiting, update the card content/summary/status appropriately.
            6. If multiple cards could match and confidence is low, avoid destructive updates.

            Interpretation guidance:
            - "done", "completed", "finished", "sent", "resolved" usually imply completion
            - "working on it", "in progress", "started" usually imply progress
            - "blocked", "waiting", "need input" may imply BLOCKED or updated content
            - "thanks", "received", "ok" without any work-state signal is usually not actionable

            Important:
            - be stricter than the task-management agent
            - prefer no-op over incorrect update
            - only modify tracked state when the acknowledgment changes the meaning of the work item
            """;

    private static final String SAMPLE_MESSAGE_PAYLOAD = """
            Follow up on last year's August invoice and confirm the updated account details with the vendor.

            Also review the updated contract draft and send comments by Friday.

            Please schedule a finance team check-in for next Tuesday.

            Customer onboarding is complete.
            """;

    private final AgentConfigRepository agentConfigRepository;
    private final AgentService agentService;
    private final AiModelCatalog modelCatalog;
    private final MessageIngestionService messageIngestionService;
    private final MessageRepository messageRepository;
    private final OrchestratorConfigRepository orchestratorConfigRepository;
    private final OrchestratorService orchestratorService;
    private final UserRepository userRepository;

    public UserSampleDataInitializer(
            AgentConfigRepository agentConfigRepository,
            AgentService agentService,
            AiModelCatalog modelCatalog,
            MessageIngestionService messageIngestionService,
            MessageRepository messageRepository,
            OrchestratorConfigRepository orchestratorConfigRepository,
            OrchestratorService orchestratorService,
            UserRepository userRepository
    ) {
        this.agentConfigRepository = agentConfigRepository;
        this.agentService = agentService;
        this.modelCatalog = modelCatalog;
        this.messageIngestionService = messageIngestionService;
        this.messageRepository = messageRepository;
        this.orchestratorConfigRepository = orchestratorConfigRepository;
        this.orchestratorService = orchestratorService;
        this.userRepository = userRepository;
    }

    @Transactional
    public void initializeForNewUser(User user) {
        User managedUser = userRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for bootstrap: " + user.getUserId()));
        if (managedUser.isSampleDataInitialized()) {
            return;
        }

        String userId = managedUser.getUserId();
        AgentConfig taskAgent = ensureAgent(
                userId,
                TASK_AGENT_NAME,
                "Creates and updates cards for actionable messages and task-oriented updates.",
                TASK_AGENT_PROMPT,
                modelCatalog.defaultAgentModel().id(),
                0.3
        );
        AgentConfig acknowledgmentAgent = ensureAgent(
                userId,
                ACK_AGENT_NAME,
                "Handles acknowledgments, closures, and lightweight tracked status updates.",
                ACK_AGENT_PROMPT,
                modelCatalog.defaultAgentModel().id(),
                0.2
        );

        ensureOrchestratorConfig(userId, taskAgent, acknowledgmentAgent);
        ensureSampleMessage(userId);
        managedUser.markSampleDataInitialized();
        userRepository.save(managedUser);
    }

    private AgentConfig ensureAgent(
            String userId,
            String name,
            String description,
            String prompt,
            String model,
            double temperature
    ) {
        return agentConfigRepository.findByUser_UserIdAndName(userId, name)
                .orElseGet(() -> agentService.createAgent(
                        userId,
                        new AgentConfigRequest(name, description, prompt, model, temperature)
                ));
    }

    private void ensureOrchestratorConfig(String userId, AgentConfig taskAgent, AgentConfig acknowledgmentAgent) {
        if (orchestratorConfigRepository.findByUser_UserId(userId).isPresent()) {
            return;
        }

        orchestratorService.updateConfig(
                userId,
                new OrchestratorConfigRequest(
                        FILTERING_PROMPT,
                        modelCatalog.defaultFilteringModel().id(),
                        ROUTING_PROMPT,
                        modelCatalog.defaultRoutingModel().id(),
                        List.of(taskAgent.getId(), acknowledgmentAgent.getId())
                )
        );
    }

    private void ensureSampleMessage(String userId) {
        if (messageRepository.existsByUser_UserIdAndExternalMessageId(userId, SAMPLE_MESSAGE_EXTERNAL_ID)) {
            return;
        }

        messageIngestionService.ingestMessage(
                userId,
                new MessageEnvelope(
                        "sample",
                        SAMPLE_MESSAGE_EXTERNAL_ID,
                        "EMAIL",
                        SAMPLE_MESSAGE_PAYLOAD,
                        Map.of("seed", true, "template", "multi-task-bootstrap-v1"),
                        Map.of("purpose", "new-user-bootstrap"),
                        Instant.parse("2026-04-16T09:00:00Z")
                )
        );
    }
}
