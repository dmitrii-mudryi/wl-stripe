package com.worldline.stripe.controller;

import com.stripe.exception.StripeException;
import com.worldline.stripe.model.PaymentRequest;
import com.worldline.stripe.service.PaymentService;
import com.worldline.stripe.model.Payment;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody PaymentRequest request) {
        Payment payment = new Payment();
        try {
            payment = paymentService.createPayment(request);
            paymentService.confirmPayment(payment.getPaymentId(), request.getPaymentMethodId());
            return ResponseEntity.ok(payment);
        } catch (StripeException e) {
            Payment errorPayment = Payment.builder()
                    .paymentId(payment.getPaymentId())
                    .amount(request.getAmount())
                    .status("failed")
                    .errorMessage(e.getStripeError().getMessage())
                    .build();
            return new ResponseEntity<>(errorPayment, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
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
        Payment payment;
        try {
            payment = paymentService.getPaymentStatus(paymentId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            payment = Payment.builder()
                    .status("failed")
                    .errorMessage("An unexpected error occurred.")
                    .build();
            return new ResponseEntity<>(payment, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
