package io.quintkard.quintkardapp.message;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageIngestionController {

    private final MessageIngestionService messageIngestionService;

    public MessageIngestionController(MessageIngestionService messageIngestionService) {
        this.messageIngestionService = messageIngestionService;
    }

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse ingestMessage(
            Authentication authentication,
            @RequestBody MessageEnvelope envelope
    ) {
        return MessageResponse.from(
            messageIngestionService.ingestMessage(authentication.getName(), envelope)
        );
    }

    @PostMapping("/ingest/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MessageResponse> ingestMessages(
            Authentication authentication,
            @RequestBody List<MessageEnvelope> envelopes
    ) {
        return messageIngestionService.ingestMessages(authentication.getName(), envelopes)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }
}
