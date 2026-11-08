package com.example.solesense.ui.product.components.variant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

@Composable
fun VariantProductPreview(
    imageUrl: String?,
    displayPrice: Double,
    displayMinPrice: Double? = null,
    displayMaxPrice: Double? = null,
    displayOriginalPrice: Double? = null,
    stockCount: Int?,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val priceString = if (displayMinPrice != null && displayMaxPrice != null) {
        "${currencyFormatter.format(displayMinPrice)} - ${currencyFormatter.format(displayMaxPrice)}"
    } else {
        currencyFormatter.format(displayPrice)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail sản phẩm
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF7F7F7))
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Ảnh biến thể",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Thông tin giá và kho
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Cụm giá
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = priceString,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935),
                        fontSize = 20.sp
                    )
                )

                if (displayOriginalPrice != null && displayOriginalPrice > displayPrice) {
                    Text(
                        text = currencyFormatter.format(displayOriginalPrice),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Số lượng tồn kho (kèm cảnh báo khi sắp hết hàng)
            val stockText = when {
                stockCount == null -> "Kho: Còn hàng"
                stockCount <= 0 -> "Kho: Hết hàng"
                stockCount <= 5 -> "🔥 Kho: Chỉ còn $stockCount đôi cuối!"
                else -> "Kho: $stockCount"
            }
            val stockColor = if (stockCount != null && stockCount in 1..5) {
                Color(0xFFE53935)
            } else {
                Color.Gray
            }

            Text(
                text = stockText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = stockColor,
                    fontWeight = if (stockCount != null && stockCount in 1..5) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            )
        }
    }
}

