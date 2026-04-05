package io.quintkard.quintkardapp.message;

import jakarta.validation.constraints.NotNull;

public record MessageStatusUpdateRequest(@NotNull MessageStatus status) {
}
