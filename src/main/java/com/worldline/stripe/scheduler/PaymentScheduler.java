package com.worldline.stripe.scheduler;

import com.stripe.exception.StripeException;
import com.worldline.stripe.service.PaymentService;
import com.worldline.stripe.model.Payment;
import com.worldline.stripe.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PaymentScheduler.class);

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public PaymentScheduler(PaymentRepository paymentRepository,
                            PaymentService paymentService) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    @Scheduled(fixedRate = 20000)
    public void checkPendingPayments() {
        logger.info("Scheduler running to check pending payments");

        List<Payment> pendingPayments = paymentRepository.findByStatus("created");
        for (Payment payment : pendingPayments) {
            try {
                paymentService.updatePaymentStatus(payment.getPaymentId());
            } catch (StripeException e) {
                logger.error("Error processing stripe payment: {}", e.getMessage(), e);
            }
        }
    }
}
