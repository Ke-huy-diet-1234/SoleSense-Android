package com.example.solesense.ui.cart

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solesense.R
import com.example.solesense.model.order.CartItem
import com.example.solesense.model.order.OrderItem
import com.example.solesense.repository.CartRepository
import com.example.solesense.repository.FavoriteRepository
import com.example.solesense.ui.checkout.CheckoutSessionHolder
import com.example.solesense.ui.components.LoginRequiredDialog
import com.example.solesense.utils.UserSessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateToAuth: () -> Unit = {},
    onNavigateToCheckout: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val isLoggedIn = currentUser != null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cartRepository = remember { CartRepository() }
    val favoriteRepository = remember { FavoriteRepository() }

    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var selectedItemIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isEditMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showLoginRequiredDialog by remember { mutableStateOf(!isLoggedIn) }

    // Trạng thái BottomSheet xác nhận xóa 1 sản phẩm hoặc xóa nhiều sản phẩm đã chọn
    var itemPendingDelete by remember { mutableStateOf<CartItem?>(null) }
    var isConfirmingBatchDelete by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.userId) {
        if (currentUser != null) {
            showLoginRequiredDialog = false
            isLoading = true
            val result = cartRepository.getCartItems(currentUser?.userId)
            val loaded = result.getOrDefault(emptyList())
            cartItems = loaded
            // Mặc định tích chọn tất cả các sản phẩm khi mở giỏ hàng
            selectedItemIds = loaded.map { it.itemId }.toSet()
            isLoading = false
        } else {
            cartItems = emptyList()
            selectedItemIds = emptySet()
            showLoginRequiredDialog = true
        }
    }

    if (!isLoggedIn) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = "Giỏ hàng",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Đăng nhập để xem Giỏ hàng",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Giỏ hàng của bạn được đồng bộ trực tiếp trên tài khoản SoleSense.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF757575)),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showLoginRequiredDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC3824))
                ) {
                    Text("Đăng nhập / Đăng ký", fontWeight = FontWeight.Bold)
                }
            }
        }

        LoginRequiredDialog(
            isVisible = showLoginRequiredDialog,
            onLoginClick = {
                showLoginRequiredDialog = false
                onNavigateToAuth()
            },
            onDismiss = {
                showLoginRequiredDialog = false
            }
        )
        return
    }

    val isAllSelected = cartItems.isNotEmpty() && selectedItemIds.size == cartItems.size
    val selectedCartItems = cartItems.filter { selectedItemIds.contains(it.itemId) }
    val totalAmount = selectedCartItems.sumOf { it.unitPrice * it.quantity }.toLong()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF8F9FA),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Thanh Header chuẩn thiết kế: Logo + Giỏ hàng bên trái | Nút "Sửa" + Icon Tìm kiếm bên phải
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_solesense_logo),
                                contentDescription = "SoleSense Logo",
                                modifier = Modifier.size(26.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (cartItems.isNotEmpty()) "Giỏ hàng (${cartItems.size})" else "Giỏ hàng",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color(0xFF111111)
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (cartItems.isNotEmpty()) {
                                Text(
                                    text = if (isEditMode) "Xong" else "Sửa",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = if (isEditMode) Color(0xFFE53935) else Color(0xFF111111)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { isEditMode = !isEditMode }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }

                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Tìm kiếm trong giỏ hàng", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Tìm kiếm",
                                    tint = Color(0xFF111111),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Thanh "Chọn tất cả" gọn nhẹ ngay dưới Header
                    if (cartItems.isNotEmpty()) {
                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .clickable {
                                    selectedItemIds = if (isAllSelected) {
                                        emptySet()
                                    } else {
                                        cartItems.map { it.itemId }.toSet()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CartCheckbox(
                                    checked = isAllSelected,
                                    onCheckedChange = {
                                        selectedItemIds = if (isAllSelected) {
                                            emptySet()
                                        } else {
                                            cartItems.map { it.itemId }.toSet()
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Chọn tất cả (${selectedItemIds.size}/${cartItems.size})",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF212121)
                                    )
                                )
                            }

                            if (isEditMode && selectedItemIds.isNotEmpty()) {
                                Text(
                                    text = "Đã chọn ${selectedItemIds.size} mục",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFE53935),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    color = Color.White,
                    shadowElevation = 10.dp
                ) {
                    if (isEditMode) {
                        // Chế độ "Sửa": Chuyển vào yêu thích hoặc Xóa các mục đã tích
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (selectedCartItems.isEmpty()) {
                                        Toast.makeText(context, "Vui lòng tích chọn sản phẩm trước", Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }
                                    scope.launch {
                                        val productIds = selectedCartItems.map { it.productId }
                                        favoriteRepository.addFavoritesBatch(productIds)
                                        Toast.makeText(
                                            context,
                                            "Đã chuyển ${selectedCartItems.size} sản phẩm vào danh sách Yêu thích!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isEditMode = false
                                    }
                                },
                                enabled = selectedCartItems.isNotEmpty(),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color(0xFF111111)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF111111)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Lưu vào yêu thích",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Button(
                                onClick = {
                                    if (selectedCartItems.isEmpty()) {
                                        Toast.makeText(context, "Vui lòng tích chọn sản phẩm cần xóa", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isConfirmingBatchDelete = true
                                },
                                enabled = selectedCartItems.isNotEmpty(),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "Xóa (${selectedItemIds.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else {
                        // Chế độ bình thường: Tổng tiền & Nút "Thanh toán →" không bao giờ bị xuống dòng
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "Tổng tiền",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF757575),
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${formatCartVnd(totalAmount)}₫",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF111111),
                                        fontSize = 19.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    if (selectedCartItems.isEmpty()) {
                                        Toast.makeText(context, "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val orderItems = selectedCartItems.map { item ->
                                        OrderItem(
                                            itemId = item.itemId,
                                            productId = item.productId,
                                            variantId = item.variantId,
                                            skuSnapshot = item.variantId,
                                            productName = item.productName,
                                            imageSnapshot = item.imageUrl,
                                            size = item.size,
                                            color = item.color,
                                            unitPrice = item.unitPrice,
                                            quantity = item.quantity,
                                            subtotal = item.unitPrice * item.quantity
                                        )
                                    }
                                    CheckoutSessionHolder.setCartItems(orderItems)
                                    onNavigateToCheckout()
                                },
                                enabled = selectedCartItems.isNotEmpty(),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF111111),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    text = "Thanh toán  →",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF111111))
            }
        } else if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Giỏ hàng của bạn đang trống",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems, key = { it.itemId }) { item ->
                    val isChecked = selectedItemIds.contains(item.itemId)
                    CartItemRowCard(
                        item = item,
                        isChecked = isChecked,
                        onCheckedChange = {
                            selectedItemIds = if (isChecked) {
                                selectedItemIds - item.itemId
                            } else {
                                selectedItemIds + item.itemId
                            }
                        },
                        onProductClick = {
                            if (item.productId.isNotBlank()) {
                                onProductClick(item.productId)
                            }
                        },
                        onDeleteClick = {
                            // Bấm icon xóa -> Hiện BottomSheet hỏi xác nhận xóa giống ảnh mẫu
                            itemPendingDelete = item
                        },
                        onDecreaseClick = {
                            if (item.quantity <= 1) {
                                // Giảm từ 1 về 0 cũng hỏi xác nhận xóa
                                itemPendingDelete = item
                            } else {
                                scope.launch {
                                    cartRepository.updateItemQuantity(
                                        itemId = item.itemId,
                                        newQuantity = item.quantity - 1,
                                        maxStock = item.maxStock,
                                        userId = currentUser?.userId
                                    ).onSuccess { cartItems = it }
                                }
                            }
                        },
                        onIncreaseClick = {
                            // Kiểm tra nghiêm ngặt giới hạn tồn kho (maxStock)
                            if (item.quantity >= item.maxStock) {
                                Toast.makeText(
                                    context,
                                    "Số lượng đạt giới hạn tồn kho (còn tối đa ${item.maxStock} đôi)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                scope.launch {
                                    cartRepository.updateItemQuantity(
                                        itemId = item.itemId,
                                        newQuantity = item.quantity + 1,
                                        maxStock = item.maxStock,
                                        userId = currentUser?.userId
                                    ).onSuccess { cartItems = it }
                                }
                            }
                        }
                    )
                }
            }
        }

        // BottomSheet xác nhận xóa 1 sản phẩm (Chuẩn ảnh thiết kế bên phải)
        if (itemPendingDelete != null) {
            val targetItem = itemPendingDelete!!
            ModalBottomSheet(
                onDismissRequest = { itemPendingDelete = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Xóa sản phẩm",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = Color(0xFF111111)
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Khung preview sản phẩm sắp xóa
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFFAFAFA),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = targetItem.imageUrl,
                                contentDescription = targetItem.productName,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(6.dp),
                                contentScale = ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = targetItem.productName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF111111)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                ColorAndSizeCompactRow(
                                    colorName = targetItem.color,
                                    sizeName = targetItem.size
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatCartVnd(targetItem.unitPrice.toLong())}₫",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.5.sp,
                                            color = Color(0xFF111111)
                                        )
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFFEEEEEE))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "x${targetItem.quantity}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF212121)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { itemPendingDelete = null },
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF0F1F3),
                                contentColor = Color(0xFF111111)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Hủy",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val idToRemove = targetItem.itemId
                                itemPendingDelete = null
                                scope.launch {
                                    cartRepository.removeCartItem(idToRemove, currentUser?.userId)
                                        .onSuccess { updated ->
                                            cartItems = updated
                                            selectedItemIds = selectedItemIds - idToRemove
                                            Toast.makeText(context, "Đã xóa sản phẩm khỏi giỏ hàng", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF111111),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Xác nhận",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // BottomSheet xác nhận xóa nhiều sản phẩm khi ở chế độ "Sửa"
        if (isConfirmingBatchDelete) {
            ModalBottomSheet(
                onDismissRequest = { isConfirmingBatchDelete = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Xóa ${selectedItemIds.size} sản phẩm đã chọn?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF111111)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Các sản phẩm được tích chọn sẽ bị xóa khỏi giỏ hàng của bạn.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF616161)),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { isConfirmingBatchDelete = false },
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF0F1F3),
                                contentColor = Color(0xFF111111)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Hủy", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                val idsToDelete = selectedItemIds.toList()
                                isConfirmingBatchDelete = false
                                scope.launch {
                                    cartRepository.removeMultipleCartItems(idsToDelete, currentUser?.userId)
                                        .onSuccess { updated ->
                                            cartItems = updated
                                            selectedItemIds = emptySet()
                                            isEditMode = false
                                            Toast.makeText(context, "Đã xóa các sản phẩm đã chọn", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF111111),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Xác nhận", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun CartItemRowCard(
    item: CartItem,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    onProductClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDecreaseClick: () -> Unit,
    onIncreaseClick: () -> Unit
) {
    val isAtMaxStock = item.quantity >= item.maxStock

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hộp tích chọn item
            CartCheckbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Ảnh sản phẩm trên nền xám nhạt bo góc
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.productName,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF5F5F7))
                    .clickable { onProductClick() }
                    .padding(6.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Cột thông tin bên phải
            Column(modifier = Modifier.weight(1f)) {
                // Hàng 1: Tên giày + Icon thùng rác góc phải
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF111111)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onProductClick() }
                    )

                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Xóa sản phẩm",
                        tint = Color(0xFF757575),
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { onDeleteClick() }
                            .padding(2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Hàng 2: [Chấm tròn màu] Màu | Size: XX (Gọn gàng, bỏ chữ "Phân loại:" rườm rà)
                ColorAndSizeCompactRow(
                    colorName = item.color,
                    sizeName = item.size
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Hàng 3: Giá tiền bên trái + Bộ tăng giảm số lượng bên phải
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${formatCartVnd(item.unitPrice.toLong())}₫",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = Color(0xFF111111)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        if (isAtMaxStock) {
                            Text(
                                text = "Tối đa ${item.maxStock} đôi",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFE53935),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    // Pill tăng giảm số lượng chuẩn mẫu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable { onDecreaseClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF111111)
                            )
                        }

                        Text(
                            text = item.quantity.toString(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF111111)
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable { onIncreaseClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isAtMaxStock) Color(0xFFBDBDBD) else Color(0xFF111111)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorAndSizeCompactRow(
    colorName: String,
    sizeName: String
) {
    val dotColor = resolveShoeColorDot(colorName)
    val needsBorder = dotColor == Color.White || colorName.contains("Trắng", ignoreCase = true)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(dotColor)
                .then(
                    if (needsBorder) Modifier.border(1.dp, Color(0xFFD0D0D0), CircleShape) else Modifier
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${colorName.ifBlank { "Tiêu chuẩn" }}  |  Size: $sizeName",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF757575),
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CartCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (checked) Color(0xFF111111) else Color.White)
            .border(
                width = 1.5.dp,
                color = if (checked) Color(0xFF111111) else Color(0xFFBDBDBD),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onCheckedChange() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Đã chọn",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun resolveShoeColorDot(colorName: String): Color {
    val lower = colorName.lowercase()
    return when {
        lower.contains("đen") || lower.contains("black") -> Color(0xFF1A1A1A)
        lower.contains("trắng") || lower.contains("white") -> Color.White
        lower.contains("xám") || lower.contains("grey") || lower.contains("bạc") -> Color(0xFF9E9E9E)
        lower.contains("xanh dương") || lower.contains("blue") -> Color(0xFF1565C0)
        lower.contains("xanh lá") || lower.contains("green") -> Color(0xFF2E7D32)
        lower.contains("đỏ") || lower.contains("red") -> Color(0xFFE53935)
        lower.contains("hồng") || lower.contains("pink") -> Color(0xFFF06292)
        lower.contains("vàng") || lower.contains("yellow") -> Color(0xFFFBC02D)
        lower.contains("nâu") || lower.contains("brown") -> Color(0xFF6D4C41)
        lower.contains("cam") || lower.contains("orange") -> Color(0xFFFB8C00)
        else -> Color(0xFF424242)
    }
}

private fun formatCartVnd(amount: Long): String {
    return String.format("%,d", amount).replace(',', '.')
}
