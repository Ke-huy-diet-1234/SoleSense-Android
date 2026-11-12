package com.example.solesense.model.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Mô hình tin nhắn trong cuộc hội thoại.
 *
 * @param senderType "USER" (Khách hàng) | "AI" (SoleSense AI) | "STAFF" (Nhân viên) | "SYSTEM" (Hệ thống)
 * @param messageType "TEXT" | "PRODUCT" | "IMAGE" | "SYSTEM"
 * @param sendStatus "SENDING" | "SENT" | "PROCESSING" | "COMPLETED" | "FAILED"
 * @param eventCode Mã sự kiện hệ thống (nếu là tin nhắn SYSTEM): HANDOFF_REQUESTED, HANDOFF_ACCEPTED, RETURNED_TO_AI, RESOLVED
 */
data class Message(
    @DocumentId val messageId: String = "",
    val senderType: String = "USER", // "USER" | "AI" | "STAFF" | "SYSTEM"
    val senderId: String? = null,
    val senderName: String? = null,
    val content: String = "",
    val messageType: String = "TEXT", // "TEXT" | "PRODUCT" | "IMAGE" | "SYSTEM"

    // Ngữ cảnh sản phẩm ghim / đính kèm (nếu gửi từ ProductDetailScreen)
    val productId: String? = null,
    val productName: String? = null,
    val productPrice: Double? = null,
    val productImage: String? = null,
    val productColor: String? = null,
    val productSize: String? = null,

    // Dữ liệu gợi ý từ AI
    val recommendedProductIds: List<String> = emptyList(),
    val recommendationReasons: Map<String, String> = emptyMap(), // productId -> "Còn size 41" | "Phù hợp nhu cầu"
    val recommendationExplanations: Map<String, List<String>> = emptyMap(), // productId -> chi tiết vì sao AI đề xuất
    val suggestedQuestions: List<String> = emptyList(), // "Đôi nào êm hơn?", "So sánh hai đôi"

    // Vòng đời tin nhắn
    val sendStatus: String = "SENT", // "SENDING" | "SENT" | "PROCESSING" | "COMPLETED" | "FAILED"
    val eventCode: String? = null,

    val createdAt: Timestamp = Timestamp.now()
)
