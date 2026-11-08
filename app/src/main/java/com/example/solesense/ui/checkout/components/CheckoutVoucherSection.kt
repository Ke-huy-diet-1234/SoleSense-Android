package com.example.solesense.ui.checkout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.model.order.Voucher

@Composable
fun CheckoutVoucherSection(
    voucherInput: String,
    appliedVoucher: Voucher?,
    voucherError: String?,
    onVoucherInputChanged: (String) -> Unit,
    onApplyClick: () -> Unit,
    onRemoveVoucherClick: () -> Unit,
    onSelectOtherVoucherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        CheckoutSectionHeader(
            title = "MÃ GIẢM GIÁ / VOUCHER",
            subtitle = "Khuyến mãi"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hàng nhập mã voucher + nút Áp dụng
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(Color(0xFFEEEEEE))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = voucherInput,
                    onValueChange = onVoucherInputChanged,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (voucherInput.isEmpty()) {
                            Text(
                                text = "Nhập mã voucher",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 13.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Button(
                onClick = onApplyClick,
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text(
                    text = "Áp dụng",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )
            }
        }

        // Thông báo lỗi nếu có
        if (!voucherError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = voucherError,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFE53935),
                    fontSize = 12.sp
                )
            )
        }

        // Chip hiển thị mã đã áp dụng (ví dụ: SOLE200 - Giảm 200.000₫)
        if (appliedVoucher != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFDFF5E3))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeText = if (!appliedVoucher.description.isNullOrBlank()) {
                    "${appliedVoucher.code} - ${appliedVoucher.description}"
                } else {
                    appliedVoucher.code
                }

                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20),
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Bỏ mã giảm giá",
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onRemoveVoucherClick() }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nút "Chọn voucher khác v"
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onSelectOtherVoucherClick() }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chọn voucher khác",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF3949AB),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Chọn voucher khác",
                tint = Color(0xFF3949AB),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

