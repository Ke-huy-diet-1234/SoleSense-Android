package com.example.solesense.repository

import android.util.Log
import com.example.solesense.data.repository.ProductRepository
import com.example.solesense.model.catalog.Product
import com.example.solesense.model.catalog.Variant
import com.example.solesense.model.chat.Chat
import com.example.solesense.model.chat.Message
import com.example.solesense.model.support.SupportRequest
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

data class ProductWithVariants(
    val product: Product,
    val variants: List<Variant>
)

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val productRepository: ProductRepository = ProductRepository()
) {

    companion object {
        private const val TAG = "ChatRepository"
        const val CURRENT_CHAT_SCHEMA_VERSION = 4
        @Volatile
        private var hasRunLegacyWipeInSession = false
    }

    /**
     * Xóa sạch toàn bộ lịch sử chat và phiếu hỗ trợ cũ (schemaVersion < CURRENT_CHAT_SCHEMA_VERSION)
     * trên Firestore để làm sạch dữ liệu thử nghiệm từ trước đến nay.
     */
    suspend fun ensureCleanHistoryMigration() {
        if (hasRunLegacyWipeInSession) return
        try {
            val allChatsSnap = db.collection("chats").get().await()
            val legacyChatDocs = allChatsSnap.documents.filter { doc ->
                (doc.getLong("schemaVersion")?.toInt() ?: 0) < CURRENT_CHAT_SCHEMA_VERSION
            }

            if (legacyChatDocs.isNotEmpty()) {
                for (chatDoc in legacyChatDocs) {
                    val msgsSnap = chatDoc.reference.collection("messages").get().await()
                    for (msgDoc in msgsSnap.documents) {
                        msgDoc.reference.delete().await()
                    }
                    chatDoc.reference.delete().await()
                }
                // Đồng thời dọn sạch toàn bộ phiếu supportRequests cũ liên quan đến lịch sử cũ
                val allReqsSnap = db.collection("supportRequests").get().await()
                for (reqDoc in allReqsSnap.documents) {
                    reqDoc.reference.delete().await()
                }
            }
            hasRunLegacyWipeInSession = true
        } catch (e: Exception) {
            Log.w(TAG, "Không thể dọn lịch sử chat cũ: ${e.message}")
        }
    }

    /**
     * Lấy hoặc tạo phòng chat duy nhất cho một User theo định dạng doc: chats/chat_{userId}
     */
    suspend fun getOrCreateChat(userId: String): Chat {
        ensureCleanHistoryMigration()

        val chatId = "chat_$userId"
        val docRef = db.collection("chats").document(chatId)
        val snapshot = docRef.get().await()

        val existingVersion = snapshot.getLong("schemaVersion")?.toInt() ?: 0
        if (snapshot.exists() && existingVersion >= CURRENT_CHAT_SCHEMA_VERSION) {
            return snapshot.toObject(Chat::class.java) ?: Chat(
                chatId = chatId,
                userId = userId,
                schemaVersion = CURRENT_CHAT_SCHEMA_VERSION
            )
        } else {
            // Nếu doc cũ vẫn còn sót messages, xóa sạch trước khi khởi tạo mới
            if (snapshot.exists()) {
                try {
                    val oldMsgs = docRef.collection("messages").get().await()
                    for (m in oldMsgs.documents) {
                        m.reference.delete().await()
                    }
                } catch (_: Exception) {}
            }

            val newChat = Chat(
                chatId = chatId,
                userId = userId,
                mode = "AI",
                status = "ACTIVE",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                schemaVersion = CURRENT_CHAT_SCHEMA_VERSION
            )
            docRef.set(newChat).await()

            // Tạo tin nhắn chào mở đầu từ SoleSense AI
            val welcomeMsg = Message(
                messageId = UUID.randomUUID().toString(),
                senderType = "AI",
                senderName = "SoleSense AI",
                content = "Xin chào! Mình là SoleSense AI, trợ lý tư vấn chọn giày, chọn size và so sánh sản phẩm. Bạn đang tìm đôi giày chạy bộ hay sneaker nào hôm nay?",
                messageType = "TEXT",
                suggestedQuestions = listOf(
                    "Tư vấn giày chạy bộ tầm 3 triệu",
                    "Sản phẩm nào đang có giá rẻ nhất?",
                    "Chân dài 25.5cm mang size mấy?",
                    "So sánh Pegasus 41 và Ultraboost 5"
                ),
                sendStatus = "COMPLETED",
                createdAt = Timestamp.now()
            )
            docRef.collection("messages").document(welcomeMsg.messageId).set(welcomeMsg).await()
            return newChat
        }
    }

    /**
     * Lắng nghe realtime dữ liệu của Chat Document (Mode, Status, Nhân viên hỗ trợ, v.v.)
     */
    fun getChatFlow(chatId: String): Flow<Chat?> = callbackFlow {
        val listener = db.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val chat = snapshot?.toObject(Chat::class.java)
                trySend(chat)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Lắng nghe realtime danh sách tin nhắn theo thứ tự thời gian tăng dần
     */
    fun getMessagesFlow(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                // Ẩn các tin nhắn AI đã bị CANCELLED (do khách chuyển sang nhân viên khi AI đang xử lý)
                val visibleMessages = messages.filter { it.sendStatus != "CANCELLED" }
                trySend(visibleMessages)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Gửi tin nhắn từ người dùng (Customer) hoặc nhân viên (Staff)
     */
    suspend fun sendMessage(chatId: String, message: Message): Result<String> {
        return try {
            val messageId = if (message.messageId.isNotEmpty()) message.messageId else UUID.randomUUID().toString()
            val msgToSave = message.copy(messageId = messageId, createdAt = Timestamp.now())

            val chatRef = db.collection("chats").document(chatId)
            val msgRef = chatRef.collection("messages").document(messageId)

            msgRef.set(msgToSave).await()

            // Cập nhật lastMessage của chat
            chatRef.update(
                mapOf(
                    "lastMessage" to msgToSave.content,
                    "lastMessageAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            // Nếu khách đang ở trạng thái chờ nhân viên (WAITING) và gửi thêm tin nhắn,
            // cập nhật previewMessage trong supportRequests để nhân viên thấy câu hỏi mới nhất
            if (msgToSave.senderType == "USER") {
                val chatSnap = chatRef.get().await()
                val mode = chatSnap.getString("mode") ?: "AI"
                val status = chatSnap.getString("status") ?: "ACTIVE"
                val currentReqId = chatSnap.getString("currentRequestId")
                if (mode == "HUMAN" && status == "WAITING" && !currentReqId.isNullOrEmpty()) {
                    try {
                        db.collection("supportRequests").document(currentReqId).update(
                            "previewMessage", msgToSave.content
                        ).await()
                    } catch (_: Exception) {}
                }
            }

            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cập nhật tin nhắn (nội dung, trạng thái xử lý, v.v.)
     */
    suspend fun updateMessage(chatId: String, messageId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy danh sách sản phẩm kèm toàn bộ biến thể (size, màu, tồn kho) chuẩn từ Firestore
     */
    private suspend fun fetchCatalogWithVariants(): List<ProductWithVariants> {
        val allProducts = productRepository.getProducts().getOrDefault(emptyList())
            .filter { it.status != "DISABLED" }
        return allProducts.map { product ->
            val variants = productRepository.getVariantsByProductId(product.productId)
                .getOrDefault(emptyList())
                .filter { it.status != "DISABLED" }
            ProductWithVariants(product, variants)
        }
    }

    /**
     * Gọi xử lý phản hồi từ AI (Gemini qua Firebase AI Logic) với cơ chế Sliding Window,
     * đối chiếu dữ liệu thực tế từ Firestore và hậu kiểm (Post-validation).
     */
    suspend fun processAiResponse(
        chatId: String,
        userMessage: Message,
        recentMessages: List<Message>
    ) {
        // Kiểm tra trạng thái mới nhất của Chat trước khi tạo phản hồi AI
        val chatDoc = try {
            db.collection("chats").document(chatId).get().await()
        } catch (_: Exception) {
            null
        }
        if (chatDoc != null && (chatDoc.getString("mode") ?: "AI") != "AI") {
            return
        }

        val aiMessageId = UUID.randomUUID().toString()
        val aiPlaceholder = Message(
            messageId = aiMessageId,
            senderType = "AI",
            senderName = "SoleSense AI",
            content = "",
            messageType = "TEXT",
            sendStatus = "PROCESSING",
            createdAt = Timestamp.now()
        )

        // 1. Lưu tin nhắn AI với trạng thái PROCESSING
        sendMessage(chatId, aiPlaceholder)

        try {
            // 2. Lấy toàn bộ danh mục sản phẩm + biến thể từ Firestore
            val catalog = fetchCatalogWithVariants()
            val allProducts = catalog.map { it.product }

            // Nếu tin nhắn có đính kèm productId (từ FAB ProductDetail), kiểm tra lại dữ liệu chuẩn từ Firestore
            var verifiedProductContextNote = ""
            if (!userMessage.productId.isNullOrEmpty()) {
                val freshProductResult = productRepository.getProductById(userMessage.productId)
                val freshProduct = freshProductResult.getOrNull()
                if (freshProduct == null || freshProduct.status == "DISABLED") {
                    verifiedProductContextNote = "LƯU Ý QUAN TRỌNG: Sản phẩm (ID: ${userMessage.productId}) khách đang hỏi hiện đã NGỪNG KINH DOANH. Hãy thông báo lịch sự cho khách và gợi ý mẫu tương đương."
                } else {
                    val freshVariants = productRepository.getVariantsByProductId(freshProduct.productId).getOrDefault(emptyList())
                    val effectivePrice = freshProduct.salePrice ?: freshProduct.basePrice
                    val matchedVariant = freshVariants.find {
                        it.color.equals(userMessage.productColor, ignoreCase = true) &&
                            it.size.equals(userMessage.productSize, ignoreCase = true)
                    }
                    val variantStockNote = if (matchedVariant != null) {
                        if (matchedVariant.stock > 0 && matchedVariant.status == "ACTIVE") {
                            "Biến thể Màu ${matchedVariant.color} - Size ${matchedVariant.size} hiện CÒN ${matchedVariant.stock} đôi (Giá biến thể: ${formatVnd(matchedVariant.price)})."
                        } else {
                            val availableSizes = freshVariants.filter { it.stock > 0 && it.status == "ACTIVE" }
                                .joinToString(", ") { "${it.color} size ${it.size} (còn ${it.stock})" }
                            "Biến thể Màu ${userMessage.productColor} - Size ${userMessage.productSize} hiện ĐÃ HẾT HÀNG. Các biến thể còn hàng của mẫu này: ${availableSizes.ifEmpty { "Hiện tất cả biến thể đều hết hàng" }}."
                        }
                    } else {
                        "Giá hiện tại trên hệ thống: ${formatVnd(effectivePrice)}, tổng tồn kho: ${freshProduct.totalStockQuantity}."
                    }
                    verifiedProductContextNote = "Khách đang hỏi trực tiếp về sản phẩm '${freshProduct.name}' (ID: ${freshProduct.productId}), Giá chuẩn mới nhất từ hệ thống: ${formatVnd(effectivePrice)}. $variantStockNote"
                }
            }

            // 3. Xây dựng System Instruction & Prompt với Sliding Window (10-15 tin nhắn gần nhất)
            val systemInstructionText = buildSystemInstruction(catalog)
            val userPromptText = buildConversationPrompt(
                userMessage = userMessage,
                recentMessages = recentMessages.filter { it.messageId != aiMessageId }.takeLast(12),
                verifiedProductContextNote = verifiedProductContextNote
            )

            var parsedReply = ""
            val recommendedIds = mutableListOf<String>()
            val reasonsMap = mutableMapOf<String, String>()
            val explanationsMap = mutableMapOf<String, List<String>>()
            val questionsList = mutableListOf<String>()

            // 4. Gọi Gemini qua Firebase AI Logic (Sử dụng model mới nhất gemini-2.5-flash, dự phòng gemini-2.0-flash)
            val rawGeminiOutput = callGeminiWithFallbackModels(
                systemInstructionText = systemInstructionText,
                promptText = userPromptText
            )

            if (!rawGeminiOutput.isNullOrBlank()) {
                val parseSuccess = parseStructuredAiOutput(
                    rawOutput = rawGeminiOutput,
                    validProducts = allProducts,
                    catalog = catalog,
                    outReply = { parsedReply = it },
                    recommendedIds = recommendedIds,
                    reasonsMap = reasonsMap,
                    explanationsMap = explanationsMap,
                    questionsList = questionsList
                )
                if (!parseSuccess || parsedReply.isBlank()) {
                    parsedReply = rawGeminiOutput.trim()
                }
            }

            // Nếu Gemini không phản hồi được (do cấu hình API trên Firebase Console hoặc mạng),
            // sử dụng Bộ phân tích Danh mục Thông minh (Smart Catalog Analyzer) truy vấn trực tiếp trên dữ liệu thật
            if (parsedReply.isBlank()) {
                parsedReply = generateSmartCatalogResponse(
                    userMessage = userMessage,
                    catalog = catalog,
                    verifiedProductContextNote = verifiedProductContextNote,
                    recommendedIds = recommendedIds,
                    reasonsMap = reasonsMap,
                    explanationsMap = explanationsMap,
                    questionsList = questionsList
                )
            }

            // 5. Hậu kiểm (Post-validation):
            // - Loại bỏ sản phẩm trùng lặp hoặc không tồn tại / DISABLED trong Firestore
            // - Kiểm tra tồn kho thực tế để gắn cảnh báo nếu hết hàng
            val validatedIds = recommendedIds.distinct().filter { id ->
                allProducts.any { p -> p.productId == id && p.status != "DISABLED" }
            }
            recommendedIds.clear()
            recommendedIds.addAll(validatedIds)

            recommendedIds.forEach { id ->
                val prod = allProducts.find { it.productId == id }
                if (prod != null && (prod.totalStockQuantity <= 0 || prod.status == "OUT_OF_STOCK")) {
                    reasonsMap[id] = "Tạm hết hàng"
                }
            }

            // 6. Kiểm tra lại trạng thái Chat trước khi ghi kết quả:
            // Nếu trong lúc AI đang xử lý mà khách bấm "Gặp nhân viên" (chuyển sang HUMAN),
            // tuyệt đối KHÔNG ghi đè câu trả lời của AI vào hội thoại của nhân viên!
            val latestChatSnap = db.collection("chats").document(chatId).get().await()
            val latestMode = latestChatSnap.getString("mode") ?: "AI"
            if (latestMode != "AI") {
                updateMessage(
                    chatId = chatId,
                    messageId = aiMessageId,
                    updates = mapOf("sendStatus" to "CANCELLED")
                )
                return
            }

            // 7. Cập nhật tin nhắn AI thành COMPLETED
            updateMessage(
                chatId,
                aiMessageId,
                mapOf(
                    "content" to parsedReply,
                    "sendStatus" to "COMPLETED",
                    "recommendedProductIds" to recommendedIds,
                    "recommendationReasons" to reasonsMap,
                    "recommendationExplanations" to explanationsMap,
                    "suggestedQuestions" to questionsList
                )
            )

            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessage" to parsedReply,
                    "lastMessageAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )
            ).await()

        } catch (e: CancellationException) {
            // Bị hủy chủ động khi khách chuyển sang Nhân viên
            updateMessage(
                chatId = chatId,
                messageId = aiMessageId,
                updates = mapOf("sendStatus" to "CANCELLED")
            )
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi xử lý phản hồi AI", e)
            updateMessage(
                chatId,
                aiMessageId,
                mapOf(
                    "content" to "Rất tiếc, kết nối tới hệ thống tư vấn đang bị gián đoạn. Bạn vui lòng nhấn Thử lại nhé.",
                    "sendStatus" to "FAILED"
                )
            )
        }
    }

    /**
     * Gọi Firebase AI Logic với danh sách model chuẩn mới nhất (gemini-2.5-flash -> gemini-2.0-flash)
     */
    private suspend fun callGeminiWithFallbackModels(
        systemInstructionText: String,
        promptText: String
    ): String? {
        val candidateModels = listOf("gemini-2.5-flash", "gemini-2.0-flash")
        val firebaseAi = try {
            Firebase.ai(backend = GenerativeBackend.googleAI())
        } catch (e: Exception) {
            Log.w(TAG, "Không khởi tạo được Firebase.ai(googleAI): ${e.message}")
            return null
        }

        for (modelName in candidateModels) {
            try {
                val model = firebaseAi.generativeModel(
                    modelName = modelName,
                    systemInstruction = content { text(systemInstructionText) }
                )
                val response = model.generateContent(promptText)
                val text = response.text
                if (!text.isNullOrBlank()) {
                    return text
                }
            } catch (e: Exception) {
                Log.w(TAG, "Model $modelName không phản hồi: ${e.message}")
            }
        }
        return null
    }

    /**
     * Xây dựng System Instruction chặt chẽ với đầy đủ dữ liệu sản phẩm & biến thể từ Firestore
     */
    private fun buildSystemInstruction(catalog: List<ProductWithVariants>): String {
        val sb = StringBuilder()
        sb.appendLine("Bạn là SoleSense AI - Chuyên gia tư vấn giày thể thao của ứng dụng SoleSense.")
        sb.appendLine("NGUYÊN TẮC BẮT BUỘC:")
        sb.appendLine("1. Trả lời bằng tiếng Việt tự nhiên, thân thiện, đi thẳng vào đúng câu hỏi của khách hàng.")
        sb.appendLine("2. CHỈ sử dụng dữ liệu sản phẩm, giá bán và tồn kho từng size trong DANH MỤC THỰC TẾ bên dưới. Tuyệt đối không bịa đặt sản phẩm, không bịa giá hoặc tồn kho.")
        sb.appendLine("3. Quy đổi chiều dài bàn chân sang size EU tham khảo: 24.5-25.0cm -> Size 40; 25.1-25.6cm -> Size 41; 25.7-26.3cm -> Size 42; 26.4-27.0cm -> Size 43.")
        sb.appendLine("4. Khi khách hỏi sản phẩm rẻ nhất, đắt nhất, bán chạy nhất, hoặc hỏi theo ngân sách/size, hãy tính toán và trả lời chính xác tên giày, mức giá, size nào còn hàng, size nào hết hàng.")
        sb.appendLine("5. Nếu không có sản phẩm nào thỏa mãn TẤT CẢ tiêu chí của khách (ví dụ: ngân sách dưới 1 triệu, hoặc size 44 không có), hãy nói rõ lý do và hỏi khách có muốn nới lỏng điều kiện theo thứ tự ưu tiên: (1) Giá -> (2) Màu sắc -> (3) Thương hiệu -> (4) Mục đích sử dụng.")
        sb.appendLine("6. Không bao giờ yêu cầu khách cung cấp mật khẩu, mã OTP hoặc thông tin thẻ thanh toán. Bạn chỉ đóng vai trò tư vấn, không tự ý thay đổi đơn hàng.")
        sb.appendLine("7. CHỈ đính kèm sản phẩm vào mảng `recommendations` khi câu trả lời thực sự có gợi ý hoặc nhắc tới mẫu giày đó. Mỗi sản phẩm có `reason` thật ngắn gọn (tối đa 22 ký tự, có căn cứ như 'Rẻ nhất • 2.450.000đ', 'Phù hợp ngân sách', 'Còn size 41', 'Đệm Boost siêu êm').")
        sb.appendLine()
        sb.appendLine("DANH MỤC SẢN PHẨM & BIẾN THỂ THỰC TẾ TRÊN FIRESTORE:")
        catalog.forEachIndexed { index, item ->
            val p = item.product
            val effectivePrice = p.salePrice ?: p.basePrice
            sb.appendLine("${index + 1}. ID: \"${p.productId}\" | Tên: \"${p.name}\" | Thương hiệu: ${p.brandId.uppercase()} | Danh mục: ${p.categoryId}")
            sb.appendLine("   - Giá bán hiện tại: ${formatVnd(effectivePrice)} (Giá gốc: ${formatVnd(p.basePrice)})")
            sb.appendLine("   - Đặc tính: Mục đích='${p.purpose}', Đệm='${p.cushioning ?: "Tiêu chuẩn"}', Chất liệu='${p.material ?: "Mesh"}', Địa hình='${p.terrain ?: "Đa năng"}'")
            sb.appendLine("   - Đánh giá: ${p.rating}/5 (${p.reviewCount} đánh giá) | Đã bán: ${p.totalSoldCount} đôi | Tổng tồn kho: ${p.totalStockQuantity}")
            if (item.variants.isNotEmpty()) {
                val variantSummary = item.variants.joinToString("; ") { v ->
                    val stockText = if (v.stock > 0 && v.status == "ACTIVE") "Còn ${v.stock} đôi" else "HẾT HÀNG"
                    "Màu ${v.color} - Size ${v.size}: ${formatVnd(v.price)} ($stockText)"
                }
                sb.appendLine("   - Biến thể chi tiết: $variantSummary")
            }
        }
        sb.appendLine()
        sb.appendLine("ĐỊNH DẠNG ĐẦU RA BẮT BUỘC (Trả về duy nhất 1 JSON object hợp lệ, không bọc trong markdown khác nếu không cần thiết):")
        sb.appendLine("""
            {
              "reply": "Câu trả lời tư vấn chi tiết, rõ ràng, giải đáp đúng trọng tâm câu hỏi của khách...",
              "recommendations": [
                {
                  "productId": "id_san_pham_chinh_xac",
                  "reason": "Nhãn ngắn gọn <= 22 ký tự (VD: Rẻ nhất • 2.45tr, Còn size 41, Phù hợp ngân sách)",
                  "explanations": [
                    "Lý do cụ thể 1 dựa trên dữ liệu thực tế (giá/ngân sách)",
                    "Lý do cụ thể 2 dựa trên công nghệ đệm/mục đích sử dụng",
                    "Lý do cụ thể 3 dựa trên tình trạng size/tồn kho"
                  ]
                }
              ],
              "suggestedQuestions": [
                "Câu hỏi gợi ý tiếp theo 1?",
                "Câu hỏi gợi ý tiếp theo 2?"
              ]
            }
        """.trimIndent())
        return sb.toString()
    }

    /**
     * Xây dựng Prompt cuộc hội thoại theo kỹ thuật Cửa sổ trượt (Sliding Window)
     */
    private fun buildConversationPrompt(
        userMessage: Message,
        recentMessages: List<Message>,
        verifiedProductContextNote: String
    ): String {
        val sb = StringBuilder()
        if (verifiedProductContextNote.isNotEmpty()) {
            sb.appendLine("[NGỮ CẢNH SẢN PHẨM ĐÍNH KÈM TỪ MÀN HÌNH CHI TIẾT]:")
            sb.appendLine(verifiedProductContextNote)
            sb.appendLine()
        }

        if (recentMessages.isNotEmpty()) {
            sb.appendLine("[LỊCH SỬ HỘI THOẠI GẦN NHẤT (Sliding Window)]:")
            recentMessages.forEach { m ->
                val roleLabel = when (m.senderType) {
                    "USER" -> "Khách hàng"
                    "AI" -> "SoleSense AI"
                    "STAFF" -> "Nhân viên hỗ trợ (${m.senderName ?: "CSKH"})"
                    else -> null
                }
                if (roleLabel != null && m.content.isNotBlank()) {
                    val prodMeta = if (!m.productId.isNullOrEmpty()) {
                        " [Đính kèm SP: ${m.productName ?: m.productId}, Màu: ${m.productColor}, Size: ${m.productSize}]"
                    } else ""
                    sb.appendLine("- $roleLabel$prodMeta: ${m.content}")
                }
            }
            sb.appendLine()
        }

        sb.appendLine("[CÂU HỎI HIỆN TẠI CỦA KHÁCH HÀNG]:")
        sb.appendLine(userMessage.content)
        sb.appendLine("\nHãy phân tích và trả về đúng định dạng JSON đã quy định.")
        return sb.toString()
    }

    /**
     * Bóc tách JSON có cấu trúc từ Gemini
     */
    private fun parseStructuredAiOutput(
        rawOutput: String,
        validProducts: List<Product>,
        catalog: List<ProductWithVariants>,
        outReply: (String) -> Unit,
        recommendedIds: MutableList<String>,
        reasonsMap: MutableMap<String, String>,
        explanationsMap: MutableMap<String, List<String>>,
        questionsList: MutableList<String>
    ): Boolean {
        return try {
            val startIdx = rawOutput.indexOf('{')
            val endIdx = rawOutput.lastIndexOf('}')
            if (startIdx == -1 || endIdx <= startIdx) return false

            val jsonStr = rawOutput.substring(startIdx, endIdx + 1)
            val jsonObj = JSONObject(jsonStr)
            val reply = jsonObj.optString("reply", "").trim()
            if (reply.isEmpty()) return false
            outReply(reply)

            val recsArray = jsonObj.optJSONArray("recommendations") ?: JSONArray()
            for (i in 0 until recsArray.length()) {
                val item = recsArray.optJSONObject(i) ?: continue
                val prodId = item.optString("productId", "").trim()
                val matchedProduct = validProducts.find { it.productId == prodId } ?: continue

                if (!recommendedIds.contains(matchedProduct.productId)) {
                    recommendedIds.add(matchedProduct.productId)
                    val reason = item.optString("reason", "Phù hợp nhu cầu").take(26)
                    reasonsMap[matchedProduct.productId] = reason

                    val expArr = item.optJSONArray("explanations")
                    val expList = mutableListOf<String>()
                    if (expArr != null) {
                        for (j in 0 until expArr.length()) {
                            val line = expArr.optString(j, "").trim()
                            if (line.isNotEmpty()) expList.add(line)
                        }
                    }
                    if (expList.isEmpty()) {
                        expList.addAll(buildDefaultExplanationsForProduct(matchedProduct, catalog))
                    }
                    explanationsMap[matchedProduct.productId] = expList
                }
            }

            val qArray = jsonObj.optJSONArray("suggestedQuestions") ?: JSONArray()
            for (i in 0 until qArray.length()) {
                val q = qArray.optString(i, "").trim()
                if (q.isNotEmpty()) questionsList.add(q)
            }
            if (questionsList.isEmpty()) {
                questionsList.addAll(listOf("Kiểm tra size còn hàng?", "So sánh chi tiết các mẫu"))
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Không parse được JSON từ Gemini, chuyển sang phân tích văn bản: ${e.message}")
            false
        }
    }

    /**
     * Bộ phân tích Danh mục Thông minh (Smart Catalog Analyzer):
     * Đảm bảo kể cả khi mất kết nối Gemini hoặc chưa bật quota, AI vẫn trả lời chính xác 100%
     * từng câu hỏi (rẻ nhất, đắt nhất, ngân sách, size chân, tồn kho biến thể, so sánh, nới lỏng điều kiện)
     * dựa trên dữ liệu thực tế từ Firestore!
     */
    private fun generateSmartCatalogResponse(
        userMessage: Message,
        catalog: List<ProductWithVariants>,
        verifiedProductContextNote: String,
        recommendedIds: MutableList<String>,
        reasonsMap: MutableMap<String, String>,
        explanationsMap: MutableMap<String, List<String>>,
        questionsList: MutableList<String>
    ): String {
        val rawQuery = userMessage.content.trim()
        val query = rawQuery.lowercase(Locale.getDefault())
        val activeCatalog = catalog.filter { it.product.status == "ACTIVE" }

        if (activeCatalog.isEmpty()) {
            return "Hiện tại không thể tải danh sách sản phẩm từ hệ thống hoặc cửa hàng đang cập nhật kho. Bạn vui lòng thử lại sau giây lát nhé!"
        }

        // 1. TRƯỜNG HỢP KHÁCH HỎI VỀ SẢN PHẨM ĐÍNH KÈM TỪ FAB PRODUCT DETAIL
        if (!userMessage.productId.isNullOrEmpty()) {
            val matchedItem = catalog.find { it.product.productId == userMessage.productId }
            if (matchedItem == null || matchedItem.product.status == "DISABLED") {
                val alternative = activeCatalog.first()
                recommendedIds.add(alternative.product.productId)
                reasonsMap[alternative.product.productId] = "Mẫu thay thế nổi bật"
                explanationsMap[alternative.product.productId] = buildDefaultExplanationsForProduct(alternative.product, catalog)
                questionsList.addAll(listOf("Mẫu thay thế này còn size gì?", "So sánh với đôi vừa xem"))
                return "Rất tiếc, sản phẩm ${userMessage.productName ?: "bạn vừa chọn"} hiện đã ngừng kinh doanh tại SoleSense. Mình gợi ý bạn tham khảo mẫu ${alternative.product.name} có kiểu dáng và công năng tương đương nhé!"
            }

            val prod = matchedItem.product
            val effectivePrice = prod.salePrice ?: prod.basePrice
            val color = userMessage.productColor ?: "Tiêu chuẩn"
            val size = userMessage.productSize ?: "41"
            val matchedVariant = matchedItem.variants.find {
                it.color.equals(color, ignoreCase = true) && it.size.equals(size, ignoreCase = true)
            }

            recommendedIds.add(prod.productId)
            val stockSentence = if (matchedVariant != null) {
                if (matchedVariant.stock > 0 && matchedVariant.status == "ACTIVE") {
                    reasonsMap[prod.productId] = "Còn size $size (${matchedVariant.stock} đôi)"
                    "Phiên bản Màu $color • Size $size bạn chọn hiện đang CÒN HÀNG (${matchedVariant.stock} đôi) với giá ${formatVnd(matchedVariant.price)}."
                } else {
                    reasonsMap[prod.productId] = "Size $size tạm hết"
                    val otherAvailable = matchedItem.variants.filter { it.stock > 0 && it.status == "ACTIVE" }
                    val altText = if (otherAvailable.isNotEmpty()) {
                        "Tuy nhiên mẫu này đang còn sẵn: " + otherAvailable.joinToString(", ") { "Màu ${it.color} size ${it.size} (${it.stock} đôi)" } + "."
                    } else {
                        "Hiện tại các size của mẫu này đang tạm hết hàng."
                    }
                    "Lưu ý: Phiên bản Màu $color • Size $size bạn đang chọn hiện ĐÃ HẾT HÀNG. $altText"
                }
            } else {
                reasonsMap[prod.productId] = "Đang xem • ${formatVnd(effectivePrice)}"
                "Mẫu này đang có giá ưu đãi ${formatVnd(effectivePrice)}."
            }

            explanationsMap[prod.productId] = listOf(
                "Giá chuẩn hiện tại từ hệ thống: ${formatVnd(effectivePrice)} (Giá gốc: ${formatVnd(prod.basePrice)}).",
                "Công nghệ đệm: ${prod.cushioning ?: "Êm ái"}, chất liệu ${prod.material ?: "thoáng khí"}.",
                "Mục đích phù hợp: ${prod.purpose}."
            )
            questionsList.addAll(
                listOf(
                    "Form đôi ${prod.name} ôm hay rộng?",
                    "Đôi này chạy đường dài êm không?",
                    "So sánh với mẫu khác cùng tầm giá"
                )
            )

            return "Về mẫu ${prod.name} bạn đang quan tâm:\n• $stockSentence\n• Đặc điểm nổi bật: Sử dụng đệm ${prod.cushioning ?: "cao cấp"}, chất liệu ${prod.material ?: "thoáng khí"}, cực kỳ phù hợp cho nhu cầu ${prod.purpose.lowercase()}.\nBạn muốn mình kiểm tra chi tiết cách chọn size theo chiều dài chân hay so sánh với một mẫu khác?"
        }

        // 2. TRƯỜNG HỢP KHÁCH HỎI SẢN PHẨM RẺ NHẤT / GIÁ THẤP NHẤT / TIẾT KIỆM NHẤT
        if (query.contains("rẻ nhất") || query.contains("thấp nhất") || query.contains("mềm nhất") || query.contains("tiết kiệm")) {
            val sortedByPrice = activeCatalog.sortedBy { it.product.salePrice ?: it.product.basePrice }
            val cheapest = sortedByPrice.first()
            val cheapestPrice = cheapest.product.salePrice ?: cheapest.product.basePrice
            val availableSizes = cheapest.variants.filter { it.stock > 0 && it.status == "ACTIVE" }
                .map { it.size }.distinct().joinToString(", ")

            recommendedIds.add(cheapest.product.productId)
            reasonsMap[cheapest.product.productId] = "Rẻ nhất • ${formatVnd(cheapestPrice)}"
            explanationsMap[cheapest.product.productId] = listOf(
                "Có mức giá thấp nhất toàn cửa hàng hiện tại: ${formatVnd(cheapestPrice)} (giảm từ ${formatVnd(cheapest.product.basePrice)}).",
                "Trang bị đệm ${cheapest.product.cushioning ?: "êm ái"}, đáp ứng tốt nhu cầu ${cheapest.product.purpose.lowercase()}.",
                if (availableSizes.isNotEmpty()) "Các size đang còn hàng: Size $availableSizes." else "Cần kiểm tra lại tồn kho biến thể khi đặt hàng."
            )
            questionsList.addAll(
                listOf(
                    "${cheapest.product.name} còn size 41 không?",
                    "So sánh với đôi đắt hơn thì khác gì?",
                    "Tư vấn chọn size đôi này"
                )
            )

            val otherComparison = if (sortedByPrice.size > 1) {
                val second = sortedByPrice[1].product
                val secondPrice = second.salePrice ?: second.basePrice
                " (tiết kiệm hơn ${formatVnd(secondPrice - cheapestPrice)} so với mẫu ${second.name} giá ${formatVnd(secondPrice)})"
            } else ""

            return "Hiện tại sản phẩm có mức giá rẻ nhất tại SoleSense là **${cheapest.product.name}** với giá ưu đãi chỉ **${formatVnd(cheapestPrice)}** (giá gốc ${formatVnd(cheapest.product.basePrice)})$otherComparison.\n\nDù có mức giá tốt nhất nhưng đôi này vẫn sở hữu đệm ${cheapest.product.cushioning ?: "cao cấp"}, chất liệu ${cheapest.product.material ?: "bền bỉ"} và hiện đang còn sẵn size $availableSizes. Bạn có thể bấm vào thẻ sản phẩm bên dưới để xem chi tiết nhé!"
        }

        // 3. TRƯỜNG HỢP KHÁCH HỎI SẢN PHẨM ĐẮT NHẤT / CAO CẤP NHẤT / BÁN CHẠY NHẤT
        if (query.contains("đắt nhất") || query.contains("cao nhất") || query.contains("cao cấp")) {
            val mostExpensive = activeCatalog.maxByOrNull { it.product.salePrice ?: it.product.basePrice }!!
            val price = mostExpensive.product.salePrice ?: mostExpensive.product.basePrice
            recommendedIds.add(mostExpensive.product.productId)
            reasonsMap[mostExpensive.product.productId] = "Cao cấp • ${formatVnd(price)}"
            explanationsMap[mostExpensive.product.productId] = buildDefaultExplanationsForProduct(mostExpensive.product, catalog)
            questionsList.addAll(listOf("Vì sao đôi này có giá cao hơn?", "Còn những size nào?", "So sánh với Ultraboost 5"))
            return "Mẫu giày cao cấp có mức giá cao nhất hiện tại là **${mostExpensive.product.name}** với giá **${formatVnd(price)}**. Đôi này nổi bật nhờ công nghệ đệm ${mostExpensive.product.cushioning ?: "hàng đầu"}, đạt đánh giá ${mostExpensive.product.rating}/5 sao từ ${mostExpensive.product.reviewCount} khách hàng."
        }

        if (query.contains("bán chạy") || query.contains("phổ biến") || query.contains("hot")) {
            val bestSeller = activeCatalog.maxByOrNull { it.product.totalSoldCount }!!
            val price = bestSeller.product.salePrice ?: bestSeller.product.basePrice
            recommendedIds.add(bestSeller.product.productId)
            reasonsMap[bestSeller.product.productId] = "Đã bán ${bestSeller.product.totalSoldCount} đôi"
            explanationsMap[bestSeller.product.productId] = listOf(
                " Dẫn đầu doanh số tại SoleSense với ${bestSeller.product.totalSoldCount} đôi đã bán.",
                "Đánh giá thực tế đạt ${bestSeller.product.rating}/5 sao (${bestSeller.product.reviewCount} lượt đánh giá).",
                "Giá bán ưu đãi hiện tại: ${formatVnd(price)}."
            )
            questionsList.addAll(listOf("Đôi này còn size 40, 41 không?", "So sánh với Nike Pegasus 41"))
            return "Mẫu giày bán chạy nhất tại SoleSense hiện nay là **${bestSeller.product.name}** với tổng cộng **${bestSeller.product.totalSoldCount} đôi đã bán** (đạt ${bestSeller.product.rating}/5 sao). Giá hiện tại là ${formatVnd(price)}, rất phù hợp cho ${bestSeller.product.purpose.lowercase()}."
        }

        // 4. TRƯỜNG HỢP KHÁCH HỎI SO SÁNH HOẶC ĐÔI NÀO ÊM HƠN
        if (query.contains("so sánh") || query.contains("êm hơn") || query.contains("khác nhau") || query.contains("chọn đôi nào")) {
            activeCatalog.take(2).forEach { item ->
                val p = item.product
                recommendedIds.add(p.productId)
                reasonsMap[p.productId] = if (p.name.contains("Ultraboost", true)) "Đệm Boost siêu êm" else "Nhẹ & Bật nảy tốt"
                explanationsMap[p.productId] = buildDefaultExplanationsForProduct(p, catalog)
            }
            questionsList.addAll(
                listOf(
                    "Chân 25.5cm đi size mấy?",
                    "Kiểm tra tồn kho size 41",
                    "Đôi nào bền hơn khi chạy nhựa?"
                )
            )
            return "Để bạn dễ dàng lựa chọn, mình so sánh trực tiếp dựa trên thông số kỹ thuật thực tế:\n\n" +
                "1. **Giày Ultraboost 5 (2.450.000đ)**:\n" +
                "   • **Độ êm**: Sử dụng đệm Light BOOST V2 dày và mềm hơn, hấp thụ chấn động cực tốt, đi bộ cả ngày hoặc chạy phục hồi rất êm chân.\n" +
                "   • **Giá bán**: Rẻ hơn 750.000đ so với Pegasus 41.\n\n" +
                "2. **Nike Pegasus 41 (3.200.000đ)**:\n" +
                "   • **Độ nảy & Tốc độ**: Kết hợp đệm ReactX + 2 túi khí Air Zoom ở mũi và gót, giúp hoàn trả năng lượng nhanh, chạy tốc độ tốt hơn và form ôm gọn hơn.\n\n" +
                "👉 **Kết luận**: Nếu bạn ưu tiên **êm ái tối đa & tiết kiệm ngân sách** thì chọn **Ultraboost 5**. Nếu thích **chạy tốc độ, bật nảy cao** thì chọn **Pegasus 41** nhé!"
        }

        // 5. TRƯỜNG HỢP KHÁCH HỎI THEO CHIỀU DÀI CHÂN (CM) HOẶC SIZE CỤ THỂ
        val footLengthRegex = Regex("""(\d{2}[.,]\d|\d{2})\s*cm""")
        val sizeRegex = Regex("""size\s*(\d{2})""")
        val footMatch = footLengthRegex.find(query)
        val sizeMatch = sizeRegex.find(query)

        if (footMatch != null || sizeMatch != null || query.contains("chân dài") || query.contains("mang size") || query.contains("chọn size")) {
            val targetSize = when {
                sizeMatch != null -> sizeMatch.groupValues[1]
                footMatch != null -> {
                    val cm = footMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 25.5
                    when {
                        cm <= 25.0 -> "40"
                        cm <= 25.6 -> "41"
                        cm <= 26.3 -> "42"
                        else -> "43"
                    }
                }
                else -> "41"
            }

            // Kiểm tra từng sản phẩm trong kho xem đôi nào còn targetSize
            val matchingSizeItems = activeCatalog.filter { item ->
                item.variants.any { v -> v.size == targetSize && v.stock > 0 && v.status == "ACTIVE" }
            }

            if (matchingSizeItems.isEmpty()) {
                // Không có đôi nào còn size này -> Nới lỏng điều kiện & gợi ý size lân cận
                val availableSizesAll = activeCatalog.flatMap { it.variants }
                    .filter { it.stock > 0 && it.status == "ACTIVE" }
                    .map { it.size }.distinct().sorted()
                questionsList.addAll(availableSizesAll.map { "Xem các mẫu còn size $it" })
                return "Hiện tại các mẫu giày trong kho đều đang tạm hết **Size $targetSize**. Các size đang còn sẵn hàng ngay lúc này là: **Size ${availableSizesAll.joinToString(", ")}**. Bạn có muốn xem thử các mẫu ở size lân cận hoặc nhờ nhân viên kiểm tra lịch nhập hàng mới không?"
            }

            val sizeBreakdown = StringBuilder()
            matchingSizeItems.forEach { item ->
                val p = item.product
                val variantInSize = item.variants.first { it.size == targetSize && it.stock > 0 && it.status == "ACTIVE" }
                recommendedIds.add(p.productId)
                val stockBadge = if (variantInSize.stock == 1) "Size $targetSize còn 1 đôi!" else "Còn size $targetSize (${variantInSize.stock} đôi)"
                reasonsMap[p.productId] = stockBadge
                explanationsMap[p.productId] = listOf(
                    "Phiên bản màu ${variantInSize.color} - Size $targetSize đang còn ${variantInSize.stock} đôi trong kho.",
                    "Giá bán của size $targetSize: ${formatVnd(variantInSize.price)}.",
                    "Công nghệ đệm ${p.cushioning ?: "êm ái"}, phù hợp ${p.purpose.lowercase()}."
                )
                sizeBreakdown.appendLine("• **${p.name}** (${formatVnd(variantInSize.price)}): Màu ${variantInSize.color} đang còn **${variantInSize.stock} đôi** size $targetSize.")
            }

            questionsList.addAll(
                listOf(
                    "Đôi nào êm hơn?",
                    "So sánh hai đôi này",
                    "Chân bè có cần tăng 1 size không?"
                )
            )

            val cmIntro = if (footMatch != null) {
                "Với chiều dài bàn chân **${footMatch.groupValues[1]} cm**, kích cỡ chuẩn phù hợp nhất với bạn là **Size $targetSize EU** (nếu bàn chân bè ngang hoặc thích mang tất dày, bạn có thể cân nhắc nhích lên 0.5 - 1 size)."
            } else {
                "Mình đã kiểm tra tồn kho trực tiếp cho **Size $targetSize EU** tại SoleSense:"
            }

            return "$cmIntro\n\nHiện tại có ${matchingSizeItems.size} mẫu đang còn sẵn Size $targetSize:\n$sizeBreakdown\nBạn thích cảm giác êm mềm tối đa hay nhẹ và bật nảy để mình tư vấn sâu hơn nhé!"
        }

        // 6. TRƯỜNG HỢP KHÁCH HỎI THEO NGÂN SÁCH (VD: "tầm 3 triệu", "dưới 2 triệu", "khoảng 2tr5")
        val budgetVnd = extractBudgetFromQuery(query)
        if (budgetVnd != null) {
            val withinBudget = activeCatalog.filter { (it.product.salePrice ?: it.product.basePrice) <= budgetVnd }
            val slightlyAbove = activeCatalog.filter {
                val p = it.product.salePrice ?: it.product.basePrice
                p > budgetVnd && p <= budgetVnd + 500_000.0
            }

            if (withinBudget.isEmpty()) {
                // Không có sản phẩm thỏa mãn ngân sách -> Áp dụng quy tắc Nới lỏng điều kiện số 1 (Giá)
                val minProduct = activeCatalog.minByOrNull { it.product.salePrice ?: it.product.basePrice }!!
                val minPrice = minProduct.product.salePrice ?: minProduct.product.basePrice
                questionsList.addAll(
                    listOf(
                        "Xem mẫu ${minProduct.product.name} (${formatVnd(minPrice)})",
                        "Có mã giảm giá Voucher nào không?",
                        "Gặp nhân viên tư vấn thêm"
                    )
                )
                return "Hiện tại mình chưa tìm được mẫu giày nào nằm dưới mức ngân sách **${formatVnd(budgetVnd)}** của bạn (mức giá thấp nhất tại cửa hàng hiện là **${formatVnd(minPrice)}** cho mẫu **${minProduct.product.name}**).\n\nBạn có muốn nới ngân sách lên khoảng **${formatVnd(minPrice)}** (hoặc áp dụng thêm voucher giảm giá `SOLE200` giảm 200.000đ) để xem mẫu này không?"
            }

            val sb = StringBuilder()
            sb.appendLine("Trong tầm ngân sách **${formatVnd(budgetVnd)}**, mình lọc được mẫu giày phù hợp nhất với tiêu chí của bạn:")
            withinBudget.forEach { item ->
                val p = item.product
                val price = p.salePrice ?: p.basePrice
                recommendedIds.add(p.productId)
                reasonsMap[p.productId] = "Phù hợp ngân sách"
                explanationsMap[p.productId] = listOf(
                    "Giá ưu đãi ${formatVnd(price)} nằm gọn trong ngân sách ${formatVnd(budgetVnd)} của bạn.",
                    "Trang bị đệm ${p.cushioning ?: "êm ái"}, chuyên dụng cho ${p.purpose.lowercase()}.",
                    "Đánh giá cao ${p.rating}/5 sao với ${p.totalSoldCount} đôi đã bán."
                )
                sb.appendLine("• **${p.name}** – Giá: **${formatVnd(price)}** (tiết kiệm ${formatVnd(budgetVnd - price)} so với ngân sách). Đệm ${p.cushioning}, rất êm để chạy bộ và đi hàng ngày.")
            }

            if (slightlyAbove.isNotEmpty()) {
                val extra = slightlyAbove.first().product
                val extraPrice = extra.salePrice ?: extra.basePrice
                recommendedIds.add(extra.productId)
                reasonsMap[extra.productId] = "Nhỉnh hơn ${formatVnd(extraPrice - budgetVnd)}"
                explanationsMap[extra.productId] = listOf(
                    "Giá ${formatVnd(extraPrice)} (chỉ nhỉnh hơn ngân sách ${formatVnd(extraPrice - budgetVnd)}, có thể dùng mã giảm giá SOLE200/SOLE10).",
                    "Công nghệ ${extra.cushioning ?: "cao cấp"} hỗ trợ chạy tốc độ vượt trội.",
                    "Đánh giá ${extra.rating}/5 sao."
                )
                sb.appendLine("• Ngoài ra, nếu bạn nới nhẹ ngân sách thêm **${formatVnd(extraPrice - budgetVnd)}** (hoặc áp mã giảm giá khi thanh toán), mẫu **${extra.name} (${formatVnd(extraPrice)})** cũng là lựa chọn cực kỳ đáng cân nhắc!")
            }

            questionsList.addAll(
                listOf(
                    "Đôi nào êm hơn?",
                    "So sánh hai đôi này",
                    "Chân dài 25.5cm đi size mấy?"
                )
            )
            return sb.toString().trim()
        }

        // 7. TRƯỜNG HỢP KHÁCH HỎI VOUCHER / MÃ GIẢM GIÁ / CHÍNH SÁCH ĐỔI TRẢ
        if (query.contains("voucher") || query.contains("giảm giá") || query.contains("khuyến mãi") || query.contains("đổi trả") || query.contains("bảo hành")) {
            questionsList.addAll(
                listOf(
                    "Tư vấn giày chạy bộ tầm 3 triệu",
                    "Sản phẩm nào rẻ nhất?",
                    "Gặp nhân viên hỗ trợ"
                )
            )
            return "Tại SoleSense hiện đang có các ưu đãi và chính sách hỗ trợ khách hàng như sau:\n" +
                "• **Mã SOLE200**: Giảm ngay 200.000đ cho đơn hàng từ 1.000.000đ.\n" +
                "• **Mã SOLE10**: Giảm 10% (tối đa 300.000đ) cho đơn từ 2.000.000đ.\n" +
                "• **Chính sách đổi trả**: Hỗ trợ đổi size trong vòng 7 ngày nếu giày còn nguyên tem mác và chưa qua sử dụng.\n" +
                "Bạn đang ngắm mẫu giày nào để mình tính giá sau khi áp mã giúp bạn nhé?"
        }

        // 8. TRƯỜNG HỢP CHÀO HỎI HOẶC CÂU HỎI CHUNG VỀ GIÀY CHẠY BỘ / SNEAKER
        activeCatalog.forEach { item ->
            val p = item.product
            val price = p.salePrice ?: p.basePrice
            recommendedIds.add(p.productId)
            reasonsMap[p.productId] = "Đánh giá ${p.rating}★ • ${formatVnd(price)}"
            explanationsMap[p.productId] = buildDefaultExplanationsForProduct(p, catalog)
        }
        questionsList.addAll(
            listOf(
                "Sản phẩm nào đang có giá rẻ nhất?",
                "Tư vấn giày chạy bộ tầm 3 triệu",
                "Chân dài 25.5cm mang size mấy?",
                "So sánh Ultraboost 5 và Pegasus 41"
            )
        )

        val summaryLines = activeCatalog.joinToString("\n") { item ->
            val p = item.product
            val price = p.salePrice ?: p.basePrice
            "• **${p.name}** (${formatVnd(price)}): ${p.purpose}, trang bị đệm ${p.cushioning ?: "cao cấp"}."
        }
        return "Hiện tại SoleSense đang có sẵn các dòng giày thể thao chính hãng nổi bật sau:\n$summaryLines\n\nBạn đang ưu tiên mức ngân sách khoảng bao nhiêu, hoặc muốn tìm hiểu kỹ hơn về đôi nào để mình tư vấn chi tiết nhé?"
    }

    private fun extractBudgetFromQuery(query: String): Double? {
        // Bắt các mẫu như: "3 triệu", "2.5 triệu", "2,5 triệu", "2tr5", "3tr"
        val regexTrieu = Regex("""(\d+(?:[.,]\d+)?)\s*(?:triệu|tr\b)""")
        val match1 = regexTrieu.find(query)
        if (match1 != null) {
            val num = match1.groupValues[1].replace(',', '.').toDoubleOrNull()
            if (num != null) return num * 1_000_000.0
        }
        val regexTrLe = Regex("""(\d+)\s*tr\s*(\d+)""")
        val match2 = regexTrLe.find(query)
        if (match2 != null) {
            val main = match2.groupValues[1].toDoubleOrNull() ?: return null
            val sub = match2.groupValues[2].toDoubleOrNull() ?: 0.0
            return main * 1_000_000.0 + sub * 100_000.0
        }
        return null
    }

    private fun buildDefaultExplanationsForProduct(
        product: Product,
        catalog: List<ProductWithVariants>
    ): List<String> {
        val price = product.salePrice ?: product.basePrice
        val variants = catalog.find { it.product.productId == product.productId }?.variants ?: emptyList()
        val activeSizes = variants.filter { it.stock > 0 && it.status == "ACTIVE" }.map { it.size }.distinct()
        val sizeLine = if (activeSizes.isNotEmpty()) {
            "Đang còn sẵn các size: ${activeSizes.joinToString(", ")}."
        } else {
            "Có lựa chọn size cần kiểm tra theo bảng size của hãng."
        }
        return listOf(
            "Giá ưu đãi hiện tại: ${formatVnd(price)} (giá gốc ${formatVnd(product.basePrice)}).",
            "Công nghệ đệm ${product.cushioning ?: "êm ái"}, phù hợp cho ${product.purpose.lowercase()}.",
            sizeLine
        )
    }

    /**
     * Thử lại một yêu cầu AI bị FAILED mà KHÔNG tạo thêm tin nhắn User mới
     */
    suspend fun retryAiResponse(
        chatId: String,
        aiMessageId: String,
        userMessage: Message,
        recentMessages: List<Message>
    ) {
        updateMessage(chatId, aiMessageId, mapOf("sendStatus" to "PROCESSING", "content" to ""))

        try {
            val catalog = fetchCatalogWithVariants()
            val allProducts = catalog.map { it.product }
            val systemInstructionText = buildSystemInstruction(catalog)
            val userPromptText = buildConversationPrompt(
                userMessage = userMessage,
                recentMessages = recentMessages.filter { it.messageId != aiMessageId }.takeLast(12),
                verifiedProductContextNote = ""
            )

            var parsedReply = ""
            val recommendedIds = mutableListOf<String>()
            val reasonsMap = mutableMapOf<String, String>()
            val explanationsMap = mutableMapOf<String, List<String>>()
            val questionsList = mutableListOf<String>()

            val rawGeminiOutput = callGeminiWithFallbackModels(systemInstructionText, userPromptText)
            if (!rawGeminiOutput.isNullOrBlank()) {
                val ok = parseStructuredAiOutput(
                    rawOutput = rawGeminiOutput,
                    validProducts = allProducts,
                    catalog = catalog,
                    outReply = { parsedReply = it },
                    recommendedIds = recommendedIds,
                    reasonsMap = reasonsMap,
                    explanationsMap = explanationsMap,
                    questionsList = questionsList
                )
                if (!ok || parsedReply.isBlank()) {
                    parsedReply = rawGeminiOutput.trim()
                }
            }

            if (parsedReply.isBlank()) {
                parsedReply = generateSmartCatalogResponse(
                    userMessage = userMessage,
                    catalog = catalog,
                    verifiedProductContextNote = "",
                    recommendedIds = recommendedIds,
                    reasonsMap = reasonsMap,
                    explanationsMap = explanationsMap,
                    questionsList = questionsList
                )
            }

            updateMessage(
                chatId,
                aiMessageId,
                mapOf(
                    "content" to parsedReply,
                    "sendStatus" to "COMPLETED",
                    "recommendedProductIds" to recommendedIds.distinct(),
                    "recommendationReasons" to reasonsMap,
                    "recommendationExplanations" to explanationsMap,
                    "suggestedQuestions" to questionsList
                )
            )
        } catch (e: Exception) {
            updateMessage(
                chatId,
                aiMessageId,
                mapOf(
                    "content" to "Rất tiếc, không thể kết nối lại. Vui lòng thử lại sau.",
                    "sendStatus" to "FAILED"
                )
            )
        }
    }

    /**
     * Hủy mọi tin nhắn AI đang ở trạng thái PROCESSING khi khách chuyển sang Nhân viên
     */
    suspend fun cancelPendingAiMessages(chatId: String) {
        try {
            val snap = db.collection("chats").document(chatId)
                .collection("messages")
                .whereEqualTo("sendStatus", "PROCESSING")
                .get()
                .await()
            for (doc in snap.documents) {
                doc.reference.update("sendStatus", "CANCELLED").await()
            }
        } catch (_: Exception) {}
    }

    /**
     * Khách hàng yêu cầu chuyển sang Nhân viên hỗ trợ (Tạo supportRequests và đưa vào hàng chờ WAITING).
     * Chống tạo trùng yêu cầu nếu đã đang ở trạng thái WAITING.
     */
    suspend fun requestHumanSupport(
        chatId: String,
        userId: String,
        customerName: String,
        customerEmail: String,
        previewMessage: String
    ): Result<String> {
        return try {
            val chatRef = db.collection("chats").document(chatId)
            val chatSnap = chatRef.get().await()
            val currentMode = chatSnap.getString("mode") ?: "AI"
            val currentStatus = chatSnap.getString("status") ?: "ACTIVE"
            val existingReqId = chatSnap.getString("currentRequestId")

            // Nếu đã đang chờ nhân viên rồi thì không tạo thêm yêu cầu trùng lặp
            if (currentMode == "HUMAN" && currentStatus == "WAITING" && !existingReqId.isNullOrEmpty()) {
                return Result.success(existingReqId)
            }

            // Hủy các tin nhắn AI đang xử lý dở (nếu có)
            cancelPendingAiMessages(chatId)

            val requestId = UUID.randomUUID().toString()
            val request = SupportRequest(
                requestId = requestId,
                chatId = chatId,
                userId = userId,
                customerName = customerName.ifBlank { "Khách hàng" },
                customerEmail = customerEmail,
                status = "WAITING",
                topic = "Tư vấn sản phẩm & Hỗ trợ khách hàng",
                previewMessage = previewMessage.ifBlank { "Khách hàng yêu cầu gặp nhân viên tư vấn" },
                createdAt = Timestamp.now()
            )

            // Lưu supportRequest
            db.collection("supportRequests").document(requestId).set(request).await()

            // Cập nhật trạng thái Chat sang HUMAN + WAITING
            chatRef.update(
                mapOf(
                    "mode" to "HUMAN",
                    "status" to "WAITING",
                    "currentRequestId" to requestId,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            // Ghi tin nhắn hệ thống thông báo
            val systemMsg = Message(
                messageId = UUID.randomUUID().toString(),
                senderType = "SYSTEM",
                content = "Yêu cầu của bạn đã được chuyển đến bộ phận hỗ trợ khách hàng. Vui lòng đợi nhân viên tiếp nhận...",
                messageType = "SYSTEM",
                eventCode = "HANDOFF_REQUESTED",
                sendStatus = "COMPLETED",
                createdAt = Timestamp.now()
            )
            sendMessage(chatId, systemMsg)

            Result.success(requestId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Khách hàng hủy yêu cầu gặp nhân viên khi đang chờ (WAITING) -> Quay lại AI
     */
    suspend fun cancelSupportRequestAndReturnToAI(chatId: String, requestId: String?): Result<Unit> {
        return try {
            if (!requestId.isNullOrEmpty()) {
                try {
                    db.collection("supportRequests").document(requestId).update(
                        mapOf(
                            "status" to "CANCELLED",
                            "closedAt" to Timestamp.now()
                        )
                    ).await()
                } catch (_: Exception) {}
            }

            db.collection("chats").document(chatId).update(
                mapOf(
                    "mode" to "AI",
                    "status" to "ACTIVE",
                    "assignedStaffId" to null,
                    "assignedStaffName" to null,
                    "currentRequestId" to null,
                    "isStaffTyping" to false,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            val systemMsg = Message(
                messageId = UUID.randomUUID().toString(),
                senderType = "SYSTEM",
                content = "Đã hủy yêu cầu gặp nhân viên. SoleSense AI tiếp tục hỗ trợ bạn.",
                messageType = "SYSTEM",
                eventCode = "RETURNED_TO_AI",
                sendStatus = "COMPLETED",
                createdAt = Timestamp.now()
            )
            sendMessage(chatId, systemMsg)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Khách hàng quay lại AI khi nhân viên đã tiếp nhận (ASSIGNED)
     */
    suspend fun returnToAIFromAssigned(chatId: String, requestId: String?): Result<Unit> {
        return try {
            if (!requestId.isNullOrEmpty()) {
                try {
                    db.collection("supportRequests").document(requestId).update(
                        mapOf(
                            "status" to "RESOLVED",
                            "resolvedAt" to Timestamp.now()
                        )
                    ).await()
                } catch (_: Exception) {}
            }

            db.collection("chats").document(chatId).update(
                mapOf(
                    "mode" to "AI",
                    "status" to "ACTIVE",
                    "assignedStaffId" to null,
                    "assignedStaffName" to null,
                    "currentRequestId" to null,
                    "isStaffTyping" to false,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            val systemMsg = Message(
                messageId = UUID.randomUUID().toString(),
                senderType = "SYSTEM",
                content = "Cuộc trò chuyện với nhân viên đã tạm kết thúc. SoleSense AI tiếp tục tư vấn cho bạn.",
                messageType = "SYSTEM",
                eventCode = "RETURNED_TO_AI",
                sendStatus = "COMPLETED",
                createdAt = Timestamp.now()
            )
            sendMessage(chatId, systemMsg)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Nhân viên nhận yêu cầu hỗ trợ (Sử dụng Transaction để bảo đảm chống race condition)
     */
    suspend fun staffAssignTicketTransaction(
        requestId: String,
        staffId: String,
        staffName: String
    ): Result<Unit> {
        return try {
            val reqRef = db.collection("supportRequests").document(requestId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(reqRef)
                val currentStatus = snapshot.getString("status") ?: ""
                val currentAssigned = snapshot.getString("assignedStaffId")
                val chatId = snapshot.getString("chatId") ?: ""

                if (currentStatus != "WAITING" || !currentAssigned.isNullOrEmpty()) {
                    throw IllegalStateException("Yêu cầu này đã được tiếp nhận bởi nhân viên khác hoặc đã hủy!")
                }

                transaction.update(
                    reqRef,
                    mapOf(
                        "status" to "ASSIGNED",
                        "assignedStaffId" to staffId,
                        "assignedStaffName" to staffName,
                        "assignedAt" to Timestamp.now(),
                        "lastStaffActivityAt" to Timestamp.now()
                    )
                )

                if (chatId.isNotEmpty()) {
                    val chatRef = db.collection("chats").document(chatId)
                    transaction.update(
                        chatRef,
                        mapOf(
                            "mode" to "HUMAN",
                            "status" to "ASSIGNED",
                            "assignedStaffId" to staffId,
                            "assignedStaffName" to staffName,
                            "updatedAt" to Timestamp.now()
                        )
                    )
                }
            }.await()

            val ticketDoc = reqRef.get().await()
            val chatId = ticketDoc.getString("chatId") ?: ""
            if (chatId.isNotEmpty()) {
                val sysMsg = Message(
                    messageId = UUID.randomUUID().toString(),
                    senderType = "SYSTEM",
                    content = "Nhân viên $staffName đã tiếp nhận hỗ trợ và sẽ trao đổi cùng bạn.",
                    messageType = "SYSTEM",
                    eventCode = "HANDOFF_ACCEPTED",
                    sendStatus = "COMPLETED",
                    createdAt = Timestamp.now()
                )
                sendMessage(chatId, sysMsg)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Nhân viên hoàn thành và đóng phiên hỗ trợ
     */
    suspend fun staffResolveTicket(requestId: String, chatId: String): Result<Unit> {
        return try {
            if (requestId.isNotEmpty()) {
                try {
                    db.collection("supportRequests").document(requestId).update(
                        mapOf(
                            "status" to "RESOLVED",
                            "resolvedAt" to Timestamp.now(),
                            "closedAt" to Timestamp.now()
                        )
                    ).await()
                } catch (_: Exception) {}
            }

            db.collection("chats").document(chatId).update(
                mapOf(
                    "mode" to "AI",
                    "status" to "ACTIVE",
                    "assignedStaffId" to null,
                    "assignedStaffName" to null,
                    "currentRequestId" to null,
                    "isStaffTyping" to false,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            val sysMsg = Message(
                messageId = UUID.randomUUID().toString(),
                senderType = "SYSTEM",
                content = "Nhân viên đã hoàn tất hỗ trợ. Bạn có thể đánh giá phiên hỗ trợ hoặc tiếp tục hỏi SoleSense AI.",
                messageType = "SYSTEM",
                eventCode = "RESOLVED",
                sendStatus = "COMPLETED",
                createdAt = Timestamp.now()
            )
            sendMessage(chatId, sysMsg)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cập nhật trạng thái đang gõ phím
     */
    suspend fun setTypingStatus(chatId: String, isStaff: Boolean, isTyping: Boolean) {
        val field = if (isStaff) "isStaffTyping" else "isUserTyping"
        try {
            db.collection("chats").document(chatId).update(field, isTyping).await()
        } catch (_: Exception) {}
    }

    /**
     * Lắng nghe danh sách ticket đang chờ (WAITING) cho SupportDashboardScreen
     */
    fun getWaitingSupportRequestsFlow(): Flow<List<SupportRequest>> = callbackFlow {
        val listener = db.collection("supportRequests")
            .whereEqualTo("status", "WAITING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(SupportRequest::class.java)
                    ?.sortedByDescending { it.createdAt.seconds }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Lắng nghe danh sách ticket đang được nhân viên phụ trách (ASSIGNED) để nhân viên có thể mở lại chat bất kỳ lúc nào
     */
    fun getAssignedSupportRequestsFlow(staffId: String): Flow<List<SupportRequest>> = callbackFlow {
        val listener = db.collection("supportRequests")
            .whereEqualTo("status", "ASSIGNED")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(SupportRequest::class.java)
                    ?.filter { staffId.isEmpty() || it.assignedStaffId == staffId }
                    ?.sortedByDescending { it.assignedAt?.seconds ?: it.createdAt.seconds }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    private fun formatVnd(amount: Double): String {
        return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(amount.toLong()) + "đ"
    }
}
