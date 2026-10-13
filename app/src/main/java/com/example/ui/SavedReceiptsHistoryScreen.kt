package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.OrderStatus
import com.example.data.entity.PaymentMethod
import com.example.data.entity.SavedReceipt
import com.example.ui.components.ReceiptCalculationCard
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenDark
import com.example.ui.theme.PaymentColors
import com.example.util.ReceiptCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SavedReceiptsCurrencyFilter {
    ALL, // ရှင်းပြီး ပြေစာ အားလုံး
    MMK, // မြန်မာကျပ် ရှင်းပြီးပြေစာများ
    THB  // ထိုင်းဘတ် ရှင်းပြီးပြေစာများ
}

@Composable
fun SavedReceiptsHistoryContent(
    viewModel: PosViewModel,
    showDeleteAllDialogState: Boolean = false,
    onDismissDeleteAllDialog: () -> Unit = {},
    showEmbeddedClearAllButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    val savedReceipts by viewModel.savedReceipts.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    var selectedCurrencyTab by rememberSaveable { mutableStateOf(SavedReceiptsCurrencyFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTimeFilter by rememberSaveable { mutableStateOf("အားလုံး") }
    var selectedPaymentFilter by rememberSaveable { mutableStateOf("အားလုံး") }
    var internalShowDeleteAllDialog by remember { mutableStateOf(false) }
    val showDeleteAllDialog = showDeleteAllDialogState || internalShowDeleteAllDialog
    val dismissDeleteAllDialog = {
        internalShowDeleteAllDialog = false
        onDismissDeleteAllDialog()
    }

    // State for viewing a specific saved receipt in an archive dialog
    var selectedSavedReceiptForDetail by remember { mutableStateOf<SavedReceipt?>(null) }
    var receiptToRestore by remember { mutableStateOf<SavedReceipt?>(null) }
    var receiptToDelete by remember { mutableStateOf<SavedReceipt?>(null) }

    val timeFilterOptions = listOf("အားလုံး", "ယနေ့", "မနေ့က", "ယခုအပတ်", "ယခုလ")
    val paymentFilterOptions = listOf("အားလုံး", "ငွေသား", "KBZPay", "WavePay", "AYA Pay", "အကြွေး")

    // Filter logic
    val baseFilteredReceipts = remember(savedReceipts, searchQuery, selectedTimeFilter, selectedPaymentFilter) {
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

        savedReceipts.filter { receipt ->
            val timestamp = if (receipt.clearedTimestamp > 0) receipt.clearedTimestamp else receipt.originalTimestamp

            // Time filter
            val matchDate = when (selectedTimeFilter) {
                "ယနေ့" -> timestamp >= startOfToday
                "မနေ့က" -> timestamp in startOfYesterday until startOfToday
                "ယခုအပတ်" -> timestamp >= startOfWeek
                "ယခုလ" -> timestamp >= startOfMonth
                else -> true
            }

            // Payment filter
            val matchPayment = when (selectedPaymentFilter) {
                "ငွေသား" -> receipt.paymentMethod == PaymentMethod.CASH
                "KBZPay" -> receipt.paymentMethod == PaymentMethod.KPAY
                "WavePay" -> receipt.paymentMethod == PaymentMethod.WAVEPAY
                "AYA Pay" -> receipt.paymentMethod == PaymentMethod.AYAPAY
                "အကြွေး" -> receipt.paymentMethod == PaymentMethod.CREDIT
                else -> true
            }

            // Search filter
            val matchSearch = searchQuery.isBlank() ||
                    receipt.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    receipt.customerName.contains(searchQuery, ignoreCase = true) ||
                    receipt.customerPhone.contains(searchQuery, ignoreCase = true)

            matchDate && matchPayment && matchSearch
        }
    }

    val mmkReceipts = remember(baseFilteredReceipts) {
        baseFilteredReceipts.filter { !com.example.data.isThbCurrencyString(it.currency) }
    }
    val thbReceipts = remember(baseFilteredReceipts) {
        baseFilteredReceipts.filter { com.example.data.isThbCurrencyString(it.currency) }
    }

    val filteredReceipts = remember(baseFilteredReceipts, mmkReceipts, thbReceipts, selectedCurrencyTab) {
        when (selectedCurrencyTab) {
            SavedReceiptsCurrencyFilter.MMK -> mmkReceipts
            SavedReceiptsCurrencyFilter.THB -> thbReceipts
            SavedReceiptsCurrencyFilter.ALL -> baseFilteredReceipts
        }
    }

    val baseReceiptCalculations = remember(baseFilteredReceipts, products, shopProfile) {
        baseFilteredReceipts.map { receipt ->
            ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
        }
    }
    val mmkReceiptCalculations = remember(baseReceiptCalculations) {
        baseReceiptCalculations.filter { !com.example.data.isThbCurrencyString(it.currency) }
    }
    val thbReceiptCalculations = remember(baseReceiptCalculations) {
        baseReceiptCalculations.filter { com.example.data.isThbCurrencyString(it.currency) }
    }

    val totalSavedRevenue = remember(baseReceiptCalculations) {
        baseReceiptCalculations.sumOf { it.sellingPriceMmk }
    }
    val totalSavedRevenueThb = remember(baseReceiptCalculations) {
        baseReceiptCalculations.sumOf { it.sellingPriceThb }
    }
    val mmkRevenue = remember(mmkReceiptCalculations) {
        mmkReceiptCalculations.sumOf { it.sellingPriceMmk }
    }
    val mmkRevenueThb = remember(mmkReceiptCalculations) {
        mmkReceiptCalculations.sumOf { it.sellingPriceThb }
    }
    val thbRevenue = remember(thbReceiptCalculations) {
        thbReceiptCalculations.sumOf { it.sellingPriceThb }
    }
    val thbRevenueMmk = remember(thbReceiptCalculations) {
        thbReceiptCalculations.sumOf { it.sellingPriceMmk }
    }

    Column(
        modifier = modifier
    ) {
            // Archive Segmented Currency Switcher Tabs (Horizontally Scrollable)
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
                    // Tab 1: ရှင်းပြီး အားလုံး
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.ALL) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.ALL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { selectedCurrencyTab = SavedReceiptsCurrencyFilter.ALL }
                            .testTag("saved_receipts_tab_all")
                    ) {
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ရှင်းပြီး အားလုံး (${baseFilteredReceipts.size})",
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                fontWeight = if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.ALL) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Tab 2: မြန်မာကျပ် ပြေစာများ
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.MMK) (if (isDark) CashGreenDark else CashGreen) else Color.Transparent,
                        contentColor = if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.MMK) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { selectedCurrencyTab = SavedReceiptsCurrencyFilter.MMK }
                            .testTag("saved_receipts_tab_mmk")
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
                                text = "မြန်မာကျပ် (${mmkReceipts.size})",
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                fontWeight = if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.MMK) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Tab 3: ထိုင်းဘတ် ပြေစာများ
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.THB) (if (isDark) Color(0xFF2563EB) else Color(0xFF0066B3)) else Color.Transparent,
                        contentColor = if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.THB) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { selectedCurrencyTab = SavedReceiptsCurrencyFilter.THB }
                            .testTag("saved_receipts_tab_thb")
                    ) {
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ထိုင်းဘတ် (${thbReceipts.size})",
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                fontWeight = if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.THB) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Archive Header Label & Clear Notice
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
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ရှင်းပြီးပြေစာများ သိမ်းဆည်းရာနေရာ (Archive)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = "အရောင်းမှတ်တမ်းမှ ရှင်းလင်းသိမ်းဆည်းထားပြီးဖြစ်သော ပြေစာဟောင်းများ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
                if (showEmbeddedClearAllButton && savedReceipts.isNotEmpty()) {
                    IconButton(
                        onClick = { internalShowDeleteAllDialog = true },
                        modifier = Modifier.testTag("saved_receipts_history_embedded_clear_all_button")
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear All Archived",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Confirmed/Archived Total Summary Card
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
                        val summaryTitle = when (selectedCurrencyTab) {
                            SavedReceiptsCurrencyFilter.MMK -> "ရှင်းပြီး မြန်မာငွေ စုစုပေါင်းပမာဏ ($selectedTimeFilter)"
                            SavedReceiptsCurrencyFilter.THB -> "ရှင်းပြီး ထိုင်းငွေ စုစုပေါင်းပမာဏ ($selectedTimeFilter)"
                            SavedReceiptsCurrencyFilter.ALL -> "ရှင်းပြီး စုစုပေါင်းပမာဏ ($selectedTimeFilter)"
                        }
                        Text(
                            text = summaryTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val revColor = if (isDark) CashGreenDark else Color(0xFF00602F)
                        val thbRevColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)

                        when (selectedCurrencyTab) {
                            SavedReceiptsCurrencyFilter.MMK -> {
                                Text(
                                    text = PosViewModel.formatKyatMm(mmkRevenue),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = revColor,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = "= ${PosViewModel.formatCurrency(mmkRevenueThb, "THB")}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = thbRevColor,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                            SavedReceiptsCurrencyFilter.THB -> {
                                Text(
                                    text = PosViewModel.formatCurrency(thbRevenue, "THB"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = thbRevColor,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = "= ${PosViewModel.formatKyatMm(thbRevenueMmk)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = revColor,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                            SavedReceiptsCurrencyFilter.ALL -> {
                                Text(
                                    text = PosViewModel.formatKyatMm(totalSavedRevenue),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = revColor,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = "= ${PosViewModel.formatCurrency(totalSavedRevenueThb, "THB")}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = thbRevColor,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }

                    val badgeCount = when (selectedCurrencyTab) {
                        SavedReceiptsCurrencyFilter.MMK -> mmkReceipts.size
                        SavedReceiptsCurrencyFilter.THB -> thbReceipts.size
                        SavedReceiptsCurrencyFilter.ALL -> filteredReceipts.size
                    }

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
                                Icons.Default.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$badgeCount စောင်",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Show breakdown cards row only when inside 'ရှင်းပြီး အားလုံး' tab
            if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.ALL) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Portrait Card (မြန်မာငွေ ကျပ် ဖြင့် ရှင်းခဲ့သော စာရင်း)
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
                                    text = "မြန်မာငွေ (ကျပ်) ${mmkReceipts.size} စောင်",
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
                                text = PosViewModel.formatKyatMm(mmkRevenue),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = revColor,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "= ${PosViewModel.formatCurrency(mmkRevenueThb, "THB")}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = thbRevColor,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }

                    // Right Portrait Card (ထိုင်းငွေ ဘတ် ဖြင့် ရှင်းခဲ့သော စာရင်း)
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
                                    text = "ထိုင်းငွေ (ဘတ်) ${thbReceipts.size} စောင်",
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
                                text = PosViewModel.formatCurrency(thbRevenue, "THB"),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = thbRevColor,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "= ${PosViewModel.formatKyatMm(thbRevenueMmk)}",
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
                placeholder = { Text("ရှင်းပြီး ဘောက်ချာနံပါတ် / ဝယ်ယူသူ ရှာပါ...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                    .testTag("saved_receipts_history_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Payment Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(paymentFilterOptions) { payment ->
                    val isSelected = payment == selectedPaymentFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPaymentFilter = payment },
                        label = {
                            Text(
                                text = payment,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

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

            // Receipts List
            if (filteredReceipts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ရှင်းပြီးပြေစာမှတ်တမ်း မရှိသေးပါ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "အရောင်းမှတ်တမ်းတွင် ပြေစာအား \"ရှင်းမည်/သိမ်းမည်\" ဖြင့် ဤနေရာသို့ သိမ်းဆည်းနိုင်ပါသည်",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                val receiptCalculations = remember(filteredReceipts, products, shopProfile) {
                    filteredReceipts.map { receipt ->
                        ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    receiptCalculations.forEach { calc ->
                        androidx.compose.runtime.key(calc.id) {
                            val matchingReceipt = filteredReceipts.find { it.id == calc.id }
                            ReceiptCalculationCard(
                                calculation = calc,
                                displayCurrency = if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.THB) "THB" else if (selectedCurrencyTab == SavedReceiptsCurrencyFilter.MMK) "MMK" else "BOTH",
                                onClick = {
                                    if (matchingReceipt != null) {
                                        selectedSavedReceiptForDetail = matchingReceipt
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
    }

    // Detail Dialog for Cleared / Archived Receipt
    selectedSavedReceiptForDetail?.let { receipt ->
        val detailCalc = remember(receipt, products, shopProfile) {
            ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
        }
        val formattedDate = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(receipt.originalTimestamp))
        val clearedDate = if (receipt.clearedTimestamp > 0) {
            SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(receipt.clearedTimestamp))
        } else null

        AlertDialog(
            onDismissRequest = { selectedSavedReceiptForDetail = null },
            title = {
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
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ရှင်းပြီးပြေစာ အသေးစိတ်",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = "ဘောက်ချာ: ${receipt.invoiceNumber}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.widthIn(max = 90.dp)
                    ) {
                        Text(
                            text = "ရှင်းပြီးသိမ်းဆည်းထားသည်",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .basicMarquee()
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "အရောင်းနေ့စွဲ: $formattedDate",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                    )
                    if (clearedDate != null) {
                        Text(
                            text = "ရှင်းလင်းသိမ်းဆည်းချိန်: $clearedDate",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                    }
                    if (receipt.customerName.isNotBlank()) {
                        Text(
                            text = "ဝယ်ယူသူ: ${receipt.customerName} ${if (receipt.customerPhone.isNotBlank()) "(${receipt.customerPhone})" else ""}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    val isThbPayment = com.example.data.isThbCurrencyString(receipt.currency)
                    val mmkColor = if (isDark) CashGreenDark else CashGreen
                    val thbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)

                    if (detailCalc.discountMmk > 0L || detailCalc.discountThb > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "လျှော့ဈေး:",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .basicMarquee()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isThbPayment) {
                                    "-${PosViewModel.formatCurrency(detailCalc.discountThb, "THB")} (= -${PosViewModel.formatKyatMm(detailCalc.discountMmk)})"
                                } else {
                                    "-${PosViewModel.formatKyatMm(detailCalc.discountMmk)} (= -${PosViewModel.formatCurrency(detailCalc.discountThb, "THB")})"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isThbPayment) "စုစုပေါင်း ရောင်းရငွေ (ဘတ်):" else "စုစုပေါင်း ရောင်းရငွေ (ကျပ်):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isThbPayment) {
                                PosViewModel.formatCurrency(detailCalc.sellingPriceThb, "THB")
                            } else {
                                PosViewModel.formatKyatMm(detailCalc.sellingPriceMmk)
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isThbPayment) thbColor else mmkColor,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isThbPayment) "ကျပ်ငွေဖြင့် ညီမျှငွေ:" else "ဘတ်ငွေဖြင့် ညီမျှငွေ:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isThbPayment) {
                                PosViewModel.formatKyatMm(detailCalc.sellingPriceMmk)
                            } else {
                                PosViewModel.formatCurrency(detailCalc.sellingPriceThb, "THB")
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isThbPayment) mmkColor else thbColor,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ခန့်မှန်း အသားတင်အမြတ်:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isThbPayment) {
                                "+${PosViewModel.formatCurrency(detailCalc.estimatedNetProfitThb, "THB")} (= +${PosViewModel.formatKyatMm(detailCalc.estimatedNetProfitMmk)})"
                            } else {
                                "+${PosViewModel.formatKyatMm(detailCalc.estimatedNetProfitMmk)} (= +${PosViewModel.formatCurrency(detailCalc.estimatedNetProfitThb, "THB")})"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = mmkColor,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ငွေပေးချေမှု:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${receipt.paymentMethod.labelMm} (${if (isThbPayment) "ဘတ်" else "ကျပ်"})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "လုပ်ဆောင်ချက် ရွေးချယ်ရန်:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                    )

                    // Action buttons row inside Dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Full Receipt View button (opens general ReceiptDialog)
                        Button(
                            onClick = {
                                viewModel.showSavedReceipt(receipt)
                                selectedSavedReceiptForDetail = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "ပြေစာ အပြည့်အစုံ",
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }

                        // Restore button
                        OutlinedButton(
                            onClick = {
                                receiptToRestore = receipt
                                selectedSavedReceiptForDetail = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ပြန်လည်ရွှေ့မည်",
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }

                    // Delete single receipt button
                    TextButton(
                        onClick = {
                            receiptToDelete = receipt
                            selectedSavedReceiptForDetail = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ဤရှင်းပြီးပြေစာအား မှတ်တမ်းမှ အပြီးတိုင် ဖျက်မည်",
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedSavedReceiptForDetail = null }) {
                    Text("ပိတ်မည်")
                }
            }
        )
    }

    // Restore Confirm Dialog
    receiptToRestore?.let { receipt ->
        AlertDialog(
            onDismissRequest = { receiptToRestore = null },
            title = {
                Text(
                    text = "အရောင်းမှတ်တမ်းသို့ ပြန်လည်ရွှေ့မည်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "ဘောက်ချာနံပါတ် (${receipt.invoiceNumber}) အား \"ရှင်းပြီးပြေစာများ\" မှ မူလလက်ရှိ \"အရောင်းမှတ်တမ်း\" သို့ ပြန်လည်ရွှေ့ပြောင်းလိုပါသလား?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreSavedReceipt(receipt.id)
                        receiptToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("ပြန်လည်ရွှေ့မည်", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptToRestore = null }) {
                    Text("မလုပ်တော့ပါ")
                }
            }
        )
    }

    // Delete Single Receipt Confirm Dialog
    receiptToDelete?.let { receipt ->
        AlertDialog(
            onDismissRequest = { receiptToDelete = null },
            title = {
                Text(
                    text = "ရှင်းပြီးပြေစာ ဖျက်မည်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "ဘောက်ချာနံပါတ် (${receipt.invoiceNumber}) အား ရှင်းပြီးမှတ်တမ်းမှ အပြီးတိုင် ဖျက်ပစ်ရန် သေချာပါသလား?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSavedReceipt(receipt.id)
                        receiptToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ဖျက်မည်", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptToDelete = null }) {
                    Text("မလုပ်တော့ပါ")
                }
            }
        )
    }

    // Delete All Confirm Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { dismissDeleteAllDialog() },
            title = {
                Text(
                    text = "ရှင်းပြီးပြေစာများ အားလုံး ဖျက်မည်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "သိမ်းဆည်းထားသော ရှင်းပြီးပြေစာ အားလုံး (${savedReceipts.size} စောင်) ကို အပြီးတိုင် ဖျက်ပစ်ရန် သေချာပါသလား?\n(ဤလုပ်ဆောင်ချက်ကို ပြန်လည်ပြင်ဆင်၍ မရနိုင်ပါ)",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllSavedReceipts()
                        dismissDeleteAllDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("အားလုံးဖျက်မည်", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissDeleteAllDialog() }) {
                    Text("မလုပ်တော့ပါ")
                }
            }
        )
    }
}

