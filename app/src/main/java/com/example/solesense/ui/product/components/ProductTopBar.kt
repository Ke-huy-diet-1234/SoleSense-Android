package com.example.solesense.ui.product.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTopBar(
    cartItemCount: Int,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCartClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding() // Tránh đè lên thanh trạng thái (pin, sóng)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nút Back
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = "Quay lại",
                modifier = Modifier.size(24.dp),
                tint = Color.Black
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Nút Tìm kiếm
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Tìm kiếm",
                modifier = Modifier.size(26.dp),
                tint = Color.Black
            )
        }

        // Nút Giỏ hàng có Badge số lượng
        IconButton(onClick = onCartClick) {
            BadgedBox(
                badge = {
                    if (cartItemCount > 0) {
                        Badge(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        ) {
                            val displayCount = if (cartItemCount > 99) "99+" else cartItemCount.toString()
                            Text(displayCount, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = "Giỏ hàng",
                    modifier = Modifier.size(26.dp),
                    tint = Color.Black
                )
            }
        }

        // Nút Menu (3 chấm)
        IconButton(onClick = onMenuClick) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_more),
                contentDescription = "Thêm",
                modifier = Modifier.size(26.dp),
                tint = Color.Black
            )
        }
    }
}