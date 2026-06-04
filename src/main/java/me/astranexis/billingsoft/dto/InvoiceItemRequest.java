package me.astranexis.billingsoft.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InvoiceItemRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
) {
}
