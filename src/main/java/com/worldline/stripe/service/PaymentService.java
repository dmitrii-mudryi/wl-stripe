package com.worldline.stripe.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.worldline.stripe.model.Payment;
import com.worldline.stripe.model.PaymentRequest;
import com.worldline.stripe.repository.PaymentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

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
        logger.info("Creating payment intent with payment method id {} for amount: {} {}",
                request.getPaymentMethodId(), request.getAmount(), request.getCurrency());

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

        logger.info("Created payment intent with payment method id {} for amount: {} {}",
                request.getPaymentMethodId(), request.getAmount(), request.getCurrency());

        Payment payment = Payment.builder()
                .paymentId(paymentIntent.getId())
                .amount(paymentIntent.getAmount())
                .currency(paymentIntent.getCurrency())
                .status("created")
                .name(request.getName())
                .email(request.getEmail())
                .build();
        paymentRepository.save(payment);

        logger.info("Payment id saved successfully: {} with amount: {}, currency {}", payment.getPaymentId(),
                payment.getAmount(), payment.getCurrency());

        return payment;
    }

    public void confirmPayment(String paymentId, String paymentMethodId) throws StripeException {
        logger.info("Confirming payment with paymentId: {}, payment method id: {}", paymentId, paymentMethodId);

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);

        PaymentIntentConfirmParams confirmParams =
                PaymentIntentConfirmParams.builder()
                        .setPaymentMethod(paymentMethodId)
                        .build();

        paymentIntent.confirm(confirmParams);
    }

    public void updatePaymentStatus(String paymentId) throws StripeException {
        logger.info("Updating payment status for paymentId: {}", paymentId);

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);

        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(getStatus(paymentIntent));
        paymentRepository.save(payment);
    }

    public Payment getPaymentStatus(String paymentId) {
        logger.info("Getting payment status for paymentId: {}", paymentId);
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    private static String getStatus(PaymentIntent paymentIntent) {
        return "succeeded".equals(paymentIntent.getStatus()) ? "succeeded" : "failed";
    }
}
