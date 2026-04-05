package io.quintkard.quintkardapp.aimodel;

public interface AiChatService {

    AiChatResponse chat(AiChatRequest request);

    <T> T chatForObject(AiStructuredChatRequest<T> request);
}
