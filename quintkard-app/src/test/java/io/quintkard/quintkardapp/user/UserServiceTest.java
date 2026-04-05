package io.quintkard.quintkardapp.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void getUserByUserIdReturnsUserWhenPresent() {
        User user = new User("alice", "Alice", "alice@example.com", "hash", false);
        when(userRepository.findByUserId("alice")).thenReturn(Optional.of(user));

        User result = userService.getUserByUserId("alice");

        assertSame(user, result);
    }

    @Test
    void getUserByUserIdThrowsWhenMissing() {
        when(userRepository.findByUserId("missing")).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserByUserId("missing")
        );

        assertEquals("User not found: missing", exception.getMessage());
    }

    @Test
    void updateUserProfileUpdatesDisplayNameEmailAndRedactionFlag() {
        User user = new User("alice", "Alice", "alice@example.com", "hash", false);
        when(userRepository.findByUserId("alice")).thenReturn(Optional.of(user));

        UpdateUserRequest request = new UpdateUserRequest("Alice Updated", "alice.updated@example.com", true);
        User updated = userService.updateUserProfile("alice", request);

        assertSame(user, updated);
        assertEquals("Alice Updated", updated.getDisplayName());
        assertEquals("alice.updated@example.com", updated.getEmail());
        assertTrue(updated.isRedactionEnabled());
    }

    @Test
    void updateUserProfileCanDisableRedaction() {
        User user = new User("alice", "Alice", "alice@example.com", "hash", true);
        when(userRepository.findByUserId("alice")).thenReturn(Optional.of(user));

        User updated = userService.updateUserProfile(
                "alice",
                new UpdateUserRequest("Alice", "alice@example.com", false)
        );

        assertSame(user, updated);
        assertFalse(updated.isRedactionEnabled());
    }
}
