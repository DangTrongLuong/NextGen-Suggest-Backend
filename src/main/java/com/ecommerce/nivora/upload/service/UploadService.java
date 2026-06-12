package com.ecommerce.nivora.upload.service;

import com.ecommerce.nivora.upload.dto.response.UploadResponse;


import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    UploadResponse uploadImage(MultipartFile file, String folder);
    void deleteImage(String publicId);
}
