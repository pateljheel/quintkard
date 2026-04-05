package io.quintkard.quintkardapp.orchestratorexecution;

public record FilteringDecision(
        boolean accepted,
        String reason
) {
}
