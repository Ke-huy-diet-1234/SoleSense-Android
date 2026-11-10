package com.example.solesense.ui.checkout

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solesense.ui.address.ShippingAddressScreen
import com.example.solesense.ui.checkout.components.*
import com.example.solesense.ui.theme.SoleSenseTheme

@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onOrderSuccess: () -> Unit = {},
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
                    Toast.makeText(context, "Tùy chọn thanh toán", Toast.LENGTH_SHORT).show()
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
                            onOrderSuccess()
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
                    onApplyClick = viewModel::applyVoucherCode,
                    onRemoveVoucherClick = viewModel::removeAppliedVoucher,
                    onSelectOtherVoucherClick = {
                        val nextVoucher = uiState.availableVouchers.firstOrNull {
                            it.voucherId != uiState.appliedVoucher?.voucherId
                        } ?: uiState.availableVouchers.firstOrNull()
                        if (nextVoucher != null) {
                            viewModel.selectVoucher(nextVoucher)
                        }
                    }
                )
            }

            // 4. Phương thức vận chuyển
            item {
                ShippingMethodSection(
                    selectedMethod = uiState.selectedShippingMethod,
                    onShippingMethodClick = {
                        val nextMethod = uiState.shippingMethods.firstOrNull {
                            it.id != uiState.selectedShippingMethod?.id
                        }
                        if (nextMethod != null) {
                            viewModel.selectShippingMethod(nextMethod)
                        }
                    }
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
                        onOrderSuccess()
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
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CheckoutScreenPreview() {
    SoleSenseTheme {
        CheckoutScreen()
    }
}
