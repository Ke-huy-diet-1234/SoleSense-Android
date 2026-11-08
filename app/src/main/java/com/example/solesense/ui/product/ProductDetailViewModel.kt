package com.example.solesense.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solesense.data.repository.ProductRepository
import com.example.solesense.model.catalog.Product
import com.example.solesense.model.catalog.Variant
import com.example.solesense.model.order.OrderItem
import com.example.solesense.repository.FavoriteRepository
import com.example.solesense.ui.checkout.CheckoutSessionHolder
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

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        val productId = savedStateHandle.get<String>("productId") ?: ""
        if (productId.isNotBlank()) {
            fetchProductDetails(productId)
        } else {
            loadMockData()
        }
    }

    private fun loadMockData() {
        // 1. Tạo 1 object Product giả
        val mockProduct = Product(
            productId = "p_001",
            name = "Air Jordan 3 Retro",
            description = "Giày thể thao Retro 3 được thiết kế dành cho những ai yêu thích sự năng động. Form giày gọn gàng, phối màu tinh tế.",
            basePrice = 4900000.0,
            salePrice = 2450000.0,
            brandId = "nike",
            totalSoldCount  = 1200
        )

        // 2. Tạo danh sách các biến thể (Variant) giả
        // Thay link ảnh bằng link thật từ internet hoặc Wikipedia bạn đã lấy
        val imgDen = "https://upload.wikimedia.org/wikipedia/commons/a/a6/Logo_NIKE.svg" // Tạm dùng logo để test ảnh
        val imgTrang = "https://upload.wikimedia.org/wikipedia/commons/2/20/Adidas_Logo.svg"

        val mockVariants = listOf(
            Variant(variantId = "v2", sku = "p_001", color = "Đen", size = "40", stock = 5, price = 2450000.0, imageUrls = listOf(imgDen)),
            Variant(variantId = "v3", sku = "p_001", color = "Đen", size = "41", stock = 0, price = 2450000.0, imageUrls = listOf(imgDen)),
            Variant(variantId = "v4", sku = "p_001", color = "Trắng", size = "40", stock = 12, price = 2500000.0, imageUrls = listOf(imgTrang)),
            Variant(variantId = "v1", sku = "p_001", color = "Đen", size = "39", stock = 10, price = 2450000.0, imageUrls = listOf(imgDen))
        )

        // Bơm vào logic phân tích ban đầu
        buildInitialState(mockProduct, mockVariants)
    }

    // Hàm gọi API lấy dữ liệu và khởi tạo trạng thái ban đầu
    private fun fetchProductDetails(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val product = repository.getProductById(productId).getOrThrow()
                val variants = repository.getVariantsByProductId(productId).getOrDefault(emptyList())
                val isFav = favoriteRepository.isFavorite(productId)

                if (variants.isNotEmpty()) {
                    // Đảm bảo nếu DB cũ đang lưu price = 0.0 thì fallback về salePrice/basePrice của Product
                    val normalizedVariants = variants.map { v ->
                        if (v.price <= 0.0) v.copy(price = product.salePrice ?: product.basePrice) else v
                    }
                    buildInitialState(product, normalizedVariants, isFav)
                } else {
                    // Nếu sản phẩm trên DB chưa có subcollection variants thì tạo biến thể mặc định từ thông tin sản phẩm
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
                // Nếu lỗi kết nối hoặc dùng ID mẫu thì fallback về dữ liệu giả để vẫn test được UI
                loadMockData()
            }
        }
    }

    // --- LOGIC NGHIỆP VỤ CỐT LÕI ---

    fun buildInitialState(
        product: Product,
        variants: List<Variant>,
        isFavorite: Boolean = false
    ) {
        // 1. Lọc ra danh sách màu độc nhất từ mảng variants
        val uniqueColors = variants.map { it.color }.distinct()
        val colorOptions = uniqueColors.map { colorName ->
            val firstVariantOfColor = variants.first { it.color == colorName }
            ColorOptionUiModel(
                colorName = colorName,
                // Lấy ảnh đầu tiên của biến thể làm thumbnail chọn màu
                thumbnailUrl = firstVariantOfColor.imageUrls.firstOrNull()
                    ?: product.imageUrls.firstOrNull()
                    ?: "",
                isSelected = false
            )
        }

        // Tổng hợp toàn bộ ảnh của sản phẩm và các biến thể để hiển thị đầy đủ trên ảnh chính
        val allGalleryImages = (product.imageUrls + variants.flatMap { it.imageUrls })
            .filter { it.isNotBlank() }
            .distinct()

        // Tính khoảng min-max từ toàn bộ biến thể
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

        // Tự động chọn màu đầu tiên nếu có
        if (colorOptions.isNotEmpty()) {
            selectColor(colorOptions.first().colorName)
        }
    }

    fun selectColor(colorName: String) {
        _uiState.update { currentState ->
            val variantsForColor = currentState.allVariants.filter { it.color == colorName }

            // 1. Cập nhật lại UI Model cho Màu (đổi trạng thái isSelected)
            val updatedColors = currentState.availableColors.map {
                it.copy(isSelected = it.colorName == colorName)
            }

            // 2. Tạo danh sách Size tương ứng với Màu vừa chọn
            val sizesForColor = variantsForColor.map { variant ->
                SizeOptionUiModel(
                    sizeName = variant.size,
                    variantId = variant.variantId,
                    stockQuantity = variant.stock,
                    isSelected = false
                )
            }.sortedBy { it.sizeName.toIntOrNull() ?: 0 } // Sắp xếp size theo thứ tự tăng dần (39, 40, 41...)

            // 3. Logic giữ Size thông minh:
            // Nếu người dùng đã chọn Size 40 ở màu Đen, khi bấm sang màu Trắng, nếu màu Trắng có Size 40 -> Giữ nguyên. Nếu hết hàng -> Xóa chọn Size.
            val previousSize = currentState.selectedSize
            val isPreviousSizeAvailable = sizesForColor.any { it.sizeName == previousSize && it.isAvailable }
            val newSelectedSize = if (isPreviousSizeAvailable) previousSize else null

            // Đánh dấu isSelected cho danh sách Size mới
            val updatedSizes = sizesForColor.map { it.copy(isSelected = it.sizeName == newSelectedSize) }

            // 4. Tìm vị trí ảnh tương ứng với màu vừa chọn trong danh sách tất cả ảnh chính
            val colorMainImage = variantsForColor.firstOrNull()?.imageUrls?.firstOrNull()
            val targetImageIndex = if (colorMainImage != null) {
                currentState.selectedColorImages.indexOf(colorMainImage).coerceAtLeast(0)
            } else {
                0
            }

            // 5. Tìm Variant cuối cùng nếu đã đủ cả Màu và Size
            val finalSelectedVariant = if (newSelectedSize != null) {
                variantsForColor.find { it.size == newSelectedSize }
            } else null

            // Tính dải giá hiển thị: nếu đã chọn biến thể cụ thể thì hiển thị giá biến thể đó
            // Nếu chưa chọn size, hiển thị dải min - max của màu này (hoặc toàn sản phẩm)
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
            // Cập nhật lại list size để highlight size được chọn
            val updatedSizes = currentState.availableSizesForSelectedColor.map {
                it.copy(isSelected = it.sizeName == sizeName)
            }

            // Tìm Variant cụ thể dựa trên Màu đang chọn và Size vừa bấm
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

    fun openChatBottomSheet() {
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
        val productId = _uiState.value.product?.productId ?: return
        // Optimistic UI update
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
        viewModelScope.launch {
            val result = favoriteRepository.toggleFavorite(productId)
            result.onSuccess { isFav ->
                _uiState.update { it.copy(isFavorite = isFav) }
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

    fun addToCart(variantId: String) {
        val variant = _uiState.value.allVariants.find { it.variantId == variantId } ?: _uiState.value.selectedVariant
        if (variant != null && variant.stock > 0) {
            val quantityToAdd = _uiState.value.selectedQuantity
            _uiState.update { currentState ->
                currentState.copy(
                    cartItemCount = currentState.cartItemCount + quantityToAdd,
                    isBottomSheetVisible = false
                )
            }
        }
    }

    private fun calculateDiscount(originalPrice: Double?, salePrice: Double?): Int? {
        if (originalPrice == null || salePrice == null || originalPrice <= 0 || salePrice >= originalPrice) return null
        return (((originalPrice - salePrice) / originalPrice) * 100).toInt()
    }
}
