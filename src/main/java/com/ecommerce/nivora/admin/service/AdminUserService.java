package com.ecommerce.nivora.admin.service;

import com.ecommerce.nivora.admin.dto.response.AdminUserResponse;
import com.ecommerce.nivora.common.dto.response.PageResponse;


import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    PageResponse<AdminUserResponse> getAllUsers(String search, Pageable pageable);
    AdminUserResponse getUserById(Long id);
    AdminUserResponse setActive(Long id, boolean active);
}
