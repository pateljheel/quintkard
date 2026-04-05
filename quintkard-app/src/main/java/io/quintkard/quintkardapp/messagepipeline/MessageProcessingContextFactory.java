package io.quintkard.quintkardapp.messagepipeline;

import io.quintkard.quintkardapp.message.Message;

public interface MessageProcessingContextFactory {

    MessageProcessingContext createContext(Message message);
}
