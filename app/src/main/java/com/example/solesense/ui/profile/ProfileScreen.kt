package com.example.solesense.ui.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
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
import com.example.solesense.utils.AssetConfig
import com.example.solesense.utils.UserSessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToAuth: () -> Unit = {},
    onNavigateToSetupProfile: () -> Unit = {},
    onNavigateToAddresses: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onSwitchToRoleDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val favoriteIds by FavoriteRepository.favoriteProductIds.collectAsState()
    val isLoggedIn = currentUser != null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showLoginRequiredDialog by remember { mutableStateOf(false) }
    var isAvatarPickerOpen by remember { mutableStateOf(false) }

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
                        .size(64.dp)
                        .clickable {
                            if (isLoggedIn) isAvatarPickerOpen = true else onNavigateToAuth()
                        },
                    contentAlignment = Alignment.Center
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
                                    placeholder = painterResource(id = R.drawable.ic_solesense_logo),
                                    error = painterResource(id = R.drawable.ic_solesense_logo),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_solesense_logo),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
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

                    if (isLoggedIn) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF111111)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Sửa hồ sơ",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
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
                            val roleLabelVi = when (currentUser!!.role.uppercase()) {
                                "ADMIN" -> "Quản trị viên"
                                "SUPPORT" -> "Hỗ trợ viên"
                                else -> "Khách hàng"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFEBEE))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = roleLabelVi,
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
                            text = "Khách chưa đăng nhập",
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
                title = "Cập nhật thông tin cá nhân",
                subtitle = "Chỉnh sửa ảnh đại diện, họ tên, biệt danh, SĐT và địa chỉ mặc định",
                onClick = {
                    requireAuthOrExecute {
                        onNavigateToSetupProfile()
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileActionItem(
                title = "Đơn hàng của tôi",
                subtitle = "Theo dõi đơn đang giao, đánh giá sản phẩm và mua lại",
                onClick = {
                    requireAuthOrExecute {
                        onNavigateToOrders()
                    }
                }
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
                title = "Thông báo của tôi",
                subtitle = "Xem thông báo đơn hàng, thanh toán, khuyến mãi và hệ thống",
                onClick = {
                    onNavigateToNotifications()
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

    if (isAvatarPickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { isAvatarPickerOpen = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Chọn ảnh đại diện có sẵn",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF111827)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chọn 1 trong ${AssetConfig.PRESET_AVATARS.size} mẫu ảnh đại diện SoleSense",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    itemsIndexed(AssetConfig.PRESET_AVATARS) { index, url ->
                        val isChosen = currentUser?.avatarUrl == url
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .border(
                                    width = if (isChosen) 3.dp else 1.dp,
                                    color = if (isChosen) Color(0xFF16A34A) else Color(0xFFE5E7EB),
                                    shape = CircleShape
                                )
                                .clickable {
                                    isAvatarPickerOpen = false
                                    scope.launch {
                                        UserSessionManager.updateAvatar(url).onSuccess {
                                            Toast.makeText(context, "Đã cập nhật ảnh đại diện!", Toast.LENGTH_SHORT).show()
                                        }.onFailure { e ->
                                            Toast.makeText(context, "Lỗi cập nhật ảnh đại diện: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Avatar ${index + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            if (isChosen) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16A34A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
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
