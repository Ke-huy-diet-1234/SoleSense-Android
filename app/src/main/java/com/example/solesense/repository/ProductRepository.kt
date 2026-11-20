package com.example.solesense.data.repository

import com.example.solesense.model.catalog.InventoryLog
import com.example.solesense.model.catalog.Product
import com.example.solesense.model.catalog.Variant
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val db = FirebaseFirestore.getInstance()

    // Lấy danh sách sản phẩm đang kinh doanh cho Khách hàng (ẩn các sản phẩm DISABLED)
    suspend fun getProducts(isForceRefresh: Boolean = false): Result<List<Product>> {
        return try {
            val source = if (isForceRefresh) Source.SERVER else Source.DEFAULT
            val snapshot = db.collection("products")
                .get(source)
                .await()

            val products = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(productId = doc.id)
            }.filter { it.status.uppercase() != "DISABLED" }
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Lấy toàn bộ sản phẩm (bao gồm cả ACTIVE, OUT_OF_STOCK, DISABLED) cho Admin quản lý
    suspend fun getAllProductsForAdmin(): Result<List<Product>> {
        return try {
            val snapshot = db.collection("products")
                .get(Source.SERVER)
                .await()
            val products = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(productId = doc.id)
            }.sortedByDescending { it.updatedAt.seconds }
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Lấy danh sách sản phẩm bán chạy nhất (Trending - ẩn sản phẩm DISABLED)
    suspend fun getTrendingProducts(limit: Long = 6, isForceRefresh: Boolean = false): Result<List<Product>> {
        return try {
            val source = if (isForceRefresh) Source.SERVER else Source.DEFAULT
            val snapshot = db.collection("products")
                .orderBy("totalSoldCount", Query.Direction.DESCENDING)
                .limit(limit + 4)
                .get(source)
                .await()

            val products = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(productId = doc.id)
            }
                .filter { it.status.uppercase() != "DISABLED" }
                .take(limit.toInt())
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Lấy chi tiết 1 sản phẩm theo ID
    suspend fun getProductById(productId: String): Result<Product> {
        return try {
            val doc = db.collection("products")
                .document(productId)
                .get()
                .await()
            val product = doc.toObject(Product::class.java)?.copy(productId = doc.id)
                ?: throw IllegalArgumentException("Không tìm thấy sản phẩm")
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Lấy danh sách biến thể (variants) của sản phẩm từ subcollection products/{productId}/variants
    suspend fun getVariantsByProductId(productId: String): Result<List<Variant>> {
        return try {
            val snapshot = db.collection("products")
                .document(productId)
                .collection("variants")
                .get()
                .await()
            val variants = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Variant::class.java)?.copy(variantId = doc.id)
            }
            Result.success(variants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Admin: Thêm mới hoặc cập nhật thông tin sản phẩm
    suspend fun saveOrUpdateProduct(product: Product): Result<Product> {
        return try {
            val now = Timestamp.now()
            val docRef = if (product.productId.isBlank()) {
                db.collection("products").document()
            } else {
                db.collection("products").document(product.productId)
            }
            val finalProduct = product.copy(
                productId = docRef.id,
                updatedAt = now
            )
            docRef.set(finalProduct, SetOptions.merge()).await()
            Result.success(finalProduct)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Admin: Ẩn / Hiện sản phẩm (ACTIVE <-> DISABLED)
    suspend fun toggleProductStatus(productId: String, currentStatus: String, totalStock: Int): Result<String> {
        return try {
            val nextStatus = if (currentStatus.uppercase() == "DISABLED") {
                if (totalStock > 0) "ACTIVE" else "OUT_OF_STOCK"
            } else {
                "DISABLED"
            }
            db.collection("products").document(productId).update(
                mapOf(
                    "status" to nextStatus,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
            Result.success(nextStatus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Admin: Nhập kho / Điều chỉnh tồn kho biến thể bằng Firestore Transaction + Ghi InventoryLog + Đồng bộ Product cha
    suspend fun adjustVariantStockWithTransaction(
        productId: String,
        variantId: String,
        deltaStock: Int,
        reason: String,
        adminId: String
    ): Result<Int> {
        if (deltaStock == 0) return Result.failure(IllegalArgumentException("Số lượng thay đổi phải khác 0"))
        return try {
            val updatedVariantStock = db.runTransaction { transaction ->
                val now = Timestamp.now()
                val productRef = db.collection("products").document(productId)
                val variantRef = productRef.collection("variants").document(variantId)

                val pSnap = transaction.get(productRef)
                val vSnap = transaction.get(variantRef)
                if (!vSnap.exists()) {
                    throw IllegalStateException("Không tìm thấy biến thể cần cập nhật kho!")
                }

                val currentVariantStock = vSnap.getLong("stock")?.toInt() ?: 0
                val newVariantStock = (currentVariantStock + deltaStock).coerceAtLeast(0)
                val actualDelta = newVariantStock - currentVariantStock

                val currentTotalStock = pSnap.getLong("totalStockQuantity")?.toInt() ?: 0
                val newTotalStock = (currentTotalStock + actualDelta).coerceAtLeast(0)
                val currentProductStatus = pSnap.getString("status") ?: "ACTIVE"
                val newProductStatus = when {
                    currentProductStatus == "DISABLED" -> "DISABLED"
                    newTotalStock > 0 -> "ACTIVE"
                    else -> "OUT_OF_STOCK"
                }

                transaction.update(
                    variantRef,
                    mapOf(
                        "stock" to newVariantStock,
                        "status" to if (newVariantStock > 0) "ACTIVE" else "OUT_OF_STOCK",
                        "updatedAt" to now
                    )
                )

                transaction.update(
                    productRef,
                    mapOf(
                        "totalStockQuantity" to newTotalStock,
                        "status" to newProductStatus,
                        "updatedAt" to now
                    )
                )

                val logRef = db.collection("inventory_logs").document()
                val log = InventoryLog(
                    logId = logRef.id,
                    variantId = variantId,
                    productId = productId,
                    type = if (deltaStock > 0) "STOCK_IN" else "MANUAL_ADJUSTMENT",
                    quantity = actualDelta,
                    previousStock = currentVariantStock,
                    newStock = newVariantStock,
                    reason = reason.ifBlank { if (deltaStock > 0) "Admin nhập thêm hàng vào kho" else "Admin điều chỉnh giảm tồn kho" },
                    createdBy = adminId,
                    createdAt = now
                )
                transaction.set(logRef, log)

                newVariantStock
            }.await()
            Result.success(updatedVariantStock)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Admin: Thêm biến thể mới (Màu + Size + SKU + Giá + Tồn kho) và đồng bộ tổng tồn kho Product cha
    suspend fun addVariantToProduct(
        productId: String,
        color: String,
        size: String,
        sku: String,
        price: Double,
        initialStock: Int,
        imageUrl: String,
        adminId: String
    ): Result<Variant> {
        return try {
            val now = Timestamp.now()
            val productRef = db.collection("products").document(productId)
            val existingVariants = getVariantsByProductId(productId).getOrDefault(emptyList())
            val duplicate = existingVariants.any {
                it.color.equals(color.trim(), ignoreCase = true) &&
                        it.size.equals(size.trim(), ignoreCase = true)
            }
            if (duplicate) {
                return Result.failure(IllegalArgumentException("Biến thể Màu $color - Size $size đã tồn tại!"))
            }

            val variantRef = productRef.collection("variants").document()
            val safeStock = initialStock.coerceAtLeast(0)
            val newVariant = Variant(
                variantId = variantRef.id,
                sku = sku.ifBlank { "${productId.take(6).uppercase()}-${size.trim()}" },
                color = color.trim(),
                size = size.trim(),
                price = price,
                stock = safeStock,
                soldCount = 0,
                imageUrls = if (imageUrl.isNotBlank()) listOf(imageUrl.trim()) else emptyList(),
                status = if (safeStock > 0) "ACTIVE" else "OUT_OF_STOCK",
                createdAt = now,
                updatedAt = now
            )

            db.runTransaction { transaction ->
                val pSnap = transaction.get(productRef)
                val currentTotalStock = pSnap.getLong("totalStockQuantity")?.toInt() ?: 0
                val currentStatus = pSnap.getString("status") ?: "ACTIVE"
                val newTotalStock = currentTotalStock + safeStock
                val newStatus = when {
                    currentStatus == "DISABLED" -> "DISABLED"
                    newTotalStock > 0 -> "ACTIVE"
                    else -> "OUT_OF_STOCK"
                }

                transaction.set(variantRef, newVariant)
                transaction.update(
                    productRef,
                    mapOf(
                        "totalStockQuantity" to newTotalStock,
                        "status" to newStatus,
                        "updatedAt" to now
                    )
                )

                if (safeStock > 0) {
                    val logRef = db.collection("inventory_logs").document()
                    val log = InventoryLog(
                        logId = logRef.id,
                        variantId = newVariant.variantId,
                        productId = productId,
                        type = "STOCK_IN",
                        quantity = safeStock,
                        previousStock = 0,
                        newStock = safeStock,
                        reason = "Khởi tạo biến thể mới (${newVariant.color} - Size ${newVariant.size})",
                        createdBy = adminId,
                        createdAt = now
                    )
                    transaction.set(logRef, log)
                }
            }.await()

            Result.success(newVariant)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
