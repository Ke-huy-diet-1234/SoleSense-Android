package com.example.solesense.repository

import com.example.solesense.model.order.OrderItem
import com.example.solesense.model.order.Voucher
import com.example.solesense.model.order.VoucherUsage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

sealed class VoucherValidationResult {
    data class Valid(
        val voucher: Voucher,
        val discountAmount: Double
    ) : VoucherValidationResult()

    data class Invalid(
        val reason: String,
        val missingAmount: Double = 0.0
    ) : VoucherValidationResult()
}

class VoucherRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Lấy toàn bộ danh sách Voucher cho màn hình Quản lý của Admin
     */
    suspend fun getAllVouchers(): Result<List<Voucher>> {
        return try {
            val snapshot = db.collection("vouchers")
                .get()
                .await()
            val vouchers = snapshot.documents.mapNotNull { doc ->
                val v = doc.toObject(Voucher::class.java) ?: return@mapNotNull null
                val explicitEnabled = doc.getBoolean("isEnabled") ?: doc.getBoolean("enabled") ?: v.isEnabled
                v.copy(voucherId = doc.id, isEnabled = explicitEnabled)
            }.sortedByDescending { it.createdAt }
            Result.success(vouchers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy danh sách Voucher cho Khách hàng chọn (Bao gồm các voucher đang bật; các voucher hết hạn/hết lượt vẫn hiển thị ở trạng thái không thể chọn)
     */
    suspend fun getActiveVouchers(): Result<List<Voucher>> {
        return try {
            val snapshot = db.collection("vouchers")
                .get()
                .await()
            val vouchers = snapshot.documents.mapNotNull { doc ->
                val v = doc.toObject(Voucher::class.java) ?: return@mapNotNull null
                // Đồng bộ cả trường isEnabled và enabled nếu có dữ liệu cũ
                val explicitEnabled = doc.getBoolean("isEnabled") ?: doc.getBoolean("enabled") ?: v.isEnabled
                v.copy(voucherId = doc.id, isEnabled = explicitEnabled)
            }
                .filter { it.isEnabled }
                .sortedByDescending { it.discountValue }
            Result.success(vouchers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kiểm tra khách hàng có phải là "Khách hàng mới" (chưa từng có đơn hàng nào hoặc số đơn = 0)
     */
    suspend fun checkIfNewCustomer(userId: String): Boolean {
        if (userId.isBlank()) return true
        return try {
            val count = db.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .size()
            count == 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Đếm số lần một tài khoản người dùng đã sử dụng một Voucher cụ thể
     */
    suspend fun getUserUsageCount(userId: String, voucherId: String): Int {
        if (userId.isBlank() || voucherId.isBlank()) return 0
        return try {
            val count = db.collection("users")
                .document(userId)
                .collection("voucherUsages")
                .whereEqualTo("voucherId", voucherId)
                .get()
                .await()
                .size()
            count
        } catch (_: Exception) {
            0
        }
    }

    /**
     * BỘ THẨM ĐỊNH 9 BƯỚC NGHIÊM NGẶT THEO ĐẶC TẢ HỆ THỐNG SOLESENSE:
     * 1. Mã có tồn tại không?
     * 2. Voucher có đang hoạt động không? (isEnabled)
     * 3. Đã đến thời gian bắt đầu chưa? (startAt)
     * 4. Đã hết hạn chưa? (endAt)
     * 5. Đã hết tổng lượt sử dụng chưa? (usageLimit vs usedCount + reservedCount)
     * 6. Người dùng đã sử dụng hết lượt cá nhân chưa? (perUserLimit)
     * 7. Đơn hàng có đạt giá trị tối thiểu không? (minOrderValue)
     * 8. Voucher có áp dụng cho sản phẩm/danh mục/thương hiệu trong giỏ hàng không? (scope)
     * 9. Voucher có áp dụng cho đối tượng khách hàng này không? (targetAudience)
     */
    suspend fun validateVoucher(
        voucherCodeOrVoucher: Any, // Có thể truyền String mã code hoặc Object Voucher
        cartItems: List<OrderItem>,
        userId: String?,
        isNewCustomer: Boolean? = null
    ): VoucherValidationResult {
        val now = Timestamp.now()

        // 1. Kiểm tra tồn tại
        val voucher = when (voucherCodeOrVoucher) {
            is Voucher -> voucherCodeOrVoucher
            is String -> {
                val code = voucherCodeOrVoucher.trim().uppercase()
                if (code.isBlank()) return VoucherValidationResult.Invalid("Vui lòng nhập mã giảm giá")
                val snap = db.collection("vouchers")
                    .whereEqualTo("code", code)
                    .limit(1)
                    .get()
                    .await()
                snap.toObjects(Voucher::class.java).firstOrNull()
                    ?: return VoucherValidationResult.Invalid("Mã giảm giá không hợp lệ")
            }
            else -> return VoucherValidationResult.Invalid("Mã giảm giá không hợp lệ")
        }

        // 2. Voucher có đang hoạt động không?
        if (!voucher.isEnabled) {
            return VoucherValidationResult.Invalid("Voucher đã bị tạm ngưng")
        }

        // 3. Đã đến thời gian bắt đầu chưa?
        if (now.seconds < voucher.startAt.seconds) {
            return VoucherValidationResult.Invalid("Voucher chưa bắt đầu")
        }

        // 4. Đã hết hạn chưa?
        if (now.seconds > voucher.endAt.seconds) {
            return VoucherValidationResult.Invalid("Voucher đã hết hạn")
        }

        // 5. Đã hết tổng lượt sử dụng chưa? (Tính cả lượt đang giữ chỗ QR)
        val totalCommitted = voucher.usedCount + voucher.reservedCount
        if (voucher.usageLimit > 0 && totalCommitted >= voucher.usageLimit) {
            return VoucherValidationResult.Invalid("Voucher đã hết lượt sử dụng")
        }

        // 6. Người dùng đã dùng hết lượt cá nhân chưa?
        if (!userId.isNullOrBlank() && voucher.perUserLimit > 0) {
            val userUsageCount = getUserUsageCount(userId, voucher.voucherId)
            if (userUsageCount >= voucher.perUserLimit) {
                return VoucherValidationResult.Invalid("Bạn đã sử dụng hết số lượt cho phép của voucher này")
            }
        }

        // 8. Kiểm tra phạm vi áp dụng (Scope) & Tính tổng tiền của các sản phẩm thỏa mãn
        val applicableItems = when (voucher.scope) {
            "BRAND" -> {
                // Đọc brandId của sản phẩm nếu cần hoặc đối chiếu
                cartItems.filter { item ->
                    voucher.applicableBrandIds.any { brandId ->
                        item.productId.contains(brandId, ignoreCase = true) || item.skuSnapshot.contains(brandId, ignoreCase = true)
                    }
                }
            }
            "CATEGORY" -> {
                cartItems.filter { item ->
                    voucher.applicableCategoryIds.any { catId ->
                        item.productId.contains(catId, ignoreCase = true)
                    }
                }
            }
            "PRODUCT" -> {
                cartItems.filter { item ->
                    voucher.applicableProductIds.contains(item.productId)
                }
            }
            else -> cartItems // "ALL"
        }

        if (applicableItems.isEmpty() && voucher.scope != "ALL") {
            return VoucherValidationResult.Invalid("Sản phẩm trong giỏ không thuộc phạm vi áp dụng")
        }

        val applicableSubtotal = applicableItems.sumOf { it.unitPrice * it.quantity }

        // 7. Đơn hàng có đạt giá trị tối thiểu không?
        if (applicableSubtotal < voucher.minOrderValue) {
            val missingAmount = voucher.minOrderValue - applicableSubtotal
            val missingText = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(missingAmount.toLong()) + "đ"
            return VoucherValidationResult.Invalid("Cần mua thêm $missingText để áp dụng voucher", missingAmount = missingAmount)
        }

        // 9. Voucher có áp dụng cho đối tượng khách này không?
        if (voucher.targetAudience == "NEW_CUSTOMER") {
            val newCustomerStatus = isNewCustomer ?: (userId?.let { checkIfNewCustomer(it) } ?: true)
            if (!newCustomerStatus) {
                return VoucherValidationResult.Invalid("Voucher chỉ áp dụng cho khách hàng mới")
            }
        }

        // TẤT CẢ 9 ĐIỀU KIỆN ĐÃ THỎA MÃN -> TÍNH MỨC GIẢM
        val discountAmount = voucher.calculateDiscountAmount(applicableSubtotal)
        return VoucherValidationResult.Valid(
            voucher = voucher,
            discountAmount = discountAmount
        )
    }

    /**
     * Tạo Voucher mới từ màn hình Admin kèm kiểm tra trùng mã
     */
    suspend fun createVoucher(voucher: Voucher): Result<String> {
        return try {
            val cleanCode = voucher.code.trim().uppercase()
            if (cleanCode.isBlank()) {
                return Result.failure(IllegalArgumentException("Mã voucher không được để trống"))
            }

            // Kiểm tra trùng mã
            val existing = db.collection("vouchers")
                .whereEqualTo("code", cleanCode)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(IllegalStateException("Mã voucher '$cleanCode' đã tồn tại trên hệ thống!"))
            }

            val docRef = db.collection("vouchers").document()
            val newVoucher = voucher.copy(
                voucherId = docRef.id,
                code = cleanCode,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            docRef.set(newVoucher).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cập nhật Voucher
     */
    suspend fun updateVoucher(voucherId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            val finalUpdates = updates.toMutableMap()
            finalUpdates["updatedAt"] = Timestamp.now()
            db.collection("vouchers").document(voucherId).update(finalUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bật / Tắt trạng thái kích hoạt của Voucher (Đồng bộ cả isEnabled & enabled để tương thích mọi bản ghi)
     */
    suspend fun toggleVoucherStatus(voucherId: String, isEnabled: Boolean): Result<Unit> {
        return updateVoucher(
            voucherId,
            mapOf(
                "isEnabled" to isEnabled,
                "enabled" to isEnabled
            )
        )
    }

    /**
     * Lưu toàn bộ thông tin chỉnh sửa của một Voucher đã có
     */
    suspend fun saveEditedVoucher(voucher: Voucher): Result<Unit> {
        return try {
            if (voucher.voucherId.isBlank()) {
                return Result.failure(IllegalArgumentException("Không tìm thấy ID voucher cần sửa"))
            }
            val cleanCode = voucher.code.trim().uppercase()
            if (cleanCode.isBlank()) {
                return Result.failure(IllegalArgumentException("Mã voucher không được để trống"))
            }
            val updatedVoucher = voucher.copy(
                code = cleanCode,
                updatedAt = Timestamp.now()
            )
            db.collection("vouchers")
                .document(voucher.voucherId)
                .set(updatedVoucher)
                .await()
            // Đảm bảo trường isEnabled và enabled đồng nhất
            db.collection("vouchers")
                .document(voucher.voucherId)
                .update(mapOf("isEnabled" to updatedVoucher.isEnabled, "enabled" to updatedVoucher.isEnabled))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ghi nhận lịch sử sử dụng Voucher của khách hàng khi đơn hàng được đặt thành công
     */
    suspend fun recordVoucherUsage(
        userId: String,
        voucherId: String,
        code: String,
        orderId: String,
        discountAmount: Double
    ) {
        if (userId.isBlank() || voucherId.isBlank()) return
        try {
            val usageId = UUID.randomUUID().toString()
            val usage = VoucherUsage(
                usageId = usageId,
                voucherId = voucherId,
                code = code,
                orderId = orderId,
                discountAmount = discountAmount,
                usedAt = Timestamp.now()
            )
            db.collection("users")
                .document(userId)
                .collection("voucherUsages")
                .document(usageId)
                .set(usage)
                .await()
        } catch (_: Exception) {}
    }
}
