package com.example.iropsim.config;

import com.example.iropsim.entity.Role;
import com.example.iropsim.entity.User;
import com.example.iropsim.repository.RoleRepository;
import com.example.iropsim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * 开发环境数据初始化器
 */
@Slf4j
@Component
@Profile({"dev", "default"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void run(String... args) throws Exception {
        // 只在开发环境初始化数据
        if (!"dev".equals(activeProfile) && !"default".equals(activeProfile)) {
            return;
        }

        // 检查是否需要初始化数据（如果数据库中没有用户）
        if (userRepository.count() == 0) {
            initializeRoles();
            initializeAdminUser();
            log.info("Development data initialization completed");
        } else {
            log.info("Database already contains data, skipping initialization");
        }
    }

    private void initializeRoles() {
        if (roleRepository.findByName("ADMIN").isEmpty()) {
            Role adminRole = Role.builder()
                    .name("ADMIN")
                    .description("System administrator with full access")
                    .createdAt(Instant.now())
                    .build();
            roleRepository.save(adminRole);
            log.info("Created ADMIN role");
        }

        if (roleRepository.findByName("OPERATOR").isEmpty()) {
            Role operatorRole = Role.builder()
                    .name("OPERATOR")
                    .description("Operations user with monitoring and management access")
                    .createdAt(Instant.now())
                    .build();
            roleRepository.save(operatorRole);
            log.info("Created OPERATOR role");
        }

        if (roleRepository.findByName("VIEWER").isEmpty()) {
            Role viewerRole = Role.builder()
                    .name("VIEWER")
                    .description("Read-only user with view access")
                    .createdAt(Instant.now())
                    .build();
            roleRepository.save(viewerRole);
            log.info("Created VIEWER role");
        }
    }

    private void initializeAdminUser() {
        User adminUser = userRepository.findByUsername("admin").orElse(null);
        if (adminUser != null) {
            // 检查密码是否需要更新（如果是{noop}前缀，则需要重新编码）
            if (adminUser.getPasswordHash().startsWith("{noop}")) {
                adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
                userRepository.save(adminUser);
                log.info("Updated admin user password to use BCrypt encoding");
            }
        } else {
            log.warn("Admin user not found in database. Please check database migration.");
        }
    }
}
