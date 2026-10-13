package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.data.ShopProfile
import com.example.data.entity.PaymentMethod
import com.example.data.entity.Product
import com.example.data.entity.SavedReceipt
import com.example.util.ReceiptCalculator
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenDark
import com.example.ui.theme.KPayBlue
import com.example.ui.theme.KPayBlueDark
import com.example.ui.theme.PaymentColors
import com.example.ui.theme.WavePayYellow
import com.example.ui.theme.WavePayYellowDark
import com.example.util.ReceiptPrinter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedReceiptsScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedReceipts by viewModel.savedReceipts.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val showSavedReceiptActions by viewModel.showSavedReceiptActions.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var receiptToDelete by remember { mutableStateOf<SavedReceipt?>(null) }
    var receiptToRestore by remember { mutableStateOf<SavedReceipt?>(null) }

    val filteredList = remember(savedReceipts, searchQuery) {
        if (searchQuery.isBlank()) {
            savedReceipts
        } else {
            savedReceipts.filter { receipt ->
                receipt.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                        receipt.customerName.contains(searchQuery, ignoreCase = true) ||
                        receipt.customerPhone.contains(searchQuery, ignoreCase = true) ||
                        receipt.itemsJson.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val allSavedReceiptCalculations = remember(savedReceipts, products, shopProfile) {
        savedReceipts.map { receipt ->
            ReceiptCalculator.calculateFromSavedReceipt(receipt, products, shopProfile)
        }
    }

    val totalSavedAmountMmk = remember(allSavedReceiptCalculations) {
        allSavedReceiptCalculations.sumOf { it.sellingPriceMmk }
    }
    val totalSavedAmountThb = remember(allSavedReceiptCalculations) {
        allSavedReceiptCalculations.sumOf { it.sellingPriceThb }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "ရှင်းပြီး ပြေစာများ",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = "သိမ်းဆည်းထားသော ပြေစာများ (${savedReceipts.size} စောင်)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("saved_receipts_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (showSavedReceiptActions && savedReceipts.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteAllDialog = true },
                            modifier = Modifier.testTag("saved_receipts_clear_all_button")
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "စုစုပေါင်း ရှင်းပြီးပြေစာ ပမာဏ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val isDark = isSystemInDarkTheme()
                        val thbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                        val mmkColor = if (isDark) CashGreenDark else CashGreen
                        Text(
                            text = PosViewModel.formatKyatMm(totalSavedAmountMmk),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = mmkColor
                        )
                        Text(
                            text = "= ${PosViewModel.formatCurrency(totalSavedAmountThb, "THB")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = thbColor
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "အရေအတွက်",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${savedReceipts.size} စောင်",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("saved_receipts_search_input"),
                placeholder = {
                    Text(
                        text = "ဘောက်ချာနံပါတ်၊ ဝယ်သူအမည်၊ ပစ္စည်းဖြင့် ရှာရန်...",
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (filteredList.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "ရှင်းပြီးသော ပြေစာများ မရှိသေးပါ" else "ရှာဖွေမှု မတွေ့ရှိပါ",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank())
                                "အရောင်းပြေစာမျက်နှာပြင်ရှိ ✅ (အမှန်ခြစ်) ခလုတ်ကို နှိပ်လိုက်ပါက ပြေစာကို အရောင်းစာရင်းမှ ဖျက်ကာ ဤရှင်းပြီး ပြေစာများ ထဲတွင် အလိုအလျောက် သိမ်းဆည်းပေးမည် ဖြစ်ပါသည်။"
                            else
                                "အခြားသော စာလုံး သို့မဟုတ် ဘောက်ချာနံပါတ်ဖြင့် ပြန်လည် ရှာဖွေကြည့်ပါ။",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                // List of saved receipts
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredList.forEach { savedReceipt ->
                        androidx.compose.runtime.key(savedReceipt.id) {
                            SavedReceiptCard(
                                savedReceipt = savedReceipt,
                                products = products,
                                shopProfile = shopProfile,
                                showActions = showSavedReceiptActions,
                                onViewReceipt = {
                                    viewModel.showSavedReceipt(savedReceipt)
                                },
                                onPrint = {
                                    ReceiptPrinter.printReceipt(
                                        context = context,
                                        orderDetails = savedReceipt.toOrderDetails(),
                                        shopProfile = shopProfile
                                    )
                                },
                                onShare = {
                                    val calcForShare = ReceiptCalculator.calculateFromSavedReceipt(savedReceipt, products, shopProfile)
                                    val isThbShare = com.example.data.isThbCurrencyString(savedReceipt.currency)
                                    val totalText = if (isThbShare) {
                                        "${PosViewModel.formatCurrency(calcForShare.sellingPriceThb, "THB")} (≈ ${PosViewModel.formatKyatMm(calcForShare.sellingPriceMmk)})"
                                    } else {
                                        "${PosViewModel.formatKyatMm(calcForShare.sellingPriceMmk)} (≈ ${PosViewModel.formatCurrency(calcForShare.sellingPriceThb, "THB")})"
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "🧾 ${shopProfile.shopName}\n" +
                                                    "ဘောက်ချာ: ${savedReceipt.invoiceNumber}\n" +
                                                    "ကျသင့်ငွေ: $totalText\n" +
                                                    "ငွေပေးချေမှု: ${savedReceipt.paymentMethod.labelMm} (${if (isThbShare) "ဘတ်" else "ကျပ်"})\n" +
                                                    "ရှင်းလင်းသိမ်းဆည်းချိန်: ${SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(savedReceipt.clearedTimestamp))}\n" +
                                                    "ကျေးဇူးတင်ပါသည်!"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "ပြေစာမျှဝေမည်"))
                                },
                                onRestore = {
                                    receiptToRestore = savedReceipt
                                },
                                onDelete = {
                                    receiptToDelete = savedReceipt
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Restore Dialog
    receiptToRestore?.let { receipt ->
        AlertDialog(
            onDismissRequest = { receiptToRestore = null },
            title = {
                Text(
                    text = "မူလ အရောင်းမှတ်တမ်းသို့ ပြန်ထည့်မည်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "ဘောက်ချာ ${receipt.invoiceNumber} အား ပင်မအရောင်းမှတ်တမ်း (History) သို့ ပြန်လည် ထည့်သွင်းလိုပါသလား?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreSavedReceipt(receipt.id)
                        receiptToRestore = null
                        Toast.makeText(context, "မူလ အရောင်းမှတ်တမ်းသို့ ပြန်လည်ထည့်သွင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ပြန်ထည့်မည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptToRestore = null }) {
                    Text("မလုပ်တော့ပါ")
                }
            }
        )
    }

    // Delete Single Receipt Dialog
    receiptToDelete?.let { receipt ->
        AlertDialog(
            onDismissRequest = { receiptToDelete = null },
            title = {
                Text(
                    text = "ပြေစာ ဖျက်ရန် သေချာပါသလား",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "ဘောက်ချာ ${receipt.invoiceNumber} အား ရှင်းပြီး ပြေစာများ မှ အပြီးတိုင် ဖျက်ပစ်ပါမည်။ ဤလုပ်ဆောင်ချက်ကို ပြန်ပြင်၍ မရပါ။",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSavedReceipt(receipt.id)
                        receiptToDelete = null
                        Toast.makeText(context, "ပြေစာကို အပြီးတိုင် ဖျက်ပစ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ဖျက်မည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptToDelete = null }) {
                    Text("မလုပ်တော့ပါ")
                }
            }
        )
    }

    // Clear All Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = {
                Text(
                    text = "သိမ်းဆည်းထားသော ပြေစာအားလုံး ဖျက်မည်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "ရှင်းပြီး ပြေစာများ တွင် သိမ်းဆည်းထားသော ပြေစာမှတ်တမ်း အားလုံးကို အပြီးတိုင် ဖျက်ဆီးလိုပါသလား?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllSavedReceipts()
                        showDeleteAllDialog = false
                        Toast.makeText(context, "ပြေစာမှတ်တမ်း အားလုံးကို ရှင်းလင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("အားလုံးဖျက်မည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("မလုပ်တော့ပါ")
                }
            }
        )
    }
}

@Composable
fun SavedReceiptCard(
    savedReceipt: SavedReceipt,
    products: List<Product> = emptyList(),
    shopProfile: ShopProfile = ShopProfile(),
    showActions: Boolean = true,
    onViewReceipt: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(savedReceipt.itemsJson) {
        SavedReceipt.parseItemsJson(savedReceipt.itemsJson)
    }

    val calc = remember(savedReceipt, products, shopProfile) {
        ReceiptCalculator.calculateFromSavedReceipt(savedReceipt, products, shopProfile)
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }
    val clearedDateStr = remember(savedReceipt.clearedTimestamp) {
        dateFormat.format(Date(savedReceipt.clearedTimestamp))
    }

    val isDark = isSystemInDarkTheme()
    val payBadgeBg = PaymentColors.getContainerColor(savedReceipt.paymentMethod, isDark)
    val payBadgeText = PaymentColors.getTextColor(savedReceipt.paymentMethod, isDark)
    val payBrandBorder = PaymentColors.getBrandColor(savedReceipt.paymentMethod, isDark).copy(alpha = 0.5f)
    var isExpanded by remember(savedReceipt.id) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewReceipt() }
            .testTag("saved_receipt_card_${savedReceipt.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top row: Invoice Number & Date Time + Arrow Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = savedReceipt.invoiceNumber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = clearedDateStr,
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
                            .testTag("saved_receipt_toggle_${savedReceipt.id}")
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

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Payment Badge & Customer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = payBadgeBg,
                            border = BorderStroke(0.5.dp, payBrandBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = PaymentColors.getIcon(savedReceipt.paymentMethod),
                                    contentDescription = null,
                                    tint = payBadgeText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (com.example.data.isThbCurrencyString(savedReceipt.currency)) "${savedReceipt.paymentMethod.labelMm} (THB)" else "${savedReceipt.paymentMethod.labelMm} (MMK)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = payBadgeText
                                )
                            }
                        }

                        if (savedReceipt.customerName.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ဝယ်သူ: ${savedReceipt.customerName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false).basicMarquee()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Item summary snippet
                    if (items.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            items.take(3).forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${item.productName}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f).basicMarquee()
                                    )
                                    Text(
                                        text = "x ${item.quantity} (${PosViewModel.formatCurrency(item.subtotal, item.currency)})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (items.size > 3) {
                                Text(
                                    text = "... နှင့် အခြား (${items.size - 3}) မျိုး",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Calculation Breakdown Card (ရောင်းဈေး၊ ဝယ်ဈေး၊ ခန့်မှန်း အသားတင်အမြတ်)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "မြန်မာကျပ် ရောင်း/ဝယ် :",
                                    fontSize = 11.sp,
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
                                        text = "ရောင်း ${PosViewModel.formatKyatMm(calc.effectiveTotalMmk)} | ဝယ် ${PosViewModel.formatKyatMm(calc.costMmk)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                            }

                            if (calc.effectiveTotalThb > 0 || calc.costThb > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ထိုင်းဘတ် ရောင်း/ဝယ် :",
                                        fontSize = 11.sp,
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
                                            text = "ရောင်း ${PosViewModel.formatCurrency(calc.effectiveTotalThb, "THB")} | ဝယ် ${PosViewModel.formatCurrency(calc.costThb, "THB")}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3),
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ခန့်မှန်း အသားတင်အမြတ် :",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) CashGreenDark else Color(0xFF047857),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    val isThbSaved = com.example.data.isThbCurrencyString(savedReceipt.currency)
                                    Text(
                                        text = if (isThbSaved) {
                                            "+${PosViewModel.formatCurrency(calc.profitThb, "THB")} (${calc.profitMarginPercent}%)"
                                        } else {
                                            "+${PosViewModel.formatKyatMm(calc.profitMmk)} (${calc.profitMarginPercent}%)"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) CashGreenDark else Color(0xFF047857),
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Total Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "အသားတင် ကျသင့်ငွေ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            val isThbSaved = com.example.data.isThbCurrencyString(savedReceipt.currency)
                            val primaryAmount = if (isThbSaved) {
                                PosViewModel.formatCurrency(calc.effectiveTotalThb, "THB")
                            } else {
                                PosViewModel.formatKyatMm(calc.effectiveTotalMmk)
                            }
                            val thbColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                            val mmkColor = if (isDark) CashGreenDark else CashGreen
                            Text(
                                text = primaryAmount,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isThbSaved) thbColor else mmkColor
                            )
                            val altText = if (isThbSaved) {
                                "= ${PosViewModel.formatKyatMm(calc.effectiveTotalMmk)}"
                            } else {
                                "= ${PosViewModel.formatCurrency(calc.effectiveTotalThb, "THB")}"
                            }
                            Text(
                                text = altText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isThbSaved) mmkColor else thbColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons Row (View Receipt, Print, Share, Restore, Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // View receipt button
                        OutlinedButton(
                            onClick = onViewReceipt,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ပြေစာကြည့်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Print
                        OutlinedIconButton(
                            onClick = onPrint,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = "Print",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Share
                        OutlinedIconButton(
                            onClick = onShare,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Restore
                        if (showActions) {
                            OutlinedIconButton(
                                onClick = onRestore,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Restore,
                                    contentDescription = "Restore",
                                    tint = if (isDark) CashGreenDark else Color(0xFF00602F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Delete
                        if (showActions) {
                            OutlinedIconButton(
                                onClick = onDelete,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
