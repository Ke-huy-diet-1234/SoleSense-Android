package com.example.solesense.model.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Quản lý phiên hội thoại giữa Khách hàng, AI và Nhân viên hỗ trợ.
 *
 * @param mode Chế độ hội thoại: "AI" (AI tự động tư vấn) | "HUMAN" (Hỗ trợ từ nhân viên)
 * @param status Trạng thái:
 *   - "ACTIVE": Đang tư vấn hoạt động
 *   - "WAITING": Đang xếp hàng chờ nhân viên tiếp nhận
 *   - "ASSIGNED": Nhân viên đã tiếp nhận và đang hỗ trợ
 *   - "RESOLVED": Yêu cầu đã được giải quyết
 *   - "CLOSED": Đã đóng hội thoại
 */
data class Chat(
    @DocumentId val chatId: String = "",
    val userId: String = "",
    val mode: String = "AI", // "AI" | "HUMAN"
    val status: String = "ACTIVE", // "ACTIVE" | "WAITING" | "ASSIGNED" | "RESOLVED" | "CLOSED"
    val assignedStaffId: String? = null,
    val assignedStaffName: String? = null,
    val currentRequestId: String? = null,
    val isStaffTyping: Boolean = false,
    val isUserTyping: Boolean = false,
    val lastMessage: String = "",
    val lastMessageAt: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val contextSummary: String = "",
    val schemaVersion: Int = 0
)
