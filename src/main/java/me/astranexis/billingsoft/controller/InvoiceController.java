package me.astranexis.billingsoft.controller;

import jakarta.validation.Valid;
import java.util.List;
import me.astranexis.billingsoft.dto.InvoiceRequest;
import me.astranexis.billingsoft.dto.InvoiceResponse;
import me.astranexis.billingsoft.dto.PaymentRequest;
import me.astranexis.billingsoft.dto.PaymentResponse;
import me.astranexis.billingsoft.model.InvoiceStatus;
import me.astranexis.billingsoft.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    @GetMapping
    public List<InvoiceResponse> list(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long customerId
    ) {
        return invoiceService.list(status, customerId);
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable Long id) {
        return invoiceService.get(id);
    }

    @PatchMapping("/{id}/cancel")
    public InvoiceResponse cancel(@PathVariable Long id) {
        return invoiceService.cancel(id);
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<PaymentResponse> addPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.addPayment(id, request));
    }

    @GetMapping("/{id}/payments")
    public List<PaymentResponse> listPayments(@PathVariable Long id) {
        return invoiceService.listPayments(id);
    }
}
