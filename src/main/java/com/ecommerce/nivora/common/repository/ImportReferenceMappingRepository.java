package com.ecommerce.nivora.common.repository;

import com.ecommerce.nivora.common.entity.ImportReferenceMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImportReferenceMappingRepository extends JpaRepository<ImportReferenceMapping, Long> {
    Optional<ImportReferenceMapping> findByEntityTypeAndSourceSystemAndSourceId(
            String entityType, String sourceSystem, String sourceId);

    List<ImportReferenceMapping> findByEntityTypeAndSourceIdOrderByIdDesc(
            String entityType, String sourceId);

    void deleteByEntityTypeAndDbId(String entityType, Long dbId);
}
