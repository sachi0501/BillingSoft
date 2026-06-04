package me.astranexis.billingsoft.repository;

import me.astranexis.billingsoft.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoiceIdOrderByPaidAtDesc(Long invoiceId);
}
