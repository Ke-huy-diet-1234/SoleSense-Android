package com.example.solesense.ui.checkout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.solesense.model.order.Payment
import java.text.NumberFormat
import java.util.Locale

@Composable
fun QrPaymentDialog(
    payment: Payment?,
    remainingSeconds: Int,
    isProcessing: Boolean,
    onConfirmPaidClick: () -> Unit,
    onCancelPaymentClick: () -> Unit
) {
    if (payment == null) return

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Dialog(
        onDismissRequest = { /* Không tự đóng khi bấm ra ngoài để tránh mất phiên giữ hàng */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Thanh toán VietQR",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Đồng hồ đếm ngược giữ đơn 5 phút
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (remainingSeconds <= 60) Color(0xFFFFEBEE) else Color(0xFFFFF8E1))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏳ Giữ đơn hàng trong: ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF424242),
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (remainingSeconds <= 60) Color(0xFFE53935) else Color(0xFFF57C00),
                            fontSize = 14.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Khung mã VietQR động
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFAFAFA))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = payment.qrCodeUrl,
                        contentDescription = "Mã VietQR",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = currencyFormatter.format(payment.amount),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE53935),
                        fontSize = 22.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Nội dung CK: THANH TOAN ${payment.transactionRef ?: ""}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121),
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Hệ thống đã tạm giữ tồn kho cho bạn. Nếu quá 5 phút chưa thanh toán, sản phẩm sẽ tự động hoàn trả lại kho.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF757575),
                        fontSize = 11.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Nút xác nhận đã chuyển khoản (Demo)
                Button(
                    onClick = onConfirmPaidClick,
                    enabled = !isProcessing && remainingSeconds > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A1A1A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "Tôi đã chuyển khoản (Xác nhận Demo)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nút hủy đơn & hoàn trả kho ngay lập tức
                TextButton(
                    onClick = onCancelPaymentClick,
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Hủy giao dịch & hoàn trả kho",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

