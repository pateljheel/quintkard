package io.quintkard.quintkardapp.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "quintkard.user.admin")
public record AdminUserProperties(
        @NotBlank String userId,
        @NotBlank String displayName,
        @NotBlank @Email String email,
        @NotBlank String password,
        boolean redactionEnabled
) {
}
