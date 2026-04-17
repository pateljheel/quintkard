package io.quintkard.quintkardapp.config;

import io.quintkard.quintkardapp.user.User;
import io.quintkard.quintkardapp.user.UserRepository;
import io.quintkard.quintkardapp.user.UserSampleDataInitializer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(AdminUserProperties.class)
public class AdminUserInitializer {

    @Bean
    ApplicationRunner initializeAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AdminUserProperties adminUserProperties,
            UserSampleDataInitializer userSampleDataInitializer
    ) {
        return args -> {
            User user = userRepository.findByUserId(adminUserProperties.userId())
                    .orElseGet(() -> userRepository.save(new User(
                            adminUserProperties.userId(),
                            adminUserProperties.displayName(),
                            adminUserProperties.email(),
                            passwordEncoder.encode(adminUserProperties.password()),
                            adminUserProperties.redactionEnabled()
                    )));
            userSampleDataInitializer.initializeForNewUser(user);
        };
    }
}
