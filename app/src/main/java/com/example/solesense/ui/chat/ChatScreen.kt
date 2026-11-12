package com.example.solesense.ui.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.solesense.R
import com.example.solesense.model.catalog.Product
import com.example.solesense.model.chat.Chat
import com.example.solesense.model.chat.Message
import com.example.solesense.ui.components.LoginRequiredDialog
import com.example.solesense.utils.UserSessionManager
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToAuth: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    viewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val isLoggedIn = currentUser != null
    var showLoginRequiredDialog by remember { mutableStateOf(!isLoggedIn) }

    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Gom gọn các tin nhắn SYSTEM liên tiếp nhau (chỉ giữ tin SYSTEM mới nhất trong chuỗi liên tiếp)
    // để tránh làm loãng khung chat khi khách bấm chuyển/hủy nhiều lần
    val displayMessages = remember(uiState.messages) {
        val result = mutableListOf<Message>()
        for (msg in uiState.messages) {
            if (msg.senderType == "SYSTEM" && result.lastOrNull()?.senderType == "SYSTEM") {
                result[result.lastIndex] = msg
            } else {
                result.add(msg)
            }
        }
        result
    }

    val isAiCurrentlyProcessing = remember(uiState.messages) {
        uiState.messages.any { it.senderType == "AI" && it.sendStatus == "PROCESSING" }
    }

    // Tự động cuộn xuống tin nhắn cuối cùng khi có tin mới
    LaunchedEffect(displayMessages.size, displayMessages.lastOrNull()?.sendStatus) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size - 1)
        }
    }

    // Sử dụng Column thay vì nested Scaffold để KHÔNG bị cộng dồn navigationBarsPadding hai lần
    // (Khắc phục hoàn toàn khoảng trắng thừa giữa ô Nhập tin nhắn và BottomNavigationBar)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
    ) {
        // 1. HEADER ĐỘNG 3 TRẠNG THÁI (Đã chống tràn chữ, chống rớt dòng)
        ChatHeader(
            chat = uiState.chat,
            onBackClick = onBackClick,
            onHandoffClick = { viewModel.onHandoffToStaffClick() },
            onCancelHandoffClick = { viewModel.cancelWaitingHandoff() },
            onReturnToAiClick = { viewModel.promptReturnToAi() }
        )

        // 2. BANNER GHIM SẢN PHẨM ĐANG QUAN TÂM (TỪ FAB PRODUCT DETAIL)
        AnimatedVisibility(
            visible = uiState.isPinnedBannerVisible && uiState.pinnedProduct != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.pinnedProduct?.let { pinned ->
                PinnedProductBanner(
                    product = pinned,
                    onAskClick = { viewModel.askAboutPinnedProduct() },
                    onDismiss = { viewModel.dismissPinnedBanner() }
                )
            }
        }

        // 3. DANH SÁCH TIN NHẮN
        if (!isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_bot_avatar),
                        contentDescription = "SoleSense AI",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Đăng nhập để trò chuyện với SoleSense AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Lịch sử tư vấn chọn giày và hỗ trợ kỹ thuật sẽ được đồng bộ theo tài khoản của bạn.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showLoginRequiredDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Text("Đăng nhập ngay", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(displayMessages, key = { it.messageId }) { message ->
                    when (message.senderType) {
                        "SYSTEM" -> {
                            SystemMessageItem(message = message)
                        }
                        "USER" -> {
                            UserMessageItem(
                                message = message,
                                onProductClick = onProductClick
                            )
                        }
                        "AI" -> {
                            AiMessageItem(
                                message = message,
                                productsMap = uiState.productsMap,
                                onProductClick = onProductClick,
                                onRetryClick = { viewModel.retryAiRequest(message) },
                                onQuickQuestionClick = { q ->
                                    if (!isAiCurrentlyProcessing) {
                                        viewModel.sendTextMessage(q)
                                    }
                                },
                                onWhyRecommendClick = { prod, reason, exps ->
                                    viewModel.openWhyAiRecommended(prod, reason, exps)
                                }
                            )
                        }
                        "STAFF" -> {
                            StaffMessageItem(message = message)
                        }
                    }
                }

                // Hiệu ứng "Nhân viên đang gõ..."
                if (uiState.chat?.isStaffTyping == true) {
                    item {
                        TypingIndicatorBubble(name = uiState.chat?.assignedStaffName ?: "Nhân viên")
                    }
                }
            }

            // Thông báo nhỏ khi đang ở chế độ chờ nhân viên tiếp nhận
            if (uiState.chat?.mode == "HUMAN" && uiState.chat?.status == "WAITING") {
                Surface(
                    color = Color(0xFFFFF8E1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Đang chờ nhân viên nhận phiếu. Muốn hỏi AI ngay?",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = Color(0xFFE65100)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Quay lại AI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315)
                            ),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable { viewModel.cancelWaitingHandoff() }
                        )
                    }
                }
            }

            // 4. THANH NHẬP TIN NHẮN (Sát khít thanh BottomBar, không còn khoảng trống thừa)
            ChatInputBar(
                text = uiState.inputText,
                onTextChanged = viewModel::onInputTextChanged,
                onSendClick = viewModel::sendCurrentInput,
                enabled = !uiState.isLoading && !isAiCurrentlyProcessing,
                placeholderText = when {
                    isAiCurrentlyProcessing -> "SoleSense AI đang trả lời..."
                    uiState.chat?.mode == "HUMAN" && uiState.chat?.status == "WAITING" -> "Nhắn thêm thông tin cho nhân viên..."
                    uiState.chat?.mode == "HUMAN" -> "Nhắn tin với nhân viên hỗ trợ..."
                    else -> "Nhập tin nhắn..."
                }
            )
        }
    }

    // 5. DIALOG: QUAY LẠI TƯ VẤN AI?
    if (uiState.showReturnToAiDialog) {
        ReturnToAiDialog(
            onConfirmReturn = { viewModel.confirmReturnToAiFromAssigned() },
            onDismiss = { viewModel.dismissReturnToAiDialog() }
        )
    }

    // 6. DIALOG ĐÁNH GIÁ HỖ TRỢ (Khi nhân viên kết thúc phiên hỗ trợ)
    if (uiState.showSupportRatingDialog) {
        SupportRatingDialog(
            onDismiss = {
                viewModel.dismissSupportRatingDialog()
            },
            onSubmit = { stars ->
                viewModel.dismissSupportRatingDialog()
                Toast.makeText(
                    context,
                    "Cảm ơn bạn đã đánh giá $stars sao cho dịch vụ hỗ trợ SoleSense!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // 7. BOTTOM SHEET: VÌ SAO AI ĐỀ XUẤT?
    uiState.whyRecommendedDetail?.let { detail ->
        WhyAiRecommendedBottomSheet(
            detail = detail,
            onDismiss = { viewModel.closeWhyAiRecommended() }
        )
    }

    // Dialog yêu cầu đăng nhập
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
}

/**
 * Header thay đổi linh hoạt theo 3 trạng thái, đã tối ưu bố cục Row:
 * - Sử dụng weight(1f, fill = false) và softWrap = false để TagPill ("AI", "Chờ", "Staff")
 *   và dòng phụ đề tuyệt đối không bao giờ bị ép dọc hay rớt chữ.
 */
@Composable
fun ChatHeader(
    chat: Chat?,
    onBackClick: () -> Unit,
    onHandoffClick: () -> Unit,
    onCancelHandoffClick: () -> Unit,
    onReturnToAiClick: () -> Unit
) {
    val mode = chat?.mode ?: "AI"
    val status = chat?.status ?: "ACTIVE"

    Surface(
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cụm bên trái: Nút Back + Avatar + Tiêu đề & Trạng thái
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Quay lại",
                        tint = Color(0xFF1E1E1E),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                when {
                    mode == "AI" -> {
                        Image(
                            painter = painterResource(id = R.drawable.ic_bot_avatar),
                            contentDescription = "SoleSense AI",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SoleSense AI",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1A1A1A)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TagPill(text = "AI", color = Color(0xFF2E7D32), bgColor = Color(0xFFE8F5E9))
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Đang tư vấn tự động",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    mode == "HUMAN" && status == "WAITING" -> {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF3E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_headphone),
                                contentDescription = "Hỗ trợ khách hàng",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Hỗ trợ khách hàng",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1A1A1A)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TagPill(text = "Chờ", color = Color(0xFFE65100), bgColor = Color(0xFFFFF3E0))
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE65100))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Đang chờ nhân viên tiếp nhận",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    else -> {
                        // Nhân viên đang hỗ trợ (ASSIGNED)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE3F2FD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Nhân viên",
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = chat?.assignedStaffName?.ifBlank { "Nguyễn Văn A" } ?: "Nguyễn Văn A",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1A1A1A)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TagPill(text = "Staff", color = Color(0xFF1976D2), bgColor = Color(0xFFE3F2FD))
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1976D2))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Nhân viên đang hỗ trợ",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Color(0xFF1976D2),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Nút hành động góc phải Header (Không bao giờ xuống dòng)
            when {
                mode == "AI" -> {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onHandoffClick() },
                        color = Color(0xFFF3F4F6)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_headphone),
                                contentDescription = "Gặp nhân viên",
                                tint = Color(0xFF222222),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gặp nhân viên",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF222222)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                mode == "HUMAN" && status == "WAITING" -> {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onCancelHandoffClick() },
                        color = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = "Hủy yêu cầu",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFFE53935)
                            ),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                else -> {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onReturnToAiClick() },
                        color = Color(0xFFFFF1F0)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_bot),
                                contentDescription = "Quay lại AI",
                                tint = Color(0xFFEC3824),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Quay lại AI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFEC3824)
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
}

