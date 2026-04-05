package io.quintkard.quintkardapp.messagepipeline;

import io.quintkard.quintkardapp.message.Message;

public interface MessageProcessor {

    void process(Message message);
}
