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
        return auth.currentUser?.uid
            ?: com.example.solesense.utils.UserSessionManager.getActiveUserId()
            ?: "mmZhfijntCf7BW2U0pf9VQbm4Xi1"
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

                // BƯỚC 1: Đọc toàn bộ biến thể VÀ sản phẩm cha trong đơn hàng trước (Firestore bắt buộc mọi lệnh READ phải chạy trước lệnh WRITE)
                val variantSnapshots = items.map { item ->
                    val variantRef = db.collection("products")
                        .document(item.productId)
                        .collection("variants")
                        .document(item.variantId)
                    val snap = transaction.get(variantRef)
                    Triple(item, variantRef, snap)
                }

                val distinctProductIds = items.map { it.productId }.filter { it.isNotBlank() }.distinct()
                val productSnapshots = distinctProductIds.associateWith { pid ->
                    val pRef = db.collection("products").document(pid)
                    Pair(pRef, transaction.get(pRef))
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

                // BƯỚC 3: Trừ tồn kho biến thể, cộng soldCount biến thể, đồng bộ totalStockQuantity & totalSoldCount & status trên Product cha, và ghi InventoryLog
                variantSnapshots.forEach { (item, variantRef, snap) ->
                    val currentStock = snap.getLong("stock")?.toInt() ?: 0
                    val currentVariantSold = snap.getLong("soldCount")?.toInt() ?: 0
                    val newStock = currentStock - item.quantity
                    val newStatus = if (newStock <= 0) "OUT_OF_STOCK" else "ACTIVE"

                    transaction.update(
                        variantRef,
                        mapOf(
                            "stock" to newStock,
                            "soldCount" to (currentVariantSold + item.quantity),
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

                // Đồng bộ totalStockQuantity, totalSoldCount và status trên document products/{productId} cha
                val qtyByProduct = items.groupBy { it.productId }.mapValues { entry -> entry.value.sumOf { it.quantity } }
                qtyByProduct.forEach { (pid, totalPurchasedQty) ->
                    val pair = productSnapshots[pid]
                    if (pair != null && pair.second.exists()) {
                        val (pRef, pSnap) = pair
                        val currentTotalStock = pSnap.getLong("totalStockQuantity")?.toInt() ?: 0
                        val currentTotalSold = pSnap.getLong("totalSoldCount")?.toInt() ?: 0
                        val currentParentStatus = pSnap.getString("status") ?: "ACTIVE"
                        val newTotalStock = (currentTotalStock - totalPurchasedQty).coerceAtLeast(0)
                        val newTotalSold = currentTotalSold + totalPurchasedQty
                        val newParentStatus = when {
                            currentParentStatus == "DISABLED" -> "DISABLED"
                            newTotalStock <= 0 -> "OUT_OF_STOCK"
                            else -> "ACTIVE"
                        }
                        transaction.update(
                            pRef,
                            mapOf(
                                "totalStockQuantity" to newTotalStock,
                                "totalSoldCount" to newTotalSold,
                                "status" to newParentStatus,
                                "updatedAt" to now
                            )
                        )
                    }
                }

                // BƯỚC 4: Kiểm tra nguyên tử điều kiện Voucher trong Transaction & Tăng lượt dùng (COD) hoặc Tạm giữ chỗ (QR_CODE)
                val isQr = paymentMethod == "QR_CODE"
                if (voucherRef != null && voucherSnap != null && voucherSnap.exists()) {
                    val isEnabled = voucherSnap.getBoolean("isEnabled") ?: true
                    val startAt = voucherSnap.getTimestamp("startAt")
                    val endAt = voucherSnap.getTimestamp("endAt")
                    val usageLimit = voucherSnap.getLong("usageLimit")?.toInt() ?: 0
                    val usedCount = voucherSnap.getLong("usedCount")?.toInt() ?: 0
                    val reservedCount = voucherSnap.getLong("reservedCount")?.toInt() ?: 0

                    if (!isEnabled) {
                        throw IllegalStateException("Mã giảm giá vừa bị tạm ngưng hiệu lực!")
                    }
                    if (startAt != null && now.seconds < startAt.seconds) {
                        throw IllegalStateException("Mã giảm giá chưa đến thời gian bắt đầu!")
                    }
                    if (endAt != null && now.seconds > endAt.seconds) {
                        throw IllegalStateException("Mã giảm giá vừa hết hạn sử dụng!")
                    }
                    if (usageLimit > 0 && (usedCount + reservedCount) >= usageLimit) {
                        throw IllegalStateException("Rất tiếc! Mã giảm giá này vừa hết lượt sử dụng!")
                    }

                    if (isQr) {
                        transaction.update(voucherRef, "reservedCount", reservedCount + 1)
                    } else {
                        transaction.update(voucherRef, "usedCount", usedCount + 1)
                    }
                }

                // BƯỚC 5: Tạo Order và Subcollection items (Snapshot)
                val orderRef = db.collection("orders").document()
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
     * Xác nhận thanh toán QR thành công (Trong thời hạn 5 phút) — Idempotent (chống gọi lặp 2 lần)
     */
    suspend fun confirmQrPayment(orderId: String, paymentId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val now = Timestamp.now()
                val paymentRef = db.collection("payments").document(paymentId)
                val orderRef = db.collection("orders").document(orderId)

                // READ PHASE (Tất cả lệnh get phải thực hiện trước update)
                val paymentSnap = transaction.get(paymentRef)
                val orderSnap = transaction.get(orderRef)
                val voucherId = orderSnap.getString("voucherId")
                val voucherRef = if (!voucherId.isNullOrBlank()) db.collection("vouchers").document(voucherId) else null
                val vSnap = voucherRef?.let { transaction.get(it) }

                val expiresAt = paymentSnap.getTimestamp("expiresAt")
                val currentStatus = paymentSnap.getString("status") ?: "PENDING"
                val currentPaymentStatus = orderSnap.getString("paymentStatus") ?: "PENDING"

                // Chống gọi lặp (Idempotency)
                if (currentStatus == "SUCCESS" || currentPaymentStatus == "SUCCESS" || currentPaymentStatus == "PAID") {
                    return@runTransaction
                }

                if (currentStatus == "EXPIRED" || (expiresAt != null && now.seconds > expiresAt.seconds)) {
                    throw IllegalStateException("Phiên thanh toán QR đã hết hạn 5 phút!")
                }

                // WRITE PHASE
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
                        "orderStatus" to "CONFIRMED",
                        "updatedAt" to now
                    )
                )

                // Chuyển voucher từ giữ chỗ (reservedCount) sang đã dùng thành công (usedCount)
                if (voucherRef != null && vSnap != null && vSnap.exists()) {
                    val reservedCount = (vSnap.getLong("reservedCount")?.toInt() ?: 1) - 1
                    val usedCount = (vSnap.getLong("usedCount")?.toInt() ?: 0) + 1
                    transaction.update(
                        voucherRef,
                        mapOf(
                            "reservedCount" to reservedCount.coerceAtLeast(0),
                            "usedCount" to usedCount
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin xác nhận thanh toán VietQR & duyệt đơn:
     * Đồng bộ cả collection `payments` (status = SUCCESS) lẫn `orders` (paymentStatus = SUCCESS, orderStatus = CONFIRMED),
     * chuyển voucher từ reservedCount -> usedCount và ghi lịch sử voucherUsages của khách.
     */
    suspend fun adminConfirmQrPaymentAndOrder(orderId: String): Result<Unit> {
        return try {
            val paymentQuery = db.collection("payments")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()
            val paymentDoc = paymentQuery.documents.firstOrNull()

            var orderUserId = ""
            var usedVoucherId = ""
            var usedVoucherCode = ""
            var orderDiscount = 0.0

            db.runTransaction { transaction ->
                val now = Timestamp.now()
                val orderRef = db.collection("orders").document(orderId)
                val orderSnap = transaction.get(orderRef)
                if (!orderSnap.exists()) {
                    throw IllegalStateException("Không tìm thấy đơn hàng!")
                }
                orderUserId = orderSnap.getString("userId") ?: ""
                orderDiscount = orderSnap.getDouble("discount") ?: 0.0
                val currentPaymentStatus = orderSnap.getString("paymentStatus") ?: "PENDING"
                val voucherId = orderSnap.getString("voucherId")
                usedVoucherId = voucherId ?: ""

                val voucherRef = if (!voucherId.isNullOrBlank()) db.collection("vouchers").document(voucherId) else null
                val vSnap = voucherRef?.let { transaction.get(it) }
                if (vSnap != null && vSnap.exists()) {
                    usedVoucherCode = vSnap.getString("code") ?: ""
                }

                val paymentRef = paymentDoc?.reference
                val pSnap = paymentRef?.let { transaction.get(it) }

                if (paymentRef != null && pSnap != null && pSnap.exists()) {
                    transaction.update(
                        paymentRef,
                        mapOf(
                            "status" to "SUCCESS",
                            "paidAt" to now,
                            "updatedAt" to now
                        )
                    )
                }

                transaction.update(
                    orderRef,
                    mapOf(
                        "paymentStatus" to "SUCCESS",
                        "orderStatus" to "CONFIRMED",
                        "updatedAt" to now
                    )
                )

                // Nếu trước đó đơn chưa xác nhận thanh toán và có dùng voucher QR, chuyển từ reserved -> used
                if (currentPaymentStatus != "SUCCESS" && currentPaymentStatus != "PAID" &&
                    voucherRef != null && vSnap != null && vSnap.exists()
                ) {
                    val reservedCount = (vSnap.getLong("reservedCount")?.toInt() ?: 1) - 1
                    val usedCount = (vSnap.getLong("usedCount")?.toInt() ?: 0) + 1
                    transaction.update(
                        voucherRef,
                        mapOf(
                            "reservedCount" to reservedCount.coerceAtLeast(0),
                            "usedCount" to usedCount
                        )
                    )
                }
            }.await()

            if (orderUserId.isNotBlank() && usedVoucherId.isNotBlank()) {
                try {
                    VoucherRepository().recordVoucherUsage(
                        userId = orderUserId,
                        voucherId = usedVoucherId,
                        code = usedVoucherCode.ifBlank { usedVoucherId },
                        orderId = orderId,
                        discountAmount = orderDiscount
                    )
                } catch (_: Exception) {}
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Hủy hoặc Hết hạn giữ đơn 5 phút -> Tự động hoàn trả lại tồn kho (Release Stock) & đồng bộ Product cha
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

                // READ PHASE
                val orderSnap = transaction.get(orderRef)
                val currentOrderStatus = orderSnap.getString("orderStatus") ?: ""
                if (currentOrderStatus == "CANCELLED") {
                    return@runTransaction
                }

                val voucherId = orderSnap.getString("voucherId")
                val voucherRef = if (!voucherId.isNullOrBlank()) db.collection("vouchers").document(voucherId) else null
                val vSnap = voucherRef?.let { transaction.get(it) }

                val variantSnaps = items.map { item ->
                    val ref = db.collection("products")
                        .document(item.productId)
                        .collection("variants")
                        .document(item.variantId)
                    Triple(item, ref, transaction.get(ref))
                }

                val distinctProductIds = items.map { it.productId }.filter { it.isNotBlank() }.distinct()
                val productSnapshots = distinctProductIds.associateWith { pid ->
                    val pRef = db.collection("products").document(pid)
                    Pair(pRef, transaction.get(pRef))
                }

                // WRITE PHASE: Cộng trả lại tồn kho và trừ lại soldCount cho từng biến thể
                variantSnaps.forEach { (item, ref, snap) ->
                    if (snap.exists()) {
                        val currentStock = snap.getLong("stock")?.toInt() ?: 0
                        val currentSold = snap.getLong("soldCount")?.toInt() ?: 0
                        val restoredStock = currentStock + item.quantity
                        transaction.update(
                            ref,
                            mapOf(
                                "stock" to restoredStock,
                                "soldCount" to (currentSold - item.quantity).coerceAtLeast(0),
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

                // Hoàn lại totalStockQuantity, trừ lại totalSoldCount và mở lại status = ACTIVE trên Product cha
                val qtyByProduct = items.groupBy { it.productId }.mapValues { entry -> entry.value.sumOf { it.quantity } }
                qtyByProduct.forEach { (pid, totalReturnedQty) ->
                    val pair = productSnapshots[pid]
                    if (pair != null && pair.second.exists()) {
                        val (pRef, pSnap) = pair
                        val currentTotalStock = pSnap.getLong("totalStockQuantity")?.toInt() ?: 0
                        val currentTotalSold = pSnap.getLong("totalSoldCount")?.toInt() ?: 0
                        val currentParentStatus = pSnap.getString("status") ?: "ACTIVE"
                        val restoredTotalStock = currentTotalStock + totalReturnedQty
                        val newParentStatus = when {
                            currentParentStatus == "DISABLED" -> "DISABLED"
                            restoredTotalStock > 0 -> "ACTIVE"
                            else -> "OUT_OF_STOCK"
                        }
                        transaction.update(
                            pRef,
                            mapOf(
                                "totalStockQuantity" to restoredTotalStock,
                                "totalSoldCount" to (currentTotalSold - totalReturnedQty).coerceAtLeast(0),
                                "status" to newParentStatus,
                                "updatedAt" to now
                            )
                        )
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

                // Giải phóng lượt giữ chỗ voucher (nếu đơn có dùng voucher)
                if (voucherRef != null && vSnap != null && vSnap.exists()) {
                    val reservedCount = (vSnap.getLong("reservedCount")?.toInt() ?: 1) - 1
                    transaction.update(voucherRef, "reservedCount", reservedCount.coerceAtLeast(0))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy danh sách toàn bộ đơn hàng kèm danh sách mặt hàng (OrderItem) của một người dùng
     */
    suspend fun getUserOrdersWithItems(userId: String = getCurrentUserId()): Result<List<Pair<Order, List<OrderItem>>>> {
        if (userId.isBlank()) return Result.success(emptyList())
        return try {
            val ordersSnap = db.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val orders = ordersSnap.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(orderId = doc.id)
            }.sortedByDescending { it.createdAt.seconds }

            val resultList = mutableListOf<Pair<Order, List<OrderItem>>>()
            for (order in orders) {
                val itemsSnap = db.collection("orders")
                    .document(order.orderId)
                    .collection("items")
                    .get()
                    .await()
                val items = itemsSnap.toObjects(OrderItem::class.java)
                resultList.add(Pair(order, items))
            }
            Result.success(resultList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy chi tiết 1 đơn hàng và danh sách items theo orderId
     */
    suspend fun getOrderDetailWithItems(orderId: String): Result<Pair<Order, List<OrderItem>>> {
        return try {
            val orderSnap = db.collection("orders").document(orderId).get().await()
            val order = orderSnap.toObject(Order::class.java)?.copy(orderId = orderSnap.id)
                ?: return Result.failure(IllegalStateException("Không tìm thấy đơn hàng"))
            val itemsSnap = db.collection("orders").document(orderId).collection("items").get().await()
            val items = itemsSnap.toObjects(OrderItem::class.java)
            Result.success(Pair(order, items))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Khách hàng hoặc Admin hủy đơn hàng -> Hoàn kho biến thể + Product cha + Hoàn lượt Voucher + Ghi InventoryLog
     */
    suspend fun cancelUserPendingOrder(
        orderId: String,
        items: List<OrderItem>,
        reason: String = "Khách hàng chủ động hủy đơn",
        isAdminOverride: Boolean = false
    ): Result<Unit> {
        val userId = getCurrentUserId()
        return try {
            db.runTransaction { transaction ->
                val now = Timestamp.now()
                val orderRef = db.collection("orders").document(orderId)
                val orderSnap = transaction.get(orderRef)
                val currentStatus = (orderSnap.getString("orderStatus") ?: "").uppercase()
                if (currentStatus == "CANCELLED") {
                    return@runTransaction
                }
                val allowedStatuses = if (isAdminOverride) {
                    setOf("PENDING", "AWAITING_PAYMENT", "CONFIRMED", "PACKING", "SHIPPING", "IN_DELIVERY")
                } else {
                    setOf("PENDING", "AWAITING_PAYMENT", "CONFIRMED")
                }
                if (currentStatus !in allowedStatuses) {
                    throw IllegalStateException("Đơn hàng đã chuyển sang trạng thái giao hàng, không thể hủy trực tiếp!")
                }

                val paymentMethod = orderSnap.getString("paymentMethod") ?: "COD"
                val paymentStatus = orderSnap.getString("paymentStatus") ?: "PENDING"
                val voucherId = orderSnap.getString("voucherId")
                val voucherRef = if (!voucherId.isNullOrBlank()) db.collection("vouchers").document(voucherId) else null
                val vSnap = voucherRef?.let { transaction.get(it) }

                val variantSnaps = items.map { item ->
                    val ref = db.collection("products")
                        .document(item.productId)
                        .collection("variants")
                        .document(item.variantId)
                    Triple(item, ref, transaction.get(ref))
                }

                val distinctProductIds = items.map { it.productId }.filter { it.isNotBlank() }.distinct()
                val productSnapshots = distinctProductIds.associateWith { pid ->
                    val pRef = db.collection("products").document(pid)
                    Pair(pRef, transaction.get(pRef))
                }

                variantSnaps.forEach { (item, ref, snap) ->
                    if (snap.exists()) {
                        val currentStock = snap.getLong("stock")?.toInt() ?: 0
                        val currentSold = snap.getLong("soldCount")?.toInt() ?: 0
                        val restoredStock = currentStock + item.quantity
                        transaction.update(
                            ref,
                            mapOf(
                                "stock" to restoredStock,
                                "soldCount" to (currentSold - item.quantity).coerceAtLeast(0),
                                "status" to if (restoredStock > 0) "ACTIVE" else "OUT_OF_STOCK",
                                "updatedAt" to now
                            )
                        )

                        val logRef = db.collection("inventory_logs").document()
                        val log = InventoryLog(
                            logId = logRef.id,
                            variantId = item.variantId,
                            productId = item.productId,
                            type = "ORDER_CANCEL_RETURN",
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

                val qtyByProduct = items.groupBy { it.productId }.mapValues { entry -> entry.value.sumOf { it.quantity } }
                qtyByProduct.forEach { (pid, totalReturnedQty) ->
                    val pair = productSnapshots[pid]
                    if (pair != null && pair.second.exists()) {
                        val (pRef, pSnap) = pair
                        val currentTotalStock = pSnap.getLong("totalStockQuantity")?.toInt() ?: 0
                        val currentTotalSold = pSnap.getLong("totalSoldCount")?.toInt() ?: 0
                        val currentParentStatus = pSnap.getString("status") ?: "ACTIVE"
                        val restoredTotalStock = currentTotalStock + totalReturnedQty
                        val newParentStatus = when {
                            currentParentStatus == "DISABLED" -> "DISABLED"
                            restoredTotalStock > 0 -> "ACTIVE"
                            else -> "OUT_OF_STOCK"
                        }
                        transaction.update(
                            pRef,
                            mapOf(
                                "totalStockQuantity" to restoredTotalStock,
                                "totalSoldCount" to (currentTotalSold - totalReturnedQty).coerceAtLeast(0),
                                "status" to newParentStatus,
                                "updatedAt" to now
                            )
                        )
                    }
                }

                // Hoàn lại lượt giữ hoặc lượt dùng của voucher khi đơn bị hủy
                if (voucherRef != null && vSnap != null && vSnap.exists()) {
                    if (paymentMethod == "QR_CODE" && paymentStatus != "SUCCESS" && paymentStatus != "PAID") {
                        val reserved = (vSnap.getLong("reservedCount")?.toInt() ?: 1) - 1
                        transaction.update(voucherRef, "reservedCount", reserved.coerceAtLeast(0))
                    } else {
                        val used = (vSnap.getLong("usedCount")?.toInt() ?: 1) - 1
                        transaction.update(voucherRef, "usedCount", used.coerceAtLeast(0))
                    }
                }

                val newPaymentStatus = if (paymentStatus == "SUCCESS" || paymentStatus == "PAID") {
                    "REFUND_PENDING"
                } else {
                    "CANCELLED"
                }

                transaction.update(
                    orderRef,
                    mapOf(
                        "orderStatus" to "CANCELLED",
                        "paymentStatus" to newPaymentStatus,
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
     * Cập nhật trạng thái đơn hàng (ví dụ: CONFIRMED -> PACKING -> SHIPPING -> DELIVERED / COMPLETED)
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "orderStatus" to newStatus,
                "updatedAt" to Timestamp.now()
            )
            if (newStatus in setOf("DELIVERED", "COMPLETED")) {
                updates["paymentStatus"] = "SUCCESS"
            }
            db.collection("orders").document(orderId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy toàn bộ đơn hàng trên hệ thống cho Admin quản lý
     */
    suspend fun getAllOrdersForAdmin(): Result<List<Pair<Order, List<OrderItem>>>> {
        return try {
            val ordersSnap = db.collection("orders").get().await()
            val orders = ordersSnap.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(orderId = doc.id)
            }.sortedByDescending { it.createdAt.seconds }

            val resultList = mutableListOf<Pair<Order, List<OrderItem>>>()
            for (order in orders) {
                val itemsSnap = db.collection("orders")
                    .document(order.orderId)
                    .collection("items")
                    .get()
                    .await()
                val items = itemsSnap.toObjects(OrderItem::class.java)
                resultList.add(Pair(order, items))
            }
            Result.success(resultList)
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

