package com.example.solesense.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solesense.model.order.OrderItem
import com.example.solesense.model.order.Voucher
import com.example.solesense.model.user.Address
import com.example.solesense.repository.AddressRepository
import com.example.solesense.repository.OrderRepository
import com.example.solesense.repository.VoucherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class CheckoutViewModel : ViewModel() {

    private val addressRepository = AddressRepository()
    private val voucherRepository = VoucherRepository()
    private val orderRepository = OrderRepository()

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private var qrCountdownJob: Job? = null
    private var paymentListenerJob: Job? = null

    init {
        initializeCheckout()
    }

    private fun initializeCheckout() {
        val sessionItems = CheckoutSessionHolder.consumeItems()

        val defaultMockAddress = Address(
            addressId = "addr_default",
            receiverName = "Vương Khắc Hoàn",
            phone = "0987654321",
            province = "Hà Nội",
            district = "Cầu Giấy",
            ward = "Dịch Vọng",
            detail = "Số 12, Ngõ 34, Đường Cầu Giấy, Hà Nội",
            isDefault = true
        )

        val fallbackItems = listOf(
            OrderItem(
                itemId = "item_1",
                productId = "adidas_ultraboost_5_white",
                variantId = "ub5_white_40",
                skuSnapshot = "ID8810-WHT-40",
                productName = "Giày Ultraboost 5",
                imageSnapshot = "https://assets.adidas.com/images/w_500,f_auto,q_auto/ff67e3405f7f46eca232d3aa4e0b78e9_9366/Giay_Ultraboost_5_trang_ID8810_HM1.jpg",
                size = "40",
                color = "Trắng/Hồng",
                unitPrice = 2450000.0,
                quantity = 1,
                subtotal = 2450000.0
            )
        )

        val fallbackVouchers = listOf(
            Voucher(
                voucherId = "vc_sole200",
                code = "SOLE200",
                description = "Giảm 200.000₫",
                discountType = "AMOUNT",
                discountValue = 200000.0,
                minOrderValue = 1000000.0
            ),
            Voucher(
                voucherId = "vc_sole10",
                code = "SOLE10",
                description = "Giảm 10% tối đa 300.000₫",
                discountType = "PERCENT",
                discountValue = 10.0,
                maxDiscount = 300000.0,
                minOrderValue = 2000000.0
            )
        )

        val shippingMethods = listOf(
            ShippingMethodUiModel(
                id = "standard",
                name = "Giao hàng tiêu chuẩn",
                estimatedTime = "Dự kiến 2-3 ngày",
                fee = 30000.0
            ),
            ShippingMethodUiModel(
                id = "express",
                name = "Giao hàng hỏa tốc",
                estimatedTime = "Nhận trong ngày",
                fee = 60000.0
            )
        )

        val activeItems = sessionItems.ifEmpty { fallbackItems }

        _uiState.update {
            it.copy(
                selectedAddress = defaultMockAddress,
                items = activeItems,
                availableVouchers = fallbackVouchers,
                appliedVoucher = fallbackVouchers.first(),
                shippingMethods = shippingMethods,
                selectedShippingMethod = shippingMethods.first(),
                selectedPaymentMethod = PaymentMethodType.QR_CODE
            )
        }
        recalculateTotals()

        // Đồng bộ địa chỉ và voucher thực tế từ Firestore
        viewModelScope.launch {
            addressRepository.getDefaultAddress().onSuccess { dbAddress ->
                if (dbAddress != null) {
                    _uiState.update { it.copy(selectedAddress = dbAddress) }
                }
            }
            voucherRepository.getActiveVouchers().onSuccess { dbVouchers ->
                if (dbVouchers.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            availableVouchers = dbVouchers,
                            appliedVoucher = dbVouchers.first()
                        )
                    }
                    recalculateTotals()
                }
            }
        }
    }

    fun openAddressSheet() {
        _uiState.update { it.copy(isAddressSheetVisible = true) }
    }

    fun closeAddressSheet() {
        _uiState.update { it.copy(isAddressSheetVisible = false) }
    }

    fun saveShippingAddress(receiverName: String, phone: String, detail: String) {
        val current = _uiState.value.selectedAddress
        val toSave = Address(
            addressId = current?.addressId ?: "addr_default",
            receiverName = receiverName,
            phone = phone,
            detail = detail,
            isDefault = true
        )
        _uiState.update {
            it.copy(
                selectedAddress = toSave,
                isAddressSheetVisible = false
            )
        }
        viewModelScope.launch {
            addressRepository.saveAddress(toSave).onSuccess { saved ->
                _uiState.update { it.copy(selectedAddress = saved) }
            }
        }
    }

    fun onVoucherInputChanged(input: String) {
        _uiState.update {
            it.copy(
                voucherInput = input,
                voucherError = null
            )
        }
    }

    fun applyVoucherCode() {
        val code = _uiState.value.voucherInput.trim().uppercase()
        if (code.isEmpty()) return

        val matchedVoucher = _uiState.value.availableVouchers.find {
            it.code.equals(code, ignoreCase = true)
        }

        if (matchedVoucher == null) {
            _uiState.update { it.copy(voucherError = "Mã voucher không hợp lệ hoặc đã hết hạn") }
            return
        }

        val currentSubtotal = _uiState.value.items.sumOf { it.unitPrice * it.quantity }
        if (currentSubtotal < matchedVoucher.minOrderValue) {
            _uiState.update { it.copy(voucherError = "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã") }
            return
        }

        _uiState.update {
            it.copy(
                appliedVoucher = matchedVoucher,
                voucherInput = "",
                voucherError = null
            )
        }
        recalculateTotals()
    }

    fun selectVoucher(voucher: Voucher) {
        _uiState.update {
            it.copy(
                appliedVoucher = voucher,
                voucherError = null
            )
        }
        recalculateTotals()
    }

    fun removeAppliedVoucher() {
        _uiState.update {
            it.copy(
                appliedVoucher = null,
                voucherError = null
            )
        }
        recalculateTotals()
    }

    fun selectShippingMethod(method: ShippingMethodUiModel) {
        _uiState.update {
            it.copy(selectedShippingMethod = method)
        }
        recalculateTotals()
    }

    fun selectPaymentMethod(method: PaymentMethodType) {
        _uiState.update {
            it.copy(selectedPaymentMethod = method)
        }
    }

    fun onOrderNoteChanged(note: String) {
        _uiState.update {
            it.copy(orderNote = note)
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun placeOrder(
        onOrderCompleted: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val state = _uiState.value
        val address = state.selectedAddress
        if (address == null) {
            onError("Vui lòng nhập địa chỉ nhận hàng")
            return
        }
        if (state.items.isEmpty() || state.isSubmittingOrder) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingOrder = true, errorMessage = null) }

            val result = orderRepository.createOrderWithTransaction(
                items = state.items,
                subtotal = state.subtotal,
                shippingFee = state.shippingFee,
                voucherId = state.appliedVoucher?.voucherId,
                discount = state.discountAmount,
                totalAmount = state.totalAmount,
                receiverName = address.receiverName,
                receiverPhone = address.phone,
                shippingAddress = address.detail,
                note = state.orderNote,
                paymentMethod = state.selectedPaymentMethod.code
            )

            result.fold(
                onSuccess = { created ->
                    if (state.selectedPaymentMethod == PaymentMethodType.QR_CODE && created.payment != null) {
                        // Mở Dialog VietQR và bắt đầu đếm ngược 5 phút (300 giây)
                        _uiState.update {
                            it.copy(
                                isSubmittingOrder = false,
                                activeOrder = created.order,
                                activePayment = created.payment,
                                qrRemainingSeconds = OrderRepository.QR_HOLD_DURATION_SECONDS.toInt()
                            )
                        }
                        startQrCountdownAndRealtimeListener(
                            orderId = created.order.orderId,
                            paymentId = created.payment.paymentId,
                            onOrderCompleted = onOrderCompleted,
                            onError = onError
                        )
                    } else {
                        // Đặt hàng COD thành công ngay
                        _uiState.update {
                            it.copy(
                                isSubmittingOrder = false,
                                isOrderSuccess = true
                            )
                        }
                        onOrderCompleted("Đặt hàng thành công (#${created.order.orderCode})!")
                    }
                },
                onFailure = { e ->
                    val msg = e.message ?: "Không thể tạo đơn hàng"
                    _uiState.update {
                        it.copy(
                            isSubmittingOrder = false,
                            errorMessage = msg
                        )
                    }
                    onError(msg)
                }
            )
        }
    }

    private fun startQrCountdownAndRealtimeListener(
        orderId: String,
        paymentId: String,
        onOrderCompleted: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        qrCountdownJob?.cancel()
        paymentListenerJob?.cancel()

        // 1. Lắng nghe Realtime từ Firestore: nếu Admin đổi status = SUCCESS thì tự động hoàn tất!
        paymentListenerJob = viewModelScope.launch {
            orderRepository.observePaymentStatus(paymentId).collect { payment ->
                if (payment?.status == "SUCCESS" && _uiState.value.activePayment != null) {
                    qrCountdownJob?.cancel()
                    val orderCode = _uiState.value.activeOrder?.orderCode ?: ""
                    _uiState.update {
                        it.copy(
                            activeOrder = null,
                            activePayment = null,
                            isProcessingQrAction = false,
                            isOrderSuccess = true
                        )
                    }
                    onOrderCompleted("Thanh toán VietQR thành công (#$orderCode)!")
                }
            }
        }

        // 2. Bộ đếm ngược 5 phút (300s -> 0s)
        qrCountdownJob = viewModelScope.launch {
            while (_uiState.value.qrRemainingSeconds > 0 && _uiState.value.activePayment != null) {
                delay(1000L)
                _uiState.update { state ->
                    state.copy(qrRemainingSeconds = (state.qrRemainingSeconds - 1).coerceAtLeast(0))
                }
            }

            // Nếu hết 5 phút mà vẫn chưa thanh toán -> Tự động hủy đơn & hoàn kho!
            if (_uiState.value.activePayment != null && _uiState.value.qrRemainingSeconds <= 0) {
                cancelQrPayment(
                    reason = "Quá hạn thanh toán QR 5 phút - Tự động hoàn kho",
                    onCancelled = { msg -> onError(msg) }
                )
            }
        }
    }

    fun confirmQrPaymentDemo(
        onOrderCompleted: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val order = _uiState.value.activeOrder ?: return
        val payment = _uiState.value.activePayment ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingQrAction = true) }
            val result = orderRepository.confirmQrPayment(order.orderId, payment.paymentId)
            result.fold(
                onSuccess = {
                    qrCountdownJob?.cancel()
                    paymentListenerJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isProcessingQrAction = false,
                            activeOrder = null,
                            activePayment = null,
                            isOrderSuccess = true
                        )
                    }
                    onOrderCompleted("Thanh toán VietQR thành công (#${order.orderCode})!")
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isProcessingQrAction = false) }
                    onError(e.message ?: "Lỗi xác nhận thanh toán")
                }
            )
        }
    }

    fun cancelQrPayment(
        reason: String = "Khách hàng hủy giao dịch QR - Hoàn trả kho",
        onCancelled: (String) -> Unit
    ) {
        val order = _uiState.value.activeOrder ?: return
        val payment = _uiState.value.activePayment ?: return
        val items = _uiState.value.items

        qrCountdownJob?.cancel()
        paymentListenerJob?.cancel()

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingQrAction = true) }
            orderRepository.cancelOrExpireQrOrder(
                orderId = order.orderId,
                paymentId = payment.paymentId,
                items = items,
                reason = reason
            )
            _uiState.update {
                it.copy(
                    isProcessingQrAction = false,
                    activeOrder = null,
                    activePayment = null
                )
            }
            onCancelled("Đã hủy giao dịch QR & hoàn trả sản phẩm về kho!")
        }
    }

    private fun recalculateTotals() {
        _uiState.update { state ->
            val subtotal = state.items.sumOf { it.unitPrice * it.quantity }
            val shippingFee = state.selectedShippingMethod?.fee ?: 0.0
            val voucher = state.appliedVoucher

            val discount = if (voucher != null && subtotal >= voucher.minOrderValue) {
                when (voucher.discountType.uppercase()) {
                    "PERCENT" -> {
                        val rawDiscount = subtotal * (voucher.discountValue / 100.0)
                        if (voucher.maxDiscount != null && voucher.maxDiscount > 0) {
                            min(rawDiscount, voucher.maxDiscount)
                        } else {
                            rawDiscount
                        }
                    }
                    else -> voucher.discountValue
                }
            } else {
                0.0
            }

            val total = max(0.0, subtotal + shippingFee - discount)

            state.copy(
                subtotal = subtotal,
                shippingFee = shippingFee,
                discountAmount = discount,
                totalAmount = total
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        qrCountdownJob?.cancel()
        paymentListenerJob?.cancel()
    }
}
