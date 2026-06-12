package com.ecommerce.nivora.config;

import com.ecommerce.nivora.user.entity.Role;
import com.ecommerce.nivora.user.repository.RoleRepository;


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
    }
}
