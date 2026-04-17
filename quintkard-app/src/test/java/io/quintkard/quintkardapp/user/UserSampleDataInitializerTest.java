package io.quintkard.quintkardapp.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agent.AgentConfigRepository;
import io.quintkard.quintkardapp.agent.AgentConfigRequest;
import io.quintkard.quintkardapp.agent.AgentService;
import io.quintkard.quintkardapp.message.MessageEnvelope;
import io.quintkard.quintkardapp.message.MessageIngestionService;
import io.quintkard.quintkardapp.message.MessageRepository;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfig;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfigRepository;
import io.quintkard.quintkardapp.orchestrator.OrchestratorConfigRequest;
import io.quintkard.quintkardapp.orchestrator.OrchestratorService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class UserSampleDataInitializerTest {

    private AgentConfigRepository agentConfigRepository;
    private AgentService agentService;
    private MessageIngestionService messageIngestionService;
    private MessageRepository messageRepository;
    private OrchestratorConfigRepository orchestratorConfigRepository;
    private OrchestratorService orchestratorService;
    private UserRepository userRepository;
    private UserSampleDataInitializer initializer;

    @BeforeEach
    void setUp() {
        agentConfigRepository = mock(AgentConfigRepository.class);
        agentService = mock(AgentService.class);
        messageIngestionService = mock(MessageIngestionService.class);
        messageRepository = mock(MessageRepository.class);
        orchestratorConfigRepository = mock(OrchestratorConfigRepository.class);
        orchestratorService = mock(OrchestratorService.class);
        userRepository = mock(UserRepository.class);
        initializer = new UserSampleDataInitializer(
                agentConfigRepository,
                agentService,
                messageIngestionService,
                messageRepository,
                orchestratorConfigRepository,
                orchestratorService,
                userRepository
        );
    }

    @Test
    void initializeForNewUserCreatesMissingAgentsConfigAndSampleMessage() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));
        AgentConfig taskAgent = agent(user, UUID.randomUUID(), "task_update_card_agent");
        AgentConfig ackAgent = agent(user, UUID.randomUUID(), "acknowledgment_status_agent");

        when(agentConfigRepository.findByUser_UserIdAndName("admin", "task_update_card_agent"))
                .thenReturn(Optional.empty());
        when(agentConfigRepository.findByUser_UserIdAndName("admin", "acknowledgment_status_agent"))
                .thenReturn(Optional.empty());
        when(agentService.createAgent(eq("admin"), any(AgentConfigRequest.class)))
                .thenReturn(taskAgent, ackAgent);
        when(orchestratorConfigRepository.findByUser_UserId("admin")).thenReturn(Optional.empty());
        when(messageRepository.existsByUser_UserIdAndExternalMessageId("admin", "sample-bootstrap-message-v1"))
                .thenReturn(false);

        initializer.initializeForNewUser(user);

        ArgumentCaptor<AgentConfigRequest> agentCaptor = ArgumentCaptor.forClass(AgentConfigRequest.class);
        verify(agentService, org.mockito.Mockito.times(2)).createAgent(eq("admin"), agentCaptor.capture());
        assertEquals("task_update_card_agent", agentCaptor.getAllValues().get(0).name());
        assertEquals("acknowledgment_status_agent", agentCaptor.getAllValues().get(1).name());

        ArgumentCaptor<OrchestratorConfigRequest> orchestratorCaptor = ArgumentCaptor.forClass(OrchestratorConfigRequest.class);
        verify(orchestratorService).updateConfig(eq("admin"), orchestratorCaptor.capture());
        assertEquals(Set.of(taskAgent.getId(), ackAgent.getId()), Set.copyOf(orchestratorCaptor.getValue().activeAgentIds()));

        ArgumentCaptor<MessageEnvelope> envelopeCaptor = ArgumentCaptor.forClass(MessageEnvelope.class);
        verify(messageIngestionService).ingestMessage(eq("admin"), envelopeCaptor.capture());
        assertEquals("sample", envelopeCaptor.getValue().sourceService());
        assertEquals("sample-bootstrap-message-v1", envelopeCaptor.getValue().externalMessageId());
        verify(userRepository).save(user);
        assertEquals(true, user.isSampleDataInitialized());
    }

    @Test
    void initializeForNewUserSkipsWhenBootstrapAlreadyCompleted() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false, true);
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));

        initializer.initializeForNewUser(user);

        verify(agentService, never()).createAgent(any(), any());
        verify(orchestratorService, never()).updateConfig(any(), any());
        verify(messageIngestionService, never()).ingestMessage(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void initializeForNewUserDoesNotRecreateDeletedSamplesAfterBootstrapCompleted() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false, true);
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));

        initializer.initializeForNewUser(user);

        verify(agentService, never()).createAgent(any(), any());
        verify(orchestratorService, never()).updateConfig(any(), any());
        verify(messageIngestionService, never()).ingestMessage(any(), any());
    }

    @Test
    void initializeForNewUserSkipsExistingSampleDataBeforeMarkingBootstrapComplete() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));
        AgentConfig taskAgent = agent(user, UUID.randomUUID(), "task_update_card_agent");
        AgentConfig ackAgent = agent(user, UUID.randomUUID(), "acknowledgment_status_agent");

        when(agentConfigRepository.findByUser_UserIdAndName("admin", "task_update_card_agent"))
                .thenReturn(Optional.of(taskAgent));
        when(agentConfigRepository.findByUser_UserIdAndName("admin", "acknowledgment_status_agent"))
                .thenReturn(Optional.of(ackAgent));
        when(orchestratorConfigRepository.findByUser_UserId("admin"))
                .thenReturn(Optional.of(new OrchestratorConfig(user, "f", "m", "r", "m", Set.of(taskAgent, ackAgent))));
        when(messageRepository.existsByUser_UserIdAndExternalMessageId("admin", "sample-bootstrap-message-v1"))
                .thenReturn(true);

        initializer.initializeForNewUser(user);

        verify(agentService, never()).createAgent(any(), any());
        verify(orchestratorService, never()).updateConfig(any(), any());
        verify(messageIngestionService, never()).ingestMessage(any(), any());
        verify(userRepository, times(1)).save(user);
        assertEquals(true, user.isSampleDataInitialized());
    }

    private AgentConfig agent(User user, UUID id, String name) {
        AgentConfig agent = new AgentConfig(
                user,
                name,
                "Description",
                "Prompt",
                "gemini-2.5-flash",
                0.3
        );
        ReflectionTestUtils.setField(agent, "id", id);
        return agent;
    }
}
