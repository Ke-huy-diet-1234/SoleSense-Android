package com.example.solesense.ui.checkout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.R
import com.example.solesense.model.order.OrderItem
import com.example.solesense.model.order.Voucher
import com.example.solesense.repository.VoucherRepository
import com.example.solesense.repository.VoucherValidationResult
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherSelectionBottomSheet(
    availableVouchers: List<Voucher>,
    appliedVoucher: Voucher?,
    cartItems: List<OrderItem>,
    userId: String?,
    onApplyVoucher: (Voucher) -> Unit,
    onRemoveVoucher: () -> Unit,
    onApplyCodeInput: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchCodeInput by remember { mutableStateOf("") }
    var codeErrorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val voucherRepository = remember { VoucherRepository() }

    // Đánh giá từng voucher theo bộ kiểm tra 9 bước thực tế
    var evaluatedVouchers by remember(availableVouchers, cartItems) {
        mutableStateOf<List<Pair<Voucher, VoucherValidationResult>>>(emptyList())
    }

    LaunchedEffect(availableVouchers, cartItems) {
        val list = mutableListOf<Pair<Voucher, VoucherValidationResult>>()
        for (v in availableVouchers) {
            val result = voucherRepository.validateVoucher(
                voucherCodeOrVoucher = v,
                cartItems = cartItems,
                userId = userId
            )
            list.add(Pair(v, result))
        }
        evaluatedVouchers = list

        // Nếu voucher đang được gán mà không hợp lệ (hết hạn, thiếu tiền đơn...), tự động gỡ bỏ ngay lập tức!
        if (appliedVoucher != null) {
            val currentResult = list.find { it.first.voucherId == appliedVoucher.voucherId }?.second
            if (currentResult is VoucherValidationResult.Invalid) {
                onRemoveVoucher()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // 1. TIÊU ĐỀ & NÚT ĐÓNG X
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chọn mã giảm giá",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF111111)
                    )
                )

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color(0xFF444444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. KHUNG NHẬP MÃ TỰ DO + NÚT "ÁP DỤNG"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color(0xFFF4F5F7))
                        .border(
                            1.dp,
                            if (codeErrorText != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                            RoundedCornerShape(23.dp)
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchCodeInput,
                            onValueChange = {
                                searchCodeInput = it.uppercase()
                                codeErrorText = null // Xóa lỗi khi gõ lại mã
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF111111),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (searchCodeInput.isEmpty()) {
                                    Text(
                                        text = "Nhập mã giảm giá",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 13.5.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        val trimmedCode = searchCodeInput.trim()
                        if (trimmedCode.isNotBlank()) {
                            scope.launch {
                                when (val res = voucherRepository.validateVoucher(
                                    voucherCodeOrVoucher = trimmedCode,
                                    cartItems = cartItems,
                                    userId = userId
                                )) {
                                    is VoucherValidationResult.Valid -> {
                                        codeErrorText = null
                                        onApplyVoucher(res.voucher)
                                        onDismissRequest()
                                    }
                                    is VoucherValidationResult.Invalid -> {
                                        codeErrorText = res.reason
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(23.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A1A1A),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Text(
                        text = "Áp dụng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }

            // DÒNG BÁO LỖI KHI NHẬP MÃ (NẾU CÓ)
            if (!codeErrorText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = codeErrorText!!,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Voucher dành cho bạn",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1F2937)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. DANH SÁCH THẺ VOUCHER (ĐỐI CHIẾU CHÍNH XÁC VỚI ẢNH MẪU 2)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(evaluatedVouchers, key = { it.first.voucherId }) { (voucher, result) ->
                    val isEligible = result is VoucherValidationResult.Valid
                    val isApplied = (appliedVoucher?.voucherId == voucher.voucherId) && isEligible
                    VoucherCardItem(
                        voucher = voucher,
                        validationResult = result,
                        isApplied = isApplied,
                        onApplyClick = {
                            if (isEligible) {
                                onApplyVoucher(voucher)
                                onDismissRequest()
                            }
                        },
                        onRemoveClick = {
                            onRemoveVoucher()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Thẻ hiển thị từng Voucher chuẩn UI Shopee/Lazada (Ảnh mẫu 2):
 * - Nếu ĐỦ ĐIỀU KIỆN: Viền xanh lá, tag "Đã áp dụng" / nút "Áp dụng" / nút "Bỏ chọn"
 * - Nếu CHƯA ĐỦ ĐIỀU KIỆN: Tag cam "Chưa đủ điều kiện", báo dòng đỏ "Cần mua thêm X đồng", nút "Chưa thể chọn" mờ đi
 */
@Composable
private fun VoucherCardItem(
    voucher: Voucher,
    validationResult: VoucherValidationResult,
    isApplied: Boolean,
    onApplyClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val isEligible = validationResult is VoucherValidationResult.Valid
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    val formattedDiscount = when (voucher.discountType) {
        "PERCENT" -> "GIẢM ${voucher.discountValue.toInt()}%"
        "FREE_SHIPPING" -> "FREESHIP ${numberFormat.format(voucher.discountValue.toLong())}đ"
        else -> "GIẢM ${numberFormat.format(voucher.discountValue.toLong())}đ"
    }

    val minOrderText = if (voucher.minOrderValue > 0) {
        "Đơn hàng từ ${numberFormat.format(voucher.minOrderValue.toLong())}đ"
    } else {
        "Đơn hàng từ 0đ"
    }

    val maxDiscountText = if (voucher.discountType == "PERCENT" && voucher.maxDiscountAmount != null) {
        " - Tối đa ${numberFormat.format(voucher.maxDiscountAmount.toLong())}đ"
    } else ""

    val borderStroke = if (isApplied) {
        BorderStroke(1.5.dp, Color(0xFF22C55E))
    } else if (isEligible) {
        BorderStroke(1.dp, Color(0xFF86EFAC))
    } else {
        BorderStroke(1.dp, Color(0xFFE5E7EB))
    }

    val cardBg = if (isApplied) {
        Color(0xFFF0FDF4)
    } else {
        Color.White
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = borderStroke,
        shadowElevation = if (isApplied) 2.dp else 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // HÀNG 1: ICON VOUCHER + THÔNG TIN GIẢM + BADGE TRẠNG THÁI
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon phiếu giảm giá [%] màu xanh ngọc
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF16A34A)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = formattedDiscount,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (isEligible) Color(0xFF111827) else Color(0xFF374151)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = minOrderText + maxDiscountText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Badge góc phải: "✓ Đã áp dụng" hoặc "Chưa đủ điều kiện"
                if (isApplied) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✓ Đã áp dụng",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                } else if (!isEligible) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFEDD5))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Chưa đủ điều kiện",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFC2410C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // HÀNG 2: THÔNG TIN HẠN DÙNG & PHẠM VI ÁP DỤNG
            val hsdString = "HSD: ${dateFormat.format(voucher.endAt.toDate())}"
            val scopeString = when (voucher.scope) {
                "BRAND" -> "Áp dụng theo thương hiệu"
                "CATEGORY" -> "Áp dụng theo danh mục"
                "PRODUCT" -> "Áp dụng cho sản phẩm chỉ định"
                else -> "Toàn sàn"
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$hsdString | $scopeString",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.5.sp
                    )
                )
            }

            if (voucher.scope == "ALL") {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "🎁 Áp dụng cho tất cả đơn hàng thỏa điều kiện",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 11.5.sp
                    )
                )
            }

            // DÒNG BÁO LỖI NẾU CHƯA ĐỦ ĐIỀU KIỆN (MÀU ĐỎ NHƯ TRONG ẢNH MẪU 2)
            if (validationResult is VoucherValidationResult.Invalid) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = validationResult.reason,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // HÀNG 3: NÚT THAO TÁC ("Bỏ chọn", "Áp dụng" hoặc "Chưa thể chọn")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isApplied) {
                    OutlinedButton(
                        onClick = onRemoveClick,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.2.dp, Color(0xFF16A34A)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A)),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "Bỏ chọn",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else if (isEligible) {
                    Button(
                        onClick = onApplyClick,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "Áp dụng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    // Chưa thể chọn
                    Text(
                        text = "Chưa thể chọn",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF9CA3AF),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                    )
                }
            }
        }
    }
}
