package com.ecommerce.nivora.upload.controller;

import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.upload.dto.response.UploadResponse;
import com.ecommerce.nivora.upload.service.UploadService;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "File upload endpoints")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image to Cloudinary")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "general") String folder) {
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully",
                uploadService.uploadImage(file, folder)));
    }

    @DeleteMapping("/image")
    @Operation(summary = "Delete an image from Cloudinary")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@RequestParam String publicId) {
        uploadService.deleteImage(publicId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted", null));
    }
}
