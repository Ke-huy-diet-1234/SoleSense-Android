package com.example.solesense.repository

import com.example.solesense.model.order.Order
import com.example.solesense.model.support.Notification
import com.example.solesense.model.support.NotificationCampaign
import com.example.solesense.push.FcmTokenManager
import com.example.solesense.push.PushDispatchClient
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

        // Đẩy Thông báo màn hình (FCM) cho cả Khách hàng (USER) và Quản trị viên (ADMIN)
        PushDispatchClient.pushToUser(
            targetUserId = order.userId,
            title = customerNotif.title,
            body = customerNotif.content,
            type = customerNotif.type,
            targetRole = "CUSTOMER",
            referenceId = order.orderId,
            notificationId = customerDocId
        )
        PushDispatchClient.pushToTopic(
            topic = FcmTokenManager.TOPIC_ROLE_ADMIN,
            title = adminNotif.title,
            body = adminNotif.content,
            type = "ADMIN_ORDER",
            targetRole = "ADMIN",
            referenceId = order.orderId,
            notificationId = adminDocId
        )
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

        PushDispatchClient.pushToUser(
            targetUserId = order.userId,
            title = notif.title,
            body = notif.content,
            type = "PAYMENT",
            targetRole = "CUSTOMER",
            referenceId = order.orderId,
            notificationId = docId
        )
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

        PushDispatchClient.pushToUser(
            targetUserId = order.userId,
            title = title,
            body = content,
            type = "ORDER",
            targetRole = "CUSTOMER",
            referenceId = order.orderId,
            notificationId = docId
        )
    }

    /**
     * 4. Tự động phát thông báo khi đơn hàng bị hủy
     */
    suspend fun notifyOrderCancelled(order: Order, reason: String) {
        val now = Timestamp.now()
        val isRefundNeeded = order.paymentStatus.uppercase() in setOf("SUCCESS", "PAID", "REFUND_PENDING")
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
        val adminTitle = if (isRefundNeeded) "Đơn đã thanh toán bị hủy (Cần hoàn tiền!)" else "Có đơn hàng vừa bị hủy"
        val adminNotif = Notification(
            notificationId = adminDocId,
            userId = "ADMIN",
            title = adminTitle,
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

        PushDispatchClient.pushToUser(
            targetUserId = order.userId,
            title = notif.title,
            body = notif.content,
            type = "ORDER",
            targetRole = "CUSTOMER",
            referenceId = order.orderId,
            notificationId = docId
        )
        PushDispatchClient.pushToTopic(
            topic = FcmTokenManager.TOPIC_ROLE_ADMIN,
            title = adminTitle,
            body = adminNotif.content,
            type = "ADMIN_ORDER",
            targetRole = "ADMIN",
            referenceId = order.orderId,
            notificationId = adminDocId
        )
    }

    /**
     * Cảnh báo tồn kho thấp / hết hàng gửi tới ADMIN ngay sau khi trừ kho
     */
    suspend fun notifyLowStockAlertToAdmin(
        productId: String,
        productName: String,
        variantLabel: String,
        remainingStock: Int
    ) {
        val now = Timestamp.now()
        val isOut = remainingStock <= 0
        val title = if (isOut) "Cảnh báo: Sản phẩm vừa HẾT HÀNG!" else "Cảnh báo tồn kho sắp hết"
        val body = if (isOut) {
            "$productName ($variantLabel) vừa hết sạch tồn kho (0 đôi). Vui lòng nhập hàng bổ sung!"
        } else {
            "$productName ($variantLabel) chỉ còn $remainingStock đôi trong kho."
        }
        val docId = "notif_ADMIN_STOCK_${productId}_${if (isOut) "OUT" else "LOW"}"
        val notif = Notification(
            notificationId = docId,
            userId = "ADMIN",
            title = title,
            content = body,
            type = "SYSTEM",
            category = "ADMIN_LOW_STOCK",
            referenceId = productId,
            referenceType = "PRODUCT",
            isRead = false,
            createdAt = now
        )
        try {
            db.collection("notifications").document(docId).set(notif).await()
        } catch (_: Exception) {}

        PushDispatchClient.pushToTopic(
            topic = FcmTokenManager.TOPIC_ROLE_ADMIN,
            title = title,
            body = body,
            type = "ADMIN_PRODUCT",
            targetRole = "ADMIN",
            referenceId = productId,
            notificationId = docId
        )
    }

    /**
     * Cảnh báo Voucher vừa hết sạch lượt sử dụng gửi tới ADMIN
     */
    suspend fun notifyVoucherExhaustedToAdmin(
        voucherId: String,
        voucherCode: String,
        usageLimit: Int
    ) {
        val now = Timestamp.now()
        val title = "Voucher $voucherCode đã hết lượt!"
        val body = "Mã giảm giá $voucherCode vừa đạt giới hạn $usageLimit/$usageLimit lượt sử dụng."
        val docId = "notif_ADMIN_VOUCHER_${voucherId}_EXHAUSTED"
        val notif = Notification(
            notificationId = docId,
            userId = "ADMIN",
            title = title,
            content = body,
            type = "PROMOTION",
            category = "ADMIN_VOUCHER_EXHAUSTED",
            referenceId = voucherId,
            referenceType = "VOUCHER",
            isRead = false,
            createdAt = now
        )
        try {
            db.collection("notifications").document(docId).set(notif).await()
        } catch (_: Exception) {}

        PushDispatchClient.pushToTopic(
            topic = FcmTokenManager.TOPIC_ROLE_ADMIN,
            title = title,
            body = body,
            type = "ADMIN_VOUCHER",
            targetRole = "ADMIN",
            referenceId = voucherId,
            notificationId = docId
        )
    }

    /**
     * Khách gửi yêu cầu Đổi size / Trả hàng -> Báo cho cả ADMIN và SUPPORT
     */
    suspend fun notifyReturnRequestCreated(
        returnId: String,
        orderId: String,
        orderCode: String,
        type: String,
        customerName: String,
        reason: String
    ) {
        val now = Timestamp.now()
        val typeVi = if (type.uppercase() == "EXCHANGE") "Đổi Size" else "Trả hàng hoàn tiền"
        val title = "Yêu cầu $typeVi mới (#$orderCode)"
        val body = "Khách hàng $customerName vừa gửi yêu cầu $typeVi: $reason"
        val adminDocId = "notif_ADMIN_RETURN_$returnId"
        val notif = Notification(
            notificationId = adminDocId,
            userId = "ADMIN",
            title = title,
            content = body,
            type = "ORDER",
            category = "ADMIN_RETURN_REQUEST",
            referenceId = orderId,
            referenceType = "ORDER",
            isRead = false,
            createdAt = now
        )
        try {
            db.collection("notifications").document(adminDocId).set(notif).await()
        } catch (_: Exception) {}

        PushDispatchClient.pushToTopic(
            topic = FcmTokenManager.TOPIC_ROLE_ADMIN,
            title = title,
            body = body,
            type = "RETURN_REQUEST",
            targetRole = "ADMIN",
            referenceId = orderId,
            notificationId = adminDocId
        )
        PushDispatchClient.pushToTopic(
            topic = FcmTokenManager.TOPIC_ROLE_SUPPORT,
            title = title,
            body = body,
            type = "RETURN_REQUEST",
            targetRole = "SUPPORT",
            referenceId = orderId,
            notificationId = "support_$adminDocId"
        )
    }

    /**
     * Admin / Support duyệt hoặc từ chối Đổi/Trả -> Báo trực tiếp về màn hình của USER
     */
    suspend fun notifyReturnRequestReviewed(
        returnId: String,
        targetUserId: String,
        orderId: String,
        type: String,
        status: String,
        adminNote: String
    ) {
        if (targetUserId.isBlank()) return
        val now = Timestamp.now()
        val typeVi = if (type.uppercase() == "EXCHANGE") "Đổi Size" else "Trả hàng"
        val approved = status.uppercase() == "APPROVED"
        val title = if (approved) "Yêu cầu $typeVi đã được duyệt" else "Yêu cầu $typeVi bị từ chối"
        val docId = "notif_${targetUserId}_RETURN_${returnId}_${status.uppercase()}"
        val notif = Notification(
            notificationId = docId,
            userId = targetUserId,
            title = title,
            content = adminNote,
            type = "ORDER",
            category = "RETURN_${status.uppercase()}",
            referenceId = orderId,
            referenceType = "ORDER",
            isRead = false,
            createdAt = now
        )
        try {
            db.collection("notifications").document(docId).set(notif).await()
        } catch (_: Exception) {}

        PushDispatchClient.pushToUser(
            targetUserId = targetUserId,
            title = title,
            body = adminNote,
            type = "RETURN_REVIEWED",
            targetRole = "CUSTOMER",
            referenceId = orderId,
            notificationId = docId
        )
    }

    /**
     * Cửa hàng phản hồi đánh giá sản phẩm -> Báo về màn hình USER
     */
    suspend fun notifyStoreRepliedToReview(
        reviewId: String,
        targetUserId: String,
        productId: String,
        storeReply: String
    ) {
        if (targetUserId.isBlank()) return
        val now = Timestamp.now()
        val title = "SoleSense đã phản hồi đánh giá của bạn"
        val docId = "notif_${targetUserId}_REVIEW_REPLY_$reviewId"
        val notif = Notification(
            notificationId = docId,
            userId = targetUserId,
            title = title,
            content = storeReply,
            type = "SYSTEM",
            category = "STORE_REVIEW_REPLY",
            referenceId = productId,
            referenceType = "PRODUCT",
            isRead = false,
            createdAt = now
        )
        try {
            db.collection("notifications").document(docId).set(notif).await()
        } catch (_: Exception) {}

        PushDispatchClient.pushToUser(
            targetUserId = targetUserId,
            title = title,
            body = storeReply,
            type = "PRODUCT_REVIEW",
            targetRole = "CUSTOMER",
            referenceId = productId,
            notificationId = docId
        )
    }

    /**
     * Đánh giá tiêu cực (1-2 sao) -> Cảnh báo ngay cho SUPPORT & ADMIN phản hồi sớm
     */
    suspend fun notifyNegativeReviewToStaff(
        reviewId: String,
        productId: String,
        userName: String,
        rating: Int,
        comment: String
    ) {
        val title = "Cảnh báo đánh giá $rating★ cần phản hồi sớm"
        val body = "$userName vừa đánh giá $rating sao: \"${comment.take(90)}\""
        PushDispatchClient.pushToTopic(
            topic = FcmTokenManager.TOPIC_ROLE_SUPPORT,
            title = title,
            body = body,
            type = "REVIEW_ALERT",
            targetRole = "SUPPORT",
            referenceId = productId,
            notificationId = "support_neg_rev_$reviewId"
        )
        PushDispatchClient.pushToTopic(
            topic = FcmTokenManager.TOPIC_ROLE_ADMIN,
            title = title,
            body = body,
            type = "REVIEW_ALERT",
            targetRole = "ADMIN",
            referenceId = productId,
            notificationId = "admin_neg_rev_$reviewId"
        )
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

            // Đẩy Thông báo màn hình (FCM) tới toàn bộ khách hàng đã đăng ký Topic `all_customers`
            PushDispatchClient.pushToTopic(
                topic = FcmTokenManager.TOPIC_ALL_CUSTOMERS,
                title = campaign.title,
                body = campaign.content,
                type = campaign.type,
                targetRole = "CUSTOMER",
                referenceId = campaign.campaignId,
                notificationId = "camp_${campaign.campaignId}"
            )

            Result.success(campaign)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
