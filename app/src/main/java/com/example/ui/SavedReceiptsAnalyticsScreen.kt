package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.PaymentMethod
import com.example.data.entity.SavedReceipt
import com.example.ui.components.ReceiptCalculationCard
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenDark
import com.example.ui.theme.PaymentColors
import com.example.util.ReceiptCalculator
import java.util.Calendar

enum class SavedReceiptsAnalyticsTabMode {
    CONFIRMED, // အတည်ပြုပြီးသော ရှင်းပြီးစာရင်းချုပ်များ မျက်နှာပြင် (Confirmed Settlements)
    MONTHLY,   // လအလိုက် ရှင်းပြီးစာရင်းချုပ် (Monthly Sales & Profit Breakdown)
    ALL        // ရှင်းပြီးပြေစာ စာရင်းချုပ် မျက်နှာပြင် (General Sales & Performance Analytics)
}

data class SavedTopProductItemStat(
    val productName: String,
    val quantity: Int,
    val totalMmk: Long,
    val totalThb: Long,
    val currency: String
)

data class SavedMethodBreakdownItemStat(
    val method: PaymentMethod,
    val totalMmk: Long,
    val totalThb: Long,
    val count: Int
)

data class SavedMonthlyFinancialReport(
    val year: Int,
    val month: Int,
    val monthNameMm: String,
    val monthShortNameMm: String,
    val totalRevenue: Long,
    val totalRevenueThb: Long = 0L,
    val totalProfit: Long,
    val totalProfitThb: Long = 0L,
    val profitMarginPercent: Int,
    val totalOrders: Int,
    val totalUnitsSold: Int,
    val totalDiscounts: Long,
    val totalDiscountsThb: Long = 0L,
    val avgTicketValue: Long,
    val avgTicketValueThb: Long = 0L,
    val topSellingItems: List<SavedTopProductItemStat>,
    val paymentBreakdown: List<SavedMethodBreakdownItemStat>
)

