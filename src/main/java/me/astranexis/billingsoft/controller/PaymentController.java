package me.astranexis.billingsoft.controller;

import java.util.List;
import me.astranexis.billingsoft.dto.PaymentResponse;
import me.astranexis.billingsoft.service.InvoiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final InvoiceService invoiceService;

    public PaymentController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public List<PaymentResponse> list() {
        return invoiceService.listAllPayments();
    }
}
