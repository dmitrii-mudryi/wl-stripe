package com.worldline.stripe.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.StripeError;
import com.worldline.stripe.model.Payment;
import com.worldline.stripe.model.PaymentRequest;
import com.worldline.stripe.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createPayment_AndConfirm_success() throws Exception {
        Payment payment = new Payment(1L, "pay_123", "succeeded", 1000L, "usd", "John Doe", "john.doe@example.com", null);

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(payment);
        doNothing().when(paymentService).confirmPayment(anyString(), anyString());

        mockMvc.perform(post("/api/payments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethodId\":\"pm_123\",\"amount\":1000,\"currency\":\"usd\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"simulateFailure\":false}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1,\"paymentId\":\"pay_123\",\"status\":\"succeeded\",\"amount\":1000,\"currency\":\"usd\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\"}"));
    }

    @Test
    void createPayment_AndConfirm_stripeException() throws Exception {
        Payment payment = new Payment(1L, "pay_123", "failed", 1000L, "usd", "John Doe", "john.doe@example.com", "Stripe error");

        StripeException stripeException = mock(StripeException.class);
        StripeError stripeError = mock(StripeError.class);
        when(stripeError.getMessage()).thenReturn("Stripe error");
        when(stripeException.getStripeError()).thenReturn(stripeError);

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(payment);
        doThrow(stripeException).when(paymentService).confirmPayment(anyString(), anyString());

        mockMvc.perform(post("/api/payments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethodId\":\"pm_123\",\"amount\":1000,\"currency\":\"usd\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"simulateFailure\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"id\":null,\"paymentId\":\"pay_123\",\"status\":\"failed\",\"amount\":1000,\"currency\":\"usd\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"errorMessage\":\"Stripe error\"}"));
    }

    @Test
    void createPayment_AndConfirm_generalException() throws Exception {
        Payment payment = new Payment(1L, "pay_123", "failed", 1000L, "usd", "John Doe", "john.doe@example.com", "General error");

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(payment);
        doThrow(new RuntimeException("General error")).when(paymentService).confirmPayment(anyString(), anyString());

        mockMvc.perform(post("/api/payments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethodId\":\"pm_123\",\"amount\":1000,\"currency\":\"usd\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"simulateFailure\":false}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getPaymentStatus_success() throws Exception {
        Payment payment = new Payment(1L, "pay_123", "succeeded", 1000L, "usd", "John Doe", "john.doe@example.com", null);

        when(paymentService.getPaymentStatus(anyString())).thenReturn(payment);

        mockMvc.perform(get("/api/payments/status/pay_123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1,\"paymentId\":\"pay_123\",\"status\":\"succeeded\",\"amount\":1000,\"currency\":\"usd\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\"}"));
    }

    @Test
    void getPaymentStatus_withNonExistentPaymentId() throws Exception {
        when(paymentService.getPaymentStatus(anyString())).thenThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(get("/api/payments/status/non_existent_id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"status\":\"failed\",\"errorMessage\":\"An unexpected error occurred.\"}"));
    }

    @Test
    void getPaymentStatus_exception() throws Exception {
        when(paymentService.getPaymentStatus(anyString())).thenThrow(new RuntimeException("General error"));

        mockMvc.perform(get("/api/payments/status/pay_123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"status\":\"failed\",\"errorMessage\":\"An unexpected error occurred.\"}"));
    }
}