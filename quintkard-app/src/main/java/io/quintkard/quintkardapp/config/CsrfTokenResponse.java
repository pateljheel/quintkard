package io.quintkard.quintkardapp.config;

public record CsrfTokenResponse(
        String headerName,
        String parameterName,
        String token
) {
}
