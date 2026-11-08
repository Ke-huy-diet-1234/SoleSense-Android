package com.example.solesense.repository

import com.example.solesense.model.user.Favorite
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FavoriteRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "mmZhfijntCf7BW2U0pf9VQbm4Xi1"
    }

    suspend fun isFavorite(
        productId: String,
        userId: String = getCurrentUserId()
    ): Boolean {
        if (productId.isBlank()) return false
        return try {
            val doc = db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(productId)
                .get()
                .await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleFavorite(
        productId: String,
        userId: String = getCurrentUserId()
    ): Result<Boolean> {
        if (productId.isBlank()) return Result.failure(IllegalArgumentException("ProductId rỗng"))
        return try {
            val favRef = db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(productId)

            val snapshot = favRef.get().await()
            if (snapshot.exists()) {
                favRef.delete().await()
                Result.success(false)
            } else {
                val favorite = Favorite(
                    productId = productId,
                    createdAt = Timestamp.now()
                )
                favRef.set(favorite).await()
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

