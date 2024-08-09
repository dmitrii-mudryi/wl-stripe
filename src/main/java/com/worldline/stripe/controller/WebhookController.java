package com.worldline.stripe.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.worldline.stripe.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    public static final String SIMULATE_WEBHOOK_FAILURE = "simulate_webhook_failure";
    public static final String PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";
    public static final String PAYMENT_INTENT_PAYMENT_FAILED = "payment_intent.payment_failed";

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final PaymentService paymentService;

    public WebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        logger.info("Received webhook from Stripe");

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);

            if (PAYMENT_INTENT_SUCCEEDED.equals(event.getType()) || PAYMENT_INTENT_PAYMENT_FAILED.equals(event.getType())) {
                logger.info("Processing webhook for event type: {}", event.getType());
                PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
                try {
                    if (Boolean.TRUE.toString().equals(paymentIntent.getMetadata().get(SIMULATE_WEBHOOK_FAILURE))) {
                        logger.warn("Webhook simulation failure enabled, skipping update for payment intent: {}", paymentIntent.getId());
                        return ResponseEntity.ok().build();
                    }
                    paymentService.updatePaymentStatus(paymentIntent.getId());
                } catch (StripeException e) {
                    logger.info("Error processing webhook: {}", e.getMessage(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating payment status");
                }
            } else {
                logger.info("Ignoring webhook for event type: {}", event.getType());
            }
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        return ResponseEntity.ok().build();
    }
}