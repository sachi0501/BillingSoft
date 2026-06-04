package me.astranexis.billingsoft.dto;

import java.math.BigDecimal;
import me.astranexis.billingsoft.model.InvoiceItem;

public record InvoiceItemResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineSubtotal,
        BigDecimal lineTax,
        BigDecimal lineTotal
) {
    public static InvoiceItemResponse from(InvoiceItem item) {
        return new InvoiceItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getSku(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTaxRate(),
                item.getLineSubtotal(),
                item.getLineTax(),
                item.getLineTotal()
        );
    }
}
