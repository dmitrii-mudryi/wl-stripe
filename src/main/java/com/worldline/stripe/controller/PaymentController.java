package com.worldline.stripe.controller;

import com.stripe.exception.StripeException;
import com.worldline.stripe.model.PaymentRequest;
import com.worldline.stripe.service.PaymentService;
import com.worldline.stripe.model.Payment;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody PaymentRequest request) {
        try {
            Payment payment = paymentService.createPayment(request);
            return ResponseEntity.ok(payment);
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<Payment> getPaymentStatus(@PathVariable String paymentId) {
        try {
            Payment payment = paymentService.getPaymentStatus(paymentId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
