package com.example.solesense.ui.support

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.R
import com.example.solesense.data.repository.ProductRepository
import com.example.solesense.model.catalog.Product
import com.example.solesense.model.chat.Message
import com.example.solesense.model.order.Order
import com.example.solesense.model.support.SupportRequest
import com.example.solesense.model.user.User
import com.example.solesense.repository.ChatRepository
import com.example.solesense.repository.OrderRepository
import com.example.solesense.ui.admin.ops.AdminCustomerOpsScreen
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDashboardScreen(
    user: User,
    onSwitchToCustomerView: () -> Unit = {},
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chatRepository = remember { ChatRepository() }

    var showCustomerOpsSubScreen by remember { mutableStateOf(false) }
    var selectedTicketTab by remember { mutableStateOf("WAITING") } // WAITING, ASSIGNED, RESOLVED

    // Lắng nghe danh sách ticket đang phụ trách (ASSIGNED), đang chờ (WAITING) và đã hoàn tất (RESOLVED) realtime từ Firestore
    val assignedRequests by chatRepository.getAssignedSupportRequestsFlow(user.userId).collectAsState(initial = emptyList())
    val waitingRequests by chatRepository.getWaitingSupportRequestsFlow().collectAsState(initial = emptyList())
    val resolvedRequests by chatRepository.getResolvedSupportRequestsFlow().collectAsState(initial = emptyList())

    // Trạng thái hội thoại mà nhân viên đang mở để chat trực tiếp
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var activeRequestId by remember { mutableStateOf<String?>(null) }
    var activeCustomerName by remember { mutableStateOf("") }
    var activeCustomerUserId by remember { mutableStateOf("") }
    var isActiveChatReadOnly by remember { mutableStateOf(false) }
    var isChatSheetOpen by remember { mutableStateOf(false) }

    if (showCustomerOpsSubScreen) {
        AdminCustomerOpsScreen(
            adminUid = user.userId,
            initialTab = "RETURNS",
            onBackClick = { showCustomerOpsSubScreen = false }
        )
        return
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
                            contentDescription = "SoleSense Support",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Trung tâm Hỗ trợ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A1A1A)
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE3F2FD))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "STAFF",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF1E88E5),
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Text(
                                text = "Chuyên viên: ${user.fullName.ifBlank { "Hỗ trợ viên" }}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                maxLines = 1
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
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Hàng chờ Hỗ trợ & Chuyển giao AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mô hình Một cuộc hội thoại – Một nhân viên chính (Firestore Transaction). Hỗ trợ tra cứu đơn hàng của khách, tra cứu tồn kho và câu trả lời mẫu ngay trong khung chat.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE0E0E0),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            // Thanh 3 Tab: Chờ tiếp nhận (WAITING) | Đang phụ trách (ASSIGNED) | Đã hoàn tất (RESOLVED)
            item {
                val tabs = listOf(
                    "WAITING" to "Chờ nhận (${waitingRequests.size})",
                    "ASSIGNED" to "Đang xử lý (${assignedRequests.size})",
                    "RESOLVED" to "Đã xong (${resolvedRequests.size})"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEach { (code, label) ->
                        val isSel = selectedTicketTab == code
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) Color(0xFF1976D2) else Color.White,
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF1976D2) else Color(0xFFE5E7EB)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedTicketTab = code }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.White else Color(0xFF374151),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            when (selectedTicketTab) {
                "ASSIGNED" -> {
                    if (assignedRequests.isEmpty()) {
                        item {
                            EmptyTicketCard(
                                title = "Bạn chưa nhận phiên hội thoại nào",
                                subtitle = "Hãy chuyển sang tab 'Chờ nhận' để tiếp nhận yêu cầu hỗ trợ từ khách hàng."
                            )
                        }
                    } else {
                        items(assignedRequests, key = { "assigned_${it.requestId}" }) { req ->
                            AssignedSupportTicketCard(
                                ticket = req,
                                onOpenChatClick = {
                                    activeChatId = req.chatId
                                    activeRequestId = req.requestId
                                    activeCustomerName = req.customerName
                                    activeCustomerUserId = req.userId
                                    isActiveChatReadOnly = false
                                    isChatSheetOpen = true
                                },
                                onResolveClick = {
                                    scope.launch {
                                        chatRepository.staffResolveTicket(req.requestId, req.chatId)
                                        Toast.makeText(context, "Đã hoàn tất hỗ trợ cho ${req.customerName}!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                "WAITING" -> {
                    if (waitingRequests.isEmpty()) {
                        item {
                            EmptyTicketCard(
                                title = "Hiện không có yêu cầu nào đang chờ",
                                subtitle = "Tất cả khách hàng đang được SoleSense AI tư vấn tự động hoặc đã có nhân viên tiếp nhận."
                            )
                        }
                    } else {
                        items(waitingRequests, key = { "waiting_${it.requestId}" }) { req ->
                            RealSupportTicketCard(
                                ticket = req,
                                onAcceptClick = {
                                    scope.launch {
                                        val result = chatRepository.staffAssignTicketTransaction(
                                            requestId = req.requestId,
                                            staffId = user.userId,
                                            staffName = user.fullName.ifBlank { "Nhân viên hỗ trợ" }
                                        )
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "Đã tiếp nhận yêu cầu từ ${req.customerName}!", Toast.LENGTH_SHORT).show()
                                            activeChatId = req.chatId
                                            activeRequestId = req.requestId
                                            activeCustomerName = req.customerName
                                            activeCustomerUserId = req.userId
                                            isActiveChatReadOnly = false
                                            selectedTicketTab = "ASSIGNED"
                                            isChatSheetOpen = true
                                        } else {
                                            Toast.makeText(context, "Không thể nhận yêu cầu: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                "RESOLVED" -> {
                    if (resolvedRequests.isEmpty()) {
                        item {
                            EmptyTicketCard(
                                title = "Chưa có lịch sử phiên hoàn tất",
                                subtitle = "Các phiên hỗ trợ sau khi nhân viên bấm 'Kết thúc' sẽ được lưu trữ tại đây."
                            )
                        }
                    } else {
                        items(resolvedRequests, key = { "resolved_${it.requestId}" }) { req ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = req.customerName.ifBlank { "Khách hàng" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF111827)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFDCFCE7))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "ĐÃ HOÀN TẤT",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Phụ trách: ${req.assignedStaffName ?: "Nhân viên"} • Chủ đề: ${req.topic}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            activeChatId = req.chatId
                                            activeRequestId = req.requestId
                                            activeCustomerName = req.customerName
                                            activeCustomerUserId = req.userId
                                            isActiveChatReadOnly = true
                                            isChatSheetOpen = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Xem lại lịch sử hội thoại", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showCustomerOpsSubScreen = true }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Xử lý Yêu cầu Đổi/Trả hàng & Phản hồi Đánh giá",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tiếp nhận yêu cầu đổi size, trả hàng và gửi phản hồi chính thức của cửa hàng cho đánh giá của khách.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
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

    // Sheet Chat trực tiếp giữa Nhân viên và Khách hàng (kèm Câu trả lời mẫu, Tra cứu Đơn hàng & Tồn kho)
    if (isChatSheetOpen && activeChatId != null) {
        StaffLiveChatBottomSheet(
            chatId = activeChatId!!,
            requestId = activeRequestId ?: "",
            customerName = activeCustomerName,
            customerUserId = activeCustomerUserId,
            isReadOnly = isActiveChatReadOnly,
            staffUser = user,
            chatRepository = chatRepository,
            onDismiss = { isChatSheetOpen = false }
        )
    }
}

@Composable
private fun EmptyTicketCard(title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AssignedSupportTicketCard(
    ticket: SupportRequest,
    onOpenChatClick: () -> Unit,
    onResolveClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.customerName.ifBlank { "Khách hàng" },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE3F2FD))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ĐANG PHỤ TRÁCH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Chủ đề: ${ticket.topic.ifBlank { "Tư vấn sản phẩm" }}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF424242),
                    lineHeight = 19.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenChatClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Mở phòng Chat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onResolveClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Hoàn tất",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RealSupportTicketCard(
    ticket: SupportRequest,
    onAcceptClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.customerName.ifBlank { "Khách hàng" },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "CHỜ TIẾP NHẬN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Chủ đề: ${ticket.topic.ifBlank { "Tư vấn sản phẩm & đặt hàng" }}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF424242),
                    lineHeight = 19.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Mã phiên: #${ticket.requestId.takeLast(6).uppercase()}",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E9E9E))
            )

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onAcceptClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tiếp nhận hỗ trợ ngay",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Giao diện màn hình Chat phía Nhân viên để trao đổi trực tiếp với Khách:
 * - Tích hợp Tra cứu Đơn hàng của khách
 * - Tích hợp Tra cứu Tồn kho & Gửi gợi ý sản phẩm
 * - Tích hợp Câu trả lời mẫu (Quick Replies)
 * - Tích hợp Bàn giao về hàng chờ (Transfer) & Hoàn tất hỗ trợ
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffLiveChatBottomSheet(
    chatId: String,
    requestId: String,
    customerName: String,
    customerUserId: String,
    isReadOnly: Boolean,
    staffUser: User,
    chatRepository: ChatRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by chatRepository.getMessagesFlow(chatId).collectAsState(initial = emptyList())
    val listState = rememberLazyListState()
    var inputStaffText by remember { mutableStateOf("") }
    var activeHelperPanel by remember { mutableStateOf<String?>(null) } // "ORDERS", "STOCK"

    var customerOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var catalogProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    val numFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }

    val quickReplies = remember {
        listOf(
            "Xin chào $customerName! Mình là chuyên viên hỗ trợ SoleSense, mình có thể giúp gì cho bạn ạ?",
            "SoleSense hỗ trợ đổi size miễn phí trong vòng 7 ngày kể từ khi nhận hàng nếu giày còn nguyên tem mác.",
            "Bảng quy đổi size chuẩn EU: Size 39 (24.5cm), Size 40 (25cm), Size 41 (25.5cm), Size 42 (26.5cm), Size 43 (27cm).",
            "Bạn có thể chọn Thanh toán VietQR (hệ thống giữ tồn kho 5 phút) hoặc Thanh toán khi nhận hàng (COD) nhé!"
        )
    }

    LaunchedEffect(activeHelperPanel) {
        if (activeHelperPanel == "ORDERS" && customerOrders.isEmpty()) {
            val resolvedUid = customerUserId.ifBlank { chatId.removePrefix("chat_") }
            OrderRepository().getUserOrdersWithItems(resolvedUid).onSuccess { list ->
                customerOrders = list.map { it.first }
            }
        } else if (activeHelperPanel == "STOCK" && catalogProducts.isEmpty()) {
            ProductRepository().getProducts().onSuccess { list ->
                catalogProducts = list
            }
        }
    }

    // Tự động đồng bộ trạng thái "Nhân viên đang soạn tin..." (isStaffTyping) lên Firestore
    LaunchedEffect(inputStaffText.isNotEmpty(), isReadOnly) {
        if (!isReadOnly) {
            chatRepository.setTypingStatus(
                chatId = chatId,
                isStaff = true,
                isTyping = inputStaffText.isNotEmpty()
            )
        }
    }

    DisposableEffect(chatId) {
        onDispose {
            if (!isReadOnly) {
                scope.launch {
                    chatRepository.setTypingStatus(chatId = chatId, isStaff = true, isTyping = false)
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Khách hàng: $customerName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = if (isReadOnly) "Phiên đã hoàn tất (Chế độ xem lịch sử)" else "Bạn đang trực tiếp phụ trách phiên này",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isReadOnly) Color.Gray else Color(0xFF2E7D32),
                            fontSize = 11.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!isReadOnly) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    chatRepository.staffReleaseTicketToWaiting(
                                        requestId = requestId,
                                        chatId = chatId,
                                        staffName = staffUser.fullName.ifBlank { "Nhân viên" }
                                    )
                                    Toast.makeText(context, "Đã bàn giao phiên về hàng chờ!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Bàn giao", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    chatRepository.setTypingStatus(chatId, isStaff = true, isTyping = false)
                                    chatRepository.staffResolveTicket(requestId, chatId)
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Kết thúc", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Đóng")
                    }
                }
            }

            // Thanh công cụ nghiệp vụ cho Nhân viên: Tra cứu Đơn hàng của khách & Tra cứu Tồn kho
            if (!isReadOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FC))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeHelperPanel == "ORDERS") Color(0xFF1976D2) else Color.White,
                        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                activeHelperPanel = if (activeHelperPanel == "ORDERS") null else "ORDERS"
                            }
                    ) {
                        Text(
                            text = "📦 Tra cứu Đơn của khách",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeHelperPanel == "ORDERS") Color.White else Color(0xFF1F2937),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeHelperPanel == "STOCK") Color(0xFF1976D2) else Color.White,
                        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                activeHelperPanel = if (activeHelperPanel == "STOCK") null else "STOCK"
                            }
                    ) {
                        Text(
                            text = "👟 Tra cứu Kho & Gợi ý SP",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeHelperPanel == "STOCK") Color.White else Color(0xFF1F2937),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }

                if (activeHelperPanel == "ORDERS") {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                    ) {
                        if (customerOrders.isEmpty()) {
                            Text(
                                text = "Khách hàng này chưa có đơn hàng nào trên hệ thống.",
                                fontSize = 12.sp,
                                color = Color(0xFF4B5563),
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(customerOrders, key = { it.orderId }) { ord ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "#${ord.orderCode} • ${ord.orderStatus} (${ord.paymentMethod})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF111827)
                                            )
                                            Text(
                                                text = "Tổng: ${numFormat.format(ord.totalAmount.toLong())} ₫ • ĐC: ${ord.shippingAddress}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF6B7280),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                inputStaffText = "Đơn hàng #${ord.orderCode} của bạn hiện đang ở trạng thái ${ord.orderStatus} (Tổng: ${numFormat.format(ord.totalAmount.toLong())} ₫)."
                                                activeHelperPanel = null
                                            }
                                        ) {
                                            Text("Chèn vào tin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (activeHelperPanel == "STOCK") {
                    Surface(
                        color = Color(0xFFF0FDF4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(catalogProducts, key = { it.productId }) { prod ->
                                val price = prod.salePrice ?: prod.basePrice
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${prod.name} (${prod.brandId.uppercase()})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF111827),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Giá: ${numFormat.format(price.toLong())} ₫ • Tồn kho khả dụng: ${prod.totalStockQuantity} đôi",
                                            fontSize = 11.sp,
                                            color = if (prod.totalStockQuantity > 0) Color(0xFF15803D) else Color(0xFFDC2626)
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            inputStaffText = "Mình gợi ý cho bạn mẫu ${prod.name} (Giá ưu đãi: ${numFormat.format(price.toLong())} ₫, hiện còn ${prod.totalStockQuantity} đôi sẵn kho) nhé!"
                                            activeHelperPanel = null
                                        }
                                    ) {
                                        Text("Gợi ý mẫu này", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // Lịch sử tin nhắn (Bao gồm cả tư vấn trước đó của AI và câu hỏi của khách)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.messageId }) { msg ->
                    when (msg.senderType) {
                        "USER" -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                                    color = Color(0xFF1A1A1A),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = customerName, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(text = msg.content, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        "STAFF" -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                                    color = Color(0xFF1976D2),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = "Bạn (Nhân viên)", color = Color(0xFFBBDEFB), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(text = msg.content, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        "AI" -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF1F1F5),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = "SoleSense AI (Lịch sử)", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(text = msg.content, color = Color(0xFF333333), fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        "SYSTEM" -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF0F0F0)) {
                                    Text(
                                        text = msg.content,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isReadOnly) {
                // Thanh Câu trả lời mẫu (Quick Replies)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickReplies) { tpl ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF3F4F6),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { inputStaffText = tpl }
                        ) {
                            Text(
                                text = tpl.take(42) + if (tpl.length > 42) "..." else "",
                                fontSize = 11.sp,
                                color = Color(0xFF374151),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Thanh gửi tin nhắn của nhân viên
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputStaffText,
                        onValueChange = { inputStaffText = it },
                        placeholder = { Text("Nhập tin nhắn trả lời khách...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F7),
                            unfocusedContainerColor = Color(0xFFF5F5F7),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            val text = inputStaffText.trim()
                            if (text.isNotEmpty()) {
                                inputStaffText = ""
                                scope.launch {
                                    chatRepository.setTypingStatus(chatId, isStaff = true, isTyping = false)
                                    val staffMsg = Message(
                                        messageId = UUID.randomUUID().toString(),
                                        senderType = "STAFF",
                                        senderId = staffUser.userId,
                                        senderName = staffUser.fullName.ifBlank { "Nhân viên hỗ trợ" },
                                        content = text,
                                        messageType = "TEXT",
                                        sendStatus = "SENT"
                                    )
                                    chatRepository.sendMessage(chatId, staffMsg)
                                }
                            }
                        })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(enabled = inputStaffText.isNotBlank()) {
                                val text = inputStaffText.trim()
                                if (text.isNotEmpty()) {
                                    inputStaffText = ""
                                    scope.launch {
                                        chatRepository.setTypingStatus(chatId, isStaff = true, isTyping = false)
                                        val staffMsg = Message(
                                            messageId = UUID.randomUUID().toString(),
                                            senderType = "STAFF",
                                            senderId = staffUser.userId,
                                            senderName = staffUser.fullName.ifBlank { "Nhân viên hỗ trợ" },
                                            content = text,
                                            messageType = "TEXT",
                                            sendStatus = "SENT"
                                        )
                                        chatRepository.sendMessage(chatId, staffMsg)
                                    }
                                }
                            },
                        shape = CircleShape,
                        color = if (inputStaffText.isNotBlank()) Color(0xFF1976D2) else Color(0xFFE0E0E0)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_send),
                                contentDescription = "Gửi",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
