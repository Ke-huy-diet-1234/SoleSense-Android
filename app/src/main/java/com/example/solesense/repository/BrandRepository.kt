package com.example.solesense.repository

import com.example.solesense.model.catalog.Brand
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BrandRepository {
    private val db = FirebaseFirestore.getInstance()

    // Lấy toàn bộ danh sách thương hiệu đang hoạt động từ Firestore
    suspend fun getActiveBrands(): Result<List<Brand>> {
        return try {
            val snapshot = db.collection("brands")
                .whereEqualTo("status", "ACTIVE") // Chỉ lấy các hãng đang kích hoạt
                .get()
                .await()

            val brands = snapshot.toObjects(Brand::class.java)
            Result.success(brands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
