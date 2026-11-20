package com.example.solesense.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solesense.model.order.OrderItem
import com.example.solesense.model.order.Voucher
import com.example.solesense.model.user.Address
import com.example.solesense.repository.AddressRepository
import com.example.solesense.repository.CartRepository
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
    private val cartRepository = CartRepository()

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private var qrCountdownJob: Job? = null
    private var paymentListenerJob: Job? = null

    init {
        initializeCheckout()
    }

    private fun initializeCheckout() {
        val sessionItems = CheckoutSessionHolder.consumeItems()

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

        val now = com.google.firebase.Timestamp.now()
        val oneMonthLater = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000))
        val yesterday = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 24L * 3600 * 1000))
        val oneMonthAgo = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 30L * 24 * 3600 * 1000))
        val twoMonthsLater = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() + 60L * 24 * 3600 * 1000))

        val fallbackVouchers = listOf(
            Voucher(
                voucherId = "vc_sole200",
                code = "SOLE200",
                name = "Khuyến mãi SoleSense 200K",
                description = "Giảm trực tiếp 200.000₫ cho đơn từ 1.000.000₫",
                discountType = "FIXED",
                discountValue = 200000.0,
                minOrderValue = 1000000.0,
                scope = "ALL",
                targetAudience = "ALL",
                usageLimit = 100,
                usedCount = 15,
                startAt = now,
                endAt = oneMonthLater,
                isEnabled = true
            ),
            Voucher(
                voucherId = "vc_sole10",
                code = "SOLE10",
                name = "Ưu đãi thành viên 10%",
                description = "Giảm 10% tối đa 300.000₫ cho đơn từ 1.000.000₫",
                discountType = "PERCENT",
                discountValue = 10.0,
                maxDiscountAmount = 300000.0,
                minOrderValue = 1000000.0,
                scope = "ALL",
                targetAudience = "ALL",
                usageLimit = 150,
                usedCount = 18,
                startAt = now,
                endAt = oneMonthLater,
                isEnabled = true
            ),
            Voucher(
                voucherId = "vc_freeship",
                code = "FREESHIP",
                name = "Miễn phí vận chuyển",
                description = "Miễn phí vận chuyển toàn quốc (giảm 30.000₫)",
                discountType = "FREE_SHIPPING",
                discountValue = 30000.0,
                maxDiscountAmount = 30000.0,
                minOrderValue = 200000.0,
                scope = "ALL",
                targetAudience = "ALL",
                usageLimit = 500,
                usedCount = 65,
                startAt = now,
                endAt = oneMonthLater,
                isEnabled = true
            ),
            Voucher(
                voucherId = "vc_expired2025",
                code = "EXPIRED2025",
                name = "Khuyến mãi đã hết hạn",
                description = "Giảm 500.000₫ cho đơn từ 1.000.000₫ (Đã hết hạn)",
                discountType = "FIXED",
                discountValue = 500000.0,
                minOrderValue = 1000000.0,
                scope = "ALL",
                targetAudience = "ALL",
                startAt = oneMonthAgo,
                endAt = yesterday,
                isEnabled = true
            ),
            Voucher(
                voucherId = "vc_hethan50k",
                code = "HETHAN50K",
                name = "Tri ân cuối tuần (Hết hạn)",
                description = "Giảm 50.000₫ (Đã hết hạn)",
                discountType = "FIXED",
                discountValue = 50000.0,
                minOrderValue = 200000.0,
                scope = "ALL",
                targetAudience = "ALL",
                startAt = oneMonthAgo,
                endAt = yesterday,
                isEnabled = true
            ),
            Voucher(
                voucherId = "vc_newyear2027",
                code = "NEWYEAR2027",
                name = "Khuyến mãi Năm Mới 2027",
                description = "Giảm 300.000₫ (Chưa bắt đầu)",
                discountType = "FIXED",
                discountValue = 300000.0,
                minOrderValue = 2000000.0,
                scope = "ALL",
                targetAudience = "ALL",
                startAt = oneMonthLater,
                endAt = twoMonthsLater,
                isEnabled = true
            ),
            Voucher(
                voucherId = "vc_soldout99",
                code = "SOLDOUT99",
                name = "Flash Sale 100K Chớp Nhoáng",
                description = "Giảm 100.000₫ (Đã hết lượt sử dụng)",
                discountType = "FIXED",
                discountValue = 100000.0,
                minOrderValue = 500000.0,
                scope = "ALL",
                targetAudience = "ALL",
                usageLimit = 50,
                usedCount = 50,
                startAt = now,
                endAt = oneMonthLater,
                isEnabled = true
            ),
            Voucher(
                voucherId = "vc_vip1trieu",
                code = "VIP1TRIEU",
                name = "Vip Diamond Giảm 1 Triệu",
                description = "Giảm 1.000.000₫ cho đơn từ 10.000.000₫",
                discountType = "FIXED",
                discountValue = 1000000.0,
                minOrderValue = 10000000.0,
                scope = "ALL",
                targetAudience = "ALL",
                startAt = now,
                endAt = oneMonthLater,
                isEnabled = true
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
                selectedAddress = null, // Mặc định chưa có địa chỉ giả cho tài khoản mới
                items = activeItems,
                availableVouchers = fallbackVouchers,
                appliedVoucher = null, // Mặc định không auto-apply mã khi chưa kiểm duyệt
                shippingMethods = shippingMethods,
                selectedShippingMethod = shippingMethods.first(),
                selectedPaymentMethod = PaymentMethodType.QR_CODE
            )
        }
        recalculateTotals()

        // Đồng bộ địa chỉ thật của người dùng, giỏ hàng (nếu session trống) và voucher thực tế từ Firestore
        viewModelScope.launch {
            if (sessionItems.isEmpty()) {
                cartRepository.getCartItems().onSuccess { dbCartItems ->
                    if (dbCartItems.isNotEmpty()) {
                        val recovered = dbCartItems.map { ci ->
                            OrderItem(
                                itemId = ci.itemId,
                                productId = ci.productId,
                                variantId = ci.variantId,
                                skuSnapshot = ci.variantId,
                                productName = ci.productName,
                                imageSnapshot = ci.imageUrl,
                                size = ci.size,
                                color = ci.color,
                                unitPrice = ci.unitPrice,
                                quantity = ci.quantity,
                                subtotal = ci.unitPrice * ci.quantity
                            )
                        }
                        _uiState.update { it.copy(items = recovered) }
                        recalculateTotals()
                    }
                }
            }
            addressRepository.getDefaultAddress().onSuccess { dbAddress ->
                if (dbAddress != null && dbAddress.receiverName.isNotBlank() && dbAddress.phone.isNotBlank()) {
                    _uiState.update { it.copy(selectedAddress = dbAddress) }
                } else {
                    // Kiểm tra thông tin mặc định từ hồ sơ người dùng nếu đã hoàn thiện hồ sơ
                    val currentUser = com.example.solesense.utils.UserSessionManager.currentUser.value
                    val userPhone = currentUser?.phone
                    val userAddress = currentUser?.defaultAddress
                    if (currentUser != null && !userPhone.isNullOrBlank() && !userAddress.isNullOrBlank()) {
                        val profileAddr = Address(
                            addressId = "profile_default",
                            receiverName = currentUser.fullName,
                            phone = userPhone,
                            province = "",
                            district = "",
                            ward = "",
                            detail = userAddress,
                            isDefault = true
                        )
                        _uiState.update { it.copy(selectedAddress = profileAddr) }
                    }
                }
            }
            voucherRepository.getActiveVouchers().onSuccess { dbVouchers ->
                if (dbVouchers.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            availableVouchers = dbVouchers
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

    fun selectShippingAddress(address: Address) {
        _uiState.update {
            it.copy(
                selectedAddress = address,
                isAddressSheetVisible = false
            )
        }
        viewModelScope.launch {
            addressRepository.saveAddress(address.copy(isDefault = true)).onSuccess { saved ->
                _uiState.update { it.copy(selectedAddress = saved) }
            }
        }
    }

    fun refreshDefaultAddress() {
        viewModelScope.launch {
            addressRepository.getDefaultAddress().onSuccess { dbAddress ->
                _uiState.update { it.copy(selectedAddress = dbAddress) }
            }
        }
    }

    private suspend fun removePurchasedItemsFromCart(purchasedItems: List<OrderItem>) {
        val idsToRemove = purchasedItems
            .flatMap { listOf(it.itemId, it.variantId) }
            .filter { it.isNotBlank() }
            .toSet()
        if (idsToRemove.isNotEmpty()) {
            cartRepository.removeMultipleCartItems(idsToRemove)
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

    fun openVoucherSheet() {
        _uiState.update { it.copy(isVoucherSheetVisible = true) }
    }

    fun closeVoucherSheet() {
        _uiState.update { it.copy(isVoucherSheetVisible = false) }
    }

    fun applyVoucherCode(customCode: String? = null) {
        val code = (customCode ?: _uiState.value.voucherInput).trim().uppercase()
        if (code.isEmpty()) return

        viewModelScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            when (val result = voucherRepository.validateVoucher(
                voucherCodeOrVoucher = code,
                cartItems = _uiState.value.items,
                userId = userId
            )) {
                is com.example.solesense.repository.VoucherValidationResult.Valid -> {
                    _uiState.update {
                        it.copy(
                            appliedVoucher = result.voucher,
                            voucherInput = "",
                            voucherError = null,
                            isVoucherSheetVisible = false
                        )
                    }
                    recalculateTotals()
                }
                is com.example.solesense.repository.VoucherValidationResult.Invalid -> {
                    _uiState.update {
                        it.copy(
                            appliedVoucher = if (it.appliedVoucher?.code.equals(code, ignoreCase = true)) null else it.appliedVoucher,
                            voucherError = result.reason
                        )
                    }
                    recalculateTotals()
                }
            }
        }
    }

    fun selectVoucher(voucher: Voucher) {
        viewModelScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            when (val result = voucherRepository.validateVoucher(
                voucherCodeOrVoucher = voucher,
                cartItems = _uiState.value.items,
                userId = userId
            )) {
                is com.example.solesense.repository.VoucherValidationResult.Valid -> {
                    _uiState.update {
                        it.copy(
                            appliedVoucher = result.voucher,
                            voucherError = null,
                            isVoucherSheetVisible = false
                        )
                    }
                    recalculateTotals()
                }
                is com.example.solesense.repository.VoucherValidationResult.Invalid -> {
                    _uiState.update {
                        it.copy(
                            appliedVoucher = if (it.appliedVoucher?.voucherId == voucher.voucherId) null else it.appliedVoucher,
                            voucherError = result.reason
                        )
                    }
                    recalculateTotals()
                }
            }
        }
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

    fun openShippingMethodSheet() {
        _uiState.update { it.copy(isShippingMethodSheetVisible = true) }
    }

    fun closeShippingMethodSheet() {
        _uiState.update { it.copy(isShippingMethodSheetVisible = false) }
    }

    fun selectShippingMethod(method: ShippingMethodUiModel) {
        _uiState.update {
            it.copy(
                selectedShippingMethod = method,
                isShippingMethodSheetVisible = false
            )
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
        val isAddressValid = address != null &&
                address.receiverName.isNotBlank() &&
                address.phone.isNotBlank() &&
                (address.detail.isNotBlank() || address.province.isNotBlank())

        if (!isAddressValid || address == null) {
            _uiState.update { it.copy(isAddressSheetVisible = true) }
            onError("Bắt buộc phải nhập đầy đủ Họ tên, Số điện thoại và Địa chỉ nhận hàng trước khi đặt hàng!")
            return
        }
        if (state.items.isEmpty() || state.isSubmittingOrder) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingOrder = true, errorMessage = null) }

            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                ?: com.example.solesense.utils.UserSessionManager.getActiveUserId()

            // Kiểm tra lại Voucher lần cuối ngay tại thời điểm bấm Đặt hàng
            val currentVoucher = _uiState.value.appliedVoucher
            if (currentVoucher != null) {
                when (val vRes = voucherRepository.validateVoucher(
                    voucherCodeOrVoucher = currentVoucher,
                    cartItems = _uiState.value.items,
                    userId = uid
                )) {
                    is com.example.solesense.repository.VoucherValidationResult.Invalid -> {
                        _uiState.update {
                            it.copy(
                                isSubmittingOrder = false,
                                appliedVoucher = null,
                                voucherError = vRes.reason
                            )
                        }
                        recalculateTotals()
                        onError("Mã giảm giá không còn hợp lệ: ${vRes.reason}")
                        return@launch
                    }
                    is com.example.solesense.repository.VoucherValidationResult.Valid -> {
                        // Hợp lệ, tiếp tục tạo đơn
                    }
                }
            }

            val freshState = _uiState.value
            val result = orderRepository.createOrderWithTransaction(
                items = freshState.items,
                subtotal = freshState.subtotal,
                shippingFee = freshState.shippingFee,
                voucherId = freshState.appliedVoucher?.voucherId,
                discount = freshState.discountAmount,
                totalAmount = freshState.totalAmount,
                receiverName = address.receiverName,
                receiverPhone = address.phone,
                shippingAddress = address.toFullSingleLineAddress(),
                note = freshState.orderNote,
                paymentMethod = freshState.selectedPaymentMethod.code
            )

            result.fold(
                onSuccess = { created ->
                    // CHỈ ghi nhận lịch sử sử dụng voucher ngay nếu là COD.
                    // Nếu là QR_CODE thì chỉ tạm giữ chỗ (reservedCount), khi thanh toán QR thành công mới ghi voucherUsages!
                    if (freshState.selectedPaymentMethod != PaymentMethodType.QR_CODE &&
                        !uid.isNullOrBlank() && freshState.appliedVoucher != null
                    ) {
                        voucherRepository.recordVoucherUsage(
                            userId = uid,
                            voucherId = freshState.appliedVoucher.voucherId,
                            code = freshState.appliedVoucher.code,
                            orderId = created.order.orderId,
                            discountAmount = freshState.discountAmount
                        )
                    }

                    // Tạo thông báo đặt hàng tự động
                    try {
                        com.example.solesense.repository.NotificationRepository().notifyOrderCreated(
                            order = created.order,
                            isQrPayment = freshState.selectedPaymentMethod == PaymentMethodType.QR_CODE
                        )
                    } catch (_: Exception) {}

                    if (freshState.selectedPaymentMethod == PaymentMethodType.QR_CODE && created.payment != null) {
                        // Tính mốc thời gian hết hạn thực tế (5 phút = 300.000ms)
                        val expireAtMillis = created.payment.expiresAt?.toDate()?.time
                            ?: (System.currentTimeMillis() + OrderRepository.QR_HOLD_DURATION_SECONDS * 1000L)
                        val initialRemaining = ((expireAtMillis - System.currentTimeMillis()) / 1000L)
                            .toInt()
                            .coerceAtLeast(0)

                        _uiState.update {
                            it.copy(
                                isSubmittingOrder = false,
                                activeOrder = created.order,
                                activePayment = created.payment,
                                qrExpireAtMillis = expireAtMillis,
                                qrRemainingSeconds = initialRemaining,
                                isQrExpired = initialRemaining <= 0
                            )
                        }
                        startQrCountdownAndRealtimeListener(
                            orderId = created.order.orderId,
                            paymentId = created.payment.paymentId,
                            onOrderCompleted = onOrderCompleted
                        )
                    } else {
                        // Đặt hàng COD thành công: chỉ xóa đúng các sản phẩm đã mua khỏi giỏ hàng & mở màn hình Đặt hàng thành công
                        removePurchasedItemsFromCart(freshState.items)
                        CheckoutSessionHolder.clearSession()
                        _uiState.update {
                            it.copy(
                                isSubmittingOrder = false,
                                isOrderSuccess = true,
                                completedOrder = created.order
                            )
                        }
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

    /**
     * Đồng bộ bộ đếm 5 phút theo thời gian thực (Wall-Clock Time).
     * Khi người dùng thoát ra màn hình nền và quay lại sau 5 phút, hàm này lập tức tính toán lại
     * số giây còn lại dựa trên System.currentTimeMillis() và giải phóng tồn kho ngay nếu đã hết giờ!
     */
    fun syncQrTimerWithWallClock() {
        val state = _uiState.value
        val payment = state.activePayment ?: return
        if (state.isQrExpired || state.qrExpireAtMillis <= 0L) return

        val remaining = ((state.qrExpireAtMillis - System.currentTimeMillis()) / 1000L)
            .toInt()
            .coerceAtLeast(0)

        if (remaining <= 0) {
            handleQrExpiredAutomatically(
                orderId = state.activeOrder?.orderId ?: payment.orderId,
                paymentId = payment.paymentId
            )
        } else {
            _uiState.update { it.copy(qrRemainingSeconds = remaining) }
        }
    }

    private fun startQrCountdownAndRealtimeListener(
        orderId: String,
        paymentId: String,
        onOrderCompleted: (String) -> Unit
    ) {
        qrCountdownJob?.cancel()
        paymentListenerJob?.cancel()

        // 1. Lắng nghe Realtime từ Firestore: nếu Admin đổi status = SUCCESS thì tự động hoàn tất!
        paymentListenerJob = viewModelScope.launch {
            orderRepository.observePaymentStatus(paymentId).collect { payment ->
                if (payment?.status == "SUCCESS" && _uiState.value.activePayment != null && !_uiState.value.isQrExpired) {
                    qrCountdownJob?.cancel()
                    removePurchasedItemsFromCart(_uiState.value.items)
                    CheckoutSessionHolder.clearSession()
                    val activeOrder = _uiState.value.activeOrder
                    val appliedVoucher = _uiState.value.appliedVoucher
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        ?: com.example.solesense.utils.UserSessionManager.getActiveUserId()
                    if (!uid.isNullOrBlank() && appliedVoucher != null && activeOrder != null) {
                        voucherRepository.recordVoucherUsage(
                            userId = uid,
                            voucherId = appliedVoucher.voucherId,
                            code = appliedVoucher.code,
                            orderId = activeOrder.orderId,
                            discountAmount = _uiState.value.discountAmount
                        )
                    }
                    if (activeOrder != null) {
                        try {
                            com.example.solesense.repository.NotificationRepository().notifyQrPaymentSuccess(activeOrder)
                        } catch (_: Exception) {}
                    }
                    _uiState.update {
                        it.copy(
                            activeOrder = null,
                            activePayment = null,
                            isQrExpired = false,
                            qrExpireAtMillis = 0L,
                            isProcessingQrAction = false,
                            isOrderSuccess = true,
                            completedOrder = activeOrder?.copy(
                                paymentStatus = "SUCCESS",
                                orderStatus = "CONFIRMED"
                            )
                        )
                    }
                }
            }
        }

        // 2. Bộ đếm ngược 5 phút đồng bộ thời gian thực (so sánh trực tiếp với qrExpireAtMillis)
        qrCountdownJob = viewModelScope.launch {
            while (_uiState.value.activePayment != null && !_uiState.value.isQrExpired) {
                val expireAt = _uiState.value.qrExpireAtMillis
                val remaining = ((expireAt - System.currentTimeMillis()) / 1000L)
                    .toInt()
                    .coerceAtLeast(0)

                _uiState.update { state ->
                    state.copy(qrRemainingSeconds = remaining)
                }

                if (remaining <= 0) {
                    handleQrExpiredAutomatically(orderId = orderId, paymentId = paymentId)
                    break
                }

                delay(500L)
            }
        }
    }

    private fun handleQrExpiredAutomatically(
        orderId: String,
        paymentId: String
    ) {
        if (_uiState.value.isQrExpired) return

        qrCountdownJob?.cancel()
        paymentListenerJob?.cancel()

        val itemsToRelease = _uiState.value.items
        val expiredOrder = _uiState.value.activeOrder

        // Đổi giao diện ngay sang trạng thái Đã hết hạn (00:00 + ic_time_expire)
        _uiState.update {
            it.copy(
                qrRemainingSeconds = 0,
                isQrExpired = true,
                isProcessingQrAction = false
            )
        }

        // Giải phóng tồn kho tạm giữ và cập nhật trạng thái EXPIRED/CANCELLED trên Firestore
        viewModelScope.launch {
            orderRepository.cancelOrExpireQrOrder(
                orderId = orderId,
                paymentId = paymentId,
                items = itemsToRelease,
                reason = "Quá hạn thanh toán QR 5 phút - Tự động giải phóng đơn hàng tạm giữ"
            )
            if (expiredOrder != null) {
                try {
                    com.example.solesense.repository.NotificationRepository().notifyOrderCancelled(
                        order = expiredOrder,
                        reason = "Đơn hàng #${expiredOrder.orderCode} đã tự động hủy do hết thời gian thanh toán QR (5 phút)."
                    )
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Khi bấm "Tạo đơn hàng mới" trên màn hình QR đã hết hạn -> đóng thông báo hết hạn để tạo đơn mới
     */
    fun dismissExpiredQrDialog() {
        qrCountdownJob?.cancel()
        paymentListenerJob?.cancel()
        _uiState.update {
            it.copy(
                activeOrder = null,
                activePayment = null,
                isQrExpired = false,
                qrExpireAtMillis = 0L,
                qrRemainingSeconds = OrderRepository.QR_HOLD_DURATION_SECONDS.toInt(),
                isProcessingQrAction = false
            )
        }
    }

    fun confirmQrPaymentDemo(
        onOrderCompleted: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val state = _uiState.value
        val order = state.activeOrder ?: return
        val payment = state.activePayment ?: return

        // Kiểm tra lại thời gian thực trước khi xác nhận
        val remaining = ((state.qrExpireAtMillis - System.currentTimeMillis()) / 1000L).toInt()
        if (state.isQrExpired || remaining <= 0) {
            handleQrExpiredAutomatically(order.orderId, payment.paymentId)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingQrAction = true) }
            val result = orderRepository.confirmQrPayment(order.orderId, payment.paymentId)
            result.fold(
                onSuccess = {
                    qrCountdownJob?.cancel()
                    paymentListenerJob?.cancel()
                    removePurchasedItemsFromCart(state.items)
                    CheckoutSessionHolder.clearSession()
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        ?: com.example.solesense.utils.UserSessionManager.getActiveUserId()
                    if (!uid.isNullOrBlank() && state.appliedVoucher != null) {
                        voucherRepository.recordVoucherUsage(
                            userId = uid,
                            voucherId = state.appliedVoucher.voucherId,
                            code = state.appliedVoucher.code,
                            orderId = order.orderId,
                            discountAmount = state.discountAmount
                        )
                    }
                    try {
                        com.example.solesense.repository.NotificationRepository().notifyQrPaymentSuccess(order)
                    } catch (_: Exception) {}
                    _uiState.update {
                        it.copy(
                            isProcessingQrAction = false,
                            activeOrder = null,
                            activePayment = null,
                            isQrExpired = false,
                            qrExpireAtMillis = 0L,
                            isOrderSuccess = true,
                            completedOrder = order.copy(
                                paymentStatus = "SUCCESS",
                                orderStatus = "CONFIRMED"
                            )
                        )
                    }
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
                    activePayment = null,
                    isQrExpired = false,
                    qrExpireAtMillis = 0L
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

            // Voucher chỉ có hiệu lực khi:
            // 1. Trạng thái thực tế là "ACTIVE" (chưa hết hạn, đã bắt đầu, chưa hết lượt, isEnabled = true)
            // 2. Giá trị đơn hàng phải đạt tối thiểu minOrderValue
            val isVoucherValid = voucher != null &&
                    voucher.calculateStatus() == "ACTIVE" &&
                    subtotal >= voucher.minOrderValue

            val effectiveVoucher = if (isVoucherValid) voucher else null
            val discount = effectiveVoucher?.calculateDiscountAmount(subtotal) ?: 0.0
            val total = max(0.0, subtotal + shippingFee - discount)

            state.copy(
                subtotal = subtotal,
                shippingFee = shippingFee,
                appliedVoucher = effectiveVoucher,
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
