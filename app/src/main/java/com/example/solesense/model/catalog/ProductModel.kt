package com.example.solesense.model.catalog

enum class ProductBadge {
    NONE,
    NEW,
    SALE,
    BESTSELLER,
    LOW_STOCK
}

data class ProductModel(
    val id: String,
    val name: String,
    val imageUrl: String,
    val rating: Double,
    val soldCount: String,     // Định dạng "1,2k" hoặc "120"
    val originalPrice: Long?,  // Giá gốc (null nếu không có giảm giá)
    val currentPrice: Long,    // Giá phải trả hiện tại
    val badge: ProductBadge,   // Nhãn chính hiển thị góc trên trái
    val isOutOfStock: Boolean,
    val rawSoldCount: Int = 0,
    val stockQuantity: Int = 20,
    val brandId: String = "",
    val categoryId: String = "",
    val discountPercent: Int = 0,
    val createdAtSeconds: Long = 0L,
    val searchKeywords: String = ""
)
