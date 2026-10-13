package com.example.data

import android.content.Context
import com.example.data.dao.OrderDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SavedReceiptDao
import com.example.data.entity.Order
import com.example.data.entity.OrderItem
import com.example.data.entity.OrderStatus
import com.example.data.entity.PaymentMethod
import com.example.data.entity.Product
import com.example.data.entity.SavedReceipt
import com.example.util.ProductImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun isThbCurrencyString(text: String): Boolean {
    val t = text.trim()
    return t.equals("THB", ignoreCase = true) ||
            t.equals("TH", ignoreCase = true) ||
            t.contains("THB", ignoreCase = true) ||
            t.contains("ထိုင်းငွေ (ဘတ်)", ignoreCase = true) ||
            t.contains("ထိုင်းငွေ", ignoreCase = true) ||
            t.contains("ဘတ်") ||
            t.contains("Baht", ignoreCase = true) ||
            t.contains("฿")
}

fun isMmkCurrencyString(text: String): Boolean {
    val t = text.trim()
    if (isThbCurrencyString(t) && !t.contains("MMK", ignoreCase = true) && !t.contains("ကျပ်") && !t.contains("Kyat", ignoreCase = true)) {
        return false
    }
    return t.isEmpty() ||
            t.equals("MMK", ignoreCase = true) ||
            t.contains("MMK", ignoreCase = true) ||
            t.contains("မြန်မာငွေ (ကျပ်)", ignoreCase = true) ||
            t.contains("မြန်မာငွေ", ignoreCase = true) ||
            t.contains("ကျပ်") ||
            t.contains("Kyat", ignoreCase = true) ||
            t.contains("Ks", ignoreCase = true)
}

fun isThbProduct(product: Product): Boolean {
    return isThbCurrencyString(product.currency) ||
            isThbCurrencyString(product.name) ||
            isThbCurrencyString(product.unit)
}

fun isMmkProduct(product: Product): Boolean {
    return !isThbProduct(product) ||
            isMmkCurrencyString(product.currency) ||
            isMmkCurrencyString(product.name) ||
            isMmkCurrencyString(product.unit)
}

/**
 * Converts a rate (which could be MMK->THB like 0.00772 or THB->MMK like 129.43)
 * into MMK per 1 THB (e.g., 129.43).
 */
fun toThbToMmkRate(rate: Double): Double {
    if (rate <= 0.0) return 0.0
    return if (rate < 1.0) (1.0 / rate) else rate
}

/**
 * Converts a rate (which could be MMK->THB like 0.00772 or THB->MMK like 129.43)
 * into THB per 1 MMK (e.g., 0.00772).
 */
fun toMmkToThbRate(rate: Double): Double {
    if (rate <= 0.0) return 0.0
    return if (rate < 1.0) rate else (1.0 / rate)
}

data class CartItem(
    val product: Product,
    val quantity: Int,
    val isWholesale: Boolean = false
) {
    val price: Long get() = if (isWholesale && product.wholesalePrice > 0L) product.wholesalePrice else product.sellingPrice
    val subtotal: Long get() = price * quantity

    val isThbItem: Boolean get() = isThbProduct(product)
    val isMmkItem: Boolean get() = !isThbItem

    /**
     * When checkout currency is MMK (မြန်မာငွေ ကျပ်):
     * If item is THB ("THB", "ထိုင်းငွေ (ဘတ်)", "ဘတ်", "Baht"), convert to MMK using "မြန်မာကျပ်ရောင်းဈေး" (mmkSellingRate / thbSellingRate).
     */
    fun getPriceMmk(rateForThbToMmk: Double): Long {
        return if (isThbItem) {
            val thbToMmk = toThbToMmkRate(rateForThbToMmk)
            if (thbToMmk > 0.0) Math.round(price * thbToMmk) else 0L
        } else {
            price
        }
    }

    /**
     * When checkout currency is THB (ထိုင်းငွေ ဘတ်):
     * If item is MMK ("MMK", "မြန်မာငွေ (ကျပ်)", "ကျပ်", "Kyat"), convert to THB using "မြန်မာကျပ်ဝယ်ဈေး" (mmkBuyingRate / thbBuyingRate).
     */
    fun getPriceThb(rateForMmkToThb: Double): Long {
        return if (isThbItem) {
            price
        } else {
            val mmkToThb = toMmkToThbRate(rateForMmkToThb)
            if (mmkToThb > 0.0) Math.round(price * mmkToThb) else 0L
        }
    }

    fun getSubtotalMmk(rateForThbToMmk: Double): Long = getPriceMmk(rateForThbToMmk) * quantity

    fun getSubtotalThb(rateForMmkToThb: Double): Long = getPriceThb(rateForMmkToThb) * quantity

    fun getProfitMmk(rateForThbToMmk: Double): Long {
        val unitProfit = price - product.costPrice
        return if (isThbItem) {
            val thbToMmk = toThbToMmkRate(rateForThbToMmk)
            if (thbToMmk > 0.0) Math.round(unitProfit * thbToMmk) else 0L
        } else {
            unitProfit
        } * quantity
    }

    fun getProfitThb(rateForMmkToThb: Double): Long {
        val unitProfit = price - product.costPrice
        return if (isThbItem) {
            unitProfit
        } else {
            val mmkToThb = toMmkToThbRate(rateForMmkToThb)
            if (mmkToThb > 0.0) Math.round(unitProfit * mmkToThb) else 0L
        } * quantity
    }
}

