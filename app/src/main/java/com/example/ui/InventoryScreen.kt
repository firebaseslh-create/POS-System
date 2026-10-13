package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.ShopProfile
import com.example.data.entity.Product
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenDark
import com.example.util.ProductImageHelper
import com.example.util.ReceiptCalculator
import java.io.File

fun saveImageToInternalStorage(context: Context, uri: Uri): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return uri.toString()
        val dir = File(context.filesDir, "product_images")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "prod_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        val fileUri = Uri.fromFile(file).toString()
        ProductImageHelper.encodeImageFileOrUriToDataUrl(context, fileUri, maxSizePx = 320, quality = 75) ?: fileUri
    } catch (e: Exception) {
        uri.toString()
    }
}

@Composable
fun InventoryScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val showOuterContainerBox by viewModel.showOuterContainerBox.collectAsStateWithLifecycle()
    val showAddProductButton by viewModel.showAddProductButton.collectAsStateWithLifecycle()
    val showProfitInfo by viewModel.showProfitInfo.collectAsStateWithLifecycle()
    val cashierProductHideOverrides by viewModel.cashierProductHideOverrides.collectAsStateWithLifecycle()
    val cashierCardDefaultHide by viewModel.cashierCardDefaultHide.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    val unsoldStockSummary = remember(allProducts, shopProfile) {
        ReceiptCalculator.calculateUnsoldStock(allProducts, shopProfile)
    }
    val totalStockValueMmk = unsoldStockSummary.unsoldSellingValueMmk
    val totalStockValueThb = unsoldStockSummary.unsoldSellingValueThb
    val totalBuyingValueMmk = unsoldStockSummary.unsoldBuyingValueMmk
    val totalBuyingValueThb = unsoldStockSummary.unsoldBuyingValueThb
    val potentialProfitMmk = unsoldStockSummary.potentialProfitMmk
    val potentialProfitThb = unsoldStockSummary.potentialProfitThb

    val nativeMmkStockValue = unsoldStockSummary.nativeMmkSellingValue
    val nativeThbStockValue = unsoldStockSummary.nativeThbSellingValue

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("အားလုံး") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showStockValueDetails by remember { mutableStateOf(false) }

    val categories = remember(allProducts) {
        listOf("အားလုံး") + allProducts.map { it.category }.distinct()
    }

    val filteredProducts = remember(allProducts, selectedCategory, searchQuery) {
        allProducts.filter { product ->
            val matchCategory = selectedCategory == "အားလုံး" || product.category == selectedCategory
            val matchSearch = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.barcode.contains(searchQuery, ignoreCase = true) ||
                    product.category.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Inventory Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ပစ္စည်းစုစုပေါင်း", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${allProducts.size} မျိုး",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lowStockProducts.isNotEmpty()) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("လက်ကျန်နည်း", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (lowStockProducts.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${lowStockProducts.size} မျိုး",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lowStockProducts.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock Value Banner Card (Calculated using MMK = THB rates)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showStockValueDetails = !showStockValueDetails }
                    .testTag("stock_value_banner_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "မြန်မာကျပ် စတော့တန်ဖိုး",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = PosViewModel.formatKyatMm(nativeMmkStockValue),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) CashGreenDark else CashGreen
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "ထိုင်းဘတ် စတော့တန်ဖိုး",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = PosViewModel.formatCurrency(nativeThbStockValue, "THB"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { showStockValueDetails = !showStockValueDetails },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (showStockValueDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showStockValueDetails) "ပိတ်ရန်" else "အသေးစိတ်ကြည့်ရန်",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = showStockValueDetails) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // ခန့်မှန်း စတော့တန်ဖိုး စုစုပေါင်း Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "ခန့်မှန်း စတော့တန်ဖိုး စုစုပေါင်း",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = PosViewModel.formatKyatMm(totalStockValueMmk),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) CashGreenDark else CashGreen
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "ဘတ်ငွေတန်ဖိုး",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = PosViewModel.formatCurrency(totalStockValueThb, "THB"),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                    )

                                    // ဝယ်ရင်းတန်ဖိုး Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "ဝယ်ရင်းတန်ဖိုး",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = PosViewModel.formatKyatMm(totalBuyingValueMmk),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) CashGreenDark else CashGreen
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "ဘတ်ငွေတန်ဖိုး",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = PosViewModel.formatCurrency(totalBuyingValueThb, "THB"),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                                            )
                                        }
                                    }

                                    if (showProfitInfo) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(0.5.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                        )

                                        // ခန့်မှန်းအမြတ် Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "ခန့်မှန်းအမြတ်",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = PosViewModel.formatKyatMm(potentialProfitMmk),
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) CashGreenDark else Color(0xFF00602F)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "ဘတ်ငွေတန်ဖိုး",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = PosViewModel.formatCurrency(potentialProfitThb, "THB"),
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (unsoldStockSummary.mmkSellingRate.isNotBlank() || unsoldStockSummary.mmkBuyingRate.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "၁ ကျပ် ရောင်းဈေး = ${unsoldStockSummary.mmkSellingRate.ifBlank { "-" }} ဘတ် (MMK->THB)  |  ၁ ကျပ် ဝယ်ဈေး = ${unsoldStockSummary.mmkBuyingRate.ifBlank { "-" }} ဘတ် (THB->MMK)",
                                            fontSize = 9.5.sp,
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
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "ပစ္စည်းအမည် / ဘားကုဒ် ရှာပါ...",
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.basicMarquee()
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inventory_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
                                fontSize = 12.sp,
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
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != "အားလုံး")
                                "ရှာဖွေမှုနှင့် ကိုက်ညီသော ပစ္စည်းမရှိပါ"
                            else
                                "ပစ္စည်းစာရင်း မရှိသေးပါ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ပစ္စည်းအသစ် ထည့်သွင်းရန် ညာဘက်အောက်ရှိ + ခလုတ်ကို နှိပ်ပါ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (searchQuery.isBlank() && selectedCategory == "အားလုံး") {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.loadSampleProducts() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("load_sample_products_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ပစ္စည်းစာရင်းများ ထည့်သွင်းမည်", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                // Products List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredProducts.forEach { product ->
                        androidx.compose.runtime.key(product.id) {
                            ProductInventoryCard(
                                product = product,
                                shopProfile = shopProfile,
                                showOuterContainerBox = showOuterContainerBox,
                                showProfitInfo = showProfitInfo,
                                onToggleQuickStockAdjustHide = {
                                    viewModel.toggleShowOuterContainerBox(!showOuterContainerBox)
                                },
                                onEdit = {
                                    productToEdit = product
                                    showAddEditDialog = true
                                },
                                onDelete = {
                                    productToDelete = product
                                },
                                onAdjustStock = { delta ->
                                    viewModel.adjustStock(product.id, delta)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Add Product Floating Action Button
        FloatingActionButton(
            onClick = {
                productToEdit = null
                showAddEditDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_product_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product")
        }
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            product = productToEdit,
            existingCategories = categories.filter { it != "အားလုံး" },
            existingProducts = allProducts,
            showProfitInfo = showProfitInfo,
            onDismiss = { showAddEditDialog = false },
            onSave = { savedProduct ->
                viewModel.saveProduct(savedProduct)
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("ပစ္စည်းဖျက်ရန် သေချာပါသလား?") },
            text = { Text("'${productToDelete?.name}' အား စာရင်းမှ ဖျက်ပစ်ပါမည်။") },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.let { viewModel.deleteProduct(it) }
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ဖျက်မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("မလုပ်တော့ပါ")
                }
            }
        )
    }
}

