package com.example.solesense.utils



import android.util.Log
import com.example.solesense.model.catalog.Product
import com.example.solesense.model.catalog.Variant

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DatabaseSeeder {
    private val db = FirebaseFirestore.getInstance()

    suspend fun seedMockDataToFirestore(): Boolean {
        val batch = db.batch()

        try {
            // ==========================================
            // SẢN PHẨM 1: Adidas Ultraboost 5 (Đang SALE)
            // ==========================================
            val product1Ref = db.collection("products").document("adidas_ultraboost_5_white")
            val product1 = Product(
                productId = product1Ref.id,
                name = "Giày Ultraboost 5",
                brandId = "adidas",
                categoryId = "running",
                description = "Giày chạy bộ với đệm Light BOOST V2, hệ thống Torsion System và đế ngoài Continental Rubber.",
                basePrice = 4900000.0,
                salePrice = 2450000.0, // Sale 50%
                gender = "MEN",
                imageUrls = listOf("https://assets.adidas.com/images/w_500,f_auto,q_auto/ff67e3405f7f46eca232d3aa4e0b78e9_9366/Giay_Ultraboost_5_trang_ID8810_HM1.jpg"),
                rating = 4.7,
                reviewCount = 615,
                totalSoldCount = 1500, // Tạo số bán cao để lên nhãn BESTSELLER
                totalStockQuantity = 45, // Tạm cấp tồn kho
                status = "ACTIVE",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            batch.set(product1Ref, product1)

            // Các biến thể của Ultraboost 5
            val ubVariants = listOf(
                Variant(
                    variantId = "ub5_white_40",
                    sku = "ID8810-WHT-40",
                    color = "White/Grey/Pink",
                    size = "40",
                    imageUrls = listOf("https://assets.adidas.com/images/w_500,f_auto,q_auto/ff67e3405f7f46eca232d3aa4e0b78e9_9366/Giay_Ultraboost_5_trang_ID8810_HM1.jpg"),
                    price = 0.0,
                    stock = 15,
                    soldCount = 500,
                    status = "ACTIVE"
                ),
                Variant(
                    variantId = "ub5_white_41",
                    sku = "ID8810-WHT-41",
                    color = "White/Grey/Pink",
                    size = "41",
                    imageUrls = listOf("https://assets.adidas.com/images/w_500,f_auto,q_auto/ff67e3405f7f46eca232d3aa4e0b78e9_9366/Giay_Ultraboost_5_trang_ID8810_HM1.jpg"),
                    price = 0.0,
                    stock = 20,
                    soldCount = 700,
                    status = "ACTIVE"
                ),
                Variant(
                    variantId = "ub5_white_42",
                    sku = "ID8810-WHT-42",
                    color = "White/Grey/Pink",
                    size = "42",
                    imageUrls = listOf("https://assets.adidas.com/images/w_500,f_auto,q_auto/ff67e3405f7f46eca232d3aa4e0b78e9_9366/Giay_Ultraboost_5_trang_ID8810_HM1.jpg"),
                    price = 0.0,
                    stock = 10,
                    soldCount = 300,
                    status = "ACTIVE"
                )
            )

            ubVariants.forEach { variant ->
                val variantRef = product1Ref.collection("variants").document(variant.variantId)
                batch.set(variantRef, variant)
            }


            // ==========================================
            // SẢN PHẨM 2: Nike Pegasus 41 (Hàng Mới, Tạm hết hàng)
            // ==========================================
            val product2Ref = db.collection("products").document("nike_pegasus_41_fd2722_102")
            val product2 = Product(
                productId = product2Ref.id,
                name = "Nike Pegasus 41",
                brandId = "nike",
                categoryId = "running",
                description = "Giày chạy bộ đường nhựa Nike Pegasus 41, sử dụng đệm ReactX và hai bộ phận Air Zoom ở gót và mũi chân.",
                basePrice = 3829000.0,
                salePrice = null,
                gender = "MEN",
                imageUrls = listOf("https://static.nike.com/a/images/t_PDP_144_v1/f_auto,q_auto:eco,u_9ddf04c7-2a9a-4d76-add1-d15af8f0263d,c_scale,fl_relative,w_1.0,h_1.0,fl_layer_apply/887859e5-9b50-4c19-a9bb-733f55f88cb2/AIR%2BZOOM%2BPEGASUS%2B41.png"),
                rating = 4.7,
                reviewCount = 657,
                totalSoldCount = 120, // Số bán thấp để test giao diện
                totalStockQuantity = 0, // Cố tình để 0 để test UI lớp phủ đen "TẠM HẾT HÀNG"
                status = "OUT_OF_STOCK",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            batch.set(product2Ref, product2)

            // Các biến thể của Nike Pegasus 41
            val pegasusVariants = listOf(
                Variant(
                    variantId = "pegasus41_white_40",
                    sku = "FD2722-102-40",
                    color = "White/Pure Platinum/Metallic Silver",
                    size = "40",
                    imageUrls = listOf("https://static.nike.com/a/images/t_PDP_144_v1/f_auto,q_auto:eco,u_9ddf04c7-2a9a-4d76-add1-d15af8f0263d,c_scale,fl_relative,w_1.0,h_1.0,fl_layer_apply/887859e5-9b50-4c19-a9bb-733f55f88cb2/AIR%2BZOOM%2BPEGASUS%2B41.png"),
                    price = 0.0,
                    stock = 0,
                    soldCount = 40,
                    status = "OUT_OF_STOCK"
                ),
                Variant(
                    variantId = "pegasus41_white_41",
                    sku = "FD2722-102-41",
                    color = "White/Pure Platinum/Metallic Silver",
                    size = "41",
                    imageUrls = listOf("https://static.nike.com/a/images/t_PDP_144_v1/f_auto,q_auto:eco,u_9ddf04c7-2a9a-4d76-add1-d15af8f0263d,c_scale,fl_relative,w_1.0,h_1.0,fl_layer_apply/887859e5-9b50-4c19-a9bb-733f55f88cb2/AIR%2BZOOM%2BPEGASUS%2B41.png"),
                    price = 0.0,
                    stock = 0,
                    soldCount = 50,
                    status = "OUT_OF_STOCK"
                ),
                Variant(
                    variantId = "pegasus41_white_42",
                    sku = "FD2722-102-42",
                    color = "White/Pure Platinum/Metallic Silver",
                    size = "42",
                    imageUrls = listOf("https://static.nike.com/a/images/t_PDP_144_v1/f_auto,q_auto:eco,u_9ddf04c7-2a9a-4d76-add1-d15af8f0263d,c_scale,fl_relative,w_1.0,h_1.0,fl_layer_apply/887859e5-9b50-4c19-a9bb-733f55f88cb2/AIR%2BZOOM%2BPEGASUS%2B41.png"),
                    price = 0.0,
                    stock = 0,
                    soldCount = 30,
                    status = "OUT_OF_STOCK"
                )
            )

            pegasusVariants.forEach { variant ->
                val variantRef = product2Ref.collection("variants").document(variant.variantId)
                batch.set(variantRef, variant)
            }

            // Thực thi ghi
            batch.commit().await()
            Log.d("DatabaseSeeder", "Bơm dữ liệu Adidas & Nike thành công!")
            return true

        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Lỗi bơm dữ liệu: ${e.message}")
            return false
        }
    }
}