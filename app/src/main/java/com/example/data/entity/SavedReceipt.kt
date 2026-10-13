package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.OrderDetails
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "saved_receipts",
    indices = [Index(value = ["invoiceNumber"], unique = true)]
)
data class SavedReceipt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalOrderId: Long = 0,
    val invoiceNumber: String,
    val originalTimestamp: Long = System.currentTimeMillis(),
    val clearedTimestamp: Long = System.currentTimeMillis(),
    val subtotal: Long,
    val discount: Long = 0,
    val grandTotal: Long,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val amountTendered: Long = 0,
    val changeAmount: Long = 0,
    val customerName: String = "",
    val customerPhone: String = "",
    val note: String = "",
    val status: OrderStatus = OrderStatus.COMPLETED,
    val itemsJson: String = "",
    val currency: String = "MMK",
    val totalMmk: Long = 0L,
    val totalThb: Long = 0L,
    val exchangeRate: Double = 0.0
) {
    fun getEffectiveTotalMmk(): Long {
        if (totalMmk > 0L) return totalMmk
        val rate = if (exchangeRate > 0) com.example.data.toThbToMmkRate(exchangeRate) else 0.0
        if (totalThb > 0L) return Math.round(totalThb * rate)
        if (currency == "THB") {
            return Math.round(grandTotal * rate)
        }
        return grandTotal
    }

    fun getEffectiveTotalThb(): Long {
        if (totalThb > 0L) return totalThb
        val rate = if (exchangeRate > 0) com.example.data.toMmkToThbRate(exchangeRate) else 0.0
        if (currency == "THB") return grandTotal
        if (totalMmk > 0L) return if (rate > 0) Math.round(totalMmk * rate) else 0L
        return if (rate > 0) Math.round(grandTotal * rate) else 0L
    }

    fun getEffectiveDiscountMmk(): Long {
        if (discount <= 0L) return 0L
        if (currency == "THB") {
            val rate = if (exchangeRate > 0) com.example.data.toThbToMmkRate(exchangeRate) else 0.0
            return Math.round(discount * rate)
        }
        return discount
    }

    fun getEffectiveDiscountThb(): Long {
        if (discount <= 0L) return 0L
        if (currency == "THB") return discount
        val rate = if (exchangeRate > 0) com.example.data.toMmkToThbRate(exchangeRate) else 0.0
        return if (rate > 0) Math.round(discount * rate) else 0L
    }

    fun getEffectiveSubtotalMmk(): Long {
        val total = getEffectiveTotalMmk()
        val disc = getEffectiveDiscountMmk()
        return total + disc
    }

    fun getEffectiveSubtotalThb(): Long {
        val total = getEffectiveTotalThb()
        val disc = getEffectiveDiscountThb()
        return total + disc
    }

    fun getEffectiveTenderedMmk(): Long {
        if (amountTendered <= 0L) return getEffectiveTotalMmk()
        if (currency == "MMK") return amountTendered
        val rate = if (exchangeRate > 0) com.example.data.toThbToMmkRate(exchangeRate) else 0.0
        return Math.round(amountTendered * rate)
    }

    fun getEffectiveTenderedThb(): Long {
        if (amountTendered <= 0L) return getEffectiveTotalThb()
        if (currency == "THB") return amountTendered
        val rate = if (exchangeRate > 0) com.example.data.toMmkToThbRate(exchangeRate) else 0.0
        return if (rate > 0) Math.round(amountTendered * rate) else 0L
    }

    fun getEffectiveChangeMmk(): Long {
        if (changeAmount <= 0L) return 0L
        if (currency == "MMK") return changeAmount
        val rate = if (exchangeRate > 0) com.example.data.toThbToMmkRate(exchangeRate) else 0.0
        return Math.round(changeAmount * rate)
    }

    fun getEffectiveChangeThb(): Long {
        if (changeAmount <= 0L) return 0L
        if (currency == "THB") return changeAmount
        val rate = if (exchangeRate > 0) com.example.data.toMmkToThbRate(exchangeRate) else 0.0
        return if (rate > 0) Math.round(changeAmount * rate) else 0L
    }

    fun toOrderDetails(): OrderDetails {
        val order = Order(
            id = originalOrderId.takeIf { it != 0L } ?: id,
            invoiceNumber = invoiceNumber,
            timestamp = originalTimestamp,
            subtotal = subtotal,
            discount = discount,
            grandTotal = grandTotal,
            paymentMethod = paymentMethod,
            amountTendered = amountTendered,
            changeAmount = changeAmount,
            customerName = customerName,
            customerPhone = customerPhone,
            note = note,
            status = status,
            currency = currency,
            totalMmk = totalMmk,
            totalThb = totalThb,
            exchangeRate = exchangeRate
        )

        val items = parseItemsJson(itemsJson, order.id)
        return OrderDetails(order, items)
    }

    companion object {
        fun fromOrderDetails(orderDetails: OrderDetails): SavedReceipt {
            val jsonArray = JSONArray()
            orderDetails.items.forEach { item ->
                val obj = JSONObject().apply {
                    put("productId", item.productId)
                    put("productName", item.productName)
                    put("unitPrice", item.unitPrice)
                    put("costPrice", item.costPrice)
                    put("quantity", item.quantity)
                    put("subtotal", item.subtotal)
                    put("currency", item.currency)
                }
                jsonArray.put(obj)
            }

            return SavedReceipt(
                originalOrderId = orderDetails.order.id,
                invoiceNumber = orderDetails.order.invoiceNumber,
                originalTimestamp = orderDetails.order.timestamp,
                clearedTimestamp = System.currentTimeMillis(),
                subtotal = orderDetails.order.subtotal,
                discount = orderDetails.order.discount,
                grandTotal = orderDetails.order.grandTotal,
                paymentMethod = orderDetails.order.paymentMethod,
                amountTendered = orderDetails.order.amountTendered,
                changeAmount = orderDetails.order.changeAmount,
                customerName = orderDetails.order.customerName,
                customerPhone = orderDetails.order.customerPhone,
                note = orderDetails.order.note,
                status = orderDetails.order.status,
                itemsJson = jsonArray.toString(),
                currency = orderDetails.order.currency,
                totalMmk = orderDetails.order.totalMmk,
                totalThb = orderDetails.order.totalThb,
                exchangeRate = orderDetails.order.exchangeRate
            )
        }

        fun parseItemsJson(jsonStr: String, orderId: Long = 0L): List<OrderItem> {
            if (jsonStr.isBlank()) return emptyList()
            val list = mutableListOf<OrderItem>()
            try {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawCurr = obj.optString("currency", "")
                    val pName = obj.optString("productName", "")
                    val effectiveCurr = if (rawCurr.isNotBlank()) {
                        if (com.example.data.isThbCurrencyString(rawCurr)) "THB" else "MMK"
                    } else if (com.example.data.isThbCurrencyString(pName)) {
                        "THB"
                    } else {
                        "MMK"
                    }
                    list.add(
                        OrderItem(
                            orderId = orderId,
                            productId = obj.optLong("productId", 0L),
                            productName = pName,
                            unitPrice = obj.optLong("unitPrice", 0L),
                            costPrice = obj.optLong("costPrice", 0L),
                            quantity = obj.optInt("quantity", 1),
                            subtotal = obj.optLong("subtotal", 0L),
                            currency = effectiveCurr
                        )
                    )
                }
            } catch (_: Exception) {
                // Return empty if parse failed
            }
            return list
        }
    }
}
