package com.example.util

import com.example.data.ShopProfile
import com.example.data.entity.Order
import com.example.data.entity.OrderItem
import com.example.data.entity.OrderStatus
import com.example.data.entity.PaymentMethod
import com.example.data.entity.Product
import com.example.data.entity.SavedReceipt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReceiptCalculation(
    val id: Long,
    val invoiceNumber: String,
    val timestamp: Long,
    val formattedDateTime: String,
    val status: OrderStatus,
    val paymentMethod: PaymentMethod,
    val currency: String, // Original checkout currency "MMK" or "THB"
    val customerName: String,
    val customerPhone: String,
    val note: String,
    val itemCount: Int,
    val totalUnitsSold: Int,
    val sellingPriceMmk: Long,
    val sellingPriceThb: Long,
    val buyingPriceMmk: Long,
    val buyingPriceThb: Long,
    val discountMmk: Long,
    val discountThb: Long,
    val estimatedNetProfitMmk: Long,
    val estimatedNetProfitThb: Long,
    val profitMarginPercent: Int,
    val mmkSellingRate: String,
    val mmkBuyingRate: String,
    val thbSellingRate: String,
    val thbBuyingRate: String
) {
    val effectiveTotalMmk: Long get() = sellingPriceMmk
    val effectiveTotalThb: Long get() = sellingPriceThb
    val costMmk: Long get() = buyingPriceMmk
    val costThb: Long get() = buyingPriceThb
    val profitMmk: Long get() = estimatedNetProfitMmk
    val profitThb: Long get() = estimatedNetProfitThb
}

data class UnsoldStockSummary(
    val unsoldProductCount: Int,
    val unsoldTotalQuantity: Int,
    val unsoldSellingValueMmk: Long,
    val unsoldSellingValueThb: Long,
    val unsoldBuyingValueMmk: Long,
    val unsoldBuyingValueThb: Long,
    val potentialProfitMmk: Long,
    val potentialProfitThb: Long,
    val mmkSellingRate: String,
    val mmkBuyingRate: String,
    val thbSellingRate: String,
    val thbBuyingRate: String,
    val nativeMmkSellingValue: Long = 0L,
    val nativeMmkBuyingValue: Long = 0L,
    val nativeThbSellingValue: Long = 0L,
    val nativeThbBuyingValue: Long = 0L
)

object ReceiptCalculator {

    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

    fun isThbOrderItem(item: OrderItem, product: Product? = null): Boolean {
        if (product != null && com.example.data.isThbProduct(product)) {
            if (!com.example.data.isMmkCurrencyString(item.currency)) return true
        }
        return com.example.data.isThbCurrencyString(item.currency) ||
                com.example.data.isThbCurrencyString(item.productName)
    }

    fun isMmkOrderItem(item: OrderItem, product: Product? = null): Boolean {
        return !isThbOrderItem(item, product)
    }

    fun resolveOrderRates(
        orderCurrency: String,
        orderExchangeRate: Double,
        shopProfile: ShopProfile = ShopProfile()
    ): Pair<Double, Double> {
        return resolveOrderRates(orderCurrency, orderExchangeRate, 0L, 0L, shopProfile)
    }

