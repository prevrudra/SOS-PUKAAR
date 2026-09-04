package com.pukaar.app.payment

import android.app.Activity
import com.pukaar.app.data.api.PaymentOrderDto
import com.razorpay.Checkout
import com.razorpay.PaymentData
import org.json.JSONObject

data class PaymentResult(
    val orderId: String,
    val paymentId: String,
    val signature: String
)

object RazorpayPaymentBridge {
    private var callback: ((Result<PaymentResult>) -> Unit)? = null

    fun startCheckout(activity: Activity, order: PaymentOrderDto, onResult: (Result<PaymentResult>) -> Unit) {
        callback = onResult
        val checkout = Checkout()
        checkout.setKeyID(order.keyId ?: return onResult(Result.failure(IllegalStateException("Missing Razorpay key"))))
        val options = JSONObject().apply {
            put("name", "PUKAAR")
            put("description", order.description ?: "PUKAAR subscription")
            put("order_id", order.orderId)
            put("currency", order.currency ?: "INR")
            put("amount", order.amount)
            put("prefill", JSONObject().apply {
                put("contact", order.userPhone)
                put("name", order.userName)
            })
            put("theme", JSONObject().put("color", "#22C55E"))
        }
        checkout.open(activity, options)
    }

    fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val cb = callback ?: return
        callback = null
        val orderId = paymentData?.orderId.orEmpty()
        val paymentId = razorpayPaymentId ?: paymentData?.paymentId.orEmpty()
        val signature = paymentData?.signature.orEmpty()
        if (orderId.isBlank() || paymentId.isBlank() || signature.isBlank()) {
            cb(Result.failure(IllegalStateException("Incomplete payment response")))
        } else {
            cb(Result.success(PaymentResult(orderId, paymentId, signature)))
        }
    }

    fun onPaymentError(code: Int, description: String?, @Suppress("UNUSED_PARAMETER") paymentData: PaymentData?) {
        val cb = callback ?: return
        callback = null
        cb(Result.failure(IllegalStateException(description ?: "Payment failed ($code)")))
    }
}
