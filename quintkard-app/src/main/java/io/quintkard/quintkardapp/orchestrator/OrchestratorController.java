package io.quintkard.quintkardapp.orchestrator;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orchestrator")
public class OrchestratorController {

    private final OrchestratorService orchestratorService;

    public OrchestratorController(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @GetMapping
    public OrchestratorConfigResponse getConfig(Authentication authentication) {
        return OrchestratorConfigResponse.from(
            orchestratorService.getConfig(authentication.getName())
        );
    }

    @PutMapping
    public OrchestratorConfigResponse updateConfig(
            Authentication authentication,
            @RequestBody OrchestratorConfigRequest request
    ) {
        return OrchestratorConfigResponse.from(
            orchestratorService.updateConfig(authentication.getName(), request)
        );
    }
}
