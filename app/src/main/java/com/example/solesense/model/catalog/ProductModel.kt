package com.example.solesense.model.catalog

enum class ProductBadge {
    NONE,
    NEW,
    SALE,
    BESTSELLER
}


data class ProductModel(
    val id: String,
    val name: String,
    val imageUrl: String,
    val rating: Double,
    val soldCount: String,     //  định dạng "1.2k"
    val originalPrice: Long?,  // Giá gốc (null nếu không có giảm giá)
    val currentPrice: Long,    // Giá phải trả hiện tại
    val badge: ProductBadge,   // Gắn với Enum ở trên
    val isOutOfStock: Boolean
)