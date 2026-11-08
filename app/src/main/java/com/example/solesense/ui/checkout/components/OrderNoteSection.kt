package com.example.solesense.ui.checkout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OrderNoteSection(
    note: String,
    onNoteChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        CheckoutSectionHeader(title = "GHI CHÚ ĐƠN HÀNG")

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEEEEEE))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = note,
                onValueChange = onNoteChanged,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black,
                    fontSize = 13.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (note.isEmpty()) {
                        Text(
                            text = "Ghi chú cho cửa hàng hoặc shipper...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF8E8E8E),
                                fontSize = 13.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

