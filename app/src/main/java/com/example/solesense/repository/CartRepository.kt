package com.example.solesense.repository

import com.example.solesense.model.catalog.Variant
import com.example.solesense.model.order.Cart
import com.example.solesense.model.order.CartItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class CartRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid ?: com.example.solesense.utils.UserSessionManager.getActiveUserId()

    suspend fun getCartItems(userId: String? = getCurrentUserId()): Result<List<CartItem>> {
        val uid = userId ?: run {
            _distinctCartItemCount.value = 0
            return Result.success(emptyList())
        }
        return try {
            val snapshot = db.collection("carts")
                .document(uid)
                .collection("items")
                .get()
                .await()
            val rawItems = snapshot.toObjects(CartItem::class.java)
                .sortedByDescending { it.addedAt.seconds }

            // Đồng bộ lại maxStock thực tế từ variants trên Firestore nếu có
            val syncedItems = rawItems.map { item ->
                val actualStock = fetchVariantStock(item.productId, item.variantId, item.maxStock)
                val safeMax = if (actualStock > 0) actualStock else item.maxStock.coerceAtLeast(1)
                val safeQty = item.quantity.coerceIn(1, safeMax)
                item.copy(quantity = safeQty, maxStock = safeMax)
            }

            // Bộ đếm trên BottomBar chỉ tính số item giày khác nhau đã thêm (không tính tổng số lượng từng item)
            _distinctCartItemCount.value = syncedItems.size
            Result.success(syncedItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchVariantStock(productId: String, variantId: String, fallback: Int): Int {
        if (productId.isBlank() || variantId.isBlank()) return fallback.coerceAtLeast(1)
        return try {
            val doc = db.collection("products")
                .document(productId)
                .collection("variants")
                .document(variantId)
                .get()
                .await()
            val variant = doc.toObject(Variant::class.java)
            variant?.stock?.takeIf { it > 0 } ?: fallback.coerceAtLeast(1)
        } catch (_: Exception) {
            fallback.coerceAtLeast(1)
        }
    }

    suspend fun getCartItemCount(userId: String? = getCurrentUserId()): Int {
        val items = getCartItems(userId).getOrDefault(emptyList())
        return items.size
    }

    suspend fun addOrUpdateCartItem(
        cartItem: CartItem,
        userId: String? = getCurrentUserId()
    ): Result<List<CartItem>> {
        val uid = userId ?: return Result.failure(IllegalStateException("Vui lòng đăng nhập để thêm vào giỏ hàng"))
        return try {
            val cartDocRef = db.collection("carts").document(uid)
            val itemId = cartItem.itemId.ifBlank { cartItem.variantId }
            val itemDocRef = cartDocRef.collection("items").document(itemId)

            val existingSnap = itemDocRef.get().await()
            val now = Timestamp.now()
            val maxAllowed = cartItem.maxStock.coerceAtLeast(1)

            val updatedItem = if (existingSnap.exists()) {
                val existing = existingSnap.toObject(CartItem::class.java)
                val newQty = ((existing?.quantity ?: 0) + cartItem.quantity).coerceAtMost(maxAllowed)
                cartItem.copy(
                    itemId = itemId,
                    quantity = newQty,
                    maxStock = maxAllowed,
                    addedAt = now
                )
            } else {
                cartItem.copy(
                    itemId = itemId,
                    quantity = cartItem.quantity.coerceAtMost(maxAllowed),
                    maxStock = maxAllowed,
                    addedAt = now
                )
            }

            val batch = db.batch()
            batch.set(cartDocRef, Cart(userId = uid, updatedAt = now))
            batch.set(itemDocRef, updatedItem)
            batch.commit().await()

            getCartItems(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateItemQuantity(
        itemId: String,
        newQuantity: Int,
        maxStock: Int = 99,
        userId: String? = getCurrentUserId()
    ): Result<List<CartItem>> {
        val uid = userId ?: return Result.failure(IllegalStateException("Chưa đăng nhập"))
        return try {
            val itemRef = db.collection("carts").document(uid).collection("items").document(itemId)
            val clampedQty = newQuantity.coerceAtMost(maxStock.coerceAtLeast(1))
            if (clampedQty <= 0) {
                itemRef.delete().await()
            } else {
                itemRef.update(mapOf("quantity" to clampedQty, "maxStock" to maxStock.coerceAtLeast(1))).await()
            }
            db.collection("carts").document(uid).update("updatedAt", Timestamp.now()).await()
            getCartItems(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeCartItem(
        itemId: String,
        userId: String? = getCurrentUserId()
    ): Result<List<CartItem>> {
        val uid = userId ?: return Result.failure(IllegalStateException("Chưa đăng nhập"))
        return try {
            db.collection("carts").document(uid).collection("items").document(itemId).delete().await()
            getCartItems(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMultipleCartItems(
        itemIds: Collection<String>,
        userId: String? = getCurrentUserId()
    ): Result<List<CartItem>> {
        val uid = userId ?: return Result.failure(IllegalStateException("Chưa đăng nhập"))
        return try {
            val itemsRef = db.collection("carts").document(uid).collection("items")
            val batch = db.batch()
            itemIds.forEach { id ->
                if (id.isNotBlank()) {
                    batch.delete(itemsRef.document(id))
                }
            }
            batch.commit().await()
            getCartItems(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCart(userId: String? = getCurrentUserId()) {
        val uid = userId ?: return
        try {
            val itemsRef = db.collection("carts").document(uid).collection("items")
            val snap = itemsRef.get().await()
            val batch = db.batch()
            for (doc in snap.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            _distinctCartItemCount.value = 0
        } catch (_: Exception) {
        }
    }

    companion object {
        private val _distinctCartItemCount = MutableStateFlow(0)
        val distinctCartItemCount: StateFlow<Int> = _distinctCartItemCount.asStateFlow()

        private val _unreadMessageCount = MutableStateFlow(1)
        val unreadMessageCount: StateFlow<Int> = _unreadMessageCount.asStateFlow()

        fun setUnreadMessageCount(count: Int) {
            _unreadMessageCount.value = count.coerceAtLeast(0)
        }

        fun resetBadgeCounts() {
            _distinctCartItemCount.value = 0
        }
    }
}
