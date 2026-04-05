package io.quintkard.quintkardapp.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.agent.AgentConfigRepository;
import io.quintkard.quintkardapp.user.User;
import io.quintkard.quintkardapp.user.UserRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class OrchestratorServiceImplTest {

    private AgentConfigRepository agentRepository;
    private OrchestratorConfigRepository orchestratorConfigRepository;
    private UserRepository userRepository;
    private OrchestratorServiceImpl service;

    @BeforeEach
    void setUp() {
        agentRepository = mock(AgentConfigRepository.class);
        orchestratorConfigRepository = mock(OrchestratorConfigRepository.class);
        userRepository = mock(UserRepository.class);
        service = new OrchestratorServiceImpl(agentRepository, orchestratorConfigRepository, userRepository);
    }

    @Test
    void getConfigReturnsExistingConfig() {
        OrchestratorConfig config = config("admin", Set.of(agent("admin", UUID.randomUUID(), "Finance")));
        when(orchestratorConfigRepository.findByUser_UserId("admin")).thenReturn(Optional.of(config));

        OrchestratorConfig result = service.getConfig("admin");

        assertSame(config, result);
    }

    @Test
    void getConfigReturnsDefaultConfigWhenMissing() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        when(orchestratorConfigRepository.findByUser_UserId("admin")).thenReturn(Optional.empty());
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));

        OrchestratorConfig result = service.getConfig("admin");

        assertSame(user, result.getUser());
        assertEquals("", result.getFilteringPrompt());
        assertEquals("", result.getFilteringModel());
        assertEquals("", result.getRoutingPrompt());
        assertEquals("", result.getRoutingModel());
        assertEquals(Set.of(), result.getActiveAgents());
    }

    @Test
    void getConfigFailsWhenUserMissingForDefaultConfig() {
        when(orchestratorConfigRepository.findByUser_UserId("admin")).thenReturn(Optional.empty());
        when(userRepository.findByUserId("admin")).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.getConfig("admin")
        );

        assertEquals("User not found: admin", exception.getMessage());
    }

    @Test
    void updateConfigTrimsFieldsResolvesAgentsAndSavesNewConfig() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        UUID agentId = UUID.randomUUID();
        AgentConfig finance = agent("admin", agentId, "Finance");
        when(orchestratorConfigRepository.findByUser_UserId("admin")).thenReturn(Optional.empty());
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));
        when(agentRepository.findByIdAndUser_UserId(agentId, "admin")).thenReturn(Optional.of(finance));
        when(orchestratorConfigRepository.save(any(OrchestratorConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrchestratorConfig saved = service.updateConfig("admin", new OrchestratorConfigRequest(
                "  Filter incoming messages  ",
                "  gemini-2.5-flash  ",
                "  Route to the best agents  ",
                "  gpt-5.4-mini  ",
                List.of(agentId, agentId)
        ));

        ArgumentCaptor<OrchestratorConfig> captor = ArgumentCaptor.forClass(OrchestratorConfig.class);
        verify(orchestratorConfigRepository).save(captor.capture());
        OrchestratorConfig persisted = captor.getValue();
        assertSame(saved, persisted);
        assertEquals("Filter incoming messages", persisted.getFilteringPrompt());
        assertEquals("gemini-2.5-flash", persisted.getFilteringModel());
        assertEquals("Route to the best agents", persisted.getRoutingPrompt());
        assertEquals("gpt-5.4-mini", persisted.getRoutingModel());
        assertEquals(Set.of(finance), persisted.getActiveAgents());
    }

    @Test
    void updateConfigUpdatesExistingConfig() {
        UUID agentId = UUID.randomUUID();
        OrchestratorConfig existing = config("admin", Set.of());
        AgentConfig finance = agent("admin", agentId, "Finance");
        when(orchestratorConfigRepository.findByUser_UserId("admin")).thenReturn(Optional.of(existing));
        when(agentRepository.findByIdAndUser_UserId(agentId, "admin")).thenReturn(Optional.of(finance));
        when(orchestratorConfigRepository.save(existing)).thenReturn(existing);

        OrchestratorConfig updated = service.updateConfig("admin", new OrchestratorConfigRequest(
                null,
                null,
                "Route",
                "gemini-2.5-flash",
                List.of(agentId)
        ));

        assertSame(existing, updated);
        assertEquals("", updated.getFilteringPrompt());
        assertEquals("", updated.getFilteringModel());
        assertEquals("Route", updated.getRoutingPrompt());
        assertEquals("gemini-2.5-flash", updated.getRoutingModel());
        assertEquals(Set.of(finance), updated.getActiveAgents());
    }

    @Test
    void updateConfigRejectsMissingRoutingPrompt() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateConfig("admin", new OrchestratorConfigRequest(
                        null,
                        null,
                        "   ",
                        "gemini-2.5-flash",
                        List.of()
                ))
        );

        assertEquals("Routing prompt is required", exception.getMessage());
        verifyNoInteractions(agentRepository, orchestratorConfigRepository, userRepository);
    }

    @Test
    void updateConfigRejectsMissingRoutingModel() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateConfig("admin", new OrchestratorConfigRequest(
                        null,
                        null,
                        "Route",
                        "   ",
                        List.of()
                ))
        );

        assertEquals("Routing model is required", exception.getMessage());
    }

    @Test
    void updateConfigRejectsMissingActiveAgentIds() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateConfig("admin", new OrchestratorConfigRequest(
                        null,
                        null,
                        "Route",
                        "gemini-2.5-flash",
                        null
                ))
        );

        assertEquals("Active agent IDs are required", exception.getMessage());
    }

    @Test
    void updateConfigRejectsFilteringPromptWithoutFilteringModel() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateConfig("admin", new OrchestratorConfigRequest(
                        "Filter",
                        "   ",
                        "Route",
                        "gemini-2.5-flash",
                        List.of()
                ))
        );

        assertEquals("Filtering model is required when filtering prompt is provided", exception.getMessage());
    }

    @Test
    void updateConfigFailsWhenActiveAgentDoesNotBelongToUser() {
        UUID agentId = UUID.randomUUID();
        when(orchestratorConfigRepository.findByUser_UserId("admin")).thenReturn(Optional.of(config("admin", Set.of())));
        when(agentRepository.findByIdAndUser_UserId(agentId, "admin")).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> service.updateConfig("admin", new OrchestratorConfigRequest(
                        null,
                        null,
                        "Route",
                        "gemini-2.5-flash",
                        List.of(agentId)
                ))
        );

        assertEquals("Agent not found for user admin: " + agentId, exception.getMessage());
    }

    private OrchestratorConfig config(String userId, Set<AgentConfig> activeAgents) {
        OrchestratorConfig config = new OrchestratorConfig(
                new User(userId, "Admin", "admin@example.com", "hash", false),
                "",
                "",
                "Existing route",
                "gemini-2.5-flash",
                activeAgents
        );
        ReflectionTestUtils.setField(config, "id", UUID.randomUUID());
        return config;
    }

    private AgentConfig agent(String userId, UUID id, String name) {
        AgentConfig agent = new AgentConfig(
                new User(userId, "Admin", "admin@example.com", "hash", false),
                name,
                "Description",
                "Prompt",
                "gemini-2.5-flash",
                0.7
        );
        ReflectionTestUtils.setField(agent, "id", id);
        return agent;
    }
}
