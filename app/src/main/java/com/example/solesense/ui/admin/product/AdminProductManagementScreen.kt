package com.example.solesense.ui.admin.product

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solesense.data.repository.ProductRepository
import com.example.solesense.model.catalog.Product
import com.example.solesense.model.catalog.Variant
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductManagementScreen(
    adminUid: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val productRepository = remember { ProductRepository() }
    val numFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }

    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, LOW_STOCK, OUT_OF_STOCK, DISABLED

    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var isCreatingNewProduct by remember { mutableStateOf(false) }
    var managingVariantsForProduct by remember { mutableStateOf<Product?>(null) }

    fun refreshProducts() {
        scope.launch {
            isLoading = true
            productRepository.getAllProductsForAdmin().onSuccess { list ->
                products = list
            }.onFailure { e ->
                Toast.makeText(context, "Lỗi tải danh sách sản phẩm: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshProducts()
    }

    val statusTabs = listOf(
        "ALL" to "Tất cả (${products.size})",
        "ACTIVE" to "Đang bán",
        "LOW_STOCK" to "Sắp hết (≤10)",
        "OUT_OF_STOCK" to "Hết hàng",
        "DISABLED" to "Đã ẩn"
    )

    val filteredProducts = remember(products, searchQuery, selectedStatusFilter) {
        val q = searchQuery.trim().lowercase()
        products.filter { p ->
            val matchQuery = q.isEmpty() ||
                    p.name.lowercase().contains(q) ||
                    p.productId.lowercase().contains(q) ||
                    p.brandId.lowercase().contains(q) ||
                    p.categoryId.lowercase().contains(q)
            val st = p.status.uppercase()
            val matchStatus = when (selectedStatusFilter) {
                "ACTIVE" -> st == "ACTIVE" && p.totalStockQuantity > 0
                "LOW_STOCK" -> st != "DISABLED" && p.totalStockQuantity in 1..10
                "OUT_OF_STOCK" -> st == "OUT_OF_STOCK" || (st != "DISABLED" && p.totalStockQuantity <= 0)
                "DISABLED" -> st == "DISABLED"
                else -> true
            }
            matchQuery && matchStatus
        }
    }

    Scaffold(
        containerColor = Color(0xFFF6F7FB),
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sản phẩm & Tồn kho (${products.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            editingProduct = null
                            isCreatingNewProduct = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm SP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Ô tìm kiếm theo tên hoặc mã sản phẩm
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm theo tên giày, mã SP, thương hiệu...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(statusTabs) { (code, label) ->
                    val isSel = selectedStatusFilter == code
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSel) Color(0xFF111827) else Color.White,
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF111827) else Color(0xFFE5E7EB)),
                        modifier = Modifier.clickable { selectedStatusFilter = code }
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) Color.White else Color(0xFF4B5563),
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF111827))
                }
            } else if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy sản phẩm nào phù hợp", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts, key = { it.productId }) { product ->
                        val isDisabled = product.status.uppercase() == "DISABLED"
                        val isOut = product.totalStockQuantity <= 0 || product.status.uppercase() == "OUT_OF_STOCK"
                        val (badgeLabel, badgeBg, badgeTextCol) = when {
                            isDisabled -> Triple("ĐÃ ẨN (DISABLED)", Color(0xFFF3F4F6), Color(0xFF6B7280))
                            isOut -> Triple("HẾT HÀNG", Color(0xFFFEE2E2), Color(0xFFDC2626))
                            product.totalStockQuantity <= 10 -> Triple("SẮP HẾT (${product.totalStockQuantity})", Color(0xFFFEF3C7), Color(0xFFD97706))
                            else -> Triple("ĐANG BÁN (${product.totalStockQuantity})", Color(0xFFDCFCE7), Color(0xFF15803D))
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF3F4F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = product.imageUrls.firstOrNull() ?: "",
                                            contentDescription = product.name,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = product.brandId.uppercase(),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF6B7280)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(badgeBg)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = badgeLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = badgeTextCol
                                                )
                                            }
                                        }

                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF111827),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        val effectivePrice = product.salePrice ?: product.basePrice
                                        Text(
                                            text = "Giá bán: ${numFormat.format(effectivePrice.toLong())} ₫ (Gốc: ${numFormat.format(product.basePrice.toLong())} ₫)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFE53935)
                                        )
                                        Text(
                                            text = "Tồn kho tổng: ${product.totalStockQuantity} đôi • Đã bán: ${product.totalSoldCount} đôi",
                                            fontSize = 12.sp,
                                            color = Color(0xFF4B5563)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFF3F4F6))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { managingVariantsForProduct = product },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(34.dp)
                                    ) {
                                        Text(
                                            text = "Biến thể & Nhập kho",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            editingProduct = product
                                            isCreatingNewProduct = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .weight(0.75f)
                                            .height(34.dp)
                                    ) {
                                        Text("Sửa SP", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                productRepository.toggleProductStatus(
                                                    productId = product.productId,
                                                    currentStatus = product.status,
                                                    totalStock = product.totalStockQuantity
                                                ).onSuccess { newSt ->
                                                    Toast.makeText(
                                                        context,
                                                        "Đã chuyển ${product.name} sang trạng thái $newSt",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    refreshProducts()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (isDisabled) Color(0xFF16A34A) else Color(0xFFDC2626)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (isDisabled) Color(0xFF16A34A) else Color(0xFFDC2626)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .weight(0.75f)
                                            .height(34.dp)
                                    ) {
                                        Text(
                                            text = if (isDisabled) "Mở bán" else "Ẩn SP",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Thêm mới hoặc Sửa Sản phẩm
    if (isCreatingNewProduct || editingProduct != null) {
        val current = editingProduct
        var name by remember(current) { mutableStateOf(current?.name ?: "") }
        var brandId by remember(current) { mutableStateOf(current?.brandId ?: "nike") }
        var categoryId by remember(current) { mutableStateOf(current?.categoryId ?: "running") }
        var basePriceStr by remember(current) { mutableStateOf(current?.basePrice?.toLong()?.toString() ?: "2500000") }
        var salePriceStr by remember(current) { mutableStateOf(current?.salePrice?.toLong()?.toString() ?: "") }
        var description by remember(current) { mutableStateOf(current?.description ?: "") }
        var imageUrl by remember(current) { mutableStateOf(current?.imageUrls?.firstOrNull() ?: "") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    isCreatingNewProduct = false
                    editingProduct = null
                }
            },
            containerColor = Color.White,
            title = {
                Text(
                    text = if (current == null) "Thêm sản phẩm mới" else "Chỉnh sửa sản phẩm",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên mẫu giày *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = brandId,
                            onValueChange = { brandId = it },
                            label = { Text("Mã Hãng (nike/adidas...)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = categoryId,
                            onValueChange = { categoryId = it },
                            label = { Text("Danh mục (running...)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = basePriceStr,
                            onValueChange = { basePriceStr = it },
                            label = { Text("Giá gốc (VNĐ) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = salePriceStr,
                            onValueChange = { salePriceStr = it },
                            label = { Text("Giá KM (Tùy chọn)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("URL Ảnh đại diện sản phẩm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Mô tả chi tiết") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = name.trim()
                        val baseP = basePriceStr.trim().toDoubleOrNull() ?: 0.0
                        val saleP = salePriceStr.trim().toDoubleOrNull()
                        if (cleanName.isBlank() || baseP <= 0.0) {
                            Toast.makeText(context, "Vui lòng nhập Tên sản phẩm và Giá gốc > 0", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (saleP != null && saleP > baseP) {
                            Toast.makeText(context, "Giá khuyến mãi không được vượt quá giá gốc!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            isSaving = true
                            val prodToSave = (current ?: Product()).copy(
                                name = cleanName,
                                brandId = brandId.trim().lowercase(),
                                categoryId = categoryId.trim().lowercase(),
                                basePrice = baseP,
                                salePrice = saleP,
                                description = description.trim(),
                                imageUrls = if (imageUrl.isNotBlank()) listOf(imageUrl.trim()) else (current?.imageUrls ?: emptyList()),
                                status = current?.status ?: "ACTIVE",
                                createdAt = current?.createdAt ?: Timestamp.now()
                            )
                            productRepository.saveOrUpdateProduct(prodToSave).onSuccess { saved ->
                                Toast.makeText(context, "Đã lưu sản phẩm ${saved.name}!", Toast.LENGTH_SHORT).show()
                                isCreatingNewProduct = false
                                editingProduct = null
                                refreshProducts()
                            }.onFailure { e ->
                                Toast.makeText(context, "Lỗi lưu sản phẩm: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                            isSaving = false
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827))
                ) {
                    Text(if (isSaving) "Đang lưu..." else "Lưu sản phẩm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isCreatingNewProduct = false
                    editingProduct = null
                }) {
                    Text("Hủy")
                }
            }
        )
    }

    // BottomSheet Quản lý Biến thể & Nhập/Xuất Tồn kho (Ghi InventoryLog & Đồng bộ Product cha)
    if (managingVariantsForProduct != null) {
        val targetProd = managingVariantsForProduct!!
        var variants by remember(targetProd.productId) { mutableStateOf<List<Variant>>(emptyList()) }
        var isLoadingVariants by remember { mutableStateOf(true) }

        var selectedVariantForStock by remember { mutableStateOf<Variant?>(null) }
        var stockDeltaInput by remember { mutableStateOf("10") }
        var stockReasonInput by remember { mutableStateOf("Nhập bổ sung lô hàng mới") }

        var isAddingVariant by remember { mutableStateOf(false) }
        var newVarColor by remember { mutableStateOf("Trắng") }
        var newVarSize by remember { mutableStateOf("41") }
        var newVarSku by remember { mutableStateOf("") }
        var newVarStock by remember { mutableStateOf("15") }

        fun loadVariants() {
            scope.launch {
                isLoadingVariants = true
                productRepository.getVariantsByProductId(targetProd.productId).onSuccess { list ->
                    variants = list.sortedWith(compareBy({ it.color }, { it.size }))
                }
                isLoadingVariants = false
            }
        }

        LaunchedEffect(targetProd.productId) {
            loadVariants()
        }

        ModalBottomSheet(
            onDismissRequest = {
                managingVariantsForProduct = null
                refreshProducts()
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Biến thể & Kho: ${targetProd.name}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "Mọi thao tác nhập/xuất đều ghi InventoryLog & cập nhật tổng kho",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    TextButton(onClick = { isAddingVariant = !isAddingVariant }) {
                        Text(
                            text = if (isAddingVariant) "Đóng" else "+ Thêm Size/Màu",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E88E5)
                        )
                    }
                }

                if (isAddingVariant) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8F9FC),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Thêm biến thể mới (Chống trùng Màu + Size)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newVarColor,
                                    onValueChange = { newVarColor = it },
                                    label = { Text("Màu sắc") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = newVarSize,
                                    onValueChange = { newVarSize = it },
                                    label = { Text("Size (EU)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f)
                                )
                                OutlinedTextField(
                                    value = newVarStock,
                                    onValueChange = { newVarStock = it },
                                    label = { Text("Tồn kho") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f)
                                )
                            }
                            Button(
                                onClick = {
                                    val initSt = newVarStock.toIntOrNull() ?: 0
                                    val price = targetProd.salePrice ?: targetProd.basePrice
                                    scope.launch {
                                        productRepository.addVariantToProduct(
                                            productId = targetProd.productId,
                                            color = newVarColor,
                                            size = newVarSize,
                                            sku = newVarSku,
                                            price = price,
                                            initialStock = initSt,
                                            imageUrl = targetProd.imageUrls.firstOrNull() ?: "",
                                            adminId = adminUid
                                        ).onSuccess {
                                            Toast.makeText(context, "Đã thêm biến thể & đồng bộ kho!", Toast.LENGTH_SHORT).show()
                                            isAddingVariant = false
                                            loadVariants()
                                            refreshProducts()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.message ?: "Lỗi thêm biến thể", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Lưu biến thể mới", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (selectedVariantForStock != null) {
                    val v = selectedVariantForStock!!
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.2.dp, Color(0xFF16A34A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nhập / Điều chỉnh kho: Màu ${v.color} - Size ${v.size} (Hiện có: ${v.stock})",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF15803D)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = stockDeltaInput,
                                    onValueChange = { stockDeltaInput = it },
                                    label = { Text("Số lượng (+ nhập / - trừ)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = stockReasonInput,
                                    onValueChange = { stockReasonInput = it },
                                    label = { Text("Lý do ghi Log") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { selectedVariantForStock = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Hủy")
                                }
                                Button(
                                    onClick = {
                                        val delta = stockDeltaInput.trim().toIntOrNull() ?: 0
                                        if (delta == 0) {
                                            Toast.makeText(context, "Nhập số lượng khác 0 (VD: 10 hoặc -2)", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        scope.launch {
                                            productRepository.adjustVariantStockWithTransaction(
                                                productId = targetProd.productId,
                                                variantId = v.variantId,
                                                deltaStock = delta,
                                                reason = stockReasonInput,
                                                adminId = adminUid
                                            ).onSuccess { newSt ->
                                                Toast.makeText(
                                                    context,
                                                    "Đã cập nhật tồn kho biến thể thành $newSt đôi & ghi InventoryLog!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                selectedVariantForStock = null
                                                loadVariants()
                                                refreshProducts()
                                            }.onFailure { err ->
                                                Toast.makeText(context, err.message ?: "Lỗi cập nhật kho", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    modifier = Modifier.weight(1.4f)
                                ) {
                                    Text("Xác nhận cập nhật kho", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (isLoadingVariants) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF111827))
                    }
                } else if (variants.isEmpty()) {
                    Text("Sản phẩm này chưa có biến thể nào. Hãy bấm '+ Thêm Size/Màu'.", color = Color.Gray)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(variants, key = { it.variantId }) { v ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF9FAFB),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Màu: ${v.color}  •  Size: ${v.size}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF111827)
                                        )
                                        Text(
                                            text = "SKU: ${v.sku} • Đã bán: ${v.soldCount}",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                        Text(
                                            text = "Tồn kho: ${v.stock} đôi",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (v.stock <= 0) Color(0xFFDC2626) else Color(0xFF15803D)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            selectedVariantForStock = v
                                            stockDeltaInput = "10"
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("+ Nhập / Sửa kho", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
