package com.ecommerce.nivora.admin.repository;

import com.ecommerce.nivora.admin.entity.ImportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    Page<ImportJob> findAllByOrderByStartedAtDesc(Pageable pageable);
}
