package com.example.solesense.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solesense.data.repository.ProductRepository
import com.example.solesense.model.catalog.Product
import com.example.solesense.model.catalog.Variant
import com.example.solesense.model.order.CartItem
import com.example.solesense.model.order.OrderItem
import com.example.solesense.repository.CartRepository
import com.example.solesense.repository.FavoriteRepository
import com.example.solesense.ui.checkout.CheckoutSessionHolder
import com.example.solesense.utils.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val repository = ProductRepository()
    private val favoriteRepository = FavoriteRepository()
    private val cartRepository = CartRepository()

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        val productId = savedStateHandle.get<String>("productId") ?: ""
        if (productId.isNotBlank()) {
            fetchProductDetails(productId)
        } else {
            loadMockData()
        }
        refreshCartBadge()
    }

    fun refreshCartBadge() {
        if (!UserSessionManager.isAuthenticated()) {
            _uiState.update { it.copy(cartItemCount = 0) }
            return
        }
        viewModelScope.launch {
            val count = cartRepository.getCartItemCount(UserSessionManager.getActiveUserId())
            _uiState.update { it.copy(cartItemCount = count) }
        }
    }

    private fun loadMockData() {
        val mockProduct = Product(
            productId = "p_001",
            name = "Air Jordan 3 Retro",
            description = "Giày thể thao Retro 3 được thiết kế dành cho những ai yêu thích sự năng động. Form giày gọn gàng, phối màu tinh tế.",
            basePrice = 4900000.0,
            salePrice = 2450000.0,
            brandId = "nike",
            totalSoldCount = 1200
        )

        val imgDen = "https://upload.wikimedia.org/wikipedia/commons/a/a6/Logo_NIKE.svg"
        val imgTrang = "https://upload.wikimedia.org/wikipedia/commons/2/20/Adidas_Logo.svg"

        val mockVariants = listOf(
            Variant(variantId = "v2", sku = "p_001", color = "Đen", size = "40", stock = 5, price = 2450000.0, imageUrls = listOf(imgDen)),
            Variant(variantId = "v3", sku = "p_001", color = "Đen", size = "41", stock = 0, price = 2450000.0, imageUrls = listOf(imgDen)),
            Variant(variantId = "v4", sku = "p_001", color = "Trắng", size = "40", stock = 12, price = 2500000.0, imageUrls = listOf(imgTrang)),
            Variant(variantId = "v1", sku = "p_001", color = "Đen", size = "39", stock = 10, price = 2450000.0, imageUrls = listOf(imgDen))
        )

        buildInitialState(mockProduct, mockVariants)
    }

    private fun fetchProductDetails(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val product = repository.getProductById(productId).getOrThrow()
                val variants = repository.getVariantsByProductId(productId).getOrDefault(emptyList())
                val isFav = if (UserSessionManager.isAuthenticated()) {
                    favoriteRepository.isFavorite(productId, UserSessionManager.getActiveUserId() ?: "")
                } else {
                    false
                }

                if (variants.isNotEmpty()) {
                    val normalizedVariants = variants.map { v ->
                        if (v.price <= 0.0) v.copy(price = product.salePrice ?: product.basePrice) else v
                    }
                    buildInitialState(product, normalizedVariants, isFav)
                } else {
                    val defaultImg = product.imageUrls.firstOrNull()
                        ?: "https://upload.wikimedia.org/wikipedia/commons/a/a6/Logo_NIKE.svg"
                    val baseVariantPrice = product.salePrice ?: product.basePrice
                    val fallbackVariants = listOf(
                        Variant(variantId = "${productId}_39", sku = productId, color = "Tiêu chuẩn", size = "39", stock = 10, price = baseVariantPrice, imageUrls = listOf(defaultImg)),
                        Variant(variantId = "${productId}_40", sku = productId, color = "Tiêu chuẩn", size = "40", stock = 5, price = baseVariantPrice, imageUrls = listOf(defaultImg)),
                        Variant(variantId = "${productId}_41", sku = productId, color = "Tiêu chuẩn", size = "41", stock = 8, price = baseVariantPrice, imageUrls = listOf(defaultImg))
                    )
                    buildInitialState(product, fallbackVariants, isFav)
                }
            } catch (e: Exception) {
                loadMockData()
            }
        }
    }

    fun buildInitialState(
        product: Product,
        variants: List<Variant>,
        isFavorite: Boolean = false
    ) {
        val uniqueColors = variants.map { it.color }.distinct()
        val colorOptions = uniqueColors.map { colorName ->
            val firstVariantOfColor = variants.first { it.color == colorName }
            ColorOptionUiModel(
                colorName = colorName,
                thumbnailUrl = firstVariantOfColor.imageUrls.firstOrNull()
                    ?: product.imageUrls.firstOrNull()
                    ?: "",
                isSelected = false
            )
        }

        val allGalleryImages = (product.imageUrls + variants.flatMap { it.imageUrls })
            .filter { it.isNotBlank() }
            .distinct()

        val prices = variants.map { it.price }.filter { it > 0 }
        val minP = prices.minOrNull() ?: product.salePrice ?: product.basePrice
        val maxP = prices.maxOrNull() ?: product.salePrice ?: product.basePrice

        _uiState.update {
            it.copy(
                isLoading = false,
                product = product,
                allVariants = variants,
                availableColors = colorOptions,
                selectedColorImages = allGalleryImages,
                displayMinPrice = if (minP < maxP) minP else null,
                displayMaxPrice = if (minP < maxP) maxP else null,
                displayPrice = minP,
                displayOriginalPrice = product.basePrice,
                discountPercent = calculateDiscount(product.basePrice, minP),
                isFavorite = isFavorite
            )
        }

        if (colorOptions.isNotEmpty()) {
            selectColor(colorOptions.first().colorName)
        }
    }

    fun selectColor(colorName: String) {
        _uiState.update { currentState ->
            val variantsForColor = currentState.allVariants.filter { it.color == colorName }

            val updatedColors = currentState.availableColors.map {
                it.copy(isSelected = it.colorName == colorName)
            }

            val sizesForColor = variantsForColor.map { variant ->
                SizeOptionUiModel(
                    sizeName = variant.size,
                    variantId = variant.variantId,
                    stockQuantity = variant.stock,
                    isSelected = false
                )
            }.sortedBy { it.sizeName.toIntOrNull() ?: 0 }

            val previousSize = currentState.selectedSize
            val isPreviousSizeAvailable = sizesForColor.any { it.sizeName == previousSize && it.isAvailable }
            val newSelectedSize = if (isPreviousSizeAvailable) previousSize else null

            val updatedSizes = sizesForColor.map { it.copy(isSelected = it.sizeName == newSelectedSize) }

            val colorMainImage = variantsForColor.firstOrNull()?.imageUrls?.firstOrNull()
            val targetImageIndex = if (colorMainImage != null) {
                currentState.selectedColorImages.indexOf(colorMainImage).coerceAtLeast(0)
            } else {
                0
            }

            val finalSelectedVariant = if (newSelectedSize != null) {
                variantsForColor.find { it.size == newSelectedSize }
            } else null

            val colorPrices = variantsForColor.map { it.price }.filter { it > 0 }
            val minP = colorPrices.minOrNull() ?: currentState.product?.salePrice ?: currentState.product?.basePrice ?: 0.0
            val maxP = colorPrices.maxOrNull() ?: currentState.product?.salePrice ?: currentState.product?.basePrice ?: 0.0

            val displayP = finalSelectedVariant?.price ?: minP
            val displayMin = if (finalSelectedVariant == null && minP < maxP) minP else null
            val displayMax = if (finalSelectedVariant == null && minP < maxP) maxP else null

            currentState.copy(
                selectedColor = colorName,
                availableColors = updatedColors,
                availableSizesForSelectedColor = updatedSizes,
                selectedSize = newSelectedSize,
                selectedImageIndex = targetImageIndex,
                imageScrollTrigger = currentState.imageScrollTrigger + 1,
                selectedVariant = finalSelectedVariant,
                displayPrice = displayP,
                displayMinPrice = displayMin,
                displayMaxPrice = displayMax,
                selectedQuantity = 1
            )
        }
    }

    fun selectSize(sizeName: String) {
        _uiState.update { currentState ->
            val updatedSizes = currentState.availableSizesForSelectedColor.map {
                it.copy(isSelected = it.sizeName == sizeName)
            }

            val finalSelectedVariant = currentState.allVariants.find {
                it.color == currentState.selectedColor && it.size == sizeName
            }

            val finalPrice = finalSelectedVariant?.price ?: currentState.displayPrice

            currentState.copy(
                availableSizesForSelectedColor = updatedSizes,
                selectedSize = sizeName,
                selectedVariant = finalSelectedVariant,
                displayPrice = finalPrice,
                displayMinPrice = null,
                displayMaxPrice = null,
                selectedQuantity = 1
            )
        }
    }

    fun openBottomSheet(actionType: BottomSheetActionType) {
        _uiState.update {
            it.copy(
                isBottomSheetVisible = true,
                bottomSheetActionType = actionType
            )
        }
    }

    fun closeBottomSheet() {
        _uiState.update {
            it.copy(isBottomSheetVisible = false)
        }
    }

    fun openSizeGuide() {
        _uiState.update {
            it.copy(isSizeGuideVisible = true)
        }
    }

    fun closeSizeGuide() {
        _uiState.update {
            it.copy(isSizeGuideVisible = false)
        }
    }

    fun dismissLoginRequiredDialog() {
        _uiState.update {
            it.copy(
                isLoginRequiredDialogVisible = false,
                pendingAuthAction = null
            )
        }
    }

    fun hideLoginDialogKeepPendingAction() {
        _uiState.update {
            it.copy(isLoginRequiredDialogVisible = false)
        }
    }

    fun openChatBottomSheet() {
        if (!UserSessionManager.isAuthenticated()) {
            _uiState.update {
                it.copy(
                    isLoginRequiredDialogVisible = true,
                    pendingAuthAction = PendingProductAuthAction.OpenChat
                )
            }
            return
        }
        _uiState.update {
            it.copy(isChatBottomSheetVisible = true)
        }
    }

    fun closeChatBottomSheet() {
        _uiState.update {
            it.copy(isChatBottomSheetVisible = false)
        }
    }

    fun toggleFavorite() {
        if (!UserSessionManager.isAuthenticated()) {
            _uiState.update {
                it.copy(
                    isLoginRequiredDialogVisible = true,
                    pendingAuthAction = PendingProductAuthAction.ToggleFavorite
                )
            }
            return
        }

        val productId = _uiState.value.product?.productId ?: return
        val uid = UserSessionManager.getActiveUserId() ?: return
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
        viewModelScope.launch {
            val result = favoriteRepository.toggleFavorite(productId, uid)
            result.onSuccess { isFav ->
                _uiState.update { it.copy(isFavorite = isFav) }
            }
        }
    }

    /**
     * Xử lý khi người dùng chọn màu, size, số lượng và nhấn nút Thêm vào giỏ hoặc Mua ngay.
     * Nếu chưa đăng nhập -> Hiển thị LoginRequiredDialog và lưu thao tác chờ để tự động tiếp tục sau khi đăng nhập!
     */
    fun handleVariantActionClick(
        onAddedToCart: (String) -> Unit,
        onNavigateToCheckout: (String) -> Unit
    ) {
        val state = _uiState.value
        val variant = state.selectedVariant ?: return
        val quantity = state.selectedQuantity

        if (!UserSessionManager.isAuthenticated()) {
            val pending = when (state.bottomSheetActionType) {
                BottomSheetActionType.ADD_TO_CART -> PendingProductAuthAction.AddToCart(variant.variantId, quantity)
                BottomSheetActionType.BUY_NOW -> PendingProductAuthAction.BuyNow(variant.variantId, quantity)
            }
            _uiState.update {
                it.copy(
                    isBottomSheetVisible = false,
                    isLoginRequiredDialogVisible = true,
                    pendingAuthAction = pending
                )
            }
            return
        }

        when (state.bottomSheetActionType) {
            BottomSheetActionType.ADD_TO_CART -> {
                addToCart(variant.variantId, quantity, onAddedToCart)
            }
            BottomSheetActionType.BUY_NOW -> {
                if (prepareDirectCheckout()) {
                    onNavigateToCheckout(variant.variantId)
                }
            }
        }
    }

    /**
     * Tự động tiếp tục thao tác đang chờ sau khi đăng nhập thành công và quay lại ProductDetailScreen
     */
    fun consumeAndExecutePendingAction(
        onAddedToCart: (String) -> Unit,
        onNavigateToCheckout: (String) -> Unit
    ) {
        val pending = _uiState.value.pendingAuthAction ?: return
        if (!UserSessionManager.isAuthenticated()) return

        _uiState.update { it.copy(pendingAuthAction = null, isLoginRequiredDialogVisible = false) }
        refreshCartBadge()

        when (pending) {
            is PendingProductAuthAction.AddToCart -> {
                addToCart(pending.variantId, pending.quantity) {
                    onAddedToCart("Đã đăng nhập & thêm sản phẩm vào giỏ hàng!")
                }
            }
            is PendingProductAuthAction.BuyNow -> {
                if (prepareDirectCheckout()) {
                    onNavigateToCheckout(pending.variantId)
                }
            }
            PendingProductAuthAction.ToggleFavorite -> {
                toggleFavorite()
            }
            PendingProductAuthAction.OpenChat -> {
                openChatBottomSheet()
            }
        }
    }

    fun prepareDirectCheckout(): Boolean {
        val state = _uiState.value
        val product = state.product ?: return false
        val variant = state.selectedVariant ?: return false
        val unitPrice = if (variant.price > 0) variant.price else (product.salePrice ?: product.basePrice)
        val imageSnapshot = variant.imageUrls.firstOrNull() ?: product.imageUrls.firstOrNull()

        CheckoutSessionHolder.setDirectBuyItem(
            OrderItem(
                itemId = "direct_${variant.variantId}",
                productId = product.productId,
                variantId = variant.variantId,
                skuSnapshot = variant.sku.ifBlank { variant.variantId },
                productName = product.name,
                imageSnapshot = imageSnapshot,
                size = variant.size,
                color = variant.color,
                unitPrice = unitPrice,
                quantity = state.selectedQuantity,
                subtotal = unitPrice * state.selectedQuantity
            )
        )
        closeBottomSheet()
        return true
    }

    fun increaseQuantity() {
        _uiState.update { currentState ->
            val maxStock = currentState.selectedVariant?.stock ?: 99
            if (currentState.selectedQuantity < maxStock) {
                currentState.copy(selectedQuantity = currentState.selectedQuantity + 1)
            } else {
                currentState
            }
        }
    }

    fun decreaseQuantity() {
        _uiState.update { currentState ->
            if (currentState.selectedQuantity > 1) {
                currentState.copy(selectedQuantity = currentState.selectedQuantity - 1)
            } else {
                currentState
            }
        }
    }

    fun addToCart(
        variantId: String,
        quantityToAdd: Int = _uiState.value.selectedQuantity,
        onSuccess: ((String) -> Unit)? = null
    ) {
        val state = _uiState.value
        val product = state.product ?: return
        val variant = state.allVariants.find { it.variantId == variantId } ?: state.selectedVariant
        if (variant != null && variant.stock > 0) {
            val unitPrice = if (variant.price > 0) variant.price else (product.salePrice ?: product.basePrice)
            val imageSnapshot = variant.imageUrls.firstOrNull() ?: product.imageUrls.firstOrNull()

            _uiState.update { currentState ->
                currentState.copy(
                    cartItemCount = currentState.cartItemCount + quantityToAdd,
                    isBottomSheetVisible = false
                )
            }

            // Đồng bộ lên Firestore carts/{userId}/items/{variantId} kèm giới hạn tồn kho (maxStock)
            viewModelScope.launch {
                val cartItem = CartItem(
                    itemId = variant.variantId,
                    productId = product.productId,
                    variantId = variant.variantId,
                    productName = product.name,
                    imageUrl = imageSnapshot,
                    size = variant.size,
                    color = variant.color,
                    unitPrice = unitPrice,
                    quantity = quantityToAdd.coerceAtMost(variant.stock),
                    maxStock = variant.stock
                )
                cartRepository.addOrUpdateCartItem(cartItem, UserSessionManager.getActiveUserId()).onSuccess { items ->
                    _uiState.update { it.copy(cartItemCount = items.size) }
                }
            }
            onSuccess?.invoke("Đã thêm vào giỏ hàng")
        }
    }

    private fun calculateDiscount(originalPrice: Double?, salePrice: Double?): Int? {
        if (originalPrice == null || salePrice == null || originalPrice <= 0 || salePrice >= originalPrice) return null
        return (((originalPrice - salePrice) / originalPrice) * 100).toInt()
    }
}
