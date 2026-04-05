package io.quintkard.quintkardapp.user;

public record UserResponse(
        String userId,
        String displayName,
        String email,
        boolean redactionEnabled
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getDisplayName(),
                user.getEmail(),
                user.isRedactionEnabled()
        );
    }
}
