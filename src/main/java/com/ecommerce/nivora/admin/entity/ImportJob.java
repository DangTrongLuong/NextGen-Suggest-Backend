package com.ecommerce.nivora.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // "PRODUCT" or "CATEGORY"

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING"; // "PENDING", "PROCESSING", "COMPLETED", "FAILED"

    @Column(name = "total_rows")
    @Builder.Default
    private Integer totalRows = 0;

    @Column(name = "success_rows")
    @Builder.Default
    private Integer successRows = 0;

    @Column(name = "failed_rows")
    @Builder.Default
    private Integer failedRows = 0;

    @Column(name = "duplicate_rows")
    @Builder.Default
    private Integer duplicateRows = 0;

    @Column(name = "error_details", columnDefinition = "LONGTEXT")
    private String errorDetails;

    @Column(name = "duplicate_details", columnDefinition = "LONGTEXT")
    private String duplicateDetails;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }
}
