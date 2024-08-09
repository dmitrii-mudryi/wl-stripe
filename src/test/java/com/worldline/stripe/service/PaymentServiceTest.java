package com.worldline.stripe.service;

import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.worldline.stripe.model.Payment;
import com.worldline.stripe.model.PaymentRequest;
import com.worldline.stripe.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createPayment_success() throws StripeException {
        PaymentRequest request = new PaymentRequest("pm_123", 1000L, "usd", "John Doe", "john.doe@example.com", false);
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_123");
        when(paymentIntent.getAmount()).thenReturn(1000L);
        when(paymentIntent.getCurrency()).thenReturn("usd");

        try (var mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class))).thenReturn(paymentIntent);

            Payment payment = paymentService.createPayment(request);

            assertEquals("pi_123", payment.getPaymentId());
            assertEquals(1000L, payment.getAmount());
            assertEquals("usd", payment.getCurrency());
            assertEquals("created", payment.getStatus());
        }
    }

    @Test
    void createPayment_stripeException() {
        PaymentRequest request = new PaymentRequest("pm_123", 1000L, "usd", "John Doe", "john.doe@example.com", false);

        try (var mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(new CardException("Stripe error", "req_Id", "code", "param", "decline_code", "charge", 500, new Exception()));

            assertThrows(StripeException.class, () -> paymentService.createPayment(request));
        }
    }

    @Test
    void confirmPayment_success() throws StripeException {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);

        try (var mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_123")).thenReturn(paymentIntent);

            paymentService.confirmPayment("pi_123", "pm_123");

            verify(paymentIntent).confirm(any(PaymentIntentConfirmParams.class));
        }
    }

    @Test
    void confirmPayment_invalidRequestException() {
        try (var mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_123")).thenThrow(new InvalidRequestException(
                    "No such payment_intent: 'pi_123'", null, null, "404", null, null));

            assertThrows(InvalidRequestException.class, () -> paymentService.confirmPayment("pi_123", "pm_123"));
        }
    }

    @Test
    void updatePaymentStatus_success() throws StripeException {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getStatus()).thenReturn("succeeded");

        try (var mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_123")).thenReturn(paymentIntent);

            Payment payment = new Payment(1L, "pi_123", "created", 1000L, "usd", "John Doe", "john.doe@example.com", null);
            when(paymentRepository.findByPaymentId("pi_123")).thenReturn(Optional.of(payment));

            paymentService.updatePaymentStatus("pi_123");

            assertEquals("succeeded", payment.getStatus());
            verify(paymentRepository).save(payment);
        }
    }

    @Test
    void updatePaymentStatus_paymentNotFound() {
        try (var mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_123")).thenReturn(mock(PaymentIntent.class));
            when(paymentRepository.findByPaymentId("pi_123")).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> paymentService.updatePaymentStatus("pi_123"));
        }
    }

    @Test
    void getPaymentStatus_success() {
        Payment payment = new Payment(1L, "pi_123", "succeeded", 1000L, "usd", "John Doe", "john.doe@example.com", null);
        when(paymentRepository.findByPaymentId("pi_123")).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentStatus("pi_123");

        assertEquals(payment, result);
    }

    @Test
    void getPaymentStatus_paymentNotFound() {
        when(paymentRepository.findByPaymentId("pi_123")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.getPaymentStatus("pi_123"));
    }
}