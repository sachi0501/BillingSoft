package me.astranexis.billingsoft.service;

import java.util.List;
import me.astranexis.billingsoft.dto.CustomerRequest;
import me.astranexis.billingsoft.dto.CustomerResponse;
import me.astranexis.billingsoft.exception.BadRequestException;
import me.astranexis.billingsoft.exception.ResourceNotFoundException;
import me.astranexis.billingsoft.model.Customer;
import me.astranexis.billingsoft.repository.CustomerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        validateEmailAvailable(request.email(), null);
        Customer customer = new Customer();
        applyRequest(customer, request);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(Boolean active, String search) {
        String normalizedSearch = normalizeOptional(search);
        return customerRepository.findAll(Sort.by("name").ascending())
                .stream()
                .filter(customer -> active == null || customer.isActive() == active)
                .filter(customer -> matchesCustomerSearch(customer, normalizedSearch))
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        return CustomerResponse.from(findCustomer(id));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findCustomer(id);
        validateEmailAvailable(request.email(), id);
        applyRequest(customer, request);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findCustomer(id);
        customer.setActive(false);
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private void validateEmailAvailable(String email, Long currentId) {
        String normalizedEmail = normalizeOptional(email);
        if (normalizedEmail == null) {
            return;
        }
        customerRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Customer email already exists: " + normalizedEmail);
                });
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setName(normalizeRequired(request.name(), "name"));
        customer.setEmail(normalizeOptional(request.email()));
        customer.setPhone(normalizeOptional(request.phone()));
        customer.setAddress(normalizeOptional(request.address()));
        customer.setTaxNumber(normalizeOptional(request.taxNumber()));
        customer.setActive(request.active() == null || request.active());
    }

    private boolean matchesCustomerSearch(Customer customer, String search) {
        if (search == null) {
            return true;
        }
        String term = search.toLowerCase();
        return containsIgnoreCase(customer.getName(), term)
                || containsIgnoreCase(customer.getEmail(), term)
                || containsIgnoreCase(customer.getPhone(), term);
    }

    private boolean containsIgnoreCase(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException(field + " is required");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
