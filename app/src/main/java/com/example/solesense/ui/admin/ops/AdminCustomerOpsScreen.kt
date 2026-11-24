package com.example.solesense.ui.admin.ops

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.solesense.model.support.ReturnRequest
import com.example.solesense.model.support.Review
import com.example.solesense.model.user.User
import com.example.solesense.model.user.toSafeUser
import com.example.solesense.repository.NotificationRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminCustomerOpsScreen(
    adminUid: String,
    initialTab: String = "USERS", // USERS, REVIEWS, RETURNS
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var activeTab by remember { mutableStateOf(initialTab) }
    var isLoading by remember { mutableStateOf(true) }

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var returnRequests by remember { mutableStateOf<List<ReturnRequest>>(emptyList()) }

    var replyingToReview by remember { mutableStateOf<Review?>(null) }
    var storeReplyInput by remember { mutableStateOf("") }

    fun loadAllOpsData() {
        scope.launch {
            isLoading = true
            try {
                val uSnap = db.collection("users").get().await()
                users = uSnap.documents.map { it.toSafeUser(it.id) }

                val rSnap = db.collection("reviews").get().await()
                reviews = rSnap.documents.mapNotNull { doc ->
                    doc.toObject(Review::class.java)?.copy(reviewId = doc.id)
                }.sortedByDescending { it.createdAt.seconds }

                val retSnap = db.collection("returnRequests").get().await()
                returnRequests = retSnap.documents.mapNotNull { doc ->
                    doc.toObject(ReturnRequest::class.java)?.copy(returnId = doc.id)
                }.sortedByDescending { it.createdAt.seconds }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadAllOpsData()
    }

    val tabs = listOf(
        "USERS" to "Người dùng (${users.size})",
        "REVIEWS" to "Đánh giá (${reviews.size})",
        "RETURNS" to "Đổi / Trả (${returnRequests.size})"
    )

    Scaffold(
        containerColor = Color(0xFFF6F7FB),
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Column {
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
                            text = "Người dùng, Đánh giá & Đổi trả",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEach { (code, label) ->
                            val isSel = activeTab == code
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSel) Color(0xFF111827) else Color(0xFFF3F4F6),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { activeTab = code }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) Color.White else Color(0xFF4B5563),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (activeTab) {
                    "USERS" -> {
                        if (users.isEmpty()) {
                            item {
                                Text("Chưa có người dùng nào trên hệ thống", color = Color.Gray)
                            }
                        } else {
                            items(users, key = { it.userId }) { u ->
                                val isBlocked = u.status.uppercase() == "BLOCKED"
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
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
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = u.fullName.ifBlank { "Người dùng SoleSense" },
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFF111827)
                                                )
                                                Text(
                                                    text = u.email.ifBlank { "UID: ${u.userId}" },
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF6B7280)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isBlocked) Color(0xFFFEE2E2) else Color(0xFFDCFCE7))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "${u.role.uppercase()} • ${if (isBlocked) "ĐÃ KHÓA" else "HOẠT ĐỘNG"}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isBlocked) Color(0xFFDC2626) else Color(0xFF15803D)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("CUSTOMER", "SUPPORT", "ADMIN").forEach { targetRole ->
                                                val isCurrentRole = u.role.equals(targetRole, ignoreCase = true)
                                                OutlinedButton(
                                                    onClick = {
                                                        if (!isCurrentRole) {
                                                            scope.launch {
                                                                db.collection("users").document(u.userId).update(
                                                                    mapOf(
                                                                        "role" to targetRole,
                                                                        "updatedAt" to Timestamp.now()
                                                                    )
                                                                ).await()
                                                                Toast.makeText(
                                                                    context,
                                                                    "Đã gán quyền $targetRole cho ${u.fullName}!",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                loadAllOpsData()
                                                            }
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (isCurrentRole) Color(0xFF111827) else Color.Transparent,
                                                        contentColor = if (isCurrentRole) Color.White else Color(0xFF374151)
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(32.dp)
                                                ) {
                                                    Text(targetRole, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            if (u.userId != adminUid) {
                                                Button(
                                                    onClick = {
                                                        val nextStatus = if (isBlocked) "ACTIVE" else "BLOCKED"
                                                        scope.launch {
                                                            db.collection("users").document(u.userId).update(
                                                                mapOf(
                                                                    "status" to nextStatus,
                                                                    "updatedAt" to Timestamp.now()
                                                                )
                                                            ).await()
                                                            Toast.makeText(
                                                                context,
                                                                if (nextStatus == "BLOCKED") "Đã khóa tài khoản ${u.fullName}" else "Đã mở khóa tài khoản ${u.fullName}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            loadAllOpsData()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isBlocked) Color(0xFF16A34A) else Color(0xFFDC2626)
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text(
                                                        text = if (isBlocked) "Mở khóa" else "Khóa",
                                                        fontSize = 10.5.sp,
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

                    "REVIEWS" -> {
                        if (reviews.isEmpty()) {
                            item {
                                Text("Chưa có đánh giá sản phẩm nào", color = Color.Gray)
                            }
                        } else {
                            items(reviews, key = { it.reviewId }) { rev ->
                                val isHidden = rev.status.uppercase() == "HIDDEN"
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
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
                                                text = "${rev.userName} • ★ ${rev.rating}/5",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.5.sp,
                                                color = Color(0xFF111827)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isHidden) Color(0xFFFEE2E2) else Color(0xFFDCFCE7))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = if (isHidden) "ĐÃ ẨN" else "HIỂN THỊ",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isHidden) Color(0xFFDC2626) else Color(0xFF15803D)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Sản phẩm: ${rev.productId} (${rev.variantInfo})",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = rev.comment ?: "",
                                            fontSize = 13.sp,
                                            color = Color(0xFF111827)
                                        )

                                        if (!rev.storeReply.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFFF3F4F6),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Phản hồi của Shop: ${rev.storeReply}",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF374151),
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    replyingToReview = rev
                                                    storeReplyInput = rev.storeReply ?: "Cảm ơn bạn đã tin tưởng và ủng hộ SoleSense Chính Hãng!"
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Phản hồi khách", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    val nextSt = if (isHidden) "APPROVED" else "HIDDEN"
                                                    scope.launch {
                                                        db.collection("reviews").document(rev.reviewId).update(
                                                            mapOf(
                                                                "status" to nextSt,
                                                                "updatedAt" to Timestamp.now()
                                                            )
                                                        ).await()
                                                        Toast.makeText(
                                                            context,
                                                            if (nextSt == "HIDDEN") "Đã ẩn đánh giá" else "Đã hiển thị lại đánh giá",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        loadAllOpsData()
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text(
                                                    text = if (isHidden) "Hiện lại" else "Ẩn đánh giá",
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

                    "RETURNS" -> {
                        if (returnRequests.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Hiện chưa có yêu cầu Đổi / Trả hàng nào từ khách hàng.",
                                        color = Color.Gray,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }
                            }
                        } else {
                            items(returnRequests, key = { it.returnId }) { req ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
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
                                                text = if (req.type == "EXCHANGE") "YÊU CẦU ĐỔI SIZE" else "YÊU CẦU TRẢ HÀNG",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF111827)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFF3F4F6))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = req.status,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1F2937)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Mã đơn: ${req.orderId}", fontSize = 12.sp, color = Color(0xFF6B7280))
                                        Text("Lý do: ${req.reason}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                                        if (!req.requestedSize.isNullOrBlank()) {
                                            Text("Đổi từ Size ${req.currentSize ?: "?"} -> Size ${req.requestedSize}", fontSize = 12.5.sp, color = Color(0xFF1E88E5))
                                        }
                                        if (!req.description.isNullOrBlank()) {
                                            Text("Chi tiết: ${req.description}", fontSize = 12.sp, color = Color(0xFF4B5563))
                                        }
                                        Text(
                                            text = "Thời gian gửi: ${dateFormat.format(req.createdAt.toDate())}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )

                                        if (req.status.uppercase() == "PENDING") {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            val approvedNote = "Đã duyệt yêu cầu đổi/trả hàng. Nhân viên vận chuyển sẽ liên hệ thu hồi và gửi sản phẩm mới."
                                                            db.collection("returnRequests").document(req.returnId).update(
                                                                mapOf(
                                                                    "status" to "APPROVED",
                                                                    "adminNote" to approvedNote,
                                                                    "updatedAt" to Timestamp.now()
                                                                )
                                                            ).await()
                                                            NotificationRepository().notifyReturnRequestReviewed(
                                                                returnId = req.returnId,
                                                                targetUserId = req.userId,
                                                                orderId = req.orderId,
                                                                type = req.type,
                                                                status = "APPROVED",
                                                                adminNote = approvedNote
                                                            )
                                                            Toast.makeText(context, "Đã duyệt yêu cầu Đổi/Trả!", Toast.LENGTH_SHORT).show()
                                                            loadAllOpsData()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(34.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("Duyệt yêu cầu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        scope.launch {
                                                            val rejectedNote = "Yêu cầu chưa đủ điều kiện đổi trả theo chính sách cửa hàng."
                                                            db.collection("returnRequests").document(req.returnId).update(
                                                                mapOf(
                                                                    "status" to "REJECTED",
                                                                    "adminNote" to rejectedNote,
                                                                    "updatedAt" to Timestamp.now()
                                                                )
                                                            ).await()
                                                            NotificationRepository().notifyReturnRequestReviewed(
                                                                returnId = req.returnId,
                                                                targetUserId = req.userId,
                                                                orderId = req.orderId,
                                                                type = req.type,
                                                                status = "REJECTED",
                                                                adminNote = rejectedNote
                                                            )
                                                            Toast.makeText(context, "Đã từ chối yêu cầu Đổi/Trả", Toast.LENGTH_SHORT).show()
                                                            loadAllOpsData()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(34.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("Từ chối", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    }

    if (replyingToReview != null) {
        val targetRev = replyingToReview!!
        AlertDialog(
            onDismissRequest = { replyingToReview = null },
            containerColor = Color.White,
            title = {
                Text("Phản hồi đánh giá của ${targetRev.userName}", fontWeight = FontWeight.ExtraBold, fontSize = 16.5.sp)
            },
            text = {
                OutlinedTextField(
                    value = storeReplyInput,
                    onValueChange = { storeReplyInput = it },
                    label = { Text("Nội dung phản hồi từ SoleSense") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val cleanReply = storeReplyInput.trim()
                            db.collection("reviews").document(targetRev.reviewId).update(
                                mapOf(
                                    "storeReply" to cleanReply,
                                    "updatedAt" to Timestamp.now()
                                )
                            ).await()
                            if (cleanReply.isNotBlank()) {
                                NotificationRepository().notifyStoreRepliedToReview(
                                    reviewId = targetRev.reviewId,
                                    targetUserId = targetRev.userId,
                                    productId = targetRev.productId,
                                    storeReply = cleanReply
                                )
                            }
                            Toast.makeText(context, "Đã lưu phản hồi cửa hàng!", Toast.LENGTH_SHORT).show()
                            replyingToReview = null
                            loadAllOpsData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827))
                ) {
                    Text("Gửi phản hồi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingToReview = null }) {
                    Text("Đóng")
                }
            }
        )
    }
}
