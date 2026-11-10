package com.example.solesense.ui.admin

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
fun AdminDashboardScreen(
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
                            contentDescription = "SoleSense Admin",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Admin Dashboard",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A1A1A)
                                    )
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
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "${user.fullName} (${user.email})",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        title = "Tổng đơn hàng",
                        value = "128",
                        subtitle = "+14 đơn hôm nay",
                        accentColor = Color(0xFF1E88E5),
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "Doanh thu",
                        value = "312,5Tr ₫",
                        subtitle = "Tháng hiện tại",
                        accentColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        title = "Sản phẩm & SKU",
                        value = "42 mẫu",
                        subtitle = "3 biến thể sắp hết kho",
                        accentColor = Color(0xFFF57C00),
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "Đơn chờ duyệt QR",
                        value = "2 đơn",
                        subtitle = "Đang giữ tồn kho 5p",
                        accentColor = Color(0xFFE53935),
                        modifier = Modifier.weight(1f)
                    )
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
                    title = "Quản lý Sản phẩm, Biến thể & Tồn kho",
                    description = "Thêm, sửa, ẩn sản phẩm; quản lý SKU, màu sắc, kích cỡ và số lượng tồn kho."
                )
            }

            item {
                AdminModuleItem(
                    title = "Quản lý Đơn hàng & Xác nhận VietQR",
                    description = "Xem danh sách đơn hàng, xác nhận thanh toán, cập nhật trạng thái giao hàng và xử lý hủy đơn."
                )
            }

            item {
                AdminModuleItem(
                    title = "Quản lý Thương hiệu, Danh mục & Mã giảm giá",
                    description = "Cập nhật thương hiệu giày, danh mục sản phẩm và cấu hình chương trình khuyến mãi."
                )
            }

            item {
                AdminModuleItem(
                    title = "Phân quyền Người dùng (Customer / Support / Admin)",
                    description = "Kiểm soát quyền truy cập dữ liệu và quản lý tài khoản nhân viên hỗ trợ."
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
        Column(modifier = Modifier.padding(16.dp)) {
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
                    fontSize = 20.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E9E9E))
            )
        }
    }
}

@Composable
private fun AdminModuleItem(
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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

