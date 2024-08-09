package com.worldline.stripe.controller;

import com.stripe.exception.CardException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.worldline.stripe.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WebhookControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private WebhookController webhookController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void handleWebhook_paymentIntentSucceeded() throws StripeException {
        String payload = "{}";
        String sigHeader = "signature";
        Event event = mock(Event.class);
        Event.Data eventData = mock(Event.Data.class);
        PaymentIntent paymentIntent = mock(PaymentIntent.class);

        when(event.getType()).thenReturn(WebhookController.PAYMENT_INTENT_SUCCEEDED);
        when(event.getData()).thenReturn(eventData);
        when(eventData.getObject()).thenReturn(paymentIntent);
        when(paymentIntent.getId()).thenReturn("pi_123");
        when(paymentIntent.getMetadata()).thenReturn(Map.of());

        try (var mockedStatic = mockStatic(Webhook.class)) {
            mockedStatic.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(event);

            ResponseEntity<String> response = webhookController.handleWebhook(payload, sigHeader);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(paymentService).updatePaymentStatus("pi_123");
        }
    }

    @Test
    void handleWebhook_paymentIntentPaymentFailed() throws StripeException {
        String payload = "{}";
        String sigHeader = "signature";
        Event event = mock(Event.class);
        Event.Data eventData = mock(Event.Data.class);
        PaymentIntent paymentIntent = mock(PaymentIntent.class);

        when(event.getType()).thenReturn(WebhookController.PAYMENT_INTENT_PAYMENT_FAILED);
        when(event.getData()).thenReturn(eventData);
        when(eventData.getObject()).thenReturn(paymentIntent);
        when(paymentIntent.getId()).thenReturn("pi_123");
        when(paymentIntent.getMetadata()).thenReturn(Map.of());

        try (var mockedStatic = mockStatic(Webhook.class)) {
            mockedStatic.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(event);

            ResponseEntity<String> response = webhookController.handleWebhook(payload, sigHeader);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(paymentService).updatePaymentStatus("pi_123");
        }
    }

    @Test
    void handleWebhook_invalidSignature() {
        String payload = "{}";
        String sigHeader = "invalid_signature";

        try (var mockedStatic = mockStatic(Webhook.class)) {
            mockedStatic.when(() -> Webhook.constructEvent(any(), any(), any()))
                    .thenThrow(new SignatureVerificationException("Invalid signature", "sigHeader"));

            ResponseEntity<String> response = webhookController.handleWebhook(payload, sigHeader);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Invalid signature", response.getBody());
        }
    }

    @Test
    void handleWebhook_stripeException() throws StripeException {
        String payload = "{}";
        String sigHeader = "signature";
        Event event = mock(Event.class);
        Event.Data eventData = mock(Event.Data.class);
        PaymentIntent paymentIntent = mock(PaymentIntent.class);

        when(event.getType()).thenReturn(WebhookController.PAYMENT_INTENT_SUCCEEDED);
        when(event.getData()).thenReturn(eventData);
        when(eventData.getObject()).thenReturn(paymentIntent);
        when(paymentIntent.getId()).thenReturn("pi_123");
        when(paymentIntent.getMetadata()).thenReturn(Map.of());

        try (var mockedStatic = mockStatic(Webhook.class)) {
            mockedStatic.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(event);
            doThrow(new CardException("Stripe error", "req_Id", "code", "param", "decline_code", "charge", 500, new Exception())).when(paymentService).updatePaymentStatus("pi_123");

            ResponseEntity<String> response = webhookController.handleWebhook(payload, sigHeader);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Error updating payment status", response.getBody());
        }
    }
}