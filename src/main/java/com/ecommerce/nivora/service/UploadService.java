package com.ecommerce.nivora.service;

import com.ecommerce.nivora.dto.response.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    UploadResponse uploadImage(MultipartFile file, String folder);
    void deleteImage(String publicId);
}
