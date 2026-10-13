package com.example.data

import android.content.Context
import com.example.data.entity.Order
import com.example.data.entity.OrderItem
import com.example.data.entity.Product
import com.example.data.entity.SavedReceipt
import com.example.util.ProductImageHelper
import com.example.util.ReceiptCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PullResult(
    val success: Boolean,
    val message: String,
    val sheetUrl: String = "",
    val products: List<Product> = emptyList(),
    val ordersJsonArray: JSONArray? = null,
    val savedReceiptsJsonArray: JSONArray? = null,
    val rates: Map<String, String> = emptyMap(),
    val shopProfileMap: Map<String, String> = emptyMap(),
    val rawJson: String = ""
)

class GoogleSheetsSyncManager {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
    }

    suspend fun testConnection(config: GoogleSheetsConfig): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) {
            return@withContext Pair(false, "Google Apps Script Web App URL မထည့်ရသေးပါ")
        }

        val payload = JSONObject().apply {
            put("action", "test")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun initSheets(config: GoogleSheetsConfig): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) {
            return@withContext Pair(false, "Google Apps Script Web App URL မထည့်ရသေးပါ")
        }

        val payload = JSONObject().apply {
            put("action", "init_sheets")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun pullAllData(config: GoogleSheetsConfig): PullResult = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank() && config.sheetUrl.isBlank()) {
            return@withContext PullResult(false, "Google Apps Script URL သို့မဟုတ် Google Sheet လင့် မထည့်ရသေးပါ")
        }

        var scriptPullResult: PullResult? = null

        if (config.scriptUrl.isNotBlank()) {
            val trimmedUrl = config.scriptUrl.trim()
            val payload = JSONObject().apply {
                put("action", "pull_all")
                put("sheetName", config.sheetName)
                put("sheetUrl", config.sheetUrl)
                put("folderId", config.folderId)
                put("shopName", config.shopName)
            }

            try {
                val body = payload.toString().toRequestBody(jsonMediaType)
                val postRequest = Request.Builder()
                    .url(trimmedUrl)
                    .post(body)
                    .build()

                var responseText = ""
                var httpSuccess = false

                client.newCall(postRequest).execute().use { response ->
                    httpSuccess = response.isSuccessful
                    responseText = response.body?.string()?.trim() ?: ""
                }

                val looksLikeValidPull = (responseText.startsWith("{") || responseText.startsWith("[")) &&
                    !responseText.contains("Unknown action", ignoreCase = true)

                if (!httpSuccess || !looksLikeValidPull) {
                    val sep = if (trimmedUrl.contains("?")) "&" else "?"
                    val encodedSheet = java.net.URLEncoder.encode(config.sheetName, "UTF-8")
                    val encodedSheetUrl = java.net.URLEncoder.encode(config.sheetUrl, "UTF-8")
                    val encodedFolder = java.net.URLEncoder.encode(config.folderId, "UTF-8")
                    val getUrl = "${trimmedUrl}${sep}action=pull_all&sheetName=${encodedSheet}&sheetUrl=${encodedSheetUrl}&folderId=${encodedFolder}"
                    val getRequest = Request.Builder()
                        .url(getUrl)
                        .get()
                        .build()

                    client.newCall(getRequest).execute().use { getRes ->
                        if (getRes.isSuccessful) {
                            val getBody = getRes.body?.string()?.trim() ?: ""
                            if (getBody.startsWith("{") || getBody.startsWith("[")) {
                                httpSuccess = true
                                responseText = getBody
                            }
                        }
                    }
                }

                if (httpSuccess && (responseText.startsWith("{") || responseText.startsWith("["))) {
                    scriptPullResult = parsePullResponseJson(responseText)
                }
            } catch (_: Exception) {}
        }

        // Also fetch directly from Google Sheet URL (Spreadsheet URL) if provided
        val directResult = if (config.sheetUrl.isNotBlank()) {
            fetchFromSpreadsheetUrlDirectly(config.sheetUrl.trim())
        } else null

        if (scriptPullResult != null && scriptPullResult.success && directResult != null && directResult.success) {
            val mergedProducts = mergeProducts(scriptPullResult.products, directResult.products)
            val mergedOrders = mergeJsonArraysByKey(scriptPullResult.ordersJsonArray, directResult.ordersJsonArray, "invoiceNumber")
            val mergedSaved = mergeJsonArraysByKey(scriptPullResult.savedReceiptsJsonArray, directResult.savedReceiptsJsonArray, "invoiceNumber")
            val mergedRates = directResult.rates + scriptPullResult.rates
            return@withContext scriptPullResult.copy(
                sheetUrl = scriptPullResult.sheetUrl.ifBlank { config.sheetUrl },
                products = mergedProducts,
                ordersJsonArray = mergedOrders,
                savedReceiptsJsonArray = mergedSaved,
                rates = mergedRates
            )
        }

        if (directResult != null && directResult.success) {
            return@withContext directResult.copy(
                sheetUrl = config.sheetUrl
            )
        }

        if (scriptPullResult != null) {
            return@withContext scriptPullResult.copy(
                sheetUrl = scriptPullResult.sheetUrl.ifBlank { config.sheetUrl }
            )
        }

        PullResult(false, "Google Sheets မှ အချက်အလက်များ ဆွဲယူ၍ မရနိုင်သေးပါ")
    }

    private fun mergeProducts(primary: List<Product>, secondary: List<Product>): List<Product> {
        if (secondary.isEmpty()) return primary
        if (primary.isEmpty()) return secondary
        val result = primary.toMutableList()
        val indexByBarcode = mutableMapOf<String, Int>()
        val indexByName = mutableMapOf<String, Int>()
        result.forEachIndexed { idx, p ->
            if (p.barcode.isNotBlank()) indexByBarcode[p.barcode] = idx
            indexByName[p.name.trim().lowercase()] = idx
        }
        for (p in secondary) {
            val matchIdx = (if (p.barcode.isNotBlank()) indexByBarcode[p.barcode] else null)
                ?: indexByName[p.name.trim().lowercase()]
            if (matchIdx != null) {
                val existing = result[matchIdx]
                if ((existing.imageUri.isBlank() || ProductImageHelper.isLocalOnlyImageUri(existing.imageUri)) && p.imageUri.isNotBlank()) {
                    result[matchIdx] = existing.copy(imageUri = p.imageUri)
                }
            } else {
                val newIdx = result.size
                result.add(p)
                if (p.barcode.isNotBlank()) indexByBarcode[p.barcode] = newIdx
                indexByName[p.name.trim().lowercase()] = newIdx
            }
        }
        return result
    }

    private fun mergeJsonArraysByKey(primary: JSONArray?, secondary: JSONArray?, key: String): JSONArray {
        val result = JSONArray()
        val seenKeys = mutableSetOf<String>()
        if (primary != null) {
            for (i in 0 until primary.length()) {
                val obj = primary.optJSONObject(i) ?: continue
                val k = obj.optString(key, "").trim()
                if (k.isNotBlank()) seenKeys.add(k)
                result.put(obj)
            }
        }
        if (secondary != null) {
            for (i in 0 until secondary.length()) {
                val obj = secondary.optJSONObject(i) ?: continue
                val k = obj.optString(key, "").trim()
                if (k.isBlank() || !seenKeys.contains(k)) {
                    if (k.isNotBlank()) seenKeys.add(k)
                    result.put(obj)
                }
            }
        }
        return result
    }

    private fun fetchFromSpreadsheetUrlDirectly(sheetUrl: String): PullResult {
        val match = Regex("/d/([a-zA-Z0-9-_]+)").find(sheetUrl) ?: return PullResult(false, "Invalid Sheet URL")
        val spreadsheetId = match.groupValues[1]

        return try {
            val productsList = mutableListOf<Product>()
            val ordersArray = JSONArray()
            val savedReceiptsArray = JSONArray()
            val ratesMap = mutableMapOf<String, String>()
            var anyFetched = false

            // 1. Fetch Products tab (or default first sheet)
            val productsTable = fetchGvizTable(spreadsheetId, "Products") ?: fetchGvizTable(spreadsheetId, null)
            if (productsTable != null) {
                anyFetched = true
                val headerSignature = extractTableHeaderSignature(productsTable)
                if (!headerSignature.contains("ပြေစာအမှတ်") && !headerSignature.contains("Invoice") && !headerSignature.contains("ရောင်းဈေးနှုန်း")) {
                    parseGvizProducts(productsTable, productsList)
                } else if (headerSignature.contains("ရှင်းလင်းသည့်ရက်စွဲ") || headerSignature.contains("Cleared")) {
                    parseGvizSavedReceipts(productsTable, savedReceiptsArray)
                } else if (headerSignature.contains("ပြေစာအမှတ်") || headerSignature.contains("Invoice")) {
                    parseGvizOrders(productsTable, ordersArray)
                }
            }

            // 2. Fetch Sales_Orders tab
            val ordersTable = fetchGvizTable(spreadsheetId, "Sales_Orders")
            if (ordersTable != null) {
                anyFetched = true
                val headerSignature = extractTableHeaderSignature(ordersTable)
                if (headerSignature.contains("ပြေစာအမှတ်") || headerSignature.contains("Invoice") || headerSignature.contains("ဝယ်သူ") || headerSignature.contains("Customer") || ordersTable.optJSONArray("cols")?.length() ?: 0 >= 12) {
                    if (!headerSignature.contains("ရှင်းလင်းသည့်ရက်စွဲ") && !headerSignature.contains("ဘားကုဒ်")) {
                        parseGvizOrders(ordersTable, ordersArray)
                    }
                }
            }

            // 3. Fetch Saved_Receipts tab
            val savedTable = fetchGvizTable(spreadsheetId, "Saved_Receipts")
            if (savedTable != null) {
                anyFetched = true
                val headerSignature = extractTableHeaderSignature(savedTable)
                if (headerSignature.contains("ရှင်းလင်းသည့်ရက်စွဲ") || headerSignature.contains("Cleared") || (!headerSignature.contains("ဘားကုဒ်") && (savedTable.optJSONArray("cols")?.length() ?: 0) in 12..14)) {
                    parseGvizSavedReceipts(savedTable, savedReceiptsArray)
                }
            }

            // 4. Fetch Exchange_Rates tab
            val ratesTable = fetchGvizTable(spreadsheetId, "Exchange_Rates")
            if (ratesTable != null) {
                anyFetched = true
                parseGvizRates(ratesTable, ratesMap)
            }

            if (!anyFetched) {
                return PullResult(false, "Google Sheet လင့်မှ ဒေတာဖတ်မရပါ")
            }

            val rootJson = JSONObject().apply {
                put("status", "success")
                put("sheetUrl", sheetUrl)
                put("productsCount", productsList.size)
                put("orders", ordersArray)
                put("savedReceipts", savedReceiptsArray)
                put("rates", JSONObject(ratesMap as Map<*, *>))
            }

            PullResult(
                success = true,
                message = "Google Sheet လင့်မှ စာရင်းများ အောင်မြင်စွာ ဆွဲယူပြီးပါပြီ",
                sheetUrl = sheetUrl,
                products = productsList,
                ordersJsonArray = ordersArray,
                savedReceiptsJsonArray = savedReceiptsArray,
                rates = ratesMap,
                rawJson = rootJson.toString(2)
            )
        } catch (e: Exception) {
            PullResult(false, "Google Sheet လင့် ချိတ်ဆက်မှု အမှား: ${e.message}")
        }
    }

    private fun fetchGvizTable(spreadsheetId: String, sheetTab: String?): JSONObject? {
        return try {
            val url = if (!sheetTab.isNullOrBlank()) {
                val encoded = java.net.URLEncoder.encode(sheetTab, "UTF-8")
                "https://docs.google.com/spreadsheets/d/$spreadsheetId/gviz/tq?tqx=out:json&sheet=$encoded"
            } else {
                "https://docs.google.com/spreadsheets/d/$spreadsheetId/gviz/tq?tqx=out:json"
            }
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { res ->
                if (!res.isSuccessful) return null
                val body = res.body?.string() ?: return null
                val startIdx = body.indexOf('{')
                val endIdx = body.lastIndexOf('}')
                if (startIdx < 0 || endIdx <= startIdx) return null
                val json = JSONObject(body.substring(startIdx, endIdx + 1))
                if (json.optString("status") == "error") return null
                json.optJSONObject("table")
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTableHeaderSignature(table: JSONObject): String {
        val sb = StringBuilder()
        val cols = table.optJSONArray("cols")
        if (cols != null) {
            for (i in 0 until cols.length()) {
                sb.append(cols.optJSONObject(i)?.optString("label", "") ?: "").append(" ")
            }
        }
        val rows = table.optJSONArray("rows")
        if (rows != null && rows.length() > 0) {
            val firstRowCells = rows.optJSONObject(0)?.optJSONArray("c")
            if (firstRowCells != null) {
                for (i in 0 until firstRowCells.length()) {
                    sb.append(gvizCellString(firstRowCells, i)).append(" ")
                }
            }
        }
        return sb.toString()
    }

    private fun gvizCellString(cells: JSONArray?, index: Int): String {
        if (cells == null || index < 0 || index >= cells.length()) return ""
        val cell = cells.optJSONObject(index) ?: return ""
        val formatted = cell.optString("f", "").trim()
        if (formatted.isNotBlank() && formatted != "null") return formatted
        if (cell.isNull("v")) return ""
        val raw = cell.opt("v") ?: return ""
        return raw.toString().trim()
    }

    private fun gvizCellLong(cells: JSONArray?, index: Int): Long {
        if (cells == null || index < 0 || index >= cells.length()) return 0L
        val cell = cells.optJSONObject(index) ?: return 0L
        if (!cell.isNull("v")) {
            val raw = cell.opt("v")
            if (raw is Number) return Math.round(raw.toDouble())
        }
        val str = gvizCellString(cells, index)
            .replace(",", "")
            .replace("Ks", "", ignoreCase = true)
            .replace("ကျပ်", "")
            .replace("ဘတ်", "")
            .trim()
        return str.toLongOrNull() ?: str.toDoubleOrNull()?.let { Math.round(it) } ?: 0L
    }

    private fun gvizCellDouble(cells: JSONArray?, index: Int): Double {
        if (cells == null || index < 0 || index >= cells.length()) return 0.0
        val cell = cells.optJSONObject(index) ?: return 0.0
        if (!cell.isNull("v")) {
            val raw = cell.opt("v")
            if (raw is Number) return raw.toDouble()
        }
        val str = gvizCellString(cells, index).replace(",", "").trim()
        return str.toDoubleOrNull() ?: 0.0
    }

    private fun parseGvizProducts(table: JSONObject, outList: MutableList<Product>) {
        val rows = table.optJSONArray("rows") ?: return
        for (i in 0 until rows.length()) {
            val cells = rows.optJSONObject(i)?.optJSONArray("c") ?: continue
            val barcode = gvizCellString(cells, 0)
            val name = gvizCellString(cells, 1)
            if (barcode.isBlank() && name.isBlank()) continue
            if (barcode.contains("ဘားကုဒ်") || barcode.equals("Barcode", ignoreCase = true) ||
                name.contains("ပစ္စည်းအမည်") || name.equals("Product Name", ignoreCase = true)
            ) continue

            var rawImg = gvizCellString(cells, 9)
            if (rawImg.isBlank()) {
                // Check if cell 9 had a raw value or formula in 'v' or 'f'
                val cell9 = cells.optJSONObject(9)
                val vStr = cell9?.optString("v", "")?.trim() ?: ""
                if (vStr.isNotBlank() && vStr != "null") rawImg = vStr
            }
            val normalizedImg = ProductImageHelper.normalizeImportedImageUri(null, rawImg)

            outList.add(
                Product(
                    name = if (name.isNotBlank()) name else "ပစ္စည်း ${i + 1}",
                    barcode = barcode,
                    category = gvizCellString(cells, 2).ifBlank { "အထွေထွေ" },
                    costPrice = gvizCellLong(cells, 3),
                    sellingPrice = gvizCellLong(cells, 4),
                    wholesalePrice = gvizCellLong(cells, 5),
                    stockQuantity = gvizCellLong(cells, 6).toInt(),
                    unit = gvizCellString(cells, 7).ifBlank { "ခု" },
                    currency = gvizCellString(cells, 8).ifBlank { "MMK" },
                    imageUri = normalizedImg.ifBlank { rawImg }
                )
            )
        }
    }

    private fun parseGvizOrders(table: JSONObject, outArray: JSONArray) {
        val rows = table.optJSONArray("rows") ?: return
        for (i in 0 until rows.length()) {
            val cells = rows.optJSONObject(i)?.optJSONArray("c") ?: continue
            val inv = gvizCellString(cells, 0)
            if (inv.isBlank() || inv.contains("ပြေစာအမှတ်") || inv.contains("Invoice", ignoreCase = true)) continue
            val statusStr = gvizCellString(cells, 17).ifBlank { "COMPLETED" }
            val itemsJsonRaw = gvizCellString(cells, 18)
            val obj = JSONObject().apply {
                put("invoiceNumber", inv)
                put("dateTime", gvizCellString(cells, 1))
                put("customerName", gvizCellString(cells, 2))
                put("customerPhone", gvizCellString(cells, 3))
                put("paymentMethod", gvizCellString(cells, 4).ifBlank { "ငွေသား" })
                put("itemsSummary", gvizCellString(cells, 5))
                put("subtotal", gvizCellLong(cells, 6))
                put("discount", gvizCellLong(cells, 7))
                put("grandTotal", gvizCellLong(cells, 8))
                put("currency", gvizCellString(cells, 9).ifBlank { "MMK" })
                put("totalMmk", gvizCellLong(cells, 10))
                put("totalThb", gvizCellLong(cells, 11))
                put("exchangeRate", gvizCellDouble(cells, 12))
                put("profitMmk", gvizCellLong(cells, 13))
                put("profitThb", gvizCellLong(cells, 14))
                put("note", gvizCellString(cells, 15))
                put("status", statusStr)
                if (itemsJsonRaw.startsWith("[")) {
                    try {
                        put("items", JSONArray(itemsJsonRaw))
                    } catch (_: Exception) {}
                }
            }
            outArray.put(obj)
        }
    }

    private fun parseGvizSavedReceipts(table: JSONObject, outArray: JSONArray) {
        val rows = table.optJSONArray("rows") ?: return
        for (i in 0 until rows.length()) {
            val cells = rows.optJSONObject(i)?.optJSONArray("c") ?: continue
            val inv = gvizCellString(cells, 0)
            if (inv.isBlank() || inv.contains("ပြေစာအမှတ်") || inv.contains("Invoice", ignoreCase = true)) continue
            val currStr = gvizCellString(cells, 13).ifBlank { "MMK" }
            val itemsJsonRaw = gvizCellString(cells, 14)
            val obj = JSONObject().apply {
                put("invoiceNumber", inv)
                put("dateTime", gvizCellString(cells, 1))
                put("customerName", gvizCellString(cells, 2))
                put("customerPhone", gvizCellString(cells, 3))
                put("paymentMethod", gvizCellString(cells, 4).ifBlank { "ငွေသား" })
                put("itemsSummary", gvizCellString(cells, 5))
                put("grandTotal", gvizCellLong(cells, 6))
                put("totalMmk", gvizCellLong(cells, 7))
                put("totalThb", gvizCellLong(cells, 8))
                put("exchangeRate", gvizCellDouble(cells, 9))
                put("profitMmk", gvizCellLong(cells, 10))
                put("profitThb", gvizCellLong(cells, 11))
                put("clearedDate", gvizCellString(cells, 12))
                put("currency", currStr)
                if (itemsJsonRaw.isNotBlank()) {
                    put("itemsJson", itemsJsonRaw)
                }
            }
            outArray.put(obj)
        }
    }

    private fun parseGvizRates(table: JSONObject, outMap: MutableMap<String, String>) {
        val rows = table.optJSONArray("rows") ?: return
        for (i in 0 until rows.length()) {
            val cells = rows.optJSONObject(i)?.optJSONArray("c") ?: continue
            val curr = gvizCellString(cells, 0).uppercase()
            val sell = gvizCellString(cells, 1)
            val buy = gvizCellString(cells, 2)
            if (curr.contains("THB") || curr.contains("ဘတ်")) {
                if (sell.isNotBlank()) outMap["thbSellingRate"] = sell
                if (buy.isNotBlank()) outMap["thbBuyingRate"] = buy
            } else if (curr.contains("MMK") || curr.contains("ကျပ်")) {
                if (sell.isNotBlank()) outMap["mmkSellingRate"] = sell
                if (buy.isNotBlank()) outMap["mmkBuyingRate"] = buy
            }
        }
    }

    fun parsePullResponseJson(responseBody: String): PullResult {
        val trimmed = responseBody.trim()
        if (trimmed.isBlank()) {
            return PullResult(false, "တုံ့ပြန်မှု ဒေတာ မရှိပါ")
        }

        return try {
            val resJson = if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                JSONObject().apply {
                    put("status", "success")
                    put("products", arr)
                }
            } else {
                JSONObject(trimmed)
            }

            val status = resJson.optString("status", "success")
            val message = resJson.optString("message", "အချက်အလက်များ JSON format ဖြင့် ရယူပြီးပါပြီ")
            val sheetUrl = resJson.optString("sheetUrl", "")

            if (status == "success" || resJson.has("products") || resJson.has("data") || resJson.has("orders")) {
                val productsList = mutableListOf<Product>()
                val productsArray = resJson.optJSONArray("products") ?: resJson.optJSONArray("data")
                if (productsArray != null) {
                    for (i in 0 until productsArray.length()) {
                        val pObj = productsArray.optJSONObject(i) ?: continue
                        val name = (pObj.optString("name", "").ifBlank { pObj.optString("productName", "") }).trim()
                        val barcode = pObj.optString("barcode", "").trim()
                        if (name.isBlank() && barcode.isBlank()) continue

                        val rawImage = pObj.optString("imageUri", "")
                            .ifBlank { pObj.optString("imageUrl", "") }
                            .ifBlank { pObj.optString("image", "") }
                            .trim()
                        val normalizedImage = ProductImageHelper.normalizeImportedImageUri(null, rawImage).ifBlank { rawImage }

                        productsList.add(
                            Product(
                                name = if (name.isNotBlank()) name else "ပစ္စည်း $i",
                                barcode = barcode,
                                category = pObj.optString("category", "အထွေထွေ").ifBlank { "အထွေထွေ" },
                                costPrice = pObj.optLong("costPrice", 0L),
                                sellingPrice = pObj.optLong("sellingPrice", pObj.optLong("price", 0L)),
                                wholesalePrice = pObj.optLong("wholesalePrice", 0L),
                                stockQuantity = pObj.optInt("stockQuantity", pObj.optInt("stock", 0)),
                                unit = pObj.optString("unit", "ခု").ifBlank { "ခု" },
                                currency = pObj.optString("currency", "MMK").ifBlank { "MMK" },
                                imageUri = normalizedImage
                            )
                        )
                    }
                }

                val ordersArray = resJson.optJSONArray("orders")
                val savedReceiptsArray = resJson.optJSONArray("savedReceipts")

                val ratesMap = mutableMapOf<String, String>()
                val ratesObj = resJson.optJSONObject("rates")
                if (ratesObj != null) {
                    ratesObj.optString("thbSellingRate").takeIf { it.isNotBlank() }?.let { ratesMap["thbSellingRate"] = it }
                    ratesObj.optString("thbBuyingRate").takeIf { it.isNotBlank() }?.let { ratesMap["thbBuyingRate"] = it }
                    ratesObj.optString("mmkSellingRate").takeIf { it.isNotBlank() }?.let { ratesMap["mmkSellingRate"] = it }
                    ratesObj.optString("mmkBuyingRate").takeIf { it.isNotBlank() }?.let { ratesMap["mmkBuyingRate"] = it }
                }

                val shopProfileMap = mutableMapOf<String, String>()
                val shopObj = resJson.optJSONObject("shopProfile")
                if (shopObj != null) {
                    shopObj.optString("shopName").takeIf { it.isNotBlank() }?.let { shopProfileMap["shopName"] = it }
                    shopObj.optString("englishName").takeIf { it.isNotBlank() }?.let { shopProfileMap["englishName"] = it }
                    shopObj.optString("phone").takeIf { it.isNotBlank() }?.let { shopProfileMap["phone"] = it }
                    shopObj.optString("address").takeIf { it.isNotBlank() }?.let { shopProfileMap["address"] = it }
                    shopObj.optString("receiptFooter").takeIf { it.isNotBlank() }?.let { shopProfileMap["receiptFooter"] = it }
                    shopObj.optString("cashierName").takeIf { it.isNotBlank() }?.let { shopProfileMap["cashierName"] = it }
                }

                PullResult(
                    success = true,
                    message = message,
                    sheetUrl = sheetUrl,
                    products = productsList,
                    ordersJsonArray = ordersArray,
                    savedReceiptsJsonArray = savedReceiptsArray,
                    rates = ratesMap,
                    shopProfileMap = shopProfileMap,
                    rawJson = resJson.toString(2)
                )
            } else {
                PullResult(false, message, sheetUrl)
            }
        } catch (e: Exception) {
            PullResult(false, "JSON တုံ့ပြန်မှု ဖတ်မရပါ: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun uploadProductImageToDrive(
        config: GoogleSheetsConfig,
        context: Context,
        product: Product
    ): String? = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext null
        val raw = product.imageUri.trim()
        if (raw.isBlank()) return@withContext null
        val dataUrl = if (raw.startsWith("data:image", ignoreCase = true)) {
            raw
        } else if (ProductImageHelper.isLocalOnlyImageUri(raw)) {
            ProductImageHelper.encodeImageFileOrUriToDataUrl(context, raw, maxSizePx = 420, quality = 80)
        } else {
            return@withContext ProductImageHelper.prepareProductImageForCloudSync(context, product)
        }
        if (dataUrl.isNullOrBlank()) return@withContext null
        val base64Part = dataUrl.substringAfter("base64,", "").trim()
        if (base64Part.isBlank()) return@withContext null

        val safeCode = (product.barcode.ifBlank { product.name }).replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24)
        val fileName = "Product_${safeCode}_${System.currentTimeMillis()}.jpg"
        val payload = JSONObject().apply {
            put("action", "upload_file")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("fileName", fileName)
            put("base64Data", base64Part)
            put("mimeType", "image/jpeg")
        }

        return@withContext try {
            val body = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(config.scriptUrl.trim())
                .post(body)
                .build()
            client.newCall(request).execute().use { res ->
                if (!res.isSuccessful) return@use dataUrl
                val resStr = res.body?.string() ?: return@use dataUrl
                val json = JSONObject(resStr)
                val fileUrl = json.optString("fileUrl", "").trim()
                if (fileUrl.isNotBlank()) {
                    val driveId = ProductImageHelper.extractGoogleDriveFileId(fileUrl)
                    if (!driveId.isNullOrBlank()) {
                        "https://drive.google.com/uc?export=view&id=$driveId"
                    } else {
                        fileUrl
                    }
                } else {
                    dataUrl
                }
            }
        } catch (_: Exception) {
            dataUrl
        }
    }

    suspend fun syncProducts(
        config: GoogleSheetsConfig,
        products: List<Product>,
        context: Context? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val productsArray = JSONArray()
        products.forEach { p ->
            val portableImg = ProductImageHelper.prepareProductImageForCloudSync(context, p)
            val obj = JSONObject().apply {
                put("barcode", p.barcode)
                put("name", p.name)
                put("category", p.category)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("wholesalePrice", p.wholesalePrice)
                put("stockQuantity", p.stockQuantity)
                put("unit", p.unit)
                put("currency", p.currency)
                put("imageUri", portableImg)
            }
            productsArray.put(obj)
        }

        val payload = JSONObject().apply {
            put("action", "sync_products")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("products", productsArray)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun syncOrders(
        config: GoogleSheetsConfig,
        orders: List<Order>,
        orderItems: List<OrderItem>,
        products: List<Product>,
        shopProfile: ShopProfile
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val itemsByOrderId = orderItems.groupBy { it.orderId }
        val ordersArray = JSONArray()

        orders.forEach { order ->
            val items = itemsByOrderId[order.id] ?: emptyList()
            val calc = ReceiptCalculator.calculateFromOrder(order, items, products, shopProfile)
            val itemsSummary = items.joinToString("; ") { "${it.productName} x${it.quantity}" }
            val itemsJsonArr = JSONArray()
            items.forEach { item ->
                itemsJsonArr.put(JSONObject().apply {
                    put("productId", item.productId)
                    put("productName", item.productName)
                    put("unitPrice", item.unitPrice)
                    put("costPrice", item.costPrice)
                    put("quantity", item.quantity)
                    put("subtotal", item.subtotal)
                    put("currency", item.currency)
                })
            }

            val obj = JSONObject().apply {
                put("invoiceNumber", order.invoiceNumber)
                put("timestamp", order.timestamp)
                put("dateTime", formatDateTime(order.timestamp))
                put("customerName", order.customerName)
                put("customerPhone", order.customerPhone)
                put("paymentMethod", order.paymentMethod.labelMm)
                put("itemsSummary", itemsSummary)
                put("items", itemsJsonArr)
                put("subtotal", order.subtotal)
                put("discount", order.discount)
                put("grandTotal", order.grandTotal)
                put("amountTendered", order.amountTendered)
                put("changeAmount", order.changeAmount)
                put("status", order.status.name)
                put("currency", order.currency)
                put("totalMmk", calc.sellingPriceMmk)
                put("totalThb", calc.sellingPriceThb)
                put("exchangeRate", order.exchangeRate)
                put("profitMmk", calc.estimatedNetProfitMmk)
                put("profitThb", calc.estimatedNetProfitThb)
                put("note", order.note)
            }
            ordersArray.put(obj)
        }

        val payload = JSONObject().apply {
            put("action", "sync_orders")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("orders", ordersArray)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun syncSingleOrder(
        config: GoogleSheetsConfig,
        orderDetails: OrderDetails,
        products: List<Product>,
        shopProfile: ShopProfile,
        receiptText: String = "",
        context: Context? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val order = orderDetails.order
        val items = orderDetails.items
        val productById = products.associateBy { it.id }
        val calc = ReceiptCalculator.calculateFromOrder(order, items, products, shopProfile)
        val itemsSummary = items.joinToString("; ") { "${it.productName} x${it.quantity}" }
        val itemsJsonArr = JSONArray()
        items.forEach { item ->
            val matchedProd = productById[item.productId]
            itemsJsonArr.put(JSONObject().apply {
                put("productId", item.productId)
                put("barcode", matchedProd?.barcode ?: "")
                put("productName", item.productName)
                put("unitPrice", item.unitPrice)
                put("costPrice", item.costPrice)
                put("quantity", item.quantity)
                put("subtotal", item.subtotal)
                put("currency", item.currency)
            })
        }

        val orderObj = JSONObject().apply {
            put("invoiceNumber", order.invoiceNumber)
            put("timestamp", order.timestamp)
            put("dateTime", formatDateTime(order.timestamp))
            put("customerName", order.customerName)
            put("customerPhone", order.customerPhone)
            put("paymentMethod", order.paymentMethod.labelMm)
            put("itemsSummary", itemsSummary)
            put("items", itemsJsonArr)
            put("subtotal", order.subtotal)
            put("discount", order.discount)
            put("grandTotal", order.grandTotal)
            put("amountTendered", order.amountTendered)
            put("changeAmount", order.changeAmount)
            put("status", order.status.name)
            put("currency", order.currency)
            put("totalMmk", calc.sellingPriceMmk)
            put("totalThb", calc.sellingPriceThb)
            put("exchangeRate", order.exchangeRate)
            put("profitMmk", calc.estimatedNetProfitMmk)
            put("profitThb", calc.estimatedNetProfitThb)
            put("note", order.note)
            put("receiptText", receiptText)
        }

        val productsArray = JSONArray()
        products.forEach { p ->
            val portableImg = ProductImageHelper.prepareProductImageForCloudSync(context, p)
            productsArray.put(JSONObject().apply {
                put("barcode", p.barcode)
                put("name", p.name)
                put("category", p.category)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("wholesalePrice", p.wholesalePrice)
                put("stockQuantity", p.stockQuantity)
                put("unit", p.unit)
                put("currency", p.currency)
                put("imageUri", portableImg)
            })
        }

        val payload = JSONObject().apply {
            put("action", "sync_single_order")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("order", orderObj)
            put("products", productsArray)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun syncRefundOrder(
        config: GoogleSheetsConfig,
        orderDetails: OrderDetails,
        products: List<Product>,
        shopProfile: ShopProfile = ShopProfile(),
        isUndo: Boolean = false,
        isUndoRefund: Boolean = isUndo,
        context: Context? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")
        val effectiveUndo = isUndo || isUndoRefund

        val order = orderDetails.order
        val items = orderDetails.items
        val productById = products.associateBy { it.id }
        val calc = ReceiptCalculator.calculateFromOrder(order, items, products, shopProfile)
        val itemsSummary = items.joinToString("; ") { "${it.productName} x${it.quantity}" }
        val itemsJsonArr = JSONArray()
        items.forEach { item ->
            val matchedProd = productById[item.productId]
            itemsJsonArr.put(JSONObject().apply {
                put("productId", item.productId)
                put("barcode", matchedProd?.barcode ?: "")
                put("productName", item.productName)
                put("unitPrice", item.unitPrice)
                put("costPrice", item.costPrice)
                put("quantity", item.quantity)
                put("subtotal", item.subtotal)
                put("currency", item.currency)
            })
        }

        val orderObj = JSONObject().apply {
            put("invoiceNumber", order.invoiceNumber)
            put("timestamp", order.timestamp)
            put("dateTime", formatDateTime(order.timestamp))
            put("customerName", order.customerName)
            put("customerPhone", order.customerPhone)
            put("paymentMethod", order.paymentMethod.labelMm)
            put("itemsSummary", itemsSummary)
            put("items", itemsJsonArr)
            put("subtotal", order.subtotal)
            put("discount", order.discount)
            put("grandTotal", order.grandTotal)
            put("amountTendered", order.amountTendered)
            put("changeAmount", order.changeAmount)
            put("status", order.status.name)
            put("currency", order.currency)
            put("totalMmk", calc.sellingPriceMmk)
            put("totalThb", calc.sellingPriceThb)
            put("exchangeRate", order.exchangeRate)
            put("profitMmk", calc.estimatedNetProfitMmk)
            put("profitThb", calc.estimatedNetProfitThb)
            put("note", order.note)
        }

        val productsArray = JSONArray()
        products.forEach { p ->
            val portableImg = ProductImageHelper.prepareProductImageForCloudSync(context, p)
            productsArray.put(JSONObject().apply {
                put("barcode", p.barcode)
                put("name", p.name)
                put("category", p.category)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("wholesalePrice", p.wholesalePrice)
                put("stockQuantity", p.stockQuantity)
                put("unit", p.unit)
                put("currency", p.currency)
                put("imageUri", portableImg)
            })
        }

        val payload = JSONObject().apply {
            put("action", if (effectiveUndo) "undo_refund_order" else "refund_order")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("invoiceNumber", order.invoiceNumber)
            put("order", orderObj)
            put("items", itemsJsonArr)
            put("products", productsArray)
        }

        val res = sendRequest(config.scriptUrl, payload)
        if (!res.first && context != null) {
            enqueueOfflineAction(
                context = context,
                actionType = if (effectiveUndo) "undo_refund_order" else "refund_order",
                description = if (effectiveUndo) "ငွေပြန်အမ်းမှု ပယ်ဖျက်ခြင်း (${order.invoiceNumber})" else "ငွေပြန်အမ်းခြင်းနှင့် လက်ကျန်ပြန်ပေါင်းခြင်း (${order.invoiceNumber})",
                payload = payload
            )
        }
        res
    }

    suspend fun syncDeleteOrder(
        config: GoogleSheetsConfig,
        orderDetails: OrderDetails,
        products: List<Product>,
        restoreStock: Boolean = true,
        context: Context? = null
    ): Pair<Boolean, String> {
        return syncDeleteOrder(
            config = config,
            invoiceNumber = orderDetails.order.invoiceNumber,
            wasRefunded = !restoreStock,
            items = orderDetails.items,
            products = products,
            context = context
        )
    }

    suspend fun syncDeleteOrder(
        config: GoogleSheetsConfig,
        invoiceNumber: String,
        wasRefunded: Boolean,
        items: List<OrderItem>,
        products: List<Product>,
        context: Context? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val productById = products.associateBy { it.id }
        val itemsJsonArr = JSONArray()
        items.forEach { item ->
            val matchedProd = productById[item.productId]
            itemsJsonArr.put(JSONObject().apply {
                put("productId", item.productId)
                put("barcode", matchedProd?.barcode ?: "")
                put("productName", item.productName)
                put("quantity", item.quantity)
            })
        }

        val productsArray = JSONArray()
        products.forEach { p ->
            val portableImg = ProductImageHelper.prepareProductImageForCloudSync(context, p)
            productsArray.put(JSONObject().apply {
                put("barcode", p.barcode)
                put("name", p.name)
                put("category", p.category)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("wholesalePrice", p.wholesalePrice)
                put("stockQuantity", p.stockQuantity)
                put("unit", p.unit)
                put("currency", p.currency)
                put("imageUri", portableImg)
            })
        }

        val payload = JSONObject().apply {
            put("action", "delete_order")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("invoiceNumber", invoiceNumber)
            put("wasRefunded", wasRefunded)
            put("restoreStock", !wasRefunded)
            put("items", itemsJsonArr)
            put("products", productsArray)
        }

        val res = sendRequest(config.scriptUrl, payload)
        if (!res.first && context != null) {
            enqueueOfflineAction(
                context = context,
                actionType = "delete_order",
                description = "အရောင်းဘောက်ချာ ဖျက်ခြင်း ($invoiceNumber)",
                payload = payload
            )
        }
        res
    }

    suspend fun syncDeleteSavedReceipt(
        config: GoogleSheetsConfig,
        invoiceNumber: String,
        context: Context? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")
        val payload = JSONObject().apply {
            put("action", "delete_saved_receipt")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("invoiceNumber", invoiceNumber)
        }
        val res = sendRequest(config.scriptUrl, payload)
        if (!res.first && context != null) {
            enqueueOfflineAction(
                context = context,
                actionType = "delete_saved_receipt",
                description = "ရှင်းပြီးပြေစာ ဖျက်ခြင်း ($invoiceNumber)",
                payload = payload
            )
        }
        res
    }

    fun loadOfflineQueue(context: Context?): List<OfflineSyncAction> {
        if (context == null) return emptyList()
        return try {
            val prefs = context.getSharedPreferences("pos_app_prefs", Context.MODE_PRIVATE)
            val raw = prefs.getString("offline_sync_action_queue_v1", null) ?: return emptyList()
            val arr = JSONArray(raw)
            val list = mutableListOf<OfflineSyncAction>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    OfflineSyncAction(
                        id = obj.optString("id", "${System.currentTimeMillis()}_$i"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        dateTime = obj.optString("dateTime", ""),
                        actionType = obj.optString("actionType", ""),
                        descriptionMm = obj.optString("descriptionMm", obj.optString("description", "")),
                        targetInvoiceNumber = obj.optString("targetInvoiceNumber", ""),
                        targetProductBarcode = obj.optString("targetProductBarcode", ""),
                        targetProductName = obj.optString("targetProductName", ""),
                        payloadJson = obj.optString("payloadJson", "{}")
                    )
                )
            }
            list.sortedBy { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveOfflineQueue(context: Context?, queue: List<OfflineSyncAction>) {
        if (context == null) return
        try {
            val arr = JSONArray()
            queue.sortedBy { it.timestamp }.takeLast(500).forEach { item ->
                arr.put(JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp)
                    put("dateTime", item.dateTime)
                    put("actionType", item.actionType)
                    put("descriptionMm", item.descriptionMm)
                    put("targetInvoiceNumber", item.targetInvoiceNumber)
                    put("targetProductBarcode", item.targetProductBarcode)
                    put("targetProductName", item.targetProductName)
                    put("payloadJson", item.payloadJson)
                })
            }
            val prefs = context.getSharedPreferences("pos_app_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("offline_sync_action_queue_v1", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun enqueueOfflineAction(
        context: Context?,
        actionType: String,
        description: String,
        payload: JSONObject
    ) {
        if (context == null) return
        val now = System.currentTimeMillis()
        val current = loadOfflineQueue(context).toMutableList()
        val inv = payload.optString("invoiceNumber", "").ifBlank {
            payload.optJSONObject("order")?.optString("invoiceNumber", "") ?: ""
        }
        val prodBarcode = payload.optString("barcode", "").ifBlank {
            payload.optJSONObject("product")?.optString("barcode", "") ?: ""
        }
        val prodName = payload.optString("productName", "").ifBlank {
            payload.optJSONObject("product")?.optString("name", "") ?: ""
        }
        current.add(
            OfflineSyncAction(
                id = "ACT_${now}_${current.size + 1}",
                timestamp = now,
                dateTime = formatDateTime(now),
                actionType = actionType,
                descriptionMm = description,
                targetInvoiceNumber = inv,
                targetProductBarcode = prodBarcode,
                targetProductName = prodName,
                payloadJson = payload.toString()
            )
        )
        saveOfflineQueue(context, current)
    }

    fun clearOfflineQueue(context: Context?) {
        if (context == null) return
        val prefs = context.getSharedPreferences("pos_app_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("offline_sync_action_queue_v1").apply()
    }

    suspend fun replayOfflineQueue(
        config: GoogleSheetsConfig,
        context: Context?
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")
        val queue = loadOfflineQueue(context)
        if (queue.isEmpty()) return@withContext Pair(true, "ပြန်လည်ပေးပို့ရန် Offline မှတ်တမ်း မရှိပါ")

        val queueArr = JSONArray()
        queue.sortedBy { it.timestamp }.forEach { act ->
            val payloadObj = try { JSONObject(act.payloadJson) } catch (_: Exception) { JSONObject() }
            queueArr.put(JSONObject().apply {
                put("id", act.id)
                put("timestamp", act.timestamp)
                put("dateTime", act.dateTime)
                put("actionType", act.actionType)
                put("descriptionMm", act.descriptionMm)
                put("description", act.descriptionMm)
                put("targetInvoiceNumber", act.targetInvoiceNumber)
                put("targetProductBarcode", act.targetProductBarcode)
                put("targetProductName", act.targetProductName)
                put("payload", payloadObj)
            })
        }

        val payload = JSONObject().apply {
            put("action", "replay_offline_queue")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("offlineQueue", queueArr)
        }

        val (success, msg) = sendRequest(config.scriptUrl, payload)
        if (success) {
            clearOfflineQueue(context)
        }
        Pair(success, msg)
    }

    suspend fun syncDeleteProduct(
        config: GoogleSheetsConfig,
        deletedProduct: Product,
        remainingProducts: List<Product>,
        context: Context? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val productsArray = JSONArray()
        remainingProducts.forEach { p ->
            val portableImg = ProductImageHelper.prepareProductImageForCloudSync(context, p)
            productsArray.put(JSONObject().apply {
                put("barcode", p.barcode)
                put("name", p.name)
                put("category", p.category)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("wholesalePrice", p.wholesalePrice)
                put("stockQuantity", p.stockQuantity)
                put("unit", p.unit)
                put("currency", p.currency)
                put("imageUri", portableImg)
            })
        }

        val payload = JSONObject().apply {
            put("action", "delete_product")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("product", JSONObject().apply {
                put("barcode", deletedProduct.barcode)
                put("name", deletedProduct.name)
            })
            put("products", productsArray)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun replayOfflineQueue(
        config: GoogleSheetsConfig,
        offlineQueue: List<OfflineSyncAction>,
        products: List<Product>,
        orders: List<Order>,
        orderItems: List<OrderItem>,
        savedReceipts: List<SavedReceipt>,
        salesSummaries: List<JSONObject>,
        savedSummaries: List<JSONObject>,
        shopProfile: ShopProfile,
        context: Context? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val queueArr = JSONArray()
        offlineQueue.sortedBy { it.timestamp }.forEach { act ->
            val payloadObj = try { JSONObject(act.payloadJson) } catch (_: Exception) { JSONObject() }
            queueArr.put(JSONObject().apply {
                put("id", act.id)
                put("timestamp", act.timestamp)
                put("dateTime", act.dateTime)
                put("actionType", act.actionType)
                put("descriptionMm", act.descriptionMm)
                put("targetInvoiceNumber", act.targetInvoiceNumber)
                put("targetProductBarcode", act.targetProductBarcode)
                put("targetProductName", act.targetProductName)
                put("payload", payloadObj)
            })
        }

        val productsArray = JSONArray()
        products.forEach { p ->
            val portableImg = ProductImageHelper.prepareProductImageForCloudSync(context, p)
            productsArray.put(JSONObject().apply {
                put("barcode", p.barcode)
                put("name", p.name)
                put("category", p.category)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("wholesalePrice", p.wholesalePrice)
                put("stockQuantity", p.stockQuantity)
                put("unit", p.unit)
                put("currency", p.currency)
                put("imageUri", portableImg)
            })
        }

        val itemsByOrderId = orderItems.groupBy { it.orderId }
        val productById = products.associateBy { it.id }
        val ordersArray = JSONArray()
        orders.forEach { order ->
            val items = itemsByOrderId[order.id] ?: emptyList()
            val calc = ReceiptCalculator.calculateFromOrder(order, items, products, shopProfile)
            val itemsSummary = items.joinToString("; ") { "${it.productName} x${it.quantity}" }
            val itemsJsonArr = JSONArray()
            items.forEach { item ->
                val matchedProd = productById[item.productId]
                itemsJsonArr.put(JSONObject().apply {
                    put("productId", item.productId)
                    put("barcode", matchedProd?.barcode ?: "")
                    put("productName", item.productName)
                    put("unitPrice", item.unitPrice)
                    put("costPrice", item.costPrice)
                    put("quantity", item.quantity)
                    put("subtotal", item.subtotal)
                    put("currency", item.currency)
                })
            }

            ordersArray.put(JSONObject().apply {
                put("invoiceNumber", order.invoiceNumber)
                put("timestamp", order.timestamp)
                put("dateTime", formatDateTime(order.timestamp))
                put("customerName", order.customerName)
                put("customerPhone", order.customerPhone)
                put("paymentMethod", order.paymentMethod.labelMm)
                put("itemsSummary", itemsSummary)
                put("items", itemsJsonArr)
                put("subtotal", order.subtotal)
                put("discount", order.discount)
                put("grandTotal", order.grandTotal)
                put("amountTendered", order.amountTendered)
                put("changeAmount", order.changeAmount)
                put("status", order.status.name)
                put("currency", order.currency)
                put("totalMmk", calc.sellingPriceMmk)
                put("totalThb", calc.sellingPriceThb)
                put("exchangeRate", order.exchangeRate)
                put("profitMmk", calc.estimatedNetProfitMmk)
                put("profitThb", calc.estimatedNetProfitThb)
                put("note", order.note)
            })
        }

        val savedArray = JSONArray()
        savedReceipts.forEach { receipt ->
            val calc = ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
            val items = SavedReceipt.parseItemsJson(receipt.itemsJson)
            val itemsSummary = items.joinToString("; ") { "${it.productName} x${it.quantity}" }

            savedArray.put(JSONObject().apply {
                put("invoiceNumber", receipt.invoiceNumber)
                put("originalTimestamp", receipt.originalTimestamp)
                put("clearedTimestamp", receipt.clearedTimestamp)
                put("dateTime", formatDateTime(receipt.originalTimestamp))
                put("customerName", receipt.customerName)
                put("customerPhone", receipt.customerPhone)
                put("paymentMethod", receipt.paymentMethod.labelMm)
                put("itemsSummary", itemsSummary)
                put("itemsJson", receipt.itemsJson)
                put("subtotal", receipt.subtotal)
                put("discount", receipt.discount)
                put("grandTotal", receipt.grandTotal)
                put("amountTendered", receipt.amountTendered)
                put("changeAmount", receipt.changeAmount)
                put("currency", receipt.currency)
                put("totalMmk", calc.sellingPriceMmk)
                put("totalThb", calc.sellingPriceThb)
                put("exchangeRate", receipt.exchangeRate)
                put("profitMmk", calc.estimatedNetProfitMmk)
                put("profitThb", calc.estimatedNetProfitThb)
                put("clearedDate", formatDateTime(receipt.clearedTimestamp))
                put("note", receipt.note)
            })
        }

        val salesArr = JSONArray()
        salesSummaries.forEach { salesArr.put(it) }

        val savedArr = JSONArray()
        savedSummaries.forEach { savedArr.put(it) }

        val ratesObj = JSONObject().apply {
            put("mmkSellingRate", shopProfile.mmkSellingRate)
            put("mmkBuyingRate", shopProfile.mmkBuyingRate)
            put("thbSellingRate", shopProfile.thbSellingRate)
            put("thbBuyingRate", shopProfile.thbBuyingRate)
        }

        val shopProfileObj = JSONObject().apply {
            put("shopName", shopProfile.shopName)
            put("englishName", shopProfile.englishName)
            put("phone", shopProfile.phone)
            put("address", shopProfile.address)
            put("receiptFooter", shopProfile.receiptFooter)
            put("cashierName", shopProfile.cashierName)
            put("mmkSellingRate", shopProfile.mmkSellingRate)
            put("mmkBuyingRate", shopProfile.mmkBuyingRate)
            put("thbSellingRate", shopProfile.thbSellingRate)
            put("thbBuyingRate", shopProfile.thbBuyingRate)
        }

        val payload = JSONObject().apply {
            put("action", "replay_offline_queue")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("offlineQueue", queueArr)
            put("mirrorScreenState", true)
            put("products", productsArray)
            put("orders", ordersArray)
            put("savedReceipts", savedArray)
            put("salesSummaries", salesArr)
            put("savedReceiptsSummaries", savedArr)
            put("rates", ratesObj)
            put("shopProfile", shopProfileObj)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun syncSavedReceipts(
        config: GoogleSheetsConfig,
        savedReceipts: List<SavedReceipt>,
        products: List<Product>,
        shopProfile: ShopProfile
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val savedArray = JSONArray()
        savedReceipts.forEach { receipt ->
            val calc = ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
            val items = SavedReceipt.parseItemsJson(receipt.itemsJson)
            val itemsSummary = items.joinToString("; ") { "${it.productName} x${it.quantity}" }

            val obj = JSONObject().apply {
                put("invoiceNumber", receipt.invoiceNumber)
                put("originalTimestamp", receipt.originalTimestamp)
                put("clearedTimestamp", receipt.clearedTimestamp)
                put("dateTime", formatDateTime(receipt.originalTimestamp))
                put("customerName", receipt.customerName)
                put("customerPhone", receipt.customerPhone)
                put("paymentMethod", receipt.paymentMethod.labelMm)
                put("itemsSummary", itemsSummary)
                put("itemsJson", receipt.itemsJson)
                put("subtotal", receipt.subtotal)
                put("discount", receipt.discount)
                put("grandTotal", receipt.grandTotal)
                put("amountTendered", receipt.amountTendered)
                put("changeAmount", receipt.changeAmount)
                put("currency", receipt.currency)
                put("totalMmk", calc.sellingPriceMmk)
                put("totalThb", calc.sellingPriceThb)
                put("exchangeRate", receipt.exchangeRate)
                put("profitMmk", calc.estimatedNetProfitMmk)
                put("profitThb", calc.estimatedNetProfitThb)
                put("clearedDate", formatDateTime(receipt.clearedTimestamp))
                put("note", receipt.note)
            }
            savedArray.put(obj)
        }

        val payload = JSONObject().apply {
            put("action", "sync_saved_receipts")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("savedReceipts", savedArray)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun syncSingleSavedReceipt(
        config: GoogleSheetsConfig,
        receipt: SavedReceipt,
        products: List<Product>,
        shopProfile: ShopProfile
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val calc = ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
        val items = SavedReceipt.parseItemsJson(receipt.itemsJson)
        val itemsSummary = items.joinToString("; ") { "${it.productName} x${it.quantity}" }

        val obj = JSONObject().apply {
            put("invoiceNumber", receipt.invoiceNumber)
            put("originalTimestamp", receipt.originalTimestamp)
            put("clearedTimestamp", receipt.clearedTimestamp)
            put("dateTime", formatDateTime(receipt.originalTimestamp))
            put("customerName", receipt.customerName)
            put("customerPhone", receipt.customerPhone)
            put("paymentMethod", receipt.paymentMethod.labelMm)
            put("itemsSummary", itemsSummary)
            put("itemsJson", receipt.itemsJson)
            put("subtotal", receipt.subtotal)
            put("discount", receipt.discount)
            put("grandTotal", receipt.grandTotal)
            put("amountTendered", receipt.amountTendered)
            put("changeAmount", receipt.changeAmount)
            put("currency", receipt.currency)
            put("totalMmk", calc.sellingPriceMmk)
            put("totalThb", calc.sellingPriceThb)
            put("exchangeRate", receipt.exchangeRate)
            put("profitMmk", calc.estimatedNetProfitMmk)
            put("profitThb", calc.estimatedNetProfitThb)
            put("clearedDate", formatDateTime(receipt.clearedTimestamp))
            put("note", receipt.note)
        }

        val payload = JSONObject().apply {
            put("action", "sync_single_saved_receipt")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("savedReceipt", obj)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun syncSummaries(
        config: GoogleSheetsConfig,
        salesSummaries: List<JSONObject>,
        savedSummaries: List<JSONObject>
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val salesArr = JSONArray()
        salesSummaries.forEach { salesArr.put(it) }

        val savedArr = JSONArray()
        savedSummaries.forEach { savedArr.put(it) }

        val payload = JSONObject().apply {
            put("action", "sync_summaries")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("salesSummaries", salesArr)
            put("savedReceiptsSummaries", savedArr)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun syncRates(config: GoogleSheetsConfig, shopProfile: ShopProfile): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val ratesObj = JSONObject().apply {
            put("mmkSellingRate", shopProfile.mmkSellingRate)
            put("mmkBuyingRate", shopProfile.mmkBuyingRate)
            put("thbSellingRate", shopProfile.thbSellingRate)
            put("thbBuyingRate", shopProfile.thbBuyingRate)
        }

        val shopProfileObj = JSONObject().apply {
            put("shopName", shopProfile.shopName)
            put("englishName", shopProfile.englishName)
            put("phone", shopProfile.phone)
            put("address", shopProfile.address)
            put("receiptFooter", shopProfile.receiptFooter)
            put("cashierName", shopProfile.cashierName)
            put("mmkSellingRate", shopProfile.mmkSellingRate)
            put("mmkBuyingRate", shopProfile.mmkBuyingRate)
            put("thbSellingRate", shopProfile.thbSellingRate)
            put("thbBuyingRate", shopProfile.thbBuyingRate)
        }

        val payload = JSONObject().apply {
            put("action", "sync_rates")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("rates", ratesObj)
            put("shopProfile", shopProfileObj)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun syncShopProfile(config: GoogleSheetsConfig, shopProfile: ShopProfile): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val ratesObj = JSONObject().apply {
            put("mmkSellingRate", shopProfile.mmkSellingRate)
            put("mmkBuyingRate", shopProfile.mmkBuyingRate)
            put("thbSellingRate", shopProfile.thbSellingRate)
            put("thbBuyingRate", shopProfile.thbBuyingRate)
        }

        val shopProfileObj = JSONObject().apply {
            put("shopName", shopProfile.shopName)
            put("englishName", shopProfile.englishName)
            put("phone", shopProfile.phone)
            put("address", shopProfile.address)
            put("receiptFooter", shopProfile.receiptFooter)
            put("cashierName", shopProfile.cashierName)
            put("mmkSellingRate", shopProfile.mmkSellingRate)
            put("mmkBuyingRate", shopProfile.mmkBuyingRate)
            put("thbSellingRate", shopProfile.thbSellingRate)
            put("thbBuyingRate", shopProfile.thbBuyingRate)
        }

        val payload = JSONObject().apply {
            put("action", "sync_shop_profile")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("rates", ratesObj)
            put("shopProfile", shopProfileObj)
        }

        sendRequest(config.scriptUrl, payload)
    }

    suspend fun syncAll(
        config: GoogleSheetsConfig,
        products: List<Product>,
        orders: List<Order>,
        orderItems: List<OrderItem>,
        savedReceipts: List<SavedReceipt>,
        salesSummaries: List<JSONObject>,
        savedSummaries: List<JSONObject>,
        shopProfile: ShopProfile,
        context: Context? = null,
        offlineQueue: List<OfflineSyncAction> = emptyList()
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.scriptUrl.isBlank()) return@withContext Pair(false, "Web App URL မထည့်ရသေးပါ")

        val queueArr = JSONArray()
        offlineQueue.sortedBy { it.timestamp }.forEach { act ->
            val payloadObj = try { JSONObject(act.payloadJson) } catch (_: Exception) { JSONObject() }
            queueArr.put(JSONObject().apply {
                put("id", act.id)
                put("timestamp", act.timestamp)
                put("dateTime", act.dateTime)
                put("actionType", act.actionType)
                put("descriptionMm", act.descriptionMm)
                put("targetInvoiceNumber", act.targetInvoiceNumber)
                put("targetProductBarcode", act.targetProductBarcode)
                put("targetProductName", act.targetProductName)
                put("payload", payloadObj)
            })
        }

        val productsArray = JSONArray()
        products.forEach { p ->
            val portableImg = ProductImageHelper.prepareProductImageForCloudSync(context, p)
            productsArray.put(JSONObject().apply {
                put("barcode", p.barcode)
                put("name", p.name)
                put("category", p.category)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("wholesalePrice", p.wholesalePrice)
                put("stockQuantity", p.stockQuantity)
                put("unit", p.unit)
                put("currency", p.currency)
                put("imageUri", portableImg)
            })
        }

        val itemsByOrderId = orderItems.groupBy { it.orderId }
        val productById = products.associateBy { it.id }
        val ordersArray = JSONArray()
        orders.forEach { order ->
            val items = itemsByOrderId[order.id] ?: emptyList()
            val calc = ReceiptCalculator.calculateFromOrder(order, items, products, shopProfile)
            val itemsSummary = items.joinToString("; ") { "${it.productName} x${it.quantity}" }
            val itemsJsonArr = JSONArray()
            items.forEach { item ->
                val matchedProd = productById[item.productId]
                itemsJsonArr.put(JSONObject().apply {
                    put("productId", item.productId)
                    put("barcode", matchedProd?.barcode ?: "")
                    put("productName", item.productName)
                    put("unitPrice", item.unitPrice)
                    put("costPrice", item.costPrice)
                    put("quantity", item.quantity)
                    put("subtotal", item.subtotal)
                    put("currency", item.currency)
                })
            }

            ordersArray.put(JSONObject().apply {
                put("invoiceNumber", order.invoiceNumber)
                put("timestamp", order.timestamp)
                put("dateTime", formatDateTime(order.timestamp))
                put("customerName", order.customerName)
                put("customerPhone", order.customerPhone)
                put("paymentMethod", order.paymentMethod.labelMm)
                put("itemsSummary", itemsSummary)
                put("items", itemsJsonArr)
                put("subtotal", order.subtotal)
                put("discount", order.discount)
                put("grandTotal", order.grandTotal)
                put("amountTendered", order.amountTendered)
                put("changeAmount", order.changeAmount)
                put("status", order.status.name)
                put("currency", order.currency)
                put("totalMmk", calc.sellingPriceMmk)
                put("totalThb", calc.sellingPriceThb)
                put("exchangeRate", order.exchangeRate)
                put("profitMmk", calc.estimatedNetProfitMmk)
                put("profitThb", calc.estimatedNetProfitThb)
                put("note", order.note)
            })
        }

        val savedArray = JSONArray()
        savedReceipts.forEach { receipt ->
            val calc = ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
            val items = SavedReceipt.parseItemsJson(receipt.itemsJson)
            val itemsSummary = items.joinToString("; ") { "${it.productName} x${it.quantity}" }

            savedArray.put(JSONObject().apply {
                put("invoiceNumber", receipt.invoiceNumber)
                put("originalTimestamp", receipt.originalTimestamp)
                put("clearedTimestamp", receipt.clearedTimestamp)
                put("dateTime", formatDateTime(receipt.originalTimestamp))
                put("customerName", receipt.customerName)
                put("customerPhone", receipt.customerPhone)
                put("paymentMethod", receipt.paymentMethod.labelMm)
                put("itemsSummary", itemsSummary)
                put("itemsJson", receipt.itemsJson)
                put("subtotal", receipt.subtotal)
                put("discount", receipt.discount)
                put("grandTotal", receipt.grandTotal)
                put("amountTendered", receipt.amountTendered)
                put("changeAmount", receipt.changeAmount)
                put("currency", receipt.currency)
                put("totalMmk", calc.sellingPriceMmk)
                put("totalThb", calc.sellingPriceThb)
                put("exchangeRate", receipt.exchangeRate)
                put("profitMmk", calc.estimatedNetProfitMmk)
                put("profitThb", calc.estimatedNetProfitThb)
                put("clearedDate", formatDateTime(receipt.clearedTimestamp))
                put("note", receipt.note)
            })
        }

        val salesArr = JSONArray()
        salesSummaries.forEach { salesArr.put(it) }

        val savedArr = JSONArray()
        savedSummaries.forEach { savedArr.put(it) }

        val ratesObj = JSONObject().apply {
            put("mmkSellingRate", shopProfile.mmkSellingRate)
            put("mmkBuyingRate", shopProfile.mmkBuyingRate)
            put("thbSellingRate", shopProfile.thbSellingRate)
            put("thbBuyingRate", shopProfile.thbBuyingRate)
        }

        val shopProfileObj = JSONObject().apply {
            put("shopName", shopProfile.shopName)
            put("englishName", shopProfile.englishName)
            put("phone", shopProfile.phone)
            put("address", shopProfile.address)
            put("receiptFooter", shopProfile.receiptFooter)
            put("cashierName", shopProfile.cashierName)
            put("mmkSellingRate", shopProfile.mmkSellingRate)
            put("mmkBuyingRate", shopProfile.mmkBuyingRate)
            put("thbSellingRate", shopProfile.thbSellingRate)
            put("thbBuyingRate", shopProfile.thbBuyingRate)
        }

        val payload = JSONObject().apply {
            put("action", "sync_all")
            put("sheetName", config.sheetName)
            put("sheetUrl", config.sheetUrl)
            put("folderId", config.folderId)
            put("shopName", config.shopName)
            put("offlineQueue", queueArr)
            put("mirrorScreenState", true)
            put("products", productsArray)
            put("orders", ordersArray)
            put("savedReceipts", savedArray)
            put("salesSummaries", salesArr)
            put("savedReceiptsSummaries", savedArr)
            put("rates", ratesObj)
            put("shopProfile", shopProfileObj)
        }

        sendRequest(config.scriptUrl, payload)
    }

    fun buildLocalStorageJson(
        config: GoogleSheetsConfig,
        products: List<Product>,
        orders: List<Order>,
        orderItems: List<OrderItem>,
        savedReceipts: List<SavedReceipt>,
        salesSummaries: List<JSONObject>,
        savedSummaries: List<JSONObject>,
        shopProfile: ShopProfile,
        pullResult: PullResult? = null,
        context: Context? = null,
        offlineQueue: List<OfflineSyncAction> = emptyList()
    ): String {
        val queueArr = JSONArray()
        offlineQueue.sortedBy { it.timestamp }.forEach { act ->
            val payloadObj = try { JSONObject(act.payloadJson) } catch (_: Exception) { JSONObject() }
            queueArr.put(JSONObject().apply {
                put("id", act.id)
                put("timestamp", act.timestamp)
                put("dateTime", act.dateTime)
                put("actionType", act.actionType)
                put("descriptionMm", act.descriptionMm)
                put("targetInvoiceNumber", act.targetInvoiceNumber)
                put("targetProductBarcode", act.targetProductBarcode)
                put("targetProductName", act.targetProductName)
                put("payload", payloadObj)
            })
        }

        val productsArray = JSONArray()
        products.forEach { p ->
            val portableImg = ProductImageHelper.prepareProductImageForCloudSync(context, p)
            productsArray.put(JSONObject().apply {
                put("id", p.id)
                put("barcode", p.barcode)
                put("name", p.name)
                put("category", p.category)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("wholesalePrice", p.wholesalePrice)
                put("stockQuantity", p.stockQuantity)
                put("unit", p.unit)
                put("currency", p.currency)
                put("imageUri", portableImg)
            })
        }

        val itemsByOrderId = orderItems.groupBy { it.orderId }
        val ordersArray = JSONArray()
        orders.forEach { order ->
            val items = itemsByOrderId[order.id] ?: emptyList()
            val calc = ReceiptCalculator.calculateFromOrder(order, items, products, shopProfile)
            val itemsSummary = items.joinToString("; ") { "${it.productName} x${it.quantity}" }
            val itemsJsonArr = JSONArray()
            items.forEach { item ->
                itemsJsonArr.put(JSONObject().apply {
                    put("productId", item.productId)
                    put("productName", item.productName)
                    put("unitPrice", item.unitPrice)
                    put("costPrice", item.costPrice)
                    put("quantity", item.quantity)
                    put("subtotal", item.subtotal)
                    put("currency", item.currency)
                })
            }

            ordersArray.put(JSONObject().apply {
                put("invoiceNumber", order.invoiceNumber)
                put("timestamp", order.timestamp)
                put("dateTime", formatDateTime(order.timestamp))
                put("customerName", order.customerName)
                put("customerPhone", order.customerPhone)
                put("paymentMethod", order.paymentMethod.labelMm)
                put("itemsSummary", itemsSummary)
                put("items", itemsJsonArr)
                put("subtotal", order.subtotal)
                put("discount", order.discount)
                put("grandTotal", order.grandTotal)
                put("amountTendered", order.amountTendered)
                put("changeAmount", order.changeAmount)
                put("status", order.status.name)
                put("currency", order.currency)
                put("totalMmk", calc.sellingPriceMmk)
                put("totalThb", calc.sellingPriceThb)
                put("exchangeRate", order.exchangeRate)
                put("profitMmk", calc.estimatedNetProfitMmk)
                put("profitThb", calc.estimatedNetProfitThb)
                put("note", order.note)
            })
        }

        val savedArray = JSONArray()
        savedReceipts.forEach { receipt ->
            val calc = ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
            val items = SavedReceipt.parseItemsJson(receipt.itemsJson)
            val itemsSummary = items.joinToString("; ") { "${it.productName} x${it.quantity}" }

            savedArray.put(JSONObject().apply {
                put("invoiceNumber", receipt.invoiceNumber)
                put("originalTimestamp", receipt.originalTimestamp)
                put("clearedTimestamp", receipt.clearedTimestamp)
                put("dateTime", formatDateTime(receipt.originalTimestamp))
                put("clearedDate", formatDateTime(receipt.clearedTimestamp))
                put("customerName", receipt.customerName)
                put("customerPhone", receipt.customerPhone)
                put("paymentMethod", receipt.paymentMethod.labelMm)
                put("itemsSummary", itemsSummary)
                put("itemsJson", receipt.itemsJson)
                put("subtotal", receipt.subtotal)
                put("discount", receipt.discount)
                put("grandTotal", receipt.grandTotal)
                put("amountTendered", receipt.amountTendered)
                put("changeAmount", receipt.changeAmount)
                put("currency", receipt.currency)
                put("totalMmk", calc.sellingPriceMmk)
                put("totalThb", calc.sellingPriceThb)
                put("exchangeRate", receipt.exchangeRate)
                put("profitMmk", calc.estimatedNetProfitMmk)
                put("profitThb", calc.estimatedNetProfitThb)
                put("note", receipt.note)
            })
        }

        val finalOrdersArray = if (ordersArray.length() > 0) ordersArray else (pullResult?.ordersJsonArray ?: ordersArray)
        val finalSavedArray = if (savedArray.length() > 0) savedArray else (pullResult?.savedReceiptsJsonArray ?: savedArray)

        val salesArr = JSONArray()
        salesSummaries.forEach { salesArr.put(it) }

        val savedArr = JSONArray()
        savedSummaries.forEach { savedArr.put(it) }

        val ratesObj = JSONObject().apply {
            put("mmkSellingRate", shopProfile.mmkSellingRate)
            put("mmkBuyingRate", shopProfile.mmkBuyingRate)
            put("thbSellingRate", shopProfile.thbSellingRate)
            put("thbBuyingRate", shopProfile.thbBuyingRate)
        }

        val shopProfileObj = JSONObject().apply {
            put("shopName", shopProfile.shopName)
            put("englishName", shopProfile.englishName)
            put("phone", shopProfile.phone)
            put("address", shopProfile.address)
            put("receiptFooter", shopProfile.receiptFooter)
            put("cashierName", shopProfile.cashierName)
            put("mmkSellingRate", shopProfile.mmkSellingRate)
            put("mmkBuyingRate", shopProfile.mmkBuyingRate)
            put("thbSellingRate", shopProfile.thbSellingRate)
            put("thbBuyingRate", shopProfile.thbBuyingRate)
        }

        val rootJson = JSONObject().apply {
            put("status", "success")
            put("syncedAt", formatDateTime(System.currentTimeMillis()))
            put("timestamp", System.currentTimeMillis())
            put("sheetName", config.sheetName)
            put("sheetUrl", pullResult?.sheetUrl?.ifBlank { config.sheetUrl } ?: config.sheetUrl)
            put("scriptUrl", config.scriptUrl)
            put("offlineQueueCount", offlineQueue.size)
            put("offlineQueue", queueArr)
            put("products", productsArray)
            put("orders", finalOrdersArray)
            put("savedReceipts", finalSavedArray)
            put("salesSummaries", salesArr)
            put("savedReceiptsSummaries", savedArr)
            put("rates", ratesObj)
            put("shopProfile", shopProfileObj)
        }

        return rootJson.toString(2)
    }

    private fun sendRequest(url: String, payload: JSONObject): Pair<Boolean, String> {
        return try {
            val body = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url.trim())
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Pair(false, "ချိတ်ဆက်မှု မအောင်မြင်ပါ (HTTP ${response.code}): $responseBody")
                } else {
                    try {
                        val resJson = JSONObject(responseBody)
                        val status = resJson.optString("status")
                        val message = resJson.optString("message", "လုပ်ဆောင်ချက် အောင်မြင်ပါသည်")
                        val sheetUrl = resJson.optString("sheetUrl", "")
                        if (status == "success") {
                            Pair(true, if (sheetUrl.isNotBlank()) "$message\n(ဇယားလင့်ခ် ရရှိပါသည်)" else message)
                        } else {
                            Pair(false, message)
                        }
                    } catch (e: Exception) {
                        if (responseBody.contains("success", ignoreCase = true)) {
                            Pair(true, "လုပ်ဆောင်ချက် အောင်မြင်ပါသည်")
                        } else {
                            Pair(false, "တုံ့ပြန်မှု ဖတ်မရပါ: $responseBody")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Pair(false, "ချိတ်ဆက်မှု အမှားဖြစ်ပေါ်ပါသည်: ${e.localizedMessage ?: e.message}")
        }
    }
}
