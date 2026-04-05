package io.quintkard.quintkardapp.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getUserByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional
    public User updateUserProfile(String userId, UpdateUserRequest request) {
        User user = getUserByUserId(userId);
        user.updateProfile(request.displayName(), request.email());
        user.updateRedactionEnabled(request.redactionEnabled());
        return user;
    }
}
