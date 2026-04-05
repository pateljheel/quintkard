package io.quintkard.quintkardapp.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class QuintkardUserDetailsServiceTest {

    private UserRepository userRepository;
    private QuintkardUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new QuintkardUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsernameReturnsStandardUserRoleForNonAdmin() {
        User user = new User("alice", "Alice", "alice@example.com", "hashed-password", false);
        when(userRepository.findByUserId("alice")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("alice");

        assertEquals("alice", userDetails.getUsername());
        assertEquals("hashed-password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsernameAddsAdminRoleForAdminUserIdIgnoringCase() {
        User user = new User("Admin", "Admin", "admin@example.com", "hashed-password", false);
        when(userRepository.findByUserId("Admin")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("Admin");

        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsernameThrowsWhenUserMissing() {
        when(userRepository.findByUserId("missing")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing")
        );

        assertEquals("User not found: missing", exception.getMessage());
    }
}
