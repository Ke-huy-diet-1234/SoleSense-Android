package com.example.solesense.ui.checkout.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.R
import com.example.solesense.ui.checkout.ShippingMethodUiModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ShippingMethodSection(
    selectedMethod: ShippingMethodUiModel?,
    onShippingMethodClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        CheckoutSectionHeader(
            title = "PHƯƠNG THỨC VẬN CHUYỂN",
            subtitle = "Phương thức vận chuyển"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onShippingMethodClick() },
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delivery),
                    contentDescription = "Vận chuyển",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 13.sp
                                )
                            ) {
                                append(selectedMethod?.name ?: "Giao hàng tiêu chuẩn")
                            }
                            if (!selectedMethod?.estimatedTime.isNullOrBlank()) {
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF757575),
                                        fontSize = 12.sp
                                    )
                                ) {
                                    append(" (${selectedMethod?.estimatedTime})")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Phí: ${currencyFormatter.format(selectedMethod?.fee ?: 0.0)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF424242),
                            fontSize = 12.sp
                        )
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Chọn phương thức vận chuyển",
                    tint = Color(0xFF616161),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

