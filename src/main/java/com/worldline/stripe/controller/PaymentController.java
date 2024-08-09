package com.worldline.stripe.controller;

import com.stripe.exception.StripeException;
import com.worldline.stripe.model.PaymentRequest;
import com.worldline.stripe.service.PaymentService;
import com.worldline.stripe.model.Payment;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody PaymentRequest request) {
        logger.info("Received request to create and confirm payment with payment method id {} for amount: {} {}",
                request.getPaymentMethodId(), request.getAmount(), request.getCurrency());

        Payment payment = new Payment();
        try {
            payment = paymentService.createPayment(request);
            paymentService.confirmPayment(payment.getPaymentId(), request.getPaymentMethodId());
            logger.info("Payment confirmed successfully for id: {}", payment.getPaymentId());
            return ResponseEntity.ok(payment);
        } catch (StripeException e) {
            logger.error("Error processing stripe payment: {}", e.getMessage(), e);
            Payment errorPayment = Payment.builder()
                    .paymentId(payment.getPaymentId())
                    .amount(request.getAmount())
                    .status("failed")
                    .errorMessage(e.getStripeError().getMessage())
                    .build();
            return new ResponseEntity<>(errorPayment, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error processing payment: {}", e.getMessage(), e);
            Payment errorPayment = Payment.builder()
                    .paymentId(payment.getPaymentId())
                    .amount(request.getAmount())
                    .status("failed")
                    .errorMessage(e.getMessage())
                    .build();
            return new ResponseEntity<>(errorPayment, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<Payment> getPaymentStatus(@PathVariable String paymentId) {
        logger.info("Received request to get status for payment ID: {}", paymentId);
        Payment payment;
        try {
            payment = paymentService.getPaymentStatus(paymentId);
            logger.info("Payment status retrieved successfully for id: {}", paymentId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            logger.error("Error retrieving payment status: {}", e.getMessage(), e);
            payment = Payment.builder()
                    .status("failed")
                    .errorMessage("An unexpected error occurred.")
                    .build();
            return new ResponseEntity<>(payment, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
