package com.example.solesense.ui.product.components.variant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

@Composable
fun VariantColorSelector(
    colors: List<ColorOptionUiModel>,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Màu sắc",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(colors, key = { it.colorName }) { colorOption ->
                val borderColor = if (colorOption.isSelected) Color(0xFFE53935) else Color(0xFFE0E0E0)
                val borderWidth = if (colorOption.isSelected) 1.5.dp else 1.dp

                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF9F9F9))
                        .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
                        .clickable { onColorSelected(colorOption.colorName) }
                        .padding(6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = colorOption.thumbnailUrl,
                                contentDescription = colorOption.colorName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = colorOption.colorName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (colorOption.isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (colorOption.isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color(0xFFE53935),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

