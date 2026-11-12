package com.example.solesense.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solesense.data.repository.ProductRepository
import com.example.solesense.model.catalog.Product
import com.example.solesense.model.chat.Chat
import com.example.solesense.model.chat.Message
import com.example.solesense.repository.ChatRepository
import com.example.solesense.utils.UserSessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Ngữ cảnh sản phẩm khi người dùng ấn nút FAB từ màn hình chi tiết sản phẩm
 */
data class PinnedProductContext(
    val productId: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val selectedColor: String,
    val selectedSize: String,
    val isDisabled: Boolean = false
)

data class WhyRecommendedDetail(
    val product: Product,
    val mainReason: String,
    val explanations: List<String>
)

data class ChatUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val pinnedProduct: PinnedProductContext? = null,
    val isPinnedBannerVisible: Boolean = false,
    val showReturnToAiDialog: Boolean = false,
    val showSupportRatingDialog: Boolean = false,
    val whyRecommendedDetail: WhyRecommendedDetail? = null,
    val productsMap: Map<String, Product> = emptyMap(),
    val errorMessage: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val productRepository: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null
    private var chatDocJob: Job? = null
    private var aiProcessingJob: Job? = null
    private var currentChatId: String? = null
    private var lastObservedUserId: String? = null
    private var lastKnownMessageCount: Int = 0

    init {
        viewModelScope.launch {
            chatRepository.ensureCleanHistoryMigration()
        }
        loadAllProducts()
        observeUserSession()
    }

    private fun observeUserSession() {
        viewModelScope.launch {
            UserSessionManager.currentUser.collect { user ->
                val uid = user?.userId ?: UserSessionManager.getActiveUserId()
                if (uid.isNullOrEmpty()) {
                    // Khi khách đăng xuất: xóa sạch dữ liệu hội thoại của tài khoản cũ trên UI
                    messagesJob?.cancel()
                    chatDocJob?.cancel()
                    aiProcessingJob?.cancel()
                    currentChatId = null
                    lastObservedUserId = null
                    _uiState.update {
                        ChatUiState(productsMap = it.productsMap)
                    }
                } else if (uid != lastObservedUserId) {
                    lastObservedUserId = uid
                    initChat(uid)
                }
            }
        }
    }

    fun loadAllProducts() {
        viewModelScope.launch {
            val result = productRepository.getProducts(isForceRefresh = true)
            result.onSuccess { list ->
                _uiState.update {
                    it.copy(
                        productsMap = list
                            .filter { p -> p.status != "DISABLED" }
                            .associateBy { p -> p.productId }
                    )
                }
            }
        }
    }

    fun initChat(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val chat = chatRepository.getOrCreateChat(userId)
            currentChatId = chat.chatId
            _uiState.update { it.copy(chat = chat, isLoading = false) }

            // Lắng nghe realtime chat doc (mode, status, assignedStaff...)
            chatDocJob?.cancel()
            chatDocJob = viewModelScope.launch {
                chatRepository.getChatFlow(chat.chatId).collect { updatedChat ->
                    _uiState.update { it.copy(chat = updatedChat) }
                }
            }

            // Lắng nghe realtime danh sách tin nhắn
            messagesJob?.cancel()
            messagesJob = viewModelScope.launch {
                chatRepository.getMessagesFlow(chat.chatId).collect { msgList ->
                    val prevCount = lastKnownMessageCount
                    lastKnownMessageCount = msgList.size

                    // Nếu nhân viên vừa bấm kết thúc hỗ trợ (tin nhắn mới nhất có eventCode == RESOLVED),
                    // hiện hộp thoại mời đánh giá chất lượng hỗ trợ cho vui
                    val newestMsg = msgList.lastOrNull()
                    val shouldPromptRating = prevCount in 1 until msgList.size &&
                        newestMsg?.senderType == "SYSTEM" &&
                        newestMsg.eventCode == "RESOLVED"

                    _uiState.update {
                        it.copy(
                            messages = msgList,
                            showSupportRatingDialog = if (shouldPromptRating) true else it.showSupportRatingDialog
                        )
                    }
                }
            }
        }
    }

    /**
     * Nhận sản phẩm đang xem từ ProductDetailScreen để ghim banner trên đầu.
     * Đồng thời đối chiếu lại dữ liệu chuẩn từ Firestore (giá mới nhất, trạng thái ngừng bán).
     */
    fun setPinnedProductContext(
        productId: String,
        name: String,
        price: Double,
        imageUrl: String,
        color: String,
        size: String
    ) {
        // Hiển thị ngay trên UI để mượt mà
        _uiState.update {
            it.copy(
                pinnedProduct = PinnedProductContext(
                    productId = productId,
                    name = name,
                    price = price,
                    imageUrl = imageUrl,
                    selectedColor = color,
                    selectedSize = size
                ),
                isPinnedBannerVisible = true
            )
        }

        // Kiểm tra lại dữ liệu chuẩn mới nhất từ Firestore
        viewModelScope.launch {
            productRepository.getProductById(productId).onSuccess { freshProduct ->
                val freshPrice = freshProduct.salePrice ?: freshProduct.basePrice
                val freshImage = freshProduct.imageUrls.firstOrNull() ?: imageUrl
                val isDisabled = freshProduct.status == "DISABLED"
                _uiState.update { state ->
                    val currentPinned = state.pinnedProduct
                    if (currentPinned != null && currentPinned.productId == productId) {
                        state.copy(
                            pinnedProduct = currentPinned.copy(
                                name = freshProduct.name.ifBlank { name },
                                price = freshPrice,
                                imageUrl = freshImage,
                                isDisabled = isDisabled
                            )
                        )
                    } else state
                }
            }
        }
    }

    fun dismissPinnedBanner() {
        _uiState.update { it.copy(isPinnedBannerVisible = false) }
    }

    /**
     * Khách hàng bấm dòng chữ "Hỏi về sản phẩm này?" trên banner
     */
    fun askAboutPinnedProduct() {
        val pinned = _uiState.value.pinnedProduct ?: return
        val currentUserId = UserSessionManager.getActiveUserId() ?: ""
        val chatId = currentChatId ?: return

        dismissPinnedBanner()

        val textContent = "Mình muốn tư vấn về sản phẩm ${pinned.name} (Màu ${pinned.selectedColor} • Size ${pinned.selectedSize})"

        val userMessage = Message(
            messageId = UUID.randomUUID().toString(),
            senderType = "USER",
            senderId = currentUserId,
            senderName = UserSessionManager.currentUser.value?.fullName ?: "Khách hàng",
            content = textContent,
            messageType = "PRODUCT",
            productId = pinned.productId,
            productName = pinned.name,
            productPrice = pinned.price,
            productImage = pinned.imageUrl,
            productColor = pinned.selectedColor,
            productSize = pinned.selectedSize,
            sendStatus = "SENT"
        )

        sendMessageInternal(chatId, userMessage)
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendCurrentInput() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        _uiState.update { it.copy(inputText = "") }
        sendTextMessage(text)
    }

    fun sendTextMessage(text: String) {
        val currentUserId = UserSessionManager.getActiveUserId() ?: ""
        val chatId = currentChatId ?: return

        val userMessage = Message(
            messageId = UUID.randomUUID().toString(),
            senderType = "USER",
            senderId = currentUserId,
            senderName = UserSessionManager.currentUser.value?.fullName ?: "Khách hàng",
            content = text,
            messageType = "TEXT",
            sendStatus = "SENT"
        )

        sendMessageInternal(chatId, userMessage)
    }

    private fun sendMessageInternal(chatId: String, userMessage: Message) {
        viewModelScope.launch {
            val result = chatRepository.sendMessage(chatId, userMessage)
            if (result.isSuccess) {
                val currentMode = _uiState.value.chat?.mode ?: "AI"
                val currentStatus = _uiState.value.chat?.status ?: "ACTIVE"

                // Chỉ khi đang ở chế độ AI và trạng thái ACTIVE thì mới kích hoạt AI trả lời
                if (currentMode == "AI" && currentStatus == "ACTIVE") {
                    aiProcessingJob?.cancel()
                    aiProcessingJob = viewModelScope.launch {
                        chatRepository.processAiResponse(
                            chatId = chatId,
                            userMessage = userMessage,
                            recentMessages = _uiState.value.messages
                        )
                    }
                }
            } else {
                // Nếu gửi thất bại (mất mạng), khôi phục nội dung vào ô nhập để khách không phải gõ lại
                _uiState.update {
                    it.copy(
                        inputText = userMessage.content,
                        errorMessage = "Gửi tin nhắn thất bại, vui lòng kiểm tra kết nối mạng."
                    )
                }
            }
        }
    }

    /**
     * Thử lại AI request bị FAILED (Không tạo lại tin nhắn User)
     */
    fun retryAiRequest(aiMessage: Message) {
        val chatId = currentChatId ?: return
        val messages = _uiState.value.messages
        val aiIndex = messages.indexOfFirst { it.messageId == aiMessage.messageId }
        val userMessage = if (aiIndex > 0) {
            messages.subList(0, aiIndex).lastOrNull { it.senderType == "USER" }
                ?: Message(content = "Tư vấn sản phẩm")
        } else {
            Message(content = "Tư vấn sản phẩm")
        }

        aiProcessingJob?.cancel()
        aiProcessingJob = viewModelScope.launch {
            chatRepository.retryAiResponse(
                chatId = chatId,
                aiMessageId = aiMessage.messageId,
                userMessage = userMessage,
                recentMessages = messages
            )
        }
    }

    /**
     * Khách hàng bấm "Gặp nhân viên" trên Header
     */
    fun onHandoffToStaffClick() {
        val chatId = currentChatId ?: return
        val currentUserId = UserSessionManager.getActiveUserId() ?: ""
        val currentUser = UserSessionManager.currentUser.value
        val lastMsg = _uiState.value.messages.lastOrNull { it.senderType == "USER" }?.content
            ?: "Khách hàng cần hỗ trợ tư vấn trực tiếp"

        // Hủy ngay yêu cầu AI đang xử lý dở (nếu có) để AI không ghi đè câu trả lời sau khi chuyển nhân viên
        aiProcessingJob?.cancel()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = chatRepository.requestHumanSupport(
                chatId = chatId,
                userId = currentUserId,
                customerName = currentUser?.fullName ?: "Khách hàng",
                customerEmail = currentUser?.email ?: "",
                previewMessage = lastMsg
            )
            _uiState.update { it.copy(isLoading = false) }
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = "Không thể gửi yêu cầu hỗ trợ. Vui lòng thử lại sau.") }
            }
        }
    }

    /**
     * Khi khách đang ở hàng chờ WAITING và muốn hủy để quay lại AI
     */
    fun cancelWaitingHandoff() {
        val chatId = currentChatId ?: return
        val requestId = _uiState.value.chat?.currentRequestId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            chatRepository.cancelSupportRequestAndReturnToAI(chatId, requestId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Mở Dialog hỏi khách có muốn quay lại AI khi nhân viên đã tiếp nhận (ASSIGNED)
     */
    fun promptReturnToAi() {
        _uiState.update { it.copy(showReturnToAiDialog = true) }
    }

    fun dismissReturnToAiDialog() {
        _uiState.update { it.copy(showReturnToAiDialog = false) }
    }

    /**
     * Khách xác nhận quay lại AI từ Dialog
     */
    fun confirmReturnToAiFromAssigned() {
        val chatId = currentChatId ?: return
        val requestId = _uiState.value.chat?.currentRequestId
        _uiState.update { it.copy(showReturnToAiDialog = false) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            chatRepository.returnToAIFromAssigned(chatId, requestId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun dismissSupportRatingDialog() {
        _uiState.update { it.copy(showSupportRatingDialog = false) }
    }

    /**
     * Mở BottomSheet giải thích "Vì sao AI đề xuất?"
     */
    fun openWhyAiRecommended(product: Product, mainReason: String, explanations: List<String>) {
        _uiState.update {
            it.copy(
                whyRecommendedDetail = WhyRecommendedDetail(
                    product = product,
                    mainReason = mainReason,
                    explanations = explanations
                )
            )
        }
    }

    fun closeWhyAiRecommended() {
        _uiState.update { it.copy(whyRecommendedDetail = null) }
    }
}
