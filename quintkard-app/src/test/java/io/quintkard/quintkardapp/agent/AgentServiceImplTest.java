package io.quintkard.quintkardapp.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quintkard.quintkardapp.user.User;
import io.quintkard.quintkardapp.user.UserRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AgentServiceImplTest {

    private AgentConfigRepository agentConfigRepository;
    private UserRepository userRepository;
    private AgentServiceImpl agentService;

    @BeforeEach
    void setUp() {
        agentConfigRepository = mock(AgentConfigRepository.class);
        userRepository = mock(UserRepository.class);
        agentService = new AgentServiceImpl(agentConfigRepository, new AgentModelCatalog(), userRepository);
    }

    @Test
    void createAgentTrimsFieldsAndSavesWhenValid() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        UUID agentId = UUID.randomUUID();
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));
        when(agentConfigRepository.countByUser_UserId("admin")).thenReturn(0L);
        when(agentConfigRepository.findByUser_UserIdAndName("admin", "Finance Agent")).thenReturn(Optional.empty());
        when(agentConfigRepository.save(any(AgentConfig.class))).thenAnswer(invocation -> {
            AgentConfig agent = invocation.getArgument(0);
            ReflectionTestUtils.setField(agent, "id", agentId);
            return agent;
        });

        AgentConfig saved = agentService.createAgent("admin", new AgentConfigRequest(
                "  Finance Agent  ",
                "  Handles finance workflows  ",
                "  Use tools carefully  ",
                "  gemini-2.5-flash  ",
                0.7
        ));

        ArgumentCaptor<AgentConfig> captor = ArgumentCaptor.forClass(AgentConfig.class);
        verify(agentConfigRepository).save(captor.capture());
        AgentConfig persisted = captor.getValue();
        assertEquals("Finance Agent", persisted.getName());
        assertEquals("Handles finance workflows", persisted.getDescription());
        assertEquals("Use tools carefully", persisted.getPrompt());
        assertEquals("gemini-2.5-flash", persisted.getModel());
        assertSame(user, persisted.getUser());
        assertEquals(agentId, saved.getId());
    }

    @Test
    void createAgentRejectsMissingName() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> agentService.createAgent("admin", new AgentConfigRequest(
                        "   ",
                        "Description",
                        "Prompt",
                        "gemini-2.5-flash",
                        0.7
                ))
        );

        assertEquals("Agent name is required", exception.getMessage());
        verifyNoInteractions(userRepository, agentConfigRepository);
    }

    @Test
    void createAgentRejectsMissingDescription() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> agentService.createAgent("admin", new AgentConfigRequest(
                        "Name",
                        "   ",
                        "Prompt",
                        "gemini-2.5-flash",
                        0.7
                ))
        );

        assertEquals("Agent description is required", exception.getMessage());
    }

    @Test
    void createAgentRejectsMissingPrompt() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> agentService.createAgent("admin", new AgentConfigRequest(
                        "Name",
                        "Description",
                        "   ",
                        "gemini-2.5-flash",
                        0.7
                ))
        );

        assertEquals("Agent prompt is required", exception.getMessage());
    }

    @Test
    void createAgentRejectsMissingModel() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> agentService.createAgent("admin", new AgentConfigRequest(
                        "Name",
                        "Description",
                        "Prompt",
                        "   ",
                        0.7
                ))
        );

        assertEquals("Agent model is required", exception.getMessage());
    }

    @Test
    void createAgentRejectsUnsupportedModel() {
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> agentService.createAgent("admin", new AgentConfigRequest(
                        "Name",
                        "Description",
                        "Prompt",
                        "unknown-model",
                        0.7
                ))
        );

        assertEquals("Unsupported agent model: unknown-model", exception.getMessage());
    }

    @Test
    void createAgentRejectsTemperatureOutsideModelRange() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> agentService.createAgent("admin", new AgentConfigRequest(
                        "Name",
                        "Description",
                        "Prompt",
                        "gpt-5.1-codex-mini",
                        1.5
                ))
        );

        assertEquals(
                "Temperature 1.50 is invalid for model gpt-5.1-codex-mini. Supported range: 0.0 to 1.2",
                exception.getMessage()
        );
    }

    @Test
    void createAgentRejectsWhenUserMissing() {
        when(userRepository.findByUserId("admin")).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> agentService.createAgent("admin", new AgentConfigRequest(
                        "Name",
                        "Description",
                        "Prompt",
                        "gemini-2.5-flash",
                        0.7
                ))
        );

        assertEquals("User not found: admin", exception.getMessage());
    }

    @Test
    void createAgentRejectsWhenLimitReached() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));
        when(agentConfigRepository.countByUser_UserId("admin")).thenReturn(30L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> agentService.createAgent("admin", new AgentConfigRequest(
                        "Name",
                        "Description",
                        "Prompt",
                        "gemini-2.5-flash",
                        0.7
                ))
        );

        assertEquals("Agent limit reached. A user can only have 30 agents.", exception.getMessage());
        verify(agentConfigRepository, never()).save(any());
    }

    @Test
    void createAgentRejectsDuplicateName() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        when(userRepository.findByUserId("admin")).thenReturn(Optional.of(user));
        when(agentConfigRepository.countByUser_UserId("admin")).thenReturn(0L);
        when(agentConfigRepository.findByUser_UserIdAndName("admin", "Finance Agent"))
                .thenReturn(Optional.of(agent("admin", UUID.randomUUID(), "Finance Agent")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> agentService.createAgent("admin", new AgentConfigRequest(
                        "Finance Agent",
                        "Description",
                        "Prompt",
                        "gemini-2.5-flash",
                        0.7
                ))
        );

        assertEquals("Agent name already exists: Finance Agent", exception.getMessage());
    }

    @Test
    void updateAgentTrimsFieldsAndKeepsSameEntity() {
        UUID agentId = UUID.randomUUID();
        AgentConfig agent = agent("admin", agentId, "Finance Agent");
        when(agentConfigRepository.findByIdAndUser_UserId(agentId, "admin")).thenReturn(Optional.of(agent));
        when(agentConfigRepository.findByUser_UserIdAndName("admin", "Operations Agent")).thenReturn(Optional.empty());

        AgentConfig updated = agentService.updateAgent("admin", agentId, new AgentConfigRequest(
                "  Operations Agent  ",
                "  Handles ops  ",
                "  Updated prompt  ",
                "  gpt-5.4-mini  ",
                0.5
        ));

        assertSame(agent, updated);
        assertEquals("Operations Agent", updated.getName());
        assertEquals("Handles ops", updated.getDescription());
        assertEquals("Updated prompt", updated.getPrompt());
        assertEquals("gpt-5.4-mini", updated.getModel());
        assertEquals(0.5, updated.getTemperature());
    }

    @Test
    void updateAgentAllowsSameNameForSameAgent() {
        UUID agentId = UUID.randomUUID();
        AgentConfig agent = agent("admin", agentId, "Finance Agent");
        when(agentConfigRepository.findByIdAndUser_UserId(agentId, "admin")).thenReturn(Optional.of(agent));
        when(agentConfigRepository.findByUser_UserIdAndName("admin", "Finance Agent")).thenReturn(Optional.of(agent));

        AgentConfig updated = agentService.updateAgent("admin", agentId, new AgentConfigRequest(
                "Finance Agent",
                "Description",
                "Prompt",
                "gemini-2.5-flash",
                0.7
        ));

        assertSame(agent, updated);
    }

    @Test
    void updateAgentRejectsDuplicateNameFromDifferentAgent() {
        UUID agentId = UUID.randomUUID();
        AgentConfig agent = agent("admin", agentId, "Finance Agent");
        AgentConfig existing = agent("admin", UUID.randomUUID(), "Operations Agent");
        when(agentConfigRepository.findByIdAndUser_UserId(agentId, "admin")).thenReturn(Optional.of(agent));
        when(agentConfigRepository.findByUser_UserIdAndName("admin", "Operations Agent")).thenReturn(Optional.of(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> agentService.updateAgent("admin", agentId, new AgentConfigRequest(
                        "Operations Agent",
                        "Description",
                        "Prompt",
                        "gemini-2.5-flash",
                        0.7
                ))
        );

        assertEquals("Agent name already exists: Operations Agent", exception.getMessage());
    }

    @Test
    void getAgentThrowsWhenMissingForUser() {
        UUID agentId = UUID.randomUUID();
        when(agentConfigRepository.findByIdAndUser_UserId(agentId, "admin")).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> agentService.getAgent("admin", agentId)
        );

        assertEquals("Agent not found for user admin: " + agentId, exception.getMessage());
    }

    @Test
    void listAgentsReturnsUserScopedAgents() {
        List<AgentConfig> agents = List.of(
                agent("admin", UUID.randomUUID(), "Finance Agent"),
                agent("admin", UUID.randomUUID(), "Ops Agent")
        );
        when(agentConfigRepository.findAllByUser_UserId("admin")).thenReturn(agents);

        List<AgentConfig> result = agentService.listAgents("admin");

        assertSame(agents, result);
    }

    @Test
    void deleteAgentDeletesResolvedEntity() {
        UUID agentId = UUID.randomUUID();
        AgentConfig agent = agent("admin", agentId, "Finance Agent");
        when(agentConfigRepository.findByIdAndUser_UserId(agentId, "admin")).thenReturn(Optional.of(agent));

        agentService.deleteAgent("admin", agentId);

        verify(agentConfigRepository).delete(agent);
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
