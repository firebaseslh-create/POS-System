package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.data.CartItem
import com.example.data.entity.PaymentMethod
import com.example.data.entity.Product
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenDark
import com.example.util.ProductImageHelper
import com.example.ui.theme.NaturalBorderLight
import com.example.ui.theme.NaturalBorderSubtle
import com.example.ui.theme.NaturalOnPrimaryContainer
import com.example.ui.theme.NaturalPastelBlue
import com.example.ui.theme.NaturalPastelPurple
import com.example.ui.theme.NaturalPastelRose
import com.example.ui.theme.NaturalPastelStone
import com.example.ui.theme.NaturalPositiveGreen
import com.example.ui.theme.NaturalPrimary
import com.example.ui.theme.NaturalPrimaryContainer
import com.example.ui.theme.NaturalSecondaryContainer
import com.example.ui.theme.NaturalSurfaceLight
import com.example.ui.theme.NaturalSurfaceVariantLight
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val cartSubtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()
    val cartItemCount by viewModel.cartItemCount.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val heldCarts by viewModel.heldCarts.collectAsStateWithLifecycle()
    val heldOrderEntries by viewModel.heldOrderEntries.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val isWholesaleMode by viewModel.isWholesaleMode.collectAsStateWithLifecycle()
    val showPriceModeCard by viewModel.showPriceModeCard.collectAsStateWithLifecycle()
    val showCartItemCheckbox by viewModel.showCartItemCheckbox.collectAsStateWithLifecycle()
    val showCheckoutDetails by viewModel.showCheckoutDetails.collectAsStateWithLifecycle()
    val showProfitInfo by viewModel.showProfitInfo.collectAsStateWithLifecycle()
    val cashierProductHideOverrides by viewModel.cashierProductHideOverrides.collectAsStateWithLifecycle()
    val cashierCardDefaultHide by viewModel.cashierCardDefaultHide.collectAsStateWithLifecycle()
    val thbSellingRate by viewModel.thbSellingRate.collectAsStateWithLifecycle()
    val thbBuyingRate by viewModel.thbBuyingRate.collectAsStateWithLifecycle()
    val mmkSellingRate by viewModel.mmkSellingRate.collectAsStateWithLifecycle()
    val mmkBuyingRate by viewModel.mmkBuyingRate.collectAsStateWithLifecycle()

    var showCartModal by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var showHeldOrdersDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }

    val checkoutSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Categories extraction
    val categories = remember(products) {
        listOf("အားလုံး") + products.map { it.category }.distinct()
    }

    // Filtered products
    val filteredProducts = remember(products, selectedCategory, searchQuery) {
        products.filter { product ->
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

            // Search Bar & Parked/Hold Orders Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "ပစ္စည်းအမည် / ဘားကုဒ် ရှာပါ...",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.basicMarquee()
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
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
                        .weight(1f)
                        .testTag("search_product_input")
                )

                if (heldCarts.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    BadgedBox(
                        badge = {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("${heldCarts.size}") }
                        }
                    ) {
                        IconButton(
                            onClick = { showHeldOrdersDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                .testTag("held_orders_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Held Orders",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showQrScannerDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            .testTag("qr_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR/Barcode",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (showPriceModeCard) {
                Spacer(modifier = Modifier.height(10.dp))

                // Wholesale / Retail Sale Mode Toggle Card Layout (Balanced Inner Spacing & Alignment)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isWholesaleMode) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isWholesaleMode) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleWholesaleMode() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isWholesaleMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (isWholesaleMode) "ကား" else "လီ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isWholesaleMode) "လက်ကားဈေးနှုန်း စနစ်" else "လက်လီဈေးနှုန်း စနစ်",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false).basicMarquee()
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleWholesaleMode() },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("toggle_wholesale_mode")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Toggle Wholesale Mode",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategory(category) },
                        label = {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
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

            Spacer(modifier = Modifier.height(8.dp))

            // Products Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ပစ္စည်း ရှာမတွေ့ပါ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank() && selectedCategory == "အားလုံး") {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.loadSampleProducts() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("cashier_load_sample_products_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ပစ္စည်းစာရင်းများ ထည့်သွင်းမည်", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredProducts.chunked(2).forEach { rowProducts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowProducts.forEach { product ->
                                Box(modifier = Modifier.weight(1f)) {
                                    androidx.compose.runtime.key(product.id) {
                                        val cartItem = cart.find { it.product.id == product.id }
                                        val inCartQty = cartItem?.quantity ?: 0
                                        val isHiddenOnCashier = cashierProductHideOverrides[product.id] ?: cashierCardDefaultHide

                                        ProductGridCard(
                                            product = product,
                                            inCartQty = inCartQty,
                                            onAddToCart = { viewModel.addToCart(product) },
                                            isWholesaleMode = isWholesaleMode,
                                            showPriceModeCard = showPriceModeCard,
                                            showProfitInfo = showProfitInfo,
                                            hideExtraDetails = isHiddenOnCashier,
                                            thbSellingRate = thbSellingRate,
                                            thbBuyingRate = thbBuyingRate,
                                            mmkSellingRate = mmkSellingRate,
                                            mmkBuyingRate = mmkBuyingRate
                                        )
                                    }
                                }
                            }
                            if (rowProducts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(if (cart.isNotEmpty()) 90.dp else 16.dp))
                }
            }
        }

        // Floating Bottom Cart Bar
        AnimatedVisibility(
            visible = cart.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cart Info (Tap to view cart details)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showCartModal = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("$cartItemCount")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        val thbSell = thbSellingRate.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val thbBuy = thbBuyingRate.replace(",", ".").toDoubleOrNull() ?: thbSell
                        val mmkSell = mmkSellingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbSell > 0.0) 1.0 / thbSell else 0.0)
                        val mmkBuy = mmkBuyingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbBuy > 0.0) 1.0 / thbBuy else mmkSell)

                        val rateForMmk = if (mmkSell > 0.0) mmkSell else thbSell
                        val rateForThb = if (mmkBuy > 0.0) mmkBuy else thbBuy

                        // Grand totals for bar (MMK uses မြန်မာကျပ်ရောင်းဈေး, THB uses မြန်မာကျပ်ဝယ်ဈေး)
                        val barTotalMmk = cart.sumOf { it.getSubtotalMmk(rateForMmk) }
                        val barTotalThb = cart.sumOf { it.getSubtotalThb(rateForThb) }

                        val barProfitMmk = cart.sumOf { it.getProfitMmk(rateForMmk) }
                        val barProfitThb = cart.sumOf { it.getProfitThb(rateForThb) }
                        val isDark = isSystemInDarkTheme()
                        val mmkBarColor = if (isDark) CashGreenDark else CashGreen
                        val thbBarColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                        val isFirstItemThb = cart.firstOrNull()?.isThbItem == true

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ကျသင့်ငွေ စုစုပေါင်း",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            if (isFirstItemThb) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = PosViewModel.formatCurrency(barTotalThb, "THB"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = thbBarColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = PosViewModel.formatCurrency(barTotalMmk, "MMK"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = mmkBarColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = PosViewModel.formatCurrency(barTotalMmk, "MMK"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = mmkBarColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = PosViewModel.formatCurrency(barTotalThb, "THB"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = thbBarColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                            }
                        }
                    }

                    // Hold & Checkout Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { viewModel.holdCurrentCart() },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .testTag("hold_cart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Hold Cart",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showCheckoutSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("checkout_main_button")
                        ) {
                            Text("ငွေရှင်းမည်", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Full Cart Modal BottomSheet
    if (showCartModal) {
        ModalBottomSheet(
            onDismissRequest = { showCartModal = false },
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val accentGreen = if (isSystemInDarkTheme()) CashGreenDark else CashGreen
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = accentGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ရွေးချယ်ထားသော ပစ္စည်းများ ($cartItemCount ခု)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                    val showCartWholesaleCheckmark by viewModel.showCartWholesaleCheckmark.collectAsStateWithLifecycle()

                    if (showCartWholesaleCheckmark) {
                        IconButton(onClick = { viewModel.toggleAllCartItemsWholesale() }) {
                            Icon(Icons.Default.Check, contentDescription = "Toggle All Wholesale")
                        }
                    }
                    IconButton(onClick = { showCartModal = false }) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(cart, key = { it.product.id }) { item ->
                        CartItemRow(
                            item = item,
                            showCheckbox = showCartItemCheckbox,
                            showProfitInfo = false,
                            onIncrease = { viewModel.addToCart(item.product) },
                            onDecrease = { viewModel.decreaseOrRemoveFromCart(item.product) },
                            onRemove = { viewModel.removeFromCart(item.product.id) },
                            onToggleWholesale = { viewModel.toggleItemWholesale(item.product.id) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val thbSell = thbSellingRate.replace(",", ".").toDoubleOrNull() ?: 0.0
                val thbBuy = thbBuyingRate.replace(",", ".").toDoubleOrNull() ?: thbSell
                val mmkSell = mmkSellingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbSell > 0.0) 1.0 / thbSell else 0.0)
                val mmkBuy = mmkBuyingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbBuy > 0.0) 1.0 / thbBuy else mmkSell)

                val rateForMmk = if (mmkSell > 0.0) mmkSell else thbSell
                val rateForThb = if (mmkBuy > 0.0) mmkBuy else thbBuy

                // Grand totals in both currencies (MMK uses မြန်မာကျပ်ရောင်းဈေး, THB uses မြန်မာကျပ်ဝယ်ဈေး)
                val grandTotalMmk = cart.sumOf { it.getSubtotalMmk(rateForMmk) }
                val grandTotalThb = cart.sumOf { it.getSubtotalThb(rateForThb) }

                val hasWholesale = cart.any { it.isWholesale }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "စုစုပေါင်း ကုန်ကျငွေ :${if (hasWholesale) " (လက်ကားပါဝင်)" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val isDark = isSystemInDarkTheme()
                    val mmkModalColor = if (isDark) CashGreenDark else CashGreen
                    val thbModalColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                    val isFirstItemThbModal = cart.firstOrNull()?.isThbItem == true
                    if (isFirstItemThbModal) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = PosViewModel.formatCurrency(grandTotalThb, "THB"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = thbModalColor
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = PosViewModel.formatCurrency(grandTotalMmk, "MMK"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = mmkModalColor
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = PosViewModel.formatCurrency(grandTotalMmk, "MMK"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = mmkModalColor
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = PosViewModel.formatCurrency(grandTotalThb, "THB"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = thbModalColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearCart()
                            showCartModal = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_cart_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ရှင်းမည်")
                    }

                    Button(
                        onClick = {
                            showCartModal = false
                            showCheckoutSheet = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier
                            .weight(2f)
                            .testTag("cart_proceed_checkout_button")
                    ) {
                        Text("ငွေရှင်းကောင်တာသို့", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Checkout Bottom Sheet
    if (showCheckoutSheet) {
        CheckoutSheet(
            cart = cart,
            subtotal = cartSubtotal,
            thbSellingRate = thbSellingRate,
            thbBuyingRate = thbBuyingRate,
            mmkSellingRate = mmkSellingRate,
            mmkBuyingRate = mmkBuyingRate,
            sheetState = checkoutSheetState,
            showDetails = showCheckoutDetails,
            showProfitInfo = showProfitInfo,
            onDismiss = { showCheckoutSheet = false },
            onConfirmCheckout = { discount, paymentMethod, amountTendered, customerName, customerPhone, note, checkoutCurrency, totalMmk, totalThb, rateUsed ->
                coroutineScope.launch {
                    viewModel.checkout(
                        discount = discount,
                        paymentMethod = paymentMethod,
                        amountTendered = amountTendered,
                        customerName = customerName,
                        customerPhone = customerPhone,
                        note = note,
                        currency = checkoutCurrency,
                        totalMmk = totalMmk,
                        totalThb = totalThb,
                        exchangeRate = rateUsed,
                        onSuccess = {
                            showCheckoutSheet = false
                            showCartModal = false
                        }
                    )
                }
            }
        )
    }

    // Barcode / QR Scanner Dialog (Live Camera Preview & Fast Scanner)
    val cashierContext = androidx.compose.ui.platform.LocalContext.current
    if (showQrScannerDialog) {
        BarcodeScannerDialog(
            products = products,
            onDismiss = { showQrScannerDialog = false },
            onBarcodeScanned = { product ->
                viewModel.addToCart(product)
                android.widget.Toast.makeText(
                    cashierContext,
                    "ခြင်းတောင်းထဲ ထည့်ပြီးပါပြီ - ${product.name}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showQrScannerDialog = false
            }
        )
    }

    // Parked/Held Orders Dialog
    if (showHeldOrdersDialog) {
        ModalBottomSheet(onDismissRequest = { showHeldOrdersDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "ဆိုင်းငံ့ထားသော အော်ဒါများ (Held Orders)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn {
                    items(heldCarts.indices.toList()) { index ->
                        val held = heldCarts[index]
                        val entry = heldOrderEntries.getOrNull(index)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    viewModel.recallHeldCart(index)
                                    showHeldOrdersDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    val titleText = if (entry?.isRefundedOrder == true) {
                                        val inv = entry.invoiceNumber?.takeIf { it.isNotBlank() }
                                        if (inv != null) {
                                            "အော်ဒါ #${index + 1} - $inv (${held.sumOf { it.quantity }} ခု)"
                                        } else {
                                            "အော်ဒါ #${index + 1} (${held.sumOf { it.quantity }} ခု)"
                                        }
                                    } else {
                                        "အော်ဒါ #${index + 1} (${held.sumOf { it.quantity }} မျိုး)"
                                    }
                                    Text(titleText, fontWeight = FontWeight.Bold)
                                    if (entry?.isRefundedOrder == true) {
                                        Text(
                                            text = "ငွေပြန်အမ်းထားသော ပြေစာ",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    val itemsPreview = held.joinToString(", ") { "${it.product.name} x${it.quantity}" }
                                    if (itemsPreview.isNotBlank()) {
                                        Text(
                                            text = itemsPreview,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    val mmkSum = held.filter { !com.example.data.isThbCurrencyString(it.product.currency) }.sumOf { it.subtotal }
                                    val thbSum = held.filter { com.example.data.isThbCurrencyString(it.product.currency) }.sumOf { it.subtotal }
                                    val heldTotalText = buildString {
                                        if (mmkSum > 0) append(PosViewModel.formatKyatMm(mmkSum))
                                        if (thbSum > 0) {
                                            if (isNotEmpty()) append(" + ")
                                            append(PosViewModel.formatCurrency(thbSum, "THB"))
                                        }
                                        if (isEmpty()) append("0 ကျပ်")
                                    }
                                    val heldTotalColor = if (isSystemInDarkTheme()) CashGreenDark else Color(0xFF00602F)
                                    Text(heldTotalText, color = heldTotalColor, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.recallHeldCart(index)
                                        showHeldOrdersDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("ပြန်လည်ဖွင့်မည်")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGridCard(
    product: Product,
    inCartQty: Int,
    onAddToCart: () -> Unit,
    isWholesaleMode: Boolean,
    showPriceModeCard: Boolean = true,
    showProfitInfo: Boolean = true,
    hideExtraDetails: Boolean = true,
    thbSellingRate: String,
    thbBuyingRate: String,
    mmkSellingRate: String = "",
    mmkBuyingRate: String = ""
) {
    val isOutOfStock = product.stockQuantity <= 0
    val isLowStock = product.stockQuantity in 1..5
    val hasWholesalePrice = product.wholesalePrice > 0L
    val isWholesaleEffective = isWholesaleMode && hasWholesalePrice
    val displayPrice = if (isWholesaleEffective) product.wholesalePrice else product.sellingPrice
    val unitProfit = displayPrice - product.costPrice

    val thbSell = thbSellingRate.replace(",", ".").toDoubleOrNull() ?: 0.0
    val thbBuy = thbBuyingRate.replace(",", ".").toDoubleOrNull() ?: thbSell
    val mmkSell = mmkSellingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbSell > 0.0) 1.0 / thbSell else 0.0)
    val mmkBuy = mmkBuyingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbBuy > 0.0) 1.0 / thbBuy else mmkSell)

    val isThbCurrency = com.example.data.isThbProduct(product)
    
    var mmkPrice = 0L
    var thbPrice = 0L
    var mmkProfit = 0L
    var thbProfit = 0L

    if (isThbCurrency) {
        thbPrice = displayPrice
        val thbToMmk = if (mmkSell > 0.0) com.example.data.toThbToMmkRate(mmkSell) else thbSell
        mmkPrice = if (thbToMmk > 0.0) Math.round(displayPrice * thbToMmk) else 0L
        
        thbProfit = unitProfit
        mmkProfit = if (thbToMmk > 0.0) Math.round(unitProfit * thbToMmk) else 0L
    } else {
        mmkPrice = displayPrice
        val mmkToThb = if (mmkBuy > 0.0) com.example.data.toMmkToThbRate(mmkBuy) else (if (thbBuy > 0.0) 1.0 / thbBuy else 0.0)
        thbPrice = if (mmkToThb > 0.0) Math.round(displayPrice * mmkToThb) else 0L
        
        mmkProfit = unitProfit
        thbProfit = if (mmkToThb > 0.0) Math.round(unitProfit * mmkToThb) else 0L
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock, onClick = onAddToCart)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (inCartQty > 0) 1.5.dp else 1.dp,
            color = if (inCartQty > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (inCartQty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // Relative Layout Container (Box overlay)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val resolvedImageModel = remember(product.imageUri) {
                ProductImageHelper.resolveImageModelForCoil(context, product.imageUri)
            }
            val hasPhoto = resolvedImageModel != null

            // Main Text Content Column (Tight spacing, right padding dynamically adjusted for photo availability)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (hasPhoto) 54.dp else 34.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Category & Wholesale/Retail Tag Row (Category hidden when hideExtraDetails is true)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!hideExtraDetails && showProfitInfo) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = product.category,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier
                                    .basicMarquee()
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isWholesaleMode) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (hasWholesalePrice) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (hasWholesalePrice) "လက်ကားဈေးနှုန်း" else "လက်လီသာ",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = if (hasWholesalePrice) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "လက်လီဈေးနှုန်း",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Product Name
                Text(
                    text = product.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                )

                val isDark = isSystemInDarkTheme()
                val mmkItemColor = if (isDark) CashGreenDark else CashGreen
                val thbItemColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)
                // When hideExtraDetails is true (Hide mode):
                // - MMK product shows only MMK (hides THB)
                // - THB product shows only THB (hides MMK)
                // When hideExtraDetails is false (Show mode toggled from Inventory): shows both currencies, profit, and category
                val showMmkCurrency = if (hideExtraDetails) !isThbCurrency else (!isThbCurrency || showPriceModeCard)
                val showThbCurrency = if (hideExtraDetails) isThbCurrency else (isThbCurrency || showPriceModeCard)

                if (showMmkCurrency) {
                    // Kyat (MMK) Price
                    Text(
                        text = PosViewModel.formatCurrency(mmkPrice, "MMK"),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (mmkPrice > 0) mmkItemColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!hideExtraDetails && showProfitInfo) {
                        // Kyat (MMK) Profit
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "အမြတ်: ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val mmkProfitColor = if (mmkProfit > 0) (if (isDark) CashGreenDark else Color(0xFF00602F)) else if (mmkProfit < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            Text(
                                text = if (mmkProfit >= 0) "+${PosViewModel.formatCurrencyShort(mmkProfit, "MMK")}" else PosViewModel.formatCurrencyShort(mmkProfit, "MMK"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = mmkProfitColor
                            )
                        }
                    }
                }

                if (showThbCurrency) {
                    // Baht (THB) Price
                    Text(
                        text = PosViewModel.formatCurrency(thbPrice, "THB"),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (thbPrice > 0) thbItemColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!hideExtraDetails && showProfitInfo) {
                        // Baht (THB) Profit
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "အမြတ်: ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val thbProfitColor = if (thbProfit > 0) (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066B3)) else if (thbProfit < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            Text(
                                text = if (thbProfit >= 0) "+${PosViewModel.formatCurrencyShort(thbProfit, "THB")}" else PosViewModel.formatCurrencyShort(thbProfit, "THB"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = thbProfitColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Stock status
                Text(
                    text = when {
                        isOutOfStock -> "ပစ္စည်းပြတ်"
                        isLowStock -> "လက်ကျန်: ${product.stockQuantity} ${product.unit}"
                        else -> "လက်ကျန်: ${product.stockQuantity} ${product.unit}"
                    },
                    fontSize = 10.5.sp,
                    color = when {
                        isOutOfStock -> MaterialTheme.colorScheme.error
                        isLowStock -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isLowStock || isOutOfStock) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            // Relative Right Action Column (Photo on Top, Plus Button on Bottom)
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Product Photo (46dp size, rounded corner rectangle shape, shifted slightly down and left)
                if (resolvedImageModel != null) {
                    AsyncImage(
                        model = resolvedImageModel,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(top = 10.dp, end = 8.dp)
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    )
                }

                // Plus (+) Button / Cart Quantity Badge
                if (!isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (inCartQty > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (inCartQty > 0) {
                            Text(
                                text = "$inCartQty",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    showCheckbox: Boolean = true,
    showProfitInfo: Boolean = true,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onToggleWholesale: () -> Unit
) {
    val isItemWholesale = item.isWholesale && item.product.wholesalePrice > 0L
    val unitProfit = item.price - item.product.costPrice
    val totalItemProfit = unitProfit * item.quantity

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showCheckbox) {
            androidx.compose.material3.Checkbox(
                checked = item.isWholesale,
                onCheckedChange = { onToggleWholesale() }
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false).basicMarquee()
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (item.isWholesale) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (item.isWholesale) "လက်ကား" else "လက်လီ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isWholesale) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            val itemCurrency = item.product.currency
            val isDark = isSystemInDarkTheme()
            val isThb = itemCurrency == "THB"
            val itemPriceColor = if (isThb) {
                if (isDark) Color(0xFF70B6FF) else Color(0xFF0066B3)
            } else {
                if (isDark) CashGreenDark else Color(0xFF00602F)
            }
            Text(
                text = "${PosViewModel.formatCurrencyShort(item.price, itemCurrency)} × ${item.quantity} = ${PosViewModel.formatCurrency(item.subtotal, itemCurrency)}",
                fontSize = 12.sp,
                color = itemPriceColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            if (showProfitInfo) {
                val profitBadgeColor = if (totalItemProfit < 0) {
                    MaterialTheme.colorScheme.error
                } else if (isThb) {
                    if (isDark) Color(0xFF70B6FF) else Color(0xFF0066B3)
                } else {
                    if (isDark) CashGreenDark else Color(0xFF006D39)
                }
                Text(
                    text = "အမြတ်: ${if (totalItemProfit >= 0) "+${PosViewModel.formatCurrencyShort(totalItemProfit, itemCurrency)}" else PosViewModel.formatCurrencyShort(totalItemProfit, itemCurrency)} (${if (item.isWholesale) "လက်ကား" else "လက်လီ"})",
                    fontSize = 11.sp,
                    color = profitBadgeColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrease,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                    contentDescription = "Decrease",
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "${item.quantity}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            IconButton(
                onClick = onIncrease,
                enabled = item.quantity < item.product.stockQuantity,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
