package com.pukaar.domain.payment;

import com.pukaar.common.ApiException;
import com.pukaar.config.PukaarProperties;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RazorpayService {
    private final PukaarProperties props;

    public boolean isConfigured() {
        return props.getRazorpay().getKeyId() != null && !props.getRazorpay().getKeyId().isBlank()
                && props.getRazorpay().getKeySecret() != null && !props.getRazorpay().getKeySecret().isBlank();
    }

    public String keyId() {
        return props.getRazorpay().getKeyId();
    }

    public Order createOrder(int amountPaise, String receipt) {
        if (!isConfigured()) {
            throw new ApiException("RAZORPAY_NOT_CONFIGURED", "Payment gateway is not configured");
        }
        try {
            RazorpayClient client = new RazorpayClient(props.getRazorpay().getKeyId(), props.getRazorpay().getKeySecret());
            JSONObject request = new JSONObject();
            request.put("amount", amountPaise);
            request.put("currency", "INR");
            request.put("receipt", receipt);
            request.put("payment_capture", 1);
            return client.orders.create(request);
        } catch (RazorpayException e) {
            throw new ApiException("RAZORPAY_ORDER_FAILED", "Could not create payment order: " + e.getMessage());
        }
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (!isConfigured()) return false;
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);
            return Utils.verifyPaymentSignature(options, props.getRazorpay().getKeySecret());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        String secret = props.getRazorpay().getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            secret = props.getRazorpay().getKeySecret();
        }
        try {
            return Utils.verifyWebhookSignature(payload, signature, secret);
        } catch (Exception e) {
            return false;
        }
    }
}
