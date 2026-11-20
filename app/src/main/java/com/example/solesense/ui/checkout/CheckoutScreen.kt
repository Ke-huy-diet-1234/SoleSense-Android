package com.example.solesense.ui.checkout

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solesense.model.order.Order
import com.example.solesense.ui.address.ShippingAddressScreen
import com.example.solesense.ui.checkout.components.*
import com.example.solesense.ui.theme.SoleSenseTheme
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onOrderSuccess: () -> Unit = {},
    onTrackOrderClick: (String) -> Unit = { onOrderSuccess() },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Đồng bộ bộ đếm QR 5 phút theo thời gian thực ngay khi người dùng quay lại ứng dụng (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncQrTimerWithWallClock()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Màn hình Đặt hàng thành công (Order Success Screen)
    if (uiState.isOrderSuccess && uiState.completedOrder != null) {
        BackHandler {
            onOrderSuccess()
        }
        OrderSuccessScreen(
            order = uiState.completedOrder!!,
            itemCount = uiState.items.sumOf { it.quantity }.coerceAtLeast(1),
            onTrackOrderClick = {
                onTrackOrderClick(uiState.completedOrder!!.orderId)
            },
            onContinueShoppingClick = onOrderSuccess
        )
        return
    }

    // Nếu đang mở màn hình Địa chỉ giao hàng (Chọn / Thêm mới / Sửa / Xóa trên Firebase)
    if (uiState.isAddressSheetVisible) {
        BackHandler {
            viewModel.closeAddressSheet()
        }
        ShippingAddressScreen(
            initialSelectedAddressId = uiState.selectedAddress?.addressId,
            onBackClick = viewModel::closeAddressSheet,
            onAddressApplied = { selectedAddress ->
                viewModel.selectShippingAddress(selectedAddress)
            }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            CheckoutTopBar(
                onBackClick = onBackClick,
                onMenuClick = {
                    Toast.makeText(context, "Kiểm tra thông tin trước khi đặt hàng", Toast.LENGTH_SHORT).show()
                }
            )
        },
        bottomBar = {
            CheckoutBottomBar(
                isEnabled = uiState.selectedAddress != null && uiState.items.isNotEmpty(),
                isSubmitting = uiState.isSubmittingOrder,
                onPlaceOrderClick = {
                    viewModel.placeOrder(
                        onOrderCompleted = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFAFAFA)),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Thông tin người nhận (bấm vào sẽ mở màn hình Địa chỉ giao hàng)
            item {
                ShippingAddressSection(
                    address = uiState.selectedAddress,
                    onEditAddressClick = viewModel::openAddressSheet
                )
            }

            // 2. Danh sách sản phẩm
            item {
                CheckoutProductListSection(items = uiState.items)
            }

            // 3. Mã giảm giá / Voucher
            item {
                CheckoutVoucherSection(
                    voucherInput = uiState.voucherInput,
                    appliedVoucher = uiState.appliedVoucher,
                    voucherError = uiState.voucherError,
                    onVoucherInputChanged = viewModel::onVoucherInputChanged,
                    onApplyClick = { viewModel.applyVoucherCode() },
                    onRemoveVoucherClick = viewModel::removeAppliedVoucher,
                    onSelectOtherVoucherClick = { viewModel.openVoucherSheet() }
                )
            }

            // 4. Phương thức vận chuyển (Mở BottomSheet chọn gói giao hàng)
            item {
                ShippingMethodSection(
                    selectedMethod = uiState.selectedShippingMethod,
                    onShippingMethodClick = viewModel::openShippingMethodSheet
                )
            }

            // 5. Phương thức thanh toán
            item {
                PaymentMethodSection(
                    selectedMethod = uiState.selectedPaymentMethod,
                    onPaymentMethodSelected = viewModel::selectPaymentMethod
                )
            }

            // 6. Ghi chú đơn hàng
            item {
                OrderNoteSection(
                    note = uiState.orderNote,
                    onNoteChanged = viewModel::onOrderNoteChanged
                )
            }

            // 7. Tổng kết chi phí
            item {
                OrderSummarySection(
                    subtotal = uiState.subtotal,
                    shippingFee = uiState.shippingFee,
                    discountAmount = uiState.discountAmount,
                    totalAmount = uiState.totalAmount
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Dialog Thanh toán VietQR (Đồng bộ thời gian thực 5 phút, tự hoàn kho & hiển thị giao diện hết hạn ic_time_expire)
        QrPaymentDialog(
            payment = uiState.activePayment,
            remainingSeconds = uiState.qrRemainingSeconds,
            isQrExpired = uiState.isQrExpired,
            isProcessing = uiState.isProcessingQrAction,
            onConfirmPaidClick = {
                viewModel.confirmQrPaymentDemo(
                    onOrderCompleted = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onCancelPaymentClick = {
                viewModel.cancelQrPayment { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            onCreateNewOrderClick = {
                viewModel.dismissExpiredQrDialog()
                Toast.makeText(context, "Vui lòng nhấn Đặt hàng để tạo mã thanh toán mới", Toast.LENGTH_SHORT).show()
            }
        )

        // BottomSheet Chọn Phương thức vận chuyển
        if (uiState.isShippingMethodSheetVisible) {
            val numFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }
            ModalBottomSheet(
                onDismissRequest = viewModel::closeShippingMethodSheet,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Chọn phương thức vận chuyển",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF111827)
                        )
                    )
                    Text(
                        text = "Phí vận chuyển sẽ được cộng trực tiếp vào tổng đơn hàng",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    uiState.shippingMethods.forEach { method ->
                        val isSelected = uiState.selectedShippingMethod?.id == method.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFFF0FDF4) else Color(0xFFF9FAFB),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF16A34A) else Color(0xFFE5E7EB)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.selectShippingMethod(method) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = method.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF111827)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = method.estimatedTime,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${numFormat.format(method.fee.toLong())} ₫",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp,
                                        color = if (isSelected) Color(0xFF15803D) else Color(0xFF111827)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.selectShippingMethod(method) },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF16A34A))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // BottomSheet Chọn mã giảm giá (Chuẩn giao diện Shopee/Lazada đối chiếu theo ảnh mẫu 2)
        if (uiState.isVoucherSheetVisible) {
            val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val activeUid = authUser?.uid ?: com.example.solesense.utils.UserSessionManager.getActiveUserId()
            com.example.solesense.ui.checkout.components.VoucherSelectionBottomSheet(
                availableVouchers = uiState.availableVouchers,
                appliedVoucher = uiState.appliedVoucher,
                cartItems = uiState.items,
                userId = activeUid,
                onApplyVoucher = { voucher ->
                    viewModel.selectVoucher(voucher)
                },
                onRemoveVoucher = {
                    viewModel.removeAppliedVoucher()
                },
                onApplyCodeInput = { code ->
                    viewModel.applyVoucherCode(code)
                },
                onDismissRequest = {
                    viewModel.closeVoucherSheet()
                }
            )
        }
    }
}

@Composable
private fun OrderSuccessScreen(
    order: Order,
    itemCount: Int,
    onTrackOrderClick: () -> Unit,
    onContinueShoppingClick: () -> Unit
) {
    val numFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }
    val isPaid = order.paymentStatus.uppercase() in setOf("SUCCESS", "PAID")

    Scaffold(
        containerColor = Color(0xFFF8F9FC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16A34A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Thành công",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Đặt hàng thành công!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827),
                    fontSize = 23.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isPaid) {
                    "Thanh toán VietQR đã được xác nhận. Đơn hàng của bạn đang được chuẩn bị xuất kho."
                } else {
                    "Đơn hàng của bạn đã được ghi nhận và đang chờ cửa hàng xác nhận."
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280),
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OrderInfoRow(label = "Mã đơn hàng", value = "#${order.orderCode}", isBold = true)
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    OrderInfoRow(label = "Người nhận", value = "${order.receiverName} • ${order.receiverPhone}")
                    OrderInfoRow(label = "Địa chỉ giao", value = order.shippingAddress)
                    OrderInfoRow(label = "Số lượng", value = "$itemCount sản phẩm")
                    OrderInfoRow(
                        label = "Phương thức",
                        value = if (order.paymentMethod == "QR_CODE") "Chuyển khoản VietQR" else "Thanh toán khi nhận hàng (COD)"
                    )
                    OrderInfoRow(
                        label = "Trạng thái TT",
                        value = if (isPaid) "Đã thanh toán" else "Thanh toán khi nhận hàng"
                    )
                    if (order.discount > 0) {
                        OrderInfoRow(
                            label = "Tiết kiệm Voucher",
                            value = "-${numFormat.format(order.discount.toLong())} ₫"
                        )
                    }
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tổng thanh toán",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "${numFormat.format(order.totalAmount.toLong())} ₫",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onTrackOrderClick,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111827),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Theo dõi đơn hàng",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onContinueShoppingClick,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.2.dp, Color(0xFFD1D5DB)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF111827)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Tiếp tục mua sắm",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun OrderInfoRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = Color(0xFF111827),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.62f)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CheckoutScreenPreview() {
    SoleSenseTheme {
        CheckoutScreen()
    }
}
