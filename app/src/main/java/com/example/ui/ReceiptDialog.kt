package com.example.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.OrderDetails
import com.example.data.ShopProfile
import com.example.data.entity.OrderStatus
import com.example.data.entity.PaymentMethod
import com.example.data.entity.Product
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenDark
import com.example.ui.theme.KPayBlue
import com.example.ui.theme.KPayBlueDark
import com.example.ui.theme.PaymentColors
import com.example.ui.theme.NaturalBorderLight
import com.example.ui.theme.NaturalPositiveGreen
import com.example.ui.theme.NaturalPrimary
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import com.example.ui.theme.WavePayYellow
import com.example.util.ReceiptPrinter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptDialog(
    orderDetails: OrderDetails,
    shopProfile: ShopProfile,
    products: List<Product> = emptyList(),
    showCheckButton: Boolean = true,
    onDismiss: () -> Unit,
    onRefund: ((Long) -> Unit)? = null,
    onUndoRefund: ((Long) -> Unit)? = null,
    onDelete: ((Long) -> Unit)? = null,
    onClearAndSave: ((OrderDetails) -> Unit)? = null
) {
    val context = LocalContext.current
    val order = orderDetails.order
    val items = orderDetails.items
    val isRefunded = order.status == OrderStatus.REFUNDED
    val isDark = isSystemInDarkTheme()

    var showRefundConfirmDialog by remember { mutableStateOf(false) }
    var showUndoRefundConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(order.timestamp))

    // Active displayed currency mode for the receipt slip ("MMK" or "THB")
    // Initializes with the actual checkout currency chosen during sale
    var displayCurrency by remember(order.id, order.currency) { mutableStateOf(order.currency) }
    var showCurrencyDetails by remember(order.id) { mutableStateOf(false) }
    val isThbReceipt = displayCurrency == "THB"

    val thbSellFromShop = shopProfile.thbSellingRate.replace(",", ".").toDoubleOrNull() ?: 0.0
    val thbBuyFromShop = shopProfile.thbBuyingRate.replace(",", ".").toDoubleOrNull() ?: thbSellFromShop
    val mmkSellFromShop = shopProfile.mmkSellingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbSellFromShop > 0) 1.0 / thbSellFromShop else 0.0)
    val mmkBuyFromShop = shopProfile.mmkBuyingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbBuyFromShop > 0) 1.0 / thbBuyFromShop else mmkSellFromShop)

    val fallbackThbToMmk = if (isThbReceipt) {
        if (mmkBuyFromShop > 0.0) com.example.data.toThbToMmkRate(mmkBuyFromShop) else if (thbBuyFromShop > 0.0) thbBuyFromShop else 129.43
    } else {
        if (mmkSellFromShop > 0.0) com.example.data.toThbToMmkRate(mmkSellFromShop) else if (thbSellFromShop > 0.0) thbSellFromShop else 129.43
    }
    val rate = if (order.exchangeRate > 0) com.example.data.toThbToMmkRate(order.exchangeRate) else fallbackThbToMmk

    val estimatedProfit = remember(orderDetails, products, rate) {
        val productCostMap = products.associate { it.id to it.costPrice }
        val totalRawProfit = items.sumOf { item ->
            val costPrice = if (item.costPrice > 0L) item.costPrice else (productCostMap[item.productId] ?: 0L)
            val unitProfit = item.unitPrice - costPrice
            val isThbItem = com.example.data.isThbCurrencyString(item.currency) || com.example.data.isThbCurrencyString(item.productName)
            
            val itemProfitMmk = if (isThbItem) {
                Math.round(unitProfit * rate)
            } else {
                unitProfit
            }
            itemProfitMmk * item.quantity
        }
        val orderDiscountMmk = order.getEffectiveDiscountMmk()
        val netProfitMmk = (totalRawProfit - orderDiscountMmk).coerceAtLeast(0L)
        netProfitMmk
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("receipt_dialog_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isRefunded) Icons.Default.KeyboardReturn else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isRefunded) MaterialTheme.colorScheme.error else (if (isSystemInDarkTheme()) CashGreenDark else CashGreen),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRefunded) "ငွေပြန်အမ်းပြီး ပြေစာ" else "အရောင်းပြေစာ (Receipt)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRefunded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("receipt_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Currency View Switcher (ကျပ်ငွေ / ဘတ်ငွေ ပြောင်းလဲကြည့်ရှုရန်) - Controlled by Toggle
                if (showCurrencyDetails) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // MMK Option
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { displayCurrency = "MMK" }
                                    .testTag("receipt_view_currency_mmk"),
                                color = if (!isThbReceipt) CashGreen else Color.Transparent,
                                shape = RoundedCornerShape(9.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🇲🇲 ကျပ်ငွေဖြင့် ပြသမည်",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = if (!isThbReceipt) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // THB Option
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { displayCurrency = "THB" }
                                    .testTag("receipt_view_currency_thb"),
                                color = if (isThbReceipt) (if (isDark) Color(0xFF2563EB) else Color(0xFF0066B3)) else Color.Transparent,
                                shape = RoundedCornerShape(9.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🇹🇭 ဘတ်ငွေဖြင့် ပြသမည်",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = if (isThbReceipt) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Thermal Slip Paper styling
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Shop Name
                        Text(
                            text = if (shopProfile.shopName.isNotBlank()) shopProfile.shopName else "အရောင်းပြေစာ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        if (shopProfile.englishName.isNotBlank()) {
                            Text(
                                text = shopProfile.englishName,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (shopProfile.address.isNotBlank()) {
                            Text(
                                text = shopProfile.address,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        if (shopProfile.phone.isNotBlank()) {
                            Text(
                                text = "ဖုန်း - ${shopProfile.phone}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Invoice & Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ဘောက်ချာ : ${order.invoiceNumber}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "နေ့စွဲ : $formattedDate",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ကောင်တာ : ${if (shopProfile.cashierName.isNotBlank()) shopProfile.cashierName else "Cashier"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (order.customerName.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ဝယ်ယူသူ : ${order.customerName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (order.customerPhone.isNotBlank()) {
                                    Text(
                                        text = order.customerPhone,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Items Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ပစ္စည်းအမည်",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "အရေအတွက်",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(64.dp)
                            )
                            Text(
                                text = if (isThbReceipt) "ကျသင့်ငွေ (ဘတ်)" else "ကျသင့်ငွေ (ကျပ်)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(80.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Items Rows
                        items.forEach { item ->
                            val isThbItem = com.example.data.isThbCurrencyString(item.currency) || com.example.data.isThbCurrencyString(item.productName)
                            val unitPriceFormatted: String
                            val subtotalFormatted: String
                            if (isThbReceipt) {
                                val uPrice = if (!isThbItem) (if (rate > 0) Math.round(item.unitPrice / rate) else 0L) else item.unitPrice
                                val sTotal = if (!isThbItem) (if (rate > 0) Math.round(item.subtotal / rate) else 0L) else item.subtotal
                                unitPriceFormatted = PosViewModel.formatCurrency(uPrice, "THB")
                                subtotalFormatted = PosViewModel.formatCurrency(sTotal, "THB")
                            } else {
                                val uPrice = if (isThbItem) Math.round(item.unitPrice * rate) else item.unitPrice
                                val sTotal = if (isThbItem) Math.round(item.subtotal * rate) else item.subtotal
                                unitPriceFormatted = PosViewModel.formatKyatMm(uPrice)
                                subtotalFormatted = PosViewModel.formatKyatMm(sTotal)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "@ $unitPriceFormatted",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "x ${item.quantity}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(64.dp)
                                )
                                Text(
                                    text = subtotalFormatted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isThbReceipt) (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)) else (if (isDark) CashGreenDark else Color(0xFF00602F)),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(80.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Summary Calculations
                        val effSubtotal = if (isThbReceipt) order.getEffectiveSubtotalThb() else order.getEffectiveSubtotalMmk()
                        val subtotalFormatted = if (isThbReceipt) {
                            PosViewModel.formatCurrency(effSubtotal, "THB")
                        } else {
                            PosViewModel.formatKyatMm(effSubtotal)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isThbReceipt) "စုစုပေါင်း ကုန်ကျငွေ (ဘတ်) :" else "စုစုပေါင်း ကုန်ကျငွေ (ကျပ်) :",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(text = subtotalFormatted, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        // Discount: ONLY show if order.discount > 0 (Manual discount applied)
                        if (order.discount > 0) {
                            val effDiscount = if (isThbReceipt) order.getEffectiveDiscountThb() else order.getEffectiveDiscountMmk()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isThbReceipt) "အထူးလျှော့စျေး (ဘတ်) :" else "အထူးလျှော့စျေး (ကျပ်) :",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                val discText = if (isThbReceipt) {
                                    "- ${PosViewModel.formatCurrency(effDiscount, "THB")}"
                                } else {
                                    "- ${PosViewModel.formatKyatMm(effDiscount)}"
                                }
                                Text(text = discText, fontSize = 13.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Grand Total in displayed currency
                        val effGrandTotal = if (isThbReceipt) order.getEffectiveTotalThb() else order.getEffectiveTotalMmk()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isThbReceipt) "အသားတင် ကျသင့်ငွေ (ဘတ်) :" else "အသားတင် ကျသင့်ငွေ (ကျပ်) :",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val grandTotalFormatted = if (isThbReceipt) {
                                PosViewModel.formatCurrency(effGrandTotal, "THB")
                            } else {
                                PosViewModel.formatKyatMm(effGrandTotal)
                            }
                            val grandTotalColor = if (isThbReceipt) (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)) else (if (isDark) CashGreenDark else CashGreen)
                            Text(
                                text = grandTotalFormatted,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = grandTotalColor
                            )
                        }

                        // Dual Currency Breakdown Box (MMK & THB) - Controlled by Toggle
                        if (showCurrencyDetails) {
                            val displayMmk = order.getEffectiveTotalMmk()
                            val displayThb = order.getEffectiveTotalThb()
                            val isDark = isSystemInDarkTheme()
                            val adaptiveThbBlue = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                            val adaptiveMmkGreen = if (isDark) CashGreenDark else CashGreen

                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    if (isThbReceipt) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "🇹🇭 ထိုင်းငွေ စုစုပေါင်း :",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = PosViewModel.formatCurrency(displayThb, "THB"),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = adaptiveThbBlue
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "🇲🇲 မြန်မာငွေဖြင့် ညီမျှငွေ :",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = PosViewModel.formatKyatMm(displayMmk),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = adaptiveMmkGreen
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "🇲🇲 မြန်မာငွေ စုစုပေါင်း :",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = PosViewModel.formatKyatMm(displayMmk),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = adaptiveMmkGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "🇹🇭 ထိုင်းငွေဖြင့် ညီမျှငွေ :",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = PosViewModel.formatCurrency(displayThb, "THB"),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = adaptiveThbBlue
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))

                                    // ခန့်မှန်း အသားတင်အမြတ် Display
                                    val grandTotalActive = if (isThbReceipt) order.getEffectiveTotalThb() else order.getEffectiveTotalMmk()
                                    val profitActive = if (isThbReceipt) {
                                        if (rate > 0) Math.round(estimatedProfit.toDouble() / rate) else 0L
                                    } else {
                                        estimatedProfit
                                    }
                                    val profitMarginPercent = if (grandTotalActive > 0) {
                                        ((profitActive.toDouble() / grandTotalActive) * 100).toInt()
                                    } else {
                                        0
                                    }
                                    val profitBadgeColor = if (isThbReceipt) (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)) else (if (isDark) CashGreenDark else Color(0xFF047857))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ခန့်မှန်း အသားတင်အမြတ် :",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Text(
                                                text = "($profitMarginPercent%) +${PosViewModel.formatCurrency(profitActive, displayCurrency)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = profitBadgeColor,
                                                maxLines = 1,
                                                modifier = Modifier.basicMarquee()
                                            )
                                        }
                                    }

                                    // ပြေစာတစ်စောင်ချင်းစီတွင် မှတ်တမ်းတင်ထားသည့် ငွေလဲနှုန်း အတိအကျ
                                    val activeThbToMmk = if (order.exchangeRate > 0) com.example.data.toThbToMmkRate(order.exchangeRate) else rate
                                    if (activeThbToMmk > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))

                                        val bahtAmount = if (order.exchangeRate > 0) {
                                            com.example.data.toMmkToThbRate(order.exchangeRate)
                                        } else {
                                            1.0 / activeThbToMmk
                                        }
                                        val bahtAmountStr = if (order.exchangeRate > 0) {
                                            val formatted = String.format(Locale.US, "%.6f", com.example.data.toMmkToThbRate(order.exchangeRate))
                                            formatted.dropLastWhile { it == '0' }.let {
                                                if (it.endsWith(".")) it.dropLast(1) else it
                                            }
                                        } else if (bahtAmount > 0) {
                                            val formatted = String.format(Locale.US, "%.6f", bahtAmount)
                                            formatted.dropLastWhile { it == '0' }.let {
                                                if (it.endsWith(".")) it.dropLast(1) else it
                                            }
                                        } else {
                                            "0"
                                        }
                                        val label = "ပြေစာငွေလဲနှုန်း (၁ ကျပ်လျှင်) :"
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Text(
                                                    text = "$bahtAmountStr ဘတ်",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isThbReceipt) (if (isDark) KPayBlueDark else Color(0xFF2563EB)) else (if (isDark) CashGreenDark else CashGreen),
                                                    maxLines = 1,
                                                    modifier = Modifier.basicMarquee()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Payment Details
                        val payBadgeBg = PaymentColors.getContainerColor(order.paymentMethod, isDark)
                        val payBadgeText = PaymentColors.getTextColor(order.paymentMethod, isDark)
                        val payBrandBorder = PaymentColors.getBrandColor(order.paymentMethod, isDark).copy(alpha = 0.4f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, payBrandBorder, RoundedCornerShape(8.dp))
                                .background(payBadgeBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ငွေပေးချေမှုစနစ် :",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = PaymentColors.getIcon(order.paymentMethod),
                                    contentDescription = null,
                                    tint = payBadgeText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${order.paymentMethod.labelMm} (${order.paymentMethod.labelEn}) - ${if (isThbReceipt) "ဘတ်" else "ကျပ်"}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = payBadgeText
                                )
                            }
                        }

                        if (order.paymentMethod == PaymentMethod.CASH) {
                            val effTendered = if (isThbReceipt) order.getEffectiveTenderedThb() else order.getEffectiveTenderedMmk()
                            val effChange = if (isThbReceipt) order.getEffectiveChangeThb() else order.getEffectiveChangeMmk()
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "လက်ခံရရှိငွေ :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val tenderedText = if (isThbReceipt) PosViewModel.formatCurrency(effTendered, "THB") else PosViewModel.formatKyatMm(effTendered)
                                Text(text = tenderedText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "ပြန်အမ်းငွေ :", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                val changeText = if (isThbReceipt) PosViewModel.formatCurrency(effChange, "THB") else PosViewModel.formatKyatMm(effChange)
                                Text(
                                    text = changeText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Barcode Simulation
                        Row(
                            modifier = Modifier
                                .height(28.dp)
                                .fillMaxWidth(0.7f)
                                .background(Color.Transparent),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bars = listOf(3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 3, 1, 2, 4, 1, 3, 2, 4, 2)
                            bars.forEachIndexed { index, width ->
                                Box(
                                    modifier = Modifier
                                        .height(26.dp)
                                        .width(width.dp)
                                        .background(if (index % 2 == 0) MaterialTheme.colorScheme.onSurface else Color.Transparent)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                        }

                        Text(
                            text = order.invoiceNumber,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (shopProfile.receiptFooter.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = shopProfile.receiptFooter,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "ဝယ်ယူအားပေးမှုကို ကျေးဇူးတင်ပါသည်",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Actions (Icon only, equal size 52.dp x 52.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Currency Details Toggle Button (Default: Hide = false)
                    OutlinedIconButton(
                        onClick = { showCurrencyDetails = !showCurrencyDetails },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (showCurrencyDetails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            containerColor = if (showCurrencyDetails) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                            contentColor = if (showCurrencyDetails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("receipt_toggle_details_button")
                    ) {
                        Icon(
                            imageVector = if (showCurrencyDetails) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showCurrencyDetails) "ငွေကြေးအသေးစိတ် ဝှက်မည်" else "ငွေကြေးအသေးစိတ် ပြသမည်",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Share Button
                    OutlinedIconButton(
                        onClick = {
                            val shopHeader = if (shopProfile.shopName.isNotBlank()) "🧾 ${shopProfile.shopName}\n" else "🧾 အရောင်းပြေစာ\n"
                            val footerText = if (shopProfile.receiptFooter.isNotBlank()) shopProfile.receiptFooter else "ကျေးဇူးတင်ပါသည်!"
                            val totalText = if (order.currency == "THB") {
                                "${PosViewModel.formatCurrency(order.grandTotal, "THB")} (≈ ${PosViewModel.formatKyatMm(order.getEffectiveTotalMmk())})"
                            } else {
                                "${PosViewModel.formatKyatMm(order.grandTotal)} (≈ ${PosViewModel.formatCurrency(order.getEffectiveTotalThb(), "THB")})"
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "$shopHeader" +
                                            "ဘောက်ချာ: ${order.invoiceNumber}\n" +
                                            "ကျသင့်ငွေ: $totalText\n" +
                                            "ငွေပေးချေမှု: ${order.paymentMethod.labelMm} (${if (order.currency == "THB") "ဘတ်" else "ကျပ်"})\n" +
                                            "$footerText"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "ပြေစာမျှဝေမည်"))
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("receipt_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "မျှဝေမည်",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Refund / Undo-Refund / Delete Actions
                    if (isRefunded) {
                        // Undo Refund / Restore Button (မူလအတိုင်း ပြန်ထားမည် / ပြန်လည်အတည်ပြုမည်)
                        if (onUndoRefund != null) {
                            OutlinedIconButton(
                                onClick = { showUndoRefundConfirmDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("receipt_undo_refund_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsBackupRestore,
                                    contentDescription = "မူလအတိုင်း ပြန်ထားမည်",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Delete Button (ဖျက်မည်)
                        if (onDelete != null) {
                            OutlinedIconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("receipt_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "ဖျက်မည်",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else if (!isRefunded && onRefund != null) {
                        // Refund Button (ငွေပြန်အမ်းရန်)
                        OutlinedIconButton(
                            onClick = { showRefundConfirmDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("receipt_refund_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardReturn,
                                contentDescription = "ငွေပြန်အမ်း",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Check / Confirm / Done Button (✅ ရှင်းရန် / ဖျက်ပြီး မျက်နှာပြင်သစ် (၁) တွင် သိမ်းဆည်းရန် ခလုတ်)
                    if (showCheckButton && !isRefunded && onClearAndSave != null) {
                        FilledIconButton(
                            onClick = {
                                val productCostMap = products.associate { it.id to it.costPrice }
                                val updatedItems = orderDetails.items.map { item ->
                                    if (item.costPrice <= 0L) {
                                        val cost = productCostMap[item.productId] ?: 0L
                                        item.copy(costPrice = cost)
                                    } else {
                                        item
                                    }
                                }
                                val updatedOrder = orderDetails.order.copy(currency = displayCurrency)
                                onClearAndSave(orderDetails.copy(order = updatedOrder, items = updatedItems))
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("receipt_done_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "ရှင်းမည် / ရှင်းပြီး ပြေစာများ တွင် သိမ်းမည်",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Print Button (Rightmost - Swapped with Toggle Button)
                    FilledTonalIconButton(
                        onClick = {
                            ReceiptPrinter.printReceipt(
                                context = context,
                                orderDetails = orderDetails,
                                shopProfile = shopProfile,
                                displayCurrency = displayCurrency,
                                showCurrencyDetails = showCurrencyDetails
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("receipt_print_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "ပရင့်ထုတ်မည်",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Refund Confirmation Dialog
        if (showRefundConfirmDialog && onRefund != null) {
            AlertDialog(
                onDismissRequest = { showRefundConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.KeyboardReturn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ငွေပြန်အမ်းရန် အတည်ပြုပါ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Text(
                        text = "ဘောက်ချာနံပါတ် (${order.invoiceNumber}) အား ငွေပြန်အမ်းရန် ပြုလုပ်လိုပါသလား?\n\n" +
                                "• ကုန်ပစ္စည်းအရေအတွက်များကို စာရင်းလက်ကျန်ထဲသို့ ပြန်လည်ဖြည့်သွင်းပေးမည် ဖြစ်ပါသည်။\n" +
                                "• ငွေပြန်အမ်းထားသော စာရင်းကို ဆိုင်းငံ့ထားသော အော်ဒါများ (Held Orders) ထဲတွင် ဆိုင်းငံ့ထားသော အော်ဒါကဲ့သို့ မှတ်သားထားပေးပါမည်။\n" +
                                "• မှားယွင်းနှိပ်မိပါက 'မူလအတိုင်းပြန်ထားမည်' ခလုတ်ဖြင့် မူလအတိုင်း အချိန်မရွေး ပြန်လည်ထားရှိနိုင်ပါသည်။",
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRefundConfirmDialog = false
                            onRefund(order.id)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("ငွေပြန်အမ်းမည်", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRefundConfirmDialog = false }) {
                        Text("မလုပ်တော့ပါ")
                    }
                }
            )
        }

        // Undo Refund Confirmation Dialog
        if (showUndoRefundConfirmDialog && onUndoRefund != null) {
            AlertDialog(
                onDismissRequest = { showUndoRefundConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsBackupRestore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "မူလအတိုင်း ပြန်ထားမည်",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Text(
                        text = "ငွေပြန်အမ်းထားသော ဘောက်ချာနံပါတ် (${order.invoiceNumber}) အား မူလအရောင်းမှတ်တမ်း (အတည်ပြုပြီး ✓) အဖြစ် ပြန်လည်ထားရှိလိုပါသလား?\n\n" +
                                "• ကုန်ပစ္စည်းလက်ကျန်များကို မူလအရောင်းအတိုင်း ပြန်လည်နုတ်ယူပေးမည် ဖြစ်ပါသည်။\n" +
                                "• အရောင်းမှတ်တမ်း စာရင်းချုပ်များတွင် ပုံမှန်အရောင်းအဖြစ် ပြန်လည်ထည့်သွင်းတွက်ချက်ပေးပါမည်။",
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUndoRefundConfirmDialog = false
                            onUndoRefund(order.id)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("မူလအတိုင်းပြန်ထားမည်", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUndoRefundConfirmDialog = false }) {
                        Text("မလုပ်တော့ပါ")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog && onDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ဘောက်ချာ ဖျက်မည်",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Text(
                        text = "ဘောက်ချာနံပါတ် (${order.invoiceNumber}) အား စနစ်ထဲမှ အပြီးတိုင် ဖျက်ပစ်လိုပါသလား?",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDelete(order.id)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("ဖျက်မည်", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("မလုပ်တော့ပါ")
                    }
                }
            )
        }
    }
}