@Composable
private fun TagPill(text: String, color: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Banner sản phẩm ghim trên đầu ChatScreen khi khách bấm FAB từ ProductDetailScreen
 */
@Composable
fun PinnedProductBanner(
    product: PinnedProductContext,
    onAskClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFEFEFEF))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E1E1E)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (product.isDisabled) {
                    Text(
                        text = "Sản phẩm ngừng kinh doanh",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    )
                } else {
                    Text(
                        text = formatVnd(product.price),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color(0xFFEC3824)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Màu ${product.selectedColor} • Size ${product.selectedSize}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = Color(0xFF757575)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hỏi về sản phẩm này?",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2)
                    ),
                    modifier = Modifier.clickable { onAskClick() }
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Đóng",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Tin nhắn từ phía khách hàng (User Bubble màu đen, chữ trắng, kèm preview sản phẩm nếu có)
 */
@Composable
fun UserMessageItem(
    message: Message,
    onProductClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.widthIn(max = 285.dp)
        ) {
            // Nếu tin nhắn User gửi kèm ngữ cảnh sản phẩm từ FAB
            if (!message.productId.isNullOrEmpty() && !message.productName.isNullOrEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clickable { onProductClick(message.productId) }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!message.productImage.isNullOrEmpty()) {
                            AsyncImage(
                                model = message.productImage,
                                contentDescription = message.productName,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF5F5F5)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(
                                text = message.productName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Màu ${message.productColor ?: "-"} • Size ${message.productSize ?: "-"}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = Color(0xFF757575)
                                )
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                color = Color(0xFF1A1A1A)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color.White,
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp)
                )
            }
        }
    }
}

