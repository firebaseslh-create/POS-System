package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CartItem
import com.example.data.entity.PaymentMethod
import com.example.ui.theme.AyaPayRed
import com.example.ui.theme.AyaPayRedDark
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenDark
import com.example.ui.theme.KPayBlue
import com.example.ui.theme.KPayBlueDark
import com.example.ui.theme.PaymentColors
import com.example.ui.theme.WavePayYellow
import com.example.ui.theme.WavePayYellowDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSheet(
    cart: List<CartItem>,
    subtotal: Long,
    thbSellingRate: String = "",
    thbBuyingRate: String = "",
    mmkSellingRate: String = "",
    mmkBuyingRate: String = "",
    sheetState: SheetState,
    showDetails: Boolean = true,
    showProfitInfo: Boolean = true,
    onDismiss: () -> Unit,
    onConfirmCheckout: (
        discount: Long,
        paymentMethod: PaymentMethod,
        amountTendered: Long,
        customerName: String,
        customerPhone: String,
        note: String,
        currency: String,
        totalMmk: Long,
        totalThb: Long,
        rateUsed: Double
    ) -> Unit
) {
    // Parse exchange rates from settings
    val thbSell = remember(thbSellingRate) {
        val r = thbSellingRate.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (r > 0.0) r else 0.0
    }
    val thbBuy = remember(thbBuyingRate, thbSell) {
        val r = thbBuyingRate.replace(",", ".").toDoubleOrNull() ?: thbSell
        if (r > 0.0) r else thbSell
    }

    // မြန်မာကျပ်ရောင်းဈေး (1 MMK = X THB, or derived from thbSellingRate)
    val mmkSell = remember(mmkSellingRate, thbSell) {
        val r = mmkSellingRate.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (r > 0.0) r else (if (thbSell > 0.0) 1.0 / thbSell else 0.0)
    }
    // မြန်မာကျပ်ဝယ်ဈေး (1 MMK = X THB, or derived from thbBuyingRate)
    val mmkBuy = remember(mmkBuyingRate, thbBuy, mmkSell) {
        val r = mmkBuyingRate.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (r > 0.0) r else (if (thbBuy > 0.0) 1.0 / thbBuy else mmkSell)
    }

    // Effective THB -> MMK rate when MMK (ကျပ်) checkout is selected (uses မြန်မာကျပ်ရောင်းဈေး)
    val sellRate = remember(mmkSell, thbSell) {
        if (mmkSell > 0.0) (if (mmkSell < 1.0) 1.0 / mmkSell else mmkSell)
        else if (thbSell > 0.0) thbSell
        else 0.0
    }
    // Effective THB -> MMK rate when THB (ဘတ်) checkout is selected (uses မြန်မာကျပ်ဝယ်ဈေး)
    val buyRate = remember(mmkBuy, thbBuy, sellRate) {
        if (mmkBuy > 0.0) (if (mmkBuy < 1.0) 1.0 / mmkBuy else mmkBuy)
        else if (thbBuy > 0.0) thbBuy
        else sellRate
    }

    val mmkSellFormatted = remember(mmkSellingRate, mmkSell) {
        if (mmkSellingRate.isNotBlank()) mmkSellingRate
        else if (mmkSell > 0.0) {
            String.format(java.util.Locale.US, "%.6f", mmkSell).trimEnd('0').trimEnd('.')
        } else "0"
    }

    val mmkBuyFormatted = remember(mmkBuyingRate, mmkBuy) {
        if (mmkBuyingRate.isNotBlank()) mmkBuyingRate
        else if (mmkBuy > 0.0) {
            String.format(java.util.Locale.US, "%.6f", mmkBuy).trimEnd('0').trimEnd('.')
        } else "0"
    }

    // Check if any wholesale item exists in the cart
    val hasWholesaleItem = remember(cart) { cart.any { it.isWholesale } }

    val hasThbItems = remember(cart) { cart.any { it.isThbItem } }
    val hasMmkItems = remember(cart) { cart.any { it.isMmkItem } }
    val hasBothCurrencies = remember(hasThbItems, hasMmkItems) { hasThbItems && hasMmkItems }

    // Dual currency subtotal:
    // - When MMK (ကျပ်) is selected: THB items are converted using မြန်မာကျပ်ရောင်းဈေး (mmkSell / sellRate)
    // - When THB (ဘတ်) is selected: MMK items are converted using မြန်မာကျပ်ဝယ်ဈေး (mmkBuy / buyRate)
    val subtotalMmk = remember(cart, mmkSell, sellRate) {
        val rate = if (mmkSell > 0.0) mmkSell else sellRate
        cart.sumOf { it.getSubtotalMmk(rate) }
    }
    val subtotalThb = remember(cart, mmkBuy, buyRate) {
        val rate = if (mmkBuy > 0.0) mmkBuy else buyRate
        cart.sumOf { it.getSubtotalThb(rate) }
    }

    // Selected currency for checkout: "MMK" (ကျပ်) or "THB" (ဘတ်)
    // Prioritize the original currency of the first-selected product in the cart
    val initialCurrency = remember(cart) {
        if (cart.firstOrNull()?.isThbItem == true) "THB" else "MMK"
    }
    var selectedCurrency by remember { mutableStateOf(initialCurrency) }

    var discountText by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf(PaymentMethod.CASH) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var orderNote by remember { mutableStateOf("") }

    // Manual discount only allowed if NO wholesale items are in the cart
    val discount = if (showDetails && !hasWholesaleItem) (discountText.toLongOrNull() ?: 0L) else 0L

    // Active subtotal and active grandTotal based on selectedCurrency
    val activeSubtotal = if (selectedCurrency == "MMK") subtotalMmk else subtotalThb
    val activeGrandTotal = (activeSubtotal - discount).coerceAtLeast(0L)

    // Calculate the equivalent in the other currency directly using the active checkout's rate
    val equivalentGrandTotal = if (selectedCurrency == "MMK") {
        if (mmkSell > 0.0) Math.round(activeGrandTotal.toDouble() * mmkSell)
        else if (sellRate > 0) Math.round(activeGrandTotal.toDouble() / sellRate)
        else 0L
    } else {
        if (mmkBuy > 0.0) Math.round(activeGrandTotal.toDouble() / mmkBuy)
        else Math.round(activeGrandTotal.toDouble() * buyRate)
    }

    val finalTotalMmk = if (selectedCurrency == "MMK") activeGrandTotal else equivalentGrandTotal
    val finalTotalThb = if (selectedCurrency == "THB") activeGrandTotal else equivalentGrandTotal

    var amountTenderedText by remember { mutableStateOf(activeGrandTotal.toString()) }

    // Update tendered text whenever currency mode or grand total changes
    LaunchedEffect(selectedCurrency, activeGrandTotal) {
        amountTenderedText = activeGrandTotal.toString()
    }

    val amountTendered = amountTenderedText.toLongOrNull() ?: 0L
    val changeAmount = (amountTendered - activeGrandTotal).coerceAtLeast(0L)
    val equivalentChangeAmount = if (selectedCurrency == "MMK") {
        if (mmkSell > 0.0) Math.round(changeAmount.toDouble() * mmkSell)
        else if (sellRate > 0) Math.round(changeAmount.toDouble() / sellRate)
        else 0L
    } else {
        if (mmkBuy > 0.0) Math.round(changeAmount.toDouble() / mmkBuy)
        else Math.round(changeAmount.toDouble() * buyRate)
    }

    val effectivePayment = if (showDetails) selectedPayment else PaymentMethod.CASH
    val isInsufficient = effectivePayment == PaymentMethod.CASH && amountTendered < activeGrandTotal
    val isDark = isSystemInDarkTheme()
    val cashAccentColor = if (isDark) CashGreenDark else CashGreen

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(cashAccentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = cashAccentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ငွေရှင်းကောင်တာ (Checkout)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = "စုစုပေါင်း ${cart.sumOf { it.quantity }} ခု (ပစ္စည်း ${cart.size} မျိုး)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Currency Selector Tabs (MMK ကျပ် vs THB ဘတ်)
            Text(
                text = "ငွေရှင်းမည့် ငွေကြေးအမျိုးအစား ရွေးချယ်ပါ :",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // MMK Tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (selectedCurrency != "MMK") {
                                selectedCurrency = "MMK"
                                discountText = ""
                            }
                        }
                        .testTag("checkout_currency_mmk_tab"),
                    color = if (selectedCurrency == "MMK") (if (isDark) Color(0xFF059669) else CashGreen) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🇲🇲 မြန်မာငွေ (ကျပ်)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedCurrency == "MMK") Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }

                // THB Tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (selectedCurrency != "THB") {
                                selectedCurrency = "THB"
                                discountText = ""
                            }
                        }
                        .testTag("checkout_currency_thb_tab"),
                    color = if (selectedCurrency == "THB") (if (isDark) Color(0xFF2563EB) else Color(0xFF0066B3)) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🇹🇭 ထိုင်းငွေ (ဘတ်)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedCurrency == "THB") Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Exchange Rate Info Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    val activeRate = if (selectedCurrency == "MMK") sellRate else buyRate
                    val rateStr = if (activeRate % 1.0 == 0.0) activeRate.toLong().toString() else activeRate.toString()
                    val rateTypeLabel = if (selectedCurrency == "MMK") {
                        "မြန်မာကျပ်ရောင်းဈေး : $mmkSellFormatted ဘတ် (၁ ဘတ် = $rateStr Ks)"
                    } else {
                        "မြန်မာကျပ်ဝယ်ဈေး : $mmkBuyFormatted ဘတ် (၁ ဘတ် = $rateStr Ks)"
                    }
                    Text(
                        text = rateTypeLabel,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee()
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                val activeCurrColor = if (selectedCurrency == "MMK") {
                    if (isDark) CashGreenDark else Color(0xFF00602F)
                } else {
                    if (isDark) KPayBlueDark else Color(0xFF004C87)
                }
                Text(
                    text = if (selectedCurrency == "MMK") "ကျပ်ငွေ" else "ဘတ်ငွေ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeCurrColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val showConversionCard = showDetails && ((selectedCurrency == "MMK" && hasThbItems) || (selectedCurrency == "THB" && hasMmkItems) || hasBothCurrencies)
            if (showConversionCard) {
                val isDarkTheme = isSystemInDarkTheme()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("checkout_dual_currency_breakdown_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ပစ္စည်းစာရင်း (ငွေလဲလှယ်မှု တွက်ချက်ခြင်း)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .basicMarquee()
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedCurrency == "MMK") (if (isDarkTheme) Color(0xFF065F46) else Color(0xFFD1FAE5)) else (if (isDarkTheme) Color(0xFF1E40AF) else Color(0xFFDBEAFE))
                            ) {
                                Text(
                                    text = if (selectedCurrency == "MMK") "ကျပ်ငွေ (MMK) ဖြင့်ရှင်းမည်" else "ဘတ်ငွေ (THB) ဖြင့်ရှင်းမည်",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCurrency == "MMK") (if (isDarkTheme) Color(0xFF34D399) else Color(0xFF065F46)) else (if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1E40AF)),
                                    maxLines = 1,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .basicMarquee()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        cart.forEachIndexed { index, item ->
                            val isItemThb = item.isThbItem
                            val isItemMmk = item.isMmkItem
                            val itemCurrency = if (isItemThb) "THB" else "MMK"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .basicMarquee()
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isItemThb) Color(0xFFE1F0FF) else Color(0xFFE8F5E9)
                                        ) {
                                            Text(
                                                text = if (isItemThb) "🇹🇭 THB" else "🇲🇲 MMK",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isItemThb) Color(0xFF0066B3) else Color(0xFF00602F),
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "မူရင်း: ${PosViewModel.formatCurrencyShort(item.price, itemCurrency)} × ${item.quantity} = ${PosViewModel.formatCurrency(item.subtotal, itemCurrency)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    if (selectedCurrency == "MMK") {
                                        if (isItemThb) {
                                            // Convert THB to MMK using မြန်မာကျပ်ရောင်းဈေး (mmkSell / sellRate)
                                            val rateUsed = if (mmkSell > 0.0) mmkSell else sellRate
                                            val convertedSubtotal = item.getSubtotalMmk(rateUsed)
                                            Text(
                                                text = PosViewModel.formatKyatMm(convertedSubtotal),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isDarkTheme) CashGreenDark else Color(0xFF00602F),
                                                maxLines = 1,
                                                modifier = Modifier.basicMarquee()
                                            )
                                            Text(
                                                text = "(မြန်မာကျပ်ရောင်းဈေး $mmkSellFormatted ဘတ်)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                modifier = Modifier.basicMarquee()
                                            )
                                        } else {
                                            // Keep MMK as MMK
                                            Text(
                                                text = PosViewModel.formatKyatMm(item.subtotal),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) CashGreenDark else Color(0xFF00602F),
                                                maxLines = 1,
                                                modifier = Modifier.basicMarquee()
                                            )
                                        }
                                    } else { // THB selected
                                        if (isItemMmk) {
                                            // Convert MMK to THB using မြန်မာကျပ်ဝယ်ဈေး (mmkBuy / buyRate)
                                            val rateUsed = if (mmkBuy > 0.0) mmkBuy else buyRate
                                            val convertedSubtotal = item.getSubtotalThb(rateUsed)
                                            Text(
                                                text = PosViewModel.formatCurrency(convertedSubtotal, "THB"),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF0066B3),
                                                maxLines = 1,
                                                modifier = Modifier.basicMarquee()
                                            )
                                            Text(
                                                text = "(မြန်မာကျပ်ဝယ်ဈေး $mmkBuyFormatted ဘတ်)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                modifier = Modifier.basicMarquee()
                                            )
                                        } else {
                                            // Keep THB as THB
                                            Text(
                                                text = PosViewModel.formatCurrency(item.subtotal, "THB"),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF0066B3),
                                                maxLines = 1,
                                                modifier = Modifier.basicMarquee()
                                            )
                                        }
                                    }
                                }
                            }
                            if (index < cart.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Total Amount Card (Displays in active currency + conversion equivalent)
            val totalRawProfit = remember(cart, selectedCurrency, mmkSell, sellRate, mmkBuy, buyRate) {
                if (selectedCurrency == "MMK") {
                    val rate = if (mmkSell > 0.0) mmkSell else sellRate
                    cart.sumOf { it.getProfitMmk(rate) }
                } else {
                    val rate = if (mmkBuy > 0.0) mmkBuy else buyRate
                    cart.sumOf { it.getProfitThb(rate) }
                }
            }
            val netOrderProfit = (totalRawProfit - discount).coerceAtLeast(0L)
            val profitMarginPercent = if (activeGrandTotal > 0) ((netOrderProfit.toDouble() / activeGrandTotal) * 100).toInt() else 0
            val wholesaleCount = remember(cart) { cart.count { it.isWholesale } }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCurrency == "MMK") "ကျသင့်ငွေ (Subtotal - Ks)" else "ကျသင့်ငွေ (Subtotal - Baht)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = PosViewModel.formatCurrency(activeSubtotal, selectedCurrency),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            val altSubtotal = if (selectedCurrency == "MMK") subtotalThb else subtotalMmk
                            val altCurr = if (selectedCurrency == "MMK") "THB" else "MMK"
                            val altSubColor = if (altCurr == "THB") (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)) else (if (isDark) CashGreenDark else CashGreen)
                            Text(
                                text = "= ${PosViewModel.formatCurrency(altSubtotal, altCurr)}",
                                color = altSubColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }

                    if (discount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "လျှော့စျေး (Discount)",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .basicMarquee()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "- ${PosViewModel.formatCurrency(discount, selectedCurrency)}",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Net Grand Total Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "အသားတင် ကျသင့်ငွေ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            val eqColor = if (selectedCurrency == "MMK") (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)) else (if (isDark) CashGreenDark else CashGreen)
                            Text(
                                text = if (selectedCurrency == "MMK") "🇹🇭 ထိုင်းငွေ: = ${PosViewModel.formatCurrency(equivalentGrandTotal, "THB")}"
                                else "🇲🇲 မြန်မာငွေ: = ${PosViewModel.formatKyatMm(equivalentGrandTotal)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = eqColor,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val mainTotalColor = if (selectedCurrency == "THB") (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)) else (if (isDark) CashGreenDark else CashGreen)
                        Text(
                            text = PosViewModel.formatCurrency(activeGrandTotal, selectedCurrency),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = mainTotalColor,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    if (showProfitInfo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isThbSelected = selectedCurrency == "THB"
                        val profitBadgeColor = if (isThbSelected) {
                            if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                        } else {
                            if (isDark) CashGreenDark else Color(0xFF047857)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "ခန့်မှန်း အသားတင်အမြတ် :",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    if (wholesaleCount > 0) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "($wholesaleCount မျိုး လက်ကား)",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = profitBadgeColor,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "($profitMarginPercent%) +${PosViewModel.formatCurrency(netOrderProfit, selectedCurrency)}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = profitBadgeColor,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }

                            // Display the exchange rate used directly below Estimated Net Profit
                            val activeRateLabel = if (selectedCurrency == "MMK") "မြန်မာကျပ်ရောင်းဈေး :" else "မြန်မာကျပ်ဝယ်ဈေး :"
                            val activeRateValue = if (selectedCurrency == "MMK") mmkSellFormatted else mmkBuyFormatted
                            val rateAccentColor = if (selectedCurrency == "MMK") profitBadgeColor else (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeRateLabel,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .weight(1f)
                                        .basicMarquee()
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "၁ ကျပ် = $activeRateValue ဘတ်",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = rateAccentColor,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }
                }
            }

            if (showDetails) {
                Spacer(modifier = Modifier.height(16.dp))

                // Payment Methods
                Text(
                    text = "ငွေပေးချေမှု နည်းလမ်း ရွေးချယ်ပါ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethodButton(
                        title = "ငွေသား",
                        subtitle = "Cash",
                        isSelected = selectedPayment == PaymentMethod.CASH,
                        method = PaymentMethod.CASH,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedPayment = PaymentMethod.CASH
                            amountTenderedText = activeGrandTotal.toString()
                        }
                    )
                    PaymentMethodButton(
                        title = "KPay",
                        subtitle = "KBZPay",
                        isSelected = selectedPayment == PaymentMethod.KPAY,
                        method = PaymentMethod.KPAY,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedPayment = PaymentMethod.KPAY }
                    )
                    PaymentMethodButton(
                        title = "WavePay",
                        subtitle = "Wave",
                        isSelected = selectedPayment == PaymentMethod.WAVEPAY,
                        method = PaymentMethod.WAVEPAY,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedPayment = PaymentMethod.WAVEPAY }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethodButton(
                        title = "AYA Pay",
                        subtitle = "Mobile Banking",
                        isSelected = selectedPayment == PaymentMethod.AYAPAY,
                        method = PaymentMethod.AYAPAY,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedPayment = PaymentMethod.AYAPAY }
                    )
                    PaymentMethodButton(
                        title = "အကြွေး",
                        subtitle = "Credit / Due",
                        isSelected = selectedPayment == PaymentMethod.CREDIT,
                        method = PaymentMethod.CREDIT,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedPayment = PaymentMethod.CREDIT }
                    )
                }
            }

            // Cash Calculator Section (Visible when paying with Cash)
            if (effectivePayment == PaymentMethod.CASH) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ငွေသား လက်ခံရရှိမှု တွက်ချက်ခြင်း",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .basicMarquee()
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedCurrency == "MMK") "ကျပ် (Ks)" else "ဘတ် (Baht)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCurrency == "MMK") (if (isDark) CashGreenDark else Color(0xFF00602F)) else (if (isDark) KPayBlueDark else Color(0xFF004C87))
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = amountTenderedText,
                            onValueChange = { amountTenderedText = it },
                            label = {
                                Text(
                                    text = if (selectedCurrency == "MMK") "လက်ခံရရှိငွေ (Amount Received in Kyats)"
                                    else "လက်ခံရရှိငွေ (Amount Received in Baht)",
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tendered_amount_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick denomination chips according to selected currency
                        Text(
                            text = if (selectedCurrency == "MMK") "မြန်မာငွေ အမြန်ဖြည့်ရန် :" else "ထိုင်းငွေ အမြန်ဖြည့်ရန် :",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                QuickChip(label = "ကွက်တိ (${PosViewModel.formatCurrency(activeGrandTotal, selectedCurrency)})") {
                                    amountTenderedText = activeGrandTotal.toString()
                                }
                            }
                            if (selectedCurrency == "MMK") {
                                item {
                                    QuickChip(label = "+1,000 Ks") {
                                        val current = amountTenderedText.toLongOrNull() ?: 0L
                                        amountTenderedText = (current + 1000).toString()
                                    }
                                }
                                item {
                                    QuickChip(label = "+5,000 Ks") {
                                        val current = amountTenderedText.toLongOrNull() ?: 0L
                                        amountTenderedText = (current + 5000).toString()
                                    }
                                }
                                item {
                                    QuickChip(label = "+10,000 Ks") {
                                        val current = amountTenderedText.toLongOrNull() ?: 0L
                                        amountTenderedText = (current + 10000).toString()
                                    }
                                }
                                item {
                                    QuickChip(label = "+20,000 Ks") {
                                        val current = amountTenderedText.toLongOrNull() ?: 0L
                                        amountTenderedText = (current + 20000).toString()
                                    }
                                }
                                item {
                                    QuickChip(label = "+50,000 Ks") {
                                        val current = amountTenderedText.toLongOrNull() ?: 0L
                                        amountTenderedText = (current + 50000).toString()
                                    }
                                }
                            } else {
                                item {
                                    QuickChip(label = "+20 ฿") {
                                        val current = amountTenderedText.toLongOrNull() ?: 0L
                                        amountTenderedText = (current + 20).toString()
                                    }
                                }
                                item {
                                    QuickChip(label = "+50 ฿") {
                                        val current = amountTenderedText.toLongOrNull() ?: 0L
                                        amountTenderedText = (current + 50).toString()
                                    }
                                }
                                item {
                                    QuickChip(label = "+100 ฿") {
                                        val current = amountTenderedText.toLongOrNull() ?: 0L
                                        amountTenderedText = (current + 100).toString()
                                    }
                                }
                                item {
                                    QuickChip(label = "+500 ฿") {
                                        val current = amountTenderedText.toLongOrNull() ?: 0L
                                        amountTenderedText = (current + 500).toString()
                                    }
                                }
                                item {
                                    QuickChip(label = "+1,000 ฿") {
                                        val current = amountTenderedText.toLongOrNull() ?: 0L
                                        amountTenderedText = (current + 1000).toString()
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Change calculation display with dual currency equivalent
                        val cashGreenText = if (isDark) CashGreenDark else Color(0xFF00602F)
                        val cashBoxBg = if (isInsufficient) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            if (isDark) Color(0xFF0F3822) else Color(0xFFE8F5E9)
                        }
                        val cashBoxBorder = if (isInsufficient) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        } else {
                            (if (isDark) CashGreenDark else Color(0xFF00602F)).copy(alpha = 0.45f)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, cashBoxBorder, RoundedCornerShape(10.dp))
                                .background(cashBoxBg)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isInsufficient) "ငွေမလုံလောက်ပါ" else "ပြန်အမ်းငွေ (Change to return)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInsufficient) MaterialTheme.colorScheme.error else cashGreenText,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                if (!isInsufficient && changeAmount > 0) {
                                    val altCurr = if (selectedCurrency == "MMK") "THB" else "MMK"
                                    val altChangeColor = if (altCurr == "THB") (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)) else (if (isDark) CashGreenDark else CashGreen)
                                    Text(
                                        text = "= ${PosViewModel.formatCurrency(equivalentChangeAmount, altCurr)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isInsufficient) MaterialTheme.colorScheme.error else altChangeColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isInsufficient)
                                    "- ${PosViewModel.formatCurrency(activeGrandTotal - amountTendered, selectedCurrency)}"
                                else
                                    PosViewModel.formatCurrency(changeAmount, selectedCurrency),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isInsufficient) MaterialTheme.colorScheme.error else cashGreenText,
                                fontSize = 17.sp,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
            }

            // KPay or WavePay QR instruction info
            if (showDetails && (selectedPayment == PaymentMethod.KPAY || selectedPayment == PaymentMethod.WAVEPAY)) {
                Spacer(modifier = Modifier.height(14.dp))
                val isKPay = selectedPayment == PaymentMethod.KPAY
                val payInstructionColor = if (isKPay) {
                    if (isDark) KPayBlueDark else Color(0xFF004C87)
                } else {
                    if (isDark) WavePayYellowDark else Color(0xFF92400E)
                }
                val payInstructionBg = if (isKPay) {
                    if (isDark) Color(0xFF103355) else Color(0xFFE1F0FF)
                } else {
                    if (isDark) Color(0xFF452205) else Color(0xFFFEF3C7)
                }
                val payInstructionBorder = (if (isKPay) {
                    if (isDark) KPayBlueDark else Color(0xFF0066B3)
                } else {
                    if (isDark) WavePayYellowDark else Color(0xFFD97706)
                }).copy(alpha = 0.45f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, payInstructionBorder, RoundedCornerShape(10.dp))
                        .background(payInstructionBg)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = payInstructionColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedPayment.labelMm} QR စကင်ဖတ်ပြီး ငွေလွှဲလက်ခံပါ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = payInstructionColor,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = "ငွေလွှဲပြေစာ စစ်ဆေးပြီးပါက အောက်ပါခလုတ်ကို နှိပ်ပါ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }

            // Discount and Customer Info
            if (showDetails) {
                Spacer(modifier = Modifier.height(14.dp))

                if (hasWholesaleItem) {
                    // Wholesale is applied: lock manual discount and inform user
                    val wholesaleGreen = if (isDark) CashGreenDark else Color(0xFF00602F)
                    val wholesaleBg = if (isDark) Color(0xFF0F3822) else Color(0xFFE8F5E9)
                    val wholesaleBorder = wholesaleGreen.copy(alpha = 0.45f)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, wholesaleBorder, RoundedCornerShape(12.dp)),
                        color = wholesaleBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = wholesaleGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "လက်ကားဈေးနှုန်း သတ်မှတ်ထားပါသည်",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = wholesaleGreen,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = "လက်ကားဈေးပါဝင်သောကြောင့် အထူးလျှော့ဈေး (Discount) ထည့်ခွင့်မပြုပါ",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }
                } else {
                    // Regular selling price for all items: cashier can enter manual discount
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = {
                            Text(
                                text = if (selectedCurrency == "MMK") "အထူးလျှော့စျေး (Discount in Ks - optional)"
                                else "အထူးလျှော့စျေး (Discount in Baht - optional)",
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Discount, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discount_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Customer Name & Phone (Optional)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = {
                            Text(
                                text = "ဝယ်ယူသူ အမည်",
                                fontSize = 10.sp,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("customer_name_input")
                    )
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = {
                            Text(
                                text = "ဖုန်းနံပါတ်",
                                fontSize = 10.sp,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_phone_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm Button
            val confirmBtnColor = if (selectedCurrency == "MMK") {
                if (isDark) Color(0xFF059669) else CashGreen
            } else {
                if (isDark) Color(0xFF2563EB) else Color(0xFF0066B3)
            }
            Button(
                onClick = {
                    onConfirmCheckout(
                        if (showDetails) discount else 0L,
                        effectivePayment,
                        if (effectivePayment == PaymentMethod.CASH) amountTendered else activeGrandTotal,
                        if (showDetails) customerName else "",
                        if (showDetails) customerPhone else "",
                        if (showDetails) orderNote else "",
                        selectedCurrency,
                        finalTotalMmk,
                        finalTotalThb,
                        if (selectedCurrency == "MMK") sellRate else buyRate
                    )
                },
                enabled = !isInsufficient,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmBtnColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_checkout_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ငွေရှင်းမည် (${PosViewModel.formatCurrency(activeGrandTotal, selectedCurrency)}) [= ${PosViewModel.formatCurrency(equivalentGrandTotal, if (selectedCurrency == "MMK") "THB" else "MMK")}]",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaymentMethodButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    method: PaymentMethod,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val brandColor = PaymentColors.getBrandColor(method, isDark)
    val containerBg = if (isSelected) {
        PaymentColors.getContainerColor(method, isDark)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isSelected) {
        PaymentColors.getTextColor(method, isDark)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subTextColor = if (isSelected) {
        textColor.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconColor = if (isSelected) brandColor else brandColor.copy(alpha = 0.7f)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) brandColor else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        color = containerBg
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = PaymentColors.getIcon(method),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = textColor,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = subTextColor,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
