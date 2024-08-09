package com.worldline.stripe.util;

import com.worldline.stripe.model.Payment;
import com.worldline.stripe.model.PaymentRequest;

public class PaymentUtil {

    public static Payment getErrorPayment(Payment payment, PaymentRequest request, String e) {
        return Payment.builder()
                .paymentId(payment.getPaymentId())
                .amount(request.getAmount())
                .status("failed")
                .errorMessage(e)
                .build();
    }

}

