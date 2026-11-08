package com.example.solesense.ui.product

import com.example.solesense.model.catalog.Product
import com.example.solesense.model.catalog.Variant

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    // Dữ liệu gốc từ Firestore
    val product: Product? = null,
    val allVariants: List<Variant> = emptyList(),

    // Trạng thái Hiển thị (UI State) - Tính toán từ ViewModel
    val displayPrice: Double = 0.0,
    val displayMinPrice: Double? = null,
    val displayMaxPrice: Double? = null,
    val displayOriginalPrice: Double? = null,
    val discountPercent: Int? = null,

    // Quản lý biến thể (Logic cốt lõi)
    val availableColors: List<ColorOptionUiModel> = emptyList(),
    val selectedColor: String? = null,
    val selectedColorImages: List<String> = emptyList(),
    val selectedImageIndex: Int = 0,
    val imageScrollTrigger: Int = 0,

    val availableSizesForSelectedColor: List<SizeOptionUiModel> = emptyList(),
    val selectedSize: String? = null,
    val selectedQuantity: Int = 1,

    // BottomSheet trạng thái
    val isBottomSheetVisible: Boolean = false,
    val bottomSheetActionType: BottomSheetActionType = BottomSheetActionType.BUY_NOW,
    val isChatBottomSheetVisible: Boolean = false,

    // Biến thể cuối cùng được chọn (Sẵn sàng thêm vào giỏ)
    val selectedVariant: Variant? = null,

    // Các trạng thái khác
    val isFavorite: Boolean = false,
    val cartItemCount: Int = 0,

    // Dữ liệu phụ trợ (Có thể fetch sau để tối ưu tốc độ load trang)
    // val reviews: List<Review> = emptyList(),
    // val averageRating: Double = 0.0,
    // val relatedProducts: List<Product> = emptyList()
)

enum class BottomSheetActionType {
    ADD_TO_CART,
    BUY_NOW
}

data class ColorOptionUiModel(
    val colorName: String,
    val thumbnailUrl: String,
    val isSelected: Boolean
)

data class SizeOptionUiModel(
    val sizeName: String,
    val variantId: String,
    val stockQuantity: Int, // Giữ lại số lượng tồn để xử lý logic UI (mờ đi nếu = 0)
    val isAvailable: Boolean = stockQuantity > 0,
    val isSelected: Boolean
)
