package com.ecommerce.nivora.upload.dto.response;


import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UploadResponse {
    private String url;
    private String publicId;
    private String fileName;
    private long size;
    private String format;
}