/**
 * Tin nhắn từ phía SoleSense AI (Avatar robot + Bubble xám nhạt + Sản phẩm gợi ý + Quick reply chips)
 */
@Composable
fun AiMessageItem(
    message: Message,
    productsMap: Map<String, Product>,
    onProductClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    onQuickQuestionClick: (String) -> Unit,
    onWhyRecommendClick: (Product, String, List<String>) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_bot_avatar),
            contentDescription = "AI",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                color = Color(0xFFF1F1F5),
                modifier = Modifier.widthIn(max = 310.dp)
            ) {
                Column(modifier = Modifier.padding(13.dp)) {
                    when (message.sendStatus) {
                        "PROCESSING" -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TypingDotsAnimation()
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SoleSense AI đang phân tích...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = Color(0xFF616161)
                                    )
                                )
                            }
                        }

                        "FAILED" -> {
                            Text(
                                text = message.content.ifBlank { "Không thể kết nối với AI tư vấn." },
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD32F2F))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onRetryClick,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Thử lại", fontSize = 11.sp)
                            }
                        }

                        else -> {
                            Text(
                                text = parseMarkdownBold(message.content),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E1E1E),
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }
            }

            // Danh sách card sản phẩm AI đề xuất (Chỉ hiển thị các sản phẩm thực sự tồn tại trong Firestore)
            val validRecommendedIds = message.recommendedProductIds.distinct().filter { productsMap.containsKey(it) }
            if (validRecommendedIds.isNotEmpty() && message.sendStatus == "COMPLETED") {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Sản phẩm gợi ý",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E1E1E)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 12.dp)
                ) {
                    items(validRecommendedIds, key = { it }) { prodId ->
                        val product = productsMap[prodId] ?: return@items
                        val reason = message.recommendationReasons[prodId] ?: "Phù hợp nhu cầu"
                        val explanations = message.recommendationExplanations[prodId] ?: emptyList()

                        RecommendedProductCard(
                            product = product,
                            reason = reason,
                            onCardClick = { onProductClick(prodId) },
                            onWhyClick = {
                                onWhyRecommendClick(product, reason, explanations)
                            }
                        )
                    }
                }
            }

            // Quick reply chips (Câu hỏi gợi ý nhanh)
            if (message.suggestedQuestions.isNotEmpty() && message.sendStatus == "COMPLETED") {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 12.dp)
                ) {
                    items(message.suggestedQuestions) { question ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0xFFD6D6DC), RoundedCornerShape(18.dp))
                                .clickable { onQuickQuestionClick(question) },
                            color = Color.White
                        ) {
                            Text(
                                text = question,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF222222)
                                ),
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card hiển thị sản phẩm AI đề xuất
 */
