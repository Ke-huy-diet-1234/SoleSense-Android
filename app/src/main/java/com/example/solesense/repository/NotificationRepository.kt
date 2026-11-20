package com.example.solesense.repository

import com.example.solesense.model.order.Order
import com.example.solesense.model.support.Notification
import com.example.solesense.model.support.NotificationCampaign
import com.example.solesense.utils.UserSessionManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private val _unreadCount = MutableStateFlow(0)
        val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

        fun updateUnreadCount(count: Int) {
            _unreadCount.value = count.coerceAtLeast(0)
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid ?: UserSessionManager.getActiveUserId()
    }

    /**
     * Lắng nghe Realtime danh sách thông báo của người dùng (kết hợp thông báo cá nhân + chiến dịch toàn sàn)
     */
    fun observeUserNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val n = doc.toObject(Notification::class.java) ?: return@mapNotNull null
                    val explicitRead = doc.getBoolean("isRead") ?: doc.getBoolean("read") ?: n.isRead
                    n.copy(notificationId = doc.id, isRead = explicitRead)
                }.orEmpty().sortedByDescending { it.createdAt.seconds }

                _unreadCount.value = list.count { !it.isRead }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Lấy danh sách thông báo cho người dùng (kèm khởi tạo thông báo chào mừng & khuyến mãi nếu tài khoản mới chưa có thông báo)
     */
    suspend fun getUserNotifications(userId: String): Result<List<Notification>> {
        if (userId.isBlank()) return Result.success(emptyList())
        return try {
            val snap = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            var list = snap.documents.mapNotNull { doc ->
                val n = doc.toObject(Notification::class.java) ?: return@mapNotNull null
                val explicitRead = doc.getBoolean("isRead") ?: doc.getBoolean("read") ?: n.isRead
                n.copy(notificationId = doc.id, isRead = explicitRead)
            }.toMutableList()

            // Đồng bộ thêm các chiến dịch thông báo chung (notification_campaigns) mà Admin đã phát hành
            val campaignsSnap = try {
                db.collection("notification_campaigns")
                    .whereEqualTo("status", "SENT")
                    .get()
                    .await()
            } catch (_: Exception) { null }

            campaignsSnap?.documents?.forEach { cDoc ->
                val camp = cDoc.toObject(NotificationCampaign::class.java)?.copy(campaignId = cDoc.id) ?: return@forEach
                val notifId = "camp_${userId}_${camp.campaignId}"
                if (list.none { it.notificationId == notifId }) {
                    val campaignNotif = Notification(
                        notificationId = notifId,
                        userId = userId,
                        title = camp.title,
                        content = camp.content,
                        type = camp.type,
                        category = "CAMPAIGN",
                        referenceId = camp.campaignId,
                        referenceType = camp.type,
                        imageUrl = camp.imageUrl,
                        isRead = false,
                        createdAt = camp.sentAt ?: camp.createdAt
                    )
                    try {
                        db.collection("notifications").document(notifId).set(campaignNotif).await()
                    } catch (_: Exception) {}
                    list.add(campaignNotif)
                }
            }

            if (list.isEmpty()) {
                val seedNotifs = seedInitialUserNotifications(userId)
                list.addAll(seedNotifs)
            }

            val sorted = list.sortedByDescending { it.createdAt.seconds }
            _unreadCount.value = sorted.count { !it.isRead }
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun seedInitialUserNotifications(userId: String): List<Notification> {
        val now = Timestamp.now()
        val welcomeId = "notif_${userId}_welcome"
        val promoId = "notif_${userId}_sole200"
        val list = listOf(
            Notification(
                notificationId = promoId,
                userId = userId,
                title = "Voucher mới dành riêng cho bạn",
                content = "Nhập mã SOLE200 để được giảm ngay 200.000đ cho đơn hàng từ 1.000.000đ!",
                type = "PROMOTION",
                category = "VOUCHER_NEW",
                referenceId = "SOLE200",
                referenceType = "VOUCHER",
                isRead = false,
                createdAt = now
            ),
            Notification(
                notificationId = welcomeId,
                userId = userId,
                title = "Chào mừng bạn đến với SoleSense",
                content = "Khám phá bộ sưu tập giày thể thao chính hãng tích hợp AI tư vấn chọn size chuẩn xác.",
                type = "SYSTEM",
                category = "SYSTEM_WELCOME",
                referenceId = null,
                referenceType = "SYSTEM",
                isRead = false,
                createdAt = Timestamp(now.seconds - 120, 0)
            )
        )
        try {
            val batch = db.batch()
            list.forEach { n ->
                batch.set(db.collection("notifications").document(n.notificationId), n)
            }
            batch.commit().await()
        } catch (_: Exception) {}
        return list
    }

    /**
     * Đánh dấu 1 thông báo là đã đọc (Đồng bộ trên mọi thiết bị)
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        if (notificationId.isBlank()) return Result.success(Unit)
        return try {
            val now = Timestamp.now()
            db.collection("notifications")
                .document(notificationId)
                .update(
                    mapOf(
                        "isRead" to true,
                        "read" to true,
                        "readAt" to now
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Đánh dấu tất cả thông báo của người dùng là đã đọc
     */
    suspend fun markAllAsRead(userId: String): Result<Unit> {
        if (userId.isBlank()) return Result.success(Unit)
        return try {
            val snap = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val now = Timestamp.now()
            val batch = db.batch()
            for (doc in snap.documents) {
                val read = doc.getBoolean("isRead") ?: doc.getBoolean("read") ?: false
                if (!read) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "isRead" to true,
                            "read" to true,
                            "readAt" to now
                        )
                    )
                }
            }
            batch.commit().await()
            _unreadCount.value = 0
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 1. Tự động phát thông báo khi khách đặt hàng (Chống trùng lặp bằng deterministic ID)
     */
    suspend fun notifyOrderCreated(order: Order, isQrPayment: Boolean) {
        val now = Timestamp.now()
        val numFmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val amountText = "${numFmt.format(order.totalAmount.toLong())}đ"

        val customerDocId = "notif_${order.userId}_${order.orderId}_CREATED"
        val customerNotif = Notification(
            notificationId = customerDocId,
            userId = order.userId,
            title = if (isQrPayment) "Đơn hàng chờ thanh toán QR" else "Đặt hàng thành công",
            content = if (isQrPayment) {
                "Đơn hàng #${order.orderCode} ($amountText) đang được giữ chỗ 5 phút chờ thanh toán VietQR."
            } else {
                "Đơn hàng #${order.orderCode} ($amountText) đã được ghi nhận và đang chờ xác nhận."
            },
            type = if (isQrPayment) "PAYMENT" else "ORDER",
            category = if (isQrPayment) "PAYMENT_PENDING" else "ORDER_CREATED",
            referenceId = order.orderId,
            referenceType = "ORDER",
            isRead = false,
            createdAt = now
        )

        val adminDocId = "notif_ADMIN_${order.orderId}_NEW"
        val adminNotif = Notification(
            notificationId = adminDocId,
            userId = "ADMIN",
            title = if (isQrPayment) "Giao dịch QR mới cần theo dõi" else "Có đơn hàng COD mới",
            content = "Khách hàng ${order.receiverName} vừa đặt đơn #${order.orderCode} trị giá $amountText.",
            type = "ORDER",
            category = "ADMIN_NEW_ORDER",
            referenceId = order.orderId,
            referenceType = "ORDER",
            isRead = false,
            createdAt = now
        )

        try {
            val batch = db.batch()
            batch.set(db.collection("notifications").document(customerDocId), customerNotif)
            batch.set(db.collection("notifications").document(adminDocId), adminNotif)
            batch.commit().await()
        } catch (_: Exception) {}
    }

    /**
     * 2. Tự động phát thông báo khi Thanh toán QR thành công (Idempotent - Không gửi trùng)
     */
    suspend fun notifyQrPaymentSuccess(order: Order) {
        val now = Timestamp.now()
        val numFmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val amountText = "${numFmt.format(order.totalAmount.toLong())}đ"

        val docId = "notif_${order.userId}_${order.orderId}_QR_PAID"
        val notif = Notification(
            notificationId = docId,
            userId = order.userId,
            title = "Thanh toán QR thành công",
            content = "Bạn đã thanh toán thành công $amountText cho đơn hàng #${order.orderCode}. Cửa hàng đang chuẩn bị giày cho bạn!",
            type = "PAYMENT",
            category = "PAYMENT_SUCCESS",
            referenceId = order.orderId,
            referenceType = "ORDER",
            isRead = false,
            createdAt = now
        )
        try {
            db.collection("notifications").document(docId).set(notif).await()
        } catch (_: Exception) {}
    }

    /**
     * 3. Tự động phát thông báo khi trạng thái đơn hàng thay đổi (Chống gửi trùng khi Admin bấm xác nhận nhiều lần)
     */
    suspend fun notifyOrderStatusChanged(order: Order, newStatus: String) {
        val now = Timestamp.now()
        val (title, content) = when (newStatus.uppercase()) {
            "CONFIRMED" -> Pair(
                "Đơn hàng đã được xác nhận",
                "Đơn hàng #${order.orderCode} đã được xác nhận và đang được đóng gói."
            )
            "PACKING" -> Pair(
                "Đơn hàng đang chuẩn bị",
                "Đơn hàng #${order.orderCode} đang được kiểm tra chất lượng và đóng gói tại kho SoleSense."
            )
            "SHIPPING", "IN_DELIVERY" -> Pair(
                "Đơn hàng đang giao",
                "Đơn hàng #${order.orderCode} đã được bàn giao cho đơn vị vận chuyển và đang trên đường giao đến bạn."
            )
            "DELIVERED", "COMPLETED" -> Pair(
                "Giao hàng thành công",
                "Đơn hàng #${order.orderCode} đã giao thành công. Hãy đánh giá sản phẩm để nhận thêm ưu đãi nhé!"
            )
            "CANCELLED" -> Pair(
                "Đơn hàng đã bị hủy",
                "Đơn hàng #${order.orderCode} đã bị hủy. ${order.cancelReason ?: ""}"
            )
            else -> return
        }

        // Deterministic ID theo orderId + newStatus -> Admin bấm nhiều lần cũng chỉ ghi đè 1 document duy nhất, không bị trùng!
        val docId = "notif_${order.userId}_${order.orderId}_STATUS_${newStatus.uppercase()}"
        val notif = Notification(
            notificationId = docId,
            userId = order.userId,
            title = title,
            content = content,
            type = "ORDER",
            category = "ORDER_STATUS_${newStatus.uppercase()}",
            referenceId = order.orderId,
            referenceType = "ORDER",
            isRead = false,
            createdAt = now
        )
        try {
            db.collection("notifications").document(docId).set(notif).await()
        } catch (_: Exception) {}
    }

    /**
     * 4. Tự động phát thông báo khi đơn hàng bị hủy
     */
    suspend fun notifyOrderCancelled(order: Order, reason: String) {
        val now = Timestamp.now()
        val docId = "notif_${order.userId}_${order.orderId}_STATUS_CANCELLED"
        val notif = Notification(
            notificationId = docId,
            userId = order.userId,
            title = "Đơn hàng đã bị hủy",
            content = reason.ifBlank { "Đơn hàng #${order.orderCode} đã bị hủy." },
            type = "ORDER",
            category = "ORDER_CANCELLED",
            referenceId = order.orderId,
            referenceType = "ORDER",
            isRead = false,
            createdAt = now
        )
        val adminDocId = "notif_ADMIN_${order.orderId}_CANCELLED"
        val adminNotif = Notification(
            notificationId = adminDocId,
            userId = "ADMIN",
            title = "Có đơn hàng vừa bị hủy",
            content = "Đơn hàng #${order.orderCode} của khách ${order.receiverName} vừa bị hủy ($reason).",
            type = "ORDER",
            category = "ADMIN_ORDER_CANCELLED",
            referenceId = order.orderId,
            referenceType = "ORDER",
            isRead = false,
            createdAt = now
        )
        try {
            val batch = db.batch()
            batch.set(db.collection("notifications").document(docId), notif)
            batch.set(db.collection("notifications").document(adminDocId), adminNotif)
            batch.commit().await()
        } catch (_: Exception) {}
    }

    /**
     * 5. Phía Admin: Lấy danh sách thông báo quản trị & chiến dịch đã gửi
     */
    suspend fun getAdminNotifications(): Result<List<Notification>> {
        return try {
            val snap = db.collection("notifications")
                .whereEqualTo("userId", "ADMIN")
                .get()
                .await()
            val list = snap.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)?.copy(notificationId = doc.id)
            }.sortedByDescending { it.createdAt.seconds }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCampaigns(): Result<List<NotificationCampaign>> {
        return try {
            val snap = db.collection("notification_campaigns").get().await()
            val list = snap.documents.mapNotNull { doc ->
                doc.toObject(NotificationCampaign::class.java)?.copy(campaignId = doc.id)
            }.sortedByDescending { it.createdAt.seconds }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin tạo chiến dịch thông báo thủ công (Lưu vào notification_campaigns, không ghi tràn lan từng user)
     */
    suspend fun createAndSendCampaign(
        title: String,
        content: String,
        imageUrl: String? = null,
        type: String = "PROMOTION",
        targetType: String = "ALL",
        adminUid: String = "admin"
    ): Result<NotificationCampaign> {
        return try {
            val now = Timestamp.now()
            val docRef = db.collection("notification_campaigns").document()
            val campaign = NotificationCampaign(
                campaignId = docRef.id,
                title = title.trim(),
                content = content.trim(),
                imageUrl = imageUrl?.trim()?.ifBlank { null },
                type = type,
                targetType = targetType,
                status = "SENT",
                createdBy = adminUid,
                createdAt = now,
                sentAt = now
            )
            docRef.set(campaign).await()
            Result.success(campaign)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
