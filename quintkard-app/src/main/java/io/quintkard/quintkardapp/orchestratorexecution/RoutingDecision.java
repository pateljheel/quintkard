package io.quintkard.quintkardapp.orchestratorexecution;

import java.util.List;
import java.util.UUID;

public record RoutingDecision(
        List<UUID> agentIds,
        String reason
) {
}
