package com.worldline.stripe.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Payment Method ID is required")
    private String paymentMethodId;
    
    @NotNull(message = "Amount is required")
    @Min(value = 50, message = "Amount should be at least 50 cents")
    private Long amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency should be a valid ISO currency code")
    private String currency;

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    private boolean simulateWebhookFailure;

    // Getters and setters
}