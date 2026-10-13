package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Natural Tones Palette (Warm Almond & Mauve)
val NaturalBgLight = Color(0xFFFDF8F6)
val NaturalSurfaceLight = Color(0xFFFFFFFF)
val NaturalSurfaceVariantLight = Color(0xFFF3EDF7)
val NaturalBorderLight = Color(0xFFCAC4D0)
val NaturalBorderSubtle = Color(0xFFF4EFF4)

val NaturalPrimary = Color(0xFF6750A4)
val NaturalOnPrimary = Color(0xFFFFFFFF)
val NaturalPrimaryContainer = Color(0xFFEADDFF)
val NaturalOnPrimaryContainer = Color(0xFF21005D)

val NaturalSecondary = Color(0xFF625B71)
val NaturalSecondaryContainer = Color(0xFFE8DEF8)
val NaturalOnSecondaryContainer = Color(0xFF1D192B)

val NaturalTertiary = Color(0xFF7D5260)
val NaturalTertiaryContainer = Color(0xFFFFD8E4)
val NaturalOnTertiaryContainer = Color(0xFF31111D)

val NaturalTextPrimary = Color(0xFF1D1B1E)
val NaturalTextSecondary = Color(0xFF49454F)

// Pastel accent highlights for tiles/chips
val NaturalPastelBlue = Color(0xFFB6EAFF)
val NaturalPastelRose = Color(0xFFFFD8E4)
val NaturalPastelPurple = Color(0xFFD0BCFF)
val NaturalPastelStone = Color(0xFFE6E1E5)

// Dark Variant for Natural Tones
val NaturalBgDark = Color(0xFF141218)
val NaturalSurfaceDark = Color(0xFF1D1B20)
val NaturalSurfaceVariantDark = Color(0xFF28252E)
val NaturalSurfaceCardDark = Color(0xFF232029)
val NaturalPrimaryDark = Color(0xFFD0BCFF)
val NaturalOnPrimaryDark = Color(0xFF381E72)
val NaturalPrimaryContainerDark = Color(0xFF4F378B)
val NaturalOnPrimaryContainerDark = Color(0xFFEADDFF)

val NaturalSecondaryDark = Color(0xFFCCC2DC)
val NaturalOnSecondaryDark = Color(0xFF332D41)
val NaturalSecondaryContainerDark = Color(0xFF4A4458)
val NaturalOnSecondaryContainerDark = Color(0xFFE8DEF8)

val NaturalTertiaryDark = Color(0xFFEFB8C8)
val NaturalOnTertiaryDark = Color(0xFF492532)
val NaturalTertiaryContainerDark = Color(0xFF633B48)
val NaturalOnTertiaryContainerDark = Color(0xFFFFD8E4)

val NaturalBorderDark = Color(0xFF49454F)
val NaturalBorderSubtleDark = Color(0xFF333038)
val NaturalTextPrimaryDark = Color(0xFFE6E1E5)
val NaturalTextSecondaryDark = Color(0xFFCAC4D0)

// Status & Payment Colors
val NaturalPositiveGreen = Color(0xFF006D39)
val CashGreen = Color(0xFF006D39)
val CashGreenDark = Color(0xFF66DF96)
val KPayBlue = Color(0xFF0066B3)
val KPayBlueDark = Color(0xFF70B6FF)
val WavePayYellow = Color(0xFFD97706)
val WavePayYellowDark = Color(0xFFFBBF24)
val AyaPayRed = Color(0xFFBA1A1A)
val AyaPayRedDark = Color(0xFFFFB4AB)

// Currency Color Constants (Green for MMK / Blue for THB)
val MmkGreenPrimary = Color(0xFF007A3D)
val MmkGreenDark = Color(0xFF52D68A)
val MmkGreenContainerLight = Color(0xFFE8F8EE)
val MmkGreenContainerDark = Color(0xFF0B331D)
val MmkGreenBorderLight = Color(0xFFA5D6A7)
val MmkGreenBorderDark = Color(0xFF1E5B37)
val MmkGreenTextLight = Color(0xFF005A2C)
val MmkGreenTextDark = Color(0xFF66DF96)

val ThbBluePrimary = Color(0xFF0066B3)
val ThbBlueDark = Color(0xFF70B6FF)
val ThbBlueContainerLight = Color(0xFFE3F2FD)
val ThbBlueContainerDark = Color(0xFF0D2D4F)
val ThbBlueBorderLight = Color(0xFF90CAF9)
val ThbBlueBorderDark = Color(0xFF1A4975)
val ThbBlueTextLight = Color(0xFF004C87)
val ThbBlueTextDark = Color(0xFF90CAF9)

