package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.Order
import com.example.data.entity.OrderStatus
import com.example.data.entity.PaymentMethod
import com.example.ui.components.ReceiptCalculationCard
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenDark
import com.example.ui.theme.KPayBlue
import com.example.ui.theme.KPayBlueDark
import com.example.ui.theme.PaymentColors
import com.example.util.ReceiptCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class HistoryTabMode {
    CONFIRMED,      // အတည်ပြုပြီးသော အရောင်းများ
    ALL,            // အရောင်းမှတ်တမ်း အားလုံး
    REFUNDED,       // ငွေပြန်အမ်းပြီး စာရင်းများ
    SAVED_RECEIPTS  // ရှင်းပြီးမှတ်တမ်း
}

@Composable
fun HistoryScreen(
    viewModel: PosViewModel,
    initialTabMode: HistoryTabMode = HistoryTabMode.CONFIRMED,
    externalTabMode: HistoryTabMode? = null,
    onTabModeChange: ((HistoryTabMode) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val allOrderItems by viewModel.allOrderItems.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val showClearIconButton by viewModel.showClearIconButton.collectAsStateWithLifecycle()
    val savedReceiptsCount by viewModel.savedReceiptsCount.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    var internalTabMode by rememberSaveable { mutableStateOf(initialTabMode) }
    val activeTabMode = externalTabMode ?: internalTabMode
    val setActiveTabMode: (HistoryTabMode) -> Unit = { newMode ->
        internalTabMode = newMode
        onTabModeChange?.invoke(newMode)
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTimeFilter by remember { mutableStateOf("အားလုံး") }

    val timeFilterOptions = listOf("အားလုံး", "ယနေ့", "မနေ့က", "ယခုအပတ်", "ယခုလ")

    // Filter logic
    val filteredOrders = remember(orders, searchQuery, selectedTimeFilter, activeTabMode) {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfYesterday = startOfToday - 24 * 3600 * 1000
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        orders.filter { order ->
            // Mode filter
            val matchMode = when (activeTabMode) {
                HistoryTabMode.CONFIRMED -> order.status == OrderStatus.COMPLETED
                HistoryTabMode.REFUNDED -> order.status == OrderStatus.REFUNDED
                HistoryTabMode.ALL -> true
                HistoryTabMode.SAVED_RECEIPTS -> false
            }

            // Time filter
            val matchDate = when (selectedTimeFilter) {
                "ယနေ့" -> order.timestamp >= startOfToday
                "မနေ့က" -> order.timestamp in startOfYesterday until startOfToday
                "ယခုအပတ်" -> order.timestamp >= startOfWeek
                "ယခုလ" -> order.timestamp >= startOfMonth
                else -> true
            }

            // Search filter
            val matchSearch = searchQuery.isBlank() ||
                    order.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    order.customerName.contains(searchQuery, ignoreCase = true) ||
                    order.customerPhone.contains(searchQuery, ignoreCase = true)

            matchMode && matchDate && matchSearch
        }
    }

    val orderCalculations = remember(filteredOrders, allOrderItems, products, shopProfile) {
        val orderItemsMap = allOrderItems.groupBy { it.orderId }
        filteredOrders.map { order ->
            val items = orderItemsMap[order.id] ?: emptyList()
            ReceiptCalculator.calculateFromOrder(order, items, products, shopProfile)
        }
    }

    val confirmedOrdersCount = remember(orders) {
        orders.count { it.status == OrderStatus.COMPLETED }
    }
    val refundedOrdersCount = remember(orders) {
        orders.count { it.status == OrderStatus.REFUNDED }
    }
    val totalConfirmedRevenue = remember(orderCalculations) {
        orderCalculations
            .filter { it.status == OrderStatus.COMPLETED }
            .sumOf { it.sellingPriceMmk }
    }
    val totalConfirmedRevenueThb = remember(orderCalculations) {
        orderCalculations
            .filter { it.status == OrderStatus.COMPLETED }
            .sumOf { it.sellingPriceThb }
    }
    val mmkConfirmedCalcs = remember(orderCalculations) {
        orderCalculations.filter { it.status == OrderStatus.COMPLETED && it.currency != "THB" }
    }
    val mmkConfirmedOrders = mmkConfirmedCalcs
    val mmkConfirmedRevenue = remember(mmkConfirmedCalcs) {
        mmkConfirmedCalcs.sumOf { it.sellingPriceMmk }
    }
    val mmkConfirmedRevenueThb = remember(mmkConfirmedCalcs) {
        mmkConfirmedCalcs.sumOf { it.sellingPriceThb }
    }
    val thbConfirmedCalcs = remember(orderCalculations) {
        orderCalculations.filter { it.status == OrderStatus.COMPLETED && it.currency == "THB" }
    }
    val thbConfirmedOrders = thbConfirmedCalcs
    val thbConfirmedRevenue = remember(thbConfirmedCalcs) {
        thbConfirmedCalcs.sumOf { it.sellingPriceThb }
    }
    val thbConfirmedRevenueMmk = remember(thbConfirmedCalcs) {
        thbConfirmedCalcs.sumOf { it.sellingPriceMmk }
    }
    val totalRefundedAmount = remember(orderCalculations) {
        orderCalculations
            .filter { it.status == OrderStatus.REFUNDED }
            .sumOf { it.sellingPriceMmk }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        if (activeTabMode == HistoryTabMode.SAVED_RECEIPTS) {
            SavedReceiptsHistoryContent(
                viewModel = viewModel,
                showEmbeddedClearAllButton = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
        // Top Switcher Segmented Control for Active Sales History Modes (Horizontally Scrollable)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: အတည်ပြုပြီး အရောင်းများ
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (activeTabMode == HistoryTabMode.CONFIRMED) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (activeTabMode == HistoryTabMode.CONFIRMED) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .height(36.dp)
                        .clickable { setActiveTabMode(HistoryTabMode.CONFIRMED) }
                ) {
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "အတည်ပြုပြီး (${confirmedOrdersCount})",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            fontWeight = if (activeTabMode == HistoryTabMode.CONFIRMED) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Tab 2: အရောင်းမှတ်တမ်း အားလုံး
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (activeTabMode == HistoryTabMode.ALL) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (activeTabMode == HistoryTabMode.ALL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .height(36.dp)
                        .clickable { setActiveTabMode(HistoryTabMode.ALL) }
                ) {
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "အားလုံး (${orders.size})",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            fontWeight = if (activeTabMode == HistoryTabMode.ALL) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Tab 3: ငွေပြန်အမ်းပြီး စာရင်းများ
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (activeTabMode == HistoryTabMode.REFUNDED) MaterialTheme.colorScheme.error else Color.Transparent,
                    contentColor = if (activeTabMode == HistoryTabMode.REFUNDED) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .height(36.dp)
                        .clickable { setActiveTabMode(HistoryTabMode.REFUNDED) }
                ) {
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "ပြန်အမ်းပြီး (${refundedOrdersCount})",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            fontWeight = if (activeTabMode == HistoryTabMode.REFUNDED) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Header based on selected Mode
        when (activeTabMode) {
            HistoryTabMode.CONFIRMED -> {
                // Confirmed Records Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "အတည်ပြုပြီးသော လက်ရှိအရောင်းမှတ်တမ်းများ",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = "ငွေလက်ခံ အတည်ပြုပြီးသော အရောင်းဘောက်ချာများ သီးသန့်",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Confirmed Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "အတည်ပြုပြီး စုစုပေါင်းရောင်းရငွေ ($selectedTimeFilter)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val revColor = if (isDark) CashGreenDark else Color(0xFF00602F)
                            val thbRevColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                            Text(
                                text = PosViewModel.formatKyatMm(totalConfirmedRevenue),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = revColor,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = "= ${PosViewModel.formatCurrency(totalConfirmedRevenueThb, "THB")}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = thbRevColor,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${filteredOrders.size} စောင်",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (showClearIconButton) {
                                var showClearDialog by remember { mutableStateOf(false) }

                                Button(
                                    onClick = { showClearDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Clear and Save All Sales",
                                        modifier = Modifier.size(20.dp).testTag("clear_sales_button")
                                    )
                                }

                                if (showClearDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showClearDialog = false },
                                        title = {
                                            Text(
                                                text = "အရောင်းပြေစာများ အားလုံး ရှင်းမည်/သိမ်းမည်",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = "အတည်ပြုပြီးသော လက်ရှိအရောင်းပြေစာများ အားလုံးကို ရှင်းလင်းပြီး ဆက်တင်ထဲရှိ \"ရှင်းပြီးပြေစာများ မှတ်တမ်း\" သို့ တစ်ခါတည်း ရွှေ့ပြောင်းသိမ်းဆည်းလိုပါသလား?\n(မှတ်ချက်။ ။ ငွေပြန်အမ်းထားသော ပြေစာများ ပြောင်းလဲမည် မဟုတ်ပါ)",
                                                fontSize = 13.sp
                                            )
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    viewModel.clearAllCompletedSales()
                                                    showClearDialog = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text("အတည်ပြုသည်", color = Color.White)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showClearDialog = false }) {
                                                Text("မလုပ်တော့ပါ")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            HistoryTabMode.REFUNDED -> {
                // Refunded Records Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ငွေပြန်အမ်းထားသော မှတ်တမ်းများ",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = "ဝယ်ယူသူအား ငွေပြန်အမ်းပြီး ကုန်ပစ္စည်းလက်ကျန် ပြန်ဖြည့်ထားသော ဘောက်ချာများ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }
            HistoryTabMode.ALL -> {
                // General / All History Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "အရောင်းမှတ်တမ်း မျက်နှာပြင် (အားလုံး)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = "လက်ရှိ အရောင်းမှတ်တမ်းများနှင့် ငွေပြန်အမ်းမှတ်တမ်းများ အားလုံး (${orders.size} စောင်)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }
            HistoryTabMode.SAVED_RECEIPTS -> {}
        }

        // Linear Landscape Layout containing Left & Right Portrait Cards (Only shown in Confirmed Mode, Right above Search Box)
        if (activeTabMode == HistoryTabMode.CONFIRMED) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Portrait Card (ငွေရှင်းကောင်တာတွင် မြန်မာငွေ ကျပ် ဖြင့် ရှင်းခဲ့သော အရောင်းစာရင်း)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.2.dp,
                        if (isDark) CashGreenDark.copy(alpha = 0.45f) else Color(0xFF00602F).copy(alpha = 0.28f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isDark) CashGreenDark else Color(0xFF00602F),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "မြန်မာငွေ (ကျပ်) ${mmkConfirmedOrders.size} စောင်",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        val revColor = if (isDark) CashGreenDark else Color(0xFF00602F)
                        val thbRevColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                        Text(
                            text = PosViewModel.formatKyatMm(mmkConfirmedRevenue),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = revColor,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "= ${PosViewModel.formatCurrency(mmkConfirmedRevenueThb, "THB")}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = thbRevColor,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }

                // Right Portrait Card (ငွေရှင်းကောင်တာတွင် ထိုင်းငွေ ဘတ် ဖြင့် ရှင်းခဲ့သော အရောင်းစာရင်း)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.2.dp,
                        if (isDark) Color(0xFF60A5FA).copy(alpha = 0.45f) else Color(0xFF0066B3).copy(alpha = 0.28f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "ထိုင်းငွေ (ဘတ်) ${thbConfirmedOrders.size} စောင်",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        val revColor = if (isDark) CashGreenDark else Color(0xFF00602F)
                        val thbRevColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                        Text(
                            text = PosViewModel.formatCurrency(thbConfirmedRevenue, "THB"),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = thbRevColor,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "= ${PosViewModel.formatKyatMm(thbConfirmedRevenueMmk)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = revColor,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ဘောက်ချာနံပါတ် / ဝယ်ယူသူ ရှာပါ...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Time Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(timeFilterOptions) { filter ->
                val isSelected = filter == selectedTimeFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTimeFilter = filter },
                    label = {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Orders List
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (activeTabMode == HistoryTabMode.CONFIRMED) Icons.Default.CheckCircle else Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (activeTabMode == HistoryTabMode.CONFIRMED) {
                            "အတည်ပြုပြီးသော အရောင်းမှတ်တမ်း မရှိသေးပါ"
                        } else {
                            "မှတ်တမ်း မရှိသေးပါ"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                orderCalculations.forEach { calc ->
                    androidx.compose.runtime.key(calc.id) {
                        ReceiptCalculationCard(
                            calculation = calc,
                            displayCurrency = "BOTH",
                            onClick = { viewModel.showReceipt(calc.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        }
    }
}

@Composable
fun OrderHistoryItemCard(
    order: Order,
    onClick: () -> Unit
) {
    val isRefunded = order.status == OrderStatus.REFUNDED
    val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(order.timestamp))
    val isDark = isSystemInDarkTheme()

    val payBrandColor = PaymentColors.getBrandColor(order.paymentMethod, isDark)
    val payContainerColor = PaymentColors.getContainerColor(order.paymentMethod, isDark)
    val payTextColor = PaymentColors.getTextColor(order.paymentMethod, isDark)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("order_item_${order.id}"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.invoiceNumber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false).basicMarquee()
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isRefunded) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "ပြန်အမ်းပြီး ✕",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "အတည်ပြုပြီး ✓",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (order.customerName.isNotBlank()) {
                    Text(
                        text = "ဝယ်ယူသူ: ${order.customerName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = payContainerColor,
                        border = BorderStroke(0.5.dp, payBrandColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = PaymentColors.getIcon(order.paymentMethod),
                                contentDescription = null,
                                tint = payTextColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (order.currency == "THB") "${order.paymentMethod.labelMm} (THB)" else "${order.paymentMethod.labelMm} (MMK)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = payTextColor
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val primaryAmount = if (order.currency == "THB") {
                    PosViewModel.formatCurrency(order.grandTotal, "THB")
                } else {
                    PosViewModel.formatKyatMm(order.grandTotal)
                }
                val thbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                val mmkColor = if (isDark) CashGreenDark else CashGreen
                Text(
                    text = primaryAmount,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (isRefunded) MaterialTheme.colorScheme.error else (if (order.currency == "THB") thbColor else mmkColor),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                val altText = if (order.currency == "THB") {
                    "= ${PosViewModel.formatKyatMm(order.getEffectiveTotalMmk())}"
                } else {
                    "= ${PosViewModel.formatCurrency(order.getEffectiveTotalThb(), "THB")}"
                }
                Text(
                    text = altText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (order.currency == "THB") mmkColor else thbColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ပြေစာကြည့်ရန်",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
