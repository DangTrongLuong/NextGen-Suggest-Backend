package com.ecommerce.nivora;

import com.ecommerce.nivora.category.entity.Category;
import com.ecommerce.nivora.category.repository.CategoryRepository;
import com.ecommerce.nivora.product.entity.Inventory;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.product.entity.ProductVariant;
import com.ecommerce.nivora.product.repository.ProductRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
class NivoraApplicationTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @Transactional
    @Commit
    void seedProducts() throws Exception {
        String jsonlPath = "../ecommerce-backend-fastapi/data/processed/products_with_documents.jsonl";
        ObjectMapper mapper = new ObjectMapper();

        try (BufferedReader br = new BufferedReader(new FileReader(jsonlPath))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Map<String, Object> item = mapper.readValue(line, Map.class);

                String sku = (String) item.get("product_id");
                if (productRepository.existsBySku(sku)) {
                    continue;
                }

                String title = (String) item.get("title");
                String desc = (String) item.get("description");
                String categoryStr = (String) item.get("category");
                String imageUrl = (String) item.get("image_url");
                Number priceVal = (Number) item.get("price");
                BigDecimal price = priceVal != null ? BigDecimal.valueOf(priceVal.doubleValue()) : BigDecimal.ZERO;

                // Parse category hierarchy
                String leafCategory = "Other";
                if (categoryStr != null && !categoryStr.trim().isEmpty()) {
                    String[] parts = categoryStr.split(">");
                    leafCategory = parts[parts.length - 1].trim();
                }

                String finalLeafCategory = leafCategory;
                Category category = categoryRepository.findByNameAndDeletedAtIsNull(finalLeafCategory).orElseGet(() -> {
                    String slug = finalLeafCategory.toLowerCase().replaceAll("[^a-z0-9]+", "-");
                    Category newCat = Category.builder()
                            .name(finalLeafCategory)
                            .slug(slug + "-" + UUID.randomUUID().toString().substring(0, 8))
                            .isActive(true)
                            .displayOrder(0)
                            .build();
                    return categoryRepository.save(newCat);
                });

                String slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-");
                if (slug.length() > 200) {
                    slug = slug.substring(0, 200);
                }
                slug = slug + "-" + sku.toLowerCase();

                Product product = Product.builder()
                        .name(title.length() > 255 ? title.substring(0, 252) + "..." : title)
                        .slug(slug)
                        .description(desc)
                        .shortDescription(desc != null && desc.length() > 490 ? desc.substring(0, 490) + "..." : desc)
                        .isActive(true)
                        .ratingAvg(BigDecimal.valueOf(4.5))
                        .ratingCount(10)
                        .category(category)
                        .imageUrl(imageUrl)
                        .build();

                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .sku(sku)
                        .name("Default")
                        .price(price)
                        .comparePrice(price.multiply(BigDecimal.valueOf(1.2)))
                        .color("")
                        .size("")
                        .imageUrl(imageUrl)
                        .isActive(true)
                        .sortOrder(0)
                        .build();

                Inventory inventory = Inventory.builder()
                        .variant(variant)
                        .quantity(100)
                        .build();
                variant.setInventory(inventory);

                product.setVariants(new ArrayList<>(List.of(variant)));

                productRepository.save(product);
                count++;
            }
            System.out.println("Successfully seeded " + count + " products from JSONL into MySQL!");
        }
    }
}
