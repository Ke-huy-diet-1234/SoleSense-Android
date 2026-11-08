package com.example.solesense.ui.product.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProductSummarySection(
    productName: String,
    displayPrice: Double,
    displayMinPrice: Double? = null,
    displayMaxPrice: Double? = null,
    displayOriginalPrice: Double?,
    soldCount: Int,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Bộ định dạng tiền tệ Việt Nam (VNĐ)
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    val priceString = if (displayMinPrice != null && displayMaxPrice != null) {
        "${currencyFormatter.format(displayMinPrice)} - ${currencyFormatter.format(displayMaxPrice)}"
    } else {
        currencyFormatter.format(displayPrice)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. Tên sản phẩm và Nút Yêu thích
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = productName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp, end = 12.dp)
            )

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Yêu thích",
                    tint = if (isFavorite) Color(0xFFE53935) else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Cụm Giá và Số lượng đã bán
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nhóm Giá bán
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Giá hiện tại (Màu đỏ, to, in đậm)
                Text(
                    text = priceString,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935),
                        fontSize = 22.sp,
                        lineHeight = 26.sp
                    )
                )

                // Giá gốc gạch ngang (Chỉ hiển thị nếu có giá gốc và giá gốc > giá bán)
                if (displayOriginalPrice != null && displayOriginalPrice > displayPrice) {
                    Text(
                        text = currencyFormatter.format(displayOriginalPrice),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray,
                            textDecoration = TextDecoration.LineThrough,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            // Lượt bán (vd: "Đã bán 1,2k")
            Text(
                text = "Đã bán ${formatSoldCount(soldCount)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Nhãn thông báo Voucher (Giống thiết kế tham khảo)
        Box(
            modifier = Modifier
                .background(Color(0xFFFFF0F1), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "🏷️ Giá chưa áp dụng voucher",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

// Hàm tiện ích chuyển đổi số lượng lớn (1200 -> 1,2k)
private fun formatSoldCount(count: Int): String {
    return when {
        count >= 1000000 -> String.format(Locale.US, "%.1fm", count / 1000000.0)
        count >= 1000 -> String.format(Locale.US, "%.1fk", count / 1000.0)
        else -> count.toString()
    }
}