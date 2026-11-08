package com.example.solesense.repository

import com.example.solesense.model.catalog.InventoryLog
import com.example.solesense.model.catalog.Variant
import com.example.solesense.model.order.Order
import com.example.solesense.model.order.OrderItem
import com.example.solesense.model.order.Payment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.util.Date
import kotlin.random.Random

data class CreatedOrderResult(
    val order: Order,
    val payment: Payment?
)

class OrderRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val QR_HOLD_DURATION_SECONDS = 300L // Giữ đơn 5 phút (300 giây)
        private const val VIETQR_BANK_ID = "MB"
        private const val VIETQR_ACCOUNT_NO = "0987654321"
        private const val VIETQR_ACCOUNT_NAME = "SOLESENSE STORE"
    }

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "mmZhfijntCf7BW2U0pf9VQbm4Xi1"
    }

    /**
     * Tạo đơn hàng bằng Firestore Transaction:
     * 1. Đọc & khóa kiểm tra tồn kho (stock >= quantity) của từng biến thể -> chống Race Condition khi 2 người tranh nhau 1 đôi giày cuối.
     * 2. Trừ ngay tồn kho (kể cả COD lẫn QR Code để khóa giữ hàng 5 phút cho người đặt).
     * 3. Lưu Order + OrderItem (Snapshot giá & tên) + Payment (với mã VietQR & expiresAt = +5 phút) + InventoryLog.
     */
    suspend fun createOrderWithTransaction(
        items: List<OrderItem>,
        subtotal: Double,
        shippingFee: Double,
        voucherId: String?,
        discount: Double,
        totalAmount: Double,
        receiverName: String,
        receiverPhone: String,
        shippingAddress: String,
        note: String?,
        paymentMethod: String
    ): Result<CreatedOrderResult> {
        val userId = getCurrentUserId()

        return try {
            val result = db.runTransaction { transaction ->
                val now = Timestamp.now()
                val orderCode = "SOLE${Random.nextInt(10000, 99999)}"

                // BƯỚC 1: Đọc toàn bộ biến thể trong đơn hàng trước (Firestore bắt buộc mọi lệnh READ phải chạy trước lệnh WRITE)
                val variantSnapshots = items.map { item ->
                    val variantRef = db.collection("products")
                        .document(item.productId)
                        .collection("variants")
                        .document(item.variantId)
                    val snap = transaction.get(variantRef)
                    Triple(item, variantRef, snap)
                }

                val voucherRef = if (!voucherId.isNullOrBlank()) {
                    db.collection("vouchers").document(voucherId)
                } else null
                val voucherSnap = voucherRef?.let { transaction.get(it) }

                // BƯỚC 2: Kiểm tra tồn kho từng biến thể (Chống tranh chấp Race Condition)
                variantSnapshots.forEach { (item, _, snap) ->
                    if (!snap.exists()) {
                        throw IllegalStateException("Biến thể ${item.productName} (Size ${item.size}) không tồn tại trên hệ thống!")
                    }
                    val variant = snap.toObject(Variant::class.java)
                        ?: throw IllegalStateException("Không đọc được dữ liệu biến thể ${item.productName}")

                    if (variant.stock < item.quantity) {
                        if (variant.stock <= 0) {
                            throw IllegalStateException("Rất tiếc! ${item.productName} (Size ${item.size} - ${item.color}) vừa hết hàng!")
                        } else {
                            throw IllegalStateException("Sản phẩm ${item.productName} (Size ${item.size}) chỉ còn ${variant.stock} đôi trong kho!")
                        }
                    }
                }

                // BƯỚC 3: Trừ tồn kho ngay lập tức & ghi InventoryLog
                variantSnapshots.forEach { (item, variantRef, snap) ->
                    val currentStock = snap.getLong("stock")?.toInt() ?: 0
                    val newStock = currentStock - item.quantity
                    val newStatus = if (newStock <= 0) "OUT_OF_STOCK" else "ACTIVE"

                    transaction.update(
                        variantRef,
                        mapOf(
                            "stock" to newStock,
                            "status" to newStatus,
                            "updatedAt" to now
                        )
                    )

                    val logRef = db.collection("inventory_logs").document()
                    val log = InventoryLog(
                        logId = logRef.id,
                        variantId = item.variantId,
                        productId = item.productId,
                        type = "SALE",
                        quantity = -item.quantity,
                        previousStock = currentStock,
                        newStock = newStock,
                        reason = if (paymentMethod == "QR_CODE") {
                            "Giữ hàng 5 phút chờ thanh toán QR đơn #$orderCode"
                        } else {
                            "Khách đặt đơn COD #$orderCode"
                        },
                        createdBy = userId,
                        createdAt = now
                    )
                    transaction.set(logRef, log)
                }

                // BƯỚC 4: Tăng số lượt dùng Voucher (nếu có)
                if (voucherRef != null && voucherSnap != null && voucherSnap.exists()) {
                    val usedCount = voucherSnap.getLong("usedCount")?.toInt() ?: 0
                    transaction.update(voucherRef, "usedCount", usedCount + 1)
                }

                // BƯỚC 5: Tạo Order và Subcollection items (Snapshot)
                val orderRef = db.collection("orders").document()
                val isQr = paymentMethod == "QR_CODE"
                val order = Order(
                    orderId = orderRef.id,
                    orderCode = orderCode,
                    userId = userId,
                    subtotal = subtotal,
                    shippingFee = shippingFee,
                    voucherId = voucherId,
                    discount = discount,
                    totalAmount = totalAmount,
                    currency = "VND",
                    receiverName = receiverName,
                    receiverPhone = receiverPhone,
                    shippingAddress = shippingAddress,
                    note = note?.ifBlank { null },
                    paymentMethod = paymentMethod,
                    paymentStatus = "PENDING",
                    orderStatus = if (isQr) "AWAITING_PAYMENT" else "PENDING",
                    createdAt = now,
                    updatedAt = now
                )
                transaction.set(orderRef, order)

                items.forEach { item ->
                    val itemRef = orderRef.collection("items").document()
                    val snapshotItem = item.copy(
                        itemId = itemRef.id,
                        subtotal = item.unitPrice * item.quantity
                    )
                    transaction.set(itemRef, snapshotItem)
                }

                // BƯỚC 6: Nếu là QR_CODE, tạo bản ghi Payment với hạn 5 phút & link VietQR
                val payment = if (isQr) {
                    val paymentRef = db.collection("payments").document()
                    val expiresAt = Timestamp(Date(now.toDate().time + QR_HOLD_DURATION_SECONDS * 1000))
                    val encodedInfo = URLEncoder.encode("THANH TOAN $orderCode", "UTF-8")
                    val encodedName = URLEncoder.encode(VIETQR_ACCOUNT_NAME, "UTF-8")
                    val qrUrl = "https://img.vietqr.io/image/$VIETQR_BANK_ID-$VIETQR_ACCOUNT_NO-compact2.png?amount=${totalAmount.toLong()}&addInfo=$encodedInfo&accountName=$encodedName"

                    val newPayment = Payment(
                        paymentId = paymentRef.id,
                        orderId = orderRef.id,
                        userId = userId,
                        provider = "VIETQR",
                        amount = totalAmount,
                        status = "PENDING",
                        transactionRef = orderCode,
                        qrCodeUrl = qrUrl,
                        expiresAt = expiresAt,
                        createdAt = now,
                        updatedAt = now
                    )
                    transaction.set(paymentRef, newPayment)
                    newPayment
                } else null

                CreatedOrderResult(order = order, payment = payment)
            }.await()

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xác nhận thanh toán QR thành công (Trong thời hạn 5 phút)
     */
    suspend fun confirmQrPayment(orderId: String, paymentId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val now = Timestamp.now()
                val paymentRef = db.collection("payments").document(paymentId)
                val orderRef = db.collection("orders").document(orderId)

                val paymentSnap = transaction.get(paymentRef)
                val expiresAt = paymentSnap.getTimestamp("expiresAt")
                val currentStatus = paymentSnap.getString("status") ?: "PENDING"

                if (currentStatus == "EXPIRED" || (expiresAt != null && now.seconds > expiresAt.seconds)) {
                    throw IllegalStateException("Phiên thanh toán QR đã hết hạn 5 phút!")
                }

                transaction.update(
                    paymentRef,
                    mapOf(
                        "status" to "SUCCESS",
                        "paidAt" to now,
                        "updatedAt" to now
                    )
                )
                transaction.update(
                    orderRef,
                    mapOf(
                        "paymentStatus" to "SUCCESS",
                        "orderStatus" to "PENDING",
                        "updatedAt" to now
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Hủy hoặc Hết hạn giữ đơn 5 phút -> Tự động hoàn trả lại tồn kho (Release Stock)
     */
    suspend fun cancelOrExpireQrOrder(
        orderId: String,
        paymentId: String,
        items: List<OrderItem>,
        reason: String = "Quá hạn giữ đơn thanh toán QR (5 phút)"
    ): Result<Unit> {
        val userId = getCurrentUserId()
        return try {
            db.runTransaction { transaction ->
                val now = Timestamp.now()
                val orderRef = db.collection("orders").document(orderId)
                val paymentRef = db.collection("payments").document(paymentId)

                val orderSnap = transaction.get(orderRef)
                val currentOrderStatus = orderSnap.getString("orderStatus") ?: ""
                if (currentOrderStatus == "CANCELLED") {
                    return@runTransaction
                }

                val variantSnaps = items.map { item ->
                    val ref = db.collection("products")
                        .document(item.productId)
                        .collection("variants")
                        .document(item.variantId)
                    Triple(item, ref, transaction.get(ref))
                }

                // Cộng trả lại tồn kho cho từng biến thể
                variantSnaps.forEach { (item, ref, snap) ->
                    if (snap.exists()) {
                        val currentStock = snap.getLong("stock")?.toInt() ?: 0
                        val restoredStock = currentStock + item.quantity
                        transaction.update(
                            ref,
                            mapOf(
                                "stock" to restoredStock,
                                "status" to if (restoredStock > 0) "ACTIVE" else "OUT_OF_STOCK",
                                "updatedAt" to now
                            )
                        )

                        val logRef = db.collection("inventory_logs").document()
                        val log = InventoryLog(
                            logId = logRef.id,
                            variantId = item.variantId,
                            productId = item.productId,
                            type = "RETURN",
                            quantity = item.quantity,
                            previousStock = currentStock,
                            newStock = restoredStock,
                            reason = reason,
                            createdBy = userId,
                            createdAt = now
                        )
                        transaction.set(logRef, log)
                    }
                }

                transaction.update(
                    paymentRef,
                    mapOf(
                        "status" to "EXPIRED",
                        "updatedAt" to now
                    )
                )

                transaction.update(
                    orderRef,
                    mapOf(
                        "paymentStatus" to "FAILED",
                        "orderStatus" to "CANCELLED",
                        "cancelReason" to reason,
                        "updatedAt" to now
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lắng nghe Realtime trạng thái Payment (Nếu Admin đổi status = SUCCESS trên Firebase hoặc máy khác, máy khách tự nhảy thành công!)
     */
    fun observePaymentStatus(paymentId: String): Flow<Payment?> = callbackFlow {
        val listener = db.collection("payments")
            .document(paymentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val payment = snapshot?.toObject(Payment::class.java)
                trySend(payment)
            }
        awaitClose { listener.remove() }
    }
}

