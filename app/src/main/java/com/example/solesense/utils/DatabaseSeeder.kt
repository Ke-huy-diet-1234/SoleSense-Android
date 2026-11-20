package com.example.solesense.utils

import android.util.Log
import com.example.solesense.model.catalog.Brand
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

    companion object {
        private const val TAG = "DatabaseSeeder"
        const val CURRENT_CATALOG_VERSION = 10
    }

    /**
     * Tự động kiểm tra và bơm danh mục sản phẩm dày (13 mẫu giày, 6 hãng, 45+ biến thể)
     * nếu Firestore chưa được cập nhật phiên bản mới nhất.
     */
    suspend fun ensureRichCatalogSeeded(): Boolean {
        return try {
            val metaRef = db.collection("config").document("catalog")
            val metaSnap = metaRef.get().await()
            val currentVer = metaSnap.getLong("version")?.toInt() ?: 0

            // Kiểm tra số lượng sản phẩm thực tế hiện có trên Firestore
            val existingProductsCount = try {
                db.collection("products").get().await().size()
            } catch (_: Exception) { 0 }

            if (currentVer < CURRENT_CATALOG_VERSION || existingProductsCount < 10) {
                Log.d(TAG, "Đang bơm dữ liệu dày lên Firestore (Phiên bản: $CURRENT_CATALOG_VERSION)...")
                val success = seedMockDataToFirestore()
                if (success) {
                    metaRef.set(mapOf("version" to CURRENT_CATALOG_VERSION, "updatedAt" to Timestamp.now())).await()
                    Log.d(TAG, "Cập nhật metadata catalog version = $CURRENT_CATALOG_VERSION thành công!")
                }
                success
            } else {
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Kiểm tra version lỗi: ${e.message}, tiến hành bơm dữ liệu...")
            seedMockDataToFirestore()
        }
    }

    suspend fun seedMockDataToFirestore(): Boolean {
        try {
            val now = Timestamp.now()
            val oneMonthLater = Timestamp(Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000))
            val twoMonthsLater = Timestamp(Date(System.currentTimeMillis() + 60L * 24 * 3600 * 1000))
            val yesterday = Timestamp(Date(System.currentTimeMillis() - 24L * 3600 * 1000))
            val oneMonthAgo = Timestamp(Date(System.currentTimeMillis() - 30L * 24 * 3600 * 1000))

            // ==========================================
            // 1. BRANDS (7 thương hiệu hàng đầu với SVG chuẩn từ GitHub solesense-assets)
            // ==========================================
            val brands = listOf(
                Brand(
                    brandId = "nike",
                    name = "Nike",
                    logoUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/Logo_NIKE.svg",
                    description = "Thương hiệu thể thao số 1 thế giới với khẩu hiệu Just Do It",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Brand(
                    brandId = "adidas",
                    name = "Adidas",
                    logoUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/Adidas_Logo.svg",
                    description = "Thương hiệu 3 sọc kinh điển từ nước Đức - Impossible Is Nothing",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Brand(
                    brandId = "asics",
                    name = "Asics",
                    logoUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/Asics_Logo.svg",
                    description = "Chuyên gia giày chạy bộ Nhật Bản - Sound Mind, Sound Body",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Brand(
                    brandId = "newbalance",
                    name = "New Balance",
                    logoUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/New_Balance_Logo.svg",
                    description = "Thương hiệu giày chạy và lifestyle đậm chất retro cổ điển nước Mỹ",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Brand(
                    brandId = "converse",
                    name = "Converse",
                    logoUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/Converse_Logo.svg",
                    description = "Biểu tượng văn hóa đại chúng với dòng giày Chuck Taylor All Star",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Brand(
                    brandId = "puma",
                    name = "Puma",
                    logoUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/Puma_Logo.svg",
                    description = "Thời trang thể thao đường phố tốc độ - Forever Faster",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Brand(
                    brandId = "reebok",
                    name = "Reebok",
                    logoUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/Reebok_Logo.svg",
                    description = "Thương hiệu giày tập luyện và thể hình hàng đầu",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                )
            )

            val officialBrandIds = brands.map { it.brandId }.toSet()
            try {
                val existingBrandSnap = db.collection("brands").get().await()
                for (doc in existingBrandSnap.documents) {
                    if (doc.id !in officialBrandIds) {
                        doc.reference.delete().await()
                    }
                }
            } catch (_: Exception) {}

            val batch1 = db.batch()
            brands.forEach { brand ->
                batch1.set(db.collection("brands").document(brand.brandId), brand)
            }

            // ==========================================
            // 2. CATEGORIES (4 danh mục chính)
            // ==========================================
            val categories = listOf(
                Category(
                    categoryId = "running",
                    name = "Giày Chạy Bộ",
                    imageUrl = "https://static.nike.com/a/images/t_PDP_1728_v1/f_auto,q_auto:eco/887859e5-9b50-4c19-a9bb-733f55f88cb2/AIR+ZOOM+PEGASUS+41.png",
                    description = "Giày chạy bộ êm ái, bứt tốc và bảo vệ khớp gối tối ưu",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Category(
                    categoryId = "lifestyle",
                    name = "Giày Thời Trang",
                    imageUrl = "https://assets.adidas.com/images/h_840,f_auto,q_auto,fl_lossy,c_fill,g_auto/3bbecbdf584e40398446a8bf0117cf62_9366/Samba_OG_Shoes_White_B75806_01_standard.jpg",
                    description = "Sneaker đi học, đi chơi, phối đồ streetwear phong cách",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Category(
                    categoryId = "basketball",
                    name = "Giày Bóng Rổ",
                    imageUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=85",
                    description = "Giày bóng rổ bật nảy, bảo vệ cổ chân, bám sân chống trượt",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                Category(
                    categoryId = "training",
                    name = "Giày Tập Luyện",
                    imageUrl = "https://images.puma.com/image/upload/f_auto,q_auto,b_rgb:fafafa,w_600,h_600/global/390776/01/sv01/fnd/VNM/fmt/png",
                    description = "Giày tập gym, cross-training, đế bằng ổn định trọng tâm",
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                )
            )
            categories.forEach { category ->
                batch1.set(db.collection("categories").document(category.categoryId), category)
            }

            // ==========================================
            // 3. VOUCHERS (Mã giảm giá phong phú - Đầy đủ các trường hợp: Hợp lệ, Hết hạn, Hết lượt, Chưa diễn ra...)
            // ==========================================
            val vouchers = listOf(
                // 1. Voucher Hợp lệ - Giảm 200.000₫ cho đơn từ 1.000.000₫
                Voucher(
                    voucherId = "vc_sole200",
                    code = "SOLE200",
                    name = "Khuyến mãi SoleSense 200K",
                    description = "Giảm trực tiếp 200.000₫ cho đơn từ 1.000.000₫",
                    discountType = "FIXED",
                    discountValue = 200000.0,
                    maxDiscountAmount = null,
                    minOrderValue = 1000000.0,
                    scope = "ALL",
                    targetAudience = "ALL",
                    usageLimit = 100,
                    usedCount = 15,
                    reservedCount = 0,
                    perUserLimit = 1,
                    startAt = now,
                    endAt = oneMonthLater,
                    isEnabled = true
                ),
                // 2. Voucher Hợp lệ - Giảm 10% tối đa 300.000₫
                Voucher(
                    voucherId = "vc_sole10",
                    code = "SOLE10",
                    name = "Ưu đãi thành viên 10%",
                    description = "Giảm 10% tối đa 300.000₫ cho đơn từ 1.000.000₫",
                    discountType = "PERCENT",
                    discountValue = 10.0,
                    maxDiscountAmount = 300000.0,
                    minOrderValue = 1000000.0,
                    scope = "ALL",
                    targetAudience = "ALL",
                    usageLimit = 150,
                    usedCount = 18,
                    reservedCount = 0,
                    perUserLimit = 1,
                    startAt = now,
                    endAt = oneMonthLater,
                    isEnabled = true
                ),
                // 3. Voucher Hợp lệ - Miễn phí vận chuyển 30.000₫
                Voucher(
                    voucherId = "vc_freeship",
                    code = "FREESHIP",
                    name = "Miễn phí vận chuyển",
                    description = "Miễn phí vận chuyển toàn quốc (giảm 30.000₫)",
                    discountType = "FREE_SHIPPING",
                    discountValue = 30000.0,
                    maxDiscountAmount = 30000.0,
                    minOrderValue = 200000.0,
                    scope = "ALL",
                    targetAudience = "ALL",
                    usageLimit = 500,
                    usedCount = 65,
                    reservedCount = 0,
                    perUserLimit = 2,
                    startAt = now,
                    endAt = oneMonthLater,
                    isEnabled = true
                ),
                // 4. Voucher Hợp lệ - Danh mục chạy bộ 15%
                Voucher(
                    voucherId = "vc_running15",
                    code = "RUNNING15",
                    name = "Voucher Giày chạy bộ 15%",
                    description = "Giảm 15% tối đa 500.000₫ cho giày chạy bộ",
                    discountType = "PERCENT",
                    discountValue = 15.0,
                    maxDiscountAmount = 500000.0,
                    minOrderValue = 500000.0,
                    scope = "CATEGORY",
                    applicableCategoryIds = listOf("running"),
                    targetAudience = "ALL",
                    usageLimit = 80,
                    usedCount = 12,
                    reservedCount = 0,
                    perUserLimit = 1,
                    startAt = now,
                    endAt = oneMonthLater,
                    isEnabled = true
                ),
                // 5. Voucher Hợp lệ - Khách hàng mới (NEW_CUSTOMER)
                Voucher(
                    voucherId = "vc_welcome50",
                    code = "WELCOME50",
                    name = "Chào bạn mới 50K",
                    description = "Giảm 50.000₫ cho khách hàng mới mua lần đầu",
                    discountType = "FIXED",
                    discountValue = 50000.0,
                    maxDiscountAmount = null,
                    minOrderValue = 300000.0,
                    scope = "ALL",
                    targetAudience = "NEW_CUSTOMER",
                    usageLimit = 200,
                    usedCount = 5,
                    reservedCount = 0,
                    perUserLimit = 1,
                    startAt = now,
                    endAt = oneMonthLater,
                    isEnabled = true
                ),
                // 6. Voucher TEST: ĐÃ HẾT HẠN (EXPIRED) - Kết thúc ngày hôm qua
                Voucher(
                    voucherId = "vc_expired2025",
                    code = "EXPIRED2025",
                    name = "Khuyến mãi đã hết hạn",
                    description = "Giảm 500.000₫ cho đơn từ 1.000.000₫ (Đã hết hạn hôm qua)",
                    discountType = "FIXED",
                    discountValue = 500000.0,
                    maxDiscountAmount = null,
                    minOrderValue = 1000000.0,
                    scope = "ALL",
                    targetAudience = "ALL",
                    usageLimit = 100,
                    usedCount = 88,
                    reservedCount = 0,
                    perUserLimit = 1,
                    startAt = oneMonthAgo,
                    endAt = yesterday,
                    isEnabled = true
                ),
                // 7. Voucher TEST: ĐÃ HẾT HẠN (EXPIRED) - HẾT HẠN 50K
                Voucher(
                    voucherId = "vc_hethan50k",
                    code = "HETHAN50K",
                    name = "Tri ân cuối tuần (Hết hạn)",
                    description = "Giảm 50.000₫ cho mọi đơn hàng (Hết hạn)",
                    discountType = "FIXED",
                    discountValue = 50000.0,
                    maxDiscountAmount = null,
                    minOrderValue = 200000.0,
                    scope = "ALL",
                    targetAudience = "ALL",
                    usageLimit = 100,
                    usedCount = 50,
                    reservedCount = 0,
                    perUserLimit = 1,
                    startAt = oneMonthAgo,
                    endAt = yesterday,
                    isEnabled = true
                ),
                // 8. Voucher TEST: CHƯA ĐẾN THỜI GIAN (SCHEDULED) - Năm mới 2027
                Voucher(
                    voucherId = "vc_newyear2027",
                    code = "NEWYEAR2027",
                    name = "Khuyến mãi Năm Mới 2027",
                    description = "Giảm 300.000₫ cho đơn từ 2 triệu (Chưa bắt đầu)",
                    discountType = "FIXED",
                    discountValue = 300000.0,
                    maxDiscountAmount = null,
                    minOrderValue = 2000000.0,
                    scope = "ALL",
                    targetAudience = "ALL",
                    usageLimit = 100,
                    usedCount = 0,
                    reservedCount = 0,
                    perUserLimit = 1,
                    startAt = oneMonthLater,
                    endAt = twoMonthsLater,
                    isEnabled = true
                ),
                // 9. Voucher TEST: ĐÃ HẾT SẠCH LƯỢT DÙNG (EXHAUSTED)
                Voucher(
                    voucherId = "vc_soldout99",
                    code = "SOLDOUT99",
                    name = "Flash Sale 100K Chớp Nhoáng",
                    description = "Giảm 100.000₫ (Đã hết 50/50 lượt sử dụng)",
                    discountType = "FIXED",
                    discountValue = 100000.0,
                    maxDiscountAmount = null,
                    minOrderValue = 500000.0,
                    scope = "ALL",
                    targetAudience = "ALL",
                    usageLimit = 50,
                    usedCount = 50,
                    reservedCount = 0,
                    perUserLimit = 1,
                    startAt = now,
                    endAt = oneMonthLater,
                    isEnabled = true
                ),
                // 10. Voucher TEST: CHƯA ĐỦ ĐƠN HÀNG TỐI THIỂU (MIN ORDER VALUE)
                Voucher(
                    voucherId = "vc_vip1trieu",
                    code = "VIP1TRIEU",
                    name = "Vip Diamond Giảm 1 Triệu",
                    description = "Giảm 1.000.000₫ cho đơn hàng tối thiểu từ 10.000.000₫",
                    discountType = "FIXED",
                    discountValue = 1000000.0,
                    maxDiscountAmount = null,
                    minOrderValue = 10000000.0,
                    scope = "ALL",
                    targetAudience = "ALL",
                    usageLimit = 20,
                    usedCount = 2,
                    reservedCount = 0,
                    perUserLimit = 1,
                    startAt = now,
                    endAt = oneMonthLater,
                    isEnabled = true
                ),
                // 11. Voucher TEST: ĐÃ TẮT BỞI ADMIN (DISABLED)
                Voucher(
                    voucherId = "vc_voucher_tat",
                    code = "VOUCHER_TAT",
                    name = "Voucher tạm ngưng áp dụng",
                    description = "Giảm 50.000₫ - Admin đã tắt",
                    discountType = "FIXED",
                    discountValue = 50000.0,
                    maxDiscountAmount = null,
                    minOrderValue = 100000.0,
                    scope = "ALL",
                    targetAudience = "ALL",
                    usageLimit = 100,
                    usedCount = 0,
                    reservedCount = 0,
                    perUserLimit = 1,
                    startAt = now,
                    endAt = oneMonthLater,
                    isEnabled = false
                )
            )
            vouchers.forEach { voucher ->
                batch1.set(db.collection("vouchers").document(voucher.voucherId), voucher)
            }

            // ==========================================
            // 4. COMMIT BRANDS, CATEGORIES, VOUCHERS
            // (Không tự động gán địa chỉ mẫu cho tài khoản mới để người dùng tự cập nhật từ Hồ sơ)
            // ==========================================
            batch1.commit().await()
            Log.d(TAG, "Đã lưu thành công Brands, Categories, Vouchers!")

            // ==========================================
            // 5. PRODUCTS & VARIANTS (13 mẫu giày chi tiết)
            // ==========================================
            seedAllProductsAndVariants(now)

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi nạp dữ liệu: ${e.message}", e)
            return false
        }
    }

    private suspend fun seedAllProductsAndVariants(now: Timestamp) {
        // Dữ liệu ảnh CDN chuẩn của từng mẫu giày (Đã kiểm chứng HTTP 200)
        val imgPegasusWhite = "https://static.nike.com/a/images/t_PDP_1728_v1/f_auto,q_auto:eco/887859e5-9b50-4c19-a9bb-733f55f88cb2/AIR+ZOOM+PEGASUS+41.png"
        val imgPegasusBlack = "https://static.nike.com/a/images/t_PDP_1728_v1/f_auto,q_auto:eco/350e7f3a-979a-402b-9396-a8a998dd76ab/AIR+ZOOM+PEGASUS+41.png"

        val imgAm270Black = "https://static.nike.com/a/images/t_PDP_1728_v1/f_auto,q_auto:eco/awjogtdnqxniqqk0wpgf/AIR+MAX+270.png"
        val imgAm270White = "https://static.nike.com/a/images/t_PDP_1728_v1/f_auto,q_auto:eco/pyyixbczj6u5kiwhpjik/AIR+MAX+270.png"

        val imgGtCut3White = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=85"

        val imgUb5White = "https://assets.adidas.com/images/w_600,f_auto,q_auto/ff67e3405f7f46eca232d3aa4e0b78e9_9366/Giay_Ultraboost_5_trang_ID8810_HM1.jpg"
        val imgUb5Grey = "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=900&q=85"

        val imgSambaWhite = "https://assets.adidas.com/images/h_840,f_auto,q_auto,fl_lossy,c_fill,g_auto/3bbecbdf584e40398446a8bf0117cf62_9366/Samba_OG_Shoes_White_B75806_01_standard.jpg"
        val imgSupernovaBlue = "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=900&q=85"

        val imgNimbus26 = "https://images.asics.com/is/image/asics/1011B794_400_SR_RT_GLB"
        val imgKayano30 = "https://images.asics.com/is/image/asics/1011B548_002_SR_RT_GLB"

        val imgNb574 = "https://nb.scene7.com/is/image/NB/ml574evw_nb_02_i"
        val imgNb1080 = "https://nb.scene7.com/is/image/NB/m1080k13_nb_02_i"

        val imgChuck70 = "https://images.unsplash.com/photo-1607522370275-f14206abe5d3?auto=format&fit=crop&w=900&q=85"

        val imgRsx = "https://images.puma.com/image/upload/f_auto,q_auto,b_rgb:fafafa,w_600,h_600/global/390776/01/sv01/fnd/VNM/fmt/png"
        val imgDeviate2 = "https://images.puma.com/image/upload/f_auto,q_auto,b_rgb:fafafa,w_600,h_600/global/376807/01/sv01/fnd/VNM/fmt/png"

        // Định nghĩa 13 sản phẩm kèm toàn bộ biến thể tương ứng
        val catalogData = listOf(
            // 1. Nike Pegasus 41
            Pair(
                Product(
                    productId = "nike_pegasus_41_fd2722_102",
                    name = "Nike Pegasus 41",
                    brandId = "nike",
                    categoryId = "running",
                    description = "Giày chạy bộ đường nhựa huyền thoại, trang bị đệm bọt ReactX cải tiến tăng 13% hoàn trả năng lượng cùng bộ đôi đệm Air Zoom ở mũi và gót chân.",
                    basePrice = 3829000.0,
                    salePrice = 3200000.0,
                    gender = "UNISEX",
                    purpose = "Chạy bộ hàng ngày, luyện tập cự ly 5km - 21km",
                    material = "Engineered Mesh thoáng khí",
                    cushioning = "ReactX Foam + Dual Air Zoom",
                    terrain = "Đường nhựa (Road)",
                    imageUrls = listOf(imgPegasusWhite, imgPegasusBlack),
                    rating = 4.8,
                    reviewCount = 142,
                    totalSoldCount = 310,
                    totalStockQuantity = 24,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "peg41_white_40", sku = "FD2722-102-40", color = "Trắng Bạc", size = "40", imageUrls = listOf(imgPegasusWhite), price = 3200000.0, stock = 6, soldCount = 80, status = "ACTIVE"),
                    Variant(variantId = "peg41_white_41", sku = "FD2722-102-41", color = "Trắng Bạc", size = "41", imageUrls = listOf(imgPegasusWhite), price = 3200000.0, stock = 11, soldCount = 120, status = "ACTIVE"),
                    Variant(variantId = "peg41_white_42", sku = "FD2722-102-42", color = "Trắng Bạc", size = "42", imageUrls = listOf(imgPegasusWhite), price = 3200000.0, stock = 5, soldCount = 70, status = "ACTIVE"),
                    Variant(variantId = "peg41_black_41", sku = "FD2722-001-41", color = "Đen Trắng", size = "41", imageUrls = listOf(imgPegasusBlack), price = 3200000.0, stock = 2, soldCount = 40, status = "ACTIVE")
                )
            ),

            // 2. Nike Air Max 270
            Pair(
                Product(
                    productId = "nike_air_max_270_ah8050",
                    name = "Nike Air Max 270",
                    brandId = "nike",
                    categoryId = "lifestyle",
                    description = "Mẫu sneaker thời trang với túi đệm Max Air cao 32mm ở gót chân - túi đệm lớn nhất từng có của Nike, mang lại cảm giác êm ái bồng bềnh suốt cả ngày.",
                    basePrice = 4490000.0,
                    salePrice = 3590000.0,
                    gender = "MEN",
                    purpose = "Đi chơi, dạo phố, phong cách sneakerhead năng động",
                    material = "Vải dệt Knit co giãn",
                    cushioning = "Túi khí Max Air 270 độ siêu nảy",
                    terrain = "Đô thị (City)",
                    imageUrls = listOf(imgAm270Black, imgAm270White),
                    rating = 4.7,
                    reviewCount = 198,
                    totalSoldCount = 450,
                    totalStockQuantity = 29,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "am270_black_40", sku = "AH8050-002-40", color = "Đen Đỏ", size = "40", imageUrls = listOf(imgAm270Black), price = 3590000.0, stock = 8, soldCount = 120, status = "ACTIVE"),
                    Variant(variantId = "am270_black_41", sku = "AH8050-002-41", color = "Đen Đỏ", size = "41", imageUrls = listOf(imgAm270Black), price = 3590000.0, stock = 12, soldCount = 180, status = "ACTIVE"),
                    Variant(variantId = "am270_black_42", sku = "AH8050-002-42", color = "Đen Đỏ", size = "42", imageUrls = listOf(imgAm270Black), price = 3590000.0, stock = 4, soldCount = 90, status = "ACTIVE"),
                    Variant(variantId = "am270_white_41", sku = "AH8050-100-41", color = "Trắng Tinh", size = "41", imageUrls = listOf(imgAm270White), price = 3590000.0, stock = 5, soldCount = 60, status = "ACTIVE")
                )
            ),

            // 3. Nike G.T. Cut 3
            Pair(
                Product(
                    productId = "nike_gt_cut_3_dv2913",
                    name = "Nike G.T. Cut 3",
                    brandId = "nike",
                    categoryId = "basketball",
                    description = "Đôi giày bóng rổ đầu tiên được trang bị bọt ZoomX cao cấp nhất của Nike, bứt tốc thần tốc, đổi hướng sắc lẹm và kiểm soát trận đấu hoàn hảo.",
                    basePrice = 5290000.0,
                    salePrice = 4490000.0,
                    gender = "MEN",
                    purpose = "Bóng rổ chuyên nghiệp, bứt tốc và cắt hướng đột ngột",
                    material = "Lưới kỹ thuật gia cường Flywire",
                    cushioning = "Bọt ZoomX toàn bàn chân",
                    terrain = "Sân bóng rổ (Court)",
                    imageUrls = listOf(imgGtCut3White),
                    rating = 4.9,
                    reviewCount = 88,
                    totalSoldCount = 180,
                    totalStockQuantity = 22,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "gtcut3_white_41", sku = "DV2913-100-41", color = "Trắng Xanh", size = "41", imageUrls = listOf(imgGtCut3White), price = 4490000.0, stock = 7, soldCount = 60, status = "ACTIVE"),
                    Variant(variantId = "gtcut3_white_42", sku = "DV2913-100-42", color = "Trắng Xanh", size = "42", imageUrls = listOf(imgGtCut3White), price = 4490000.0, stock = 10, soldCount = 80, status = "ACTIVE"),
                    Variant(variantId = "gtcut3_white_43", sku = "DV2913-100-43", color = "Trắng Xanh", size = "43", imageUrls = listOf(imgGtCut3White), price = 4490000.0, stock = 5, soldCount = 40, status = "ACTIVE")
                )
            ),

            // 4. Adidas Ultraboost 5
            Pair(
                Product(
                    productId = "adidas_ultraboost_5_white",
                    name = "Giày Ultraboost 5",
                    brandId = "adidas",
                    categoryId = "running",
                    description = "Giày chạy bộ đỉnh cao với công nghệ đệm Light BOOST V2 nhẹ hơn 30%, khung Torsion System chống vặn xoắn và mặt đế cao su Continental Rubber bám đường bền bỉ.",
                    basePrice = 4900000.0,
                    salePrice = 2450000.0,
                    gender = "UNISEX",
                    purpose = "Chạy bộ êm ái, bảo vệ khớp gối, đi bộ đường dài",
                    material = "Primeknit dệt kim ôm chân",
                    cushioning = "Light BOOST V2 hoàn trả năng lượng",
                    terrain = "Đường nhựa & Máy chạy",
                    imageUrls = listOf(imgUb5White, imgUb5Grey),
                    rating = 4.7,
                    reviewCount = 615,
                    totalSoldCount = 1500,
                    totalStockQuantity = 36,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "ub5_white_40", sku = "ID8810-WHT-40", color = "Trắng/Hồng", size = "40", imageUrls = listOf(imgUb5White), price = 2450000.0, stock = 15, soldCount = 500, status = "ACTIVE"),
                    Variant(variantId = "ub5_white_41", sku = "ID8810-WHT-41", color = "Trắng/Hồng", size = "41", imageUrls = listOf(imgUb5White), price = 2450000.0, stock = 1, soldCount = 700, status = "ACTIVE"),
                    Variant(variantId = "ub5_white_42", sku = "ID8810-WHT-42", color = "Trắng/Hồng", size = "42", imageUrls = listOf(imgUb5White), price = 2450000.0, stock = 0, soldCount = 300, status = "OUT_OF_STOCK"),
                    Variant(variantId = "ub5_grey_40", sku = "ID8812-GRY-40", color = "Xám Bạc", size = "40", imageUrls = listOf(imgUb5Grey), price = 2450000.0, stock = 20, soldCount = 120, status = "ACTIVE")
                )
            ),

            // 5. Adidas Samba OG
            Pair(
                Product(
                    productId = "adidas_samba_og_b75806",
                    name = "Adidas Samba OG",
                    brandId = "adidas",
                    categoryId = "lifestyle",
                    description = "Đôi sneaker biểu tượng vượt thời gian từ sân cỏ bóng đá thập niên 50 đến trào lưu thời trang đường phố toàn cầu, sở hữu chất liệu da cao cấp và mũi da lộn chữ T kinh điển.",
                    basePrice = 2700000.0,
                    salePrice = null,
                    gender = "UNISEX",
                    purpose = "Sneaker cổ điển vintage, đi học, đi làm, phối đồ casual",
                    material = "Da hạt cao cấp + Mũi da lộn chữ T",
                    cushioning = "Đế cao su gum truyền thống",
                    terrain = "Đường phố",
                    imageUrls = listOf(imgSambaWhite),
                    rating = 4.8,
                    reviewCount = 890,
                    totalSoldCount = 2200,
                    totalStockQuantity = 40,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "samba_white_39", sku = "B75806-39", color = "Trắng Đen", size = "39", imageUrls = listOf(imgSambaWhite), price = 2700000.0, stock = 6, soldCount = 400, status = "ACTIVE"),
                    Variant(variantId = "samba_white_40", sku = "B75806-40", color = "Trắng Đen", size = "40", imageUrls = listOf(imgSambaWhite), price = 2700000.0, stock = 12, soldCount = 600, status = "ACTIVE"),
                    Variant(variantId = "samba_white_41", sku = "B75806-41", color = "Trắng Đen", size = "41", imageUrls = listOf(imgSambaWhite), price = 2700000.0, stock = 15, soldCount = 800, status = "ACTIVE"),
                    Variant(variantId = "samba_white_42", sku = "B75806-42", color = "Trắng Đen", size = "42", imageUrls = listOf(imgSambaWhite), price = 2700000.0, stock = 7, soldCount = 400, status = "ACTIVE")
                )
            ),

            // 6. Adidas Supernova Rise
            Pair(
                Product(
                    productId = "adidas_supernova_rise_ig5840",
                    name = "Adidas Supernova Rise",
                    brandId = "adidas",
                    categoryId = "running",
                    description = "Mẫu giày chạy bộ phục hồi đạt nhiều giải thưởng quốc tế, sử dụng đệm Dreamstrike+ mật độ sinh học cao mang đến những bước chạy êm ái tối đa.",
                    basePrice = 3800000.0,
                    salePrice = 2990000.0,
                    gender = "MEN",
                    purpose = "Chạy bộ phục hồi, tập luyện hàng ngày êm ái",
                    material = "Sandwich Mesh đa lớp",
                    cushioning = "Bọt Dreamstrike+ mật độ cao",
                    terrain = "Đường nhựa",
                    imageUrls = listOf(imgSupernovaBlue),
                    rating = 4.6,
                    reviewCount = 95,
                    totalSoldCount = 210,
                    totalStockQuantity = 19,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "supernova_blue_40", sku = "IG5840-40", color = "Xanh Dạ Quang", size = "40", imageUrls = listOf(imgSupernovaBlue), price = 2990000.0, stock = 6, soldCount = 60, status = "ACTIVE"),
                    Variant(variantId = "supernova_blue_41", sku = "IG5840-41", color = "Xanh Dạ Quang", size = "41", imageUrls = listOf(imgSupernovaBlue), price = 2990000.0, stock = 9, soldCount = 100, status = "ACTIVE"),
                    Variant(variantId = "supernova_blue_42", sku = "IG5840-42", color = "Xanh Dạ Quang", size = "42", imageUrls = listOf(imgSupernovaBlue), price = 2990000.0, stock = 4, soldCount = 50, status = "ACTIVE")
                )
            ),

            // 7. ASICS Gel-Nimbus 26
            Pair(
                Product(
                    productId = "asics_gel_nimbus_26_1011b794",
                    name = "ASICS Gel-Nimbus 26",
                    brandId = "asics",
                    categoryId = "running",
                    description = "Được mệnh danh là 'đôi giày chạy êm nhất hành tinh', tích hợp công nghệ PureGEL mềm hơn 65% cùng bọt FF BLAST PLUS ECO đàn hồi bền vững.",
                    basePrice = 4290000.0,
                    salePrice = 3690000.0,
                    gender = "UNISEX",
                    purpose = "Chạy marathon, chạy cự ly dài 21km - 42km, tiếp đất siêu êm",
                    material = "Knit dệt co giãn cao cấp",
                    cushioning = "PureGEL + Bọt FF BLAST PLUS ECO",
                    terrain = "Đường nhựa (Road)",
                    imageUrls = listOf(imgNimbus26),
                    rating = 4.9,
                    reviewCount = 160,
                    totalSoldCount = 340,
                    totalStockQuantity = 26,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "nimbus26_blue_40", sku = "1011B794-400-40", color = "Xanh Navy", size = "40", imageUrls = listOf(imgNimbus26), price = 3690000.0, stock = 7, soldCount = 80, status = "ACTIVE"),
                    Variant(variantId = "nimbus26_blue_41", sku = "1011B794-400-41", color = "Xanh Navy", size = "41", imageUrls = listOf(imgNimbus26), price = 3690000.0, stock = 11, soldCount = 150, status = "ACTIVE"),
                    Variant(variantId = "nimbus26_blue_42", sku = "1011B794-400-42", color = "Xanh Navy", size = "42", imageUrls = listOf(imgNimbus26), price = 3690000.0, stock = 5, soldCount = 70, status = "ACTIVE"),
                    Variant(variantId = "nimbus26_blue_43", sku = "1011B794-400-43", color = "Xanh Navy", size = "43", imageUrls = listOf(imgNimbus26), price = 3690000.0, stock = 3, soldCount = 40, status = "ACTIVE")
                )
            ),

            // 8. ASICS Gel-Kayano 30
            Pair(
                Product(
                    productId = "asics_gel_kayano_30_1011b548",
                    name = "ASICS Gel-Kayano 30",
                    brandId = "asics",
                    categoryId = "running",
                    description = "Dòng giày chạy chuyên hỗ trợ kiểm soát chuyển động tốt nhất của ASICS, giải cứu bàn chân bẹt nhờ cấu trúc ổn định sinh học 4D GUIDANCE SYSTEM.",
                    basePrice = 4490000.0,
                    salePrice = 3890000.0,
                    gender = "MEN",
                    purpose = "Chạy bộ cho người chân bẹt, hỗ trợ chống sụp vòm cổ chân",
                    material = "Engineered Stretch Mesh",
                    cushioning = "4D GUIDANCE SYSTEM + PureGEL",
                    terrain = "Đường nhựa",
                    imageUrls = listOf(imgKayano30),
                    rating = 4.8,
                    reviewCount = 120,
                    totalSoldCount = 280,
                    totalStockQuantity = 21,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "kayano30_black_40", sku = "1011B548-002-40", color = "Đen Bạc", size = "40", imageUrls = listOf(imgKayano30), price = 3890000.0, stock = 5, soldCount = 70, status = "ACTIVE"),
                    Variant(variantId = "kayano30_black_41", sku = "1011B548-002-41", color = "Đen Bạc", size = "41", imageUrls = listOf(imgKayano30), price = 3890000.0, stock = 10, soldCount = 130, status = "ACTIVE"),
                    Variant(variantId = "kayano30_black_42", sku = "1011B548-002-42", color = "Đen Bạc", size = "42", imageUrls = listOf(imgKayano30), price = 3890000.0, stock = 6, soldCount = 80, status = "ACTIVE")
                )
            ),

            // 9. New Balance 574 Core
            Pair(
                Product(
                    productId = "nb_574_core_ml574evw",
                    name = "New Balance 574 Core",
                    brandId = "newbalance",
                    categoryId = "lifestyle",
                    description = "Thiết kế giày sneaker di sản đại diện cho phong cách tối giản thanh lịch của New Balance, kết hợp đệm bọt EVA êm ái cùng vành bảo vệ gót ENCAP danh tiếng.",
                    basePrice = 2395000.0,
                    salePrice = 1890000.0,
                    gender = "UNISEX",
                    purpose = "Sneaker cổ điển biểu tượng của New Balance, đi học, đi chơi",
                    material = "Da lộn Suede + Lưới dệt Mesh",
                    cushioning = "Viền ENCAP bền bỉ + Đệm EVA",
                    terrain = "Đô thị",
                    imageUrls = listOf(imgNb574),
                    rating = 4.7,
                    reviewCount = 520,
                    totalSoldCount = 1800,
                    totalStockQuantity = 37,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "nb574_grey_40", sku = "ML574EVW-40", color = "Xám Cổ Điển", size = "40", imageUrls = listOf(imgNb574), price = 1890000.0, stock = 8, soldCount = 450, status = "ACTIVE"),
                    Variant(variantId = "nb574_grey_41", sku = "ML574EVW-41", color = "Xám Cổ Điển", size = "41", imageUrls = listOf(imgNb574), price = 1890000.0, stock = 16, soldCount = 750, status = "ACTIVE"),
                    Variant(variantId = "nb574_grey_42", sku = "ML574EVW-42", color = "Xám Cổ Điển", size = "42", imageUrls = listOf(imgNb574), price = 1890000.0, stock = 7, soldCount = 380, status = "ACTIVE"),
                    Variant(variantId = "nb574_navy_41", sku = "ML574EVN-41", color = "Xanh Navy", size = "41", imageUrls = listOf(imgNb574), price = 1890000.0, stock = 6, soldCount = 220, status = "ACTIVE")
                )
            ),

            // 10. New Balance Fresh Foam X 1080v13
            Pair(
                Product(
                    productId = "nb_fresh_foam_x_1080_v13",
                    name = "New Balance Fresh Foam X 1080v13",
                    brandId = "newbalance",
                    categoryId = "running",
                    description = "Mẫu giày chạy bộ êm ái hàng đầu từ New Balance, sở hữu lớp đệm Fresh Foam X bồng bềnh dày dặn giúp mỗi sải chân nhẹ nhàng không áp lực.",
                    basePrice = 4195000.0,
                    salePrice = 3490000.0,
                    gender = "UNISEX",
                    purpose = "Chạy bộ êm ái, bồng bềnh, hỗ trợ phục hồi sau thi đấu",
                    material = "Engineered Air Mesh siêu nhẹ",
                    cushioning = "Fresh Foam X bồng bềnh như mây",
                    terrain = "Đường nhựa",
                    imageUrls = listOf(imgNb1080),
                    rating = 4.8,
                    reviewCount = 145,
                    totalSoldCount = 310,
                    totalStockQuantity = 21,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "nb1080_white_40", sku = "M108013W-40", color = "Trắng Xanh", size = "40", imageUrls = listOf(imgNb1080), price = 3490000.0, stock = 6, soldCount = 80, status = "ACTIVE"),
                    Variant(variantId = "nb1080_white_41", sku = "M108013W-41", color = "Trắng Xanh", size = "41", imageUrls = listOf(imgNb1080), price = 3490000.0, stock = 10, soldCount = 150, status = "ACTIVE"),
                    Variant(variantId = "nb1080_white_42", sku = "M108013W-42", color = "Trắng Xanh", size = "42", imageUrls = listOf(imgNb1080), price = 3490000.0, stock = 5, soldCount = 80, status = "ACTIVE")
                )
            ),

            // 11. Converse Chuck 70 Vintage Canvas
            Pair(
                Product(
                    productId = "converse_chuck_70_vintage_162058c",
                    name = "Converse Chuck 70 Vintage Canvas",
                    brandId = "converse",
                    categoryId = "lifestyle",
                    description = "Phiên bản nâng cấp cao cấp của đôi Chuck Taylor nguyên bản thập niên 70, vải canvas 12oz siêu dày dặn, đường may gia cố tỉ mỉ và đệm lót OrthoLite êm ái cả ngày.",
                    basePrice = 2100000.0,
                    salePrice = 1690000.0,
                    gender = "UNISEX",
                    purpose = "Học sinh, sinh viên, phối đồ retro, thời trang bất hủ",
                    material = "Vải Canvas 12oz dày dặn",
                    cushioning = "Lót giày đệm OrthoLite êm ái",
                    terrain = "Đường phố",
                    imageUrls = listOf(imgChuck70),
                    rating = 4.8,
                    reviewCount = 1200,
                    totalSoldCount = 3500,
                    totalStockQuantity = 44,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "c70_black_39", sku = "162058C-39", color = "Đen Cổ Cao", size = "39", imageUrls = listOf(imgChuck70), price = 1690000.0, stock = 10, soldCount = 800, status = "ACTIVE"),
                    Variant(variantId = "c70_black_40", sku = "162058C-40", color = "Đen Cổ Cao", size = "40", imageUrls = listOf(imgChuck70), price = 1690000.0, stock = 14, soldCount = 1100, status = "ACTIVE"),
                    Variant(variantId = "c70_black_41", sku = "162058C-41", color = "Đen Cổ Cao", size = "41", imageUrls = listOf(imgChuck70), price = 1690000.0, stock = 15, soldCount = 1200, status = "ACTIVE"),
                    Variant(variantId = "c70_black_42", sku = "162058C-42", color = "Đen Cổ Cao", size = "42", imageUrls = listOf(imgChuck70), price = 1690000.0, stock = 5, soldCount = 400, status = "ACTIVE")
                )
            ),

            // 12. Puma RS-X Efekt PRM
            Pair(
                Product(
                    productId = "puma_rs_x_efekt_390776",
                    name = "Puma RS-X Efekt PRM",
                    brandId = "puma",
                    categoryId = "lifestyle",
                    description = "Phong cách thiết kế vị lai ấn tượng với các lớp phủ xếp tầng hầm hố, tôn vinh công nghệ giảm chấn Running System (RS) kinh điển từ kho lưu trữ của Puma.",
                    basePrice = 3299000.0,
                    salePrice = 2290000.0,
                    gender = "UNISEX",
                    purpose = "Sneaker chunky cá tính, dạo phố, phong cách Y2K nổi bật",
                    material = "Lưới Mesh + Da nubuck cao cấp",
                    cushioning = "Running System (RS) đệm êm giảm chấn",
                    terrain = "Đường phố",
                    imageUrls = listOf(imgRsx),
                    rating = 4.6,
                    reviewCount = 130,
                    totalSoldCount = 410,
                    totalStockQuantity = 25,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "rsx_white_39", sku = "390776-01-39", color = "Trắng Xám", size = "39", imageUrls = listOf(imgRsx), price = 2290000.0, stock = 5, soldCount = 80, status = "ACTIVE"),
                    Variant(variantId = "rsx_white_40", sku = "390776-01-40", color = "Trắng Xám", size = "40", imageUrls = listOf(imgRsx), price = 2290000.0, stock = 8, soldCount = 120, status = "ACTIVE"),
                    Variant(variantId = "rsx_white_41", sku = "390776-01-41", color = "Trắng Xám", size = "41", imageUrls = listOf(imgRsx), price = 2290000.0, stock = 9, soldCount = 150, status = "ACTIVE"),
                    Variant(variantId = "rsx_white_42", sku = "390776-01-42", color = "Trắng Xám", size = "42", imageUrls = listOf(imgRsx), price = 2290000.0, stock = 3, soldCount = 60, status = "ACTIVE")
                )
            ),

            // 13. Puma Deviate Nitro 2
            Pair(
                Product(
                    productId = "puma_deviate_nitro_2_376807",
                    name = "Puma Deviate Nitro 2",
                    brandId = "puma",
                    categoryId = "running",
                    description = "Vũ khí chạy marathon cự ly dài được tối ưu tốc độ đỉnh cao với tấm đĩa carbon INNOPLATE và lớp đệm kép bọt NITRO Elite siêu phản hồi lực.",
                    basePrice = 4199000.0,
                    salePrice = 2990000.0,
                    gender = "MEN",
                    purpose = "Chạy đua marathon cự ly 10k-42k, trợ lực bật nảy tốc độ cao",
                    material = "Lưới Mesh kỹ thuật siêu thoáng khí",
                    cushioning = "NITRO Elite Foam + Tấm carbon INNOPLATE",
                    terrain = "Đường chạy (Road/Track)",
                    imageUrls = listOf(imgDeviate2),
                    rating = 4.8,
                    reviewCount = 110,
                    totalSoldCount = 250,
                    totalStockQuantity = 17,
                    status = "ACTIVE",
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Variant(variantId = "deviate_orange_40", sku = "376807-01-40", color = "Cam Lửa", size = "40", imageUrls = listOf(imgDeviate2), price = 2990000.0, stock = 5, soldCount = 70, status = "ACTIVE"),
                    Variant(variantId = "deviate_orange_41", sku = "376807-01-41", color = "Cam Lửa", size = "41", imageUrls = listOf(imgDeviate2), price = 2990000.0, stock = 8, soldCount = 120, status = "ACTIVE"),
                    Variant(variantId = "deviate_orange_42", sku = "376807-01-42", color = "Cam Lửa", size = "42", imageUrls = listOf(imgDeviate2), price = 2990000.0, stock = 4, soldCount = 60, status = "ACTIVE")
                )
            )
        )

        // Phân tách thành các batch nhỏ hơn (mỗi batch tối đa 30 writes) để đảm bảo an toàn tuyệt đối
        val chunks = catalogData.chunked(4)
        for ((chunkIndex, chunk) in chunks.withIndex()) {
            val batch = db.batch()
            for ((product, variants) in chunk) {
                val productRef = db.collection("products").document(product.productId)
                batch.set(productRef, product)

                for (variant in variants) {
                    val variantRef = productRef.collection("variants").document(variant.variantId)
                    batch.set(variantRef, variant)
                }
            }
            batch.commit().await()
            Log.d(TAG, "Đã lưu thành công nhóm sản phẩm ${chunkIndex + 1}/${chunks.size} vào Firestore")
        }
        Log.d(TAG, "Hoàn tất nạp toàn bộ 13 sản phẩm cùng các biến thể lên Firebase!")
    }
}
