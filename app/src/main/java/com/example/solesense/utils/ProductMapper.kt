package com.example.solesense.utils



import com.example.solesense.model.catalog.Product
import com.example.solesense.model.catalog.ProductBadge
import com.example.solesense.model.catalog.ProductModel
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

// Hàm mở rộng giúp chuyển đổi 1 đối tượng Product (DB) thành ProductModel (UI)
fun Product.toProductModel(): ProductModel {
    // 1. Tính toán Nhãn (Badge) bằng Logic nghiệp vụ
    val badge = when {
        this.salePrice != null && this.salePrice < this.basePrice -> ProductBadge.SALE
        this.totalSoldCount > 1000 -> ProductBadge.BESTSELLER
        isNewProduct(this.createdAt) -> ProductBadge.NEW
        else -> ProductBadge.NONE
    }

    // 2. Tính toán Giá trị hiển thị
    val currentPrice = this.salePrice ?: this.basePrice
    val originalPrice = if (this.salePrice != null) this.basePrice else null

    // 3. Trả về Model đã được "làm sạch" cho Compose vẽ
    return ProductModel(
        id = this.productId,
        name = this.name,
        // UI chỉ cần 1 ảnh đại diện (phần tử đầu tiên), nếu mảng rỗng thì trả về chuỗi rỗng
        imageUrl = this.imageUrls.firstOrNull() ?: "",
        rating = this.rating,
        soldCount = formatSoldCount(this.totalSoldCount), // Chuyển "1200" thành "1.2k"
        originalPrice = originalPrice?.toLong(),
        currentPrice = currentPrice.toLong(),
        badge = badge,
        isOutOfStock = this.status == "OUT_OF_STOCK" || this.totalStockQuantity <= 0
    )
}

// Logic phụ trợ: Định nghĩa thế nào là "Hàng mới" (Ví dụ: tạo trong vòng 14 ngày qua)
private fun isNewProduct(createdAt: Timestamp): Boolean {
    val now = Timestamp.now().seconds
    val created = createdAt.seconds
    val diffInDays = TimeUnit.SECONDS.toDays(now - created)
    return diffInDays <= 14
}

// Logic phụ trợ: Rút gọn số lượng bán
private fun formatSoldCount(count: Int): String {
    return when {
        count >= 1000 -> String.format("%.1fk", count / 1000.0)
        else -> count.toString()
    }
}