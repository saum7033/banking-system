package com.banking.paymentservice.service;

import com.banking.paymentservice.dto.CreatePaymentRequest;
import com.banking.paymentservice.dto.PaymentOrderResponse;
import com.banking.paymentservice.entity.Payment;
import com.banking.paymentservice.entity.PaymentStatus;
import com.banking.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;

    private final KafkaTemplate<String,Object> kafkaTemplate;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";

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
        log.info("Received Razorpay webhook: {}", payload.get("event"));
        String event = (String) payload.get("event");
        if("payment.captured".equals(event)){
            handlePaymentSuccess(payload);
        }else if("payment.failed".equals(event)){
            handlePaymentFailure(payload);
        }
    }

    private void handlePaymentSuccess(Map<String, Object> payload){
        try{
            Map<String, Object> paymentData = extractPaymentData(payload);
            String orderId = (String) paymentData.get("order_id");
            String paymentId = (String) paymentData.get("id");

            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException(
                            "Payment not found for order :" + orderId
                    ));
            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("razorpayPaymentId", paymentId);

            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, payment.getId(),event);
            log.info("Payment completed : {}", payment.getId());
        }
        catch (Exception e){
            log.error("Error handling payment success : {}", e.getMessage());
        }
    }

    private void handlePaymentFailure(Map<String, Object> payload){
        try{
            Map<String, Object> paymentData = extractPaymentData(payload);
            String orderId = (String) paymentData.get("order_id");

            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException(
                            "Payment not found for order: " + orderId
                    ));
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed via Razorpay");
            paymentRepository.save(payment);

            Map<String,Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("reason", "Payment failed via razorpay");

            kafkaTemplate.send(PAYMENT_FAILED_TOPIC, payment.getId(), event);

            log.warn("Payment failed : {}", payment.getId());
        }
        catch (Exception e){
            log.error("Error handling payment failure : {}", e.getMessage());
        }
    }

    private Map<String, Object> extractPaymentData(Map<String,Object> payload){
        Map<String, Object> entity = (Map<String, Object>) payload.get("payload");

        Map<String, Object> paymentWrapper = (Map<String, Object>) entity.get("payment");

        return (Map<String,Object>) paymentWrapper.get("entity");
    }
}
