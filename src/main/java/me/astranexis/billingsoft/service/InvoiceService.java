package me.astranexis.billingsoft.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import me.astranexis.billingsoft.dto.InvoiceItemRequest;
import me.astranexis.billingsoft.dto.InvoiceResponse;
import me.astranexis.billingsoft.dto.InvoiceRequest;
import me.astranexis.billingsoft.dto.PaymentRequest;
import me.astranexis.billingsoft.dto.PaymentResponse;
import me.astranexis.billingsoft.exception.BadRequestException;
import me.astranexis.billingsoft.exception.ResourceNotFoundException;
import me.astranexis.billingsoft.model.Customer;
import me.astranexis.billingsoft.model.Invoice;
import me.astranexis.billingsoft.model.InvoiceItem;
import me.astranexis.billingsoft.model.InvoiceStatus;
import me.astranexis.billingsoft.model.Payment;
import me.astranexis.billingsoft.model.Product;
import me.astranexis.billingsoft.repository.CustomerRepository;
import me.astranexis.billingsoft.repository.InvoiceRepository;
import me.astranexis.billingsoft.repository.PaymentRepository;
import me.astranexis.billingsoft.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            PaymentRepository paymentRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .filter(Customer::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active customer not found: " + request.customerId()));

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setCustomer(customer);
        invoice.setDiscountAmount(money(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount()));
        invoice.setNotes(normalizeOptional(request.notes()));
        invoice.setDueDate(request.dueDate());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        for (InvoiceItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Active product not found: " + itemRequest.productId()));
            if (product.getStockQuantity() < itemRequest.quantity()) {
                throw new BadRequestException("Insufficient stock for product " + product.getSku());
            }

            InvoiceItem item = buildInvoiceItem(product, itemRequest.quantity());
            product.setStockQuantity(product.getStockQuantity() - itemRequest.quantity());
            subtotal = subtotal.add(item.getLineSubtotal());
            taxTotal = taxTotal.add(item.getLineTax());
            invoice.addItem(item);
        }

        BigDecimal grossTotal = subtotal.add(taxTotal);
        if (invoice.getDiscountAmount().compareTo(grossTotal) > 0) {
            throw new BadRequestException("Discount cannot be greater than invoice total");
        }

        invoice.setSubtotal(money(subtotal));
        invoice.setTaxTotal(money(taxTotal));
        invoice.setGrandTotal(money(grossTotal.subtract(invoice.getDiscountAmount())));
        invoice.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        invoice.setStatus(InvoiceStatus.UNPAID);

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> list(InvoiceStatus status, Long customerId) {
        return invoiceRepository.findAll(Sort.by("createdAt").descending())
                .stream()
                .filter(invoice -> status == null || invoice.getStatus() == status)
                .filter(invoice -> customerId == null || invoice.getCustomer().getId().equals(customerId))
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(Long id) {
        return InvoiceResponse.from(findInvoice(id));
    }

    @Transactional
    public InvoiceResponse cancel(Long id) {
        Invoice invoice = findInvoice(id);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            return InvoiceResponse.from(invoice);
        }
        if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Paid invoices cannot be cancelled without refund handling");
        }
        invoice.getItems().forEach(item -> {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        });
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public PaymentResponse addPayment(Long invoiceId, PaymentRequest request) {
        Invoice invoice = findInvoice(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Cannot add payment to cancelled invoice");
        }

        BigDecimal amount = money(request.amount());
        BigDecimal balanceDue = invoice.getGrandTotal().subtract(invoice.getPaidAmount());
        if (amount.compareTo(balanceDue) > 0) {
            throw new BadRequestException("Payment amount cannot exceed balance due");
        }

        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setPaymentMethod(request.paymentMethod());
        payment.setReferenceNumber(normalizeOptional(request.referenceNumber()));
        payment.setNotes(normalizeOptional(request.notes()));
        payment.setPaidAt(request.paidAt());
        invoice.addPayment(payment);

        BigDecimal paidAmount = money(invoice.getPaidAmount().add(amount));
        invoice.setPaidAmount(paidAmount);
        invoice.setStatus(paidAmount.compareTo(invoice.getGrandTotal()) >= 0
                ? InvoiceStatus.PAID
                : InvoiceStatus.PARTIALLY_PAID);

        invoiceRepository.save(invoice);
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(Long invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("Invoice not found: " + invoiceId);
        }
        return paymentRepository.findByInvoiceIdOrderByPaidAtDesc(invoiceId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    private Invoice findInvoice(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    private InvoiceItem buildInvoiceItem(Product product, Integer quantity) {
        BigDecimal lineSubtotal = money(product.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
        BigDecimal lineTax = money(lineSubtotal.multiply(product.getTaxRate()).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));

        InvoiceItem item = new InvoiceItem();
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setSku(product.getSku());
        item.setQuantity(quantity);
        item.setUnitPrice(product.getUnitPrice());
        item.setTaxRate(product.getTaxRate());
        item.setLineSubtotal(lineSubtotal);
        item.setLineTax(lineTax);
        item.setLineTotal(money(lineSubtotal.add(lineTax)));
        return item;
    }

    private String generateInvoiceNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String invoiceNumber = "INV-" + timestamp + "-" + suffix;
        if (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
            return generateInvoiceNumber();
        }
        return invoiceNumber;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
