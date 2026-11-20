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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProductClick() }
            .padding(bottom = 4.dp)
    ) {
        // 1. KHU VỰC ẢNH SẢN PHẨM TRÀN KHUNG SẮC NÉT & NHÃN NHỎ GỌN
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.06f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF5F5F7))
        ) {
            // Ảnh sản phẩm tràn đều khung bo góc, không bị co lọt sâu vào trong
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            // Nhãn Badge góc trên trái: Thiết kế mảnh, nhỏ gọn, tinh tế
            if (product.badge != ProductBadge.NONE && !product.isOutOfStock) {
                val (badgeText, badgeColor) = when (product.badge) {
                    ProductBadge.BESTSELLER -> "Bán chạy" to Color(0xFF111827)
                    ProductBadge.NEW -> "Mới" to Color(0xFF0284C7)
                    ProductBadge.SALE -> "Giảm giá" to Color(0xFFE53935)
                    ProductBadge.LOW_STOCK -> "Sắp hết" to Color(0xFFD97706)
                    ProductBadge.NONE -> "" to Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 8.dp, start = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.88f))
                        .padding(horizontal = 6.dp, vertical = 2.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 9.5.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }

            // Nút Trái tim Yêu thích nhỏ gọn góc trên phải
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFavorite) Color(0xFFFFEBEE) else Color.Black.copy(alpha = 0.65f)
                    )
                    .clickable { onFavoriteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Yêu thích",
                    tint = if (isFavorite) Color(0xFFE53935) else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Lớp phủ TẠM HẾT HÀNG ở chính giữa ảnh
            if (product.isOutOfStock) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.12f))
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0xD91E1E1E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_package),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tạm hết hàng",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 10.5.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }

        // Phần nội dung chữ bên dưới tách khỏi khung bo góc & có khoảng đệm an toàn không bao giờ bị cắt chữ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            // 2. TÊN SẢN PHẨM
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                ),
                color = if (product.isOutOfStock) Color.Gray else Color(0xFF111827),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. ĐÁNH GIÁ & SỐ LƯỢNG ĐÃ BÁN
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB800),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${product.rating}",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        lineHeight = 14.sp,
                        color = Color(0xFF111827),
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Text(
                    text = "  |  ",
                    fontSize = 11.sp,
                    color = Color(0xFFD0D0D0)
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Đã bán ${product.soldCount}",
                        style = TextStyle(
                            fontSize = 10.5.sp,
                            lineHeight = 12.sp,
                            color = Color(0xFF6B7280),
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. GIÁ TIỀN (VND) & % GIẢM GIÁ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${formatCurrencyVND(product.currentPrice)}đ",
                    style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        color = if (product.isOutOfStock) Color.Gray else Color(0xFF111827)
                    ),
                    maxLines = 1,
                    softWrap = false
                )

                if (product.discountPercent > 0 && !product.isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = "-${product.discountPercent}%",
                            style = TextStyle(
                                color = Color(0xFFE53935),
                                fontSize = 9.5.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (product.originalPrice != null && product.originalPrice > product.currentPrice && !product.isOutOfStock) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatCurrencyVND(product.originalPrice)}đ",
                    style = TextStyle(
                        textDecoration = TextDecoration.LineThrough,
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

private fun formatCurrencyVND(amount: Long): String {
    return String.format("%,d", amount).replace(',', '.')
}
