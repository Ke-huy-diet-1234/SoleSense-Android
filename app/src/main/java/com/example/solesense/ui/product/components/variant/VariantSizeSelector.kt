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
import com.example.solesense.ui.product.SizeOptionUiModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VariantSizeSelector(
    sizes: List<SizeOptionUiModel>,
    onSizeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Size",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Hướng dẫn chọn kích cỡ >",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sizes.forEach { sizeOption ->
                val borderColor = when {
                    sizeOption.isSelected -> Color(0xFFE53935)
                    !sizeOption.isAvailable -> Color(0xFFEEEEEE)
                    else -> Color(0xFFE0E0E0)
                }

                val backgroundColor = when {
                    !sizeOption.isAvailable -> Color(0xFFF7F7F7)
                    sizeOption.isSelected -> Color(0xFFFFF0F1)
                    else -> Color.White
                }

                val textColor = when {
                    !sizeOption.isAvailable -> Color.LightGray
                    sizeOption.isSelected -> Color(0xFFE53935)
                    else -> Color.Black
                }

                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(backgroundColor)
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable(enabled = sizeOption.isAvailable) {
                            onSizeSelected(sizeOption.sizeName)
                        },
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
        }
    }
}

