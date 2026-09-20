package com.example.parkio.config;

import com.example.parkio.entity.Role;
import com.example.parkio.entity.User;
import com.example.parkio.repository.RoleRepository;
import com.example.parkio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;

/**
 * Seeds required lookup data on first startup.
 * Safe to re-run — it only inserts missing records.
 *
 * Order(1) = roles first, Order(2) = admin user second.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.first-name}")
    private String adminFirstName;

    @Value("${app.admin.last-name}")
    private String adminLastName;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
    }

    private void seedRoles() {
        Arrays.stream(Role.RoleName.values()).forEach(roleName -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.info("Seeded role: {}", roleName);
            }
        });
    }

    private void seedAdminUser() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found after seeding"));

        User admin = User.builder()
                .firstName(adminFirstName)
                .lastName(adminLastName)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);
        log.info("Seeded default admin user: {}", adminEmail);
    }
}
