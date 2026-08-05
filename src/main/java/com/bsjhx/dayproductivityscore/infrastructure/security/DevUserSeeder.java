package com.bsjhx.dayproductivityscore.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Configuration
@Profile("local")
@Slf4j
public class DevUserSeeder {

    @Bean
    CommandLineRunner seedDevUsers(UserSeedingService seedingService) {
        return args -> seedingService.seedUsers();
    }

    @Component
    @Profile("local")
    static class UserSeedingService {
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        UserSeedingService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
        }

        @Transactional
        public void seedUsers() {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User devUser = new User(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    "user",
                    passwordEncoder.encode("user"),
                    "USER"
                );

                User devAdmin = new User(
                    UUID.fromString("00000000-0000-0000-0000-000000000002"),
                    "admin",
                    passwordEncoder.encode("admin"),
                    "USER,ADMIN"
                );

                userRepository.save(devUser);
                userRepository.save(devAdmin);

                log.info("✓ Dev users seeded: user/user, admin/admin");
            } else {
                log.info("Users already exist, skipping seeding");
            }
        }
    }
}
