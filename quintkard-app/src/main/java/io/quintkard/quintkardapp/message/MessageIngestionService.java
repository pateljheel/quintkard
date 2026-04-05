package io.quintkard.quintkardapp.message;

import java.util.List;

public interface MessageIngestionService {

    Message ingestMessage(String userId, MessageEnvelope envelope);

    List<Message> ingestMessages(String userId, List<MessageEnvelope> envelopes);
}