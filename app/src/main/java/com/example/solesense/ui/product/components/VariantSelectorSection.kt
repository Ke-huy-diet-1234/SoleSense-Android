package com.example.solesense.ui.product.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solesense.ui.product.ColorOptionUiModel
import com.example.solesense.ui.product.SizeOptionUiModel

@Composable
fun VariantSelectorSection(
    colors: List<ColorOptionUiModel>,
    sizes: List<SizeOptionUiModel>,
    selectedColorName: String?,
    selectedSizeName: String?,
    onColorSelected: (String) -> Unit,
    onSizeSelected: (String) -> Unit,
    onSizeGuideClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 16.dp)
    ) {
        // 1. Phân vùng chọn Màu sắc
        Text(
            text = "Màu sắc: ${selectedColorName ?: "__"}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(colors, key = { it.colorName }) { colorOption ->
                ColorItem(
                    colorOption = colorOption,
                    onClick = { onColorSelected(colorOption.colorName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Phân vùng chọn Kích cỡ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kích cỡ (EU): ${selectedSizeName ?: "__"}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Hướng dẫn chọn kích cỡ >",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF616161),
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSizeGuideClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sizes, key = { it.sizeName }) { sizeOption ->
                SizeItem(
                    sizeOption = sizeOption,
                    onClick = {
                        // Chỉ cho phép click nếu còn hàng
                        if (sizeOption.isAvailable) onSizeSelected(sizeOption.sizeName)
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorItem(
    colorOption: ColorOptionUiModel,
    onClick: () -> Unit
) {
    val borderColor = if (colorOption.isSelected) Color(0xFFE53935) else Color.LightGray
    val borderWidth = if (colorOption.isSelected) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F5F5))
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = colorOption.thumbnailUrl,
            contentDescription = colorOption.colorName,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp), // Thu nhỏ vừa với khung để hiển thị hết toàn bộ ảnh chính
            contentScale = ContentScale.Fit
        )

        // Icon check đỏ góc phải trên khi được chọn
        if (colorOption.isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = Color(0xFFE53935),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun SizeItem(
    sizeOption: SizeOptionUiModel,
    onClick: () -> Unit
) {
    // Trạng thái thiết kế: Chọn (Đen/Trắng), Chưa chọn (Trắng/Đen/Viền xám), Hết hàng (Xám nhạt/Chữ mờ)
    val backgroundColor = when {
        !sizeOption.isAvailable -> Color(0xFFF5F5F5)
        sizeOption.isSelected -> Color.Black
        else -> Color.White
    }

    val textColor = when {
        !sizeOption.isAvailable -> Color.LightGray
        sizeOption.isSelected -> Color.White
        else -> Color.Black
    }

    val borderStroke = if (!sizeOption.isSelected && sizeOption.isAvailable) {
        BorderStroke(1.dp, Color.LightGray)
    } else null

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(if (borderStroke != null) Modifier.border(borderStroke, CircleShape) else Modifier)
            .clickable(enabled = sizeOption.isAvailable) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = sizeOption.sizeName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (sizeOption.isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            ),
            color = textColor
        )
    }
}