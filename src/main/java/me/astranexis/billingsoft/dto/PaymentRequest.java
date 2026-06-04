package me.astranexis.billingsoft.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import me.astranexis.billingsoft.model.PaymentMethod;

public record PaymentRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        String referenceNumber,
        String notes,
        LocalDateTime paidAt
) {
}