@Composable
fun RecommendedProductCard(
    product: Product,
    reason: String,
    onCardClick: () -> Unit,
    onWhyClick: () -> Unit
) {
    val name = product.name
    val price = product.salePrice ?: product.basePrice
    val imageUrl = product.imageUrls.firstOrNull() ?: ""
    val isOutOfStock = product.totalStockQuantity <= 0 || product.status == "OUT_OF_STOCK"

    val displayReason = if (isOutOfStock) "Tạm hết hàng" else reason
    val badgeBg = when {
        isOutOfStock -> Color(0xFFFFEBEE)
        displayReason.contains("size", ignoreCase = true) || displayReason.contains("Rẻ nhất", ignoreCase = true) -> Color(0xFFE8F5E9)
        else -> Color(0xFFE3F2FD)
    }
    val badgeText = when {
        isOutOfStock -> Color(0xFFD32F2F)
        displayReason.contains("size", ignoreCase = true) || displayReason.contains("Rẻ nhất", ignoreCase = true) -> Color(0xFF2E7D32)
        else -> Color(0xFF1976D2)
    }

    Surface(
        modifier = Modifier
            .width(164.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE8E8EE))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8F8FA)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shoe),
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E1E1E)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formatVnd(price),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = Color(0xFFEC3824)
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tag lý do đề xuất (Clickable để mở BottomSheet giải thích "Vì sao AI đề xuất?")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .clickable { onWhyClick() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = displayReason,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeText
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Tin nhắn từ phía Nhân viên hỗ trợ (Staff Bubble)
 */
@Composable
fun StaffMessageItem(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Staff",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.widthIn(max = 285.dp)) {
            Text(
                text = message.senderName ?: "Nhân viên hỗ trợ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )

            Surface(
                shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                color = Color(0xFFEBF3FE)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color(0xFF1E1E1E),
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                )
            }
        }
    }
}

/**
 * Tin nhắn sự kiện hệ thống nằm ở giữa màn hình (System Event Pill)
 */
@Composable
fun SystemMessageItem(message: Message) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEEEEF2)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFF616161),
                    lineHeight = 15.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

