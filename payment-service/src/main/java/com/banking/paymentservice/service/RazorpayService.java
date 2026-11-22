package com.banking.paymentservice.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RazorpayService {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    public Map<String, Object> createOrder(String amountInPaise, String currency, String receipt) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", Integer.parseInt(amountInPaise));
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);

            Order order = client.orders.create(orderRequest);

            Map<String, Object> result = new HashMap<>();
            result.put("id", order.get("id"));
            result.put("entity", order.get("entity"));
            result.put("amount", order.get("amount"));
            result.put("amount_paid", order.get("amount_paid"));
            result.put("amount_due", order.get("amount_due"));
            result.put("currency", order.get("currency"));
            result.put("receipt", order.get("receipt"));
            result.put("status", order.get("status"));
            result.put("created_at", order.get("created_at"));
            return result;
        } catch (RazorpayException e) {
            throw new IllegalStateException("Failed to create Razorpay order", e);
        }
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", orderId);
        attributes.put("razorpay_payment_id", paymentId);
        attributes.put("razorpay_signature", signature);
        try {
            return Utils.verifyPaymentSignature(attributes, keySecret);
        } catch (RazorpayException e) {
            throw new IllegalStateException("Failed to verify Razorpay payment signature", e);
        }
    }

    public Map<String, Object> fetchPayment(String paymentId) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            com.razorpay.Payment payment = client.payments.fetch(paymentId);
            return payment.toJson().toMap();
        } catch (RazorpayException e) {
            throw new IllegalStateException("Failed to fetch Razorpay payment", e);
        }
    }
}
