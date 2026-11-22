package com.banking.paymentservice.service;

import com.banking.paymentservice.dto.CreatePaymentRequest;
import com.banking.paymentservice.dto.PaymentOrderResponse;
import com.banking.paymentservice.entity.Payment;
import com.banking.paymentservice.entity.PaymentStatus;
import com.banking.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;

    @Value("${razorpay.key-id}")
    private String keyId;

    /**create razorpay payment order
     * save payment in DB
     * Return order detail to frontend
     * Frontend show Razorpay Checkout
     * User pays
     * Razorpay calls webhook
     */
    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest request) {
        log.info("Creating payment order for account: {} amount: {}",
                request.getAccountNumber(), request.getAmount());

        BigDecimal amount = request.getAmount();
        int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValueExact();
        String receipt = "rcpt_" + UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> order = razorpayService.createOrder(String.valueOf(amountInPaise), "INR", receipt);
        String razorpayOrderId = (String) order.get("id");

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setAccountNumber(request.getAccountNumber());
        payment.setAmount(amount.toPlainString());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(request.getDescription());

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentOrderResponse(
                savedPayment.getId(),
                razorpayOrderId,
                amount,
                "INR",
                PaymentStatus.CREATED.name(),
                keyId
        );
    }

    public void handleWebhook(Map<String, Object> payload) {
    }
}
