package com.example.solesense.model.support

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Phiếu yêu cầu hỗ trợ khách hàng xếp trong hàng chờ nhân viên.
 *
 * @param status "WAITING" (Chờ tiếp nhận) | "ASSIGNED" (Đã có người nhận) | "RESOLVED" (Đã xử lý) | "CANCELLED" (Khách đã hủy)
 */
data class SupportRequest(
    @DocumentId val requestId: String = "",
    val chatId: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val status: String = "WAITING", // "WAITING" | "ASSIGNED" | "RESOLVED" | "CANCELLED"
    val assignedStaffId: String? = null,
    val assignedStaffName: String? = null,
    val assignedAt: Timestamp? = null,
    val lastStaffActivityAt: Timestamp? = null,
    val topic: String = "Tư vấn sản phẩm",
    val previewMessage: String = "",
    val priority: String = "NORMAL",
    val handoffCount: Int = 1,
    val createdAt: Timestamp = Timestamp.now(),
    val resolvedAt: Timestamp? = null,
    val closedAt: Timestamp? = null
)

