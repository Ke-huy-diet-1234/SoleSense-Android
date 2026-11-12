package com.example.solesense.ui.support

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.R
import com.example.solesense.model.chat.Message
import com.example.solesense.model.support.SupportRequest
import com.example.solesense.model.user.User
import com.example.solesense.repository.ChatRepository
import kotlinx.coroutines.launch
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

    LaunchedEffect(Unit) {
        chatRepository.ensureCleanHistoryMigration()
    }

    // Lắng nghe danh sách ticket đang phụ trách (ASSIGNED) và đang chờ (WAITING) realtime từ Firestore
    val assignedRequests by chatRepository.getAssignedSupportRequestsFlow(user.userId).collectAsState(initial = emptyList())
    val waitingRequests by chatRepository.getWaitingSupportRequestsFlow().collectAsState(initial = emptyList())

    // Trạng thái hội thoại mà nhân viên đang mở để chat trực tiếp
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var activeRequestId by remember { mutableStateOf<String?>(null) }
    var activeCustomerName by remember { mutableStateOf("") }
    var isChatSheetOpen by remember { mutableStateOf(false) }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_solesense_logo),
                            contentDescription = "SoleSense Support",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Support Center",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A1A1A)
                                    )
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
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "Nhân viên hỗ trợ: ${user.fullName.ifBlank { "Support Specialist" }}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                    }

                    TextButton(onClick = onLogoutClick) {
                        Text(
                            text = "Đăng xuất",
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold
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
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Hàng chờ hỗ trợ & Chuyển giao AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mô hình Một cuộc hội thoại – Một nhân viên chính. Khi tiếp nhận, hệ thống sử dụng Firestore Transaction để phân quyền duy nhất và đảm bảo an toàn nghiệp vụ.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE0E0E0),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            // 1. DANH SÁCH HỘI THOẠI NHÂN VIÊN ĐANG PHỤ TRÁCH (ASSIGNED)
            if (assignedRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "ĐANG PHỤ TRÁCH (${assignedRequests.size})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    )
                }

                items(assignedRequests, key = { "assigned_${it.requestId}" }) { req ->
                    AssignedSupportTicketCard(
                        ticket = req,
                        onOpenChatClick = {
                            activeChatId = req.chatId
                            activeRequestId = req.requestId
                            activeCustomerName = req.customerName
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

            // 2. DANH SÁCH YÊU CẦU ĐANG CHỜ TIẾP NHẬN (WAITING)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YÊU CẦU ĐANG CHỜ (${waitingRequests.size})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF616161)
                        )
                    )
                }
            }

            if (waitingRequests.isEmpty()) {
                item {
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
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Hiện không có yêu cầu nào đang chờ",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tất cả khách hàng đang được SoleSense AI tư vấn tự động hoặc đã có nhân viên tiếp nhận.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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
                                    isChatSheetOpen = true
                                } else {
                                    Toast.makeText(context, "Không thể nhận yêu cầu: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSwitchToCustomerView,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Xem giao diện Khách hàng (Customer Home)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }
        }
    }

    // Sheet Chat trực tiếp giữa Nhân viên và Khách hàng
    if (isChatSheetOpen && activeChatId != null) {
        StaffLiveChatBottomSheet(
            chatId = activeChatId!!,
            requestId = activeRequestId ?: "",
            customerName = activeCustomerName,
            staffUser = user,
            chatRepository = chatRepository,
            onDismiss = { isChatSheetOpen = false }
        )
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE3F2FD))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Đang hỗ trợ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = ticket.topic,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = ticket.previewMessage.ifBlank { "Đang trao đổi trực tiếp với khách hàng..." },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF616161),
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenChatClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text("Mở khung chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onResolveClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Kết thúc", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Chờ nhân viên",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = ticket.topic,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = ticket.previewMessage.ifBlank { "Khách hàng muốn tư vấn trực tiếp từ nhân viên..." },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF616161),
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAcceptClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
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
 * Giao diện màn hình Chat phía Nhân viên để trao đổi trực tiếp với Khách
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffLiveChatBottomSheet(
    chatId: String,
    requestId: String,
    customerName: String,
    staffUser: User,
    chatRepository: ChatRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val messages by chatRepository.getMessagesFlow(chatId).collectAsState(initial = emptyList())
    val listState = rememberLazyListState()
    var inputStaffText by remember { mutableStateOf("") }

    // Tự động đồng bộ trạng thái "Nhân viên đang soạn tin..." (isStaffTyping) lên Firestore
    LaunchedEffect(inputStaffText.isNotEmpty()) {
        chatRepository.setTypingStatus(
            chatId = chatId,
            isStaff = true,
            isTyping = inputStaffText.isNotEmpty()
        )
    }

    DisposableEffect(chatId) {
        onDispose {
            scope.launch {
                chatRepository.setTypingStatus(chatId = chatId, isStaff = true, isTyping = false)
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
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hỗ trợ: $customerName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Bạn đang chịu trách nhiệm giải quyết phiên này",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4CAF50), fontSize = 11.sp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Kết thúc", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Đóng")
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
