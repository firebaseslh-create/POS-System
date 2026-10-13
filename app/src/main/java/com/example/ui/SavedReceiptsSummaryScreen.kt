package com.example.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SavedReceipt
import com.example.ui.components.ReceiptCalculationCard
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenDark
import com.example.ui.theme.KPayBlue
import com.example.ui.theme.KPayBlueDark
import com.example.util.ReceiptCalculator
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SavedReceiptsSummaryTab {
    OVERVIEW, // အနှစ်ချုပ် စာရင်း
    MONTHLY   // လအလိုက် စာရင်းချုပ် (Monthly Sales & Profit)
}

data class TopProductSummaryItemStat(
    val productName: String,
    val quantity: Int,
    val totalMmk: Long,
    val totalThb: Long,
    val currency: String
)

data class PaymentMethodBreakdownStat(
    val methodName: String,
    val totalMmk: Long,
    val totalThb: Long,
    val count: Int
)

data class MonthlySavedReceiptReport(
    val year: Int,
    val month: Int,
    val monthNameMm: String,
    val monthShortNameMm: String,
    val totalRevenue: Long,
    val totalRevenueThb: Long = 0L,
    val totalProfit: Long,
    val totalProfitThb: Long = 0L,
    val profitMarginPercent: Int,
    val totalReceipts: Int,
    val totalUnitsSold: Int,
    val totalDiscounts: Long,
    val totalDiscountsThb: Long = 0L,
    val avgTicketValue: Long,
    val avgTicketValueThb: Long = 0L,
    val topSellingItems: List<TopProductSummaryItemStat>,
    val paymentBreakdown: List<PaymentMethodBreakdownStat>
)

