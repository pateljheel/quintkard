package io.quintkard.quintkardapp.card;

import java.util.List;
import org.springframework.data.domain.Slice;

public record CardSliceResponse(
        List<CardListItemResponse> items,
        int page,
        int size,
        boolean hasNext
) {

    public static CardSliceResponse from(Slice<CardSummaryProjection> cards) {
        return new CardSliceResponse(
                cards.stream()
                        .map(CardListItemResponse::from)
                        .toList(),
                cards.getNumber(),
                cards.getSize(),
                cards.hasNext()
        );
    }
}
