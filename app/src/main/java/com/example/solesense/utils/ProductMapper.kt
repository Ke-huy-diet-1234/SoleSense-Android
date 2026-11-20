package com.example.solesense.utils

import com.example.solesense.model.catalog.Product
import com.example.solesense.model.catalog.ProductBadge
import com.example.solesense.model.catalog.ProductModel
import java.text.Normalizer
import java.util.Locale

/**
 * Chuyển đổi đối tượng Product (Firestore) sang ProductModel (UI)
 * với cấu hình nhãn dán (Badge) phân bổ đa dạng, hài hòa theo đặc trưng từng sản phẩm
 * và tích hợp từ khóa tìm kiếm tiếng Việt (có dấu & không dấu).
 */
fun Product.toProductModel(): ProductModel {
    val currentPrice = this.salePrice ?: this.basePrice
    val originalPrice = if (this.salePrice != null && this.salePrice < this.basePrice) this.basePrice else null
    val discountPct = if (originalPrice != null && originalPrice > 0) {
        (((originalPrice - currentPrice) / originalPrice) * 100.0).toInt().coerceAtLeast(1)
    } else 0

    val badge = when {
        this.totalStockQuantity in 1..18 -> ProductBadge.LOW_STOCK
        this.totalSoldCount >= 600 -> ProductBadge.BESTSELLER
        this.totalSoldCount in 1..220 -> ProductBadge.NEW
        discountPct >= 15 -> ProductBadge.SALE
        else -> ProductBadge.NONE
    }

    val categoryViKeywords = when (this.categoryId.lowercase()) {
        "running" -> "giày chạy bộ marathon đi bộ đường dài thể thao"
        "lifestyle" -> "giày thời trang đi chơi đi học dạo phố sneaker cổ điển"
        "basketball" -> "giày bóng rổ sân bóng rổ bật nhảy"
        "training" -> "giày tập luyện tập gym thể hình"
        else -> this.categoryId
    }

    val brandDisplayKeywords = when (this.brandId.lowercase()) {
        "newbalance", "new_balance" -> "new balance nb"
        "nike" -> "nike air jordan pegasus"
        "adidas" -> "adidas ultraboost samba supernova"
        "asics" -> "asics gel nimbus kayano"
        "converse" -> "converse chuck taylor 70"
        "puma" -> "puma rs-x deviate nitro"
        "reebok" -> "reebok"
        else -> this.brandId
    }

    val rawSearchBlob = listOfNotNull(
        this.name,
        this.brandId,
        brandDisplayKeywords,
        this.categoryId,
        categoryViKeywords,
        this.description,
        this.purpose,
        this.material,
        this.cushioning,
        this.terrain
    ).joinToString(" ").lowercase(Locale.getDefault())

    val fullSearchKeywords = "$rawSearchBlob ${rawSearchBlob.removeVietnameseAccents()}"

    return ProductModel(
        id = this.productId,
        name = this.name,
        imageUrl = this.imageUrls.firstOrNull() ?: "",
        rating = this.rating,
        soldCount = formatSoldCount(this.totalSoldCount),
        originalPrice = originalPrice?.toLong(),
        currentPrice = currentPrice.toLong(),
        badge = badge,
        isOutOfStock = this.status == "OUT_OF_STOCK" || this.totalStockQuantity <= 0,
        rawSoldCount = this.totalSoldCount,
        stockQuantity = this.totalStockQuantity,
        brandId = if (this.brandId.equals("new_balance", ignoreCase = true)) "newbalance" else this.brandId,
        categoryId = this.categoryId,
        discountPercent = discountPct,
        createdAtSeconds = this.createdAt.seconds,
        searchKeywords = fullSearchKeywords
    )
}

fun String.removeVietnameseAccents(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .replace('đ', 'd')
        .replace('Đ', 'D')
}

private fun formatSoldCount(count: Int): String {
    return when {
        count >= 1000 -> String.format(Locale.US, "%.1fk", count / 1000.0).replace('.', ',')
        else -> count.toString()
    }
}
