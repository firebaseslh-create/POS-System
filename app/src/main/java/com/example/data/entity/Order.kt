package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentMethod(val labelMm: String, val labelEn: String) {
    CASH("ငွေသား", "Cash"),
    KPAY("KBZPay", "KPay"),
    WAVEPAY("WavePay", "WavePay"),
    AYAPAY("AYA Pay", "AYA Pay"),
    CREDIT("အကြွေး", "Credit")
}

enum class OrderStatus {
    COMPLETED,
    REFUNDED
}

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
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
    val currency: String = "MMK", // Selected checkout currency ("MMK" or "THB")
    val totalMmk: Long = 0L,      // Total in Myanmar Kyat (ကျပ်)
    val totalThb: Long = 0L,      // Total in Thai Baht (ဘတ်)
    val exchangeRate: Double = 0.0 // Exchange rate (1 THB = X MMK)
) {
    fun getEffectiveTotalMmk(): Long {
        if (totalMmk > 0L) return totalMmk
        if (currency == "THB") {
            val rate = if (exchangeRate > 0) com.example.data.toThbToMmkRate(exchangeRate) else 0.0
            return Math.round(grandTotal * rate)
        }
        return grandTotal
    }

    fun getEffectiveTotalThb(): Long {
        if (totalThb > 0L) return totalThb
        if (currency == "THB") return grandTotal
        val rate = if (exchangeRate > 0) com.example.data.toMmkToThbRate(exchangeRate) else 0.0
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
}
