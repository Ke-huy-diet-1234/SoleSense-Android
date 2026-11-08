package com.example.solesense.ui.product.components.variant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VariantQuantitySelector(
    quantity: Int,
    maxStock: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Số lượng",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
        ) {
            // Nút trừ (-)
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 32.dp)
                    .background(Color(0xFFFAFAFA))
                    .clickable(enabled = quantity > 1) { onDecrease() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    fontSize = 18.sp,
                    color = if (quantity > 1) Color.Black else Color.LightGray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Hiển thị số lượng
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(32.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = quantity.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Nút cộng (+)
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 32.dp)
                    .background(Color(0xFFFAFAFA))
                    .clickable(enabled = quantity < maxStock) { onIncrease() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 18.sp,
                    color = if (quantity < maxStock) Color.Black else Color.LightGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

