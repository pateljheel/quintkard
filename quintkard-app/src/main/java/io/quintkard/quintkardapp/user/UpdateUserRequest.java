package io.quintkard.quintkardapp.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank String displayName,
        @NotBlank @Email String email,
        boolean redactionEnabled
) {
}
