package io.quintkard.quintkardapp.message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface MessageSearchRepository {

    Slice<MessageSummaryProjection> searchSummaries(MessageFilter filter, Pageable pageable);
}
