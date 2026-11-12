package com.example.solesense.ui.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solesense.R
import com.example.solesense.repository.FavoriteRepository
import com.example.solesense.ui.components.LoginRequiredDialog
import com.example.solesense.utils.UserSessionManager

@Composable
fun ProfileScreen(
    onNavigateToAuth: () -> Unit = {},
    onNavigateToAddresses: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onSwitchToRoleDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val favoriteIds by FavoriteRepository.favoriteProductIds.collectAsState()
    val isLoggedIn = currentUser != null
    val context = LocalContext.current

    var showLoginRequiredDialog by remember { mutableStateOf(false) }

    fun requireAuthOrExecute(action: () -> Unit) {
        if (!isLoggedIn) {
            showLoginRequiredDialog = true
        } else {
            action()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Tài khoản cá nhân",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color(0xFF111111)
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Thẻ thông tin người dùng hoặc Khách (Guest) - Đồng bộ Avatar với HomeHeader
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF2F2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoggedIn) {
                        val avatarUrl = currentUser?.avatarUrl
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_solesense_logo),
                                contentDescription = "Avatar",
                                modifier = Modifier.size(42.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Khách",
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (isLoggedIn && currentUser != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentUser!!.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF111111)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFEBEE))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentUser!!.role,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFE53935),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = currentUser!!.email,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF757575))
                        )
                    } else {
                        Text(
                            text = "Khách chưa đăng nhập (Guest)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Đăng nhập để quản lý địa chỉ, giỏ hàng & đơn hàng",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF757575))
                        )
                    }
                }
            }
        }

        if (!isLoggedIn) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onNavigateToAuth,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEC3824),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Đăng nhập / Đăng ký tài khoản",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        val userRole = currentUser?.role?.uppercase() ?: ""
        val isStaffOrAdmin = isLoggedIn && userRole in setOf("ADMIN", "SUPPORT")

        if (isStaffOrAdmin) {
            Text(
                text = "CHẾ ĐỘ XEM TRƯỚC CỬA HÀNG",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF757575)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF8E1),
                border = BorderStroke(1.dp, Color(0xFFFFE082)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tài khoản $userRole chỉ được xem giao diện",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Các tính năng mua hàng, giỏ hàng, thanh toán, địa chỉ nhận hàng và yêu thích đã được khóa để đảm bảo đúng phân quyền.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF5D4037),
                            lineHeight = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onSwitchToRoleDashboard,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E1E1E),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Quay về Bảng điều khiển $userRole",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Text(
                text = "TIỆN ÍCH TÀI KHOẢN",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF757575)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileActionItem(
                title = "Địa chỉ giao hàng",
                subtitle = "Thêm, sửa, xóa và đặt địa chỉ nhận hàng mặc định",
                onClick = {
                    requireAuthOrExecute {
                        onNavigateToAddresses()
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileActionItem(
                title = "Lịch sử đơn hàng",
                subtitle = "Theo dõi trạng thái chờ xác nhận, đang giao và đã giao",
                onClick = {
                    requireAuthOrExecute {
                        Toast.makeText(context, "Đang tải lịch sử đơn hàng của bạn", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileActionItem(
                title = if (isLoggedIn && favoriteIds.isNotEmpty()) {
                    "Danh sách sản phẩm yêu thích (${favoriteIds.size})"
                } else {
                    "Danh sách sản phẩm yêu thích"
                },
                subtitle = "Xem các mẫu giày bạn đã lưu trên hệ thống",
                onClick = {
                    requireAuthOrExecute {
                        onNavigateToFavorites()
                    }
                }
            )
        }

        if (isLoggedIn) {
            Spacer(modifier = Modifier.height(26.dp))
            OutlinedButton(
                onClick = {
                    UserSessionManager.signOut()
                    Toast.makeText(context, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE53935)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Đăng xuất",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }

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

@Composable
private fun ProfileActionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF757575),
                        fontSize = 12.sp
                    )
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF9E9E9E)
            )
        }
    }
}