    fun resolveOrderRates(
        orderCurrency: String,
        orderExchangeRate: Double,
        totalMmk: Long,
        totalThb: Long,
        shopProfile: ShopProfile = ShopProfile()
    ): Pair<Double, Double> {
        // Priority 1: Exact exchange rate recorded on the receipt/order at checkout time
        if (orderExchangeRate > 0.0) {
            val thbToMmk = com.example.data.toThbToMmkRate(orderExchangeRate)
            val mmkToThb = com.example.data.toMmkToThbRate(orderExchangeRate)
            return Pair(thbToMmk, mmkToThb)
        }

        // Priority 2: Direct ratio of recorded total MMK and total THB from the receipt
        if (totalMmk > 0L && totalThb > 0L) {
            val thbToMmk = totalMmk.toDouble() / totalThb.toDouble()
            val mmkToThb = totalThb.toDouble() / totalMmk.toDouble()
            return Pair(thbToMmk, mmkToThb)
        }

        // Fallback for legacy orders without stored exchange rate
        val thbSell = shopProfile.thbSellingRate.replace(",", ".").toDoubleOrNull() ?: 0.0
        val thbBuy = shopProfile.thbBuyingRate.replace(",", ".").toDoubleOrNull() ?: thbSell
        val mmkSell = shopProfile.mmkSellingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbSell > 0) 1.0 / thbSell else 0.0)
        val mmkBuy = shopProfile.mmkBuyingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbBuy > 0) 1.0 / thbBuy else mmkSell)

        val effectiveMmkToThb = if (mmkSell > 0) com.example.data.toMmkToThbRate(mmkSell) else if (thbSell > 0) 1.0 / thbSell else 0.0
        val effectiveThbToMmk = if (mmkBuy > 0) com.example.data.toThbToMmkRate(mmkBuy) else if (thbBuy > 0) thbBuy else 0.0

        return Pair(effectiveThbToMmk, effectiveMmkToThb)
    }

    fun calculateFromOrder(
        order: Order,
        products: List<Product> = emptyList(),
        shopProfile: ShopProfile = ShopProfile()
    ): ReceiptCalculation {
        return calculateFromOrder(order, emptyList(), products, shopProfile)
    }

    fun calculateFromOrder(
        order: Order,
        items: List<OrderItem>,
        products: List<Product>,
        shopProfile: ShopProfile
    ): ReceiptCalculation {
        val productMap = products.associateBy { it.id }

        val (rateThbToMmk, rateMmkToThb) = resolveOrderRates(order.currency, order.exchangeRate, order.totalMmk, order.totalThb, shopProfile)

        val discountMmk = if (order.discount <= 0L) 0L else if (order.currency == "THB") {
            if (rateThbToMmk > 0) Math.round(order.discount * rateThbToMmk) else 0L
        } else {
            order.discount
        }
        val discountThb = if (order.discount <= 0L) 0L else if (order.currency == "THB") {
            order.discount
        } else {
            if (rateMmkToThb > 0) Math.round(order.discount * rateMmkToThb) else 0L
        }

        val totalUnits = items.sumOf { it.quantity }

        // Calculate item selling, buying/cost, and profit per receipt item
        var totalSellingItemsMmk = 0L
        var totalSellingItemsThb = 0L
        var totalRawProfitMmk = 0L
        var totalRawProfitThb = 0L
        var totalCostMmk = 0L
        var totalCostThb = 0L

        if (items.isNotEmpty()) {
            items.forEach { item ->
                val prod = productMap[item.productId]
                val costPrice = if (item.costPrice > 0L) item.costPrice else (prod?.costPrice ?: 0L)
                val isThbItem = isThbOrderItem(item, prod)

                val unitSellMmk = if (isThbItem) {
                    if (rateThbToMmk > 0) Math.round(item.unitPrice * rateThbToMmk) else 0L
                } else {
                    item.unitPrice
                }
                val unitSellThb = if (isThbItem) {
                    item.unitPrice
                } else {
                    if (rateMmkToThb > 0) Math.round(item.unitPrice * rateMmkToThb) else 0L
                }

                val unitCostMmk = if (isThbItem) {
                    if (rateThbToMmk > 0) Math.round(costPrice * rateThbToMmk) else 0L
                } else {
                    costPrice
                }
                val unitCostThb = if (isThbItem) {
                    costPrice
                } else {
                    if (rateMmkToThb > 0) Math.round(costPrice * rateMmkToThb) else 0L
                }

                val itemSellMmk = unitSellMmk * item.quantity
                val itemSellThb = unitSellThb * item.quantity
                val itemCostMmk = unitCostMmk * item.quantity
                val itemCostThb = unitCostThb * item.quantity

                val unitProfit = item.unitPrice - costPrice
                val itemProfitMmk = if (isThbItem) {
                    if (rateThbToMmk > 0) Math.round(unitProfit * rateThbToMmk) * item.quantity else 0L
                } else {
                    unitProfit * item.quantity
                }
                val itemProfitThb = if (isThbItem) {
                    unitProfit * item.quantity
                } else {
                    if (rateMmkToThb > 0) Math.round(unitProfit * rateMmkToThb) * item.quantity else 0L
                }

                totalSellingItemsMmk += itemSellMmk
                totalSellingItemsThb += itemSellThb
                totalCostMmk += itemCostMmk
                totalCostThb += itemCostThb
                totalRawProfitMmk += itemProfitMmk
                totalRawProfitThb += itemProfitThb
            }
        }

        val sellingMmk = if (order.totalMmk > 0L) {
            order.totalMmk
        } else if (items.isNotEmpty() && totalSellingItemsMmk > 0L) {
            (totalSellingItemsMmk - discountMmk).coerceAtLeast(0L)
        } else {
            val eff = order.getEffectiveTotalMmk()
            if (eff > 0L) eff else if (order.currency == "THB" && rateThbToMmk > 0) Math.round(order.grandTotal * rateThbToMmk) else order.grandTotal
        }

        val sellingThb = if (order.totalThb > 0L) {
            order.totalThb
        } else if (items.isNotEmpty() && totalSellingItemsThb > 0L) {
            (totalSellingItemsThb - discountThb).coerceAtLeast(0L)
        } else {
            val eff = order.getEffectiveTotalThb()
            if (eff > 0L) eff else if (order.currency != "THB" && rateMmkToThb > 0) Math.round(sellingMmk * rateMmkToThb) else order.grandTotal
        }

        if (items.isEmpty()) {
            val estimatedMargin = 0.20
            totalRawProfitMmk = Math.round(sellingMmk * estimatedMargin)
            totalRawProfitThb = Math.round(sellingThb * estimatedMargin)
            totalCostMmk = (sellingMmk - totalRawProfitMmk).coerceAtLeast(0L)
            totalCostThb = (sellingThb - totalRawProfitThb).coerceAtLeast(0L)
        }

        val netProfitMmk = (totalRawProfitMmk - discountMmk).coerceAtLeast(0L)
        val netProfitThb = if (totalRawProfitThb > 0L || items.isNotEmpty()) {
            (totalRawProfitThb - discountThb).coerceAtLeast(0L)
        } else {
            if (rateMmkToThb > 0) Math.round(netProfitMmk * rateMmkToThb) else 0L
        }

        val buyingMmk = if (totalCostMmk > 0L) totalCostMmk else (sellingMmk - netProfitMmk).coerceAtLeast(0L)
        val buyingThb = if (totalCostThb > 0L) totalCostThb else (if (rateMmkToThb > 0) Math.round(buyingMmk * rateMmkToThb) else 0L)

        val marginPercent = if (sellingMmk > 0) {
            ((netProfitMmk.toDouble() / sellingMmk) * 100).toInt()
        } else if (sellingThb > 0) {
            ((netProfitThb.toDouble() / sellingThb) * 100).toInt()
        } else {
            0
        }

        val dateStr = try {
            dateTimeFormat.format(Date(order.timestamp))
        } catch (e: Exception) {
            ""
        }

        val resolvedMmkRateStr = if (rateMmkToThb > 0) {
            String.format(Locale.US, "%.6f", rateMmkToThb).trimEnd('0').trimEnd('.')
        } else ""

        val resolvedThbRateStr = if (rateThbToMmk > 0) {
            String.format(Locale.US, "%.4f", rateThbToMmk).trimEnd('0').trimEnd('.')
        } else ""

        return ReceiptCalculation(
            id = order.id,
            invoiceNumber = order.invoiceNumber,
            timestamp = order.timestamp,
            formattedDateTime = dateStr,
            status = order.status,
            paymentMethod = order.paymentMethod,
            currency = order.currency,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            note = order.note,
            itemCount = items.size,
            totalUnitsSold = totalUnits,
            sellingPriceMmk = sellingMmk,
            sellingPriceThb = sellingThb,
            buyingPriceMmk = buyingMmk,
            buyingPriceThb = buyingThb,
            discountMmk = discountMmk,
            discountThb = discountThb,
            estimatedNetProfitMmk = netProfitMmk,
            estimatedNetProfitThb = netProfitThb,
            profitMarginPercent = marginPercent,
            mmkSellingRate = resolvedMmkRateStr,
            mmkBuyingRate = resolvedMmkRateStr,
            thbSellingRate = resolvedThbRateStr,
            thbBuyingRate = resolvedThbRateStr
        )
    }

    fun calculateFromSavedReceipt(
        receipt: SavedReceipt,
        products: List<Product>,
        shopProfile: ShopProfile
    ): ReceiptCalculation {
        val items = SavedReceipt.parseItemsJson(receipt.itemsJson)
        val productMap = products.associateBy { it.id }

        val (rateThbToMmk, rateMmkToThb) = resolveOrderRates(receipt.currency, receipt.exchangeRate, receipt.totalMmk, receipt.totalThb, shopProfile)

        val discountMmk = if (receipt.discount <= 0L) 0L else if (receipt.currency == "THB") {
            if (rateThbToMmk > 0) Math.round(receipt.discount * rateThbToMmk) else 0L
        } else {
            receipt.discount
        }
        val discountThb = if (receipt.discount <= 0L) 0L else if (receipt.currency == "THB") {
            receipt.discount
        } else {
            if (rateMmkToThb > 0) Math.round(receipt.discount * rateMmkToThb) else 0L
        }

        val totalUnits = items.sumOf { it.quantity }

        var totalSellingItemsMmk = 0L
        var totalSellingItemsThb = 0L
        var totalRawProfitMmk = 0L
        var totalRawProfitThb = 0L
        var totalCostMmk = 0L
        var totalCostThb = 0L

        items.forEach { item ->
            val prod = productMap[item.productId]
            val costPrice = if (item.costPrice > 0L) item.costPrice else (prod?.costPrice ?: 0L)
            val isThbItem = isThbOrderItem(item, prod)

            val unitSellMmk = if (isThbItem) {
                if (rateThbToMmk > 0) Math.round(item.unitPrice * rateThbToMmk) else 0L
            } else {
                item.unitPrice
            }
            val unitSellThb = if (isThbItem) {
                item.unitPrice
            } else {
                if (rateMmkToThb > 0) Math.round(item.unitPrice * rateMmkToThb) else 0L
            }

            val unitCostMmk = if (isThbItem) {
                if (rateThbToMmk > 0) Math.round(costPrice * rateThbToMmk) else 0L
            } else {
                costPrice
            }
            val unitCostThb = if (isThbItem) {
                costPrice
            } else {
                if (rateMmkToThb > 0) Math.round(costPrice * rateMmkToThb) else 0L
            }

            val itemSellMmk = unitSellMmk * item.quantity
            val itemSellThb = unitSellThb * item.quantity
            val itemCostMmk = unitCostMmk * item.quantity
            val itemCostThb = unitCostThb * item.quantity

            val unitProfit = item.unitPrice - costPrice
            val itemProfitMmk = if (isThbItem) {
                if (rateThbToMmk > 0) Math.round(unitProfit * rateThbToMmk) * item.quantity else 0L
            } else {
                unitProfit * item.quantity
            }
            val itemProfitThb = if (isThbItem) {
                unitProfit * item.quantity
            } else {
                if (rateMmkToThb > 0) Math.round(unitProfit * rateMmkToThb) * item.quantity else 0L
            }

            totalSellingItemsMmk += itemSellMmk
            totalSellingItemsThb += itemSellThb
            totalCostMmk += itemCostMmk
            totalCostThb += itemCostThb
            totalRawProfitMmk += itemProfitMmk
            totalRawProfitThb += itemProfitThb
        }

        val sellingMmk = if (receipt.totalMmk > 0L) {
            receipt.totalMmk
        } else if (items.isNotEmpty() && totalSellingItemsMmk > 0L) {
            (totalSellingItemsMmk - discountMmk).coerceAtLeast(0L)
        } else {
            val eff = receipt.getEffectiveTotalMmk()
            if (eff > 0L) eff else if (com.example.data.isThbCurrencyString(receipt.currency) && rateThbToMmk > 0) Math.round(receipt.grandTotal * rateThbToMmk) else receipt.grandTotal
        }

        val sellingThb = if (receipt.totalThb > 0L) {
            receipt.totalThb
        } else if (items.isNotEmpty() && totalSellingItemsThb > 0L) {
            (totalSellingItemsThb - discountThb).coerceAtLeast(0L)
        } else {
            val eff = receipt.getEffectiveTotalThb()
            if (eff > 0L) eff else if (!com.example.data.isThbCurrencyString(receipt.currency) && rateMmkToThb > 0) Math.round(sellingMmk * rateMmkToThb) else receipt.grandTotal
        }

        if (items.isEmpty()) {
            val estimatedMargin = 0.20
            totalRawProfitMmk = Math.round(sellingMmk * estimatedMargin)
            totalRawProfitThb = Math.round(sellingThb * estimatedMargin)
            totalCostMmk = (sellingMmk - totalRawProfitMmk).coerceAtLeast(0L)
            totalCostThb = (sellingThb - totalRawProfitThb).coerceAtLeast(0L)
        }

        val netProfitMmk = (totalRawProfitMmk - discountMmk).coerceAtLeast(0L)
        val netProfitThb = if (totalRawProfitThb > 0L || items.isNotEmpty()) {
            (totalRawProfitThb - discountThb).coerceAtLeast(0L)
        } else {
            if (rateMmkToThb > 0) Math.round(netProfitMmk * rateMmkToThb) else 0L
        }

        val buyingMmk = if (totalCostMmk > 0L) totalCostMmk else (sellingMmk - netProfitMmk).coerceAtLeast(0L)
        val buyingThb = if (totalCostThb > 0L) totalCostThb else if (sellingThb > 0L) (sellingThb - netProfitThb).coerceAtLeast(0L) else (if (rateMmkToThb > 0) Math.round(buyingMmk * rateMmkToThb) else 0L)

        val marginPercent = if (sellingMmk > 0) {
            ((netProfitMmk.toDouble() / sellingMmk) * 100).toInt()
        } else if (sellingThb > 0) {
            ((netProfitThb.toDouble() / sellingThb) * 100).toInt()
        } else {
            0
        }

        val dateStr = try {
            dateTimeFormat.format(Date(if (receipt.clearedTimestamp > 0) receipt.clearedTimestamp else receipt.originalTimestamp))
        } catch (e: Exception) {
            ""
        }

        val resolvedMmkRateStr = if (rateMmkToThb > 0) {
            String.format(Locale.US, "%.6f", rateMmkToThb).trimEnd('0').trimEnd('.')
        } else ""

        val resolvedThbRateStr = if (rateThbToMmk > 0) {
            String.format(Locale.US, "%.4f", rateThbToMmk).trimEnd('0').trimEnd('.')
        } else ""

        return ReceiptCalculation(
            id = receipt.id,
            invoiceNumber = receipt.invoiceNumber,
            timestamp = if (receipt.clearedTimestamp > 0) receipt.clearedTimestamp else receipt.originalTimestamp,
            formattedDateTime = dateStr,
            status = receipt.status,
            paymentMethod = receipt.paymentMethod,
            currency = receipt.currency,
            customerName = receipt.customerName,
            customerPhone = receipt.customerPhone,
            note = receipt.note,
            itemCount = items.size,
            totalUnitsSold = totalUnits,
            sellingPriceMmk = sellingMmk,
            sellingPriceThb = sellingThb,
            buyingPriceMmk = buyingMmk,
            buyingPriceThb = buyingThb,
            discountMmk = discountMmk,
            discountThb = discountThb,
            estimatedNetProfitMmk = netProfitMmk,
            estimatedNetProfitThb = netProfitThb,
            profitMarginPercent = marginPercent,
            mmkSellingRate = resolvedMmkRateStr,
            mmkBuyingRate = resolvedMmkRateStr,
            thbSellingRate = resolvedThbRateStr,
            thbBuyingRate = resolvedThbRateStr
        )
    }

    /**
     * Unsold stock calculation strictly restricted to remaining, unsold in-stock products (stockQuantity > 0).
     * Sold products and receipt items are strictly EXCLUDED from this calculation!
     * Uses MMK and THB exchange rates:
     * - MMK products are converted to THB using mmkSellingRate (မြန်မာကျပ်ရောင်းဈေး)
     * - THB products are converted to MMK using mmkBuyingRate (မြန်မာကျပ်ဝယ်ဈေး)
     */
    fun calculateUnsoldStock(
        products: List<Product>,
        shopProfile: ShopProfile
    ): UnsoldStockSummary {
        val thbSell = shopProfile.thbSellingRate.replace(",", ".").toDoubleOrNull() ?: 0.0
        val thbBuy = shopProfile.thbBuyingRate.replace(",", ".").toDoubleOrNull() ?: thbSell

        val mmkSell = shopProfile.mmkSellingRate.replace(",", ".").toDoubleOrNull()
            ?: (if (thbSell > 0.0) 1.0 / thbSell else 0.0)
        val mmkBuy = shopProfile.mmkBuyingRate.replace(",", ".").toDoubleOrNull()
            ?: (if (thbBuy > 0.0) 1.0 / thbBuy else mmkSell)

        val mmkToThbRate = if (mmkSell > 0.0) com.example.data.toMmkToThbRate(mmkSell) else if (thbSell > 0.0) (1.0 / com.example.data.toThbToMmkRate(thbSell)) else 0.0
        val thbToMmkRate = if (mmkBuy > 0.0) com.example.data.toThbToMmkRate(mmkBuy) else if (thbBuy > 0.0) com.example.data.toThbToMmkRate(thbBuy) else 0.0

        // Filter strictly to unsold remaining in-stock products
        val unsold = products.filter { it.stockQuantity > 0 }
        val count = unsold.size
        val totalQty = unsold.sumOf { it.stockQuantity }

        var nativeMmkSell = 0L
        var nativeMmkBuy = 0L
        var nativeThbSell = 0L
        var nativeThbBuy = 0L

        var sellValMmk = 0L
        var sellValThb = 0L
        var buyValMmk = 0L
        var buyValThb = 0L

        unsold.forEach { p ->
            val isThb = com.example.data.isThbProduct(p)
            val itemSell = p.sellingPrice * p.stockQuantity
            val itemBuy = p.costPrice * p.stockQuantity

            if (isThb) {
                nativeThbSell += itemSell
                nativeThbBuy += itemBuy
                sellValThb += itemSell
                buyValThb += itemBuy

                // Convert THB -> MMK using mmkBuyingRate (မြန်မာကျပ်ဝယ်ဈေး)
                val convSellMmk = if (thbToMmkRate > 0.0) Math.round(itemSell.toDouble() * thbToMmkRate) else 0L
                val convBuyMmk = if (thbToMmkRate > 0.0) Math.round(itemBuy.toDouble() * thbToMmkRate) else 0L
                sellValMmk += convSellMmk
                buyValMmk += convBuyMmk
            } else {
                nativeMmkSell += itemSell
                nativeMmkBuy += itemBuy
                sellValMmk += itemSell
                buyValMmk += itemBuy

                // Convert MMK -> THB using mmkSellingRate (မြန်မာကျပ်ရောင်းဈေး)
                val convSellThb = if (mmkToThbRate > 0.0) Math.round(itemSell.toDouble() * mmkToThbRate) else 0L
                val convBuyThb = if (mmkToThbRate > 0.0) Math.round(itemBuy.toDouble() * mmkToThbRate) else 0L
                sellValThb += convSellThb
                buyValThb += convBuyThb
            }
        }

        val profitMmk = (sellValMmk - buyValMmk).coerceAtLeast(0L)
        val profitThb = (sellValThb - buyValThb).coerceAtLeast(0L)

        val resolvedMmkSellStr = shopProfile.mmkSellingRate.ifBlank {
            if (mmkSell > 0.0) String.format(Locale.US, "%.5f", mmkSell) else ""
        }
        val resolvedMmkBuyStr = shopProfile.mmkBuyingRate.ifBlank {
            if (mmkBuy > 0.0) String.format(Locale.US, "%.5f", mmkBuy) else ""
        }

        return UnsoldStockSummary(
            unsoldProductCount = count,
            unsoldTotalQuantity = totalQty,
            unsoldSellingValueMmk = sellValMmk,
            unsoldSellingValueThb = sellValThb,
            unsoldBuyingValueMmk = buyValMmk,
            unsoldBuyingValueThb = buyValThb,
            potentialProfitMmk = profitMmk,
            potentialProfitThb = profitThb,
            mmkSellingRate = resolvedMmkSellStr,
            mmkBuyingRate = resolvedMmkBuyStr,
            thbSellingRate = shopProfile.thbSellingRate,
            thbBuyingRate = shopProfile.thbBuyingRate,
            nativeMmkSellingValue = nativeMmkSell,
            nativeMmkBuyingValue = nativeMmkBuy,
            nativeThbSellingValue = nativeThbSell,
            nativeThbBuyingValue = nativeThbBuy
        )
    }
}
