package com.example.solesense.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solesense.R
import com.example.solesense.repository.FavoriteRepository

@Composable
fun HomeHeader(
    isLoggedIn: Boolean,
    userName: String? = null,
    avatarUrl: String? = null,
    onLoginClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteIds by FavoriteRepository.favoriteProductIds.collectAsState()
    val hasFavorites = isLoggedIn && favoriteIds.isNotEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. CỤM AVATAR + XIN CHÀO (GÓC TRÁI) - Đồng bộ 100% với màn hình Tôi (ProfileScreen)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                if (isLoggedIn) onProfileClick() else onLoginClick()
            }
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF2F2F2)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoggedIn) {
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
                            contentDescription = "SoleSense Avatar",
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Guest Avatar",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                if (isLoggedIn) {
                    Text(
                        text = "Xin chào 👋",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = userName ?: "Người dùng",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                } else {
                    Text(
                        text = "Xin chào, Khách",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Đăng nhập/Đăng ký ›",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                }
            }
        }

        // 2. CỤM ICON THÔNG BÁO & DANH MỤC YÊU THÍCH (GÓC PHẢI)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onFavoriteClick) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = if (hasFavorites) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorites",
                        tint = if (hasFavorites) Color(0xFFE53935) else Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    if (hasFavorites) {
                        Box(
                            modifier = Modifier
                                .offset(x = 4.dp, y = (-3).dp)
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = favoriteIds.size.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
