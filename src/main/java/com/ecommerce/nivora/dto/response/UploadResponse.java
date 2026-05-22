package com.ecommerce.nivora.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UploadResponse {
    private String url;
    private String publicId;
    private String fileName;
    private long size;
    private String format;
}
