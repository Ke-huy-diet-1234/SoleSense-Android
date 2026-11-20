package com.example.solesense.ui.admin.notification

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Notifications
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
import com.example.solesense.model.support.Notification
import com.example.solesense.model.support.NotificationCampaign
import com.example.solesense.repository.NotificationRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationManagementScreen(
    adminUid: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationRepository = remember { NotificationRepository() }

    var selectedTab by remember { mutableStateOf("AUTO") } // AUTO (Thông báo sự kiện), CAMPAIGNS (Chiến dịch gửi thủ công)
    var adminNotifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var campaigns by remember { mutableStateOf<List<NotificationCampaign>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isCreateCampaignOpen by remember { mutableStateOf(false) }

    fun refreshData() {
        scope.launch {
            isLoading = true
            notificationRepository.getAdminNotifications().onSuccess { adminNotifications = it }
            notificationRepository.getAllCampaigns().onSuccess { campaigns = it }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        containerColor = Color(0xFFF6F7FB),
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
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quản lý Thông báo",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { isCreateCampaignOpen = true },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gửi thông báo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        val isAuto = selectedTab == "AUTO"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = "AUTO" }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Sự kiện tự động (${adminNotifications.size})",
                                fontWeight = if (isAuto) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAuto) Color(0xFF111111) else Color.Gray,
                                fontSize = 13.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .background(if (isAuto) Color(0xFF111111) else Color.Transparent)
                            )
                        }

                        val isCamp = selectedTab == "CAMPAIGNS"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = "CAMPAIGNS" }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Chiến dịch đã gửi (${campaigns.size})",
                                fontWeight = if (isCamp) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCamp) Color(0xFF111111) else Color.Gray,
                                fontSize = 13.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .background(if (isCamp) Color(0xFF111111) else Color.Transparent)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault()) }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1A1A1A))
            }
        } else if (selectedTab == "AUTO") {
            if (adminNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chưa có thông báo sự kiện đơn hàng mới", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(adminNotifications, key = { it.notificationId }) { notif ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = notif.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color(0xFF111827)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.displayContent(),
                                    fontSize = 13.sp,
                                    color = Color(0xFF4B5563)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = dateFormat.format(notif.createdAt.toDate()),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        } else {
            if (campaigns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chưa có chiến dịch thông báo nào được gửi", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(campaigns, key = { it.campaignId }) { camp ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = camp.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = Color(0xFF111827),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFDCFCE7))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "${camp.type} • ${camp.status}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = camp.content,
                                    fontSize = 13.sp,
                                    color = Color(0xFF4B5563)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Đối tượng: ${camp.targetType} • ${dateFormat.format(camp.createdAt.toDate())}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCreateCampaignOpen) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var imageUrl by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("PROMOTION") } // PROMOTION, SYSTEM
        var targetType by remember { mutableStateOf("ALL") } // ALL, NEW_CUSTOMER
        var isSending by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { isCreateCampaignOpen = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gửi thông báo thủ công", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    IconButton(onClick = { isCreateCampaignOpen = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tiêu đề thông báo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Nội dung chi tiết") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Link ảnh minh họa (Không bắt buộc)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Loại thông báo:", fontSize = 12.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == "PROMOTION",
                            onClick = { type = "PROMOTION" },
                            label = { Text("🎁 Khuyến mãi", maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = type == "SYSTEM",
                            onClick = { type = "SYSTEM" },
                            label = { Text("⚙️ Hệ thống", maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Đối tượng nhận:", fontSize = 12.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = targetType == "ALL",
                            onClick = { targetType = "ALL" },
                            label = { Text("Tất cả khách", maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = targetType == "NEW_CUSTOMER",
                            onClick = { targetType = "NEW_CUSTOMER" },
                            label = { Text("Khách mới", maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isBlank() || content.isBlank()) {
                            Toast.makeText(context, "Vui lòng nhập tiêu đề và nội dung", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            isSending = true
                            notificationRepository.createAndSendCampaign(
                                title = title,
                                content = content,
                                imageUrl = imageUrl,
                                type = type,
                                targetType = targetType,
                                adminUid = adminUid
                            ).onSuccess {
                                isCreateCampaignOpen = false
                                selectedTab = "CAMPAIGNS"
                                refreshData()
                                Toast.makeText(context, "Đã phát hành chiến dịch thông báo thành công!", Toast.LENGTH_SHORT).show()
                            }
                            isSending = false
                        }
                    },
                    enabled = !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text(if (isSending) "Đang gửi..." else "Phát hành ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { isCreateCampaignOpen = false }) {
                    Text("Hủy", color = Color.Gray)
                }
            }
        )
    }
}