@Composable
fun ProductInventoryCard(
    product: Product,
    shopProfile: ShopProfile = ShopProfile(),
    showOuterContainerBox: Boolean = true,
    showProfitInfo: Boolean = true,
    onToggleQuickStockAdjustHide: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: (Int) -> Unit
) {
    val isLowStock = product.stockQuantity <= product.lowStockThreshold
    val isDark = isSystemInDarkTheme()
    var showItemDetails by remember { mutableStateOf(false) }
    var showQuickStockAdjust by remember { mutableStateOf(false) }

    val thbSell = shopProfile.thbSellingRate.replace(",", ".").toDoubleOrNull() ?: 0.0
    val thbBuy = shopProfile.thbBuyingRate.replace(",", ".").toDoubleOrNull() ?: thbSell
    val mmkSell = shopProfile.mmkSellingRate.replace(",", ".").toDoubleOrNull()
        ?: (if (thbSell > 0.0) 1.0 / thbSell else 0.0)
    val mmkBuy = shopProfile.mmkBuyingRate.replace(",", ".").toDoubleOrNull()
        ?: (if (thbBuy > 0.0) 1.0 / thbBuy else mmkSell)

    val mmkToThbRate = if (mmkSell > 0.0) com.example.data.toMmkToThbRate(mmkSell) else if (thbSell > 0.0) (1.0 / com.example.data.toThbToMmkRate(thbSell)) else 0.0
    val thbToMmkRate = if (mmkBuy > 0.0) com.example.data.toThbToMmkRate(mmkBuy) else if (thbBuy > 0.0) com.example.data.toThbToMmkRate(thbBuy) else 0.0
    val isThbItem = com.example.data.isThbProduct(product)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_item_${product.id}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    val resolvedImageModel = remember(product.imageUri) {
                        ProductImageHelper.resolveImageModelForCoil(context, product.imageUri)
                    }
                    if (resolvedImageModel != null) {
                        AsyncImage(
                            model = resolvedImageModel,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = product.category,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .basicMarquee()
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (product.barcode.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ကုဒ်: ${product.barcode}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showItemDetails = !showItemDetails },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("inventory_item_expand_${product.id}")
                    ) {
                        Icon(
                            imageVector = if (showItemDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showItemDetails) "ပိတ်ရန်" else "အသေးစိတ်ကြည့်ရန်",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { showQuickStockAdjust = !showQuickStockAdjust },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("inventory_item_cashier_hide_${product.id}")
                    ) {
                        Icon(
                            imageVector = if (!showQuickStockAdjust) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (!showQuickStockAdjust) "အရေအတွက် အမြန်ပြင် ပြသရန် (Show)" else "အရေအတွက် အမြန်ပြင် ဝှက်ရန် (Hide)",
                            tint = if (!showQuickStockAdjust) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing & Stock Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    val retailProfit = product.sellingPrice - product.costPrice
                    val itemPriceColor = if (isThbItem) (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)) else (if (isDark) CashGreenDark else CashGreen)
                    val convertedSellText = if (isThbItem) {
                        if (thbToMmkRate > 0.0) " (= ${PosViewModel.formatKyatMm(Math.round(product.sellingPrice.toDouble() * thbToMmkRate))})" else ""
                    } else {
                        if (mmkToThbRate > 0.0) " (= ${PosViewModel.formatCurrency(Math.round(product.sellingPrice.toDouble() * mmkToThbRate), "THB")})" else ""
                    }
                    Text(
                        text = "အရောင်းဈေး : ${PosViewModel.formatCurrency(product.sellingPrice, product.currency)}$convertedSellText",
                        fontWeight = FontWeight.Bold,
                        color = itemPriceColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )

                    AnimatedVisibility(visible = showItemDetails) {
                        Column(
                            modifier = Modifier.padding(top = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (showProfitInfo) {
                                val profitColor = if (retailProfit < 0) {
                                    MaterialTheme.colorScheme.error
                                } else if (isThbItem) {
                                    if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                                } else {
                                    if (isDark) CashGreenDark else Color(0xFF00602F)
                                }
                                Text(
                                    text = "(လက်လီအမြတ်: ${if (retailProfit >= 0) "+${PosViewModel.formatCurrencyShort(retailProfit, product.currency)}" else PosViewModel.formatCurrencyShort(retailProfit, product.currency)})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = profitColor,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }

                            if (product.wholesalePrice > 0L) {
                                val wholesaleProfit = product.wholesalePrice - product.costPrice
                                val convertedWholesaleText = if (isThbItem) {
                                    if (thbToMmkRate > 0.0) " (= ${PosViewModel.formatKyatMm(Math.round(product.wholesalePrice.toDouble() * thbToMmkRate))})" else ""
                                } else {
                                    if (mmkToThbRate > 0.0) " (= ${PosViewModel.formatCurrency(Math.round(product.wholesalePrice.toDouble() * mmkToThbRate), "THB")})" else ""
                                }
                                Text(
                                    text = "လက်ကားဈေး : ${PosViewModel.formatCurrency(product.wholesalePrice, product.currency)}$convertedWholesaleText",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = itemPriceColor,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                if (showProfitInfo) {
                                    val wsProfitColor = if (wholesaleProfit < 0) {
                                        MaterialTheme.colorScheme.error
                                    } else if (isThbItem) {
                                        if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                                    } else {
                                        if (isDark) CashGreenDark else Color(0xFF00602F)
                                    }
                                    Text(
                                        text = "(လက်ကားအမြတ်: ${if (wholesaleProfit >= 0) "+${PosViewModel.formatCurrencyShort(wholesaleProfit, product.currency)}" else PosViewModel.formatCurrencyShort(wholesaleProfit, product.currency)})",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = wsProfitColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                            }

                            val convertedCostText = if (isThbItem) {
                                if (thbToMmkRate > 0.0) " (= ${PosViewModel.formatKyatMm(Math.round(product.costPrice.toDouble() * thbToMmkRate))})" else ""
                            } else {
                                if (mmkToThbRate > 0.0) " (= ${PosViewModel.formatCurrency(Math.round(product.costPrice.toDouble() * mmkToThbRate), "THB")})" else ""
                            }
                            Text(
                                text = "ဝယ်ရင်းဈေး : ${PosViewModel.formatCurrencyShort(product.costPrice, product.currency)}$convertedCostText",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Current Stock Indicator
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isLowStock) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "လက်ကျန်: ${product.stockQuantity} ${product.unit}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = if (isLowStock) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (showOuterContainerBox && showQuickStockAdjust) {
                Spacer(modifier = Modifier.height(10.dp))

                // Quick Stock Adjust Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "အရေအတွက် အမြန်ပြင် :",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onAdjustStock(-1) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = "-1",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            OutlinedButton(
                                onClick = { onAdjustStock(5) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = "+5",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            OutlinedButton(
                                onClick = { onAdjustStock(10) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                                    contentColor = MaterialTheme.colorScheme.secondary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = "+10",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    product: Product?,
    existingCategories: List<String>,
    existingProducts: List<Product> = emptyList(),
    showProfitInfo: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var costPrice by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var sellingPrice by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "") }
    var wholesalePrice by remember { mutableStateOf(product?.wholesalePrice?.toString() ?: "") }
    var lowStockThreshold by remember { mutableStateOf(product?.lowStockThreshold?.toString() ?: "") }
    var stockQuantity by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(product?.unit ?: "") }
    var currency by remember { mutableStateOf(product?.currency ?: "MMK") }
    var imageUri by remember { mutableStateOf(product?.imageUri ?: "") }
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var tempUrlInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var currencyExpanded by remember { mutableStateOf(false) }
    var showCameraScanDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = saveImageToInternalStorage(context, uri)
            imageUri = saved
        }
    }

    val isEdit = product != null
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val fieldShape = RoundedCornerShape(10.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )
    val inputTextStyle = LocalTextStyle.current.copy(fontSize = 12.sp)

    fun saveProductIfValid() {
        if (name.isNotBlank() && sellingPrice.isNotBlank()) {
            keyboardController?.hide()
            focusManager.clearFocus()
            val toSave = (product ?: Product(name = name, category = category)).copy(
                name = name.trim(),
                category = category.trim().ifBlank { "အထွေထွေ" },
                barcode = barcode.trim(),
                costPrice = costPrice.toLongOrNull() ?: 0L,
                sellingPrice = sellingPrice.toLongOrNull() ?: 0L,
                wholesalePrice = wholesalePrice.toLongOrNull() ?: 0L,
                lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 5,
                stockQuantity = stockQuantity.toIntOrNull() ?: 0,
                unit = unit.trim().ifBlank { "ခု" },
                currency = currency,
                imageUri = imageUri.trim()
            )
            onSave(toSave)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Sticky Top Header (Never moves up/down when keyboard opens)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Box {
                            Surface(
                                onClick = { currencyExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = currency,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Currency",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = currencyExpanded,
                                onDismissRequest = { currencyExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "MMK - (Myanmar Kyat)",
                                            fontSize = 12.sp,
                                            fontWeight = if (currency == "MMK") FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        currency = "MMK"
                                        currencyExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "THB - (Thai Baht)",
                                            fontSize = 12.sp,
                                            fontWeight = if (currency == "THB") FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        currency = "THB"
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }

                        Text(
                            text = if (isEdit) "ပစ္စည်းပြင်ဆင်မည်" else "ပစ္စည်းအသစ် ထည့်သွင်းမည်",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee()
                        )

                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                onDismiss()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Scrollable Form Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Product Image Selector Card (Compact)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "ပစ္စည်း ဓာတ်ပုံ (Product Image)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val previewModel = remember(imageUri) {
                                    ProductImageHelper.resolveImageModelForCoil(context, imageUri)
                                }
                                if (previewModel != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        AsyncImage(
                                            model = previewModel,
                                            contentDescription = "Product Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { imageUri = "" },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(26.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove Image",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(68.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                            .clickable {
                                                imagePickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.AddAPhoto,
                                                contentDescription = "Add Photo",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "ဓာတ်ပုံ ရွေးချယ်ရန် နှိပ်ပါ",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            imagePickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Gallery မှ", fontSize = 10.sp, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            tempUrlInput = imageUri
                                            showUrlInputDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("URL လင့်ခ်", fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            textStyle = inputTextStyle,
                            label = {
                                Text(
                                    text = "ပစ္စည်းအမည် *",
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            shape = fieldShape,
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_name_input")
                        )

                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            textStyle = inputTextStyle,
                            label = {
                                Text(
                                    text = "အမျိုးအစား (Category)",
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            shape = fieldShape,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Function to auto-generate next SKU based on prefix
                        fun generateNextSku(input: String): String {
                            val rawPrefix = input.trim()
                            val cleanPrefix = if (rawPrefix.isBlank()) {
                                "NTR"
                            } else {
                                val withoutTrailingDigits = rawPrefix.replace(Regex("[0-9]+$"), "")
                                val trimmedDash = withoutTrailingDigits.trimEnd('-')
                                if (trimmedDash.isBlank()) "NTR" else trimmedDash.uppercase()
                            }
                            val prefixWithDash = "$cleanPrefix-"

                            val matchingNumbers = existingProducts.mapNotNull { prod ->
                                val code = prod.barcode.trim()
                                if (code.startsWith(prefixWithDash, ignoreCase = true)) {
                                    code.substring(prefixWithDash.length).toLongOrNull()
                                } else null
                            }

                            val maxNum = matchingNumbers.maxOrNull() ?: 0L
                            val nextNum = maxNum + 1L
                            val formattedNum = nextNum.toString().padStart(6, '0')
                            return "$prefixWithDash$formattedNum"
                        }

                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            textStyle = inputTextStyle,
                            label = {
                                Text(
                                    text = "ဘားကုဒ် / SKU (ရွေးချယ်ရန်)",
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            },
                            placeholder = { Text("ဥပမာ: NTR-000001", fontSize = 10.5.sp) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            showCameraScanDialog = true
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("camera_scan_barcode_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Scan with Camera",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            barcode = generateNextSku(barcode)
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("auto_sku_generate_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = "Auto Generate SKU",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            supportingText = {
                                Text(
                                    text = "💡 ကင်မရာဖြင့်ဖတ်ရန် 📷 သို့မဟုတ် Prefix ရိုက်ပြီး Auto နံပါတ်စဉ်ရယူရန် 🪄 ကိုနှိပ်ပါ",
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.basicMarquee()
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            shape = fieldShape,
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_barcode_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sellingPrice,
                                onValueChange = { sellingPrice = it },
                                textStyle = inputTextStyle,
                                label = {
                                    Text(
                                        text = if (currency == "MMK") "ရောင်းဈေး (Kyat) *" else "ရောင်းဈေး (Baht) *",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Right) }
                                ),
                                singleLine = true,
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_price_input")
                            )
                            OutlinedTextField(
                                value = costPrice,
                                onValueChange = { costPrice = it },
                                textStyle = inputTextStyle,
                                label = {
                                    Text(
                                        text = if (currency == "MMK") "ဝယ်ရင်းဈေး (Kyat) *" else "ဝယ်ရင်းဈေး (Baht) *",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                singleLine = true,
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = wholesalePrice,
                                onValueChange = { wholesalePrice = it },
                                textStyle = inputTextStyle,
                                label = {
                                    Text(
                                        text = if (currency == "MMK") "လက်ကားဈေး (Kyat) *" else "လက်ကားဈေး (Baht) *",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Right) }
                                ),
                                singleLine = true,
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_wholesale_price_input")
                            )
                            OutlinedTextField(
                                value = lowStockThreshold,
                                onValueChange = { lowStockThreshold = it },
                                textStyle = inputTextStyle,
                                label = {
                                    Text(
                                        text = "လက်ကျန်သတ်မှတ်ချက်",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                singleLine = true,
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Live Profit Calculation Preview Banner
                        val costVal = costPrice.toLongOrNull() ?: 0L
                        val sellVal = sellingPrice.toLongOrNull() ?: 0L
                        val wsVal = wholesalePrice.toLongOrNull() ?: 0L
                        if (showProfitInfo && (sellVal > 0L || costVal > 0L || wsVal > 0L)) {
                            val liveRetailProfit = sellVal - costVal
                            val liveWsProfit = if (wsVal > 0L) wsVal - costVal else 0L
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("လက်လီအမြတ်:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = if (liveRetailProfit >= 0) "+${PosViewModel.formatCurrencyShort(liveRetailProfit, currency)}" else PosViewModel.formatCurrencyShort(liveRetailProfit, currency),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (liveRetailProfit >= 0) (if (isDark) CashGreenDark else Color(0xFF00602F)) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                    if (wsVal > 0L) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("လက်ကားအမြတ်:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = if (liveWsProfit >= 0) "+${PosViewModel.formatCurrencyShort(liveWsProfit, currency)}" else PosViewModel.formatCurrencyShort(liveWsProfit, currency),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (liveWsProfit >= 0) (if (isDark) CashGreenDark else Color(0xFF00602F)) else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    if (wsVal > sellVal && sellVal > 0L) {
                                        Text(
                                            text = "သတိပြုရန်: လက်ကားဈေးသည် လက်လီဈေးထက် ပိုများနေပါသည်",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = stockQuantity,
                                onValueChange = { stockQuantity = it },
                                textStyle = inputTextStyle,
                                label = {
                                    Text(
                                        text = "လက်ကျန် အရေအတွက်",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Right) }
                                ),
                                singleLine = true,
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                textStyle = inputTextStyle,
                                label = {
                                    Text(
                                        text = "ယူနစ် (ခု/ထုပ်/ဗူး)",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                ),
                                singleLine = true,
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Sticky Bottom Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                keyboardController?.hide()
                                onDismiss()
                            }
                        ) {
                            Text("မလုပ်တော့ပါ", fontSize = 11.5.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { saveProductIfValid() },
                            enabled = name.isNotBlank() && sellingPrice.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = CashGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("save_product_button")
                        ) {
                            Text(
                                text = "သိမ်းဆည်းမည် (Sheet သိမ်းမည်)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCameraScanDialog) {
        SimpleCameraCaptureDialog(
            onDismiss = { showCameraScanDialog = false },
            onBarcodeCaptured = { capturedCode ->
                barcode = capturedCode
                showCameraScanDialog = false
            }
        )
    }

    if (showUrlInputDialog) {
        AlertDialog(
            onDismissRequest = { showUrlInputDialog = false },
            title = { Text("ဓာတ်ပုံ URL ထည့်ရန်", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = tempUrlInput,
                    onValueChange = { tempUrlInput = it },
                    label = { Text("Image URL (https://...)") },
                    placeholder = { Text("https://example.com/image.jpg", fontSize = 12.sp) },
                    singleLine = true,
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        imageUri = tempUrlInput.trim()
                        showUrlInputDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("ထည့်မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlInputDialog = false }) {
                    Text("မလုပ်တော့ပါ")
                }
            }
        )
    }
}
