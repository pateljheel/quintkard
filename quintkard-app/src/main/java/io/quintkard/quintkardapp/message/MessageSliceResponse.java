package io.quintkard.quintkardapp.message;

import java.util.List;
import org.springframework.data.domain.Slice;

public record MessageSliceResponse(
        List<MessageListItemResponse> items,
        int page,
        int size,
        boolean hasNext
) {

    public static MessageSliceResponse from(Slice<MessageSummaryProjection> messages) {
        return new MessageSliceResponse(
                messages.stream()
                        .map(MessageListItemResponse::from)
                        .toList(),
                messages.getNumber(),
                messages.getSize(),
                messages.hasNext()
        );
    }
}
