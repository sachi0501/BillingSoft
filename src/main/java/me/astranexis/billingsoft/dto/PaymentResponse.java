package me.astranexis.billingsoft.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import me.astranexis.billingsoft.model.Payment;
import me.astranexis.billingsoft.model.PaymentMethod;

public record PaymentResponse(
        Long id,
        Long invoiceId,
        String invoiceNumber,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String referenceNumber,
        String notes,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getInvoice().getId(),
                payment.getInvoice().getInvoiceNumber(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getReferenceNumber(),
                payment.getNotes(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
