package com.example.solesense.utils

import android.util.Log
import com.example.solesense.model.catalog.Category
import com.example.solesense.model.catalog.Product
import com.example.solesense.model.catalog.Variant
import com.example.solesense.model.order.Voucher
import com.example.solesense.model.user.Address
import com.example.solesense.model.user.Favorite
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class DatabaseSeeder {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun seedMockDataToFirestore(): Boolean {
        val batch = db.batch()

        try {
            val now = Timestamp.now()
            val oneMonthLater = Timestamp(Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000))

            // ==========================================
            // 1. CATEGORIES (Danh mục giày)
            // ==========================================
            val categories = listOf(
                Category(
                    categoryId = "running",
                    name = "Giày Chạy Bộ",
                    description = "Giày chạy bộ êm ái, hỗ trợ hoàn trả năng lượng tối ưu",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Category(
                    categoryId = "basketball",
                    name = "Giày Bóng Rổ",
                    description = "Giày bóng rổ cổ cao bảo vệ cổ chân, bám sân vượt trội",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Category(
                    categoryId = "lifestyle",
                    name = "Giày Thời Trang (Lifestyle)",
                    description = "Giày sneaker đi học, đi chơi, phối đồ hàng ngày",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Category(
                    categoryId = "training",
                    name = "Giày Tập Luyện (Gym & Training)",
                    description = "Giày đế bằng ổn định cho tập gym và thể thao đa năng",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                )
            )
            categories.forEach { category ->
                val ref = db.collection("categories").document(category.categoryId)
                batch.set(ref, category)
            }

            // ==========================================
            // 2. PRODUCTS & VARIANTS (Có đủ biến thể nhiều hàng, còn 1 đôi và hết hàng)
            // ==========================================
            val ubWhiteImg = "https://assets.adidas.com/images/w_500,f_auto,q_auto/ff67e3405f7f46eca232d3aa4e0b78e9_9366/Giay_Ultraboost_5_trang_ID8810_HM1.jpg"
            val pegasusImg = "https://static.nike.com/a/images/t_PDP_144_v1/f_auto,q_auto:eco,u_9ddf04c7-2a9a-4d76-add1-d15af8f0263d,c_scale,fl_relative,w_1.0,h_1.0,fl_layer_apply/887859e5-9b50-4c19-a9bb-733f55f88cb2/AIR%2BZOOM%2BPEGASUS%2B41.png"

            val product1Ref = db.collection("products").document("adidas_ultraboost_5_white")
            val product1 = Product(
                productId = product1Ref.id,
                name = "Giày Ultraboost 5",
                brandId = "adidas",
                categoryId = "running",
                description = "Giày chạy bộ với đệm Light BOOST V2, hệ thống Torsion System và đế ngoài Continental Rubber.",
                basePrice = 4900000.0,
                salePrice = 2450000.0,
                gender = "MEN",
                purpose = "Chạy bộ đường nhựa, đi bộ hàng ngày",
                material = "Primeknit thoáng khí",
                cushioning = "Light BOOST V2 êm ái",
                terrain = "Đường nhựa (Road)",
                imageUrls = listOf(ubWhiteImg, pegasusImg),
                rating = 4.7,
                reviewCount = 615,
                totalSoldCount = 1500,
                totalStockQuantity = 36,
                status = "ACTIVE",
                createdAt = now,
                updatedAt = now
            )
            batch.set(product1Ref, product1)

            val ubVariants = listOf(
                Variant(
                    variantId = "ub5_white_40",
                    sku = "ID8810-WHT-40",
                    color = "Trắng/Hồng",
                    size = "40",
                    imageUrls = listOf(ubWhiteImg),
                    price = 2450000.0,
                    stock = 15,
                    soldCount = 500,
                    status = "ACTIVE"
                ),
                Variant(
                    variantId = "ub5_white_41",
                    sku = "ID8810-WHT-41",
                    color = "Trắng/Hồng",
                    size = "41",
                    imageUrls = listOf(ubWhiteImg),
                    price = 2450000.0,
                    stock = 1, // Cố tình để stock = 1 để test cảnh báo sắp hết hàng & tranh chấp đơn!
                    soldCount = 700,
                    status = "ACTIVE"
                ),
                Variant(
                    variantId = "ub5_white_42",
                    sku = "ID8810-WHT-42",
                    color = "Trắng/Hồng",
                    size = "42",
                    imageUrls = listOf(ubWhiteImg),
                    price = 2550000.0,
                    stock = 0, // Cố tình để stock = 0 để test hết hàng
                    soldCount = 300,
                    status = "OUT_OF_STOCK"
                ),
                Variant(
                    variantId = "ub5_grey_40",
                    sku = "ID8810-GRY-40",
                    color = "Xám Bạc",
                    size = "40",
                    imageUrls = listOf(pegasusImg),
                    price = 2500000.0,
                    stock = 20,
                    soldCount = 120,
                    status = "ACTIVE"
                )
            )

            ubVariants.forEach { variant ->
                val variantRef = product1Ref.collection("variants").document(variant.variantId)
                batch.set(variantRef, variant)
            }

            // SẢN PHẨM 2: Nike Pegasus 41
            val product2Ref = db.collection("products").document("nike_pegasus_41_fd2722_102")
            val product2 = Product(
                productId = product2Ref.id,
                name = "Nike Pegasus 41",
                brandId = "nike",
                categoryId = "running",
                description = "Giày chạy bộ đường nhựa Nike Pegasus 41, sử dụng đệm ReactX và hai bộ phận Air Zoom ở gót và mũi chân.",
                basePrice = 3829000.0,
                salePrice = 3200000.0,
                gender = "MEN",
                purpose = "Chạy bộ tốc độ, tập luyện hàng ngày",
                material = "Engineered Mesh",
                cushioning = "ReactX + Dual Air Zoom",
                terrain = "Đường nhựa (Road)",
                imageUrls = listOf(pegasusImg, ubWhiteImg),
                rating = 4.8,
                reviewCount = 657,
                totalSoldCount = 120,
                totalStockQuantity = 12,
                status = "ACTIVE",
                createdAt = now,
                updatedAt = now
            )
            batch.set(product2Ref, product2)

            val pegasusVariants = listOf(
                Variant(
                    variantId = "pegasus41_white_40",
                    sku = "FD2722-102-40",
                    color = "Trắng Bạc",
                    size = "40",
                    imageUrls = listOf(pegasusImg),
                    price = 3200000.0,
                    stock = 1, // Chỉ còn 1 đôi để test giữ hàng 5 phút
                    soldCount = 40,
                    status = "ACTIVE"
                ),
                Variant(
                    variantId = "pegasus41_white_41",
                    sku = "FD2722-102-41",
                    color = "Trắng Bạc",
                    size = "41",
                    imageUrls = listOf(pegasusImg),
                    price = 3200000.0,
                    stock = 11,
                    soldCount = 50,
                    status = "ACTIVE"
                ),
                Variant(
                    variantId = "pegasus41_white_42",
                    sku = "FD2722-102-42",
                    color = "Trắng Bạc",
                    size = "42",
                    imageUrls = listOf(pegasusImg),
                    price = 3350000.0,
                    stock = 0,
                    soldCount = 30,
                    status = "OUT_OF_STOCK"
                )
            )

            pegasusVariants.forEach { variant ->
                val variantRef = product2Ref.collection("variants").document(variant.variantId)
                batch.set(variantRef, variant)
            }

            // ==========================================
            // 3. VOUCHERS (Mã giảm giá)
            // ==========================================
            val vouchers = listOf(
                Voucher(
                    voucherId = "vc_sole200",
                    code = "SOLE200",
                    description = "Giảm 200.000₫",
                    discountType = "AMOUNT",
                    discountValue = 200000.0,
                    minOrderValue = 1000000.0,
                    maxDiscount = 200000.0,
                    usageLimit = 100,
                    usedCount = 5,
                    startAt = now,
                    endAt = oneMonthLater,
                    status = "ACTIVE"
                ),
                Voucher(
                    voucherId = "vc_sole10",
                    code = "SOLE10",
                    description = "Giảm 10% tối đa 300.000₫",
                    discountType = "PERCENT",
                    discountValue = 10.0,
                    minOrderValue = 2000000.0,
                    maxDiscount = 300000.0,
                    usageLimit = 50,
                    usedCount = 2,
                    startAt = now,
                    endAt = oneMonthLater,
                    status = "ACTIVE"
                )
            )
            vouchers.forEach { voucher ->
                val ref = db.collection("vouchers").document(voucher.voucherId)
                batch.set(ref, voucher)
            }

            // ==========================================
            // 4. ADDRESSES & FAVORITES (Cho user hiện tại + user admin trên Firebase của bạn)
            // ==========================================
            val targetUserIds = listOfNotNull(
                auth.currentUser?.uid,
                "mmZhfijntCf7BW2U0pf9VQbm4Xi1",
                "demo_customer"
            ).distinct()

            val defaultAddress = Address(
                addressId = "addr_default",
                receiverName = "Vương Khắc Hoàn",
                phone = "0987654321",
                province = "Hà Nội",
                district = "Cầu Giấy",
                ward = "Dịch Vọng",
                detail = "Số 12, Ngõ 34, Đường Cầu Giấy, Hà Nội",
                isDefault = true,
                createdAt = now,
                updatedAt = now
            )

            val defaultFavorite = Favorite(
                productId = "adidas_ultraboost_5_white",
                createdAt = now
            )

            targetUserIds.forEach { uid ->
                val userRef = db.collection("users").document(uid)
                batch.set(
                    userRef.collection("addresses").document(defaultAddress.addressId),
                    defaultAddress
                )
                batch.set(
                    userRef.collection("favorites").document(defaultFavorite.productId),
                    defaultFavorite
                )
            }

            batch.commit().await()
            Log.d("DatabaseSeeder", "Khởi tạo Categories, Products/Variants, Vouchers, Addresses, Favorites thành công!")
            return true
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Lỗi bơm dữ liệu: ${e.message}")
            return false
        }
    }
}
