package com.mfapoc.config;

import com.mfapoc.entity.User;
import com.mfapoc.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@mfapoc.com");
            userRepository.save(admin);

            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setEmail("user@mfapoc.com");
            userRepository.save(user);

            log.info("Default users created: admin/admin123, user/user123");
        }
    }
}
