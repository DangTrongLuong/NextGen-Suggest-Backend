package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByVariantId(Long variantId);

    @Query("SELECT i FROM Inventory i WHERE i.variant.product.deletedAt IS NULL AND i.quantity <= i.lowStockThreshold AND i.quantity > 0")
    List<Inventory> findLowStockItems();
}
