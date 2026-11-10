package com.example.solesense.ui.support

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.R
import com.example.solesense.model.user.User

@Composable
fun SupportDashboardScreen(
    user: User,
    onSwitchToCustomerView: () -> Unit = {},
    onLogoutClick: () -> Unit
) {
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
                                        text = "SUPPORT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF1E88E5),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "Nhân viên hỗ trợ: ${user.fullName}",
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
                            text = "Không gian làm việc Nhân viên hỗ trợ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tư vấn chọn size giày, trả lời câu hỏi của khách hàng và tiếp nhận xử lý yêu cầu hỗ trợ / đổi trả.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE0E0E0),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    text = "YÊU CẦU TƯ VẤN & HỖ TRỢ ĐANG CHỜ",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF616161)
                    ),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item {
                SupportTicketCard(
                    customerName = "Nguyễn Đức Hiếu",
                    topic = "Tư vấn chọn size giày chạy bộ Ultraboost 5",
                    previewMessage = "Chân mình dài 25.5cm nên chọn size 41 hay 42 để chạy đường dài thoải mái ạ?",
                    badgeText = "Chuyển từ AI",
                    badgeColor = Color(0xFFE65100)
                )
            }

            item {
                SupportTicketCard(
                    customerName = "Trần Minh Anh",
                    topic = "Hỗ trợ kiểm tra trạng thái thanh toán VietQR",
                    previewMessage = "Mình vừa quét mã QR đơn hàng #SS-90241, nhờ shop kiểm tra xác nhận giúp.",
                    badgeText = "Đang chờ",
                    badgeColor = Color(0xFF1E88E5)
                )
            }

            item {
                SupportTicketCard(
                    customerName = "Lê Hoàng Nam",
                    topic = "Yêu cầu đổi kích cỡ sản phẩm (Size 40 -> 41)",
                    previewMessage = "Giày nhận đẹp nhưng hơi ôm mũi chân, mình muốn đăng ký đổi lên 1 size.",
                    badgeText = "Đổi trả",
                    badgeColor = Color(0xFF2E7D32)
                )
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
                        text = "Xem giao diện Cửa hàng (Customer Home)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportTicketCard(
    customerName: String,
    topic: String,
    previewMessage: String,
    badgeText: String,
    badgeColor: Color
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
                    text = customerName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = topic,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = previewMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF616161),
                    lineHeight = 18.sp
                )
            )
        }
    }
}

