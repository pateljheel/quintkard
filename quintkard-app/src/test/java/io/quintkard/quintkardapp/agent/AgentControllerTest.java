package io.quintkard.quintkardapp.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.quintkard.quintkardapp.aimodel.AiModelCatalog;
import io.quintkard.quintkardapp.user.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AgentControllerTest {

    private AgentService agentService;
    private AgentController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        agentService = mock(AgentService.class);
        controller = new AgentController(agentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createAgentUsesAuthenticatedUser() throws Exception {
        AgentConfig agent = agent("admin", UUID.randomUUID(), "Finance Agent");
        when(agentService.createAgent(eq("admin"), any(AgentConfigRequest.class))).thenReturn(agent);

        mockMvc.perform(authorized(post("/api/agents"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Finance Agent",
                                  "description": "Handles finance",
                                  "prompt": "Use tools carefully",
                                  "model": "gemini-2.5-flash",
                                  "temperature": 0.7
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(agent.getId().toString()))
                .andExpect(jsonPath("$.userId").value("admin"))
                .andExpect(jsonPath("$.name").value("Finance Agent"))
                .andExpect(jsonPath("$.model").value("gemini-2.5-flash"));

        verify(agentService).createAgent(eq("admin"), any(AgentConfigRequest.class));
    }

    @Test
    void listAgentsReturnsAuthenticatedUsersAgents() throws Exception {
        when(agentService.listAgents("admin")).thenReturn(List.of(
                agent("admin", UUID.randomUUID(), "Finance Agent"),
                agent("admin", UUID.randomUUID(), "Ops Agent")
        ));

        mockMvc.perform(authorized(get("/api/agents")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("admin"))
                .andExpect(jsonPath("$[0].name").value("Finance Agent"))
                .andExpect(jsonPath("$[1].name").value("Ops Agent"));

        verify(agentService).listAgents("admin");
    }

    @Test
    void getAgentReturnsAuthenticatedUsersAgent() throws Exception {
        AgentConfig agent = agent("admin", UUID.randomUUID(), "Finance Agent");
        when(agentService.getAgent("admin", agent.getId())).thenReturn(agent);

        mockMvc.perform(authorized(get("/api/agents/{agentId}", agent.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(agent.getId().toString()))
                .andExpect(jsonPath("$.prompt").value("Prompt"))
                .andExpect(jsonPath("$.temperature").value(0.7));

        verify(agentService).getAgent("admin", agent.getId());
    }

    @Test
    void updateAgentUsesAuthenticatedUser() throws Exception {
        AgentConfig agent = agent("admin", UUID.randomUUID(), "Updated Agent");
        when(agentService.updateAgent(eq("admin"), eq(agent.getId()), any(AgentConfigRequest.class)))
                .thenReturn(agent);

        mockMvc.perform(authorized(put("/api/agents/{agentId}", agent.getId()))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Agent",
                                  "description": "Handles finance",
                                  "prompt": "Use tools carefully",
                                  "model": "gemini-2.5-flash",
                                  "temperature": 0.7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Agent"))
                .andExpect(jsonPath("$.description").value("Description"));

        verify(agentService).updateAgent(eq("admin"), eq(agent.getId()), any(AgentConfigRequest.class));
    }

    @Test
    void deleteAgentUsesAuthenticatedUser() throws Exception {
        UUID agentId = UUID.randomUUID();

        mockMvc.perform(authorized(delete("/api/agents/{agentId}", agentId)))
                .andExpect(status().isNoContent());

        verify(agentService).deleteAgent("admin", agentId);
    }

    @Test
    void getAgentConfigReturnsSharedCatalogMetadata() {
        AiModelCatalog catalog = new AiModelCatalog();

        AgentConfigMetadataResponse response = controller.getAgentConfig(catalog);

        assertEquals("gemini-2.5-flash", response.defaultAgentModelId());
        assertEquals("gemini-2.5-flash", response.defaultRoutingModelId());
        assertEquals("gemini-2.5-flash", response.defaultFilteringModelId());
        assertEquals("gemini-2.5-flash", response.models().getFirst().id());
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

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder) {
        return builder.principal(new TestingAuthenticationToken("admin", "password"));
    }
}