data class OrderDetails(
    val order: Order,
    val items: List<OrderItem>
)

data class HeldOrderEntry(
    val items: List<CartItem>,
    val orderId: Long? = null,
    val invoiceNumber: String? = null,
    val isRefundedOrder: Boolean = false
)

enum class SaveReceiptResult {
    SUCCESS,
    DUPLICATE_INVOICE,
    REFUNDED_NOT_ALLOWED,
    FAILED
}

class PosRepository(
    private val productDao: ProductDao,
    private val orderDao: OrderDao,
    private val savedReceiptDao: SavedReceiptDao,
    private val context: Context? = null
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val activeProducts: Flow<List<Product>> = productDao.getActiveProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    val allOrderItems: Flow<List<OrderItem>> = orderDao.getAllOrderItems()
    val savedReceipts: Flow<List<SavedReceipt>> = savedReceiptDao.getAllSavedReceipts()
    val savedReceiptsCount: Flow<Int> = savedReceiptDao.getSavedReceiptsCount()

    suspend fun initDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val prefs = context?.getSharedPreferences("pos_app_prefs", Context.MODE_PRIVATE)
        val isConfigured = prefs?.getBoolean("is_clean_install_v16_configured", false) ?: false

        if (!isConfigured) {
            // Remove all pre-existing sample products, orders, and saved receipts completely
            productDao.clearAllProducts()
            orderDao.deleteAllOrders()
            orderDao.deleteAllOrderItems()
            savedReceiptDao.deleteAllSavedReceipts()

            // Remove any cached backup JSON file from previous sessions
            try {
                val backupFile = java.io.File(context?.filesDir, "pos_data_backup.json")
                if (backupFile.exists()) {
                    backupFile.delete()
                }
            } catch (_: Exception) {}

            // Set clean installation state - clear all Google Sheets & Apps Script credentials/URLs
            prefs?.edit()
                ?.putString("shop_name", "POS System Software")
                ?.putString("shop_eng_name", "POS System Software")
                ?.putString("shop_phone", "")
                ?.putString("shop_address", "")
                ?.putString("shop_footer", "")
                ?.putString("shop_cashier", "")
                ?.putString("thb_selling_rate", "130")
                ?.putString("thb_buying_rate", "128")
                ?.putString("mmk_selling_rate", "0.00769")
                ?.putString("mmk_buying_rate", "0.00781")
                ?.putString("gs_script_url", "")
                ?.putString("gs_sheet_name", "")
                ?.putString("gs_sheet_url", "")
                ?.putString("gs_folder_name", "")
                ?.putString("gs_folder_id", "")
                ?.putString("gs_shop_name", "POS System Software")
                ?.putBoolean("gs_auto_sync_sales", false)
                ?.putBoolean("gs_periodic_auto_sync", false)
                ?.putString("gs_last_sync_status", "စတင်အသုံးပြုရန် အဆင်သင့်ဖြစ်ပါသည်")
                ?.putBoolean("is_clean_install_v16_configured", true)
                ?.putBoolean("is_clean_install_v15_configured", true)
                ?.putBoolean("is_clean_install_v11_configured", true)
                ?.putBoolean("is_auto_sync_default_off_v12", true)
                ?.apply()
        }

        // Seed initial products if product catalog has not been seeded yet or is currently empty
        val isSampleSeeded = prefs?.getBoolean("is_sample_products_v17_seeded", false) ?: false
        if (!isSampleSeeded || productDao.getProductCount() == 0) {
            if (productDao.getProductCount() == 0) {
                val sampleProducts = InitialData.getSampleProducts()
                if (sampleProducts.isNotEmpty()) {
                    productDao.insertProducts(sampleProducts)
                }
            }
            if ((prefs?.getString("thb_selling_rate", "") ?: "").isBlank()) {
                prefs?.edit()
                    ?.putString("thb_selling_rate", "130")
                    ?.putString("thb_buying_rate", "128")
                    ?.putString("mmk_selling_rate", "0.00769")
                    ?.putString("mmk_buying_rate", "0.00781")
                    ?.apply()
            }
            prefs?.edit()?.putBoolean("is_sample_products_v17_seeded", true)?.apply()
        }

        // Normalize any existing orders so that grandTotal is always accurate in Myanmar Kyats
        try {
            val existingOrders = orderDao.getAllOrdersOnce()
            for (o in existingOrders) {
                val effMmk = o.getEffectiveTotalMmk()
                if (o.grandTotal != effMmk || o.totalMmk == 0L) {
                    orderDao.updateOrder(o.copy(grandTotal = effMmk, subtotal = effMmk, totalMmk = effMmk))
                }
            }
        } catch (_: Exception) {}
    }

    private fun parseFlexibleTimestamp(rawDateStr: String): Long {
        val dtStr = rawDateStr.trim()
        if (dtStr.isBlank()) return System.currentTimeMillis()

        // Check Google Visualization Date(yyyy,M,d,H,m,s) format (month is 0-indexed in gviz)
        if (dtStr.startsWith("Date(") && dtStr.endsWith(")")) {
            val inside = dtStr.removePrefix("Date(").removeSuffix(")")
            val nums = inside.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (nums.size >= 3) {
                val cal = java.util.Calendar.getInstance()
                cal.set(
                    nums[0],
                    nums[1],
                    nums[2],
                    nums.getOrElse(3) { 0 },
                    nums.getOrElse(4) { 0 },
                    nums.getOrElse(5) { 0 }
                )
                cal.set(java.util.Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }
        }

        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "dd/MM/yyyy hh:mm a",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "M/d/yyyy HH:mm:ss",
            "M/d/yyyy H:mm:ss",
            "M/d/yyyy H:mm",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "EEE MMM dd yyyy HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val cleaned = if (pattern.startsWith("EEE MMM") && dtStr.contains(" GMT")) {
                    dtStr.substringBefore(" GMT").trim()
                } else {
                    dtStr
                }
                val parsed = SimpleDateFormat(pattern, Locale.US).parse(cleaned)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    private fun JSONObject.optCleanLong(key: String, defaultVal: Long = 0L): Long {
        val raw = opt(key) ?: return defaultVal
        if (raw is Number) return raw.toLong()
        val str = raw.toString().replace(",", "").replace("Ks", "", ignoreCase = true)
            .replace("ကျပ်", "").replace("ဘတ်", "").replace("THB", "", ignoreCase = true)
            .replace("MMK", "", ignoreCase = true).trim()
        return str.toLongOrNull() ?: str.toDoubleOrNull()?.let { Math.round(it) } ?: defaultVal
    }

    private fun JSONObject.optCleanDouble(key: String, defaultVal: Double = 0.0): Double {
        val raw = opt(key) ?: return defaultVal
        if (raw is Number) return raw.toDouble()
        val str = raw.toString().replace(",", "").trim()
        return str.toDoubleOrNull() ?: defaultVal
    }

    suspend fun saveImportedProductsFromSheet(sheetProducts: List<Product>): Int = withContext(Dispatchers.IO) {
        if (sheetProducts.isEmpty()) return@withContext 0
        val existing = productDao.getActiveProducts().first()
        val existingByBarcode = existing.filter { it.barcode.isNotBlank() }.associateBy { it.barcode }
        val existingByName = existing.associateBy { it.name.trim().lowercase() }

        val toUpsert = sheetProducts.map { sp ->
            val match = (if (sp.barcode.isNotBlank()) existingByBarcode[sp.barcode] else null)
                ?: existingByName[sp.name.trim().lowercase()]

            val normalizedIncomingImg = ProductImageHelper.normalizeImportedImageUri(context, sp.imageUri)
            val normalizedExistingImg = ProductImageHelper.normalizeImportedImageUri(context, match?.imageUri)
            val resolvedImg = normalizedIncomingImg.ifBlank {
                normalizedExistingImg.ifBlank {
                    match?.imageUri?.takeIf { !ProductImageHelper.isLocalOnlyImageUri(it) } ?: ""
                }
            }

            if (match != null) {
                match.copy(
                    name = if (sp.name.isNotBlank()) sp.name else match.name,
                    category = if (sp.category.isNotBlank()) sp.category else match.category,
                    costPrice = if (sp.costPrice > 0) sp.costPrice else match.costPrice,
                    sellingPrice = if (sp.sellingPrice > 0) sp.sellingPrice else match.sellingPrice,
                    wholesalePrice = if (sp.wholesalePrice > 0) sp.wholesalePrice else match.wholesalePrice,
                    stockQuantity = sp.stockQuantity,
                    unit = if (sp.unit.isNotBlank()) sp.unit else match.unit,
                    currency = if (sp.currency.isNotBlank()) sp.currency else match.currency,
                    imageUri = resolvedImg,
                    isAvailable = true
                )
            } else {
                sp.copy(
                    imageUri = resolvedImg,
                    isAvailable = true
                )
            }
        }
        productDao.insertProducts(toUpsert)
        toUpsert.size
    }

    suspend fun saveImportedOrdersFromJson(ordersArray: JSONArray?): Int = withContext(Dispatchers.IO) {
        if (ordersArray == null || ordersArray.length() == 0) return@withContext 0
        var importedCount = 0
        val currentProducts = productDao.getActiveProducts().first()
        val productsByName = currentProducts.associateBy { it.name.trim().lowercase() }

        for (i in 0 until ordersArray.length()) {
            val oObj = ordersArray.optJSONObject(i) ?: continue
            val inv = (oObj.optString("invoiceNumber", "").ifBlank { oObj.optString("invoiceNo", "") }).trim()
            if (inv.isBlank()) continue

            val ts = oObj.optLong("timestamp", 0L).takeIf { it > 0L }
                ?: parseFlexibleTimestamp(oObj.optString("dateTime", ""))

            val pmStr = oObj.optString("paymentMethod", "ငွေသား").trim()
            val pm = PaymentMethod.entries.find {
                it.name.equals(pmStr, ignoreCase = true) ||
                it.labelMm.equals(pmStr, ignoreCase = true) ||
                it.labelEn.equals(pmStr, ignoreCase = true)
            } ?: PaymentMethod.CASH

            val statusStr = oObj.optString("status", "COMPLETED").trim()
            val status = if (statusStr.equals("REFUNDED", ignoreCase = true)) OrderStatus.REFUNDED else OrderStatus.COMPLETED

            val rawGrandTotal = oObj.optCleanLong("grandTotal", 0L)
            val totalMmk = oObj.optCleanLong("totalMmk", rawGrandTotal)
            val totalThb = oObj.optCleanLong("totalThb", 0L)
            val grandTotal = if (rawGrandTotal > 0L) rawGrandTotal else totalMmk
            val subtotal = oObj.optCleanLong("subtotal", grandTotal).let { if (it > 0L) it else grandTotal }
            val discount = oObj.optCleanLong("discount", 0L)
            val exchangeRate = oObj.optCleanDouble("exchangeRate", 0.0)
            val currency = oObj.optString("currency", "MMK").ifBlank { "MMK" }

            val existing = orderDao.getOrderByInvoiceNumber(inv)
            if (existing != null) {
                if (existing.grandTotal == 0L && grandTotal > 0L) {
                    orderDao.updateOrder(
                        existing.copy(
                            subtotal = subtotal,
                            discount = discount,
                            grandTotal = grandTotal,
                            totalMmk = totalMmk,
                            totalThb = totalThb,
                            exchangeRate = if (exchangeRate.isNaN()) 0.0 else exchangeRate
                        )
                    )
                }
                continue
            }

            val newOrder = Order(
                invoiceNumber = inv,
                timestamp = ts,
                subtotal = subtotal,
                discount = discount,
                grandTotal = grandTotal,
                paymentMethod = pm,
                amountTendered = oObj.optCleanLong("amountTendered", grandTotal),
                changeAmount = oObj.optCleanLong("changeAmount", 0L),
                customerName = oObj.optString("customerName", ""),
                customerPhone = oObj.optString("customerPhone", ""),
                note = oObj.optString("note", ""),
                status = status,
                currency = currency,
                totalMmk = totalMmk,
                totalThb = totalThb,
                exchangeRate = if (exchangeRate.isNaN()) 0.0 else exchangeRate
            )

            val orderId = orderDao.insertOrder(newOrder)
            if (orderId > 0) {
                val itemsList = mutableListOf<OrderItem>()
                val itemsArr = oObj.optJSONArray("items")
                if (itemsArr != null && itemsArr.length() > 0) {
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.optJSONObject(j) ?: continue
                        val pName = itemObj.optString("productName", "ပစ္စည်း").trim()
                        val matchedProd = productsByName[pName.lowercase()]
                        val qty = itemObj.optInt("quantity", 1).coerceAtLeast(1)
                        val uPrice = itemObj.optCleanLong("unitPrice", matchedProd?.sellingPrice ?: 0L)
                        val cPrice = itemObj.optCleanLong("costPrice", matchedProd?.costPrice ?: 0L)
                        itemsList.add(
                            OrderItem(
                                orderId = orderId,
                                productId = itemObj.optLong("productId", matchedProd?.id ?: 0L),
                                productName = pName,
                                unitPrice = uPrice,
                                costPrice = cPrice,
                                quantity = qty,
                                subtotal = itemObj.optCleanLong("subtotal", uPrice * qty),
                                currency = itemObj.optString("currency", matchedProd?.currency ?: currency)
                            )
                        )
                    }
                } else {
                    val itemsSummary = oObj.optString("itemsSummary", "").trim()
                    if (itemsSummary.isNotBlank()) {
                        val parts = itemsSummary.split(";").map { it.trim() }.filter { it.isNotBlank() }
                        parts.forEach { part ->
                            val xIdx = part.lastIndexOf(" x")
                            val pName = if (xIdx > 0) part.substring(0, xIdx).trim() else part
                            val qty = if (xIdx > 0) part.substring(xIdx + 2).trim().toIntOrNull() ?: 1 else 1
                            val matchedProd = productsByName[pName.lowercase()]
                            val uPrice = matchedProd?.sellingPrice ?: (if (parts.size == 1 && qty > 0) grandTotal / qty else 0L)
                            val cPrice = matchedProd?.costPrice ?: 0L
                            itemsList.add(
                                OrderItem(
                                    orderId = orderId,
                                    productId = matchedProd?.id ?: 0L,
                                    productName = pName,
                                    unitPrice = uPrice,
                                    costPrice = cPrice,
                                    quantity = qty,
                                    subtotal = uPrice * qty,
                                    currency = matchedProd?.currency ?: currency
                                )
                            )
                        }
                    }
                }

                if (itemsList.isNotEmpty()) {
                    orderDao.insertOrderItems(itemsList)
                }
                importedCount++
            }
        }
        importedCount
    }

    suspend fun saveImportedSavedReceiptsFromJson(savedArray: JSONArray?): Int = withContext(Dispatchers.IO) {
        if (savedArray == null || savedArray.length() == 0) return@withContext 0
        var importedCount = 0
        val currentProducts = productDao.getActiveProducts().first()
        val productsByName = currentProducts.associateBy { it.name.trim().lowercase() }

        for (i in 0 until savedArray.length()) {
            val sObj = savedArray.optJSONObject(i) ?: continue
            val inv = (sObj.optString("invoiceNumber", "").ifBlank { sObj.optString("invoiceNo", "") }).trim()
            if (inv.isBlank()) continue

            val existing = savedReceiptDao.getSavedReceiptByInvoiceNumber(inv)
            if (existing != null) continue

            val origTs = sObj.optLong("originalTimestamp", 0L).takeIf { it > 0L }
                ?: parseFlexibleTimestamp(sObj.optString("dateTime", ""))
            val clearedTs = sObj.optLong("clearedTimestamp", 0L).takeIf { it > 0L }
                ?: parseFlexibleTimestamp(sObj.optString("clearedDate", "").ifBlank { sObj.optString("dateTime", "") })

            val pmStr = sObj.optString("paymentMethod", "ငွေသား").trim()
            val pm = PaymentMethod.entries.find {
                it.name.equals(pmStr, ignoreCase = true) ||
                it.labelMm.equals(pmStr, ignoreCase = true) ||
                it.labelEn.equals(pmStr, ignoreCase = true)
            } ?: PaymentMethod.CASH

            val rawGrandTotal = sObj.optCleanLong("grandTotal", 0L)
            val totalMmk = sObj.optCleanLong("totalMmk", rawGrandTotal)
            val totalThb = sObj.optCleanLong("totalThb", 0L)
            val grandTotal = if (rawGrandTotal > 0L) rawGrandTotal else totalMmk
            val subtotal = sObj.optCleanLong("subtotal", grandTotal).let { if (it > 0L) it else grandTotal }
            val discount = sObj.optCleanLong("discount", 0L)
            val exchangeRate = sObj.optCleanDouble("exchangeRate", 0.0)
            val currency = sObj.optString("currency", "MMK").ifBlank { "MMK" }

            var itemsJsonStr = sObj.optString("itemsJson", "").trim()
            if (itemsJsonStr.isBlank()) {
                val itemsSummary = sObj.optString("itemsSummary", "").trim()
                val arr = JSONArray()
                if (itemsSummary.isNotBlank()) {
                    val parts = itemsSummary.split(";").map { it.trim() }.filter { it.isNotBlank() }
                    parts.forEach { part ->
                        val xIdx = part.lastIndexOf(" x")
                        val pName = if (xIdx > 0) part.substring(0, xIdx).trim() else part
                        val qty = if (xIdx > 0) part.substring(xIdx + 2).trim().toIntOrNull() ?: 1 else 1
                        val matchedProd = productsByName[pName.lowercase()]
                        val uPrice = matchedProd?.sellingPrice ?: (if (parts.size == 1 && qty > 0) grandTotal / qty else 0L)
                        val cPrice = matchedProd?.costPrice ?: 0L
                        arr.put(JSONObject().apply {
                            put("productId", matchedProd?.id ?: 0L)
                            put("productName", pName)
                            put("unitPrice", uPrice)
                            put("costPrice", cPrice)
                            put("quantity", qty)
                            put("subtotal", uPrice * qty)
                            put("currency", matchedProd?.currency ?: currency)
                        })
                    }
                }
                itemsJsonStr = arr.toString()
            }

            val saved = SavedReceipt(
                invoiceNumber = inv,
                originalTimestamp = origTs,
                clearedTimestamp = clearedTs,
                subtotal = subtotal,
                discount = discount,
                grandTotal = grandTotal,
                paymentMethod = pm,
                amountTendered = sObj.optLong("amountTendered", grandTotal),
                changeAmount = sObj.optLong("changeAmount", 0L),
                customerName = sObj.optString("customerName", ""),
                customerPhone = sObj.optString("customerPhone", ""),
                note = sObj.optString("note", ""),
                status = OrderStatus.COMPLETED,
                itemsJson = itemsJsonStr,
                currency = currency,
                totalMmk = totalMmk,
                totalThb = totalThb,
                exchangeRate = if (exchangeRate.isNaN()) 0.0 else exchangeRate
            )

            if (savedReceiptDao.insertSavedReceipt(saved) > 0) {
                importedCount++
            }
        }
        importedCount
    }

    fun saveJsonToLocalStorage(jsonString: String) {
        if (jsonString.isBlank()) return
        val prefs = context?.getSharedPreferences("pos_app_prefs", Context.MODE_PRIVATE)
        prefs?.edit()
            ?.putString("local_storage_sync_json", jsonString)
            ?.putLong("local_storage_sync_time", System.currentTimeMillis())
            ?.apply()
        try {
            context?.filesDir?.let { dir ->
                File(dir, "local_storage_sync.json").writeText(jsonString)
            }
        } catch (_: Exception) {}
    }

    fun clearJsonLocalStorage(): Boolean {
        val prefs = context?.getSharedPreferences("pos_app_prefs", Context.MODE_PRIVATE)
        prefs?.edit()
            ?.remove("local_storage_sync_json")
            ?.remove("local_storage_sync_time")
            ?.apply()
        return try {
            context?.filesDir?.let { dir ->
                File(dir, "local_storage_sync.json").takeIf { it.exists() }?.delete()
                File(dir, "pos_data_backup.json").takeIf { it.exists() }?.delete()
            }
            true
        } catch (_: Exception) {
            true
        }
    }

    fun getJsonFromLocalStorage(): String {
        val prefs = context?.getSharedPreferences("pos_app_prefs", Context.MODE_PRIVATE)
        val fromPrefs = prefs?.getString("local_storage_sync_json", null)
        if (!fromPrefs.isNullOrBlank()) return fromPrefs
        return try {
            val file = context?.filesDir?.let { File(it, "local_storage_sync.json") }
            if (file != null && file.exists()) file.readText() else ""
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun restoreFromLocalStorageJson(jsonStr: String = getJsonFromLocalStorage()): Triple<Boolean, String, Map<String, String>> = withContext(Dispatchers.IO) {
        if (jsonStr.isBlank()) {
            return@withContext Triple(false, "LocalStorage ထဲတွင် သိမ်းဆည်းထားသော JSON ဒေတာ မရှိသေးပါ", emptyMap())
        }
        try {
            val syncManager = GoogleSheetsSyncManager()
            val parsed = syncManager.parsePullResponseJson(jsonStr)
            if (!parsed.success) {
                return@withContext Triple(false, parsed.message, emptyMap())
            }
            val prodCount = if (parsed.products.isNotEmpty()) {
                saveImportedProductsFromSheet(parsed.products)
            } else 0
            val orderCount = saveImportedOrdersFromJson(parsed.ordersJsonArray)
            val savedCount = saveImportedSavedReceiptsFromJson(parsed.savedReceiptsJsonArray)

            val summaryMsg = "LocalStorage (JSON) မှ ကုန်ပစ္စည်း ($prodCount) မျိုး၊ အရောင်းဘောက်ချာ ($orderCount) စောင်၊ ရှင်းပြီးပြေစာ ($savedCount) စောင် ပြန်လည်ရယူပြီးပါပြီ"
            Triple(true, summaryMsg, parsed.rates)
        } catch (e: Exception) {
            Triple(false, "JSON ပြန်လည်ရယူရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.localizedMessage ?: e.message}", emptyMap())
        }
    }

    suspend fun forceResetDatabase() = withContext(Dispatchers.IO) {
        productDao.clearAllProducts()
        orderDao.deleteAllOrders()
        orderDao.deleteAllOrderItems()
        savedReceiptDao.deleteAllSavedReceipts()
    }

    suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun seedSampleProductsIfNeeded(): Int = withContext(Dispatchers.IO) {
        val sampleProducts = InitialData.getSampleProducts()
        if (sampleProducts.isEmpty()) return@withContext 0
        val existing = productDao.getActiveProducts().first()
        val existingBarcodes = existing.mapNotNull { it.barcode.takeIf { b -> b.isNotBlank() } }.toSet()
        val existingNames = existing.map { it.name.trim().lowercase() }.toSet()
        val toInsert = sampleProducts.filter { sp ->
            (sp.barcode.isBlank() || sp.barcode !in existingBarcodes) &&
                sp.name.trim().lowercase() !in existingNames
        }
        if (toInsert.isNotEmpty()) {
            productDao.insertProducts(toInsert)
        }
        toInsert.size
    }

    suspend fun getProductById(id: Long): Product? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun adjustStock(productId: Long, delta: Int) = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId) ?: return@withContext
        val newStock = (product.stockQuantity + delta).coerceAtLeast(0)
        productDao.updateStock(productId, newStock)
    }

    suspend fun checkout(
        cartItems: List<CartItem>,
        discount: Long,
        paymentMethod: PaymentMethod,
        amountTendered: Long,
        customerName: String,
        customerPhone: String,
        note: String,
        thbSellRate: Double = 0.0,
        currency: String = "MMK",
        totalMmk: Long = 0L,
        totalThb: Long = 0L,
        exchangeRate: Double = 0.0
    ): OrderDetails = withContext(Dispatchers.IO) {
        val rawRate = if (exchangeRate > 0.0) exchangeRate else (if (thbSellRate > 0.0) thbSellRate else 0.0)
        // Store order.exchangeRate normalized as MMK per 1 THB (so Order.getEffectiveTotalThb/Mmk works consistently)
        val effRate = toThbToMmkRate(rawRate)

        // Calculate dual currency subtotals accurately from all items
        val subtotalInMmk = cartItems.sumOf { it.getSubtotalMmk(effRate) }
        val subtotalInThb = cartItems.sumOf { it.getSubtotalThb(effRate) }

        // Discount in both currencies
        val discountInMmk = if (currency == "MMK") discount else Math.round(discount * effRate)
        val discountInThb = if (currency == "THB") discount else (if (effRate > 0) Math.round(discount / effRate) else 0L)

        // Net grand totals stored for both currencies
        val computedTotalMmk = (subtotalInMmk - discountInMmk).coerceAtLeast(0L)
        val computedTotalThb = (subtotalInThb - discountInThb).coerceAtLeast(0L)

        val finalTotalMmk = if (totalMmk > 0L) totalMmk else computedTotalMmk
        val finalTotalThb = if (totalThb > 0L) totalThb else computedTotalThb

        val activeSubtotal = if (currency == "THB") subtotalInThb else subtotalInMmk
        val activeDiscount = discount
        val activeGrandTotal = if (currency == "THB") finalTotalThb else finalTotalMmk

        val change = if (paymentMethod == PaymentMethod.CASH) {
            (amountTendered - activeGrandTotal).coerceAtLeast(0L)
        } else {
            0L
        }

        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(timestamp))
        var invoiceNumber = "INV-$dateStr"

        // Ensure no duplicate invoice number exists in the database (e.g. from refunded orders)
        var suffixCount = 0
        while (orderDao.getOrderByInvoiceNumber(invoiceNumber) != null) {
            suffixCount++
            invoiceNumber = "INV-$dateStr-${String.format(Locale.US, "%02d", suffixCount)}"
        }

        val order = Order(
            invoiceNumber = invoiceNumber,
            timestamp = timestamp,
            subtotal = activeSubtotal,
            discount = activeDiscount,
            grandTotal = activeGrandTotal,
            paymentMethod = paymentMethod,
            amountTendered = if (paymentMethod == PaymentMethod.CASH) amountTendered else activeGrandTotal,
            changeAmount = change,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            note = note.trim(),
            status = OrderStatus.COMPLETED,
            currency = currency,
            totalMmk = finalTotalMmk,
            totalThb = finalTotalThb,
            exchangeRate = effRate
        )

        val orderId = orderDao.insertOrder(order)
        val savedOrder = order.copy(id = orderId)

        val orderItems = cartItems.map { cartItem ->
            OrderItem(
                orderId = orderId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                unitPrice = cartItem.price,
                costPrice = cartItem.product.costPrice,
                quantity = cartItem.quantity,
                subtotal = cartItem.subtotal,
                currency = if (cartItem.isThbItem) "THB" else "MMK"
            )
        }
        orderDao.insertOrderItems(orderItems)

        // Decrement stock for each product
        for (item in cartItems) {
            val currentProduct = productDao.getProductById(item.product.id)
            if (currentProduct != null) {
                val newStock = (currentProduct.stockQuantity - item.quantity).coerceAtLeast(0)
                productDao.updateStock(currentProduct.id, newStock)
            }
        }

        OrderDetails(savedOrder, orderItems)
    }

    suspend fun getOrderDetails(orderId: Long): OrderDetails? = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext null
        val items = orderDao.getOrderItems(orderId)
        OrderDetails(order, items)
    }

    suspend fun refundOrder(orderId: Long) = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext
        if (order.status == OrderStatus.REFUNDED) return@withContext

        // Restore inventory
        val items = orderDao.getOrderItems(orderId)
        for (item in items) {
            val allProds = productDao.getAllProducts().first()
            val product = productDao.getProductById(item.productId)
                ?: allProds.find { it.name.trim().equals(item.productName.trim(), ignoreCase = true) }
            if (product != null) {
                productDao.updateStock(product.id, product.stockQuantity + item.quantity)
            } else if (item.productName.isNotBlank()) {
                productDao.insertProduct(
                    Product(
                        name = item.productName.trim(),
                        category = "အထွေထွေ",
                        costPrice = item.costPrice,
                        sellingPrice = item.unitPrice,
                        stockQuantity = item.quantity,
                        currency = item.currency,
                        isAvailable = true
                    )
                )
            }
        }

        // Mark refunded
        orderDao.updateOrder(order.copy(status = OrderStatus.REFUNDED))
    }

    suspend fun undoRefundOrder(orderId: Long) = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext
        if (order.status != OrderStatus.REFUNDED) return@withContext

        // Deduct inventory back as the order is restored to completed sale
        val items = orderDao.getOrderItems(orderId)
        for (item in items) {
            val allProds = productDao.getAllProducts().first()
            val product = productDao.getProductById(item.productId)
                ?: allProds.find { it.name.trim().equals(item.productName.trim(), ignoreCase = true) }
            if (product != null) {
                val newStock = (product.stockQuantity - item.quantity).coerceAtLeast(0)
                productDao.updateStock(product.id, newStock)
            }
        }

        // Mark back to COMPLETED
        orderDao.updateOrder(order.copy(status = OrderStatus.COMPLETED))
    }

    suspend fun deleteOrder(orderId: Long) = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId)
        if (order != null && order.status != OrderStatus.REFUNDED) {
            // Restore inventory
            val items = orderDao.getOrderItems(orderId)
            for (item in items) {
                val allProds = productDao.getAllProducts().first()
                val product = productDao.getProductById(item.productId)
                    ?: allProds.find { it.name.trim().equals(item.productName.trim(), ignoreCase = true) }
                if (product != null) {
                    productDao.updateStock(product.id, product.stockQuantity + item.quantity)
                }
            }
        }
        orderDao.deleteOrderById(orderId)
        orderDao.deleteOrderItemsByOrderId(orderId)
    }

    suspend fun isInvoiceNumberAlreadySaved(invoiceNumber: String): Boolean = withContext(Dispatchers.IO) {
        savedReceiptDao.getSavedReceiptByInvoiceNumber(invoiceNumber) != null
    }

    suspend fun clearReceiptAndSaveToNewScreen1(orderDetails: OrderDetails): SaveReceiptResult = withContext(Dispatchers.IO) {
        if (orderDetails.order.status == OrderStatus.REFUNDED) {
            return@withContext SaveReceiptResult.REFUNDED_NOT_ALLOWED
        }

        val invoiceNo = orderDetails.order.invoiceNumber
        val existing = savedReceiptDao.getSavedReceiptByInvoiceNumber(invoiceNo)
        if (existing != null) {
            return@withContext SaveReceiptResult.DUPLICATE_INVOICE
        }

        // Enrich items with costPrice if missing or zero
        val enrichedItems = orderDetails.items.map { item ->
            if (item.costPrice <= 0L) {
                val prod = productDao.getProductById(item.productId)
                if (prod != null && prod.costPrice > 0L) {
                    item.copy(costPrice = prod.costPrice)
                } else {
                    item
                }
            } else {
                item
            }
        }
        val enrichedOrderDetails = orderDetails.copy(items = enrichedItems)

        val savedReceipt = SavedReceipt.fromOrderDetails(enrichedOrderDetails)
        val savedId = savedReceiptDao.insertSavedReceipt(savedReceipt)
        if (savedId <= 0) {
            return@withContext SaveReceiptResult.DUPLICATE_INVOICE
        }
        
        // Delete from active orders & order items so it is removed from active list/history
        if (orderDetails.order.id > 0) {
            orderDao.deleteOrderById(orderDetails.order.id)
            orderDao.deleteOrderItemsByOrderId(orderDetails.order.id)
        }
        SaveReceiptResult.SUCCESS
    }

    suspend fun deleteSavedReceipt(id: Long) = withContext(Dispatchers.IO) {
        savedReceiptDao.deleteSavedReceiptById(id)
    }

    suspend fun getSavedReceiptById(id: Long): SavedReceipt? = withContext(Dispatchers.IO) {
        savedReceiptDao.getSavedReceiptById(id)
    }

    suspend fun deleteAllSavedReceipts() = withContext(Dispatchers.IO) {
        savedReceiptDao.deleteAllSavedReceipts()
    }

    suspend fun restoreSavedReceiptToOrders(savedReceiptId: Long) = withContext(Dispatchers.IO) {
        val savedReceipt = savedReceiptDao.getSavedReceiptById(savedReceiptId) ?: return@withContext
        val order = Order(
            invoiceNumber = savedReceipt.invoiceNumber,
            timestamp = savedReceipt.originalTimestamp,
            subtotal = savedReceipt.subtotal,
            discount = savedReceipt.discount,
            grandTotal = savedReceipt.grandTotal,
            paymentMethod = savedReceipt.paymentMethod,
            amountTendered = savedReceipt.amountTendered,
            changeAmount = savedReceipt.changeAmount,
            customerName = savedReceipt.customerName,
            customerPhone = savedReceipt.customerPhone,
            note = savedReceipt.note,
            status = savedReceipt.status,
            currency = savedReceipt.currency,
            totalMmk = savedReceipt.totalMmk,
            totalThb = savedReceipt.totalThb,
            exchangeRate = savedReceipt.exchangeRate
        )
        val newOrderId = orderDao.insertOrder(order)
        val items = SavedReceipt.parseItemsJson(savedReceipt.itemsJson, newOrderId)
        if (items.isNotEmpty()) {
            orderDao.insertOrderItems(items)
        }
        savedReceiptDao.deleteSavedReceiptById(savedReceiptId)
    }

    suspend fun clearAllCompletedOrders() = withContext(Dispatchers.IO) {
        val allOrders = orderDao.getAllOrdersOnce()
        val completedOrders = allOrders.filter { it.status == OrderStatus.COMPLETED }
        for (order in completedOrders) {
            orderDao.deleteOrderItemsByOrderId(order.id)
            orderDao.deleteOrderById(order.id)
        }
    }

    suspend fun clearAllCompletedOrdersAndSaveToSavedReceipts() = withContext(Dispatchers.IO) {
        val allOrders = orderDao.getAllOrdersOnce()
        val completedOrders = allOrders.filter { it.status == OrderStatus.COMPLETED }
        for (order in completedOrders) {
            val rawItems = orderDao.getOrderItems(order.id)
            val items = rawItems.map { item ->
                if (item.costPrice <= 0L) {
                    val prod = productDao.getProductById(item.productId)
                    if (prod != null && prod.costPrice > 0L) {
                        item.copy(costPrice = prod.costPrice)
                    } else {
                        item
                    }
                } else {
                    item
                }
            }
            val invoiceNo = order.invoiceNumber
            
            // Avoid duplicate voucher number in saved_receipts
            val existing = savedReceiptDao.getSavedReceiptByInvoiceNumber(invoiceNo)
            if (existing == null) {
                val orderDetails = OrderDetails(order, items)
                val savedReceipt = SavedReceipt.fromOrderDetails(orderDetails)
                savedReceiptDao.insertSavedReceipt(savedReceipt)
            }
            
            // Delete from active orders & order items
            orderDao.deleteOrderById(order.id)
            orderDao.deleteOrderItemsByOrderId(order.id)
        }
    }
}
