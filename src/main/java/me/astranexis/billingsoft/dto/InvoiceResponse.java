package me.astranexis.billingsoft.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import me.astranexis.billingsoft.model.Invoice;
import me.astranexis.billingsoft.model.InvoiceStatus;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long customerId,
        String customerName,
        List<InvoiceItemResponse> items,
        BigDecimal subtotal,
        BigDecimal taxTotal,
        BigDecimal discountAmount,
        BigDecimal grandTotal,
        BigDecimal paidAmount,
        BigDecimal balanceDue,
        InvoiceStatus status,
        String notes,
        LocalDate issuedAt,
        LocalDate dueDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getCustomer().getId(),
                invoice.getCustomer().getName(),
                invoice.getItems().stream().map(InvoiceItemResponse::from).toList(),
                invoice.getSubtotal(),
                invoice.getTaxTotal(),
                invoice.getDiscountAmount(),
                invoice.getGrandTotal(),
                invoice.getPaidAmount(),
                invoice.getGrandTotal().subtract(invoice.getPaidAmount()),
                invoice.getStatus(),
                invoice.getNotes(),
                invoice.getIssuedAt(),
                invoice.getDueDate(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }
}
