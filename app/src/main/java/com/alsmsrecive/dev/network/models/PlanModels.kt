package com.alsmsrecive.dev.network.models

data class CreateOrderRequest(
    val planType: Int,
    val paymentMethod: String
)

data class CreateOrderResponse(
    val msg: String,
    val orderId: String,
    val amountBDT: Int,
    val amountUSDT: Double
)

data class VerifyOrderRequest(
    val orderId: String,
    val transactionId: String
)

data class VerifyOrderResponse(
    val msg: String,
    val newExpiry: String?,
    val deviceLimit: Int?,
    val activeSessionsCount: Int?
)

data class PaymentInfoResponse(
    val bkash: String,
    val nagad: String,
    val binance: String
)
