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

import static com.worldline.stripe.util.PaymentUtil.getErrorPayment;

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
            Payment errorPayment = getErrorPayment(payment, request, e.getStripeError().getMessage());
            return new ResponseEntity<>(errorPayment, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error processing payment: {}", e.getMessage(), e);
            Payment errorPayment = getErrorPayment(payment, request, e.getMessage());
            return new ResponseEntity<>(errorPayment, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<Payment> getPaymentStatus(@PathVariable String paymentId) {
        logger.info("Received request to get status for payment ID: {}", paymentId);
        try {
            Payment payment = paymentService.getPaymentStatus(paymentId);
            logger.info("Payment status retrieved successfully for id: {}", payment.getPaymentId());
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            logger.error("Error retrieving payment status: {}", e.getMessage(), e);
            Payment errorPayment = Payment.builder()
                    .status("failed")
                    .errorMessage("An unexpected error occurred.")
                    .build();
            return ResponseEntity.internalServerError().body(errorPayment);
        }
    }
}
