package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.NaturalBorderLight
import com.example.ui.theme.NaturalOnSecondaryContainer
import com.example.ui.theme.NaturalPositiveGreen
import com.example.ui.theme.NaturalPrimary
import com.example.ui.theme.NaturalSecondaryContainer
import com.example.ui.theme.NaturalSurfaceVariantLight
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary

enum class PosTab(
    val titleMm: String,
    val titleEn: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    CASHIER("အရောင်း", "Sale", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale, "tab_cashier"),
    INVENTORY("ပစ္စည်းများ", "Items", Icons.Filled.Inventory, Icons.Outlined.Inventory, "tab_inventory"),
    HISTORY("မှတ်တမ်း", "History", Icons.Filled.History, Icons.Outlined.History, "tab_history"),
    ANALYTICS("စာရင်းချုပ်", "Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment, "tab_analytics"),
    SETTINGS("ဆက်တင်", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosApp(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var historyTabMode by rememberSaveable { mutableStateOf(HistoryTabMode.CONFIRMED) }
    var analyticsTabMode by rememberSaveable { mutableStateOf(AnalyticsTabMode.CONFIRMED) }
    var currentSubScreen by remember { mutableStateOf<String?>(null) }
    val activeReceipt by viewModel.activeReceipt.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val cartItemCount by viewModel.cartItemCount.collectAsStateWithLifecycle()
    val showClearIconButton by viewModel.showClearIconButton.collectAsStateWithLifecycle()
    val showTopAppBar by viewModel.showTopAppBar.collectAsStateWithLifecycle()
    val showAddProductButton by viewModel.showAddProductButton.collectAsStateWithLifecycle()
    val darkModePref by viewModel.darkModePreference.collectAsStateWithLifecycle()

    val visibleTabs = remember(showAddProductButton) {
        PosTab.values().filter { tab ->
            if (tab == PosTab.INVENTORY) showAddProductButton else true
        }
    }

    if (currentSubScreen != null) {
        BackHandler { currentSubScreen = null }
    } else {
        Scaffold(
            topBar = {
                if (showTopAppBar) {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (shopProfile.shopName.isNotBlank()) {
                                        Text(
                                            text = shopProfile.shopName.first().toString(),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Store,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (shopProfile.shopName.isNotBlank()) shopProfile.shopName else "POS System Software",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    Text(
                                        text = "အရောင်းနှင့် စာရင်းကိုင်စနစ်",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                            }
                        },
                        actions = {
                            val (themeIcon, themeLabel) = when (darkModePref) {
                                "light" -> Icons.Default.LightMode to "အလင်း"
                                "dark" -> Icons.Default.DarkMode to "အမှောင်"
                                else -> Icons.Default.SettingsBrightness to "စနစ်အတိုင်း"
                            }
                            Box(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                                    .clickable {
                                        val nextPref = when (darkModePref) {
                                            "light" -> "dark"
                                            "dark" -> "system"
                                            else -> "light"
                                        }
                                        viewModel.setDarkModePreference(nextPref)
                                    }
                                    .testTag("top_bar_dark_mode_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = themeIcon,
                                    contentDescription = "ညအမှောင်စနစ် ($themeLabel)",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp,
                    modifier = Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                ) {
                    visibleTabs.forEach { tab ->
                        val isSelected = (PosTab.values().getOrNull(selectedTabIndex) ?: PosTab.CASHIER) == tab
                        val isSavedReceiptsMode = tab == PosTab.HISTORY && isSelected && historyTabMode == HistoryTabMode.SAVED_RECEIPTS
                        val isAnalyticsSavedMode = tab == PosTab.ANALYTICS && isSelected && analyticsTabMode == AnalyticsTabMode.SAVED_SUMMARY
                        val isAnalyticsCombinedMode = tab == PosTab.ANALYTICS && isSelected && analyticsTabMode == AnalyticsTabMode.COMBINED_SUMMARY
                        val isAnalyticsMainMode = tab == PosTab.ANALYTICS && isSelected && !isAnalyticsSavedMode && !isAnalyticsCombinedMode

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (tab == PosTab.HISTORY) {
                                    if (!isSelected) {
                                        // First click when coming from another tab -> Confirmed History (အတည်ပြုပြီး)
                                        selectedTabIndex = tab.ordinal
                                        historyTabMode = HistoryTabMode.CONFIRMED
                                    } else {
                                        // Second click while already on History tab -> toggle between အတည်ပြုပြီး and ရှင်းပြီးမှတ်တမ်း
                                        historyTabMode = if (historyTabMode == HistoryTabMode.SAVED_RECEIPTS) {
                                            HistoryTabMode.CONFIRMED
                                        } else {
                                            HistoryTabMode.SAVED_RECEIPTS
                                        }
                                    }
                                } else if (tab == PosTab.ANALYTICS) {
                                    if (!isSelected) {
                                        // First click when coming from another tab -> Confirmed Analytics (အတည်ပြုပြီး)
                                        selectedTabIndex = tab.ordinal
                                        analyticsTabMode = AnalyticsTabMode.CONFIRMED
                                    } else {
                                        // Subsequent clicks while on Analytics tab -> cycle:
                                        // 1. အတည်ပြုပြီး (CONFIRMED / MONTHLY / ALL) -> 2. ရှင်းပြီးစာရင်းချုပ် (SAVED_SUMMARY) -> 3. မြန်မာငွေ နှင့် ထိုင်းငွေ စာရင်းချုပ် (COMBINED_SUMMARY) -> back to 1
                                        analyticsTabMode = when (analyticsTabMode) {
                                            AnalyticsTabMode.SAVED_SUMMARY -> AnalyticsTabMode.COMBINED_SUMMARY
                                            AnalyticsTabMode.COMBINED_SUMMARY -> AnalyticsTabMode.CONFIRMED
                                            else -> AnalyticsTabMode.SAVED_SUMMARY
                                        }
                                    }
                                } else {
                                    selectedTabIndex = tab.ordinal
                                }
                            },
                            icon = {
                                if (tab == PosTab.CASHIER && cartItemCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                Text("$cartItemCount")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.titleMm
                                        )
                                    }
                                } else if (tab == PosTab.HISTORY) {
                                    // Single icon for History that gets replaced depending on active mode
                                    val historyIcon = when {
                                        isSavedReceiptsMode -> Icons.Filled.Layers
                                        isSelected -> Icons.Filled.History
                                        else -> Icons.Outlined.History
                                    }
                                    Icon(
                                        imageVector = historyIcon,
                                        contentDescription = tab.titleMm
                                    )
                                } else if (tab == PosTab.ANALYTICS) {
                                    // Single icon for Analytics that gets replaced depending on active mode
                                    val analyticsIcon = when {
                                        isAnalyticsSavedMode -> Icons.Filled.Layers
                                        isAnalyticsCombinedMode -> Icons.Filled.CurrencyExchange
                                        isSelected -> Icons.Filled.Assessment
                                        else -> Icons.Outlined.Assessment
                                    }
                                    Icon(
                                        imageVector = analyticsIcon,
                                        contentDescription = tab.titleMm
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.titleMm
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tab.titleMm,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            val activeTab = PosTab.values().getOrNull(selectedTabIndex) ?: PosTab.CASHIER
            val currentTab = if (activeTab == PosTab.INVENTORY && !showAddProductButton) PosTab.CASHIER else activeTab

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    PosTab.CASHIER -> CashierScreen(viewModel = viewModel)
                    PosTab.INVENTORY -> InventoryScreen(viewModel = viewModel)
                    PosTab.HISTORY -> HistoryScreen(
                        viewModel = viewModel,
                        externalTabMode = historyTabMode,
                        onTabModeChange = { historyTabMode = it }
                    )
                    PosTab.ANALYTICS -> AnalyticsScreen(
                        viewModel = viewModel,
                        externalTabMode = analyticsTabMode,
                        onTabModeChange = { analyticsTabMode = it }
                    )
                    PosTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Receipt Dialog Overlay
    activeReceipt?.let { receiptDetails ->
        val activeTab = PosTab.values().getOrNull(selectedTabIndex) ?: PosTab.CASHIER
        val currentTab = if (activeTab == PosTab.INVENTORY && !showAddProductButton) PosTab.CASHIER else activeTab
        val isSavedReceiptOrder = receiptDetails.order.id < 0L
        val isInsideSavedReceipts = isSavedReceiptOrder || currentSubScreen == "Blank1" || currentSubScreen == "SavedReceiptsHistory" || currentSubScreen == "SavedReceiptsAnalytics" || (currentTab == PosTab.HISTORY && historyTabMode == HistoryTabMode.SAVED_RECEIPTS)
        // Only allow clearing/saving, confirming, refunding, undo-refunding, or deleting from the active Sales History screen
        val isAllowedInActiveSalesHistory = currentSubScreen == null && currentTab == PosTab.HISTORY && historyTabMode != HistoryTabMode.SAVED_RECEIPTS && !isInsideSavedReceipts
        ReceiptDialog(
            orderDetails = receiptDetails,
            shopProfile = shopProfile,
            products = products,
            showCheckButton = isAllowedInActiveSalesHistory && showClearIconButton,
            onDismiss = { viewModel.closeReceipt() },
            onRefund = if (isAllowedInActiveSalesHistory) { orderId -> viewModel.refundOrder(orderId) } else null,
            onUndoRefund = if (isAllowedInActiveSalesHistory) { orderId -> viewModel.undoRefundOrder(orderId) } else null,
            onDelete = if (isAllowedInActiveSalesHistory) { orderId -> viewModel.deleteOrder(orderId) } else null,
            onClearAndSave = if (isAllowedInActiveSalesHistory) { details -> viewModel.clearAndSaveReceiptToScreen1(details) } else null
        )
    }
}
