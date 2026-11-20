package com.example.solesense.ui.notification

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.R
import com.example.solesense.model.support.Notification
import com.example.solesense.repository.NotificationRepository
import com.example.solesense.repository.OrderRepository
import com.example.solesense.utils.UserSessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    onNavigateToOrder: (String) -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationRepository = remember { NotificationRepository() }
    val orderRepository = remember { OrderRepository() }
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val userId = currentUser?.userId ?: notificationRepository.getCurrentUserId().orEmpty()

    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReadTab by remember { mutableStateOf("ALL") } // ALL, UNREAD
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, ORDER, PAYMENT, PROMOTION, SUPPORT, SYSTEM

    fun loadNotifications() {
        scope.launch {
            isLoading = true
            notificationRepository.getUserNotifications(userId).onSuccess { list ->
                notifications = list
            }
            isLoading = false
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            loadNotifications()
        } else {
            isLoading = false
        }
    }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    val filteredList = remember(notifications, selectedReadTab, selectedTypeFilter) {
        notifications.filter { n ->
            val matchRead = if (selectedReadTab == "UNREAD") !n.isRead else true
            val matchType = if (selectedTypeFilter == "ALL") true else n.type.equals(selectedTypeFilter, ignoreCase = true)
            matchRead && matchType
        }
    }

    val typeTabs = listOf(
        "ALL" to "Tất cả nhóm",
        "ORDER" to "📦 Đơn hàng",
        "PAYMENT" to "💳 Thanh toán",
        "PROMOTION" to "🎁 Khuyến mãi",
        "SUPPORT" to "💬 Hỗ trợ",
        "SYSTEM" to "⚙️ Hệ thống"
    )

    Scaffold(
        containerColor = Color(0xFFF8F9FC),
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Quay lại",
                                    tint = Color(0xFF111111)
                                )
                            }
                            Text(
                                text = "Thông báo",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color(0xFF111111)
                                )
                            )
                        }

                        if (unreadCount > 0) {
                            TextButton(
                                onClick = {
                                    notifications = notifications.map { it.copy(isRead = true) }
                                    NotificationRepository.updateUnreadCount(0)
                                    scope.launch {
                                        notificationRepository.markAllAsRead(userId)
                                        Toast.makeText(context, "Đã đánh dấu đọc tất cả thông báo", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Đọc tất cả",
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // 2 Tab chính: Tất cả | Chưa đọc
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        val isAllSelected = selectedReadTab == "ALL"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedReadTab = "ALL" }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tất cả (${notifications.size})",
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAllSelected) Color(0xFF111111) else Color.Gray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .background(if (isAllSelected) Color(0xFF111111) else Color.Transparent)
                            )
                        }

                        val isUnreadSelected = selectedReadTab == "UNREAD"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedReadTab = "UNREAD" }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Chưa đọc ($unreadCount)",
                                fontWeight = if (isUnreadSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isUnreadSelected) Color(0xFF111111) else Color.Gray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .background(if (isUnreadSelected) Color(0xFF111111) else Color.Transparent)
                            )
                        }
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
            // Dãy chip lọc theo 5 nhóm thông báo
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(typeTabs) { (code, label) ->
                    val isSelected = selectedTypeFilter == code
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) Color(0xFF1A1A1A) else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF1A1A1A) else Color(0xFFE5E7EB)),
                        modifier = Modifier.clickable { selectedTypeFilter = code }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isSelected) Color.White else Color(0xFF4B5563),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF111111))
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedReadTab == "UNREAD") "Bạn đã đọc hết tất cả thông báo" else "Chưa có thông báo nào",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.notificationId }) { item ->
                        NotificationCardItem(
                            notification = item,
                            onClick = {
                                if (!item.isRead) {
                                    notifications = notifications.map {
                                        if (it.notificationId == item.notificationId) it.copy(isRead = true) else it
                                    }
                                    NotificationRepository.updateUnreadCount(notifications.count { !it.isRead })
                                    scope.launch {
                                        notificationRepository.markAsRead(item.notificationId)
                                    }
                                }

                                // Điều hướng an toàn theo referenceType / type
                                when {
                                    (item.referenceType == "ORDER" || item.type == "ORDER" || item.type == "PAYMENT") && !item.referenceId.isNullOrBlank() -> {
                                        scope.launch {
                                            val orderCheck = orderRepository.getOrderDetailWithItems(item.referenceId)
                                            if (orderCheck.isSuccess) {
                                                onNavigateToOrder(item.referenceId)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Đơn hàng liên kết không còn tồn tại hoặc đã bị xóa",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                    item.type == "SUPPORT" || item.referenceType == "SUPPORT" -> {
                                        onNavigateToChat()
                                    }
                                    item.type == "PROMOTION" || item.referenceType == "VOUCHER" -> {
                                        onNavigateToExplore()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault()) }

    val (badgeEmoji, iconBgColor, typeLabel) = when (notification.type.uppercase()) {
        "ORDER" -> Triple("📦", Color(0xFFE0F2FE), "Đơn hàng")
        "PAYMENT" -> Triple("💳", Color(0xFFDCFCE7), "Thanh toán")
        "PROMOTION" -> Triple("🎁", Color(0xFFFEF3C7), "Khuyến mãi")
        "SUPPORT" -> Triple("💬", Color(0xFFF3E8FF), "Hỗ trợ")
        else -> Triple("🔔", Color(0xFFF3F4F6), "Hệ thống")
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (!notification.isRead) Color(0xFFF0F9FF) else Color.White,
        border = BorderStroke(
            1.dp,
            if (!notification.isRead) Color(0xFFBAE6FD) else Color(0xFFF1F5F9)
        ),
        shadowElevation = 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(text = badgeEmoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (!notification.isRead) FontWeight.ExtraBold else FontWeight.Bold,
                            color = Color(0xFF111827),
                            fontSize = 14.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.displayContent(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF4B5563),
                        lineHeight = 18.sp,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF6366F1),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "•",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                    Text(
                        text = dateFormat.format(notification.createdAt.toDate()),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
