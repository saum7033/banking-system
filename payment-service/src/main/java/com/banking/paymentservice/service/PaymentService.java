package com.banking.paymentservice.service;

import com.banking.paymentservice.dto.CreatePaymentRequest;
import com.banking.paymentservice.dto.PaymentOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayService razorpayService;

    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest request) {
        return null;
    }

    public void handleWebhook(Map<String, Object> payload) {
    }
}
