package me.astranexis.billingsoft.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import me.astranexis.billingsoft.model.Product;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        Integer stockQuantity,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getUnitPrice(),
                product.getTaxRate(),
                product.getStockQuantity(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
