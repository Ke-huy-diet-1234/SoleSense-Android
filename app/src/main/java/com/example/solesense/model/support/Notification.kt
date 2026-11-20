package com.example.solesense.model.support

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Mô hình Thông báo cá nhân hóa: notifications/{notificationId}
 * Hỗ trợ 5 nhóm:
 * 1. ORDER (Thông báo đơn hàng)
 * 2. PAYMENT (Thông báo thanh toán)
 * 3. PROMOTION (Thông báo khuyến mãi)
 * 4. SUPPORT (Thông báo hỗ trợ)
 * 5. SYSTEM (Thông báo hệ thống)
 */
data class Notification(
    @DocumentId val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val body: String = "", // Tương thích dữ liệu cũ nếu dùng trường body
    val type: String = "SYSTEM", // ORDER, PAYMENT, PROMOTION, SUPPORT, SYSTEM
    val category: String = "GENERAL", // ORDER_STATUS, PAYMENT_QR, VOUCHER_NEW, SUPPORT_REPLY, SYSTEM_POLICY
    val referenceId: String? = null, // orderId, voucherId, chatId...
    val referenceType: String? = null, // ORDER, VOUCHER, SUPPORT, SYSTEM
    val imageUrl: String? = null,
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val readAt: Timestamp? = null
) {
    fun displayContent(): String = content.ifBlank { body }
}

/**
 * Mô hình Chiến dịch thông báo của Admin: notification_campaigns/{campaignId}
 */
data class NotificationCampaign(
    @DocumentId val campaignId: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val type: String = "PROMOTION", // PROMOTION, SYSTEM
    val targetType: String = "ALL", // ALL, NEW_CUSTOMER
    val status: String = "SENT", // DRAFT, SCHEDULED, SENT
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val sentAt: Timestamp? = Timestamp.now()
)
