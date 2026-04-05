package io.quintkard.quintkardapp.orchestrator;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.quintkard.quintkardapp.agent.AgentConfig;
import io.quintkard.quintkardapp.user.User;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OrchestratorControllerTest {

    private OrchestratorService orchestratorService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orchestratorService = org.mockito.Mockito.mock(OrchestratorService.class);
        OrchestratorController controller = new OrchestratorController(orchestratorService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void getConfigReturnsAuthenticatedUsersConfig() throws Exception {
        OrchestratorConfig config = config();
        when(orchestratorService.getConfig("admin")).thenReturn(config);

        mockMvc.perform(authorized(get("/api/orchestrator")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("admin"))
                .andExpect(jsonPath("$.activeAgents[0].name").value("Finance Agent"));

        verify(orchestratorService).getConfig("admin");
    }

    @Test
    void updateConfigUsesAuthenticatedUser() throws Exception {
        OrchestratorConfig config = config();
        when(orchestratorService.updateConfig(org.mockito.ArgumentMatchers.eq("admin"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(config);

        mockMvc.perform(authorized(put("/api/orchestrator"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filteringPrompt": "Filter",
                                  "filteringModel": "gemini-2.5-flash",
                                  "routingPrompt": "Route",
                                  "routingModel": "gemini-2.5-flash",
                                  "activeAgentIds": ["%s"]
                                }
                                """.formatted(config.getActiveAgents().iterator().next().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routingModel").value("gemini-2.5-flash"))
                .andExpect(jsonPath("$.activeAgents[0].description").value("Handles finance"));

        verify(orchestratorService).updateConfig(org.mockito.ArgumentMatchers.eq("admin"), org.mockito.ArgumentMatchers.any());
    }

    private OrchestratorConfig config() {
        User user = new User("admin", "Admin", "admin@example.com", "hash", false);
        AgentConfig agent = new AgentConfig(
                user,
                "Finance Agent",
                "Handles finance",
                "Prompt",
                "gemini-2.5-flash",
                0.2
        );
        ReflectionTestUtils.setField(agent, "id", UUID.fromString("11111111-1111-1111-1111-111111111111"));
        OrchestratorConfig config = new OrchestratorConfig(
                user,
                "Filter",
                "gemini-2.5-flash",
                "Route",
                "gemini-2.5-flash",
                Set.of(agent)
        );
        ReflectionTestUtils.setField(config, "id", UUID.fromString("22222222-2222-2222-2222-222222222222"));
        return config;
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder) {
        return builder.principal(new TestingAuthenticationToken("admin", "password"));
    }
}
