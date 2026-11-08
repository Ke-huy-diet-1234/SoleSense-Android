package com.example.solesense.ui.checkout.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.ui.checkout.PaymentMethodType

@Composable
fun PaymentMethodSection(
    selectedMethod: PaymentMethodType,
    onPaymentMethodSelected: (PaymentMethodType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        CheckoutSectionHeader(
            title = "PHƯƠNG THỨC THANH TOÁN",
            subtitle = "Phương thức thanh toán"
        )

        Spacer(modifier = Modifier.height(6.dp))

        PaymentMethodType.entries.forEach { method ->
            val isSelected = method == selectedMethod

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPaymentMethodSelected(method) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onPaymentMethodSelected(method) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF1A1A1A),
                        unselectedColor = Color(0xFF9E9E9E)
                    )
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = method.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = Color(0xFF212121),
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(id = method.iconRes),
                    contentDescription = method.title,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

