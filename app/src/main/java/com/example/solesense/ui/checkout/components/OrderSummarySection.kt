package com.example.solesense.ui.checkout.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OrderSummarySection(
    subtotal: Double,
    shippingFee: Double,
    discountAmount: Double,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        CheckoutSectionHeader(title = "TỔNG KẾT CHI PHÍ")

        Spacer(modifier = Modifier.height(8.dp))

        // Tạm tính
        SummaryRow(
            label = "Tạm tính (Giày):",
            value = currencyFormatter.format(subtotal)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Phí vận chuyển
        SummaryRow(
            label = "+ Phí vận chuyển:",
            value = "+ ${currencyFormatter.format(shippingFee)}"
        )

        // Giảm giá voucher (nếu có)
        if (discountAmount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            SummaryRow(
                label = "- Giảm giá voucher:",
                value = "- ${currencyFormatter.format(discountAmount)}",
                valueColor = Color(0xFF2E7D32)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(10.dp))

        // Tổng thanh toán
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tổng thanh toán:",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Color(0xFF212121)
                )
            )

            Text(
                text = currencyFormatter.format(totalAmount),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color.Black
                )
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF424242)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF424242),
                fontSize = 13.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = valueColor,
                fontSize = 13.sp
            )
        )
    }
}

