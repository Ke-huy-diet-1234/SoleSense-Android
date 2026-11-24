package com.example.solesense.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.R
import com.example.solesense.data.repository.ProductRepository
import com.example.solesense.model.catalog.Product
import com.example.solesense.model.order.Order
import com.example.solesense.model.user.User
import com.example.solesense.push.PushNavigationBus
import com.example.solesense.ui.admin.notification.AdminNotificationManagementScreen
import com.example.solesense.ui.admin.ops.AdminCustomerOpsScreen
import com.example.solesense.ui.admin.order.AdminOrderManagementScreen
import com.example.solesense.ui.admin.product.AdminProductManagementScreen
import com.example.solesense.ui.admin.voucher.AdminVoucherManagementScreen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    user: User,
    onSwitchToCustomerView: () -> Unit = {},
    onLogoutClick: () -> Unit
) {
    var activeSubScreen by remember { mutableStateOf<String?>(null) } // "VOUCHER", "ORDER", "NOTIFICATION", "PRODUCT", "OPS"
    var activeOpsInitialTab by remember { mutableStateOf("USERS") }

    // Lắng nghe Deep-Link khi Admin bấm vào Thông báo màn hình (FCM)
    val pendingPushNav by PushNavigationBus.pendingNavigation.collectAsState()
    LaunchedEffect(pendingPushNav) {
        val nav = pendingPushNav ?: return@LaunchedEffect
        val route = nav.deepLinkRoute
        when (route) {
            "admin_order" -> {
                activeSubScreen = "ORDER"
                PushNavigationBus.consume()
            }
            "admin_product" -> {
                activeSubScreen = "PRODUCT"
                PushNavigationBus.consume()
            }
            "admin_voucher" -> {
                activeSubScreen = "VOUCHER"
                PushNavigationBus.consume()
            }
            "admin_notification" -> {
                activeSubScreen = "NOTIFICATION"
                PushNavigationBus.consume()
            }
            "admin_ops_returns" -> {
                activeOpsInitialTab = "RETURNS"
                activeSubScreen = "OPS"
                PushNavigationBus.consume()
            }
            "admin_ops_reviews" -> {
                activeOpsInitialTab = "REVIEWS"
                activeSubScreen = "OPS"
                PushNavigationBus.consume()
            }
        }
    }

    // Số liệu thống kê thực tế tải trực tiếp từ Firestore (Không dùng số liệu giả!)
    var isLoadingStats by remember { mutableStateOf(true) }
    var totalOrdersCount by remember { mutableStateOf(0) }
    var todayOrdersCount by remember { mutableStateOf(0) }
    var pendingOrdersCount by remember { mutableStateOf(0) }
    var cancelledOrdersCount by remember { mutableStateOf(0) }
    var confirmedRevenue by remember { mutableStateOf(0.0) }
    var totalOrderValue by remember { mutableStateOf(0.0) }
    var activeProductsCount by remember { mutableStateOf(0) }
    var lowStockProductsCount by remember { mutableStateOf(0) }
    var totalStockPairs by remember { mutableStateOf(0) }
    var pendingReturnCount by remember { mutableStateOf(0) }
    var topSellingProducts by remember { mutableStateOf<List<Product>>(emptyList()) }

    val numFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }

    LaunchedEffect(activeSubScreen) {
        if (activeSubScreen != null) return@LaunchedEffect
        isLoadingStats = true
        try {
            val db = FirebaseFirestore.getInstance()
            val ordersSnap = db.collection("orders").get().await()
            val orders = ordersSnap.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(orderId = doc.id)
            }

            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            totalOrdersCount = orders.size
            todayOrdersCount = orders.count { it.createdAt.toDate().time >= startOfToday }
            cancelledOrdersCount = orders.count { it.orderStatus.uppercase() == "CANCELLED" }
            pendingOrdersCount = orders.count {
                it.orderStatus.uppercase() in setOf("PENDING", "AWAITING_PAYMENT")
            }

            // Quy tắc tính doanh thu chuẩn:
            // - Không tính đơn CANCELLED
            // - Doanh thu thực nhận chỉ tính đơn đã thanh toán (SUCCESS/PAID) hoặc đã giao thành công (DELIVERED/COMPLETED)
            val nonCancelledOrders = orders.filter { it.orderStatus.uppercase() != "CANCELLED" }
            totalOrderValue = nonCancelledOrders.sumOf { it.totalAmount }
            confirmedRevenue = nonCancelledOrders.filter { o ->
                o.paymentStatus.uppercase() in setOf("SUCCESS", "PAID") ||
                        o.orderStatus.uppercase() in setOf("DELIVERED", "COMPLETED")
            }.sumOf { it.totalAmount }

            val allProds = ProductRepository().getAllProductsForAdmin().getOrDefault(emptyList())
            val activeProds = allProds.filter { it.status.uppercase() != "DISABLED" }
            activeProductsCount = activeProds.size
            lowStockProductsCount = activeProds.count { it.totalStockQuantity <= 10 }
            totalStockPairs = activeProds.sumOf { it.totalStockQuantity }
            topSellingProducts = activeProds.sortedByDescending { it.totalSoldCount }.take(3)

            val returnsSnap = db.collection("returnRequests").get().await()
            pendingReturnCount = returnsSnap.documents.count {
                (it.getString("status") ?: "PENDING").uppercase() == "PENDING"
            }
        } catch (_: Exception) {}
        isLoadingStats = false
    }

    when (activeSubScreen) {
        "VOUCHER" -> {
            AdminVoucherManagementScreen(onBackClick = { activeSubScreen = null })
            return
        }
        "ORDER" -> {
            AdminOrderManagementScreen(onBackClick = { activeSubScreen = null })
            return
        }
        "NOTIFICATION" -> {
            AdminNotificationManagementScreen(
                adminUid = user.userId,
                onBackClick = { activeSubScreen = null }
            )
            return
        }
        "PRODUCT" -> {
            AdminProductManagementScreen(
                adminUid = user.userId,
                onBackClick = { activeSubScreen = null }
            )
            return
        }
        "OPS" -> {
            AdminCustomerOpsScreen(
                adminUid = user.userId,
                initialTab = activeOpsInitialTab,
                onBackClick = {
                    activeSubScreen = null
                    activeOpsInitialTab = "USERS"
                }
            )
            return
        }
    }

    val subtitleText = if (user.email.isNotBlank()) {
        "${user.fullName} (${user.email})"
    } else {
        user.fullName.ifBlank { "Administrator" }
    }

    Scaffold(
        containerColor = Color(0xFFF6F7FB),
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_solesense_logo),
                            contentDescription = "SoleSense Admin",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bảng điều khiển Quản trị",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A1A1A)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFEBEE))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFE53935),
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    TextButton(
                        onClick = onLogoutClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Đăng xuất",
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                val cancelRate = if (totalOrdersCount > 0) {
                    (cancelledOrdersCount * 100) / totalOrdersCount
                } else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        title = "Tổng đơn hàng",
                        value = if (isLoadingStats) "..." else "$totalOrdersCount đơn",
                        subtitle = "+$todayOrdersCount hôm nay • Hủy: $cancelRate%",
                        accentColor = Color(0xFF1E88E5),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeSubScreen = "ORDER" }
                    )
                    AdminStatCard(
                        title = "Doanh thu thực nhận",
                        value = if (isLoadingStats) "..." else "${numFormat.format(confirmedRevenue.toLong())} ₫",
                        subtitle = "Tổng đặt: ${numFormat.format(totalOrderValue.toLong())} ₫",
                        accentColor = Color(0xFF2E7D32),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeSubScreen = "ORDER" }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        title = "Sản phẩm đang bán",
                        value = if (isLoadingStats) "..." else "$activeProductsCount mẫu",
                        subtitle = "Tổng kho: $totalStockPairs đôi • Sắp hết: $lowStockProductsCount",
                        accentColor = Color(0xFFF57C00),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeSubScreen = "PRODUCT" }
                    )
                    AdminStatCard(
                        title = "Đơn chờ xử lý / QR",
                        value = if (isLoadingStats) "..." else "$pendingOrdersCount đơn",
                        subtitle = "Đổi/Trả chờ duyệt: $pendingReturnCount",
                        accentColor = Color(0xFFE53935),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeSubScreen = "ORDER" }
                    )
                }
            }

            // Top sản phẩm bán chạy thực tế từ Firestore
            if (topSellingProducts.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "TOP SẢN PHẨM BÁN CHẠY (DỮ LIỆU THỰC TẾ)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF6B7280)
                                )
                            )
                            topSellingProducts.forEachIndexed { idx, p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${p.name}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Đã bán: ${p.totalSoldCount} • Kho: ${p.totalStockQuantity}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "CHỨC NĂNG QUẢN TRỊ HỆ THỐNG",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF616161)
                    ),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item {
                AdminModuleItem(
                    title = "1. Quản lý Đơn hàng, Duyệt VietQR & Hủy đơn",
                    description = "Xem chi tiết đơn, duyệt thanh toán QR (đồng bộ realtime máy khách), cập nhật giao hàng, hủy đơn hoàn kho tự động.",
                    onClick = { activeSubScreen = "ORDER" }
                )
            }

            item {
                AdminModuleItem(
                    title = "2. Quản lý Sản phẩm, Biến thể & Nhập/Xuất Tồn kho",
                    description = "Thêm/sửa/ẩn sản phẩm, quản lý biến thể Size/Màu, nhập hàng bổ sung ghi InventoryLog và đồng bộ tồn kho tổng.",
                    onClick = { activeSubScreen = "PRODUCT" }
                )
            }

            item {
                AdminModuleItem(
                    title = "3. Quản lý Mã giảm giá (Vouchers & Khuyến mãi)",
                    description = "Cấu hình voucher, chỉnh sửa, bật/tắt trạng thái, giới hạn số lượt và thời gian hoạt động.",
                    onClick = { activeSubScreen = "VOUCHER" }
                )
            }

            item {
                AdminModuleItem(
                    title = "4. Quản lý Thông báo & Chiến dịch",
                    description = "Theo dõi thông báo tự động khi có đơn hàng mới và phát hành thông báo khuyến mãi / hệ thống.",
                    onClick = { activeSubScreen = "NOTIFICATION" }
                )
            }

            item {
                AdminModuleItem(
                    title = "5. Quản lý Người dùng, Đánh giá & Đổi/Trả hàng",
                    description = "Phân quyền tài khoản (Customer/Support/Admin), khóa/mở khóa người dùng, phản hồi đánh giá và duyệt yêu cầu đổi trả.",
                    onClick = { activeSubScreen = "OPS" }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSwitchToCustomerView,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Xem giao diện Cửa hàng (Chế độ Chỉ xem)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    fontSize = 18.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E9E9E)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminModuleItem(
    title: String,
    description: String,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    fontSize = 15.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF616161),
                    lineHeight = 18.sp
                )
            )
        }
    }
}
