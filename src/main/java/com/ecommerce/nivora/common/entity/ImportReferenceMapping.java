package com.ecommerce.nivora.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_reference_mappings", indexes = {
    @Index(name = "idx_import_ref_entity_system_source", columnList = "entity_type, source_system, source_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportReferenceMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // "PRODUCT" or "CATEGORY"

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem; // e.g. "Excel"

    @Column(name = "source_id", nullable = false, length = 255)
    private String sourceId; // external source ID from Excel

    @Column(name = "db_id", nullable = false)
    private Long dbId; // database ID in MySQL

    @Column(name = "import_batch_id", length = 255)
    private String importBatchId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
