package com.example.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ShopProfile
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenDark
import com.example.ui.theme.NaturalBorderLight
import com.example.ui.theme.NaturalPrimary
import com.example.ui.theme.NaturalSurfaceVariantLight
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary

@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()
    val effectiveDarkMode = isDarkMode ?: systemInDark

    var showEditShopDialog by remember { mutableStateOf(false) }
    var showThbRateDialog by remember { mutableStateOf(false) }
    var showMmkRateDialog by remember { mutableStateOf(false) }

    val thbSellingRate by viewModel.thbSellingRate.collectAsStateWithLifecycle()
    val thbBuyingRate by viewModel.thbBuyingRate.collectAsStateWithLifecycle()
    val mmkSellingRate by viewModel.mmkSellingRate.collectAsStateWithLifecycle()
    val mmkBuyingRate by viewModel.mmkBuyingRate.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Shop Status Banner (Mimics the User's Image)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Light purple container with purple store icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (shopProfile.shopName.isNotBlank()) shopProfile.shopName else "ဆိုင်အမည် သတ်မှတ်ရန်",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (shopProfile.shopName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (shopProfile.phone.isNotBlank()) "ဖုန်း: ${shopProfile.phone}" else "ဖုန်း: မထည့်ရသေးပါ",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (shopProfile.address.isNotBlank()) shopProfile.address else "လိပ်စာ: မထည့်ရသေးပါ",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                    
                    // Edit Pencil Icon
                    IconButton(
                        onClick = { showEditShopDialog = true },
                        modifier = Modifier.testTag("settings_edit_shop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Shop Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Navigation Buttons (မျက်နှာပြင် ခလုတ်များ)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. မြန်မာကျပ် ရောင်းဈေး နှင့် ဝယ်ဈေး နှုန်း Icon Button
                Button(
                    onClick = { showMmkRateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_mmk_rate_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (effectiveDarkMode) Color(0xFF065F46) else Color(0xFF006D39)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "မြန်မာကျပ် ရောင်းဈေး နှင့် ဝယ်ဈေး နှုန်း",
                            maxLines = 1,
                            color = Color.White,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }

                // 2. ထိုင်းဘတ် ရောင်းဈေး  နှင့် ဝယ်ဈေး နှုန်း Icon Button
                Button(
                    onClick = { showThbRateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_thb_rate_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (effectiveDarkMode) Color(0xFF1E3A8A) else Color(0xFF0066B3)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ထိုင်းဘတ် ရောင်းဈေး  နှင့် ဝယ်ဈေး နှုန်း",
                            maxLines = 1,
                            color = Color.White,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }

                // Landscape Layout: Checkboxes for ရှင်းမည် icon Button, Outer Container Box Layout, Header Bar & Cart Item controls
                val showClearIconButton by viewModel.showClearIconButton.collectAsStateWithLifecycle()
                val showOuterContainerBox by viewModel.showOuterContainerBox.collectAsStateWithLifecycle()
                val showTopAppBar by viewModel.showTopAppBar.collectAsStateWithLifecycle()
                val showCartItemCheckbox by viewModel.showCartItemCheckbox.collectAsStateWithLifecycle()
                val showPriceModeCard by viewModel.showPriceModeCard.collectAsStateWithLifecycle()
                val showAddProductButton by viewModel.showAddProductButton.collectAsStateWithLifecycle()
                val showSavedReceiptActions by viewModel.showSavedReceiptActions.collectAsStateWithLifecycle()
                val showCheckoutDetails by viewModel.showCheckoutDetails.collectAsStateWithLifecycle()
                val showProfitInfo by viewModel.showProfitInfo.collectAsStateWithLifecycle()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("landscape_control_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "မျက်နှာပြင် ပြသမှု ထိန်းချုပ်ရန် ဆက်တင်များ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Row 1: 4 columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. ရှင်းမည်/သိမ်းမည်
                            DisplayControlCheckboxItem(
                                title = "ရှင်းမည်/သိမ်းမည်",
                                checked = showClearIconButton,
                                onCheckedChange = { viewModel.toggleShowClearIconButton(it) },
                                modifier = Modifier.weight(1f)
                            )

                            // 2. အရေအတွက် အမြန်ပြင်
                            DisplayControlCheckboxItem(
                                title = "အရေအတွက် အမြန်ပြင်",
                                checked = showOuterContainerBox,
                                onCheckedChange = { viewModel.toggleShowOuterContainerBox(it) },
                                modifier = Modifier.weight(1f)
                            )

                            // 3. ဆိုင်အမည်
                            DisplayControlCheckboxItem(
                                title = "ဆိုင်အမည်",
                                checked = showTopAppBar,
                                onCheckedChange = { viewModel.toggleShowTopAppBar(it) },
                                modifier = Modifier.weight(1f)
                            )

                            // 4. လက်ကားဈေးနှုန်း
                            val isWholesaleSettingsEnabled = showCartItemCheckbox && showPriceModeCard
                            DisplayControlCheckboxItem(
                                title = "လက်ကားဈေးနှုန်း",
                                checked = isWholesaleSettingsEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.toggleShowWholesale(checked)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 2: 4 columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 5. ပစ္စည်းများ
                            DisplayControlCheckboxItem(
                                title = "ပစ္စည်းများ",
                                checked = showAddProductButton,
                                onCheckedChange = { viewModel.toggleShowAddProductButton(it) },
                                modifier = Modifier.weight(1f)
                            )

                            // 6. ရှင်းပြီးပြေစာ ခလုတ်
                            DisplayControlCheckboxItem(
                                title = "ရှင်းပြီးပြေစာ ခလုတ်",
                                checked = showSavedReceiptActions,
                                onCheckedChange = { viewModel.toggleShowSavedReceiptActions(it) },
                                modifier = Modifier.weight(1f)
                            )

                            // 7. ရှင်းကောင်တာ
                            DisplayControlCheckboxItem(
                                title = "ရှင်းကောင်တာ",
                                checked = showCheckoutDetails,
                                onCheckedChange = { viewModel.toggleShowCheckoutDetails(it) },
                                modifier = Modifier.weight(1f)
                            )

                            // 8. အမြတ်ငွေ / ပေါင်
                            DisplayControlCheckboxItem(
                                title = "အမြတ်ငွေ / ပေါင်",
                                checked = showProfitInfo,
                                onCheckedChange = { viewModel.toggleShowProfitInfo(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 3: Add new checkmark toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val showCartWholesaleCheckmark by viewModel.showCartWholesaleCheckmark.collectAsStateWithLifecycle()
                            DisplayControlCheckboxItem(
                                title = "လက်ကားအမှန်ခြစ်",
                                checked = showCartWholesaleCheckmark,
                                onCheckedChange = { viewModel.toggleShowCartWholesaleCheckmark(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // About Card (Always pinned at the bottom)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "POS System",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "မြန်မာလက်လီဆိုင်များအတွက် အထူးထုတ်လုပ်ထားသည်။",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Edit Shop Profile Dialog (Triggers from Pencil Icon on the Top Card)
    if (showEditShopDialog) {
        var tempShopName by remember(shopProfile) { mutableStateOf(shopProfile.shopName) }
        var tempPhone by remember(shopProfile) { mutableStateOf(shopProfile.phone) }
        var tempAddress by remember(shopProfile) { mutableStateOf(shopProfile.address) }
        var tempCashierName by remember(shopProfile) { mutableStateOf(shopProfile.cashierName) }
        var tempReceiptFooter by remember(shopProfile) { mutableStateOf(shopProfile.receiptFooter) }

        AlertDialog(
            onDismissRequest = { showEditShopDialog = false },
            title = {
                Text(
                    text = "ဆိုင်အချက်အလက် ပြင်ဆင်ရန်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NaturalTextPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempShopName,
                        onValueChange = { tempShopName = it },
                        label = { Text("ဆိုင်အမည်") },
                        leadingIcon = { Icon(Icons.Default.Store, null, tint = NaturalTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalPrimary,
                            unfocusedBorderColor = NaturalBorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("ဖုန်းနံပါတ်") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = NaturalTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalPrimary,
                            unfocusedBorderColor = NaturalBorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempAddress,
                        onValueChange = { tempAddress = it },
                        label = { Text("ဆိုင်လိပ်စာ") },
                        leadingIcon = { Icon(Icons.Default.Business, null, tint = NaturalTextSecondary) },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalPrimary,
                            unfocusedBorderColor = NaturalBorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempCashierName,
                        onValueChange = { tempCashierName = it },
                        label = { Text("ကောင်တာမှူး အမည်") },
                        leadingIcon = { Icon(Icons.Default.Badge, null, tint = NaturalTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalPrimary,
                            unfocusedBorderColor = NaturalBorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempReceiptFooter,
                        onValueChange = { tempReceiptFooter = it },
                        label = { Text("ပြေစာအောက်ခြေ ကျေးဇူးတင်စာသား") },
                        leadingIcon = { Icon(Icons.Default.Receipt, null, tint = NaturalTextSecondary) },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalPrimary,
                            unfocusedBorderColor = NaturalBorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempShopName.isBlank()) {
                            Toast.makeText(context, "ဆိုင်အမည် ဖြည့်စွက်ပေးပါ", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateShopProfile(
                                shopProfile.copy(
                                    shopName = tempShopName.trim(),
                                    phone = tempPhone.trim(),
                                    address = tempAddress.trim(),
                                    cashierName = tempCashierName.trim(),
                                    receiptFooter = tempReceiptFooter.trim()
                                ),
                                syncToCloudIfEnabled = true
                            )
                            showEditShopDialog = false
                            Toast.makeText(context, "ဆက်တင်များကို သိမ်းဆည်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditShopDialog = false }) {
                    Text("မလုပ်တော့ပါ", color = NaturalTextSecondary)
                }
            }
        )
    }

    // Thai Baht Rate Dialog
    if (showThbRateDialog) {
        var tempSellingRate by remember(thbSellingRate) {
            mutableStateOf(thbSellingRate)
        }
        var tempBuyingRate by remember(thbBuyingRate) {
            mutableStateOf(thbBuyingRate)
        }

        AlertDialog(
            onDismissRequest = { showThbRateDialog = false },
            title = {
                Text(
                    text = "ထိုင်းဘတ် ရောင်းဈေး နှင့် ဝယ်ဈေး နှုန်း",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val previewThbColor = if (effectiveDarkMode) Color(0xFF60A5FA) else Color(0xFF0066B3)
                    val previewMmkColor = if (effectiveDarkMode) CashGreenDark else Color(0xFF00602F)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = tempSellingRate,
                            onValueChange = { input ->
                                // Restrict to numbers only and at most one decimal point
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    tempSellingRate = filtered
                                }
                            },
                            label = { Text("ထိုင်းဘတ်ရောင်းဈေး") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("thb_selling_rate_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("၁ ", fontSize = 11.5.sp, color = previewThbColor, fontWeight = FontWeight.Bold)
                            Text("ဘတ် (THB) ", fontSize = 11.5.sp, color = previewThbColor, fontWeight = FontWeight.Bold)
                            Text("= ", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (tempSellingRate.isNotBlank()) tempSellingRate else "-", fontSize = 11.5.sp, color = previewMmkColor, fontWeight = FontWeight.Bold)
                            Text(" ကျပ် (MMK)", fontSize = 11.5.sp, color = previewMmkColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = tempBuyingRate,
                            onValueChange = { input ->
                                // Restrict to numbers only and at most one decimal point
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    tempBuyingRate = filtered
                                }
                            },
                            label = { Text("ထိုင်းဘတ်ဝယ်ဈေး") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("thb_buying_rate_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("၁ ", fontSize = 11.5.sp, color = previewThbColor, fontWeight = FontWeight.Bold)
                            Text("ဘတ် (THB) ", fontSize = 11.5.sp, color = previewThbColor, fontWeight = FontWeight.Bold)
                            Text("= ", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (tempBuyingRate.isNotBlank()) tempBuyingRate else "-", fontSize = 11.5.sp, color = previewMmkColor, fontWeight = FontWeight.Bold)
                            Text(" ကျပ် (MMK)", fontSize = 11.5.sp, color = previewMmkColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalSellingRate = tempSellingRate.trim()
                        val finalBuyingRate = tempBuyingRate.trim()
                        viewModel.updateThbRates(finalSellingRate, finalBuyingRate)
                        showThbRateDialog = false
                        Toast.makeText(context, "သတ်မှတ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("thb_rate_confirm_button")
                ) {
                    Text("သတ်မှတ်မည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showThbRateDialog = false },
                    modifier = Modifier.testTag("thb_rate_cancel_button")
                ) {
                    Text("မလုပ်တော့ပါ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showMmkRateDialog) {
        var tempMmkSellingRate by remember(mmkSellingRate) {
            mutableStateOf(mmkSellingRate)
        }
        var tempMmkBuyingRate by remember(mmkBuyingRate) {
            mutableStateOf(mmkBuyingRate)
        }

        AlertDialog(
            onDismissRequest = { showMmkRateDialog = false },
            title = {
                Text(
                    text = "မြန်မာကျပ် ရောင်းဈေး နှင့် ဝယ်ဈေး နှုန်း",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val previewThbColor = if (effectiveDarkMode) Color(0xFF60A5FA) else Color(0xFF0066B3)
                    val previewMmkColor = if (effectiveDarkMode) CashGreenDark else Color(0xFF00602F)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = tempMmkSellingRate,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    tempMmkSellingRate = filtered
                                }
                            },
                            label = { Text("မြန်မာကျပ်ရောင်းဈေး") },
                            placeholder = { Text("0.00000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (effectiveDarkMode) CashGreenDark else Color(0xFF00602F),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mmk_selling_rate_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("၁ ", fontSize = 11.5.sp, color = previewMmkColor, fontWeight = FontWeight.Bold)
                            Text("ကျပ် (MMK) ", fontSize = 11.5.sp, color = previewMmkColor, fontWeight = FontWeight.Bold)
                            Text("= ", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (tempMmkSellingRate.isNotBlank()) tempMmkSellingRate else "-", fontSize = 11.5.sp, color = previewThbColor, fontWeight = FontWeight.Bold)
                            Text(" ဘတ် (THB)", fontSize = 11.5.sp, color = previewThbColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = tempMmkBuyingRate,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    tempMmkBuyingRate = filtered
                                }
                            },
                            label = { Text("မြန်မာကျပ်ဝယ်ဈေး") },
                            placeholder = { Text("0.00000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (effectiveDarkMode) CashGreenDark else Color(0xFF00602F),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mmk_buying_rate_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("၁ ", fontSize = 11.5.sp, color = previewMmkColor, fontWeight = FontWeight.Bold)
                            Text("ကျပ် (MMK) ", fontSize = 11.5.sp, color = previewMmkColor, fontWeight = FontWeight.Bold)
                            Text("= ", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (tempMmkBuyingRate.isNotBlank()) tempMmkBuyingRate else "-", fontSize = 11.5.sp, color = previewThbColor, fontWeight = FontWeight.Bold)
                            Text(" ဘတ် (THB)", fontSize = 11.5.sp, color = previewThbColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalSellingRate = tempMmkSellingRate.trim()
                        val finalBuyingRate = tempMmkBuyingRate.trim()
                        viewModel.updateMmkRates(finalSellingRate, finalBuyingRate)
                        showMmkRateDialog = false
                        Toast.makeText(context, "သတ်မှတ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (effectiveDarkMode) CashGreenDark else Color(0xFF00602F)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("mmk_rate_confirm_button")
                ) {
                    Text("သတ်မှတ်မည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMmkRateDialog = false },
                    modifier = Modifier.testTag("mmk_rate_cancel_button")
                ) {
                    Text("မလုပ်တော့ပါ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun DisplayControlCheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 8.2.sp,
            maxLines = 1,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.basicMarquee()
        )
        Spacer(modifier = Modifier.height(1.dp))
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.size(20.dp)
        )
    }
}
