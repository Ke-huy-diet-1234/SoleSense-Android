package com.example.solesense.ui.admin.order

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.model.order.Order
import com.example.solesense.model.order.OrderItem
import com.example.solesense.repository.NotificationRepository
import com.example.solesense.repository.OrderRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminOrderManagementScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val orderRepository = remember { OrderRepository() }
    val notificationRepository = remember { NotificationRepository() }

    var ordersWithItems by remember { mutableStateOf<List<Pair<Order, List<OrderItem>>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var processingOrderId by remember { mutableStateOf<String?>(null) }

    var selectedOrderForDetail by remember { mutableStateOf<Pair<Order, List<OrderItem>>?>(null) }
    var orderPendingAdminCancel by remember { mutableStateOf<Pair<Order, List<OrderItem>>?>(null) }

    fun refreshOrders() {
        scope.launch {
            isLoading = true
            orderRepository.getAllOrdersForAdmin().onSuccess { list ->
                ordersWithItems = list
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshOrders()
    }

    val filterTabs = listOf(
        "ALL" to "Tất cả",
        "AWAITING_PAYMENT" to "Chờ duyệt QR",
        "PENDING" to "Chờ xác nhận",
        "CONFIRMED" to "Đang chuẩn bị",
        "SHIPPING" to "Đang giao",
        "DELIVERED" to "Hoàn thành",
        "CANCELLED" to "Đã hủy"
    )

    val filteredOrders = remember(ordersWithItems, selectedFilter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        ordersWithItems.filter { (order, items) ->
            val st = order.orderStatus.uppercase()
            val matchTab = when (selectedFilter) {
                "AWAITING_PAYMENT" -> st == "AWAITING_PAYMENT" || (order.paymentMethod == "QR_CODE" && order.paymentStatus.uppercase() == "PENDING" && st != "CANCELLED")
                "PENDING" -> st == "PENDING"
                "CONFIRMED" -> st in setOf("CONFIRMED", "PACKING")
                "SHIPPING" -> st in setOf("SHIPPING", "IN_DELIVERY")
                "DELIVERED" -> st in setOf("DELIVERED", "COMPLETED")
                "CANCELLED" -> st == "CANCELLED"
                else -> true
            }
            val matchQuery = q.isEmpty() ||
                    order.orderCode.lowercase().contains(q) ||
                    order.receiverName.lowercase().contains(q) ||
                    order.receiverPhone.lowercase().contains(q) ||
                    items.any { it.productName.lowercase().contains(q) }
            matchTab && matchQuery
        }
    }

    val numFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        containerColor = Color(0xFFF6F7FB),
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quản lý Đơn hàng (${ordersWithItems.size})",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm theo mã đơn (#SOLE...), tên khách, SĐT...", fontSize = 13.sp) },
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
                items(filterTabs) { (code, label) ->
                    val isSelected = selectedFilter == code
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) Color(0xFF1A1A1A) else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF1A1A1A) else Color(0xFFE5E7EB)),
                        modifier = Modifier.clickable { selectedFilter = code }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF4B5563),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1A1A1A))
                }
            } else if (filteredOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có đơn hàng nào trong mục này", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders, key = { it.first.orderId }) { (order, items) ->
                        val stUpper = order.orderStatus.uppercase()
                        val isQrUnpaid = order.paymentMethod == "QR_CODE" &&
                                order.paymentStatus.uppercase() !in setOf("SUCCESS", "PAID") &&
                                stUpper != "CANCELLED"
                        val isBusy = processingOrderId == order.orderId

                        val nextStatusPair = when {
                            isQrUnpaid || stUpper == "AWAITING_PAYMENT" -> "CONFIRMED" to "Duyệt QR & Xác nhận"
                            stUpper == "PENDING" -> "CONFIRMED" to "Xác nhận đơn"
                            stUpper == "CONFIRMED" -> "PACKING" to "Đóng gói"
                            stUpper == "PACKING" -> "SHIPPING" to "Giao vận chuyển"
                            stUpper in setOf("SHIPPING", "IN_DELIVERY") -> "DELIVERED" to "Xác nhận đã giao"
                            else -> null
                        }

                        val canCancel = stUpper in setOf("PENDING", "AWAITING_PAYMENT", "CONFIRMED", "PACKING")

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOrderForDetail = order to items }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${order.orderCode}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF111827)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (order.paymentStatus.uppercase() in setOf("SUCCESS", "PAID")) Color(0xFFDCFCE7)
                                                    else Color(0xFFFEF3C7)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${order.paymentMethod} • ${order.paymentStatus}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = if (order.paymentStatus.uppercase() in setOf("SUCCESS", "PAID")) Color(0xFF15803D) else Color(0xFFD97706)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF3F4F6))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = order.orderStatus,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF1F2937)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Khách: ${order.receiverName} • ${order.receiverPhone}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151)
                                )
                                Text(
                                    text = "Địa chỉ: ${order.shippingAddress}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                                Text(
                                    text = "Sản phẩm: ${items.joinToString { "${it.productName} (${it.color}, Cỡ ${it.size} x${it.quantity})" }}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4B5563),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${numFormat.format(order.totalAmount.toLong())} ₫",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFE53935),
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = dateFormat.format(order.createdAt.toDate()),
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (canCancel) {
                                            OutlinedButton(
                                                onClick = { orderPendingAdminCancel = order to items },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(16.dp),
                                                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("Hủy đơn", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (nextStatusPair != null) {
                                            Button(
                                                onClick = {
                                                    val (nextStatus, _) = nextStatusPair
                                                    scope.launch {
                                                        processingOrderId = order.orderId
                                                        val res = if (isQrUnpaid || stUpper == "AWAITING_PAYMENT") {
                                                            orderRepository.adminConfirmQrPaymentAndOrder(order.orderId)
                                                        } else {
                                                            orderRepository.updateOrderStatus(order.orderId, nextStatus)
                                                        }
                                                        res.onSuccess {
                                                            notificationRepository.notifyOrderStatusChanged(order, nextStatus)
                                                            Toast.makeText(
                                                                context,
                                                                "Đã xác nhận đơn #${order.orderCode} sang $nextStatus!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            refreshOrders()
                                                        }.onFailure { err ->
                                                            Toast.makeText(
                                                                context,
                                                                "Lỗi cập nhật: ${err.message}",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                        processingOrderId = null
                                                    }
                                                },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text(
                                                    text = if (isBusy) "Đang xử lý..." else nextStatusPair.second,
                                                    fontSize = 11.5.sp,
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
            }
        }
    }

    // Dialog Xem Chi tiết Đơn hàng phía Admin
    if (selectedOrderForDetail != null) {
        val (order, items) = selectedOrderForDetail!!
        AlertDialog(
            onDismissRequest = { selectedOrderForDetail = null },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Chi tiết đơn hàng #${order.orderCode}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Trạng thái đơn: ${order.orderStatus}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    Text("Thanh toán: ${order.paymentMethod} (${order.paymentStatus})", fontSize = 13.sp)
                    Text("Người nhận: ${order.receiverName} - ${order.receiverPhone}", fontSize = 13.sp)
                    Text("Địa chỉ: ${order.shippingAddress}", fontSize = 12.5.sp, color = Color(0xFF4B5563))
                    if (!order.cancelReason.isNullOrBlank()) {
                        Text("Lý do hủy: ${order.cancelReason}", fontSize = 12.5.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    Text("DANH SÁCH SẢN PHẨM (${items.sumOf { it.quantity }}):", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                    items.forEach { it ->
                        Text(
                            text = "• ${it.productName} | Màu ${it.color}, Cỡ ${it.size} (SKU: ${it.skuSnapshot}) | SL: x${it.quantity} = ${numFormat.format((it.unitPrice * it.quantity).toLong())} ₫",
                            fontSize = 12.5.sp,
                            color = Color(0xFF111827)
                        )
                    }
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    Text("Tạm tính: ${numFormat.format(order.subtotal.toLong())} ₫", fontSize = 12.5.sp)
                    Text("Phí vận chuyển: ${numFormat.format(order.shippingFee.toLong())} ₫", fontSize = 12.5.sp)
                    if (order.discount > 0) {
                        Text("Giảm giá Voucher: -${numFormat.format(order.discount.toLong())} ₫", fontSize = 12.5.sp, color = Color(0xFF16A34A))
                    }
                    Text(
                        text = "Tổng thu: ${numFormat.format(order.totalAmount.toLong())} ₫",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color(0xFFE53935)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedOrderForDetail = null }) {
                    Text("Đóng", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Dialog Admin Hủy đơn hàng (kèm lý do & hoàn kho tự động)
    if (orderPendingAdminCancel != null) {
        val (order, items) = orderPendingAdminCancel!!
        var cancelReasonInput by remember { mutableStateOf("Admin hủy đơn theo yêu cầu / chưa nhận được thanh toán") }
        AlertDialog(
            onDismissRequest = { orderPendingAdminCancel = null },
            containerColor = Color.White,
            title = {
                Text("Hủy đơn hàng #${order.orderCode}", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tồn kho của các sản phẩm trong đơn sẽ được hoàn trả tự động và gửi thông báo tới khách hàng.",
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563)
                    )
                    OutlinedTextField(
                        value = cancelReasonInput,
                        onValueChange = { cancelReasonInput = it },
                        label = { Text("Lý do hủy đơn *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = cancelReasonInput.trim().ifBlank { "Cửa hàng hủy đơn & hoàn trả tồn kho" }
                        orderPendingAdminCancel = null
                        scope.launch {
                            processingOrderId = order.orderId
                            orderRepository.cancelUserPendingOrder(
                                orderId = order.orderId,
                                items = items,
                                reason = reason,
                                isAdminOverride = true
                            ).onSuccess {
                                notificationRepository.notifyOrderCancelled(
                                    order = order,
                                    reason = "Đơn hàng #${order.orderCode} đã bị hủy bởi cửa hàng (Lý do: $reason)."
                                )
                                Toast.makeText(context, "Đã hủy đơn #${order.orderCode} & hoàn kho!", Toast.LENGTH_SHORT).show()
                                refreshOrders()
                            }.onFailure { err ->
                                Toast.makeText(context, "Lỗi hủy đơn: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                            processingOrderId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Xác nhận hủy đơn", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderPendingAdminCancel = null }) {
                    Text("Đóng")
                }
            }
        )
    }
}
