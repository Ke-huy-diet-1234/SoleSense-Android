package com.example.solesense.repository

import com.example.solesense.model.user.Favorite
import com.example.solesense.utils.UserSessionManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FavoriteRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid
            ?: UserSessionManager.getActiveUserId()
            ?: "mmZhfijntCf7BW2U0pf9VQbm4Xi1"
    }

    suspend fun loadFavoriteProductIds(userId: String = getCurrentUserId()): Set<String> {
        if (!UserSessionManager.isAuthenticated()) {
            _favoriteProductIds.value = emptySet()
            return emptySet()
        }
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("favorites")
                .get()
                .await()
            val ids = snapshot.documents.map { it.id }.toSet()
            _favoriteProductIds.value = ids
            ids
        } catch (e: Exception) {
            _favoriteProductIds.value
        }
    }

    suspend fun isFavorite(
        productId: String,
        userId: String = getCurrentUserId()
    ): Boolean {
        if (productId.isBlank() || !UserSessionManager.isAuthenticated()) return false
        if (_favoriteProductIds.value.contains(productId)) return true
        return try {
            val doc = db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(productId)
                .get()
                .await()
            val exists = doc.exists()
            if (exists) {
                _favoriteProductIds.value = _favoriteProductIds.value + productId
            }
            exists
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleFavorite(
        productId: String,
        userId: String = getCurrentUserId()
    ): Result<Boolean> {
        if (productId.isBlank()) return Result.failure(IllegalArgumentException("ProductId rỗng"))
        val currentlyFav = _favoriteProductIds.value.contains(productId)
        // Optimistic update ngay lập tức trên toàn ứng dụng
        _favoriteProductIds.value = if (currentlyFav) {
            _favoriteProductIds.value - productId
        } else {
            _favoriteProductIds.value + productId
        }

        return try {
            val favRef = db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(productId)

            val snapshot = favRef.get().await()
            if (snapshot.exists()) {
                favRef.delete().await()
                _favoriteProductIds.value = _favoriteProductIds.value - productId
                Result.success(false)
            } else {
                val favorite = Favorite(
                    productId = productId,
                    createdAt = Timestamp.now()
                )
                favRef.set(favorite).await()
                _favoriteProductIds.value = _favoriteProductIds.value + productId
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFavoritesBatch(
        productIds: Collection<String>,
        userId: String = getCurrentUserId()
    ): Result<Unit> {
        val validIds = productIds.filter { it.isNotBlank() }.distinct()
        if (validIds.isEmpty()) return Result.success(Unit)
        _favoriteProductIds.value = _favoriteProductIds.value + validIds
        return try {
            val favsRef = db.collection("users").document(userId).collection("favorites")
            val batch = db.batch()
            val now = Timestamp.now()
            validIds.forEach { pid ->
                batch.set(favsRef.document(pid), Favorite(productId = pid, createdAt = now))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private val _favoriteProductIds = MutableStateFlow<Set<String>>(emptySet())
        val favoriteProductIds: StateFlow<Set<String>> = _favoriteProductIds.asStateFlow()

        fun clearFavoritesState() {
            _favoriteProductIds.value = emptySet()
        }
    }
}
