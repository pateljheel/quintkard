package io.quintkard.quintkardapp.agent;

import io.quintkard.quintkardapp.aimodel.AiModelCatalog;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentConfigResponse createAgent(
            Authentication authentication,
            @RequestBody AgentConfigRequest request
    ) {
        return AgentConfigResponse.from(agentService.createAgent(authentication.getName(), request));
    }

    @GetMapping
    public List<AgentSummaryResponse> listAgents(Authentication authentication) {
        return agentService.listAgents(authentication.getName()).stream()
                .map(AgentSummaryResponse::from)
                .toList();
    }

    @GetMapping("/config")
    public AgentConfigMetadataResponse getAgentConfig(AiModelCatalog modelCatalog) {
        return new AgentConfigMetadataResponse(
                modelCatalog.listModels().stream()
                        .map(model -> new AgentModelConfigResponse(
                                model.id(),
                                model.label(),
                                model.minTemperature(),
                                model.maxTemperature(),
                                model.defaultTemperature()
                        ))
                        .toList(),
                modelCatalog.defaultAgentModel().id(),
                modelCatalog.defaultRoutingModel().id(),
                modelCatalog.defaultFilteringModel().id()
        );
    }

    @DeleteMapping("/{agentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAgent(
            Authentication authentication,
            @PathVariable UUID agentId
    ) {
        agentService.deleteAgent(authentication.getName(), agentId);
    }

    @GetMapping("/{agentId}")
    public AgentConfigResponse getAgent(
            Authentication authentication,
            @PathVariable UUID agentId
    ) {
        return AgentConfigResponse.from(agentService.getAgent(authentication.getName(), agentId));
    }

    @PutMapping("/{agentId}")
    public AgentConfigResponse updateAgent(
            Authentication authentication,
            @PathVariable UUID agentId,
            @RequestBody AgentConfigRequest request
    ) {
        return AgentConfigResponse.from(
                agentService.updateAgent(authentication.getName(), agentId, request)
        );
    }
}
