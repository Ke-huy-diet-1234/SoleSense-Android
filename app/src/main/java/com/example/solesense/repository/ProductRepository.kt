package com.example.solesense.data.repository

import com.example.solesense.model.catalog.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val db = FirebaseFirestore.getInstance()

    // Lấy toàn bộ danh sách sản phẩm từ collection "products"
    // isForceRefresh: Nếu true sẽ bắt buộc lấy từ SERVER để cập nhật dữ liệu mới nhất
    suspend fun getProducts(isForceRefresh: Boolean = false): Result<List<Product>> {
        return try {
            val source = if (isForceRefresh) Source.SERVER else Source.DEFAULT
            val snapshot = db.collection("products")
                .get(source)
                .await()

            val products = snapshot.toObjects(Product::class.java)
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Lấy danh sách sản phẩm bán chạy nhất (Trending)
    suspend fun getTrendingProducts(limit: Long = 6, isForceRefresh: Boolean = false): Result<List<Product>> {
        return try {
            val source = if (isForceRefresh) Source.SERVER else Source.DEFAULT
            val snapshot = db.collection("products")
                .orderBy("totalSoldCount", Query.Direction.DESCENDING)
                .limit(limit)
                .get(source)
                .await()

            val products = snapshot.toObjects(Product::class.java)
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
