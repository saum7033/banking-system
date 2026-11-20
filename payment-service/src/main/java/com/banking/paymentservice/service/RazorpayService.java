package com.banking.paymentservice.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RazorpayService {

    public Map<String, Object> createOrder(String amountInPaise, String currency, String receipt) {
        return null;
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        return false;
    }

    public Map<String, Object> fetchPayment(String paymentId) {
        return null;
    }
}
