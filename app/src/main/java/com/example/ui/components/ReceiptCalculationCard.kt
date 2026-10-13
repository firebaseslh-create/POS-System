package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.OrderStatus
import com.example.ui.PosViewModel
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenDark
import com.example.ui.theme.PaymentColors
import com.example.util.ReceiptCalculation

@Composable
fun ReceiptCalculationCard(
    calculation: ReceiptCalculation,
    displayCurrency: String = "MMK", // "MMK", "THB", "BOTH"
    index: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val isRefunded = calculation.status == OrderStatus.REFUNDED
    val cashGreenColor = if (isDark) CashGreenDark else CashGreen
    val thbBlueColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)

    val payBrandColor = PaymentColors.getBrandColor(calculation.paymentMethod, isDark)
    val payContainerColor = PaymentColors.getContainerColor(calculation.paymentMethod, isDark)
    val payTextColor = PaymentColors.getTextColor(calculation.paymentMethod, isDark)
    var isExpanded by rememberSaveable(calculation.id) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("receipt_calc_card_${calculation.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Index + Voucher Number & Date Time + Collapse/Expand Arrow Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (index != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$index",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = calculation.invoiceNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRefunded) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "ပြန်အမ်းပြီး",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = calculation.formattedDateTime,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { isExpanded = !isExpanded }
                            .testTag("receipt_calc_toggle_${calculation.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "ချုပ်ရန်" else "ဖြန့်ကြည့်ရန်",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Collapsed Compact Summary Line when isExpanded == false
            if (!isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = payContainerColor,
                            border = BorderStroke(0.5.dp, payBrandColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = calculation.paymentMethod.labelMm,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = payTextColor,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${calculation.itemCount} မျိုး (${calculation.totalUnitsSold} ခု)",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isCalcThb = com.example.data.isThbCurrencyString(calculation.currency)
                        val compactAmountText = when (displayCurrency) {
                            "THB" -> PosViewModel.formatCurrency(calculation.sellingPriceThb, "THB")
                            "MMK" -> PosViewModel.formatKyatMm(calculation.sellingPriceMmk)
                            else -> if (isCalcThb) {
                                PosViewModel.formatCurrency(calculation.sellingPriceThb, "THB")
                            } else {
                                PosViewModel.formatKyatMm(calculation.sellingPriceMmk)
                            }
                        }
                        val compactColor = if (isCalcThb || displayCurrency == "THB") thbBlueColor else cashGreenColor
                        Text(
                            text = compactAmountText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = compactColor,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Customer Name & Note if any
                    if (calculation.customerName.isNotBlank() || calculation.note.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (calculation.customerName.isNotBlank()) {
                                Text(
                                    text = "ဝယ်သူ: ${calculation.customerName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false).basicMarquee()
                                )
                            }
                            if (calculation.note.isNotBlank()) {
                                Text(
                                    text = "(${calculation.note})",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Row 2: Selling Price vs Buying / Cost Price (Precise calculations)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Selling Price
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "အရောင်းဈေး",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            when (displayCurrency) {
                                "THB" -> {
                                    Text(
                                        text = PosViewModel.formatCurrency(calculation.sellingPriceThb, "THB"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = thbBlueColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    Text(
                                        text = "(= ${PosViewModel.formatKyatMm(calculation.sellingPriceMmk)})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = cashGreenColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                                "MMK" -> {
                                    Text(
                                        text = PosViewModel.formatKyatMm(calculation.sellingPriceMmk),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cashGreenColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    Text(
                                        text = "(= ${PosViewModel.formatCurrency(calculation.sellingPriceThb, "THB")})",
                                        fontSize = 10.sp,
                                        color = thbBlueColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                                else -> { // BOTH
                                    val isCalcThb = com.example.data.isThbCurrencyString(calculation.currency)
                                    if (isCalcThb) {
                                        Text(
                                            text = PosViewModel.formatCurrency(calculation.sellingPriceThb, "THB"),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = thbBlueColor,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                        Text(
                                            text = PosViewModel.formatKyatMm(calculation.sellingPriceMmk),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cashGreenColor,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                    } else {
                                        Text(
                                            text = PosViewModel.formatKyatMm(calculation.sellingPriceMmk),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cashGreenColor,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                        Text(
                                            text = PosViewModel.formatCurrency(calculation.sellingPriceThb, "THB"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = thbBlueColor,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Buying / Cost Price
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ဝယ်ရင်းဈေး (ကုန်ကျစရိတ်)",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            when (displayCurrency) {
                                "THB" -> {
                                    Text(
                                        text = PosViewModel.formatCurrency(calculation.buyingPriceThb, "THB"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = thbBlueColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    Text(
                                        text = "(= ${PosViewModel.formatKyatMm(calculation.buyingPriceMmk)})",
                                        fontSize = 10.sp,
                                        color = cashGreenColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                                "MMK" -> {
                                    Text(
                                        text = PosViewModel.formatKyatMm(calculation.buyingPriceMmk),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cashGreenColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    Text(
                                        text = "(= ${PosViewModel.formatCurrency(calculation.buyingPriceThb, "THB")})",
                                        fontSize = 10.sp,
                                        color = thbBlueColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                                else -> { // BOTH
                                    val isCalcThb = com.example.data.isThbCurrencyString(calculation.currency)
                                    if (isCalcThb) {
                                        Text(
                                            text = PosViewModel.formatCurrency(calculation.buyingPriceThb, "THB"),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = thbBlueColor,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                        Text(
                                            text = PosViewModel.formatKyatMm(calculation.buyingPriceMmk),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cashGreenColor,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                    } else {
                                        Text(
                                            text = PosViewModel.formatKyatMm(calculation.buyingPriceMmk),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cashGreenColor,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                        Text(
                                            text = PosViewModel.formatCurrency(calculation.buyingPriceThb, "THB"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = thbBlueColor,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Row 3: Estimated Net Profit Banner (ခန့်မှန်း အသားတင်အမြတ်) + Exchange Rate Below It
                    val isThbOrder = com.example.data.isThbCurrencyString(calculation.currency) || displayCurrency == "THB"
                    val activeProfitBannerColor = if (isThbOrder) thbBlueColor else cashGreenColor
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = activeProfitBannerColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, activeProfitBannerColor.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = activeProfitBannerColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ခန့်မှန်း အသားတင်အမြတ် :",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    val profitText = when (displayCurrency) {
                                        "THB" -> "+${PosViewModel.formatCurrency(calculation.estimatedNetProfitThb, "THB")}"
                                        "MMK" -> "+${PosViewModel.formatKyatMm(calculation.estimatedNetProfitMmk)}"
                                        else -> if (isThbOrder) {
                                            "+${PosViewModel.formatCurrency(calculation.estimatedNetProfitThb, "THB")}"
                                        } else {
                                            "+${PosViewModel.formatKyatMm(calculation.estimatedNetProfitMmk)}"
                                        }
                                    }
                                    Text(
                                        text = "(${calculation.profitMarginPercent}%) $profitText",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = activeProfitBannerColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                            }

                            val rateLabel = "ပြေစာငွေလဲနှုန်း :"
                            val rateValue = if (isThbOrder) {
                                calculation.mmkBuyingRate.ifBlank { calculation.thbBuyingRate }
                            } else {
                                calculation.mmkSellingRate.ifBlank { calculation.thbSellingRate }
                            }
                            if (rateValue.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rateLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = "1 ကျပ် = $rateValue ဘတ်",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isThbOrder) thbBlueColor else cashGreenColor,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Row 4: Exchange Rates from Settings Applied & Payment / Details Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = payContainerColor,
                                border = BorderStroke(0.5.dp, payBrandColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = calculation.paymentMethod.labelMm,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = payTextColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = "${calculation.itemCount} မျိုး (${calculation.totalUnitsSold} ခု)",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ပြေစာကြည့်ရန်",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(9.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
