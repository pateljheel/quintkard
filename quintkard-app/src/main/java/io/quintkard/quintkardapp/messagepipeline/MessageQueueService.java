package io.quintkard.quintkardapp.messagepipeline;

public interface MessageQueueService {

    void triggerPendingMessageProcessing();
}
