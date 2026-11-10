package com.example.solesense.ui.checkout

import com.example.solesense.R
import com.example.solesense.model.order.Order
import com.example.solesense.model.order.OrderItem
import com.example.solesense.model.order.Payment
import com.example.solesense.model.order.Voucher
import com.example.solesense.model.user.Address

object CheckoutSessionHolder {
    private var pendingItems: List<OrderItem> = emptyList()

    fun setDirectBuyItem(item: OrderItem) {
        pendingItems = listOf(item)
    }

    fun setCartItems(items: List<OrderItem>) {
        pendingItems = items
    }

    fun consumeItems(): List<OrderItem> {
        val items = pendingItems
        pendingItems = emptyList()
        return items
    }
}

data class CheckoutUiState(
    val isLoading: Boolean = false,
    val isSubmittingOrder: Boolean = false,
    val errorMessage: String? = null,
    val isOrderSuccess: Boolean = false,

    // 1. Thông tin người nhận
    val selectedAddress: Address? = null,
    val isAddressSheetVisible: Boolean = false,

    // 2. Danh sách sản phẩm trong đơn hàng
    val items: List<OrderItem> = emptyList(),

    // 3. Mã giảm giá / Voucher
    val voucherInput: String = "",
    val appliedVoucher: Voucher? = null,
    val availableVouchers: List<Voucher> = emptyList(),
    val voucherError: String? = null,

    // 4. Phương thức vận chuyển
    val shippingMethods: List<ShippingMethodUiModel> = emptyList(),
    val selectedShippingMethod: ShippingMethodUiModel? = null,

    // 5. Phương thức thanh toán
    val selectedPaymentMethod: PaymentMethodType = PaymentMethodType.QR_CODE,

    // 6. Ghi chú đơn hàng
    val orderNote: String = "",

    // 7. Tổng kết chi phí
    val subtotal: Double = 0.0,
    val shippingFee: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,

    // 8. Phiên thanh toán VietQR (Giữ đơn 5 phút đồng bộ thời gian thực)
    val activeOrder: Order? = null,
    val activePayment: Payment? = null,
    val qrExpireAtMillis: Long = 0L,
    val qrRemainingSeconds: Int = 300,
    val isQrExpired: Boolean = false,
    val isProcessingQrAction: Boolean = false
)

data class ShippingMethodUiModel(
    val id: String,
    val name: String,
    val estimatedTime: String,
    val fee: Double
)

enum class PaymentMethodType(
    val code: String,
    val title: String,
    val iconRes: Int
) {
    QR_CODE(
        code = "QR_CODE",
        title = "Thanh toán bằng QR Code",
        iconRes = R.drawable.ic_qr_code
    ),
    COD(
        code = "COD",
        title = "Thanh toán khi nhận hàng (COD)",
        iconRes = R.drawable.ic_cash
    )
}
