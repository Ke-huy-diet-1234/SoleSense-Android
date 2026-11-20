package com.example.solesense.model.order

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Mô hình Voucher chuẩn theo đặc tả hệ thống SoleSense.
 *
 * @param discountType "FIXED" (Giảm tiền cố định) | "PERCENT" (Giảm theo %) | "FREE_SHIPPING" (Miễn phí vận chuyển)
 * @param scope "ALL" (Toàn sàn) | "BRAND" (Theo hãng) | "CATEGORY" (Theo danh mục) | "PRODUCT" (Theo sản phẩm)
 * @param targetAudience "ALL" (Tất cả khách) | "NEW_CUSTOMER" (Khách hàng mới)
 */
data class Voucher(
    @DocumentId val voucherId: String = "",
    val code: String = "",
    val name: String = "",
    val description: String? = null,

    // Nhóm hình thức giảm giá
    val discountType: String = "FIXED", // FIXED, PERCENT, FREE_SHIPPING
    val discountValue: Double = 0.0,
    val maxDiscountAmount: Double? = null,

    // Nhóm điều kiện
    val minOrderValue: Double = 0.0,
    val scope: String = "ALL", // ALL, BRAND, CATEGORY, PRODUCT
    val applicableBrandIds: List<String> = emptyList(),
    val applicableCategoryIds: List<String> = emptyList(),
    val applicableProductIds: List<String> = emptyList(),

    val targetAudience: String = "ALL", // ALL, NEW_CUSTOMER

    // Nhóm lượt sử dụng
    val usageLimit: Int = 100,
    val usedCount: Int = 0,
    val reservedCount: Int = 0, // Dành cho cơ chế giữ chỗ đơn QR 5 phút
    val perUserLimit: Int = 1,

    // Nhóm thời gian & trạng thái (Mặc định bắt đầu từ hiện tại, hạn dùng 30 ngày sau)
    val startAt: Timestamp = Timestamp.now(),
    val endAt: Timestamp = Timestamp(java.util.Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)),
    @get:PropertyName("isEnabled")
    @set:PropertyName("isEnabled")
    var isEnabled: Boolean = true,

    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    /**
     * Tính toán trạng thái thực tế dựa trên thời gian và cấu hình:
     * - "DISABLED": Admin chủ động tắt
     * - "SCHEDULED": Chưa đến thời gian bắt đầu
     * - "EXPIRED": Đã quá hạn kết thúc
     * - "EXHAUSTED": Đã hết sạch tổng lượt sử dụng
     * - "ACTIVE": Đang trong thời gian áp dụng hợp lệ
     */
    fun calculateStatus(): String {
        if (!isEnabled) return "DISABLED"
        val now = Timestamp.now().seconds
        if (now < startAt.seconds) return "SCHEDULED"
        if (now > endAt.seconds) return "EXPIRED"
        if (usageLimit > 0 && (usedCount + reservedCount) >= usageLimit) return "EXHAUSTED"
        return "ACTIVE"
    }

    fun isCurrentlyActive(): Boolean = calculateStatus() == "ACTIVE"

    /**
     * Tính toán số tiền được giảm thực tế dựa trên giá trị đơn hàng áp dụng
     */
    fun calculateDiscountAmount(applicableSubtotal: Double): Double {
        if (applicableSubtotal <= 0) return 0.0
        return when (discountType) {
            "PERCENT" -> {
                val rawDiscount = (applicableSubtotal * discountValue) / 100.0
                if (maxDiscountAmount != null && maxDiscountAmount > 0) {
                    minOf(rawDiscount, maxDiscountAmount)
                } else {
                    rawDiscount
                }
            }
            "FREE_SHIPPING" -> {
                discountValue.coerceAtLeast(0.0)
            }
            else -> { // "FIXED"
                minOf(discountValue, applicableSubtotal)
            }
        }
    }
}

/**
 * Lịch sử sử dụng voucher của từng khách hàng: users/{uid}/voucherUsages/{usageId}
 */
data class VoucherUsage(
    @DocumentId val usageId: String = "",
    val voucherId: String = "",
    val code: String = "",
    val orderId: String = "",
    val discountAmount: Double = 0.0,
    val usedAt: Timestamp = Timestamp.now()
)
