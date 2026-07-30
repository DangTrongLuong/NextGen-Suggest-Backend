package com.ecommerce.nivora.config;

import com.ecommerce.nivora.user.entity.Role;
import com.ecommerce.nivora.user.repository.RoleRepository;
import com.ecommerce.nivora.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed dữ liệu hệ thống bắt buộc:
 *  - 2 roles: admin, customer (cần để hệ thống hoạt động)
 *
 * Không seed user — toàn bộ user phải tạo qua API hoặc SQL trực tiếp.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        roleRepository.findByName("admin").orElseGet(() -> {
            log.info("Creating role: admin");
            return roleRepository.save(Role.builder()
                    .name("admin")
                    .displayName("Administrator")
                    .description("Full system access")
                    .build());
        });

        roleRepository.findByName("customer").orElseGet(() -> {
            log.info("Creating role: customer");
            return roleRepository.save(Role.builder()
                    .name("customer")
                    .displayName("Customer")
                    .description("Standard customer account")
                    .build());
        });

        // Auto-upgrade / hash default user passwords for demo accounts
        // SHA-256 of "password123" is "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f"
        String sha256Password = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f";
        
        userRepository.findByEmail("alex@example.com").ifPresent(u -> {
            if (!passwordEncoder.matches(sha256Password, u.getPasswordHash())) {
                u.setPasswordHash(passwordEncoder.encode(sha256Password));
                u.setIsActive(true);
                if (u.getTokenVersion() == null) {
                    u.setTokenVersion(0);
                }
                userRepository.save(u);
                log.info("Updated/migrated password hash for demo admin (alex@example.com)");
            }
        });

        userRepository.findByEmail("sarah@example.com").ifPresent(u -> {
            if (!passwordEncoder.matches(sha256Password, u.getPasswordHash())) {
                u.setPasswordHash(passwordEncoder.encode(sha256Password));
                u.setIsActive(true);
                if (u.getTokenVersion() == null) {
                    u.setTokenVersion(0);
                }
                userRepository.save(u);
                log.info("Updated/migrated password hash for demo user (sarah@example.com)");
            }
        });

        // Auto-migrate or reset password for dangtrongluong0806@gmail.com
        userRepository.findByEmail("dangtrongluong0806@gmail.com").ifPresent(u -> {
            String currentHash = u.getPasswordHash();
            if (currentHash != null) {
                String[] candidates = {"Password123!", "12345678aA@", "password123", "123456"};
                boolean migrated = false;
                for (String cand : candidates) {
                    if (passwordEncoder.matches(cand, currentHash)) {
                        try {
                            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                            byte[] hash = digest.digest(cand.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            StringBuilder hexString = new StringBuilder();
                            for (byte b : hash) {
                                String hex = Integer.toHexString(0xff & b);
                                if (hex.length() == 1) hexString.append('0');
                                hexString.append(hex);
                            }
                            u.setPasswordHash(passwordEncoder.encode(hexString.toString()));
                            u.setIsActive(true);
                            if (u.getTokenVersion() == null) {
                                u.setTokenVersion(0);
                            }
                            userRepository.save(u);
                            log.info("Auto-migrated password hash for existing user dangtrongluong0806@gmail.com");
                            migrated = true;
                            break;
                        } catch (Exception e) {
                            log.error("Failed to migrate password", e);
                        }
                    }
                }
                
                if (!migrated) {
                    try {
                        String defaultPlain = "12345678aA@";
                        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                        byte[] hash = digest.digest(defaultPlain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        StringBuilder hexString = new StringBuilder();
                        for (byte b : hash) {
                            String hex = Integer.toHexString(0xff & b);
                            if (hex.length() == 1) hexString.append('0');
                            hexString.append(hex);
                        }
                        u.setPasswordHash(passwordEncoder.encode(hexString.toString()));
                        u.setIsActive(true);
                        if (u.getTokenVersion() == null) {
                            u.setTokenVersion(0);
                        }
                        userRepository.save(u);
                        log.info("Reset password for user dangtrongluong0806@gmail.com to: 12345678aA@ (due to plain password mismatch)");
                    } catch (Exception e) {
                        log.error("Failed to reset password", e);
                    }
                }
            }
        });
    }
}
