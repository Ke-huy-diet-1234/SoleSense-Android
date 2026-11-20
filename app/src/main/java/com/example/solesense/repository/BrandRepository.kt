package com.example.solesense.repository

import com.example.solesense.model.catalog.Brand
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BrandRepository {
    private val db = FirebaseFirestore.getInstance()

    companion object {
        val OFFICIAL_BRAND_ORDER = listOf(
            "nike",
            "adidas",
            "asics",
            "newbalance",
            "converse",
            "puma",
            "reebok"
        )
    }

    /**
     * Lấy danh sách thương hiệu đang hoạt động từ Firestore,
     * đồng thời tự động dọn sạch các document thương hiệu bị lỗi/trùng lặp cũ (VD: new_balance hoặc hãng rỗng).
     */
    suspend fun getActiveBrands(): Result<List<Brand>> {
        return try {
            val snapshot = db.collection("brands")
                .get()
                .await()

            val validBrands = mutableListOf<Brand>()
            val seenNames = mutableSetOf<String>()

            for (doc in snapshot.documents) {
                val docId = doc.id.trim().lowercase()
                val brand = doc.toObject(Brand::class.java)?.copy(brandId = doc.id)

                val isOfficial = docId in OFFICIAL_BRAND_ORDER
                val hasValidName = !brand?.name.isNullOrBlank() && brand?.name != "."

                if (!isOfficial || !hasValidName || brand == null) {
                    // Xóa vĩnh viễn hãng bị lỗi hoặc bản trùng "new_balance" khỏi Firestore
                    try {
                        doc.reference.delete()
                    } catch (_: Exception) {}
                    continue
                }

                if (brand.status.equals("ACTIVE", ignoreCase = true)) {
                    val normName = brand.name.trim().lowercase()
                    if (seenNames.add(normName)) {
                        validBrands.add(brand)
                    } else {
                        try {
                            doc.reference.delete()
                        } catch (_: Exception) {}
                    }
                }
            }

            val sorted = validBrands.sortedBy { b ->
                val idx = OFFICIAL_BRAND_ORDER.indexOf(b.brandId.lowercase())
                if (idx >= 0) idx else 999
            }

            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
