package me.astranexis.billingsoft.dto;

import java.time.LocalDateTime;
import me.astranexis.billingsoft.model.Customer;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        String taxNumber,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getTaxNumber(),
                customer.isActive(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
