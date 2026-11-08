package com.example.solesense.repository

import com.example.solesense.model.catalog.Category
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CategoryRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getActiveCategories(): Result<List<Category>> {
        return try {
            val snapshot = db.collection("categories")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()
            val categories = snapshot.toObjects(Category::class.java)
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