object CurrencyColors {
    @Composable
    fun getPrimaryColor(currency: String, isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Color {
        val isThb = currency.equals("THB", ignoreCase = true) || currency.contains("ဘတ်")
        return if (isThb) {
            if (isDark) ThbBlueDark else ThbBluePrimary
        } else {
            if (isDark) MmkGreenDark else MmkGreenPrimary
        }
    }

    @Composable
    fun getContainerColor(currency: String, isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Color {
        val isThb = currency.equals("THB", ignoreCase = true) || currency.contains("ဘတ်")
        return if (isThb) {
            if (isDark) ThbBlueContainerDark else ThbBlueContainerLight
        } else {
            if (isDark) MmkGreenContainerDark else MmkGreenContainerLight
        }
    }

    @Composable
    fun getTextColor(currency: String, isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Color {
        val isThb = currency.equals("THB", ignoreCase = true) || currency.contains("ဘတ်")
        return if (isThb) {
            if (isDark) ThbBlueTextDark else ThbBlueTextLight
        } else {
            if (isDark) MmkGreenTextDark else MmkGreenTextLight
        }
    }

    @Composable
    fun getBorderColor(currency: String, isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Color {
        val isThb = currency.equals("THB", ignoreCase = true) || currency.contains("ဘတ်")
        return if (isThb) {
            if (isDark) ThbBlueBorderDark else ThbBlueBorderLight
        } else {
            if (isDark) MmkGreenBorderDark else MmkGreenBorderLight
        }
    }
}

object PaymentColors {
    @Composable
    fun getBrandColor(method: com.example.data.entity.PaymentMethod, isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Color {
        return when (method) {
            com.example.data.entity.PaymentMethod.CASH -> if (isDark) CashGreenDark else CashGreen
            com.example.data.entity.PaymentMethod.KPAY -> if (isDark) KPayBlueDark else KPayBlue
            com.example.data.entity.PaymentMethod.WAVEPAY -> if (isDark) WavePayYellowDark else WavePayYellow
            com.example.data.entity.PaymentMethod.AYAPAY -> if (isDark) AyaPayRedDark else AyaPayRed
            com.example.data.entity.PaymentMethod.CREDIT -> if (isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563)
        }
    }

    @Composable
    fun getContainerColor(method: com.example.data.entity.PaymentMethod, isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Color {
        return if (isDark) {
            when (method) {
                com.example.data.entity.PaymentMethod.CASH -> Color(0xFF0F3822)
                com.example.data.entity.PaymentMethod.KPAY -> Color(0xFF103355)
                com.example.data.entity.PaymentMethod.WAVEPAY -> Color(0xFF452205)
                com.example.data.entity.PaymentMethod.AYAPAY -> Color(0xFF4C1518)
                com.example.data.entity.PaymentMethod.CREDIT -> Color(0xFF262A30)
            }
        } else {
            when (method) {
                com.example.data.entity.PaymentMethod.CASH -> Color(0xFFE8F5E9)
                com.example.data.entity.PaymentMethod.KPAY -> Color(0xFFE1F0FF)
                com.example.data.entity.PaymentMethod.WAVEPAY -> Color(0xFFFEF3C7)
                com.example.data.entity.PaymentMethod.AYAPAY -> Color(0xFFFFEBEE)
                com.example.data.entity.PaymentMethod.CREDIT -> Color(0xFFF3F4F6)
            }
        }
    }

    @Composable
    fun getTextColor(method: com.example.data.entity.PaymentMethod, isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Color {
        return if (isDark) {
            when (method) {
                com.example.data.entity.PaymentMethod.CASH -> Color(0xFF66DF96) // Crisp mint green
                com.example.data.entity.PaymentMethod.KPAY -> Color(0xFF70B6FF) // Crisp sky blue
                com.example.data.entity.PaymentMethod.WAVEPAY -> Color(0xFFFBBF24) // Luminous warm yellow
                com.example.data.entity.PaymentMethod.AYAPAY -> Color(0xFFFFB4AB) // Bright coral red
                com.example.data.entity.PaymentMethod.CREDIT -> Color(0xFFE5E7EB) // Light clean gray
            }
        } else {
            when (method) {
                com.example.data.entity.PaymentMethod.CASH -> Color(0xFF00602F) // Deep rich forest green
                com.example.data.entity.PaymentMethod.KPAY -> Color(0xFF004C87) // Deep navy blue
                com.example.data.entity.PaymentMethod.WAVEPAY -> Color(0xFF92400E) // Deep amber brown (Never low contrast on light container)
                com.example.data.entity.PaymentMethod.AYAPAY -> Color(0xFF991B1B) // Deep crimson red
                com.example.data.entity.PaymentMethod.CREDIT -> Color(0xFF1F2937) // Deep slate
            }
        }
    }

    @Composable
    fun getBorderColor(method: com.example.data.entity.PaymentMethod, isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Color {
        return getBrandColor(method, isDark).copy(alpha = if (isDark) 0.6f else 0.45f)
    }

    fun getIcon(method: com.example.data.entity.PaymentMethod): ImageVector {
        return when (method) {
            com.example.data.entity.PaymentMethod.CASH -> Icons.Default.Payments
            com.example.data.entity.PaymentMethod.KPAY -> Icons.Default.AccountBalanceWallet
            com.example.data.entity.PaymentMethod.WAVEPAY -> Icons.Default.ElectricBolt
            com.example.data.entity.PaymentMethod.AYAPAY -> Icons.Default.CreditCard
            com.example.data.entity.PaymentMethod.CREDIT -> Icons.Default.ReceiptLong
        }
    }
}

@Composable
fun adaptiveCashColor(): Color = if (androidx.compose.foundation.isSystemInDarkTheme()) CashGreenDark else Color(0xFF00602F)

@Composable
fun adaptiveKPayColor(): Color = if (androidx.compose.foundation.isSystemInDarkTheme()) KPayBlueDark else Color(0xFF004C87)

@Composable
fun adaptiveWavePayColor(): Color = if (androidx.compose.foundation.isSystemInDarkTheme()) WavePayYellowDark else Color(0xFF92400E)

@Composable
fun adaptiveAyaPayColor(): Color = if (androidx.compose.foundation.isSystemInDarkTheme()) AyaPayRedDark else Color(0xFF991B1B)

@Composable
fun adaptiveProfitColor(): Color = if (androidx.compose.foundation.isSystemInDarkTheme()) CashGreenDark else Color(0xFF006D39)

@Composable
fun adaptiveCurrencyColor(currency: String): Color =
    if (currency == "THB") {
        if (androidx.compose.foundation.isSystemInDarkTheme()) KPayBlueDark else Color(0xFF0066B3)
    } else {
        if (androidx.compose.foundation.isSystemInDarkTheme()) CashGreenDark else CashGreen
    }