@Composable
fun SavedReceiptsAnalyticsContent(
    viewModel: PosViewModel,
    initialTabMode: SavedReceiptsAnalyticsTabMode = SavedReceiptsAnalyticsTabMode.CONFIRMED,
    modifier: Modifier = Modifier
) {
    val savedReceipts by viewModel.savedReceipts.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    var activeTabMode by rememberSaveable { mutableStateOf(initialTabMode) }
    var selectedTimePeriod by rememberSaveable { mutableStateOf("ယနေ့") }

    val periodOptions = listOf("ယနေ့", "မနေ့က", "ယခုအပတ်", "ယခုလ", "အားလုံး")

    // Timestamps
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val startOfYesterday = remember(startOfToday) { startOfToday - 24 * 3600 * 1000 }
    val startOfWeek = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val startOfMonth = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Filter saved receipts by selected time period
    val periodSavedReceipts = remember(savedReceipts, selectedTimePeriod) {
        savedReceipts.filter { receipt ->
            val timestamp = if (receipt.clearedTimestamp > 0) receipt.clearedTimestamp else receipt.originalTimestamp
            when (selectedTimePeriod) {
                "ယနေ့" -> timestamp >= startOfToday
                "မနေ့က" -> timestamp in startOfYesterday until startOfToday
                "ယခုအပတ်" -> timestamp >= startOfWeek
                "ယခုလ" -> timestamp >= startOfMonth
                else -> true
            }
        }
    }

    val defaultRate = remember(shopProfile) {
        val r = shopProfile.thbSellingRate.toDoubleOrNull() ?: 0.0
        if (r > 0) r else 0.0
    }

    // Individual calculations for saved receipts
    val confirmedReceiptCalculations = remember(periodSavedReceipts, products, shopProfile) {
        periodSavedReceipts.map { receipt ->
            ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
        }
    }

    val confirmedRevenue = remember(confirmedReceiptCalculations) {
        confirmedReceiptCalculations.sumOf { it.sellingPriceMmk }
    }
    val confirmedRevenueThb = remember(confirmedReceiptCalculations) {
        confirmedReceiptCalculations.sumOf { it.sellingPriceThb }
    }
    val confirmedProfit = remember(confirmedReceiptCalculations) {
        confirmedReceiptCalculations.sumOf { it.estimatedNetProfitMmk }
    }
    val confirmedProfitThb = remember(confirmedReceiptCalculations) {
        confirmedReceiptCalculations.sumOf { it.estimatedNetProfitThb }
    }

    val confirmedDiscountsMmk = remember(confirmedReceiptCalculations) {
        confirmedReceiptCalculations.sumOf { it.discountMmk }
    }
    val confirmedDiscountsThb = remember(confirmedReceiptCalculations) {
        confirmedReceiptCalculations.sumOf { it.discountThb }
    }

    val avgTicketValue = remember(confirmedRevenue, confirmedReceiptCalculations) {
        if (confirmedReceiptCalculations.isNotEmpty()) confirmedRevenue / confirmedReceiptCalculations.size else 0L
    }
    val avgTicketValueThb = remember(confirmedRevenueThb, confirmedReceiptCalculations) {
        if (confirmedReceiptCalculations.isNotEmpty()) confirmedRevenueThb / confirmedReceiptCalculations.size else 0L
    }

    // Payment methods distribution for saved receipts in the period
    val paymentBreakdown = remember(confirmedReceiptCalculations) {
        PaymentMethod.values().map { method ->
            val matching = confirmedReceiptCalculations.filter { it.paymentMethod == method }
            val amountMmk = matching.sumOf { it.sellingPriceMmk }
            val count = matching.size
            Triple(method, amountMmk, count)
        }.filter { it.second > 0 }
    }

    val productMap = remember(products) { products.associateBy { it.id } }

    // Monthly Financial Breakdown Calculations for Saved Receipts
    val monthlyReports = remember(savedReceipts, products, productMap, shopProfile) {
        val monthNamesMm = listOf(
            "", "ဇန်နဝါရီ", "ဖေဖော်ဝါရီ", "မတ်", "ဧပြီ", "မေ", "ဇွန်",
            "ဇူလိုင်", "သြဂုတ်", "စက်တင်ဘာ", "အောက်တိုဘာ", "နိုဝင်ဘာ", "ဒီဇင်ဘာ"
        )

        savedReceipts
            .groupBy { receipt ->
                val ts = if (receipt.clearedTimestamp > 0) receipt.clearedTimestamp else receipt.originalTimestamp
                val cal = Calendar.getInstance().apply { timeInMillis = ts }
                Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            }
            .map { (yearMonth, monthReceipts) ->
                val (year, month) = yearMonth
                val monthCalcs = monthReceipts.map { ReceiptCalculator.calculateFromSavedReceipt(it, products, shopProfile) }
                val totalRev = monthCalcs.sumOf { it.sellingPriceMmk }
                val totalRevThb = monthCalcs.sumOf { it.sellingPriceThb }
                val totalProfit = monthCalcs.sumOf { it.estimatedNetProfitMmk }
                val totalProfitThb = monthCalcs.sumOf { it.estimatedNetProfitThb }
                val totalDisc = monthCalcs.sumOf { it.discountMmk }
                val totalDiscThb = monthCalcs.sumOf { it.discountThb }
                val margin = if (totalRev > 0) ((totalProfit.toDouble() / totalRev) * 100).toInt() else 0
                val totalUnits = monthReceipts.sumOf { r ->
                    SavedReceipt.parseItemsJson(r.itemsJson, r.id).sumOf { it.quantity }
                }
                val avgTicket = if (monthReceipts.isNotEmpty()) totalRev / monthReceipts.size else 0L
                val avgTicketThb = if (monthReceipts.isNotEmpty()) totalRevThb / monthReceipts.size else 0L

                val topItems = monthReceipts
                    .flatMap { receipt ->
                        val items = SavedReceipt.parseItemsJson(receipt.itemsJson, receipt.id)
                        items.map { item -> Pair(item, receipt) }
                    }
                    .groupBy { it.first.productName }
                    .map { (name, list) ->
                        val totalQty = list.sumOf { it.first.quantity }
                        val thbCount = list.count { (item, _) -> ReceiptCalculator.isThbOrderItem(item, productMap[item.productId]) }
                        val mmkCount = list.size - thbCount
                        val primaryCurr = if (thbCount > mmkCount) "THB" else "MMK"
                        val totalMmk = list.sumOf { (item, receipt) ->
                            val (rThbToMmk, _) = ReceiptCalculator.resolveOrderRates(receipt.currency, receipt.exchangeRate, receipt.totalMmk, receipt.totalThb, shopProfile)
                            val isThbItem = ReceiptCalculator.isThbOrderItem(item, productMap[item.productId])
                            if (isThbItem) {
                                if (rThbToMmk > 0) Math.round(item.subtotal * rThbToMmk) else 0L
                            } else {
                                item.subtotal
                            }
                        }
                        val totalThb = list.sumOf { (item, receipt) ->
                            val (_, rMmkToThb) = ReceiptCalculator.resolveOrderRates(receipt.currency, receipt.exchangeRate, receipt.totalMmk, receipt.totalThb, shopProfile)
                            val isThbItem = ReceiptCalculator.isThbOrderItem(item, productMap[item.productId])
                            if (isThbItem) {
                                item.subtotal
                            } else {
                                if (rMmkToThb > 0) Math.round(item.subtotal * rMmkToThb) else 0L
                            }
                        }
                        SavedTopProductItemStat(
                            productName = name,
                            quantity = totalQty,
                            totalMmk = totalMmk,
                            totalThb = totalThb,
                            currency = primaryCurr
                        )
                    }
                    .sortedByDescending { it.quantity }
                    .take(3)

                val pmtBreakdown = PaymentMethod.values().map { method ->
                    val matching = monthCalcs.filter { it.paymentMethod == method }
                    SavedMethodBreakdownItemStat(
                        method = method,
                        totalMmk = matching.sumOf { it.sellingPriceMmk },
                        totalThb = matching.sumOf { it.sellingPriceThb },
                        count = matching.size
                    )
                }.filter { it.count > 0 }

                val monthName = monthNamesMm.getOrElse(month) { "$month လ" }
                SavedMonthlyFinancialReport(
                    year = year,
                    month = month,
                    monthNameMm = "$year ခုနှစ် $monthName",
                    monthShortNameMm = "$monthName $year",
                    totalRevenue = totalRev,
                    totalRevenueThb = totalRevThb,
                    totalProfit = totalProfit,
                    totalProfitThb = totalProfitThb,
                    profitMarginPercent = margin,
                    totalOrders = monthReceipts.size,
                    totalUnitsSold = totalUnits,
                    totalDiscounts = totalDisc,
                    totalDiscountsThb = totalDiscThb,
                    avgTicketValue = avgTicket,
                    avgTicketValueThb = avgTicketThb,
                    topSellingItems = topItems,
                    paymentBreakdown = pmtBreakdown
                )
            }
            .sortedWith(compareByDescending<SavedMonthlyFinancialReport> { it.year }.thenByDescending { it.month })
    }

    val totalAllMonthsRevenue = remember(monthlyReports) { monthlyReports.sumOf { it.totalRevenue } }
    val totalAllMonthsRevenueThb = remember(monthlyReports) { monthlyReports.sumOf { it.totalRevenueThb } }
    val totalAllMonthsProfit = remember(monthlyReports) { monthlyReports.sumOf { it.totalProfit } }
    val totalAllMonthsProfitThb = remember(monthlyReports) { monthlyReports.sumOf { it.totalProfitThb } }
    val maxMonthlyRevenue = remember(monthlyReports) { monthlyReports.maxOfOrNull { it.totalRevenue } ?: 1L }

    // Top Selling Products in Period (from saved receipts)
    val topSellingItems = remember(periodSavedReceipts, productMap, shopProfile) {
        periodSavedReceipts
            .flatMap { receipt ->
                val items = SavedReceipt.parseItemsJson(receipt.itemsJson, receipt.id)
                items.map { item -> Pair(item, receipt) }
            }
            .groupBy { it.first.productName }
            .map { (name, list) ->
                val totalQty = list.sumOf { it.first.quantity }
                val thbCount = list.count { (item, _) -> ReceiptCalculator.isThbOrderItem(item, productMap[item.productId]) }
                val mmkCount = list.size - thbCount
                val primaryCurr = if (thbCount > mmkCount) "THB" else "MMK"
                val totalMmk = list.sumOf { (item, receipt) ->
                    val (rThbToMmk, _) = ReceiptCalculator.resolveOrderRates(receipt.currency, receipt.exchangeRate, receipt.totalMmk, receipt.totalThb, shopProfile)
                    val isThbItem = ReceiptCalculator.isThbOrderItem(item, productMap[item.productId])
                    if (isThbItem) {
                        if (rThbToMmk > 0) Math.round(item.subtotal * rThbToMmk) else 0L
                    } else {
                        item.subtotal
                    }
                }
                val totalThb = list.sumOf { (item, receipt) ->
                    val (_, rMmkToThb) = ReceiptCalculator.resolveOrderRates(receipt.currency, receipt.exchangeRate, receipt.totalMmk, receipt.totalThb, shopProfile)
                    val isThbItem = ReceiptCalculator.isThbOrderItem(item, productMap[item.productId])
                    if (isThbItem) {
                        item.subtotal
                    } else {
                        if (rMmkToThb > 0) Math.round(item.subtotal * rMmkToThb) else 0L
                    }
                }
                SavedTopProductItemStat(
                    productName = name,
                    quantity = totalQty,
                    totalMmk = totalMmk,
                    totalThb = totalThb,
                    currency = primaryCurr
                )
            }
            .sortedByDescending { it.quantity }
            .take(5)
    }

    // Inventory status calculation strictly for unsold remaining stock
    val unsoldStockSummary = remember(products, shopProfile) {
        ReceiptCalculator.calculateUnsoldStock(products, shopProfile)
    }
    val totalStockValueMmk = unsoldStockSummary.unsoldSellingValueMmk
    val totalStockValueThb = unsoldStockSummary.unsoldSellingValueThb
    val totalStockUnits = unsoldStockSummary.unsoldTotalQuantity
    val totalUnitsSold = remember(periodSavedReceipts) {
        periodSavedReceipts.sumOf { receipt ->
            SavedReceipt.parseItemsJson(receipt.itemsJson, receipt.id).sumOf { it.quantity }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
            // Top Switcher Segmented Control: Horizontally Scrollable 3 Tabs (Confirmed, Monthly, All)
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
                    // Tab 1: အတည်ပြုပြီး စာရင်းချုပ်
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTabMode == SavedReceiptsAnalyticsTabMode.CONFIRMED) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeTabMode == SavedReceiptsAnalyticsTabMode.CONFIRMED) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { activeTabMode = SavedReceiptsAnalyticsTabMode.CONFIRMED }
                            .testTag("saved_analytics_tab_confirmed")
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
                                text = "အတည်ပြုပြီး",
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                fontWeight = if (activeTabMode == SavedReceiptsAnalyticsTabMode.CONFIRMED) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Tab 2: လအလိုက် စာရင်းချုပ်
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTabMode == SavedReceiptsAnalyticsTabMode.MONTHLY) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeTabMode == SavedReceiptsAnalyticsTabMode.MONTHLY) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { activeTabMode = SavedReceiptsAnalyticsTabMode.MONTHLY }
                            .testTag("saved_analytics_tab_monthly")
                    ) {
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "လအလိုက်",
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                fontWeight = if (activeTabMode == SavedReceiptsAnalyticsTabMode.MONTHLY) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Tab 3: စာရင်းချုပ် (အားလုံး)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTabMode == SavedReceiptsAnalyticsTabMode.ALL) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (activeTabMode == SavedReceiptsAnalyticsTabMode.ALL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { activeTabMode = SavedReceiptsAnalyticsTabMode.ALL }
                            .testTag("saved_analytics_tab_all")
                    ) {
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "စာရင်းချုပ် (အားလုံး)",
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                fontWeight = if (activeTabMode == SavedReceiptsAnalyticsTabMode.ALL) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Screen Title Header based on Selected Mode
            when (activeTabMode) {
                SavedReceiptsAnalyticsTabMode.CONFIRMED -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent),
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
                                text = "အတည်ပြုပြီးသော ရှင်းပြီးစာရင်းချုပ်များ မျက်နှာပြင်",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = "ရှင်းလင်းသိမ်းဆည်းထားသော အရောင်းဝင်ငွေ၊ အသားတင်အမြတ်နှင့် စာရင်းရှင်းတမ်း အစီရင်ခံစာ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
                SavedReceiptsAnalyticsTabMode.MONTHLY -> {
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
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "လအလိုက် ရှင်းပြီး ရောင်းငွေနှင့် အမြတ်ငွေ စာရင်းချုပ်",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = "ရှင်းလင်းသိမ်းဆည်းထားသော လစဉ် ရောင်းရငွေ၊ အသားတင်အမြတ်ငွေနှင့် အရောင်းစွမ်းဆောင်ရည် နှိုင်းယှဉ်ချက်များ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
                SavedReceiptsAnalyticsTabMode.ALL -> {
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
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ရှင်းပြီးပြေစာများ စာရင်းချုပ် မျက်နှာပြင်",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = "ရှင်းလင်းသိမ်းဆည်းထားသော ကုန်ပစ္စည်းအရောင်းစွမ်းဆောင်ရည်၊ စတော့တန်ဖိုးနှင့် စာရင်းအင်းများ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
            }

            // Time Period Filter Chips
            if (activeTabMode != SavedReceiptsAnalyticsTabMode.MONTHLY) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(periodOptions) { period ->
                            val isSelected = period == selectedTimePeriod
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTimePeriod = period },
                                label = {
                                    Text(
                                        text = if (activeTabMode == SavedReceiptsAnalyticsTabMode.CONFIRMED) "$period စာရင်းချုပ်" else period,
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
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // ==========================================
            // CONTENT FOR MODE 1: CONFIRMED SETTLEMENTS
            // ==========================================
            if (activeTabMode == SavedReceiptsAnalyticsTabMode.CONFIRMED) {
                // Confirmed Financial KPIs Cards (2x2 Grid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val primaryRevenueStr = PosViewModel.formatKyatMm(confirmedRevenue)
                    val revenueSubtitle = "ရှင်းပြီးဘောက်ချာ ${confirmedReceiptCalculations.size} စောင် (≈ ${PosViewModel.formatCurrency(confirmedRevenueThb, "THB")})"
                    val kpiAccentColor = if (isDark) CashGreenDark else Color(0xFF00602F)
                    SavedKpiCard(
                        title = "အတည်ပြုပြီး ရောင်းရငွေ (ကျပ်)",
                        amount = primaryRevenueStr,
                        subtitle = revenueSubtitle,
                        icon = Icons.Default.Payments,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = kpiAccentColor,
                        modifier = Modifier.weight(1f)
                    )

                    val profitMargin = if (confirmedRevenue > 0) ((confirmedProfit.toDouble() / confirmedRevenue) * 100).toInt() else 0
                    val primaryProfitStr = PosViewModel.formatKyatMm(confirmedProfit)
                    val profitSubtitle = "အမြတ် $profitMargin% (≈ ${PosViewModel.formatCurrency(confirmedProfitThb, "THB")})"
                    SavedKpiCard(
                        title = "အတည်ပြုပြီး အမြတ်ငွေ (ကျပ်)",
                        amount = primaryProfitStr,
                        subtitle = profitSubtitle,
                        icon = Icons.Default.TrendingUp,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = kpiAccentColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val primaryAvgTicketStr = PosViewModel.formatKyatMm(avgTicketValue)
                    val avgSubtitle = "ပြေစာပျမ်းမျှ (≈ ${PosViewModel.formatCurrency(avgTicketValueThb, "THB")})"
                    val kpiAvgColor = if (isDark) CashGreenDark else Color(0xFF00602F)
                    SavedKpiCard(
                        title = "ပျမ်းမျှ ဘောက်ချာတန်ဖိုး",
                        amount = primaryAvgTicketStr,
                        subtitle = avgSubtitle,
                        icon = Icons.Default.Receipt,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = kpiAvgColor,
                        modifier = Modifier.weight(1f)
                    )

                    val primaryDiscountStr = PosViewModel.formatKyatMm(confirmedDiscountsMmk)
                    val discountSubtitle = "≈ ${PosViewModel.formatCurrency(confirmedDiscountsThb, "THB")}"

                    SavedKpiCard(
                        title = "ပေးထားသော လျှော့စျေး",
                        amount = primaryDiscountStr,
                        subtitle = discountSubtitle,
                        icon = Icons.Default.MonetizationOn,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Methods Settlement Breakdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ငွေပေးချေမှုပုံစံအလိုက် ငွေစာရင်းရှင်းတမ်း ($selectedTimePeriod) - မြန်မာငွေ (ကျပ်)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (paymentBreakdown.isEmpty()) {
                            Text(
                                text = "ရွေးချယ်ထားသောကာလအတွင်း အတည်ပြုပြီး ရှင်းပြီးပြေစာမှတ်တမ်း မရှိသေးပါ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        } else {
                            val activeTotalRevenue = confirmedRevenue
                            paymentBreakdown.forEach { (method, amount, count) ->
                                val ratio = if (activeTotalRevenue > 0) amount.toFloat() / activeTotalRevenue else 0f
                                val brandColor = PaymentColors.getBrandColor(method, isDark)
                                val textColor = PaymentColors.getTextColor(method, isDark)

                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = PaymentColors.getIcon(method),
                                                contentDescription = null,
                                                tint = textColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${method.labelMm} ($count ကြိမ်)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = PosViewModel.formatCurrency(amount, "MMK"),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = brandColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Financial Closing Audit Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "စာရင်းချုပ် အနှစ်ချုပ်မှတ်တမ်း ($selectedTimePeriod)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("အတည်ပြုပြီး ရှင်းပြီးဘောက်ချာစုစုပေါင်း :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${confirmedReceiptCalculations.size} စောင်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("အရောင်းဝင်ငွေ စုစုပေါင်း :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val revText = "${PosViewModel.formatKyatMm(confirmedRevenue)} (≈ ${PosViewModel.formatCurrency(confirmedRevenueThb, "THB")})"
                            val mmkColor = if (isDark) CashGreenDark else CashGreen
                            Text(revText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mmkColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("အသားတင် အမြတ်ငွေ :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val profText = "${PosViewModel.formatKyatMm(confirmedProfit)} (= ${PosViewModel.formatCurrency(confirmedProfitThb, "THB")})"
                            val mmkColor = if (isDark) CashGreenDark else CashGreen
                            Text(profText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mmkColor)
                        }
                        if (confirmedDiscountsMmk > 0 || confirmedDiscountsThb > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ပေးထားသော လျှော့စျေး စုစုပေါင်း :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val discText = "${PosViewModel.formatKyatMm(confirmedDiscountsMmk)} (= ${PosViewModel.formatCurrency(confirmedDiscountsThb, "THB")})"
                                Text(discText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Confirmed Receipts One-By-One Calculation List ("တစ်စောင်ပြီးမှ နောက်တစ်စောင် အတိအကျ တွက်ချက်၍ ပြပေးပါ")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "အတည်ပြုပြီးသော အရောင်းပြေစာများ တစ်စောင်ချင်းတွက်ချက်မှု ($selectedTimePeriod)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee()
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${confirmedReceiptCalculations.size} စောင်",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (confirmedReceiptCalculations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ရွေးချယ်ထားသော ကာလအတွက် ရှင်းပြီးပြေစာ မရှိသေးပါ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        confirmedReceiptCalculations.forEachIndexed { index, calc ->
                            val matchingReceipt = periodSavedReceipts.find { it.id == calc.id }
                            ReceiptCalculationCard(
                                calculation = calc,
                                displayCurrency = if (com.example.data.isThbCurrencyString(calc.currency)) "THB" else "MMK",
                                index = index + 1,
                                onClick = {
                                    if (matchingReceipt != null) {
                                        viewModel.showSavedReceipt(matchingReceipt)
                                    }
                                }
                            )
                        }
                    }
                }
            } else if (activeTabMode == SavedReceiptsAnalyticsTabMode.MONTHLY) {
                // ==========================================
                // CONTENT FOR MODE 2: MONTHLY SALES & PROFIT
                // ==========================================
                // Overall All-Months KPIs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val revSubtitle = if (totalAllMonthsRevenueThb > 0) {
                        "စုစုပေါင်း ${monthlyReports.size} လတာ စာရင်း (≈ ${PosViewModel.formatCurrency(totalAllMonthsRevenueThb, "THB")})"
                    } else {
                        "စုစုပေါင်း ${monthlyReports.size} လတာ စာရင်း"
                    }
                    SavedKpiCard(
                        title = "လအလိုက် စုစုပေါင်းရောင်းငွေ",
                        amount = PosViewModel.formatKyatMm(totalAllMonthsRevenue),
                        subtitle = revSubtitle,
                        icon = Icons.Default.Payments,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    val overallMargin = if (totalAllMonthsRevenue > 0) ((totalAllMonthsProfit.toDouble() / totalAllMonthsRevenue) * 100).toInt() else 0
                    val profSubtitle = if (totalAllMonthsProfitThb > 0) {
                        "အမြတ်ပျမ်းမျှ $overallMargin% (≈ ${PosViewModel.formatCurrency(totalAllMonthsProfitThb, "THB")})"
                    } else {
                        "အမြတ်ပျမ်းမျှ $overallMargin%"
                    }
                    SavedKpiCard(
                        title = "လအလိုက် စုစုပေါင်းအမြတ်ငွေ",
                        amount = PosViewModel.formatKyatMm(totalAllMonthsProfit),
                        subtitle = profSubtitle,
                        icon = Icons.Default.TrendingUp,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (monthlyReports.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "လအလိုက် ရှင်းပြီးပြေစာ စာရင်းများ မရှိသေးပါ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee()
                            )
                            Text(
                                text = "ပြေစာများ ရှင်းလင်းသိမ်းဆည်းပြီးသည်နှင့် လစဉ်အလိုက် အလိုအလျောက် စာရင်းတွက်ချက်ပြသပေးမည် ဖြစ်ပါသည်။",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    // Monthly Revenue & Profit Comparison Visual Chart
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "လအလိုက် ရောင်းငွေနှင့် အမြတ်ငွေ နှိုင်းယှဉ်ချက်",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Legend
                            val chartProfitColor = if (isDark) CashGreenDark else CashGreen
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ရောင်းရငွေ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(chartProfitColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("အမြတ်ငွေ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Monthly Comparison Bars
                            monthlyReports.forEach { report ->
                                val revRatio = if (maxMonthlyRevenue > 0) (report.totalRevenue.toFloat() / maxMonthlyRevenue).coerceIn(0.05f, 1f) else 0f
                                val profitRatio = if (maxMonthlyRevenue > 0) (report.totalProfit.toFloat() / maxMonthlyRevenue).coerceIn(0.02f, 1f) else 0f

                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = report.monthShortNameMm,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(
                                                text = "ရောင်းငွေ: ${PosViewModel.formatKyat(report.totalRevenue)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "အမြတ်: ${PosViewModel.formatKyat(report.totalProfit)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = chartProfitColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Revenue Progress Bar
                                    LinearProgressIndicator(
                                        progress = { revRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Profit Progress Bar
                                    LinearProgressIndicator(
                                        progress = { profitRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = chartProfitColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Monthly Detailed Report Breakdown Cards
                    Text(
                        text = "လတစ်လချင်းစီ၏ အသေးစိတ် စာရင်းချုပ်များ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    monthlyReports.forEach { report ->
                        SavedMonthlyDetailCard(report = report)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            } else {
                // ==========================================
                // CONTENT FOR MODE 3: GENERAL ANALYTICS
                // ==========================================
                // Store Overview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shopProfile.shopName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = "ဖုန်း: ${shopProfile.phone} • ${shopProfile.address}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // General Business KPIs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SavedKpiCard(
                        title = "ရောင်းချပြီး ပစ္စည်းစုစုပေါင်း",
                        amount = "$totalUnitsSold ခု",
                        subtitle = "ရှင်းပြီးပြေစာ ${periodSavedReceipts.size} စောင်အတွင်း",
                        icon = Icons.Default.ShoppingBag,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    val stockSubtitle = if (totalStockValueThb > 0) {
                        "${products.size} မျိုး ($totalStockUnits ခု) ≈ ${PosViewModel.formatCurrency(totalStockValueThb, "THB")}"
                    } else {
                        "${products.size} မျိုး ($totalStockUnits ခု)"
                    }
                    SavedKpiCard(
                        title = "စတော့လက်ကျန် စုစုပေါင်းတန်ဖိုး",
                        amount = PosViewModel.formatKyatMm(totalStockValueMmk),
                        subtitle = stockSubtitle,
                        icon = Icons.Default.Inventory2,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Unsold Stock Exchange Rate Calculation Card (MMK = THB)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "မရောင်းရသေးသော လက်ကျန်ပစ္စည်း တွက်ချက်မှု (${unsoldStockSummary.unsoldProductCount} မျိုး • $totalStockUnits ခု)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                        val unsoldBuyingStr = "ဝယ်ရင်းတန်ဖိုး: ${PosViewModel.formatKyatMm(unsoldStockSummary.unsoldBuyingValueMmk)}" +
                            (if (unsoldStockSummary.unsoldBuyingValueThb > 0) " (= ${PosViewModel.formatCurrency(unsoldStockSummary.unsoldBuyingValueThb, "THB")})" else "")
                        val unsoldSellingStr = "ရောင်းဈေးတန်ဖိုး: ${PosViewModel.formatKyatMm(unsoldStockSummary.unsoldSellingValueMmk)}" +
                            (if (unsoldStockSummary.unsoldSellingValueThb > 0) " (= ${PosViewModel.formatCurrency(unsoldStockSummary.unsoldSellingValueThb, "THB")})" else "")
                        val unsoldProfitStr = "အမြတ်: ${PosViewModel.formatKyatMm(unsoldStockSummary.potentialProfitMmk)}" +
                            (if (unsoldStockSummary.potentialProfitThb > 0) " (= ${PosViewModel.formatCurrency(unsoldStockSummary.potentialProfitThb, "THB")})" else "")

                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                pushStyle(
                                    androidx.compose.ui.text.SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                                append(unsoldBuyingStr)
                                pop()
                                append("   •   ")
                                pushStyle(
                                    androidx.compose.ui.text.SpanStyle(
                                        color = if (isDark) CashGreenDark else CashGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                append(unsoldSellingStr)
                                pop()
                                append("   •   ")
                                pushStyle(
                                    androidx.compose.ui.text.SpanStyle(
                                        color = if (isDark) CashGreenDark else Color(0xFF00602F),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                append(unsoldProfitStr)
                                pop()
                            },
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                        if (unsoldStockSummary.mmkSellingRate.isNotBlank() || unsoldStockSummary.mmkBuyingRate.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Text(
                                text = "မှတ်ချက် - ငွေလဲနှုန်း • မြန်မာကျပ် (MMK) = ထိုင်းဘတ် (THB)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "၁ ကျပ် ရောင်းဈေး = ${unsoldStockSummary.mmkSellingRate.ifBlank { "-" }} ဘတ် (MMK->THB)  |  ၁ ကျပ် ဝယ်ဈေး = ${unsoldStockSummary.mmkBuyingRate.ifBlank { "-" }} ဘတ် (THB->MMK)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SavedKpiCard(
                        title = "စုစုပေါင်း ရှင်းပြီးပြေစာမှတ်တမ်း",
                        amount = "${periodSavedReceipts.size} စောင်",
                        subtitle = "$selectedTimePeriod ကာလအတွင်း ရှင်းပြီးမှတ်တမ်း",
                        icon = Icons.Default.Receipt,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    SavedKpiCard(
                        title = "ဆိုင်တွင်း ပစ္စည်းအမျိုးအစား",
                        amount = "${products.size} မျိုး",
                        subtitle = "လက်ကျန်ပစ္စည်းစာရင်း",
                        icon = Icons.Default.MonetizationOn,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Top 5 Best Selling Items
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "အရောင်းရဆုံး ပစ္စည်းများ (Top 5 Best Sellers)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (topSellingItems.isEmpty()) {
                            Text(
                                text = "ရွေးချယ်ထားသောကာလအတွင်း ရှင်းပြီးပြေစာမှတ်တမ်း မရှိသေးပါ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        } else {
                            topSellingItems.forEachIndexed { index, itemStat ->
                                val qty = itemStat.quantity
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (index == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = itemStat.productName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "ရောင်းရအရေအတွက်: $qty ခု",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        val primaryText = if (itemStat.currency == "THB") {
                                            PosViewModel.formatCurrency(itemStat.totalThb, "THB")
                                        } else {
                                            PosViewModel.formatKyatMm(itemStat.totalMmk)
                                        }
                                        val topThbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                                        val topMmkColor = if (isDark) CashGreenDark else CashGreen
                                        Text(
                                            text = primaryText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (itemStat.currency == "THB") topThbColor else topMmkColor
                                        )
                                        val altText = if (itemStat.currency == "THB") {
                                            "= ${PosViewModel.formatKyatMm(itemStat.totalMmk)}"
                                        } else {
                                            "= ${PosViewModel.formatCurrency(itemStat.totalThb, "THB")}"
                                        }
                                        Text(
                                            text = altText,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (itemStat.currency == "THB") topMmkColor else topThbColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SavedMonthlyDetailCard(
    report: SavedMonthlyFinancialReport,
    settlementCurrency: String = "MMK"
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val isThb = settlementCurrency == "THB"
    val cardThbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
    val cardProfitColor = if (isDark) CashGreenDark else Color(0xFF00602F)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = report.monthNameMm,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ရှင်းပြီးဘောက်ချာ ${report.totalOrders} စောင် • ပစ္စည်း ${report.totalUnitsSold} ခု",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Financial Row: Revenue vs Net Profit
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("လစဉ် ရောင်းရငွေ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    val primaryRev = if (isThb) PosViewModel.formatCurrency(report.totalRevenueThb, "THB") else PosViewModel.formatKyatMm(report.totalRevenue)
                    val secondaryRev = if (isThb) "= ${PosViewModel.formatKyatMm(report.totalRevenue)}" else "= ${PosViewModel.formatCurrency(report.totalRevenueThb, "THB")}"
                    Text(
                        text = primaryRev,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (isThb) cardThbColor else cardProfitColor
                    )
                    Text(
                        text = secondaryRev,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isThb) cardProfitColor else cardThbColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("အသားတင် အမြတ်ငွေ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) CashGreenDark.copy(alpha = 0.22f) else CashGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${report.profitMarginPercent}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = cardProfitColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    val primaryProfit = if (isThb) PosViewModel.formatCurrency(report.totalProfitThb, "THB") else PosViewModel.formatKyatMm(report.totalProfit)
                    val secondaryProfit = if (isThb) "= ${PosViewModel.formatKyatMm(report.totalProfit)}" else "= ${PosViewModel.formatCurrency(report.totalProfitThb, "THB")}"
                    Text(
                        text = primaryProfit,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (isThb) cardThbColor else cardProfitColor
                    )
                    Text(
                        text = secondaryProfit,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isThb) cardProfitColor else cardThbColor
                    )
                }
            }

            // Expandable details section
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Metrics (Discounts & Average Ticket)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ပေးထားသော လျှော့စျေး", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val primDisc = if (isThb) PosViewModel.formatCurrency(report.totalDiscountsThb, "THB") else PosViewModel.formatKyatMm(report.totalDiscounts)
                            val secDisc = if (isThb) "= ${PosViewModel.formatKyatMm(report.totalDiscounts)}" else "= ${PosViewModel.formatCurrency(report.totalDiscountsThb, "THB")}"
                            Text(
                                text = primDisc,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = secDisc,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ပျမ်းမျှ ဘောက်ချာတန်ဖိုး", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val primAvg = if (isThb) PosViewModel.formatCurrency(report.avgTicketValueThb, "THB") else PosViewModel.formatKyatMm(report.avgTicketValue)
                            val secAvg = if (isThb) "= ${PosViewModel.formatKyatMm(report.avgTicketValue)}" else "= ${PosViewModel.formatCurrency(report.avgTicketValueThb, "THB")}"
                            Text(
                                text = primAvg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isThb) cardThbColor else cardProfitColor
                            )
                            Text(
                                text = secAvg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isThb) cardProfitColor else cardThbColor
                            )
                        }
                    }

                    // Top 3 Products in this Month
                    if (report.topSellingItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ယခုလအတွင်း အရောင်းရဆုံး ပစ္စည်းများ:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        report.topSellingItems.forEachIndexed { idx, itemStat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${idx + 1}. ${itemStat.productName} (${itemStat.quantity} ခု)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                Column(horizontalAlignment = Alignment.End) {
                                    val prim = if (itemStat.currency == "THB") PosViewModel.formatCurrency(itemStat.totalThb, "THB") else PosViewModel.formatKyatMm(itemStat.totalMmk)
                                    val sec = if (itemStat.currency == "THB") "= ${PosViewModel.formatKyatMm(itemStat.totalMmk)}" else "= ${PosViewModel.formatCurrency(itemStat.totalThb, "THB")}"
                                    Text(prim, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (itemStat.currency == "THB") cardThbColor else cardProfitColor)
                                    Text(sec, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = if (itemStat.currency == "THB") cardProfitColor else cardThbColor)
                                }
                            }
                        }
                    }

                    // Payment Breakdown in this Month
                    if (report.paymentBreakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ယခုလ ငွေပေးချေမှု နည်းလမ်းများ:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        report.paymentBreakdown.forEach { stat ->
                            val methodTextColor = PaymentColors.getTextColor(stat.method, isDark)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = PaymentColors.getIcon(stat.method),
                                        contentDescription = null,
                                        tint = methodTextColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("• ${stat.method.labelMm} (${stat.count} စောင်)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                val pmtText = if (isThb) {
                                    PosViewModel.formatCurrency(stat.totalThb, "THB")
                                } else {
                                    PosViewModel.formatKyatMm(stat.totalMmk)
                                }
                                Text(pmtText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = methodTextColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedKpiCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).basicMarquee()
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = amount,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}
