package com.ecommerce.nivora.service.impl;

import com.ecommerce.nivora.common.PageResponse;
import com.ecommerce.nivora.dto.response.AdminUserResponse;
import com.ecommerce.nivora.entity.Role;
import com.ecommerce.nivora.entity.User;
import com.ecommerce.nivora.exception.ResourceNotFoundException;
import com.ecommerce.nivora.repository.OrderRepository;
import com.ecommerce.nivora.repository.UserRepository;
import com.ecommerce.nivora.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getAllUsers(String search, Pageable pageable) {
        return PageResponse.of(userRepository.findAllForAdmin(search, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse setActive(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setIsActive(active);
        userRepository.save(user);
        return toResponse(user);
    }

    private AdminUserResponse toResponse(User u) {
        Set<String> roleNames = u.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        long orderCount = orderRepository.countByUserId(u.getId());
        BigDecimal totalSpent = orderRepository.sumTotalSpentByUserId(u.getId());
        return AdminUserResponse.builder()
                .id(u.getId())
                .uuid(u.getUuid())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .roles(roleNames)
                .role(roleNames.contains("admin") ? "admin" : "customer")
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .orderCount(orderCount)
                .totalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO)
                .build();
    }
}
