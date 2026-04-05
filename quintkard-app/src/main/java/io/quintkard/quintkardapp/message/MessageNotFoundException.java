package io.quintkard.quintkardapp.message;

import java.util.UUID;

public class MessageNotFoundException extends RuntimeException {

    public MessageNotFoundException(UUID messageId) {
        super("Message not found: " + messageId);
    }
}
