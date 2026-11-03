package com.example.solesense.utils


import com.example.solesense.R // Để test tạm bằng ảnh local
import com.example.solesense.model.catalog.ProductBadge
import com.example.solesense.model.catalog.ProductModel

object MockData {
    val sampleProducts = listOf(
        // 1. Sản phẩm Bán chạy (Bestseller)
        ProductModel(
            id = "1",
            name = "K-Swiss Vista Trainer",
            imageUrl = "", // Tạm để rỗng, dùng imageRes bên UI để test
            rating = 4.5,
            soldCount = "1.2k",
            originalPrice = null,
            currentPrice = 2150000L,
            badge = ProductBadge.BESTSELLER, // Khai báo trực tiếp từ Enum
            isOutOfStock = false
        ),

        // 2. Sản phẩm Mới (New) + Đang giảm giá (Sale)
        // Trong thực tế Mapper sẽ ưu tiên 1 badge (thường ưu tiên SALE hoặc NEW). Ở đây test nhãn NEW.
        ProductModel(
            id = "2",
            name = "RS-X Women Sneaker",
            imageUrl = "",
            rating = 4.7,
            soldCount = "345",
            originalPrice = 3500000L,
            currentPrice = 2800000L,
            badge = ProductBadge.NEW,
            isOutOfStock = false
        ),

        // 3. Sản phẩm Tạm hết hàng (Out of Stock)
        ProductModel(
            id = "3",
            name = "Fila Windshift 15",
            imageUrl = "",
            rating = 4.3,
            soldCount = "89",
            originalPrice = null,
            currentPrice = 1750000L,
            badge = ProductBadge.NONE, // Hết hàng thì mờ đi, không hiển thị badge
            isOutOfStock = true // Cờ này sẽ kích hoạt lớp phủ đen "TẠM HẾT HÀNG" trên Card
        ),

        // 4. Sản phẩm Đang giảm giá (Sale)
        ProductModel(
            id = "4",
            name = "Nike Air Max 270",
            imageUrl = "",
            rating = 4.8,
            soldCount = "5.4k",
            originalPrice = 4000000L,
            currentPrice = 3200000L,
            badge = ProductBadge.SALE,
            isOutOfStock = false
        )
    )
}