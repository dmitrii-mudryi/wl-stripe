package com.worldline.stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.worldline.stripe.model.Payment;
import com.worldline.stripe.repository.PaymentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public Payment createPayment(Long amount) throws StripeException {
        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(amount)
                        .setCurrency("usd")
                        .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        Payment payment = new Payment();
        payment.setPaymentId(paymentIntent.getId());
        payment.setAmount(amount);
        payment.setStatus("created");
        payment.setClientSecret(paymentIntent.getClientSecret());
        paymentRepository.save(payment);

        return payment;
    }

    public Payment updatePaymentStatus(String paymentId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(paymentIntent.getStatus());
        paymentRepository.save(payment);

        return payment;
    }

    public Payment getPaymentStatus(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    @Scheduled(fixedRate = 60000) // 1 minute
    public void checkPendingPayments() {
        List<Payment> pendingPayments = paymentRepository.findByStatus("created");
        for (Payment payment : pendingPayments) {
            try {
                updatePaymentStatus(payment.getPaymentId());
            } catch (StripeException e) {
                e.printStackTrace(); // TODO: Log error
            }
        }
    }
}
