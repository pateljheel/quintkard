package io.quintkard.quintkardapp.messagepipeline;

import io.quintkard.quintkardapp.aimodel.AiMemoryScope;
import io.quintkard.quintkardapp.message.Message;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefaultMessageProcessingContextFactory implements MessageProcessingContextFactory {

    @Override
    public MessageProcessingContext createContext(Message message) {
        String runId = UUID.randomUUID().toString();
        return new MessageProcessingContext(
                message.getId(),
                message.getUser().getUserId(),
                runId,
                new AiMemoryScope("msg:%s:run:%s".formatted(message.getId(), runId))
        );
    }
}
