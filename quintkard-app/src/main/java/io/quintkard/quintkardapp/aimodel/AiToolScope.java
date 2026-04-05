package io.quintkard.quintkardapp.aimodel;

import java.util.Set;

public record AiToolScope(
        Set<String> allowedToolNames
) {
}
