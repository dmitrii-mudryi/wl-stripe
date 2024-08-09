package com.worldline.stripe.scheduler;

import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.worldline.stripe.model.Payment;
import com.worldline.stripe.repository.PaymentRepository;
import com.worldline.stripe.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.Mockito.*;

class PaymentSchedulerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentScheduler paymentScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void checkPendingPayments_success() throws StripeException {
        Payment payment = new Payment(1L, "pi_123", "created", 1000L, "usd", "John Doe", "john.doe@example.com", null);
        when(paymentRepository.findByStatus("created")).thenReturn(List.of(payment));

        paymentScheduler.checkPendingPayments();

        verify(paymentService).updatePaymentStatus("pi_123");
    }

    @Test
    void checkPendingPayments_stripeException() throws StripeException {
        Payment payment = new Payment(1L, "pi_123", "created", 1000L, "usd", "John Doe", "john.doe@example.com", null);
        when(paymentRepository.findByStatus("created")).thenReturn(List.of(payment));
        doThrow(new CardException("Stripe error", "req_Id", "code", "param", "decline_code", "charge", 500, new Exception())).when(paymentService).updatePaymentStatus("pi_123");

        paymentScheduler.checkPendingPayments();

        verify(paymentService).updatePaymentStatus("pi_123");
    }
}