@Composable
fun SavedReceiptsSummaryContent(
    viewModel: PosViewModel,
    initialCurrency: String = "BOTH",
    modifier: Modifier = Modifier
) {
    val savedReceipts by viewModel.savedReceipts.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()

    var activeTab by rememberSaveable { mutableStateOf(SavedReceiptsSummaryTab.OVERVIEW) }
    // Overview tab and Monthly tab have separate independent currency states
    var overviewCurrency by rememberSaveable { mutableStateOf(if (initialCurrency == "THB") "THB" else "MMK") } // "MMK" (ကျပ်) or "THB" (ဘတ်)
    val isOverviewThb = overviewCurrency == "THB"

    var monthlyCurrency by rememberSaveable { mutableStateOf(if (initialCurrency == "THB") "THB" else "MMK") } // "MMK" (ကျပ်) or "THB" (ဘတ်)
    val isMonthlyThb = monthlyCurrency == "THB"

    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Helper to check if a SavedReceipt belongs to THB or MMK
    fun isReceiptMatchingCurrency(receipt: SavedReceipt, targetCurrency: String): Boolean {
        val isThbReceipt = com.example.data.isThbCurrencyString(receipt.currency)
        return if (targetCurrency == "THB") isThbReceipt else !isThbReceipt
    }

    // Filter saved receipts for Overview tab by overviewCurrency ("MMK" = ကျပ် or "THB" = ဘတ်)
    val overviewFilteredReceipts = remember(savedReceipts, overviewCurrency) {
        savedReceipts.filter { isReceiptMatchingCurrency(it, overviewCurrency) }
    }

    // Filter today's saved receipts for Overview tab by overviewCurrency
    val todaySavedReceipts = remember(overviewFilteredReceipts, startOfToday) {
        overviewFilteredReceipts.filter {
            it.clearedTimestamp >= startOfToday || it.originalTimestamp >= startOfToday
        }
    }

    // Filter saved receipts for Monthly tab by monthlyCurrency ("MMK" = ကျပ် or "THB" = ဘတ်)
    val monthlyFilteredReceipts = remember(savedReceipts, monthlyCurrency) {
        savedReceipts.filter { isReceiptMatchingCurrency(it, monthlyCurrency) }
    }

    val todayMonthlyReceipts = remember(monthlyFilteredReceipts, startOfToday) {
        monthlyFilteredReceipts.filter {
            it.clearedTimestamp >= startOfToday || it.originalTimestamp >= startOfToday
        }
    }

    val savedReceiptCalculations = remember(overviewFilteredReceipts, products, shopProfile) {
        overviewFilteredReceipts.map { receipt ->
            ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
        }
    }

    val todaySavedReceiptCalculations = remember(todaySavedReceipts, products, shopProfile) {
        todaySavedReceipts.map { receipt ->
            ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
        }
    }

    val monthlyReceiptCalculations = remember(monthlyFilteredReceipts, products, shopProfile) {
        monthlyFilteredReceipts.map { receipt ->
            ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
        }
    }

    // Sales totals for Overview tab (strictly from filtered per-receipt calculations)
    val todaySales = remember(todaySavedReceiptCalculations) {
        todaySavedReceiptCalculations.sumOf { it.sellingPriceMmk }
    }
    val todaySalesThb = remember(todaySavedReceiptCalculations) {
        todaySavedReceiptCalculations.sumOf { it.sellingPriceThb }
    }

    val totalSales = remember(savedReceiptCalculations) {
        savedReceiptCalculations.sumOf { it.sellingPriceMmk }
    }
    val totalSalesThb = remember(savedReceiptCalculations) {
        savedReceiptCalculations.sumOf { it.sellingPriceThb }
    }

    val todayProfit = remember(todaySavedReceiptCalculations) {
        todaySavedReceiptCalculations.sumOf { it.estimatedNetProfitMmk }
    }
    val todayProfitThb = remember(todaySavedReceiptCalculations) {
        todaySavedReceiptCalculations.sumOf { it.estimatedNetProfitThb }
    }

    val totalProfit = remember(savedReceiptCalculations) {
        savedReceiptCalculations.sumOf { it.estimatedNetProfitMmk }
    }
    val totalProfitThb = remember(savedReceiptCalculations) {
        savedReceiptCalculations.sumOf { it.estimatedNetProfitThb }
    }

    // Sales & Profit totals for Monthly tab (strictly from monthlyFilteredReceipts)
    val monthlyTotalSales = remember(monthlyReceiptCalculations) {
        monthlyReceiptCalculations.sumOf { it.sellingPriceMmk }
    }
    val monthlyTotalSalesThb = remember(monthlyReceiptCalculations) {
        monthlyReceiptCalculations.sumOf { it.sellingPriceThb }
    }
    val monthlyTotalProfit = remember(monthlyReceiptCalculations) {
        monthlyReceiptCalculations.sumOf { it.estimatedNetProfitMmk }
    }
    val monthlyTotalProfitThb = remember(monthlyReceiptCalculations) {
        monthlyReceiptCalculations.sumOf { it.estimatedNetProfitThb }
    }

    val productMap = remember(products) {
        products.associateBy { it.id }
    }

    // Monthly Report Grouping from Filtered Saved Receipts
    val monthlyReports = remember(monthlyFilteredReceipts, products, productMap, shopProfile) {
        val monthNamesMm = listOf(
            "", "ဇန်နဝါရီ", "ဖေဖော်ဝါရီ", "မတ်", "ဧပြီ", "မေ", "ဇွန်",
            "ဇူလိုင်", "သြဂုတ်", "စက်တင်ဘာ", "အောက်တိုဘာ", "နိုဝင်ဘာ", "ဒီဇင်ဘာ"
        )

        monthlyFilteredReceipts
            .groupBy { receipt ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = if (receipt.clearedTimestamp > 0) receipt.clearedTimestamp else receipt.originalTimestamp
                }
                Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            }
            .map { (yearMonth, monthReceipts) ->
                val (year, month) = yearMonth
                val monthCalcs = monthReceipts.map { ReceiptCalculator.calculateFromSavedReceipt(it, products, shopProfile) }
                val totalRev = monthCalcs.sumOf { it.sellingPriceMmk }
                val totalRevThb = monthCalcs.sumOf { it.sellingPriceThb }
                val totalProf = monthCalcs.sumOf { it.estimatedNetProfitMmk }
                val totalProfThb = monthCalcs.sumOf { it.estimatedNetProfitThb }
                val totalDisc = monthCalcs.sumOf { it.discountMmk }
                val totalDiscThb = monthCalcs.sumOf { it.discountThb }
                val margin = if (totalRev > 0) ((totalProf.toDouble() / totalRev) * 100).toInt() else 0
                val avgTicket = if (monthReceipts.isNotEmpty()) totalRev / monthReceipts.size else 0L
                val avgTicketThb = if (monthReceipts.isNotEmpty()) totalRevThb / monthReceipts.size else 0L

                // Extract items with their receipt context for accurate per-receipt exchange rate calculation
                val allMonthItemsWithReceipt = monthReceipts.flatMap { r ->
                    SavedReceipt.parseItemsJson(r.itemsJson).map { item -> Pair(item, r) }
                }
                val totalUnits = allMonthItemsWithReceipt.sumOf { it.first.quantity }

                val topItems = allMonthItemsWithReceipt
                    .groupBy { it.first.productName }
                    .map { (name, pairs) ->
                        val totalQty = pairs.sumOf { it.first.quantity }
                        val thbCount = pairs.count { (item, _) -> ReceiptCalculator.isThbOrderItem(item, productMap[item.productId]) }
                        val mmkCount = pairs.size - thbCount
                        val primaryCurr = if (thbCount > mmkCount) "THB" else "MMK"
                        val totalMmk = pairs.sumOf { (item, r) ->
                            val (rThbToMmk, _) = ReceiptCalculator.resolveOrderRates(r.currency, r.exchangeRate, r.totalMmk, r.totalThb, shopProfile)
                            val isThbItem = ReceiptCalculator.isThbOrderItem(item, productMap[item.productId])
                            if (isThbItem) {
                                if (rThbToMmk > 0) Math.round(item.subtotal * rThbToMmk) else 0L
                            } else {
                                item.subtotal
                            }
                        }
                        val totalThb = pairs.sumOf { (item, r) ->
                            val (_, rMmkToThb) = ReceiptCalculator.resolveOrderRates(r.currency, r.exchangeRate, r.totalMmk, r.totalThb, shopProfile)
                            val isThbItem = ReceiptCalculator.isThbOrderItem(item, productMap[item.productId])
                            if (isThbItem) {
                                item.subtotal
                            } else {
                                if (rMmkToThb > 0) Math.round(item.subtotal * rMmkToThb) else 0L
                            }
                        }
                        TopProductSummaryItemStat(
                            productName = name,
                            quantity = totalQty,
                            totalMmk = totalMmk,
                            totalThb = totalThb,
                            currency = primaryCurr
                        )
                    }
                    .sortedByDescending { it.quantity }
                    .take(3)

                val pmtBreakdown = monthCalcs
                    .groupBy { it.paymentMethod.labelMm }
                    .map { (methodName, calcs) ->
                        PaymentMethodBreakdownStat(
                            methodName = methodName,
                            totalMmk = calcs.sumOf { it.sellingPriceMmk },
                            totalThb = calcs.sumOf { it.sellingPriceThb },
                            count = calcs.size
                        )
                    }

                val monthName = monthNamesMm.getOrElse(month) { "$month လ" }
                MonthlySavedReceiptReport(
                    year = year,
                    month = month,
                    monthNameMm = "$year ခုနှစ် $monthName",
                    monthShortNameMm = "$monthName $year",
                    totalRevenue = totalRev,
                    totalRevenueThb = totalRevThb,
                    totalProfit = totalProf,
                    totalProfitThb = totalProfThb,
                    profitMarginPercent = margin,
                    totalReceipts = monthReceipts.size,
                    totalUnitsSold = totalUnits,
                    totalDiscounts = totalDisc,
                    totalDiscountsThb = totalDiscThb,
                    avgTicketValue = avgTicket,
                    avgTicketValueThb = avgTicketThb,
                    topSellingItems = topItems,
                    paymentBreakdown = pmtBreakdown
                )
            }
            .sortedWith(compareByDescending<MonthlySavedReceiptReport> { it.year }.thenByDescending { it.month })
    }

    val maxMonthlyRevenue = remember(monthlyReports) { monthlyReports.maxOfOrNull { it.totalRevenue } ?: 1L }

    val isDark = isSystemInDarkTheme()
    val cashGreenColor = if (isDark) CashGreenDark else CashGreen
    val kPayBlueColor = if (isDark) KPayBlueDark else KPayBlue
    val cashBgColor = if (isDark) CashGreenDark.copy(alpha = 0.18f) else CashGreen.copy(alpha = 0.12f)
    val metricCardBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
            // Segmented Tab Switcher (Overview vs Monthly) - Balanced Equal-Width Two-Button Layout
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: အနှစ်ချုပ် စာရင်း (Overview)
                    val isOverviewSelected = activeTab == SavedReceiptsSummaryTab.OVERVIEW
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOverviewSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isOverviewSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        shadowElevation = if (isOverviewSelected) 2.dp else 0.dp,
                        border = if (isOverviewSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("saved_summary_tab_overview")
                            .clickable { activeTab = SavedReceiptsSummaryTab.OVERVIEW }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "အနှစ်ချုပ် စာရင်း",
                                fontSize = 12.sp,
                                maxLines = 1,
                                fontWeight = if (isOverviewSelected) FontWeight.Bold else FontWeight.SemiBold,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }

                    // Tab 2: လအလိုက် စာရင်းချုပ် (Monthly Sales & Profit)
                    val isMonthlySelected = activeTab == SavedReceiptsSummaryTab.MONTHLY
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMonthlySelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isMonthlySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        shadowElevation = if (isMonthlySelected) 2.dp else 0.dp,
                        border = if (isMonthlySelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("saved_summary_tab_monthly")
                            .clickable { activeTab = SavedReceiptsSummaryTab.MONTHLY }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "လအလိုက် စာရင်းချုပ်",
                                fontSize = 12.sp,
                                maxLines = 1,
                                fontWeight = if (isMonthlySelected) FontWeight.Bold else FontWeight.SemiBold,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
            }

            // Header Info Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val activeFilteredCount = if (activeTab == SavedReceiptsSummaryTab.OVERVIEW) overviewFilteredReceipts.size else monthlyFilteredReceipts.size
                        val activeTodayCount = if (activeTab == SavedReceiptsSummaryTab.OVERVIEW) todaySavedReceipts.size else todayMonthlyReceipts.size
                        val activeCurrencyLabel = if (activeTab == SavedReceiptsSummaryTab.OVERVIEW) {
                            if (isOverviewThb) "ထိုင်းဘတ်ငွေ (THB)" else "မြန်မာကျပ်ငွေ (MMK)"
                        } else {
                            if (isMonthlyThb) "ထိုင်းဘတ်ငွေ (THB)" else "မြန်မာကျပ်ငွေ (MMK)"
                        }
                        Text(
                            text = "ရှင်းပြီး သိမ်းဆည်းထားသော ပြေစာများ စာရင်းချုပ် ($activeCurrencyLabel)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "စုစုပေါင်း ပြေစာ $activeFilteredCount စောင် (ယနေ့ $activeTodayCount စောင်)",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }

            // CONTENT TAB 1: OVERVIEW
            if (activeTab == SavedReceiptsSummaryTab.OVERVIEW) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "အရောင်း နှင့် အမြတ်ငွေ အနှစ်ချုပ် ဇယား",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Integrated Interactive Toggle Button (combining MMK and THB) next to the title (resized to match FilterChip style)
                    Surface(
                        onClick = {
                            overviewCurrency = if (isOverviewThb) "MMK" else "THB"
                        },
                        shape = RoundedCornerShape(12.dp), // Matches FilterChip corner radius
                        color = if (isOverviewThb) (if (isDark) Color(0xFF2563EB) else Color(0xFF0066B3)) else (if (isDark) Color(0xFF059669) else CashGreen),
                        contentColor = Color.White,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .height(32.dp) // Matches standard FilterChip height perfectly
                            .testTag("saved_summary_currency_toggle_overview")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isOverviewThb) "🇹🇭 ဘတ်" else "🇲🇲 ကျပ်",
                                fontSize = 11.sp, // Matches FilterChip text size
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Toggle Currency",
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // Grid of 4 Key Metrics Cards
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Row 1: Today's Sales & Total Sales
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryMetricCard(
                            title = "ယနေ့ ရောင်းရငွေ",
                            amount = todaySales,
                            amountThb = todaySalesThb,
                            subtitle = "${todaySavedReceipts.size} စောင် မှ",
                            icon = Icons.Default.TrendingUp,
                            containerColor = metricCardBg,
                            accentColor = MaterialTheme.colorScheme.primary,
                            settlementCurrency = overviewCurrency,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryMetricCard(
                            title = "စုစုပေါင်း ရောင်းရငွေ",
                            amount = totalSales,
                            amountThb = totalSalesThb,
                            subtitle = "ပြေစာ ${overviewFilteredReceipts.size} စောင် အားလုံး",
                            icon = Icons.Default.MonetizationOn,
                            containerColor = metricCardBg,
                            accentColor = kPayBlueColor,
                            settlementCurrency = overviewCurrency,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Today's Profit & Total Profit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryMetricCard(
                            title = "ယနေ့ အမြတ်ငွေ",
                            amount = todayProfit,
                            amountThb = todayProfitThb,
                            subtitle = "ယနေ့ ရှင်းပြီး ပြေစာ အမြတ်",
                            icon = Icons.Default.Payments,
                            containerColor = cashBgColor,
                            accentColor = cashGreenColor,
                            settlementCurrency = overviewCurrency,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryMetricCard(
                            title = "စုစုပေါင်း အမြတ်ငွေ",
                            amount = totalProfit,
                            amountThb = totalProfitThb,
                            subtitle = "ရှင်းပြီး ပြေစာ အားလုံး အမြတ်",
                            icon = Icons.Default.Savings,
                            containerColor = cashBgColor,
                            accentColor = cashGreenColor,
                            settlementCurrency = overviewCurrency,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Details Breakdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "အသေးစိတ် အချက်အလက်များ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        MetricRowItem(
                            label = "ယနေ့ ရှင်းပြီး ပြေစာ အရေအတွက်",
                            value = "${todaySavedReceipts.size} စောင်"
                        )

                        MetricRowItem(
                            label = "စုစုပေါင်း ရှင်းပြီး ပြေစာ အရေအတွက်",
                            value = "${overviewFilteredReceipts.size} စောင်"
                        )

                        val avgTodaySale = if (todaySavedReceipts.isNotEmpty()) todaySales / todaySavedReceipts.size else 0L
                        val avgTodaySaleThb = if (todaySavedReceipts.isNotEmpty()) todaySalesThb / todaySavedReceipts.size else 0L
                        val avgTodayText = if (isOverviewThb) {
                            "${PosViewModel.formatCurrency(avgTodaySaleThb, "THB")} (= ${PosViewModel.formatKyatMm(avgTodaySale)})"
                        } else {
                            "${PosViewModel.formatKyatMm(avgTodaySale)} (= ${PosViewModel.formatCurrency(avgTodaySaleThb, "THB")})"
                        }
                        MetricRowItem(
                            label = "ယနေ့ ပျမ်းမျှ ပြေစာတန်ဖိုး",
                            value = avgTodayText
                        )

                        val avgTotalSale = if (overviewFilteredReceipts.isNotEmpty()) totalSales / overviewFilteredReceipts.size else 0L
                        val avgTotalSaleThb = if (overviewFilteredReceipts.isNotEmpty()) totalSalesThb / overviewFilteredReceipts.size else 0L
                        val avgTotalText = if (isOverviewThb) {
                            "${PosViewModel.formatCurrency(avgTotalSaleThb, "THB")} (= ${PosViewModel.formatKyatMm(avgTotalSale)})"
                        } else {
                            "${PosViewModel.formatKyatMm(avgTotalSale)} (= ${PosViewModel.formatCurrency(avgTotalSaleThb, "THB")})"
                        }
                        MetricRowItem(
                            label = "စုစုပေါင်း ပျမ်းမျှ ပြေစာတန်ဖိုး",
                            value = avgTotalText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ရှင်းပြီးသား ပြေစာများ၏ တစ်စောင်ချင်းတွက်ချက်မှု တန်းစီဇယား (ကတ်အပြင်ဘက်သို့ ထုတ်၍ ပြသထားသည်)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ရှင်းပြီးပြေစာများ တစ်စောင်ချင်းတွက်ချက်မှု",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${savedReceiptCalculations.size} စောင်",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (savedReceiptCalculations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isOverviewThb) "ထိုင်းငွေ (ဘတ်) ဖြင့် ရှင်းထားသော ပြေစာ မရှိသေးပါ" else "မြန်မာငွေ (ကျပ်) ဖြင့် ရှင်းထားသော ပြေစာ မရှိသေးပါ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        savedReceiptCalculations.forEachIndexed { index, calc ->
                            val matchingReceipt = overviewFilteredReceipts.find { it.id == calc.id }
                            ReceiptCalculationCard(
                                calculation = calc,
                                displayCurrency = overviewCurrency,
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
            } else {
                // CONTENT TAB 2: MONTHLY SALES & PROFIT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "လအလိုက် အရောင်း နှင့် အမြတ်ငွေ အနှစ်ချုပ် ဇယား",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Integrated Interactive Toggle Button (combining MMK and THB) next to the title (resized to match FilterChip style)
                    Surface(
                        onClick = {
                            monthlyCurrency = if (isMonthlyThb) "MMK" else "THB"
                        },
                        shape = RoundedCornerShape(12.dp), // Matches FilterChip corner radius
                        color = if (isMonthlyThb) (if (isDark) Color(0xFF2563EB) else Color(0xFF0066B3)) else (if (isDark) Color(0xFF059669) else CashGreen),
                        contentColor = Color.White,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .height(32.dp) // Matches standard FilterChip height perfectly
                            .testTag("saved_summary_currency_toggle_monthly")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isMonthlyThb) "🇹🇭 ဘတ်" else "🇲🇲 ကျပ်",
                                fontSize = 11.sp, // Matches FilterChip text size
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Toggle Currency",
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricCard(
                        title = "လအလိုက် ရောင်းငွေ စုစုပေါင်း",
                        amount = monthlyTotalSales,
                        amountThb = monthlyTotalSalesThb,
                        subtitle = "စုစုပေါင်း ${monthlyReports.size} လတာ ပြေစာ (${monthlyFilteredReceipts.size} စောင်)",
                        icon = Icons.Default.MonetizationOn,
                        containerColor = metricCardBg,
                        accentColor = MaterialTheme.colorScheme.primary,
                        settlementCurrency = monthlyCurrency,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryMetricCard(
                        title = "လအလိုက် အမြတ်ငွေ စုစုပေါင်း",
                        amount = monthlyTotalProfit,
                        amountThb = monthlyTotalProfitThb,
                        subtitle = "စုစုပေါင်း ${monthlyReports.size} လတာ အမြတ်",
                        icon = Icons.Default.TrendingUp,
                        containerColor = metricCardBg,
                        accentColor = cashGreenColor,
                        settlementCurrency = monthlyCurrency,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (monthlyReports.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
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
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isMonthlyThb) "ထိုင်းငွေ (ဘတ်) ဖြင့် ရှင်းထားသော လအလိုက် ပြေစာများ မရှိသေးပါ" else "မြန်မာငွေ (ကျပ်) ဖြင့် ရှင်းထားသော လအလိုက် ပြေစာများ မရှိသေးပါ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee()
                            )
                            Text(
                                text = "အရောင်းစာရင်းများ ရှင်းလင်းသိမ်းဆည်းထားပါက လအလိုက် ရောင်းငွေနှင့် အမြတ်ငွေများကို ဤနေရာတွင် အလိုအလျောက် ခွဲခြမ်းပြသပေးမည် ဖြစ်ပါသည်။",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    // Visual Comparison Chart
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "လအလိုက် ရောင်းငွေနှင့် အမြတ်ငွေ နှိုင်းယှဉ်ချက်",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Legend
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ရောင်းရငွေ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(cashGreenColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("အမြတ်ငွေ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            monthlyReports.forEach { report ->
                                val revRatio = if (maxMonthlyRevenue > 0) (report.totalRevenue.toFloat() / maxMonthlyRevenue).coerceIn(0.05f, 1f) else 0f
                                val profitRatio = if (maxMonthlyRevenue > 0) (report.totalProfit.toFloat() / maxMonthlyRevenue).coerceIn(0.02f, 1f) else 0f

                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = report.monthShortNameMm,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val revText = if (isMonthlyThb) PosViewModel.formatCurrency(report.totalRevenueThb, "THB") else PosViewModel.formatKyat(report.totalRevenue)
                                            val profitText = if (isMonthlyThb) PosViewModel.formatCurrency(report.totalProfitThb, "THB") else PosViewModel.formatKyat(report.totalProfit)
                                            Text(
                                                text = "ရောင်း: $revText",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "အမြတ်: $profitText",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) CashGreenDark else Color(0xFF00602F)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    LinearProgressIndicator(
                                        progress = { revRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    LinearProgressIndicator(
                                        progress = { profitRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (isDark) CashGreenDark else CashGreen,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "လတစ်လချင်းစီ၏ အသေးစိတ် ပြေစာစာရင်းချုပ်များ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    monthlyReports.forEach { report ->
                        SavedMonthlyDetailCard(report = report, settlementCurrency = monthlyCurrency)
                    }
                }
            }
    }
}

@Composable
private fun SavedMonthlyDetailCard(
    report: MonthlySavedReceiptReport,
    settlementCurrency: String = "MMK"
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val isThb = settlementCurrency == "THB"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = report.monthNameMm,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ပြေစာ ${report.totalReceipts} စောင် • ပစ္စည်း ${report.totalUnitsSold} ခု",
                            fontSize = 10.5.sp,
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

            Spacer(modifier = Modifier.height(8.dp))

            // Main Financial Row: Revenue vs Net Profit
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("လစဉ် ရောင်းရငွေ", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    val thbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                    val mmkColor = if (isDark) CashGreenDark else CashGreen
                    val primaryRev = if (isThb) PosViewModel.formatCurrency(report.totalRevenueThb, "THB") else PosViewModel.formatKyatMm(report.totalRevenue)
                    val secondaryRev = if (isThb) "= ${PosViewModel.formatKyatMm(report.totalRevenue)}" else "= ${PosViewModel.formatCurrency(report.totalRevenueThb, "THB")}"
                    Text(
                        text = primaryRev,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = if (isThb) thbColor else mmkColor
                    )
                    Text(
                        text = secondaryRev,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isThb) mmkColor else thbColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val thbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                    val mmkColor = if (isDark) CashGreenDark else CashGreen
                    val profitBadgeBg = if (isDark) CashGreenDark.copy(alpha = 0.2f) else CashGreen.copy(alpha = 0.15f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("အသားတင် အမြတ်ငွေ", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = profitBadgeBg
                        ) {
                            Text(
                                text = "${report.profitMarginPercent}%",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = mmkColor,
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
                        fontSize = 13.5.sp,
                        color = if (isThb) thbColor else mmkColor
                    )
                    Text(
                        text = secondaryProfit,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isThb) mmkColor else thbColor
                    )
                }
            }

            // Expandable details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ပေးထားသော လျှော့စျေး", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val primaryDisc = if (isThb) PosViewModel.formatCurrency(report.totalDiscountsThb, "THB") else PosViewModel.formatKyatMm(report.totalDiscounts)
                            val secondaryDisc = if (isThb) "= ${PosViewModel.formatKyatMm(report.totalDiscounts)}" else "= ${PosViewModel.formatCurrency(report.totalDiscountsThb, "THB")}"
                            Text(
                                text = primaryDisc,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = secondaryDisc,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val thbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                            val mmkColor = if (isDark) CashGreenDark else CashGreen
                            Text("ပျမ်းမျှ ပြေစာတန်ဖိုး", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val primaryAvg = if (isThb) PosViewModel.formatCurrency(report.avgTicketValueThb, "THB") else PosViewModel.formatKyatMm(report.avgTicketValue)
                            val secondaryAvg = if (isThb) "= ${PosViewModel.formatKyatMm(report.avgTicketValue)}" else "= ${PosViewModel.formatCurrency(report.avgTicketValueThb, "THB")}"
                            Text(
                                text = primaryAvg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = if (isThb) thbColor else mmkColor
                            )
                            Text(
                                text = secondaryAvg,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isThb) mmkColor else thbColor
                            )
                        }
                    }

                    // Top 3 Products in this Month
                    if (report.topSellingItems.isNotEmpty()) {
                        val thbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                        val mmkColor = if (isDark) CashGreenDark else CashGreen
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ယခုလအတွင်း အရောင်းရဆုံး ပစ္စည်းများ:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        report.topSellingItems.forEachIndexed { idx, itemStat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${idx + 1}. ${itemStat.productName} (${itemStat.quantity} ခု)", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                Column(horizontalAlignment = Alignment.End) {
                                    val prim = if (isThb) PosViewModel.formatCurrency(itemStat.totalThb, "THB") else PosViewModel.formatKyatMm(itemStat.totalMmk)
                                    val sec = if (isThb) "= ${PosViewModel.formatKyatMm(itemStat.totalMmk)}" else "= ${PosViewModel.formatCurrency(itemStat.totalThb, "THB")}"
                                    Text(prim, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = if (isThb) thbColor else mmkColor)
                                    Text(sec, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = if (isThb) mmkColor else thbColor)
                                }
                            }
                        }
                    }

                    // Payment Breakdown in this Month
                    if (report.paymentBreakdown.isNotEmpty()) {
                        val thbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                        val mmkColor = if (isDark) CashGreenDark else CashGreen
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ယခုလ ငွေပေးချေမှု နည်းလမ်းများ:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        report.paymentBreakdown.forEach { pmt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• ${pmt.methodName} (${pmt.count} စောင်)", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val pmtText = if (isThb) {
                                    PosViewModel.formatCurrency(pmt.totalThb, "THB")
                                } else {
                                    PosViewModel.formatKyatMm(pmt.totalMmk)
                                }
                                Text(pmtText, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = if (isThb) thbColor else mmkColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    amount: Long,
    amountThb: Long = 0L,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    accentColor: Color,
    settlementCurrency: String = "MMK",
    modifier: Modifier = Modifier
) {
    val isThb = settlementCurrency == "THB"
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).basicMarquee()
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val isDark = isSystemInDarkTheme()
            val thbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
            val mmkColor = if (isDark) CashGreenDark else CashGreen

            val mainAmountText = if (isThb) PosViewModel.formatCurrency(amountThb, "THB") else PosViewModel.formatKyatMm(amount)
            val subAmountText = if (isThb) "= ${PosViewModel.formatKyatMm(amount)}" else "= ${PosViewModel.formatCurrency(amountThb, "THB")}"

            Text(
                text = mainAmountText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isThb) thbColor else mmkColor
            )
            Text(
                text = subAmountText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isThb) mmkColor else thbColor
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
private fun MetricRowItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