/**
 * Thanh nhập tin nhắn ở đáy màn hình:
 * Dùng BasicTextField chiều cao 36.dp thay vì Material3 TextField (mặc định 56.dp)
 * giúp thanh nhập tin nhắn gọn gàng, thanh thoát và sát khít ngay trên thanh BottomNavigationBar.
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    placeholderText: String = "Nhập tin nhắn..."
) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = placeholderText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                color = Color(0xFF9E9E9E)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChanged,
                        enabled = enabled,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            color = Color(0xFF1A1A1A)
                        ),
                        cursorBrush = SolidColor(Color(0xFF1E1E1E)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (enabled && text.isNotBlank()) onSendClick()
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(enabled = enabled && text.isNotBlank()) { onSendClick() },
                    shape = CircleShape,
                    color = if (enabled && text.isNotBlank()) Color(0xFF1E1E1E) else Color(0xFFE0E0E0)
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

/**
 * Dialog: Quay lại tư vấn AI?
 */
@Composable
fun ReturnToAiDialog(
    onConfirmReturn: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bot),
                    contentDescription = null,
                    tint = Color(0xFF1E1E1E),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Quay lại tư vấn AI?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF1A1A1A)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                text = "Cuộc trò chuyện với nhân viên sẽ tạm kết thúc. Bạn vẫn có thể quay lại yêu cầu hỗ trợ sau này.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onConfirmReturn,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC3824)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Quay lại AI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tiếp tục với nhân viên",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E1E)
                        )
                    )
                }
            }
        },
        dismissButton = {}
    )
}

/**
 * Dialog mời khách đánh giá chất lượng hỗ trợ sau khi nhân viên kết thúc phiên (Chọn trên UI cho vui)
 */
@Composable
fun SupportRatingDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Đánh giá chất lượng hỗ trợ",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Phiên hỗ trợ với nhân viên đã hoàn tất. Bạn có hài lòng với trải nghiệm tư vấn vừa rồi không?",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF616161)),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        IconButton(
                            onClick = { selectedStars = star },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$star sao",
                                tint = if (star <= selectedStars) Color(0xFFFFB300) else Color(0xFFE0E0E0),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedStars) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gửi đánh giá", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Bỏ qua", color = Color.Gray)
            }
        }
    )
}

/**
 * BottomSheet: Vì sao AI đề xuất?
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhyAiRecommendedBottomSheet(
    detail: WhyRecommendedDetail,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = detail.product.imageUrls.firstOrNull() ?: "",
                    contentDescription = detail.product.name,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = detail.product.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1A1A1A)
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatVnd(detail.product.salePrice ?: detail.product.basePrice),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEC3824)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = detail.mainReason,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Vì sao AI đề xuất?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1A1A1A)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            val bullets = if (detail.explanations.isNotEmpty()) {
                detail.explanations
            } else {
                listOf(
                    "Giá nằm trong ngân sách khoảng 2 - 3 triệu đồng.",
                    "Có đặc tính đệm phù hợp nhu cầu chạy bộ & đi hàng ngày.",
                    "Có lựa chọn size cần kiểm tra theo bảng size của hãng."
                )
            }

            bullets.forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "• ", fontWeight = FontWeight.Bold, color = Color(0xFF424242))
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = Color(0xFF424242),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Thông tin tồn kho cần được xác nhận lại tại thời điểm đặt hàng.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic
                )
            )
        }
    }
}

@Composable
private fun TypingIndicatorBubble(name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFE8F0FE)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingDotsAnimation()
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$name đang soạn tin...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = Color(0xFF1976D2)
                    )
                )
            }
        }
    }
}

@Composable
private fun TypingDotsAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF757575).copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF757575).copy(alpha = 1f - alpha))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF757575).copy(alpha = alpha))
        )
    }
}

/**
 * Chuyển đổi cú pháp **in đậm** của AI sang AnnotatedString hiển thị đẹp mắt trên Compose
 */
private fun parseMarkdownBold(text: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}

private fun formatVnd(amount: Double): String {
    return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(amount.toLong()) + "đ"
}
