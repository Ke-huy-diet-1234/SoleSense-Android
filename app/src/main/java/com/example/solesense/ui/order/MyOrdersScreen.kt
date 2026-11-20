package com.example.solesense.ui.order

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
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
import com.example.solesense.model.order.Order
import com.example.solesense.model.order.OrderItem
import com.example.solesense.repository.NotificationRepository
import com.example.solesense.repository.OrderRepository
import com.example.solesense.repository.ReviewRepository
import com.example.solesense.utils.CloudinaryHelper
import com.example.solesense.utils.UserSessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Màn hình Quản lý Đơn hàng của Khách hàng (My Orders - Sao y Ảnh 13, 14, 16)
 * tích hợp Theo dõi đơn hàng (Track Order - Ảnh 15) & Đánh giá sản phẩm có up tối đa 4 ảnh lên Cloudinary (Ảnh 17/18)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    initialOrderIdToTrack: String? = null,
    onBackClick: () -> Unit,
    onBuyAgainProductClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val orderRepository = remember { OrderRepository() }
    val reviewRepository = remember { ReviewRepository() }
    val notificationRepository = remember { NotificationRepository() }
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val userId = currentUser?.userId ?: UserSessionManager.getActiveUserId().orEmpty()

    var ordersWithItems by remember { mutableStateOf<List<Pair<Order, List<OrderItem>>>>(emptyList()) }
    var reviewedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedTab by remember { mutableStateOf("ACTIVE") } // ACTIVE, COMPLETED
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Trạng thái mở màn hình Track Order (Ảnh 15)
    var trackingOrderPair by remember { mutableStateOf<Pair<Order, List<OrderItem>>?>(null) }

    // Trạng thái mở BottomSheet Đánh giá sản phẩm (Ảnh 17/18)
    var reviewTarget by remember { mutableStateOf<Pair<Order, OrderItem>?>(null) }

    fun loadOrders() {
        scope.launch {
            isLoading = true
            if (userId.isNotBlank()) {
                orderRepository.getUserOrdersWithItems(userId).onSuccess { list ->
                    ordersWithItems = list
                    if (!initialOrderIdToTrack.isNullOrBlank() && trackingOrderPair == null) {
                        trackingOrderPair = list.find { it.first.orderId == initialOrderIdToTrack }
                    }
                }
                reviewedKeys = reviewRepository.getReviewedOrderProductKeys(userId)
            }
            isLoading = false
        }
    }

    LaunchedEffect(userId, initialOrderIdToTrack) {
        loadOrders()
    }

    // Nếu đang xem chi tiết hành trình đơn hàng (Track Order - Ảnh 15)
    if (trackingOrderPair != null) {
        val (order, items) = trackingOrderPair!!
        TrackOrderScreen(
            order = order,
            items = items,
            onBackClick = { trackingOrderPair = null },
            onConfirmReceived = {
                scope.launch {
                    orderRepository.updateOrderStatus(order.orderId, "DELIVERED").onSuccess {
                        notificationRepository.notifyOrderStatusChanged(order, "DELIVERED")
                        Toast.makeText(context, "Đã xác nhận nhận hàng thành công! Bạn có thể đánh giá sản phẩm ngay.", Toast.LENGTH_SHORT).show()
                        trackingOrderPair = null
                        selectedTab = "COMPLETED"
                        loadOrders()
                    }
                }
            },
            onCancelOrder = { cancelReason ->
                scope.launch {
                    orderRepository.cancelUserPendingOrder(order.orderId, items, cancelReason)
                        .onSuccess {
                            notificationRepository.notifyOrderCancelled(order, "Bạn đã hủy đơn hàng #${order.orderCode} (Lý do: $cancelReason).")
                            Toast.makeText(context, "Đã hủy đơn hàng & hoàn kho thành công!", Toast.LENGTH_SHORT).show()
                            trackingOrderPair = null
                            selectedTab = "COMPLETED"
                            loadOrders()
                        }
                        .onFailure { err ->
                            Toast.makeText(context, err.message ?: "Không thể hủy đơn", Toast.LENGTH_LONG).show()
                        }
                }
            }
        )
        return
    }

    val activeOrders = remember(ordersWithItems, searchQuery) {
        val q = searchQuery.trim().lowercase()
        ordersWithItems.filter { (order, items) ->
            val isAct = order.orderStatus.uppercase() in setOf(
                "PENDING", "AWAITING_PAYMENT", "CONFIRMED", "PACKING", "SHIPPING", "IN_DELIVERY"
            )
            val matchQ = q.isEmpty() ||
                    order.orderCode.lowercase().contains(q) ||
                    items.any { it.productName.lowercase().contains(q) }
            isAct && matchQ
        }
    }

    val completedOrders = remember(ordersWithItems, searchQuery) {
        val q = searchQuery.trim().lowercase()
        ordersWithItems.filter { (order, items) ->
            val isComp = order.orderStatus.uppercase() in setOf("DELIVERED", "COMPLETED", "CANCELLED", "RETURNED")
            val matchQ = q.isEmpty() ||
                    order.orderCode.lowercase().contains(q) ||
                    items.any { it.productName.lowercase().contains(q) }
            isComp && matchQ
        }
    }

    val displayedOrders = if (selectedTab == "ACTIVE") activeOrders else completedOrders

    Scaffold(
        containerColor = Color(0xFFF8F9FC),
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Quay lại",
                                    tint = Color(0xFF111827)
                                )
                            }
                            Text(
                                text = "Đơn hàng của tôi",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 21.sp,
                                    color = Color(0xFF111827)
                                )
                            )
                        }

                        IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                            Icon(
                                imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Tìm kiếm đơn hàng",
                                tint = Color(0xFF111827)
                            )
                        }
                    }

                    if (isSearchVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(42.dp)
                                .clip(RoundedCornerShape(21.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF111827),
                                    fontSize = 14.sp
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Tìm theo mã đơn (#SOLE...) hoặc tên giày...",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    // 2 Tab: Active (Đang xử lý) | Completed (Hoàn thành) - Ảnh 13, 14, 16
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        val isAct = selectedTab == "ACTIVE"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = "ACTIVE" }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Đang xử lý (${activeOrders.size})",
                                fontWeight = if (isAct) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isAct) Color(0xFF111827) else Color(0xFF9CA3AF),
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(if (isAct) Color(0xFF111827) else Color(0xFFE5E7EB))
                            )
                        }

                        val isComp = selectedTab == "COMPLETED"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = "COMPLETED" }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Hoàn thành (${completedOrders.size})",
                                fontWeight = if (isComp) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isComp) Color(0xFF111827) else Color(0xFF9CA3AF),
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(if (isComp) Color(0xFF111827) else Color(0xFFE5E7EB))
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF111827))
            }
        } else if (displayedOrders.isEmpty()) {
            // GIAO DIỆN TRỐNG CHUẨN ẢNH 13
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.img_not_found),
                        contentDescription = "Chưa có đơn hàng",
                        modifier = Modifier.size(150.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bạn chưa có đơn hàng nào",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color(0xFF111827)
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedTab == "ACTIVE") {
                            "Hiện tại bạn không có đơn hàng nào đang trong quá trình xử lý hoặc giao hàng."
                        } else {
                            "Bạn chưa có đơn hàng nào đã hoàn tất."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280),
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(displayedOrders, key = { it.first.orderId }) { (order, items) ->
                    val primaryItem = items.firstOrNull()
                    val isReviewed = primaryItem != null && reviewedKeys.contains("${order.orderId}_${primaryItem.productId}")

                    MyOrderCardItem(
                        order = order,
                        items = items,
                        isCompletedTab = selectedTab == "COMPLETED",
                        isReviewed = isReviewed,
                        onTrackOrderClick = {
                            trackingOrderPair = Pair(order, items)
                        },
                        onLeaveReviewClick = {
                            if (primaryItem != null) {
                                reviewTarget = Pair(order, primaryItem)
                            }
                        },
                        onBuyAgainClick = {
                            if (primaryItem != null && primaryItem.productId.isNotBlank()) {
                                onBuyAgainProductClick(primaryItem.productId)
                            }
                        }
                    )
                }
            }
        }
    }

    // BOTTOM SHEET ĐÁNH GIÁ SẢN PHẨM KÈM UPLOAD TỐI ĐA 4 ẢNH CLOUDINARY (ẢNH 17/18)
    if (reviewTarget != null) {
        val (targetOrder, targetItem) = reviewTarget!!
        LeaveReviewBottomSheet(
            order = targetOrder,
            item = targetItem,
            onDismiss = { reviewTarget = null },
            onReviewSubmitted = {
                reviewTarget = null
                loadOrders()
                Toast.makeText(context, "Cảm ơn bạn đã gửi đánh giá sản phẩm!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Thẻ Đơn hàng chuẩn Ảnh 14 (Tab Active) & Ảnh 16 (Tab Completed)
 */
@Composable
private fun MyOrderCardItem(
    order: Order,
    items: List<OrderItem>,
    isCompletedTab: Boolean,
    isReviewed: Boolean,
    onTrackOrderClick: () -> Unit,
    onLeaveReviewClick: () -> Unit,
    onBuyAgainClick: () -> Unit
) {
    val numFormat = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }
    val firstItem = items.firstOrNull()
    val totalQty = items.sumOf { it.quantity }.coerceAtLeast(1)

    val (statusBadgeText, statusBgColor, statusTextColor) = when (order.orderStatus.uppercase()) {
        "AWAITING_PAYMENT" -> Triple("Chờ thanh toán QR", Color(0xFFFEF3C7), Color(0xFFD97706))
        "PENDING" -> Triple("Chờ xác nhận", Color(0xFFE0F2FE), Color(0xFF0284C7))
        "CONFIRMED", "PACKING" -> Triple("Đang đóng gói", Color(0xFFEDE9FE), Color(0xFF6D28D9))
        "SHIPPING", "IN_DELIVERY" -> Triple("Đang giao hàng", Color(0xFFF3F4F6), Color(0xFF1F2937))
        "DELIVERED", "COMPLETED" -> Triple("Hoàn thành", Color(0xFFDCFCE7), Color(0xFF15803D))
        "CANCELLED" -> Triple("Đã hủy", Color(0xFFFEE2E2), Color(0xFFDC2626))
        else -> Triple(order.orderStatus, Color(0xFFF3F4F6), Color(0xFF374151))
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 1.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onTrackOrderClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ảnh sản phẩm bên trái
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF4F5F7)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = firstItem?.imageSnapshot ?: "",
                    contentDescription = firstItem?.productName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = firstItem?.productName ?: "Đơn hàng #${order.orderCode}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFF111827)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${firstItem?.color ?: "Tiêu chuẩn"}  |  Cỡ ${firstItem?.size ?: "41"}  |  SL: $totalQty",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (items.size > 1) {
                    Text(
                        text = "+ ${items.size - 1} sản phẩm khác trong đơn #${order.orderCode}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF6366F1))
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Badge trạng thái (In Delivery / Completed...)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBgColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hàng dưới cùng: Tổng tiền (luôn nằm gọn 1 dòng) + Nút hành động
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${numFormat.format(order.totalAmount.toLong())}đ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = Color(0xFF111827)
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (!isCompletedTab) {
                        Button(
                            onClick = onTrackOrderClick,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF111827),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "Theo dõi đơn",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        val isDelivered = order.orderStatus.uppercase() in setOf("DELIVERED", "COMPLETED")
                        if (isDelivered && !isReviewed) {
                            Button(
                                onClick = onLeaveReviewClick,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF111827),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Đánh giá",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            Button(
                                onClick = onBuyAgainClick,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF111827),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Mua lại",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Màn hình Theo dõi hành trình & Chi tiết đơn hàng (Track Order & Order Details)
 * - Chỉ hiển thị đúng các mốc sự kiện đã thực sự diễn ra (không bịa mốc thời gian tương lai cho đơn chờ/đã hủy)
 * - Hiển thị đầy đủ danh sách sản phẩm trong đơn hàng nhiều mặt hàng + bảng tổng kết thanh toán
 * - Hộp thoại chọn lý do hủy đơn hàng & gửi yêu cầu Đổi/Trả hàng
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOrderScreen(
    order: Order,
    items: List<OrderItem>,
    onBackClick: () -> Unit,
    onConfirmReceived: () -> Unit,
    onCancelOrder: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val numFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }
    val dateFormat = remember { SimpleDateFormat("dd 'thg' MM", Locale.forLanguageTag("vi-VN")) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")) }

    val statusUpper = order.orderStatus.uppercase()
    val activeStep = when (statusUpper) {
        "PENDING", "AWAITING_PAYMENT", "CONFIRMED", "PACKING" -> 1
        "SHIPPING" -> 2
        "IN_DELIVERY" -> 3
        "DELIVERED", "COMPLETED" -> 4
        else -> 0 // CANCELLED
    }

    val statusHeadline = when (statusUpper) {
        "AWAITING_PAYMENT" -> "Đơn hàng đang chờ thanh toán VietQR"
        "PENDING" -> "Đơn hàng đang chờ cửa hàng xác nhận"
        "CONFIRMED", "PACKING" -> "Đơn hàng đã xác nhận & đang đóng gói tại kho"
        "SHIPPING", "IN_DELIVERY" -> "Đơn hàng đang trên đường giao đến bạn"
        "DELIVERED", "COMPLETED" -> "Đơn hàng đã giao thành công"
        "CANCELLED" -> "Đơn hàng đã bị hủy"
        else -> order.orderStatus
    }

    var showCancelDialog by remember { mutableStateOf(false) }
    var showReturnSheet by remember { mutableStateOf(false) }

    // Chỉ xây dựng các mốc thời gian ĐÃ THỰC SỰ XẢY RA dựa trên trạng thái đơn hàng thực tế
    val createdDate = order.createdAt.toDate()
    val updatedDate = order.updatedAt.toDate()
    val timelineEvents = remember(order) {
        buildList {
            when (statusUpper) {
                "CANCELLED" -> {
                    add(
                        Triple(
                            "Đơn hàng đã bị hủy - ${dateFormat.format(updatedDate)}",
                            "Lý do: ${order.cancelReason?.ifBlank { "Đơn hàng đã hủy & hoàn trả tồn kho" } ?: "Đơn hàng đã hủy & hoàn trả tồn kho"}",
                            timeFormat.format(updatedDate)
                        )
                    )
                }
                "DELIVERED", "COMPLETED" -> {
                    add(
                        Triple(
                            "Giao hàng thành công - ${dateFormat.format(updatedDate)}",
                            "Người nhận: ${order.receiverName} • Đã giao tại ${order.shippingAddress}",
                            timeFormat.format(updatedDate)
                        )
                    )
                    add(
                        Triple(
                            "Đơn hàng đang giao đến địa chỉ nhận - ${dateFormat.format(updatedDate)}",
                            order.shippingAddress.ifBlank { "Địa chỉ người nhận: ${order.receiverName}" },
                            timeFormat.format(Date((createdDate.time + updatedDate.time) / 2))
                        )
                    )
                    add(
                        Triple(
                            "Đã xác nhận & đóng gói xuất kho - ${dateFormat.format(createdDate)}",
                            "Kho tổng SoleSense Chính Hãng • Kiểm định chất lượng & tem",
                            timeFormat.format(createdDate)
                        )
                    )
                }
                "SHIPPING", "IN_DELIVERY" -> {
                    add(
                        Triple(
                            "Đang giao đến địa chỉ người nhận - ${dateFormat.format(updatedDate)}",
                            "Đơn vị vận chuyển đang giao tới: ${order.shippingAddress}",
                            timeFormat.format(updatedDate)
                        )
                    )
                    add(
                        Triple(
                            "Đã xác nhận & bàn giao vận chuyển - ${dateFormat.format(createdDate)}",
                            "Kho tổng SoleSense Chính Hãng",
                            timeFormat.format(createdDate)
                        )
                    )
                }
                "CONFIRMED", "PACKING" -> {
                    add(
                        Triple(
                            "Cửa hàng đã xác nhận & đang đóng gói - ${dateFormat.format(updatedDate)}",
                            "Kiểm định chất lượng giày & đóng gói tiêu chuẩn tại Kho SoleSense",
                            timeFormat.format(updatedDate)
                        )
                    )
                }
                "AWAITING_PAYMENT" -> {
                    add(
                        Triple(
                            "Đang chờ xác nhận thanh toán VietQR - ${dateFormat.format(createdDate)}",
                            "Hệ thống tạm giữ tồn kho 5 phút chờ chuyển khoản",
                            timeFormat.format(createdDate)
                        )
                    )
                }
            }
            // Mốc gốc luôn tồn tại: thời điểm khách tạo đơn hàng
            add(
                Triple(
                    "Đã tạo đơn hàng #${order.orderCode} - ${dateFormat.format(createdDate)}",
                    "Phương thức: ${if (order.paymentMethod == "QR_CODE") "VietQR" else "COD"} • Người nhận: ${order.receiverName} (${order.receiverPhone})",
                    timeFormat.format(createdDate)
                )
            )
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FC),
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF111827)
                        )
                    }
                    Text(
                        text = "Chi tiết & Theo dõi đơn #${order.orderCode}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF111827)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 1. Danh sách toàn bộ sản phẩm trong đơn hàng
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SẢN PHẨM TRONG ĐƠN (${items.sumOf { it.quantity }.coerceAtLeast(1)})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6B7280)
                        )
                    )
                    items.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF4F5F7)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = item.imageSnapshot ?: "",
                                    contentDescription = item.productName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF111827)
                                    )
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Màu: ${item.color}  |  Cỡ: ${item.size}  |  SL: x${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "${numFormat.format((item.unitPrice * item.quantity).toLong())} ₫",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF111827)
                                    )
                                )
                            }
                        }
                        if (idx < items.lastIndex) {
                            HorizontalDivider(color = Color(0xFFF3F4F6))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Thanh tiến trình 4 bước với Icon thu gọn tinh tế
            val steps = listOf(
                Icons.Default.ShoppingCart to 1,
                Icons.AutoMirrored.Filled.Send to 2,
                Icons.Default.LocationOn to 3,
                Icons.Default.CheckCircle to 4
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { idx, (iconVec, stepNum) ->
                    val isDone = activeStep >= stepNum
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = iconVec,
                            contentDescription = null,
                            tint = if (isDone) Color(0xFF111827) else Color(0xFFD1D5DB),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(if (isDone) Color(0xFF111827) else Color(0xFFD1D5DB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }

                    if (idx < steps.lastIndex) {
                        Text(
                            text = "- - -",
                            color = if (activeStep > stepNum) Color(0xFF111827) else Color(0xFFD1D5DB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = statusHeadline,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (statusUpper == "CANCELLED") Color(0xFFDC2626) else Color(0xFF111827)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Thông tin giao hàng & Chi tiết thanh toán
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "THÔNG TIN GIAO HÀNG & THANH TOÁN",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6B7280)
                        )
                    )
                    Text(
                        text = "Người nhận: ${order.receiverName} • ${order.receiverPhone}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "Địa chỉ: ${order.shippingAddress}",
                        fontSize = 12.5.sp,
                        color = Color(0xFF4B5563)
                    )
                    if (!order.note.isNullOrBlank()) {
                        Text(
                            text = "Ghi chú: ${order.note}",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Phương thức thanh toán", fontSize = 12.5.sp, color = Color(0xFF6B7280))
                        Text(
                            text = if (order.paymentMethod == "QR_CODE") "Chuyển khoản VietQR" else "Thanh toán khi nhận hàng (COD)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Trạng thái thanh toán", fontSize = 12.5.sp, color = Color(0xFF6B7280))
                        val payLabel = when (order.paymentStatus.uppercase()) {
                            "SUCCESS", "PAID" -> "Đã thanh toán"
                            "REFUND_PENDING" -> "Chờ hoàn tiền"
                            "FAILED", "CANCELLED" -> "Đã hủy / Thất bại"
                            else -> "Chưa thanh toán"
                        }
                        Text(
                            text = payLabel,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (order.paymentStatus.uppercase() in setOf("SUCCESS", "PAID")) Color(0xFF16A34A) else Color(0xFFD97706)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tạm tính tiền hàng", fontSize = 12.5.sp, color = Color(0xFF6B7280))
                        Text("${numFormat.format(order.subtotal.toLong())} ₫", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Phí vận chuyển", fontSize = 12.5.sp, color = Color(0xFF6B7280))
                        Text("${numFormat.format(order.shippingFee.toLong())} ₫", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    }
                    if (order.discount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Giảm giá Voucher", fontSize = 12.5.sp, color = Color(0xFF16A34A))
                            Text("-${numFormat.format(order.discount.toLong())} ₫", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng thanh toán", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
                        Text("${numFormat.format(order.totalAmount.toLong())} ₫", fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE53935))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Lịch sử cập nhật trạng thái thực tế
            Text(
                text = "Lịch sử trạng thái đơn hàng",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFF111827)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            timelineEvents.forEachIndexed { index, (title, subtitle, timeStr) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color(0xFF111827), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (index == 0) Color(0xFF111827) else Color(0xFF6B7280))
                            )
                        }

                        if (index < timelineEvents.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(36.dp)
                                    .background(Color(0xFFD1D5DB))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827),
                                    fontSize = 14.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280),
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF6B7280),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Các nút thao tác nghiệp vụ đơn hàng
            if (statusUpper in setOf("PENDING", "AWAITING_PAYMENT", "CONFIRMED", "PACKING", "SHIPPING", "IN_DELIVERY")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (statusUpper in setOf("PENDING", "AWAITING_PAYMENT", "CONFIRMED")) {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.2.dp, Color(0xFFDC2626)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Hủy đơn hàng", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (statusUpper in setOf("SHIPPING", "IN_DELIVERY")) {
                        Button(
                            onClick = onConfirmReceived,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Đã nhận được hàng", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (statusUpper in setOf("DELIVERED", "COMPLETED")) {
                OutlinedButton(
                    onClick = { showReturnSheet = true },
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.2.dp, Color(0xFF111827)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF111827)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Yêu cầu Đổi size / Trả hàng", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Hộp thoại chọn lý do hủy đơn hàng
    if (showCancelDialog) {
        val reasons = listOf(
            "Muốn thay đổi địa chỉ hoặc số điện thoại nhận hàng",
            "Muốn đổi kích cỡ (Size) / màu sắc hoặc áp dụng mã giảm giá",
            "Tìm thấy mẫu giày khác phù hợp hơn",
            "Đổi ý, chưa có nhu cầu mua lúc này"
        )
        var selectedReason by remember { mutableStateOf(reasons.first()) }

        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Xác nhận hủy đơn #${order.orderCode}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Vui lòng chọn lý do hủy đơn hàng (sản phẩm sẽ được hoàn trả tồn kho ngay lập tức):",
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563)
                    )
                    reasons.forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedReason = r }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == r,
                                onClick = { selectedReason = r }
                            )
                            Text(
                                text = r,
                                fontSize = 13.sp,
                                color = Color(0xFF111827)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelOrder(selectedReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Xác nhận hủy", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Đóng", color = Color(0xFF4B5563))
                }
            }
        )
    }

    // BottomSheet Gửi yêu cầu Đổi size / Trả hàng (Lưu lên Firestore collection `returnRequests`)
    if (showReturnSheet) {
        val returnReasons = listOf(
            "Muốn đổi sang kích cỡ (Size) khác cho vừa chân",
            "Sản phẩm có lỗi kỹ thuật / bung keo từ nhà sản xuất",
            "Cửa hàng giao nhầm mẫu hoặc nhầm size so với đơn đặt",
            "Sản phẩm thực tế không giống mô tả"
        )
        var requestType by remember { mutableStateOf("EXCHANGE") } // EXCHANGE hoặc RETURN
        var selectedReason by remember { mutableStateOf(returnReasons.first()) }
        var requestedSize by remember { mutableStateOf("41") }
        var description by remember { mutableStateOf("") }
        var isSubmittingReturn by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { if (!isSubmittingReturn) showReturnSheet = false },
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
                Text(
                    text = "Yêu cầu Đổi / Trả hàng (#${order.orderCode})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF111827)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("EXCHANGE" to "Đổi Size / Mẫu", "RETURN" to "Trả hàng hoàn tiền").forEach { (typeCode, label) ->
                        val isSel = requestType == typeCode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) Color(0xFF111827) else Color(0xFFF3F4F6),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { requestType = typeCode }
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.White else Color(0xFF374151),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                Text("Chọn lý do:", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                returnReasons.forEach { reasonItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reasonItem },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reasonItem,
                            onClick = { selectedReason = reasonItem }
                        )
                        Text(reasonItem, fontSize = 13.sp, color = Color(0xFF111827))
                    }
                }

                if (requestType == "EXCHANGE") {
                    OutlinedTextField(
                        value = requestedSize,
                        onValueChange = { requestedSize = it },
                        label = { Text("Size giày mong muốn đổi sang (VD: 41, 42)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả chi tiết tình trạng sản phẩm") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val firstIt = items.firstOrNull()
                        scope.launch {
                            isSubmittingReturn = true
                            try {
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val docRef = db.collection("returnRequests").document()
                                val reqObj = com.example.solesense.model.support.ReturnRequest(
                                    returnId = docRef.id,
                                    orderId = order.orderId,
                                    orderItemId = firstIt?.itemId ?: "",
                                    userId = order.userId,
                                    type = requestType,
                                    reason = selectedReason,
                                    description = description.ifBlank { selectedReason },
                                    currentSize = firstIt?.size,
                                    requestedSize = if (requestType == "EXCHANGE") requestedSize else null,
                                    status = "PENDING",
                                    createdAt = com.google.firebase.Timestamp.now(),
                                    updatedAt = com.google.firebase.Timestamp.now()
                                )
                                docRef.set(reqObj).await()
                                Toast.makeText(
                                    context,
                                    "Đã gửi yêu cầu Đổi/Trả hàng! Nhân viên sẽ phản hồi sớm nhất.",
                                    Toast.LENGTH_LONG
                                ).show()
                                showReturnSheet = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Lỗi gửi yêu cầu: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSubmittingReturn = false
                            }
                        }
                    },
                    enabled = !isSubmittingReturn,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (isSubmittingReturn) "Đang gửi..." else "Gửi yêu cầu Đổi / Trả",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * BottomSheet Đánh giá sản phẩm (Leave a Review - Sao y bản chính Ảnh 17/18)
 * - Cho phép chọn tối đa 4 ảnh từ thiết bị
 * - Upload trực tiếp lên Cloudinary (Cloud name: hbxjrmsz, Preset: solesense_unsigned)
 * - Lưu đánh giá vào Firestore & cập nhật điểm sao trung bình của sản phẩm
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveReviewBottomSheet(
    order: Order,
    item: OrderItem,
    onDismiss: () -> Unit,
    onReviewSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reviewRepository = remember { ReviewRepository() }
    val numFormat = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }

    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSubmitting by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val combined = (selectedImageUris + uris).distinct()
            if (combined.size > CloudinaryHelper.MAX_REVIEW_IMAGES) {
                Toast.makeText(
                    context,
                    "Chỉ được chọn tối đa ${CloudinaryHelper.MAX_REVIEW_IMAGES} ảnh đánh giá!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            selectedImageUris = combined.take(CloudinaryHelper.MAX_REVIEW_IMAGES)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Đánh giá sản phẩm",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color(0xFF111827)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Thẻ sản phẩm đang đánh giá (Ảnh 17/18)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF9FAFB),
                border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = item.imageSnapshot ?: "",
                            contentDescription = item.productName,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.productName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${item.color}  |  Cỡ ${item.size}  |  SL: ${item.quantity}",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE5E7EB))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Hoàn thành",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${numFormat.format(item.subtotal.toLong())}đ",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFF111827)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Câu hỏi chấm sao (Ảnh 17/18)
            Text(
                text = "Đơn hàng của bạn thế nào?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF111827)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hãy chọn mức sao đánh giá & chia sẻ cảm nhận kèm tối đa 4 ảnh thực tế nhé...",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5 ngôi sao lớn
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (s in 1..5) {
                    val isSelected = s <= rating
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.Star,
                        contentDescription = "$s sao",
                        tint = if (isSelected) Color(0xFF111827) else Color(0xFFD1D5DB),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { rating = s }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Khung nhập nhận xét + Icon chọn ảnh góc phải (Ảnh 17/18)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0xFF111827), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (comment.isEmpty()) {
                            Text(
                                text = "Sản phẩm rất êm và giao hàng nhanh!...",
                                color = Color(0xFF9CA3AF),
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF111827),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (selectedImageUris.size >= CloudinaryHelper.MAX_REVIEW_IMAGES) {
                                Toast.makeText(
                                    context,
                                    "Bạn đã chọn đủ tối đa 4 ảnh!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                imagePickerLauncher.launch("image/*")
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Thêm ảnh đánh giá (Tối đa 4 ảnh)",
                            tint = Color(0xFF111827),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 4. Danh sách preview tối đa 4 ảnh đã chọn
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hình ảnh đính kèm (${selectedImageUris.size}/${CloudinaryHelper.MAX_REVIEW_IMAGES} ảnh):",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF4B5563),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (selectedImageUris.size < CloudinaryHelper.MAX_REVIEW_IMAGES) {
                    Text(
                        text = "+ Chọn ảnh",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF4F46E5),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable { imagePickerLauncher.launch("image/*") }
                    )
                }
            }

            if (selectedImageUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selectedImageUris) { uri ->
                        Box(modifier = Modifier.size(72.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Ảnh đã chọn",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF111827))
                                    .clickable {
                                        selectedImageUris = selectedImageUris.filterNot { it == uri }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Xóa ảnh",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))

            // 5. Nút Cancel & Submit (Ảnh 17/18)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = Color(0xFF111827)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Hủy", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            val variantLabel = "Màu ${item.color} - Size ${item.size}"
                            val res = reviewRepository.submitProductReview(
                                context = context,
                                orderId = order.orderId,
                                orderItemId = item.itemId,
                                productId = item.productId,
                                variantInfo = variantLabel,
                                rating = rating,
                                comment = comment.ifBlank { "Sản phẩm rất tốt, đóng gói cẩn thận!" },
                                selectedImageUris = selectedImageUris
                            )
                            isSubmitting = false
                            res.fold(
                                onSuccess = { onReviewSubmitted() },
                                onFailure = { err ->
                                    Toast.makeText(
                                        context,
                                        err.message ?: "Gửi đánh giá thất bại",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111827),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(
                        text = if (isSubmitting) "Đang tải lên..." else "Gửi đánh giá",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
