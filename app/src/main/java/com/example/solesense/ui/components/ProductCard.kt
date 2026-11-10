package com.example.solesense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solesense.R
import com.example.solesense.model.catalog.ProductBadge
import com.example.solesense.model.catalog.ProductModel
import com.example.solesense.repository.FavoriteRepository

@Composable
fun ProductCard(
    product: ProductModel,
    onFavoriteClick: () -> Unit,
    onProductClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val favoriteIds by FavoriteRepository.favoriteProductIds.collectAsState()
    val isFavorite = favoriteIds.contains(product.id)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProductClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // 1. KHU VỰC ẢNH SẢN PHẨM & NHÃN OVERLAY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF7F7F8))
            ) {
                // Ảnh sản phẩm
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )

                // Nhãn Badge góc trái (BESTSELLER / NEW / SALE)
                if (product.badge != ProductBadge.NONE && !product.isOutOfStock) {
                    val (badgeText, badgeColor, icon) = when (product.badge) {
                        ProductBadge.BESTSELLER -> Triple("BESTSELLER", Color(0xFFFF8A00), R.drawable.ic_crown)
                        ProductBadge.NEW -> Triple("NEW", Color(0xFF00B14F), null)
                        ProductBadge.SALE -> Triple("SALE", Color(0xFFE74C3C), null)
                        ProductBadge.NONE -> Triple("", Color.Transparent, null)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 8.dp, start = 8.dp)
                            .background(badgeColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 1.dp)
                    ) {
                        if (icon != null) {
                            Icon(
                                painterResource(icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Nút Trái tim Yêu thích (Đổi màu đỏ khi đã thích, ấn lại để bỏ yêu thích)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isFavorite) Color(0xFFFFEBEE) else Color(0xFF1E1E1E))
                        .clickable { onFavoriteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFE53935) else Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Lớp phủ TẠM HẾT HÀNG ở chính giữa ảnh
                if (product.isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.05f))
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color(0xCC1E1E1E), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_package),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TẠM HẾT HÀNG",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. TÊN SẢN PHẨM
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = if (product.isOutOfStock) Color.Gray else Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3. ĐÁNH GIÁ & SỐ LƯỢNG ĐÃ BÁN
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${product.rating}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = Color.Black
                )
                Text(
                    text = "  |  ",
                    fontSize = 12.sp,
                    color = Color(0xFFD0D0D0)
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF2F2F2), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Đã bán ${product.soldCount}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. GIÁ TIỀN (VND)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (product.originalPrice != null && product.badge == ProductBadge.SALE && !product.isOutOfStock) {
                    Text(
                        text = "${formatCurrencyVND(product.originalPrice)} đ",
                        modifier = Modifier.padding(horizontal = 2.5.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = Color.Gray,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }

                Text(
                    text = "${formatCurrencyVND(product.currentPrice)} đ",
                    modifier = Modifier.padding(horizontal = 2.5.dp),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp
                    ),
                    color = if (product.isOutOfStock) Color.Gray else Color.Black,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

private fun formatCurrencyVND(amount: Long): String {
    return String.format("%,d", amount).replace(',', '.')
}
