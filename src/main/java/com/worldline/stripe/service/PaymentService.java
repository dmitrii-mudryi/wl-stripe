package com.worldline.stripe.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.worldline.stripe.model.Payment;
import com.worldline.stripe.model.PaymentRequest;
import com.worldline.stripe.repository.PaymentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public Payment createPayment(PaymentRequest request) throws StripeException {
        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(request.getAmount())
                        .setCurrency(request.getCurrency())
                        .setReceiptEmail(request.getEmail())
                        .addPaymentMethodType("card")
                        .setDescription("Payment from " + request.getName())
                        .putMetadata("simulate_webhook_failure", String.valueOf(request.isSimulateWebhookFailure()))
                        .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        Payment payment = new Payment();
        payment.setPaymentId(paymentIntent.getId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setStatus("created");
        payment.setClientSecret(paymentIntent.getClientSecret());
        paymentRepository.save(payment);

        return payment;
    }

    public Payment updatePaymentStatus(String paymentId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);

        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(getErrorCode(paymentIntent));
        paymentRepository.save(payment);

        return payment;
    }

    public Payment getPaymentStatus(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    private static String getErrorCode(PaymentIntent paymentIntent) {
        return (paymentIntent.getLastPaymentError() != null && paymentIntent.getLastPaymentError().getDeclineCode() != null) ?
                paymentIntent.getLastPaymentError().getDeclineCode() : paymentIntent.getStatus();
    }
}
