package com.clutch.app.config;

import com.clutch.app.entity.User;
import com.clutch.app.enums.Role;
import com.clutch.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "admin@clutch.app";

        TenantContext.runAsSystem(() -> {

            if (!userRepository.existsByEmail(adminEmail)) {
                log.info("Default system admin not found. Creating...");

                User admin = new User();
                admin.setEmail(adminEmail);
                // pass from application.properties / ENV
                admin.setPassword(passwordEncoder.encode("adminABC"));
                admin.setRole(Role.ROLE_SYSTEM_ADMIN);
                admin.setCompanyUuid(TenantContext.SYSTEM_UUID);

                userRepository.save(admin);
                log.info("System admin successfully created with email: {}", adminEmail);
            } else {
                log.info("System admin already exists. Skipping initialization.");
            }

        });
    }
}
