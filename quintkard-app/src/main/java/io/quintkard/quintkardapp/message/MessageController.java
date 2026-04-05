package io.quintkard.quintkardapp.message;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public MessageSliceResponse listMessages(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MessageStatus status
    ) {
        Slice<MessageSummaryProjection> messages = messageService.listMessages(
                authentication.getName(),
                page,
                size,
                query,
                status
        );

        return MessageSliceResponse.from(messages);
    }

    @GetMapping("/{messageId}")
    public MessageResponse getMessage(
            Authentication authentication,
            @PathVariable UUID messageId
    ) {
        return MessageResponse.from(messageService.getMessage(authentication.getName(), messageId));
    }

    @PatchMapping("/{messageId}/status")
    public ResponseEntity<MessageResponse> updateMessageStatus(
            Authentication authentication,
            @PathVariable UUID messageId,
            @Valid @RequestBody MessageStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                MessageResponse.from(
                        messageService.updateMessageStatus(
                                authentication.getName(),
                                messageId,
                                request.status()
                        )
                )
        );
    }

    @DeleteMapping("/{messageId}")
    @ResponseStatus(NO_CONTENT)
    public void deleteMessage(
            Authentication authentication,
            @PathVariable UUID messageId
    ) {
        messageService.deleteMessage(authentication.getName(), messageId);
    }
}
