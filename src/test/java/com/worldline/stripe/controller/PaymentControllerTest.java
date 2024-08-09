package com.worldline.stripe.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.StripeError;
import com.worldline.stripe.model.Payment;
import com.worldline.stripe.model.PaymentRequest;
import com.worldline.stripe.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createPayment_AndConfirm_success() throws StripeException {
        PaymentRequest request = new PaymentRequest("pm_123", 1000L, "usd", "John Doe", "john.doe@example.com", false);
        Payment payment = new Payment(1L, "pay_123", "succeeded", 1000L, "usd", "John Doe", "john.doe@example.com", null);

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(payment);
        doNothing().when(paymentService).confirmPayment(anyString(), anyString());

        ResponseEntity<Payment> response = paymentController.createPaymentAndConfirm(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(payment, response.getBody());
    }

    @Test
    void createPayment_AndConfirm_stripeException() throws StripeException {
        PaymentRequest request = new PaymentRequest("pm_123", 1000L, "usd", "John Doe", "john.doe@example.com", false);
        Payment payment = new Payment(1L, "pay_123", "failed", 1000L, "usd", "John Doe", "john.doe@example.com", null);
        StripeError stripeError = new StripeError();
        stripeError.setMessage("Stripe error");

        StripeException stripeException = mock(StripeException.class);
        when(stripeException.getStripeError()).thenReturn(stripeError);

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(payment);
        doThrow(stripeException).when(paymentService).confirmPayment(anyString(), anyString());

        ResponseEntity<Payment> response = paymentController.createPaymentAndConfirm(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("failed", response.getBody().getStatus());
        assertEquals("Stripe error", response.getBody().getErrorMessage());
    }

    @Test
    void createPayment_AndConfirm_generalException() throws StripeException {
        PaymentRequest request = new PaymentRequest("pm_123", 1000L, "usd", "John Doe", "john.doe@example.com", false);
        Payment payment = new Payment(1L, "pay_123", "failed", 1000L, "usd", "John Doe", "john.doe@example.com", null);

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(payment);
        doThrow(new RuntimeException("General error")).when(paymentService).confirmPayment(anyString(), anyString());

        ResponseEntity<Payment> response = paymentController.createPaymentAndConfirm(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("failed", response.getBody().getStatus());
        assertEquals("General error", response.getBody().getErrorMessage());
    }

    @Test
    void getPaymentStatus_success() {
        Payment payment = new Payment(1L, "pay_123", "succeeded", 1000L, "usd", "John Doe", "john.doe@example.com", null);

        when(paymentService.getPaymentStatus(anyString())).thenReturn(payment);

        ResponseEntity<Payment> response = paymentController.getPaymentStatus("pay_123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(payment, response.getBody());
    }

    @Test
    void getPaymentStatus_withNonExistentPaymentId() {
        when(paymentService.getPaymentStatus(anyString())).thenThrow(new RuntimeException("Payment not found"));

        ResponseEntity<Payment> response = paymentController.getPaymentStatus("non_existent_id");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("failed", response.getBody().getStatus());
        assertEquals("An unexpected error occurred.", response.getBody().getErrorMessage());
    }

    @Test
    void getPaymentStatus_exception() {
        when(paymentService.getPaymentStatus(anyString())).thenThrow(new RuntimeException("General error"));

        ResponseEntity<Payment> response = paymentController.getPaymentStatus("pay_123");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("failed", response.getBody().getStatus());
        assertEquals("An unexpected error occurred.", response.getBody().getErrorMessage());
    }
}