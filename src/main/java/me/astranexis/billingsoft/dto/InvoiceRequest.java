package me.astranexis.billingsoft.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
        @NotNull Long customerId,
        @NotEmpty List<@Valid InvoiceItemRequest> items,
        @DecimalMin(value = "0.0") BigDecimal discountAmount,
        String notes,
        LocalDate dueDate
) {
}
