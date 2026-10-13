package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CartItem
import com.example.data.GoogleSheetsConfig
import com.example.data.GoogleSheetsSyncManager
import com.example.data.HeldOrderEntry
import com.example.data.OfflineSyncAction
import com.example.data.OrderDetails
import com.example.data.PosRepository
import com.example.data.SaveReceiptResult
import com.example.data.ShopProfile
import com.example.data.SyncStatus
import com.example.data.entity.Order
import com.example.data.entity.OrderItem
import com.example.data.entity.OrderStatus
import com.example.data.entity.PaymentMethod
import com.example.data.entity.Product
import com.example.data.entity.SavedReceipt
import com.example.util.ReceiptCalculator
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("pos_app_prefs", android.content.Context.MODE_PRIVATE)
    private val googleSheetsSyncManager = GoogleSheetsSyncManager()

    private fun loadSavedGoogleSheetsConfig(): GoogleSheetsConfig {
        val rawScriptUrl = prefs.getString("gs_script_url", "") ?: ""
        val scriptUrl = if (rawScriptUrl.isNotBlank()) rawScriptUrl else GoogleSheetsConfig.DEFAULT_SCRIPT_URL
        if (rawScriptUrl.isBlank()) {
            prefs.edit().putString("gs_script_url", scriptUrl).apply()
        }
        val rawSheetName = prefs.getString("gs_sheet_name", "") ?: ""
        val sheetName = if (rawSheetName.isNotBlank() && rawSheetName != "POS Retail Sales & Inventory & Myanmar POS API") {
            rawSheetName
        } else {
            GoogleSheetsConfig.DEFAULT_SHEET_NAME
        }
        if (rawSheetName.isBlank() || rawSheetName == "POS Retail Sales & Inventory & Myanmar POS API") {
            prefs.edit().putString("gs_sheet_name", sheetName).apply()
        }
        val sheetUrl = prefs.getString("gs_sheet_url", "") ?: ""
        val folderName = prefs.getString("gs_folder_name", "") ?: ""
        val folderId = prefs.getString("gs_folder_id", "") ?: ""
        val shopName = prefs.getString("gs_shop_name", "POS System Software") ?: "POS System Software"
        val autoSync = prefs.getBoolean("gs_auto_sync_sales", false)
        val periodicSync = prefs.getBoolean("gs_periodic_auto_sync", false)
        val syncInterval = prefs.getInt("gs_sync_interval_seconds", 30)

        return GoogleSheetsConfig(
            scriptUrl = scriptUrl,
            sheetName = sheetName,
            folderName = folderName,
            folderId = folderId,
            shopName = shopName,
            autoSyncSales = autoSync,
            periodicAutoSync = periodicSync,
            syncIntervalSeconds = syncInterval,
            sheetUrl = sheetUrl,
            lastSyncTime = prefs.getLong("gs_last_sync_time", 0L),
            lastSyncStatus = prefs.getString("gs_last_sync_status", "စတင်အသုံးပြုရန် အဆင်သင့်ဖြစ်ပါသည်") ?: "စတင်အသုံးပြုရန် အဆင်သင့်ဖြစ်ပါသည်",
            syncStatus = SyncStatus.IDLE,
            pendingOfflineActionsCount = googleSheetsSyncManager.loadOfflineQueue(getApplication<Application>()).size
        )
    }

    private val _googleSheetsConfig = MutableStateFlow(loadSavedGoogleSheetsConfig())
    val googleSheetsConfig: StateFlow<GoogleSheetsConfig> = _googleSheetsConfig.asStateFlow()

    private val _offlineQueue = MutableStateFlow<List<OfflineSyncAction>>(
        googleSheetsSyncManager.loadOfflineQueue(application)
    )
    val offlineQueue: StateFlow<List<OfflineSyncAction>> = _offlineQueue.asStateFlow()

    fun refreshOfflineQueueState() {
        val list = googleSheetsSyncManager.loadOfflineQueue(getApplication<Application>())
        _offlineQueue.value = list
        _googleSheetsConfig.value = _googleSheetsConfig.value.copy(
            pendingOfflineActionsCount = list.size
        )
    }

    fun clearOfflineQueue(onResult: ((Boolean, String) -> Unit)? = null) {
        googleSheetsSyncManager.clearOfflineQueue(getApplication<Application>())
        refreshOfflineQueueState()
        onResult?.invoke(true, "အင်တာနက်မရှိချိန် မှတ်သားထားသော လုပ်ဆောင်ချက်စာရင်း (Offline Queue) အား ရှင်းလင်းပြီးပါပြီ")
    }

    fun replayOfflineQueueNow(onResult: ((Boolean, String) -> Unit)? = null) {
        val cfg = _googleSheetsConfig.value
        if (cfg.scriptUrl.isBlank()) {
            onResult?.invoke(false, "Google Apps Script Web App URL မထည့်ရသေးပါ")
            return
        }
        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            val appContext = getApplication<Application>()
            val (success, msg) = googleSheetsSyncManager.replayOfflineQueue(cfg, appContext)
            refreshOfflineQueueState()
            if (success) {
                // After replaying ordered actions, also mirror current screen state so Google Sheets matches 100%
                val latestProducts = repository.allProducts.first()
                val latestOrders = repository.allOrders.first()
                val latestOrderItems = repository.allOrderItems.first()
                val latestSavedReceipts = repository.savedReceipts.first()
                val salesSummaries = buildSalesSummaries()
                val savedSummaries = buildSavedReceiptsSummaries()
                googleSheetsSyncManager.syncAll(
                    config = cfg,
                    products = latestProducts,
                    orders = latestOrders,
                    orderItems = latestOrderItems,
                    savedReceipts = latestSavedReceipts,
                    salesSummaries = salesSummaries,
                    savedSummaries = savedSummaries,
                    shopProfile = _shopProfile.value,
                    context = appContext
                )
                refreshOfflineQueueState()
                updateGoogleSheetsConfig(
                    cfg.copy(
                        syncStatus = SyncStatus.SUCCESS,
                        lastSyncTime = System.currentTimeMillis(),
                        lastSyncStatus = "Offline မှတ်တမ်းများ အစဉ်လိုက် ပြန်လည်ပေးပို့ပြီးပါပြီ",
                        pendingOfflineActionsCount = _offlineQueue.value.size
                    ),
                    triggerImmediateSync = false
                )
            }
            _isGoogleSheetsSyncing.value = false
            onResult?.invoke(success, msg)
        }
    }

    private val _isGoogleSheetsSyncing = MutableStateFlow(false)
    val isGoogleSheetsSyncing: StateFlow<Boolean> = _isGoogleSheetsSyncing.asStateFlow()

    private val _cachedSyncJson = MutableStateFlow(prefs.getString("local_storage_sync_json", "") ?: "")
    val cachedSyncJson: StateFlow<String> = _cachedSyncJson.asStateFlow()

    private val _lastJsonSaveTime = MutableStateFlow(prefs.getLong("local_storage_sync_time", 0L))
    val lastJsonSaveTime: StateFlow<Long> = _lastJsonSaveTime.asStateFlow()

    private var autoSyncJob: Job? = null

    fun startPeriodicAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = null
        val cfg = try { _googleSheetsConfig?.value } catch (_: Exception) { null }
        if (cfg == null || (!cfg.periodicAutoSync && !cfg.autoSyncSales) || cfg.scriptUrl.isBlank()) return
        val intervalMs = (cfg.syncIntervalSeconds.coerceAtLeast(10)) * 1000L

        autoSyncJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(intervalMs)
                val currentCfg = try { _googleSheetsConfig?.value } catch (_: Exception) { null }
                if (currentCfg == null || (!currentCfg.periodicAutoSync && !currentCfg.autoSyncSales) || currentCfg.scriptUrl.isBlank()) {
                    break
                }
                val syncing = try { _isGoogleSheetsSyncing?.value == true } catch (_: Exception) { false }
                if (!syncing) {
                    syncAll(isPeriodic = true)
                }
            }
        }
    }

    fun stopPeriodicAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = null
    }

    fun toggleAutoSync(onToggled: ((Boolean) -> Unit)? = null) {
        val currentCfg = _googleSheetsConfig.value
        val isCurrentlyAuto = currentCfg.periodicAutoSync || currentCfg.autoSyncSales
        val nextAutoState = !isCurrentlyAuto
        val updatedCfg = currentCfg.copy(
            periodicAutoSync = nextAutoState,
            autoSyncSales = nextAutoState,
            lastSyncStatus = if (nextAutoState) "Auto Sync ဖွင့်ထားသည်" else "အလိုအလျောက် ဆွဲတင်ခြင်း ပိတ်ထားပါသည်"
        )
        if (!nextAutoState) {
            stopPeriodicAutoSync()
        }
        updateGoogleSheetsConfig(
            config = updatedCfg,
            triggerImmediateSync = nextAutoState
        )
        onToggled?.invoke(nextAutoState)
    }

    private val repository: PosRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PosRepository(db.productDao(), db.orderDao(), db.savedReceiptDao(), application)
        viewModelScope.launch {
            repository.initDatabaseIfEmpty()
            _shopProfile.value = loadSavedShopProfile()
            _googleSheetsConfig.value = loadSavedGoogleSheetsConfig()
            val savedJson = repository.getJsonFromLocalStorage()
            if (savedJson.isNotBlank()) {
                _cachedSyncJson.value = savedJson
            }
            initializeRefundedHeldOrdersIfNeeded()
            // Only pull from Google Sheets if auto-sync is explicitly configured and enabled
            val cfg = _googleSheetsConfig.value
            if (cfg.autoSyncSales && cfg.scriptUrl.isNotBlank()) {
                pullFromGoogleSheets()
            }
        }
        startPeriodicAutoSync()
    }

    val products: StateFlow<List<Product>> = repository.activeProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrderItems: StateFlow<List<OrderItem>> = repository.allOrderItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedReceipts: StateFlow<List<SavedReceipt>> = repository.savedReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedReceiptsCount: StateFlow<Int> = repository.savedReceiptsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Cart state
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // Held/Parked orders
    private val _heldOrderEntries = MutableStateFlow<List<HeldOrderEntry>>(emptyList())
    val heldOrderEntries: StateFlow<List<HeldOrderEntry>> = _heldOrderEntries.asStateFlow()

    private val _heldCarts = MutableStateFlow<List<List<CartItem>>>(emptyList())
    val heldCarts: StateFlow<List<List<CartItem>>> = _heldCarts.asStateFlow()

    private var activeCartOriginEntry: HeldOrderEntry? = null

    private fun updateHeldEntries(entries: List<HeldOrderEntry>) {
        _heldOrderEntries.value = entries
        _heldCarts.value = entries.map { it.items }
    }

    private fun getDismissedRefundOrderIds(): Set<Long> {
        val raw = prefs.getString("dismissed_refund_order_ids", "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
    }

    private fun markRefundOrderDismissed(orderId: Long) {
        val updated = getDismissedRefundOrderIds() + orderId
        prefs.edit().putString("dismissed_refund_order_ids", updated.joinToString(",")).apply()
    }

    private fun unmarkRefundOrderDismissed(orderId: Long) {
        val updated = getDismissedRefundOrderIds() - orderId
        prefs.edit().putString("dismissed_refund_order_ids", updated.joinToString(",")).apply()
    }

    private suspend fun buildCartItemsForOrder(
        items: List<OrderItem>,
        latestProducts: List<Product>
    ): List<CartItem> {
        val byProductKey = linkedMapOf<String, CartItem>()
        for (item in items) {
            val product = repository.getProductById(item.productId)
                ?: latestProducts.find { it.id == item.productId && item.productId > 0L }
                ?: latestProducts.find { it.name.trim().equals(item.productName.trim(), ignoreCase = true) }
                ?: Product(
                    id = item.productId,
                    name = item.productName,
                    category = "အထွေထွေ",
                    costPrice = item.costPrice,
                    sellingPrice = item.unitPrice,
                    stockQuantity = item.quantity,
                    currency = item.currency
                )
            val isWholesale = (product.wholesalePrice > 0L && item.unitPrice == product.wholesalePrice)
            val key = if (product.id > 0L) "id_${product.id}" else "name_${product.name.trim().lowercase()}"
            val existing = byProductKey[key]
            if (existing != null) {
                byProductKey[key] = existing.copy(
                    product = product,
                    quantity = existing.quantity + item.quantity,
                    isWholesale = existing.isWholesale || isWholesale
                )
            } else {
                byProductKey[key] = CartItem(product, item.quantity, isWholesale)
            }
        }
        return byProductKey.values.toList()
    }

    private fun refreshCartAndHeldProductSnapshots(latestProducts: List<Product>) {
        val prodById = latestProducts.associateBy { it.id }
        if (_cart.value.isNotEmpty()) {
            _cart.value = _cart.value.map { ci ->
                val latest = prodById[ci.product.id] ?: ci.product
                ci.copy(product = latest)
            }
        }
        if (_heldOrderEntries.value.isNotEmpty()) {
            val refreshedHeld = _heldOrderEntries.value.map { entry ->
                entry.copy(
                    items = entry.items.map { ci ->
                        val latest = prodById[ci.product.id] ?: ci.product
                        ci.copy(product = latest)
                    }
                )
            }
            updateHeldEntries(refreshedHeld)
        }
    }

    private suspend fun initializeRefundedHeldOrdersIfNeeded() {
        val allOrders = repository.allOrders.first()
        val allItems = repository.allOrderItems.first()
        val latestProducts = repository.allProducts.first()
        val dismissedIds = getDismissedRefundOrderIds()
        val itemsByOrderId = allItems.groupBy { it.orderId }
        val activeRefundedOrders = allOrders.filter { it.status == OrderStatus.REFUNDED && it.id !in dismissedIds }

        if (activeRefundedOrders.isNotEmpty()) {
            val currentHeld = _heldOrderEntries.value.toMutableList()
            for (rOrder in activeRefundedOrders.sortedBy { it.timestamp }) {
                if (currentHeld.none { it.orderId == rOrder.id } && activeCartOriginEntry?.orderId != rOrder.id) {
                    val cartItems = buildCartItemsForOrder(itemsByOrderId[rOrder.id] ?: emptyList(), latestProducts)
                    if (cartItems.isNotEmpty()) {
                        currentHeld.add(
                            HeldOrderEntry(
                                items = cartItems,
                                orderId = rOrder.id,
                                invoiceNumber = rOrder.invoiceNumber,
                                isRefundedOrder = true
                            )
                        )
                    }
                }
            }
            updateHeldEntries(currentHeld)
        }
    }

    // Filter states
    private val _selectedCategory = MutableStateFlow("အားလုံး")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active receipt to display
    private val _activeReceipt = MutableStateFlow<OrderDetails?>(null)
    val activeReceipt: StateFlow<OrderDetails?> = _activeReceipt.asStateFlow()

    // Shop settings

    private fun loadSavedShopProfile(): ShopProfile {
        val name = prefs.getString("shop_name", "POS System Software")?.takeIf { it.isNotBlank() } ?: "POS System Software"
        val engName = prefs.getString("shop_eng_name", "POS System Software")?.takeIf { it.isNotBlank() } ?: "POS System Software"
        val phone = prefs.getString("shop_phone", "") ?: ""
        val address = prefs.getString("shop_address", "") ?: ""
        val footer = prefs.getString("shop_footer", "") ?: ""
        val cashier = prefs.getString("shop_cashier", "") ?: ""
        val thbSell = prefs.getString("thb_selling_rate", "") ?: ""
        val thbBuy = prefs.getString("thb_buying_rate", "") ?: ""
        val mmkSell = prefs.getString("mmk_selling_rate", "") ?: ""
        val mmkBuy = prefs.getString("mmk_buying_rate", "") ?: ""
        return ShopProfile(
            shopName = name,
            englishName = engName,
            phone = phone,
            address = address,
            receiptFooter = footer,
            cashierName = cashier,
            thbSellingRate = thbSell,
            thbBuyingRate = thbBuy,
            mmkSellingRate = mmkSell,
            mmkBuyingRate = mmkBuy
        )
    }

    private val _shopProfile = MutableStateFlow(loadSavedShopProfile())
    val shopProfile: StateFlow<ShopProfile> = _shopProfile.asStateFlow()


    val cartItemCount: StateFlow<Int> = _cart
        .combine(MutableStateFlow(0)) { cart, _ -> cart.sumOf { it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _isWholesaleMode = MutableStateFlow(false)
    val isWholesaleMode: StateFlow<Boolean> = _isWholesaleMode.asStateFlow()

    // UI Toggle Preferences
    private val _showClearIconButton = MutableStateFlow(prefs.getBoolean("show_clear_icon_button", true))
    val showClearIconButton: StateFlow<Boolean> = _showClearIconButton.asStateFlow()

    private val _showOuterContainerBox = MutableStateFlow(prefs.getBoolean("show_outer_container_box", true))
    val showOuterContainerBox: StateFlow<Boolean> = _showOuterContainerBox.asStateFlow()

    private val _showTopAppBar = MutableStateFlow(prefs.getBoolean("show_top_app_bar", true))
    val showTopAppBar: StateFlow<Boolean> = _showTopAppBar.asStateFlow()

    private val _showCartItemCheckbox = MutableStateFlow(prefs.getBoolean("show_cart_item_checkbox", true))
    val showCartItemCheckbox: StateFlow<Boolean> = _showCartItemCheckbox.asStateFlow()

    private val _showPriceModeCard = MutableStateFlow(prefs.getBoolean("show_price_mode_card", true))
    val showPriceModeCard: StateFlow<Boolean> = _showPriceModeCard.asStateFlow()

    private val _showAddProductButton = MutableStateFlow(prefs.getBoolean("show_add_product_button", true))
    val showAddProductButton: StateFlow<Boolean> = _showAddProductButton.asStateFlow()

    private val _showSavedReceiptActions = MutableStateFlow(prefs.getBoolean("show_saved_receipt_actions", true))
    val showSavedReceiptActions: StateFlow<Boolean> = _showSavedReceiptActions.asStateFlow()

    private val _showCheckoutDetails = MutableStateFlow(prefs.getBoolean("show_checkout_details", true))
    val showCheckoutDetails: StateFlow<Boolean> = _showCheckoutDetails.asStateFlow()

    private val _showProfitInfo = MutableStateFlow(prefs.getBoolean("show_profit_info", true))
    val showProfitInfo: StateFlow<Boolean> = _showProfitInfo.asStateFlow()

    private val _showCartWholesaleCheckmark = MutableStateFlow(prefs.getBoolean("show_cart_wholesale_checkmark", true))
    val showCartWholesaleCheckmark: StateFlow<Boolean> = _showCartWholesaleCheckmark.asStateFlow()

    // Per-product Cashier card details hide/show state (controlled by the Hide/Show icon button before Edit in Inventory)
    private fun loadHiddenCashierProductIds(): Set<Long> {
        val raw = prefs.getString("hidden_cashier_product_ids", null)
        if (raw == null) return emptySet()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
    }

    private val _hiddenCashierProductIds = MutableStateFlow<Set<Long>>(loadHiddenCashierProductIds())
    val hiddenCashierProductIds: StateFlow<Set<Long>> = _hiddenCashierProductIds.asStateFlow()

    private val _cashierCardDefaultHide = MutableStateFlow(prefs.getBoolean("cashier_card_default_hide", true))
    val cashierCardDefaultHide: StateFlow<Boolean> = _cashierCardDefaultHide.asStateFlow()

    // Track explicit per-product overrides: Map<productId, isHiddenOnCashier>
    private fun loadCashierProductHideOverrides(): Map<Long, Boolean> {
        val raw = prefs.getString("cashier_product_hide_overrides", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val id = parts[0].toLongOrNull()
                val hidden = parts[1].toBooleanStrictOrNull()
                if (id != null && hidden != null) id to hidden else null
            } else null
        }.toMap()
    }

    private val _cashierProductHideOverrides = MutableStateFlow<Map<Long, Boolean>>(loadCashierProductHideOverrides())
    val cashierProductHideOverrides: StateFlow<Map<Long, Boolean>> = _cashierProductHideOverrides.asStateFlow()

    fun isProductHiddenDetailsOnCashier(productId: Long): Boolean {
        return _cashierProductHideOverrides.value[productId] ?: _cashierCardDefaultHide.value
    }

    fun toggleProductCashierDetailsHide(productId: Long) {
        val currentMap = _cashierProductHideOverrides.value.toMutableMap()
        val currentState = currentMap[productId] ?: _cashierCardDefaultHide.value
        val nextState = !currentState
        currentMap[productId] = nextState
        _cashierProductHideOverrides.value = currentMap
        val serialized = currentMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        prefs.edit().putString("cashier_product_hide_overrides", serialized).apply()
    }

    // Thai Baht Exchange Rates (Derived from Shop Profile for ease of use in other screens)
    val thbSellingRate: StateFlow<String> = _shopProfile
        .map { it.thbSellingRate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _shopProfile.value.thbSellingRate)

    val thbBuyingRate: StateFlow<String> = _shopProfile
        .map { it.thbBuyingRate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _shopProfile.value.thbBuyingRate)

    val mmkSellingRate: StateFlow<String> = _shopProfile
        .map { it.mmkSellingRate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _shopProfile.value.mmkSellingRate)

    val mmkBuyingRate: StateFlow<String> = _shopProfile
        .map { it.mmkBuyingRate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _shopProfile.value.mmkBuyingRate)

    // Cart calculations
    val cartSubtotal: StateFlow<Long> = combine(_cart, thbSellingRate, mmkSellingRate) { cart, sellRateStr, mmkSellStr ->
            val thbSell = sellRateStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            val mmkSell = mmkSellStr.replace(",", ".").toDoubleOrNull() ?: (if (thbSell > 0.0) 1.0 / thbSell else 0.0)
            val rate = if (mmkSell > 0.0) mmkSell else thbSell
            cart.sumOf { item ->
                item.getSubtotalMmk(rate)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun updateThbRates(sellingRate: String, buyingRate: String) {
        val currentProfile = _shopProfile.value
        val sRate = sellingRate.toDoubleOrNull() ?: 0.0
        val bRate = buyingRate.toDoubleOrNull() ?: 0.0
        val mmkSell = if (sRate > 0) String.format(java.util.Locale.US, "%.5f", 1.0 / sRate) else ""
        val mmkBuy = if (bRate > 0) String.format(java.util.Locale.US, "%.5f", 1.0 / bRate) else ""
        val updatedProfile = currentProfile.copy(
            thbSellingRate = sellingRate,
            thbBuyingRate = buyingRate,
            mmkSellingRate = mmkSell,
            mmkBuyingRate = mmkBuy
        )
        updateShopProfile(updatedProfile, syncToCloudIfEnabled = true)
    }

    fun updateMmkRates(sellingRate: String, buyingRate: String) {
        val currentProfile = _shopProfile.value
        val mmkSell = sellingRate.toDoubleOrNull() ?: 0.0
        val mmkBuy = buyingRate.toDoubleOrNull() ?: 0.0
        val thbSell = if (mmkSell > 0) String.format(java.util.Locale.US, "%.2f", 1.0 / mmkSell) else ""
        val thbBuy = if (mmkBuy > 0) String.format(java.util.Locale.US, "%.2f", 1.0 / mmkBuy) else ""
        val updatedProfile = currentProfile.copy(
            thbSellingRate = thbSell,
            thbBuyingRate = thbBuy,
            mmkSellingRate = sellingRate,
            mmkBuyingRate = buyingRate
        )
        updateShopProfile(updatedProfile, syncToCloudIfEnabled = true)
    }

    fun toggleShowClearIconButton(show: Boolean) {
        _showClearIconButton.value = show
        prefs.edit().putBoolean("show_clear_icon_button", show).apply()
    }

    fun toggleShowOuterContainerBox(show: Boolean) {
        _showOuterContainerBox.value = show
        prefs.edit().putBoolean("show_outer_container_box", show).apply()
    }

    fun toggleShowTopAppBar(show: Boolean) {
        _showTopAppBar.value = show
        prefs.edit().putBoolean("show_top_app_bar", show).apply()
    }

    fun toggleShowCartItemCheckbox(show: Boolean) {
        _showCartItemCheckbox.value = show
        prefs.edit().putBoolean("show_cart_item_checkbox", show).apply()
    }

    fun toggleShowPriceModeCard(show: Boolean) {
        _showPriceModeCard.value = show
        prefs.edit().putBoolean("show_price_mode_card", show).apply()
    }

    fun toggleShowWholesale(show: Boolean) {
        _showCartItemCheckbox.value = show
        _showPriceModeCard.value = show
        prefs.edit()
            .putBoolean("show_cart_item_checkbox", show)
            .putBoolean("show_price_mode_card", show)
            .apply()
    }

    fun toggleShowAddProductButton(show: Boolean) {
        _showAddProductButton.value = show
        prefs.edit().putBoolean("show_add_product_button", show).apply()
    }

    fun toggleShowSavedReceiptActions(show: Boolean) {
        _showSavedReceiptActions.value = show
        prefs.edit().putBoolean("show_saved_receipt_actions", show).apply()
    }

    fun toggleShowCheckoutDetails(show: Boolean) {
        _showCheckoutDetails.value = show
        prefs.edit().putBoolean("show_checkout_details", show).apply()
    }

    fun toggleShowProfitInfo(show: Boolean) {
        _showProfitInfo.value = show
        prefs.edit().putBoolean("show_profit_info", show).apply()
    }

    fun toggleShowCartWholesaleCheckmark(show: Boolean) {
        _showCartWholesaleCheckmark.value = show
        prefs.edit().putBoolean("show_cart_wholesale_checkmark", show).apply()
        if (!show) {
            uncheckAllCartWholesale()
        }
    }

    fun toggleWholesaleMode() {
        val nextMode = !_isWholesaleMode.value
        _isWholesaleMode.value = nextMode
        // Update all existing items in cart to match this mode
        _cart.value = _cart.value.map { it.copy(isWholesale = nextMode) }
    }

    fun toggleItemWholesale(productId: Long) {
        _cart.value = _cart.value.map {
            if (it.product.id == productId) {
                it.copy(isWholesale = !it.isWholesale)
            } else {
                it
            }
        }
    }

    fun toggleAllCartItemsWholesale() {
        val allAreWholesale = _cart.value.all { it.isWholesale }
        _cart.value = _cart.value.map {
            it.copy(isWholesale = !allAreWholesale)
        }
    }

    fun uncheckAllCartWholesale() {
        _cart.value = _cart.value.map {
            it.copy(isWholesale = false)
        }
    }

    fun addToCart(product: Product) {
        val currentList = _cart.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val item = currentList[existingIndex]
            if (item.quantity < product.stockQuantity) {
                currentList[existingIndex] = item.copy(
                    quantity = item.quantity + 1,
                    isWholesale = _isWholesaleMode.value
                )
            }
        } else {
            if (product.stockQuantity > 0) {
                currentList.add(CartItem(product, 1, isWholesale = _isWholesaleMode.value))
            }
        }
        _cart.value = currentList
    }

    fun decreaseOrRemoveFromCart(product: Product) {
        val currentList = _cart.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val item = currentList[existingIndex]
            if (item.quantity > 1) {
                currentList[existingIndex] = item.copy(quantity = item.quantity - 1)
            } else {
                currentList.removeAt(existingIndex)
            }
            _cart.value = currentList
        }
    }

    fun removeFromCart(productId: Long) {
        val next = _cart.value.filter { it.product.id != productId }
        _cart.value = next
        if (next.isEmpty()) {
            activeCartOriginEntry?.orderId?.let { markRefundOrderDismissed(it) }
            activeCartOriginEntry = null
        }
    }

    fun clearCart() {
        activeCartOriginEntry?.orderId?.let { markRefundOrderDismissed(it) }
        activeCartOriginEntry = null
        _cart.value = emptyList()
    }

    fun holdCurrentCart() {
        if (_cart.value.isNotEmpty()) {
            val entry = activeCartOriginEntry?.copy(items = _cart.value)
                ?: HeldOrderEntry(items = _cart.value)
            val updated = _heldOrderEntries.value.toMutableList()
            if (entry.orderId != null) {
                val existingIdx = updated.indexOfFirst { it.orderId == entry.orderId }
                if (existingIdx >= 0) {
                    updated[existingIdx] = entry
                } else {
                    updated.add(entry)
                }
            } else {
                updated.add(entry)
            }
            updateHeldEntries(updated)
            _cart.value = emptyList()
            activeCartOriginEntry = null
        }
    }

    fun recallHeldCart(index: Int) {
        val held = _heldOrderEntries.value.toMutableList()
        if (index in held.indices) {
            val recalled = held.removeAt(index)
            if (_cart.value.isNotEmpty()) {
                val currentEntry = activeCartOriginEntry?.copy(items = _cart.value)
                    ?: HeldOrderEntry(items = _cart.value)
                held.add(index.coerceAtMost(held.size), currentEntry)
            }
            updateHeldEntries(held)
            _cart.value = recalled.items
            activeCartOriginEntry = recalled
            _isWholesaleMode.value = recalled.items.any { it.isWholesale }
        }
    }

    fun checkout(
        discount: Long,
        paymentMethod: PaymentMethod,
        amountTendered: Long,
        customerName: String,
        customerPhone: String,
        note: String,
        currency: String = "MMK",
        totalMmk: Long = 0L,
        totalThb: Long = 0L,
        exchangeRate: Double = 0.0,
        onSuccess: () -> Unit = {}
    ) {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return

        viewModelScope.launch {
            val thbSell = thbSellingRate.value.replace(",", ".").toDoubleOrNull() ?: 0.0
            val thbBuy = thbBuyingRate.value.replace(",", ".").toDoubleOrNull() ?: thbSell
            val mmkSell = mmkSellingRate.value.replace(",", ".").toDoubleOrNull() ?: (if (thbSell > 0.0) 1.0 / thbSell else 0.0)
            val mmkBuy = mmkBuyingRate.value.replace(",", ".").toDoubleOrNull() ?: (if (thbBuy > 0.0) 1.0 / thbBuy else mmkSell)
            val sellRate = if (mmkSell > 0.0) com.example.data.toThbToMmkRate(mmkSell) else thbSell
            val buyRate = if (mmkBuy > 0.0) com.example.data.toThbToMmkRate(mmkBuy) else thbBuy
            val effRate = if (exchangeRate > 0.0) com.example.data.toThbToMmkRate(exchangeRate) else (if (currency == "THB") buyRate else sellRate)
            val result = repository.checkout(
                cartItems = currentCart,
                discount = discount,
                paymentMethod = paymentMethod,
                amountTendered = amountTendered,
                customerName = customerName,
                customerPhone = customerPhone,
                note = note,
                thbSellRate = sellRate,
                currency = currency,
                totalMmk = totalMmk,
                totalThb = totalThb,
                exchangeRate = effRate
            )
            _cart.value = emptyList()
            activeCartOriginEntry?.orderId?.let { markRefundOrderDismissed(it) }
            activeCartOriginEntry = null
            _activeReceipt.value = result
            onSuccess()

            // Auto sync to Google Sheets if enabled (or record in chronological offline queue if offline/disabled)
            val appContext = getApplication<Application>()
            if (_googleSheetsConfig.value.autoSyncSales && _googleSheetsConfig.value.scriptUrl.isNotBlank()) {
                viewModelScope.launch {
                    val (success, _) = googleSheetsSyncManager.syncSingleOrder(
                        config = _googleSheetsConfig.value,
                        orderDetails = result,
                        products = repository.allProducts.first(),
                        shopProfile = _shopProfile.value,
                        context = appContext
                    )
                    refreshOfflineQueueState()
                    triggerBackgroundSheetsAndLocalCacheUpdate(
                        syncOrdersToo = false,
                        syncSavedToo = false,
                        syncProductsToo = success
                    )
                    if (success) {
                        Toast.makeText(getApplication(), "Google Sheets သို့ အရောင်းနှင့် လက်ကျန်ပစ္စည်း စာရင်းတင်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Record sale & stock deduction in chronological offline queue so when internet returns / user syncs, it replays in order
                viewModelScope.launch {
                    val latestProducts = repository.allProducts.first()
                    val payload = JSONObject().apply {
                        put("action", "append_order")
                        put("deductStock", true)
                        put("order", googleSheetsSyncManager.run {
                            // Build order payload with items and barcode
                            val calc = ReceiptCalculator.calculateFromOrder(result.order, result.items, latestProducts, _shopProfile.value)
                            val prodById = latestProducts.associateBy { it.id }
                            JSONObject().apply {
                                put("invoiceNumber", result.order.invoiceNumber)
                                put("timestamp", result.order.timestamp)
                                put("dateTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(result.order.timestamp)))
                                put("itemsSummary", result.items.joinToString("; ") { "${it.productName} x${it.quantity}" })
                                put("totalUnits", calc.totalUnitsSold)
                                put("subtotal", result.order.subtotal)
                                put("discount", result.order.discount)
                                put("grandTotal", result.order.getEffectiveTotalMmk())
                                put("totalMmk", calc.sellingPriceMmk)
                                put("totalThb", calc.sellingPriceThb)
                                put("profitMmk", calc.estimatedNetProfitMmk)
                                put("profitThb", calc.estimatedNetProfitThb)
                                put("paymentMethod", result.order.paymentMethod.labelMm)
                                put("customerName", result.order.customerName)
                                put("customerPhone", result.order.customerPhone)
                                put("status", result.order.status.name)
                                put("note", result.order.note)
                                val itemsArr = org.json.JSONArray()
                                result.items.forEach { item ->
                                    val matchedProd = prodById[item.productId]
                                    itemsArr.put(JSONObject().apply {
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
                                put("items", itemsArr)
                            }
                        })
                    }
                    googleSheetsSyncManager.enqueueOfflineAction(
                        appContext,
                        "append_order",
                        "အရောင်းဘောက်ချာ ${result.order.invoiceNumber} (ရောင်းပြီးပစ္စည်း နုတ်ယူရန်)",
                        payload
                    )
                    refreshOfflineQueueState()
                    triggerBackgroundSheetsAndLocalCacheUpdate()
                }
            }
        }
    }

    fun showReceipt(orderId: Long) {
        viewModelScope.launch {
            val details = repository.getOrderDetails(orderId)
            _activeReceipt.value = details
        }
    }

    fun closeReceipt() {
        _activeReceipt.value = null
    }

    fun clearAndSaveReceiptToScreen1(orderDetails: OrderDetails, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            if (orderDetails.order.status == OrderStatus.REFUNDED) {
                Toast.makeText(
                    getApplication(),
                    "ငွေပြန်အမ်းပြီးသော ပြေစာဖြစ်၍ ရှင်းပြီး ပြေစာများ တွင် သိမ်းဆည်းခွင့် မပြုပါ",
                    Toast.LENGTH_LONG
                ).show()
                onComplete?.invoke(false)
                return@launch
            }

            val result = repository.clearReceiptAndSaveToNewScreen1(orderDetails)
            when (result) {
                SaveReceiptResult.SUCCESS -> {
                    _activeReceipt.value = null
                    Toast.makeText(
                        getApplication(),
                        "ပြေစာအား ရှင်းပြီး ပြေစာများ သို့ အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete?.invoke(true)
                    triggerBackgroundSheetsAndLocalCacheUpdate(syncOrdersToo = true, syncSavedToo = true)
                }
                SaveReceiptResult.DUPLICATE_INVOICE -> {
                    Toast.makeText(
                        getApplication(),
                        "ဘောက်ချာနံပါတ် (${orderDetails.order.invoiceNumber}) တူညီသော ဘောက်ချာ ရှိပြီးဖြစ်၍ ထပ်မံသိမ်းဆည်းခွင့် မပြုပါ",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete?.invoke(false)
                }
                SaveReceiptResult.REFUNDED_NOT_ALLOWED -> {
                    Toast.makeText(
                        getApplication(),
                        "ငွေပြန်အမ်းပြီးသော ပြေစာဖြစ်၍ ရှင်းပြီး ပြေစာများ တွင် သိမ်းဆည်းခွင့် မပြုပါ",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete?.invoke(false)
                }
                SaveReceiptResult.FAILED -> {
                    Toast.makeText(
                        getApplication(),
                        "သိမ်းဆည်းမှု မအောင်မြင်ပါ",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete?.invoke(false)
                }
            }
        }
    }

    fun showSavedReceipt(savedReceipt: SavedReceipt) {
        _activeReceipt.value = savedReceipt.toOrderDetails()
    }

    fun deleteSavedReceipt(id: Long) {
        viewModelScope.launch {
            val existing = repository.getSavedReceiptById(id)
            repository.deleteSavedReceipt(id)
            val cfg = _googleSheetsConfig.value
            val appContext = getApplication<Application>()
            if (existing != null) {
                if ((cfg.autoSyncSales || cfg.periodicAutoSync) && cfg.scriptUrl.isNotBlank()) {
                    googleSheetsSyncManager.syncDeleteSavedReceipt(cfg, existing.invoiceNumber, appContext)
                    refreshOfflineQueueState()
                } else {
                    val payload = JSONObject().apply {
                        put("action", "delete_saved_receipt")
                        put("invoiceNumber", existing.invoiceNumber)
                    }
                    googleSheetsSyncManager.enqueueOfflineAction(
                        appContext,
                        "delete_saved_receipt",
                        "ရှင်းပြီးပြေစာ ဖျက်ခြင်း (${existing.invoiceNumber})",
                        payload
                    )
                    refreshOfflineQueueState()
                }
            }
            triggerBackgroundSheetsAndLocalCacheUpdate(syncOrdersToo = false, syncSavedToo = true)
        }
    }

    fun deleteAllSavedReceipts() {
        viewModelScope.launch {
            repository.deleteAllSavedReceipts()
            triggerBackgroundSheetsAndLocalCacheUpdate(syncOrdersToo = false, syncSavedToo = true)
        }
    }

    fun restoreSavedReceipt(id: Long) {
        viewModelScope.launch {
            repository.restoreSavedReceiptToOrders(id)
            triggerBackgroundSheetsAndLocalCacheUpdate(syncOrdersToo = true, syncSavedToo = true)
        }
    }

    fun refundOrder(orderId: Long) {
        viewModelScope.launch {
            val details = repository.getOrderDetails(orderId)

            // Refund in database (marks refunded and restores stock in DB)
            repository.refundOrder(orderId)
            unmarkRefundOrderDismissed(orderId)

            val latestProducts = repository.allProducts.first()
            refreshCartAndHeldProductSnapshots(latestProducts)

            if (details != null) {
                val restoredCartItems = buildCartItemsForOrder(details.items, latestProducts)
                val allOrders = repository.allOrders.first()
                val allItems = repository.allOrderItems.first()
                val itemsByOrderId = allItems.groupBy { it.orderId }
                val totalRefundedOrders = allOrders.filter { it.status == OrderStatus.REFUNDED }
                val dismissedIds = getDismissedRefundOrderIds()
                val activeRefundedOrders = totalRefundedOrders.filter { it.id !in dismissedIds }

                // Store all refunded orders inside Held Orders (ဆိုင်းငံ့ထားသော အော်ဒါများ) just like held orders
                val updatedHeld = _heldOrderEntries.value.toMutableList()

                if (restoredCartItems.isNotEmpty()) {
                    val existingIdx = updatedHeld.indexOfFirst { it.orderId == orderId }
                    val entry = HeldOrderEntry(
                        items = restoredCartItems,
                        orderId = orderId,
                        invoiceNumber = details.order.invoiceNumber,
                        isRefundedOrder = true
                    )
                    if (existingIdx >= 0) {
                        updatedHeld[existingIdx] = entry
                    } else {
                        updatedHeld.add(entry)
                    }
                }

                // Ensure any other active refunded orders are also present in Held Orders
                for (rOrder in activeRefundedOrders.sortedBy { it.timestamp }) {
                    if (rOrder.id != orderId && updatedHeld.none { it.orderId == rOrder.id } && activeCartOriginEntry?.orderId != rOrder.id) {
                        val rItems = buildCartItemsForOrder(itemsByOrderId[rOrder.id] ?: emptyList(), latestProducts)
                        if (rItems.isNotEmpty()) {
                            updatedHeld.add(
                                HeldOrderEntry(
                                    items = rItems,
                                    orderId = rOrder.id,
                                    invoiceNumber = rOrder.invoiceNumber,
                                    isRefundedOrder = true
                                )
                            )
                        }
                    }
                }

                updateHeldEntries(updatedHeld)
            }

            // Refresh the displayed receipt
            val updatedDetails = repository.getOrderDetails(orderId)
            _activeReceipt.value = updatedDetails
            Toast.makeText(
                getApplication(),
                "ငွေပြန်အမ်းပြီးပါပြီ • လက်ကျန်ပြန်ဖြည့်ပြီး ဆိုင်းငံ့ထားသော အော်ဒါများ (Held Orders) တွင် မှတ်သားပြီးပါပြီ",
                Toast.LENGTH_LONG
            ).show()

            val cfg = _googleSheetsConfig.value
            val appContext = getApplication<Application>()
            if (updatedDetails != null) {
                val latestProducts = repository.allProducts.first()
                if ((cfg.autoSyncSales || cfg.periodicAutoSync) && cfg.scriptUrl.isNotBlank()) {
                    googleSheetsSyncManager.syncRefundOrder(
                        config = cfg,
                        orderDetails = updatedDetails,
                        products = latestProducts,
                        isUndoRefund = false,
                        context = appContext
                    )
                    refreshOfflineQueueState()
                } else {
                    val prodById = latestProducts.associateBy { it.id }
                    val itemsArr = org.json.JSONArray()
                    updatedDetails.items.forEach { item ->
                        val matchedProd = prodById[item.productId]
                        itemsArr.put(JSONObject().apply {
                            put("productId", item.productId)
                            put("barcode", matchedProd?.barcode ?: "")
                            put("productName", item.productName)
                            put("quantity", item.quantity)
                        })
                    }
                    val payload = JSONObject().apply {
                        put("action", "refund_order")
                        put("invoiceNumber", updatedDetails.order.invoiceNumber)
                        put("items", itemsArr)
                    }
                    googleSheetsSyncManager.enqueueOfflineAction(
                        appContext,
                        "refund_order",
                        "ငွေပြန်အမ်းခြင်းနှင့် လက်ကျန်ပြန်ပေါင်းခြင်း (${updatedDetails.order.invoiceNumber})",
                        payload
                    )
                    refreshOfflineQueueState()
                }
            }
            triggerBackgroundSheetsAndLocalCacheUpdate(syncOrdersToo = true, syncSavedToo = false, syncProductsToo = true)
        }
    }

    fun undoRefundOrder(orderId: Long) {
        viewModelScope.launch {
            val details = repository.getOrderDetails(orderId)
            repository.undoRefundOrder(orderId)
            unmarkRefundOrderDismissed(orderId)

            // Remove from Held Orders if this refunded order was stored there
            if (_heldOrderEntries.value.any { it.orderId == orderId }) {
                updateHeldEntries(_heldOrderEntries.value.filterNot { it.orderId == orderId })
            }

            // Clear the items that were sent to the cart when refund was confirmed
            if (activeCartOriginEntry?.orderId == orderId) {
                _cart.value = emptyList()
                activeCartOriginEntry = null
                _isWholesaleMode.value = false
            } else if (details != null && _cart.value.isNotEmpty()) {
                val refundedQtyByProductId = details.items.groupBy { it.productId }
                    .mapValues { entry -> entry.value.sumOf { it.quantity } }
                val updatedCart = _cart.value.mapNotNull { cartItem ->
                    val refundedQty = refundedQtyByProductId[cartItem.product.id] ?: 0
                    if (refundedQty > 0) {
                        val remainingQty = cartItem.quantity - refundedQty
                        if (remainingQty > 0) cartItem.copy(quantity = remainingQty) else null
                    } else {
                        cartItem
                    }
                }
                _cart.value = updatedCart
                if (updatedCart.isEmpty()) {
                    activeCartOriginEntry = null
                    _isWholesaleMode.value = false
                }
            }

            val latestProdsAfterUndo = repository.allProducts.first()
            refreshCartAndHeldProductSnapshots(latestProdsAfterUndo)

            val updatedDetails = repository.getOrderDetails(orderId)
            _activeReceipt.value = updatedDetails
            Toast.makeText(
                getApplication(),
                "မူလအရောင်းမှတ်တမ်းအတိုင်း ပြန်လည်ထားရှိပြီးပါပြီ (✓ အတည်ပြုပြီး)",
                Toast.LENGTH_SHORT
            ).show()

            val cfg = _googleSheetsConfig.value
            val appContext = getApplication<Application>()
            if (updatedDetails != null) {
                val latestProducts = repository.allProducts.first()
                if ((cfg.autoSyncSales || cfg.periodicAutoSync) && cfg.scriptUrl.isNotBlank()) {
                    googleSheetsSyncManager.syncRefundOrder(
                        config = cfg,
                        orderDetails = updatedDetails,
                        products = latestProducts,
                        isUndoRefund = true,
                        context = appContext
                    )
                    refreshOfflineQueueState()
                } else {
                    val prodById = latestProducts.associateBy { it.id }
                    val itemsArr = org.json.JSONArray()
                    updatedDetails.items.forEach { item ->
                        val matchedProd = prodById[item.productId]
                        itemsArr.put(JSONObject().apply {
                            put("productId", item.productId)
                            put("barcode", matchedProd?.barcode ?: "")
                            put("productName", item.productName)
                            put("quantity", item.quantity)
                        })
                    }
                    val payload = JSONObject().apply {
                        put("action", "undo_refund_order")
                        put("invoiceNumber", updatedDetails.order.invoiceNumber)
                        put("items", itemsArr)
                    }
                    googleSheetsSyncManager.enqueueOfflineAction(
                        appContext,
                        "undo_refund_order",
                        "ငွေပြန်အမ်းမှု ပယ်ဖျက်ပြီး မူလအရောင်းအတိုင်း ပြန်ထားခြင်း (${updatedDetails.order.invoiceNumber})",
                        payload
                    )
                    refreshOfflineQueueState()
                }
            }
            triggerBackgroundSheetsAndLocalCacheUpdate(syncOrdersToo = true, syncSavedToo = false, syncProductsToo = true)
        }
    }

    fun deleteOrder(orderId: Long) {
        viewModelScope.launch {
            val existingDetails = repository.getOrderDetails(orderId)
            val wasCompleted = existingDetails?.order?.status == OrderStatus.COMPLETED
            repository.deleteOrder(orderId)
            if (_heldOrderEntries.value.any { it.orderId == orderId }) {
                updateHeldEntries(_heldOrderEntries.value.filterNot { it.orderId == orderId })
            }
            if (activeCartOriginEntry?.orderId == orderId) {
                _cart.value = emptyList()
                activeCartOriginEntry = null
                _isWholesaleMode.value = false
            }
            _activeReceipt.value = null

            val cfg = _googleSheetsConfig.value
            val appContext = getApplication<Application>()
            if (existingDetails != null) {
                val latestProducts = repository.allProducts.first()
                if ((cfg.autoSyncSales || cfg.periodicAutoSync) && cfg.scriptUrl.isNotBlank()) {
                    googleSheetsSyncManager.syncDeleteOrder(
                        config = cfg,
                        orderDetails = existingDetails,
                        products = latestProducts,
                        restoreStock = wasCompleted,
                        context = appContext
                    )
                    refreshOfflineQueueState()
                } else {
                    val prodById = latestProducts.associateBy { it.id }
                    val itemsArr = org.json.JSONArray()
                    existingDetails.items.forEach { item ->
                        val matchedProd = prodById[item.productId]
                        itemsArr.put(JSONObject().apply {
                            put("productId", item.productId)
                            put("barcode", matchedProd?.barcode ?: "")
                            put("productName", item.productName)
                            put("quantity", item.quantity)
                        })
                    }
                    val payload = JSONObject().apply {
                        put("action", "delete_order")
                        put("invoiceNumber", existingDetails.order.invoiceNumber)
                        put("restoreStock", wasCompleted)
                        put("items", itemsArr)
                    }
                    googleSheetsSyncManager.enqueueOfflineAction(
                        appContext,
                        "delete_order",
                        "အရောင်းဘောက်ချာ ဖျက်ခြင်း (${existingDetails.order.invoiceNumber})",
                        payload
                    )
                    refreshOfflineQueueState()
                }
            }
            triggerBackgroundSheetsAndLocalCacheUpdate(syncOrdersToo = true, syncSavedToo = false, syncProductsToo = true)
        }
    }

    private fun triggerBackgroundSheetsAndLocalCacheUpdate(
        syncOrdersToo: Boolean = false,
        syncSavedToo: Boolean = false,
        syncProductsToo: Boolean = false
    ) {
        viewModelScope.launch {
            val cfg = _googleSheetsConfig.value
            val appContext = getApplication<Application>()
            val latestProducts = repository.allProducts.first()
            val latestOrders = repository.allOrders.first()
            val latestOrderItems = repository.allOrderItems.first()
            val latestSavedReceipts = repository.savedReceipts.first()
            val salesSummaries = buildSalesSummaries()
            val savedSummaries = buildSavedReceiptsSummaries()

            val fullJsonStr = googleSheetsSyncManager.buildLocalStorageJson(
                config = cfg,
                products = latestProducts,
                orders = latestOrders,
                orderItems = latestOrderItems,
                savedReceipts = latestSavedReceipts,
                salesSummaries = salesSummaries,
                savedSummaries = savedSummaries,
                shopProfile = _shopProfile.value,
                pullResult = null,
                context = appContext
            )
            repository.saveJsonToLocalStorage(fullJsonStr)
            _cachedSyncJson.value = fullJsonStr
            _lastJsonSaveTime.value = System.currentTimeMillis()

            if ((cfg.autoSyncSales || cfg.periodicAutoSync) && cfg.scriptUrl.isNotBlank()) {
                if (syncProductsToo) {
                    googleSheetsSyncManager.syncProducts(cfg, latestProducts, appContext)
                }
                if (syncOrdersToo) {
                    googleSheetsSyncManager.syncOrders(cfg, latestOrders, latestOrderItems, latestProducts, _shopProfile.value)
                }
                if (syncSavedToo) {
                    googleSheetsSyncManager.syncSavedReceipts(cfg, latestSavedReceipts, latestProducts, _shopProfile.value)
                }
                if (syncOrdersToo || syncSavedToo) {
                    googleSheetsSyncManager.syncSummaries(cfg, salesSummaries, savedSummaries)
                }
            }
        }
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            val isNew = product.id == 0L
            val savedId = if (isNew) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
                product.id
            }

            val savedProd = product.copy(id = savedId)
            val cfg = _googleSheetsConfig.value

            Toast.makeText(
                getApplication(),
                if (isNew) "ပစ္စည်းအသစ် '${savedProd.name}' အား သိမ်းဆည်းပြီးပါပြီ" else "ပစ္စည်း '${savedProd.name}' အား ပြင်ဆင်သိမ်းဆည်းပြီးပါပြီ",
                Toast.LENGTH_SHORT
            ).show()

            // Upload local image to Google Drive and sync product list & full state to Google Sheets immediately
            val appContext = getApplication<Application>()
            if ((cfg.autoSyncSales || cfg.periodicAutoSync) && cfg.scriptUrl.isNotBlank()) {
                var finalProd = savedProd
                if (savedProd.imageUri.isNotBlank() &&
                    (com.example.util.ProductImageHelper.isLocalOnlyImageUri(savedProd.imageUri) ||
                        savedProd.imageUri.startsWith("data:image", ignoreCase = true))
                ) {
                    val cloudUrl = googleSheetsSyncManager.uploadProductImageToDrive(cfg, appContext, savedProd)
                    if (!cloudUrl.isNullOrBlank() && cloudUrl != savedProd.imageUri) {
                        finalProd = savedProd.copy(imageUri = cloudUrl)
                        repository.updateProduct(finalProd)
                    }
                }
                val latestProducts = repository.allProducts.first()
                val (success, _) = googleSheetsSyncManager.syncProducts(cfg, latestProducts, appContext)
                refreshOfflineQueueState()

                // Also update local storage JSON with the updated products
                val latestOrders = repository.allOrders.first()
                val latestOrderItems = repository.allOrderItems.first()
                val latestSavedReceipts = repository.savedReceipts.first()
                val salesSummaries = buildSalesSummaries()
                val savedSummaries = buildSavedReceiptsSummaries()
                val fullJsonStr = googleSheetsSyncManager.buildLocalStorageJson(
                    config = cfg,
                    products = latestProducts,
                    orders = latestOrders,
                    orderItems = latestOrderItems,
                    savedReceipts = latestSavedReceipts,
                    salesSummaries = salesSummaries,
                    savedSummaries = savedSummaries,
                    shopProfile = _shopProfile.value,
                    pullResult = null,
                    context = appContext
                )
                repository.saveJsonToLocalStorage(fullJsonStr)
                _cachedSyncJson.value = fullJsonStr
                _lastJsonSaveTime.value = System.currentTimeMillis()

                if (success) {
                    Toast.makeText(
                        getApplication(),
                        "Google Sheet သို့ ပစ္စည်းအချက်အလက်များ တိုက်ရိုက် သိမ်းဆည်းပြီးပါပြီ",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                val payload = JSONObject().apply {
                    put("action", "upsert_product")
                    put("product", JSONObject().apply {
                        put("id", savedProd.id)
                        put("barcode", savedProd.barcode)
                        put("name", savedProd.name)
                        put("category", savedProd.category)
                        put("costPrice", savedProd.costPrice)
                        put("sellingPrice", savedProd.sellingPrice)
                        put("wholesalePrice", savedProd.wholesalePrice)
                        put("stockQuantity", savedProd.stockQuantity)
                        put("unit", savedProd.unit)
                        put("currency", savedProd.currency)
                        put("imageUri", savedProd.imageUri)
                        put("isAvailable", savedProd.isAvailable)
                    })
                }
                googleSheetsSyncManager.enqueueOfflineAction(
                    appContext,
                    "upsert_product",
                    if (isNew) "ပစ္စည်းအသစ် ထည့်သွင်းခြင်း (${savedProd.name})" else "ပစ္စည်း ပြင်ဆင်ခြင်း (${savedProd.name})",
                    payload
                )
                refreshOfflineQueueState()
                triggerBackgroundSheetsAndLocalCacheUpdate()
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            val cfg = _googleSheetsConfig.value
            val appContext = getApplication<Application>()
            if ((cfg.autoSyncSales || cfg.periodicAutoSync) && cfg.scriptUrl.isNotBlank()) {
                val latestProducts = repository.allProducts.first()
                googleSheetsSyncManager.syncDeleteProduct(
                    config = cfg,
                    deletedProduct = product,
                    remainingProducts = latestProducts,
                    context = appContext
                )
                refreshOfflineQueueState()

                val latestOrders = repository.allOrders.first()
                val latestOrderItems = repository.allOrderItems.first()
                val latestSavedReceipts = repository.savedReceipts.first()
                val salesSummaries = buildSalesSummaries()
                val savedSummaries = buildSavedReceiptsSummaries()
                val fullJsonStr = googleSheetsSyncManager.buildLocalStorageJson(
                    config = cfg,
                    products = latestProducts,
                    orders = latestOrders,
                    orderItems = latestOrderItems,
                    savedReceipts = latestSavedReceipts,
                    salesSummaries = salesSummaries,
                    savedSummaries = savedSummaries,
                    shopProfile = _shopProfile.value,
                    pullResult = null,
                    context = appContext
                )
                repository.saveJsonToLocalStorage(fullJsonStr)
                _cachedSyncJson.value = fullJsonStr
                _lastJsonSaveTime.value = System.currentTimeMillis()
            } else {
                val payload = JSONObject().apply {
                    put("action", "delete_product")
                    put("product", JSONObject().apply {
                        put("id", product.id)
                        put("barcode", product.barcode)
                        put("name", product.name)
                    })
                }
                googleSheetsSyncManager.enqueueOfflineAction(
                    appContext,
                    "delete_product",
                    "ပစ္စည်း ဖျက်ခြင်း (${product.name})",
                    payload
                )
                refreshOfflineQueueState()
                triggerBackgroundSheetsAndLocalCacheUpdate()
            }
            Toast.makeText(getApplication(), "'${product.name}' အား ဖျက်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
        }
    }

    fun adjustStock(productId: Long, delta: Int) {
        viewModelScope.launch {
            val prodBefore = repository.getProductById(productId)
            repository.adjustStock(productId, delta)
            val cfg = _googleSheetsConfig.value
            val appContext = getApplication<Application>()
            if ((cfg.autoSyncSales || cfg.periodicAutoSync) && cfg.scriptUrl.isNotBlank()) {
                val latestProducts = repository.allProducts.first()
                googleSheetsSyncManager.syncProducts(cfg, latestProducts, appContext)
                refreshOfflineQueueState()
            } else if (prodBefore != null) {
                val payload = JSONObject().apply {
                    put("action", "adjust_stock")
                    put("productId", prodBefore.id)
                    put("barcode", prodBefore.barcode)
                    put("productName", prodBefore.name)
                    put("delta", delta)
                }
                val signStr = if (delta >= 0) "+$delta" else "$delta"
                googleSheetsSyncManager.enqueueOfflineAction(
                    appContext,
                    "adjust_stock",
                    "လက်ကျန်ပစ္စည်း ပြင်ဆင်ခြင်း (${prodBefore.name}: $signStr)",
                    payload
                )
                refreshOfflineQueueState()
                triggerBackgroundSheetsAndLocalCacheUpdate()
            }
        }
    }

    private fun loadInitialDarkModePreference(): String {
        val fromPrefs = prefs.getString("dark_mode_preference", null)?.trim()
        if (!fromPrefs.isNullOrEmpty() && (fromPrefs == "light" || fromPrefs == "dark" || fromPrefs == "system")) {
            return fromPrefs
        }
        val fromFile = try {
            val file = java.io.File(getApplication<Application>().filesDir, "dark_mode_preference.txt")
            if (file.exists()) file.readText().trim() else null
        } catch (_: Exception) {
            null
        }
        if (!fromFile.isNullOrEmpty() && (fromFile == "light" || fromFile == "dark" || fromFile == "system")) {
            prefs.edit().putString("dark_mode_preference", fromFile).commit()
            return fromFile
        }
        return if (prefs.contains("is_dark_mode")) {
            if (prefs.getBoolean("is_dark_mode", false)) "dark" else "light"
        } else {
            "system"
        }
    }

    private val _darkModePreference = MutableStateFlow(loadInitialDarkModePreference())
    val darkModePreference: StateFlow<String> = _darkModePreference.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean?>(
        when (_darkModePreference.value) {
            "dark" -> true
            "light" -> false
            else -> null
        }
    )
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    fun setDarkModePreference(mode: String) {
        val normalizedMode = when (mode.trim().lowercase(Locale.US)) {
            "dark" -> "dark"
            "light" -> "light"
            else -> "system"
        }
        _darkModePreference.value = normalizedMode
        val boolVal = when (normalizedMode) {
            "dark" -> true
            "light" -> false
            else -> null
        }
        _isDarkMode.value = boolVal
        val editor = prefs.edit().putString("dark_mode_preference", normalizedMode)
        if (boolVal != null) {
            editor.putBoolean("is_dark_mode", boolVal)
        } else {
            editor.remove("is_dark_mode")
        }
        editor.commit()
        try {
            java.io.File(getApplication<Application>().filesDir, "dark_mode_preference.txt").writeText(normalizedMode)
        } catch (_: Exception) {}
    }

    fun toggleDarkMode(currentEffective: Boolean? = null) {
        val current = when (_darkModePreference.value) {
            "dark" -> true
            "light" -> false
            else -> currentEffective ?: false
        }
        val newMode = if (!current) "dark" else "light"
        setDarkModePreference(newMode)
    }

    fun setDarkMode(enabled: Boolean) {
        setDarkModePreference(if (enabled) "dark" else "light")
    }

    fun clearAllCompletedSales() {
        viewModelScope.launch {
            repository.clearAllCompletedOrdersAndSaveToSavedReceipts()
            _activeReceipt.value = null
            triggerBackgroundSheetsAndLocalCacheUpdate(syncOrdersToo = true, syncSavedToo = true)
        }
    }

    fun resetToNewInitialData() {
        viewModelScope.launch {
            repository.forceResetDatabase()
            _cart.value = emptyList()
            _activeReceipt.value = null
        }
    }

    fun loadSampleProducts() {
        viewModelScope.launch {
            val addedCount = repository.seedSampleProductsIfNeeded()
            if (addedCount > 0) {
                Toast.makeText(
                    getApplication(),
                    "ပစ္စည်းစာရင်း ($addedCount) မျိုး အောင်မြင်စွာ ထည့်သွင်းပြီးပါပြီ",
                    Toast.LENGTH_SHORT
                ).show()
                triggerBackgroundSheetsAndLocalCacheUpdate(syncProductsToo = true)
            } else {
                Toast.makeText(
                    getApplication(),
                    "ပစ္စည်းစာရင်းများ ရှိပြီးဖြစ်ပါသည်",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun updateGoogleSheetsConfig(config: GoogleSheetsConfig, triggerImmediateSync: Boolean = false) {
        val oldUrl = _googleSheetsConfig.value.scriptUrl
        val oldSheetUrl = _googleSheetsConfig.value.sheetUrl
        _googleSheetsConfig.value = config
        prefs.edit()
            .putString("gs_script_url", config.scriptUrl)
            .putString("gs_sheet_name", config.sheetName)
            .putString("gs_folder_name", config.folderName)
            .putString("gs_folder_id", config.folderId)
            .putString("gs_shop_name", config.shopName)
            .putBoolean("gs_auto_sync_sales", config.autoSyncSales)
            .putBoolean("gs_periodic_auto_sync", config.periodicAutoSync)
            .putInt("gs_sync_interval_seconds", config.syncIntervalSeconds)
            .putString("gs_sheet_url", config.sheetUrl)
            .putLong("gs_last_sync_time", config.lastSyncTime)
            .putString("gs_last_sync_status", config.lastSyncStatus)
            .apply()

        val isAutoEnabled = config.periodicAutoSync || config.autoSyncSales
        if (isAutoEnabled) {
            startPeriodicAutoSync()
        } else {
            stopPeriodicAutoSync()
        }

        if (triggerImmediateSync && isAutoEnabled) {
            syncAll(isPeriodic = false)
        }
    }

    fun testGoogleSheetsConnection(onResult: (Boolean, String) -> Unit) {
        val cfg = _googleSheetsConfig.value
        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            val (success, msg) = googleSheetsSyncManager.testConnection(cfg)
            _isGoogleSheetsSyncing.value = false
            if (success) {
                updateGoogleSheetsConfig(cfg.copy(lastSyncTime = System.currentTimeMillis(), lastSyncStatus = "ချိတ်ဆက်မှု အောင်မြင်ပါသည်"), triggerImmediateSync = false)
            } else {
                updateGoogleSheetsConfig(cfg.copy(lastSyncStatus = "ချိတ်ဆက်မှု မအောင်မြင်ပါ"), triggerImmediateSync = false)
            }
            onResult(success, msg)
        }
    }

    fun initGoogleSheets(onResult: (Boolean, String) -> Unit) {
        val cfg = _googleSheetsConfig.value
        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            val (success, msg) = googleSheetsSyncManager.initSheets(cfg)
            _isGoogleSheetsSyncing.value = false
            if (success) {
                updateGoogleSheetsConfig(cfg.copy(lastSyncTime = System.currentTimeMillis(), lastSyncStatus = "ဇယားများ တည်ဆောက်ပြီးပါပြီ"), triggerImmediateSync = false)
            }
            onResult(success, msg)
        }
    }

    fun syncAll(isPeriodic: Boolean = false, onResult: ((Boolean, String) -> Unit)? = null) {
        val cfg = _googleSheetsConfig.value
        val isAutoEnabled = cfg.periodicAutoSync || cfg.autoSyncSales
        if (isPeriodic && !isAutoEnabled) {
            // Auto sync is disabled by user, strictly abort
            return
        }
        if (cfg.scriptUrl.isBlank()) {
            val msg = "Google Apps Script Web App URL မထည့်ရသေးပါ"
            updateGoogleSheetsConfig(cfg.copy(syncStatus = SyncStatus.FAILED, lastSyncStatus = "Sync Failed"), triggerImmediateSync = false)
            onResult?.invoke(false, msg)
            return
        }

        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            _googleSheetsConfig.value = _googleSheetsConfig.value.copy(
                syncStatus = SyncStatus.SYNCING,
                lastSyncStatus = "Syncing..."
            )

            try {
                val appContext = getApplication<Application>()

                // Step 0: If there are pending offline actions recorded while internet was unavailable,
                // replay them in strict chronological order to Google Sheets BEFORE pulling!
                val pendingActions = googleSheetsSyncManager.loadOfflineQueue(appContext)
                if (pendingActions.isNotEmpty() && cfg.scriptUrl.isNotBlank()) {
                    googleSheetsSyncManager.replayOfflineQueue(cfg, appContext)
                    refreshOfflineQueueState()
                }

                // Step 1: Fetch (pull) all data in JSON format from Google Sheet URL
                val pullResult = googleSheetsSyncManager.pullAllData(cfg)
                var importedProdsCount = 0
                var importedOrdersCount = 0
                var importedSavedCount = 0

                val localProdsBeforePull = repository.allProducts.first()
                val localOrdersBeforePull = repository.allOrders.first()
                val localSavedBeforePull = repository.savedReceipts.first()

                if (pullResult.success) {
                    // Only import products from Sheet if local inventory is empty (otherwise local screen state is authoritative)
                    if (localProdsBeforePull.isEmpty() && pullResult.products.isNotEmpty()) {
                        importedProdsCount = repository.saveImportedProductsFromSheet(pullResult.products)
                    }
                    if (localOrdersBeforePull.isEmpty() && pullResult.ordersJsonArray != null && pullResult.ordersJsonArray.length() > 0) {
                        importedOrdersCount = repository.saveImportedOrdersFromJson(pullResult.ordersJsonArray)
                    }
                    if (localSavedBeforePull.isEmpty() && pullResult.savedReceiptsJsonArray != null && pullResult.savedReceiptsJsonArray.length() > 0) {
                        importedSavedCount = repository.saveImportedSavedReceiptsFromJson(pullResult.savedReceiptsJsonArray)
                    }
                    if (pullResult.rates.isNotEmpty() || pullResult.shopProfileMap.isNotEmpty()) {
                        val cur = _shopProfile.value
                        val thbS = cur.thbSellingRate.ifBlank { pullResult.rates["thbSellingRate"] ?: pullResult.shopProfileMap["thbSellingRate"] ?: "" }
                        val thbB = cur.thbBuyingRate.ifBlank { pullResult.rates["thbBuyingRate"] ?: pullResult.shopProfileMap["thbBuyingRate"] ?: "" }
                        val mmkS = cur.mmkSellingRate.ifBlank { pullResult.rates["mmkSellingRate"] ?: pullResult.shopProfileMap["mmkSellingRate"] ?: "" }
                        val mmkB = cur.mmkBuyingRate.ifBlank { pullResult.rates["mmkBuyingRate"] ?: pullResult.shopProfileMap["mmkBuyingRate"] ?: "" }
                        val sName = pullResult.shopProfileMap["shopName"]?.takeIf { it.isNotBlank() && cur.shopName == "POS System Software" } ?: cur.shopName
                        val eName = pullResult.shopProfileMap["englishName"]?.takeIf { it.isNotBlank() && cur.englishName == "POS System Software" } ?: cur.englishName
                        val sPhone = cur.phone.ifBlank { pullResult.shopProfileMap["phone"] ?: "" }
                        val sAddr = cur.address.ifBlank { pullResult.shopProfileMap["address"] ?: "" }
                        val sFooter = cur.receiptFooter.ifBlank { pullResult.shopProfileMap["receiptFooter"] ?: "" }
                        val sCashier = cur.cashierName.ifBlank { pullResult.shopProfileMap["cashierName"] ?: "" }
                        updateShopProfile(cur.copy(
                            shopName = sName,
                            englishName = eName,
                            phone = sPhone,
                            address = sAddr,
                            receiptFooter = sFooter,
                            cashierName = sCashier,
                            thbSellingRate = thbS,
                            thbBuyingRate = thbB,
                            mmkSellingRate = mmkS,
                            mmkBuyingRate = mmkB
                        ))
                    }
                }

                // Read latest merged state from DB
                initializeRefundedHeldOrdersIfNeeded()
                val latestProducts = repository.allProducts.first()
                val latestOrders = repository.allOrders.first()
                val latestOrderItems = repository.allOrderItems.first()
                val latestSavedReceipts = repository.savedReceipts.first()

                // Step 2: Build complete JSON format and save into localStorage
                val salesSummaries = buildSalesSummaries()
                val savedSummaries = buildSavedReceiptsSummaries()
                val fullJsonStr = googleSheetsSyncManager.buildLocalStorageJson(
                    config = cfg,
                    products = latestProducts,
                    orders = latestOrders,
                    orderItems = latestOrderItems,
                    savedReceipts = latestSavedReceipts,
                    salesSummaries = salesSummaries,
                    savedSummaries = savedSummaries,
                    shopProfile = _shopProfile.value,
                    pullResult = pullResult,
                    context = appContext
                )
                repository.saveJsonToLocalStorage(fullJsonStr)
                _cachedSyncJson.value = fullJsonStr
                _lastJsonSaveTime.value = System.currentTimeMillis()

                // Step 3: Push local screen state (products, orders, savedReceipts, summaries) to Google Sheet so Sheet mirrors the app screen
                val (pushSuccess, pushMsg) = if (cfg.scriptUrl.isNotBlank()) {
                    googleSheetsSyncManager.syncAll(
                        config = cfg,
                        products = latestProducts,
                        orders = latestOrders,
                        orderItems = latestOrderItems,
                        savedReceipts = latestSavedReceipts,
                        salesSummaries = salesSummaries,
                        savedSummaries = savedSummaries,
                        shopProfile = _shopProfile.value,
                        context = appContext
                    )
                } else {
                    Pair(pullResult.success, pullResult.message)
                }
                refreshOfflineQueueState()

                _isGoogleSheetsSyncing.value = false

                if (pushSuccess || pullResult.success) {
                    val resolvedUrl = if (pullResult.sheetUrl.isNotBlank()) pullResult.sheetUrl else cfg.sheetUrl
                    val now = System.currentTimeMillis()
                    val updated = cfg.copy(
                        syncStatus = SyncStatus.SUCCESS,
                        lastSyncTime = now,
                        lastSyncStatus = "Sync Success (မျက်နှာပြင်အတိုင်း ချိတ်ဆက်ပြီး)",
                        sheetUrl = resolvedUrl,
                        pendingOfflineActionsCount = _offlineQueue.value.size
                    )
                    updateGoogleSheetsConfig(updated, triggerImmediateSync = false)
                    val detailMsg = buildString {
                        append("လက်ရှိ မျက်နှာပြင်အတိုင်း Google Sheets နှင့် LocalStorage သို့ အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ။\n")
                        if (pendingActions.isNotEmpty()) {
                            append("• အင်တာနက်မရှိချိန် မှတ်သားထားသော လုပ်ဆောင်ချက် (${pendingActions.size}) ခုအား အစဉ်လိုက် ပြန်လည်ဆောင်ရွက်ပြီးပါပြီ။\n")
                        }
                        append("• ကုန်ပစ္စည်းစုစုပေါင်း: ${latestProducts.size} မျိုး")
                        if (importedProdsCount > 0) append(" (ဆွဲယူရရှိ: $importedProdsCount မျိုး)")
                        append("\n• အရောင်းဘောက်ချာ: ${latestOrders.size} စောင်")
                        if (importedOrdersCount > 0) append(" (အသစ်ရရှိ: $importedOrdersCount စောင်)")
                        append("\n• ရှင်းပြီးပြေစာ: ${latestSavedReceipts.size} စောင်")
                        if (importedSavedCount > 0) append(" (အသစ်ရရှိ: $importedSavedCount စောင်)")
                    }
                    onResult?.invoke(true, detailMsg)
                } else {
                    val errorMsg = pushMsg.ifBlank { pullResult.message }
                    val updated = cfg.copy(
                        syncStatus = SyncStatus.FAILED,
                        lastSyncStatus = "Sync Failed"
                    )
                    updateGoogleSheetsConfig(updated, triggerImmediateSync = false)
                    onResult?.invoke(false, "Sync Failed: $errorMsg")
                }
            } catch (e: Exception) {
                _isGoogleSheetsSyncing.value = false
                val updated = cfg.copy(
                    syncStatus = SyncStatus.FAILED,
                    lastSyncStatus = "Sync Failed"
                )
                updateGoogleSheetsConfig(updated, triggerImmediateSync = false)
                onResult?.invoke(false, "Sync Failed: ${e.message}")
            }
        }
    }

    fun pullFromGoogleSheets(onResult: ((Boolean, String) -> Unit)? = null) {
        val cfg = _googleSheetsConfig.value
        if (cfg.scriptUrl.isBlank() && cfg.sheetUrl.isBlank()) {
            onResult?.invoke(false, "Google Apps Script URL သို့မဟုတ် Google Sheet လင့် မထည့်ရသေးပါ")
            return
        }

        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            _googleSheetsConfig.value = _googleSheetsConfig.value.copy(
                syncStatus = SyncStatus.SYNCING,
                lastSyncStatus = "Syncing..."
            )
            try {
                val pullResult = googleSheetsSyncManager.pullAllData(cfg)
                var importedProdsCount = 0
                var importedOrdersCount = 0
                var importedSavedCount = 0

                if (pullResult.success) {
                    if (pullResult.products.isNotEmpty()) {
                        importedProdsCount = repository.saveImportedProductsFromSheet(pullResult.products)
                    }
                    if (pullResult.ordersJsonArray != null && pullResult.ordersJsonArray.length() > 0) {
                        importedOrdersCount = repository.saveImportedOrdersFromJson(pullResult.ordersJsonArray)
                    }
                    if (pullResult.savedReceiptsJsonArray != null && pullResult.savedReceiptsJsonArray.length() > 0) {
                        importedSavedCount = repository.saveImportedSavedReceiptsFromJson(pullResult.savedReceiptsJsonArray)
                    }
                    if (pullResult.rates.isNotEmpty() || pullResult.shopProfileMap.isNotEmpty()) {
                        val cur = _shopProfile.value
                        val thbS = pullResult.rates["thbSellingRate"] ?: pullResult.shopProfileMap["thbSellingRate"] ?: cur.thbSellingRate
                        val thbB = pullResult.rates["thbBuyingRate"] ?: pullResult.shopProfileMap["thbBuyingRate"] ?: cur.thbBuyingRate
                        val mmkS = pullResult.rates["mmkSellingRate"] ?: pullResult.shopProfileMap["mmkSellingRate"] ?: cur.mmkSellingRate
                        val mmkB = pullResult.rates["mmkBuyingRate"] ?: pullResult.shopProfileMap["mmkBuyingRate"] ?: cur.mmkBuyingRate
                        val sName = pullResult.shopProfileMap["shopName"]?.takeIf { it.isNotBlank() } ?: cur.shopName
                        val eName = pullResult.shopProfileMap["englishName"]?.takeIf { it.isNotBlank() } ?: cur.englishName
                        val sPhone = pullResult.shopProfileMap["phone"]?.takeIf { it.isNotBlank() } ?: cur.phone
                        val sAddr = pullResult.shopProfileMap["address"]?.takeIf { it.isNotBlank() } ?: cur.address
                        val sFooter = pullResult.shopProfileMap["receiptFooter"]?.takeIf { it.isNotBlank() } ?: cur.receiptFooter
                        val sCashier = pullResult.shopProfileMap["cashierName"]?.takeIf { it.isNotBlank() } ?: cur.cashierName
                        updateShopProfile(cur.copy(
                            shopName = sName,
                            englishName = eName,
                            phone = sPhone,
                            address = sAddr,
                            receiptFooter = sFooter,
                            cashierName = sCashier,
                            thbSellingRate = thbS,
                            thbBuyingRate = thbB,
                            mmkSellingRate = mmkS,
                            mmkBuyingRate = mmkB
                        ))
                    }

                    initializeRefundedHeldOrdersIfNeeded()
                    val latestProducts = repository.allProducts.first()
                    val latestOrders = repository.allOrders.first()
                    val latestOrderItems = repository.allOrderItems.first()
                    val latestSavedReceipts = repository.savedReceipts.first()

                    val salesSummaries = buildSalesSummaries()
                    val savedSummaries = buildSavedReceiptsSummaries()
                    val appContext = getApplication<Application>()
                    val fullJsonStr = googleSheetsSyncManager.buildLocalStorageJson(
                        config = cfg,
                        products = latestProducts,
                        orders = latestOrders,
                        orderItems = latestOrderItems,
                        savedReceipts = latestSavedReceipts,
                        salesSummaries = salesSummaries,
                        savedSummaries = savedSummaries,
                        shopProfile = _shopProfile.value,
                        pullResult = pullResult,
                        context = appContext
                    )
                    repository.saveJsonToLocalStorage(fullJsonStr)
                    _cachedSyncJson.value = fullJsonStr
                    _lastJsonSaveTime.value = System.currentTimeMillis()

                    _isGoogleSheetsSyncing.value = false
                    val resolvedUrl = if (pullResult.sheetUrl.isNotBlank()) pullResult.sheetUrl else cfg.sheetUrl
                    val updated = cfg.copy(
                        syncStatus = SyncStatus.SUCCESS,
                        lastSyncTime = System.currentTimeMillis(),
                        lastSyncStatus = "Sync Success",
                        sheetUrl = resolvedUrl
                    )
                    updateGoogleSheetsConfig(updated, triggerImmediateSync = false)

                    val detailMsg = buildString {
                        append("သိမ်းဆည်းထားသော Google Sheets စာရင်းများကို အောင်မြင်စွာ ဆွဲယူပြီးပါပြီ။\n")
                        append("• ကုန်ပစ္စည်းစုစုပေါင်း: ${latestProducts.size} မျိုး")
                        if (importedProdsCount > 0) append(" (ဆွဲယူရရှိ: $importedProdsCount မျိုး)")
                        append("\n• အရောင်းဘောက်ချာ: ${latestOrders.size} စောင်")
                        if (importedOrdersCount > 0) append(" (အသစ်ရရှိ: $importedOrdersCount စောင်)")
                        append("\n• ရှင်းပြီးပြေစာ: ${latestSavedReceipts.size} စောင်")
                        if (importedSavedCount > 0) append(" (အသစ်ရရှိ: $importedSavedCount စောင်)")
                    }
                    onResult?.invoke(true, detailMsg)
                } else {
                    _isGoogleSheetsSyncing.value = false
                    val updated = cfg.copy(
                        syncStatus = SyncStatus.FAILED,
                        lastSyncStatus = "Sync Failed"
                    )
                    updateGoogleSheetsConfig(updated, triggerImmediateSync = false)
                    onResult?.invoke(false, pullResult.message)
                }
            } catch (e: Exception) {
                _isGoogleSheetsSyncing.value = false
                val updated = cfg.copy(
                    syncStatus = SyncStatus.FAILED,
                    lastSyncStatus = "Sync Failed"
                )
                updateGoogleSheetsConfig(updated, triggerImmediateSync = false)
                onResult?.invoke(false, "စာရင်းများ ဆွဲယူရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.message}")
            }
        }
    }

    fun restoreFromLocalStorage(customJson: String? = null, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            try {
                val targetJson = if (!customJson.isNullOrBlank()) {
                    repository.saveJsonToLocalStorage(customJson)
                    _cachedSyncJson.value = customJson
                    _lastJsonSaveTime.value = System.currentTimeMillis()
                    customJson
                } else {
                    repository.getJsonFromLocalStorage()
                }

                val (success, msg, rates) = repository.restoreFromLocalStorageJson(targetJson)
                if (success) {
                    initializeRefundedHeldOrdersIfNeeded()
                }
                if (success && rates.isNotEmpty()) {
                    val thbS = rates["thbSellingRate"] ?: _shopProfile.value.thbSellingRate
                    val thbB = rates["thbBuyingRate"] ?: _shopProfile.value.thbBuyingRate
                    val mmkS = rates["mmkSellingRate"] ?: _shopProfile.value.mmkSellingRate
                    val mmkB = rates["mmkBuyingRate"] ?: _shopProfile.value.mmkBuyingRate
                    updateShopProfile(_shopProfile.value.copy(
                        thbSellingRate = thbS,
                        thbBuyingRate = thbB,
                        mmkSellingRate = mmkS,
                        mmkBuyingRate = mmkB
                    ))
                }
                _isGoogleSheetsSyncing.value = false
                onResult?.invoke(success, msg)
            } catch (e: Exception) {
                _isGoogleSheetsSyncing.value = false
                onResult?.invoke(false, "LocalStorage မှ ပြန်ယူရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.message}")
            }
        }
    }

    fun clearLocalStorageJson(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val cleared = repository.clearJsonLocalStorage()
            _cachedSyncJson.value = ""
            prefs.edit().putLong("gs_last_json_save_time", 0L).apply()
            _lastJsonSaveTime.value = 0L
            if (cleared) {
                Toast.makeText(getApplication(), "LocalStorage (JSON) အား အောင်မြင်စွာ ရှင်းထုတ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                onResult?.invoke(true, "LocalStorage (JSON) ဒေတာများကို အောင်မြင်စွာ ရှင်းထုတ်ပြီးပါပြီ")
            } else {
                Toast.makeText(getApplication(), "LocalStorage (JSON) ရှင်းထုတ်ရာတွင် မအောင်မြင်ပါ", Toast.LENGTH_SHORT).show()
                onResult?.invoke(false, "LocalStorage (JSON) ရှင်းထုတ်ရာတွင် မအောင်မြင်ပါ")
            }
        }
    }

    fun syncAllToGoogleSheets(onResult: (Boolean, String) -> Unit) {
        syncAll(isPeriodic = false) { s, m -> onResult(s, m) }
    }

    fun syncProductsToGoogleSheets(onResult: (Boolean, String) -> Unit) {
        val cfg = _googleSheetsConfig.value
        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            val appContext = getApplication<Application>()
            val currentList = repository.allProducts.first()
            for (prod in currentList) {
                if (prod.imageUri.isNotBlank() &&
                    (com.example.util.ProductImageHelper.isLocalOnlyImageUri(prod.imageUri) ||
                        prod.imageUri.startsWith("data:image", ignoreCase = true))
                ) {
                    val cloudUrl = googleSheetsSyncManager.uploadProductImageToDrive(cfg, appContext, prod)
                    if (!cloudUrl.isNullOrBlank() && cloudUrl != prod.imageUri) {
                        repository.updateProduct(prod.copy(imageUri = cloudUrl))
                    }
                }
            }
            val updatedList = repository.allProducts.first()
            val (success, msg) = googleSheetsSyncManager.syncProducts(cfg, updatedList, appContext)
            _isGoogleSheetsSyncing.value = false
            if (success) {
                updateGoogleSheetsConfig(cfg.copy(lastSyncTime = System.currentTimeMillis(), lastSyncStatus = "ကုန်ပစ္စည်းများ သိမ်းဆည်းပြီးပါပြီ"))
            }
            onResult(success, msg)
        }
    }

    fun syncOrdersToGoogleSheets(onResult: (Boolean, String) -> Unit) {
        val cfg = _googleSheetsConfig.value
        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            val (success, msg) = googleSheetsSyncManager.syncOrders(
                config = cfg,
                orders = orders.value,
                orderItems = allOrderItems.value,
                products = allProducts.value,
                shopProfile = _shopProfile.value
            )
            _isGoogleSheetsSyncing.value = false
            if (success) {
                updateGoogleSheetsConfig(cfg.copy(lastSyncTime = System.currentTimeMillis(), lastSyncStatus = "အရောင်းမှတ်တမ်းများ သိမ်းဆည်းပြီးပါပြီ"))
            }
            onResult(success, msg)
        }
    }

    fun syncSavedReceiptsToGoogleSheets(onResult: (Boolean, String) -> Unit) {
        val cfg = _googleSheetsConfig.value
        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            val (success, msg) = googleSheetsSyncManager.syncSavedReceipts(
                config = cfg,
                savedReceipts = savedReceipts.value,
                products = allProducts.value,
                shopProfile = _shopProfile.value
            )
            _isGoogleSheetsSyncing.value = false
            if (success) {
                updateGoogleSheetsConfig(cfg.copy(lastSyncTime = System.currentTimeMillis(), lastSyncStatus = "ရှင်းပြီးပြေစာများ သိမ်းဆည်းပြီးပါပြီ"))
            }
            onResult(success, msg)
        }
    }

    fun syncSummariesToGoogleSheets(onResult: (Boolean, String) -> Unit) {
        val cfg = _googleSheetsConfig.value
        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            val salesSummaries = buildSalesSummaries()
            val savedSummaries = buildSavedReceiptsSummaries()
            val (success, msg) = googleSheetsSyncManager.syncSummaries(cfg, salesSummaries, savedSummaries)
            _isGoogleSheetsSyncing.value = false
            if (success) {
                updateGoogleSheetsConfig(cfg.copy(lastSyncTime = System.currentTimeMillis(), lastSyncStatus = "စာရင်းချုပ်များ သိမ်းဆည်းပြီးပါပြီ"))
            }
            onResult(success, msg)
        }
    }

    fun syncRatesToGoogleSheets(onResult: (Boolean, String) -> Unit) {
        val cfg = _googleSheetsConfig.value
        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            val (success, msg) = googleSheetsSyncManager.syncRates(cfg, _shopProfile.value)
            _isGoogleSheetsSyncing.value = false
            if (success) {
                updateGoogleSheetsConfig(cfg.copy(lastSyncTime = System.currentTimeMillis(), lastSyncStatus = "မြန်မာကျပ် နှင့် ဘတ်ငွေ လဲလှယ်နှုန်းများ သိမ်းဆည်းပြီးပါပြီ"))
            }
            onResult(success, msg)
        }
    }

    fun syncShopProfileToGoogleSheets(onResult: (Boolean, String) -> Unit) {
        val cfg = _googleSheetsConfig.value
        viewModelScope.launch {
            _isGoogleSheetsSyncing.value = true
            val (success, msg) = googleSheetsSyncManager.syncShopProfile(cfg, _shopProfile.value)
            _isGoogleSheetsSyncing.value = false
            if (success) {
                updateGoogleSheetsConfig(cfg.copy(lastSyncTime = System.currentTimeMillis(), lastSyncStatus = "ဆိုင်အချက်အလက်နှင့် ငွေလဲနှုန်းများ သိမ်းဆည်းပြီးပါပြီ"))
            }
            onResult(success, msg)
        }
    }

    private fun buildSalesSummaries(): List<JSONObject> {
        val allOrders = orders.value
        val itemsByOrderId = allOrderItems.value.groupBy { it.orderId }
        val prods = allProducts.value
        val prof = _shopProfile.value
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val calculations = allOrders.map { order ->
            val items = itemsByOrderId[order.id] ?: emptyList()
            ReceiptCalculator.calculateFromOrder(order, items, prods, prof)
        }

        val completedCalcs = calculations.filter { it.status == OrderStatus.COMPLETED }
        val list = mutableListOf<JSONObject>()

        // 1. အတည်ပြုပြီး စာရင်းချုပ် (Confirmed Orders)
        val confUnits = completedCalcs.sumOf { it.totalUnitsSold }
        val confRevMmk = completedCalcs.sumOf { it.sellingPriceMmk }
        val confRevThb = completedCalcs.sumOf { it.sellingPriceThb }
        val confDiscMmk = completedCalcs.sumOf { it.discountMmk }
        val confDiscThb = completedCalcs.sumOf { it.discountThb }
        val confProfMmk = completedCalcs.sumOf { it.estimatedNetProfitMmk }
        val confProfThb = completedCalcs.sumOf { it.estimatedNetProfitThb }

        list.add(JSONObject().apply {
            put("type", "အတည်ပြုပြီး စာရင်းချုပ်")
            put("period", "လက်ရှိ အတည်ပြုပြီးအားလုံး")
            put("ordersCount", completedCalcs.size)
            put("unitsSold", confUnits)
            put("revenueMmk", confRevMmk)
            put("revenueThb", confRevThb)
            put("discountMmk", confDiscMmk)
            put("discountThb", confDiscThb)
            put("profitMmk", confProfMmk)
            put("profitThb", confProfThb)
            put("generatedDate", nowStr)
        })

        // 2. လအလိုက် စာရင်းချုပ် (Monthly)
        val byMonth = completedCalcs.groupBy { monthFormat.format(Date(it.timestamp)) }
        byMonth.keys.sortedDescending().forEach { month ->
            val monthCalcs = byMonth[month] ?: emptyList()
            list.add(JSONObject().apply {
                put("type", "လအလိုက် စာရင်းချုပ်")
                put("period", month)
                put("ordersCount", monthCalcs.size)
                put("unitsSold", monthCalcs.sumOf { it.totalUnitsSold })
                put("revenueMmk", monthCalcs.sumOf { it.sellingPriceMmk })
                put("revenueThb", monthCalcs.sumOf { it.sellingPriceThb })
                put("discountMmk", monthCalcs.sumOf { it.discountMmk })
                put("discountThb", monthCalcs.sumOf { it.discountThb })
                put("profitMmk", monthCalcs.sumOf { it.estimatedNetProfitMmk })
                put("profitThb", monthCalcs.sumOf { it.estimatedNetProfitThb })
                put("generatedDate", nowStr)
            })
        }

        // 3. စာရင်းချုပ် (အားလုံး) (All-time orders)
        list.add(JSONObject().apply {
            put("type", "စာရင်းချုပ် (အားလုံး)")
            put("period", "မှတ်တမ်းအားလုံး")
            put("ordersCount", calculations.size)
            put("unitsSold", calculations.sumOf { it.totalUnitsSold })
            put("revenueMmk", calculations.sumOf { it.sellingPriceMmk })
            put("revenueThb", calculations.sumOf { it.sellingPriceThb })
            put("discountMmk", calculations.sumOf { it.discountMmk })
            put("discountThb", calculations.sumOf { it.discountThb })
            put("profitMmk", calculations.sumOf { it.estimatedNetProfitMmk })
            put("profitThb", calculations.sumOf { it.estimatedNetProfitThb })
            put("generatedDate", nowStr)
        })

        return list
    }

    private fun buildSavedReceiptsSummaries(): List<JSONObject> {
        val receipts = savedReceipts.value
        val prods = allProducts.value
        val prof = _shopProfile.value
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val calculations = receipts.map { receipt ->
            ReceiptCalculator.calculateFromSavedReceipt(receipt, prods, prof)
        }

        val completedCalcs = calculations.filter { it.status == OrderStatus.COMPLETED }
        val list = mutableListOf<JSONObject>()

        // 1. အတည်ပြုပြီး ရှင်းပြီးပြေစာများ စာရင်းချုပ်
        list.add(JSONObject().apply {
            put("type", "အတည်ပြုပြီး စာရင်းချုပ် (ရှင်းပြီးပြေစာများ)")
            put("period", "ရှင်းပြီးပြေစာ အတည်ပြုပြီးအားလုံး")
            put("ordersCount", completedCalcs.size)
            put("unitsSold", completedCalcs.sumOf { it.totalUnitsSold })
            put("revenueMmk", completedCalcs.sumOf { it.sellingPriceMmk })
            put("revenueThb", completedCalcs.sumOf { it.sellingPriceThb })
            put("profitMmk", completedCalcs.sumOf { it.estimatedNetProfitMmk })
            put("profitThb", completedCalcs.sumOf { it.estimatedNetProfitThb })
            put("generatedDate", nowStr)
        })

        // 2. လအလိုက် ရှင်းပြီးပြေစာများ စာရင်းချုပ်
        val byMonth = completedCalcs.groupBy { monthFormat.format(Date(it.timestamp)) }
        byMonth.keys.sortedDescending().forEach { month ->
            val monthCalcs = byMonth[month] ?: emptyList()
            list.add(JSONObject().apply {
                put("type", "လအလိုက် စာရင်းချုပ် (ရှင်းပြီးပြေစာများ)")
                put("period", month)
                put("ordersCount", monthCalcs.size)
                put("unitsSold", monthCalcs.sumOf { it.totalUnitsSold })
                put("revenueMmk", monthCalcs.sumOf { it.sellingPriceMmk })
                put("revenueThb", monthCalcs.sumOf { it.sellingPriceThb })
                put("profitMmk", monthCalcs.sumOf { it.estimatedNetProfitMmk })
                put("profitThb", monthCalcs.sumOf { it.estimatedNetProfitThb })
                put("generatedDate", nowStr)
            })
        }

        // 3. ရှင်းပြီးပြေစာများ စာရင်းချုပ် (အားလုံး)
        list.add(JSONObject().apply {
            put("type", "စာရင်းချုပ် အားလုံး (ရှင်းပြီးပြေစာများ)")
            put("period", "ရှင်းပြီးပြေစာ မှတ်တမ်းအားလုံး")
            put("ordersCount", calculations.size)
            put("unitsSold", calculations.sumOf { it.totalUnitsSold })
            put("revenueMmk", calculations.sumOf { it.sellingPriceMmk })
            put("revenueThb", calculations.sumOf { it.sellingPriceThb })
            put("profitMmk", calculations.sumOf { it.estimatedNetProfitMmk })
            put("profitThb", calculations.sumOf { it.estimatedNetProfitThb })
            put("generatedDate", nowStr)
        })

        return list
    }

    fun updateShopProfile(profile: ShopProfile, syncToCloudIfEnabled: Boolean = false) {
        _shopProfile.value = profile
        prefs.edit()
            .putString("shop_name", profile.shopName)
            .putString("shop_eng_name", profile.englishName)
            .putString("shop_phone", profile.phone)
            .putString("shop_address", profile.address)
            .putString("shop_footer", profile.receiptFooter)
            .putString("shop_cashier", profile.cashierName)
            .putString("thb_selling_rate", profile.thbSellingRate)
            .putString("thb_buying_rate", profile.thbBuyingRate)
            .putString("mmk_selling_rate", profile.mmkSellingRate)
            .putString("mmk_buying_rate", profile.mmkBuyingRate)
            .apply()

        if (syncToCloudIfEnabled) {
            val cfg = _googleSheetsConfig.value
            if ((cfg.autoSyncSales || cfg.periodicAutoSync) && cfg.scriptUrl.isNotBlank()) {
                viewModelScope.launch {
                    googleSheetsSyncManager.syncShopProfile(cfg, profile)
                    triggerBackgroundSheetsAndLocalCacheUpdate()
                }
            } else {
                triggerBackgroundSheetsAndLocalCacheUpdate()
            }
        }
    }

    companion object {
        fun formatKyat(amount: Long): String {
            val formatted = NumberFormat.getNumberInstance(Locale.US).format(amount)
            return "$formatted Ks"
        }

        fun formatKyatMm(amount: Long): String {
            val formatted = NumberFormat.getNumberInstance(Locale.US).format(amount)
            return "$formatted ကျပ်"
        }

        fun formatCurrency(amount: Long, currency: String): String {
            val formatted = NumberFormat.getNumberInstance(Locale.US).format(amount)
            val suffix = if (currency == "THB") "ဘတ်" else "ကျပ်"
            return "$formatted $suffix"
        }

        fun formatCurrencyShort(amount: Long, currency: String): String {
            val formatted = NumberFormat.getNumberInstance(Locale.US).format(amount)
            val suffix = if (currency == "THB") "ဘတ်" else "ကျပ်"
            return "$formatted $suffix"
        }
    }
}
