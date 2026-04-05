package io.quintkard.quintkardapp.messagepipeline;

import io.quintkard.quintkardapp.aimodel.AiMemoryScope;
import java.util.UUID;

public record MessageProcessingContext(
        UUID messageId,
        String userId,
        String runId,
        AiMemoryScope memoryScope
) {
}
