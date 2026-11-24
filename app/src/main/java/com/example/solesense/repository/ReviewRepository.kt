package com.example.solesense.repository

import android.content.Context
import android.net.Uri
import com.example.solesense.model.support.Review
import com.example.solesense.utils.CloudinaryHelper
import com.example.solesense.utils.UserSessionManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

class ReviewRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: UserSessionManager.getActiveUserId() ?: ""
    }

    /**
     * Lấy danh sách đánh giá đã duyệt (APPROVED) của một sản phẩm từ Firestore
     */
    suspend fun getProductReviews(productId: String): Result<List<Review>> {
        if (productId.isBlank()) return Result.success(emptyList())
        return try {
            val snap = db.collection("reviews")
                .whereEqualTo("productId", productId)
                .get()
                .await()
            val list = snap.documents.mapNotNull { doc ->
                doc.toObject(Review::class.java)?.copy(reviewId = doc.id)
            }
                .filter { it.status == "APPROVED" }
                .sortedByDescending { it.createdAt.seconds }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kiểm tra xem người dùng đã đánh giá sản phẩm trong đơn hàng này chưa
     */
    suspend fun getReviewedOrderProductKeys(userId: String = getCurrentUserId()): Set<String> {
        if (userId.isBlank()) return emptySet()
        return try {
            val snap = db.collection("reviews")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                val orderId = doc.getString("orderId") ?: ""
                val productId = doc.getString("productId") ?: ""
                if (orderId.isNotBlank() && productId.isNotBlank()) "${orderId}_${productId}" else null
            }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * Gửi đánh giá sản phẩm:
     * 1. Upload tối đa 4 ảnh từ thiết bị lên Cloudinary (hbxjrmsz / solesense_unsigned)
     * 2. Lưu Review vào Firestore collection `reviews`
     * 3. Tính lại trung bình cộng `rating` và `reviewCount` trên document `products/{productId}`
     */
    suspend fun submitProductReview(
        context: Context,
        orderId: String,
        orderItemId: String,
        productId: String,
        variantInfo: String,
        rating: Int,
        comment: String,
        selectedImageUris: List<Uri>
    ): Result<Review> {
        val currentUser = UserSessionManager.currentUser.value
        val uid = currentUser?.userId ?: getCurrentUserId()
        if (uid.isBlank()) {
            return Result.failure(IllegalStateException("Vui lòng đăng nhập để gửi đánh giá"))
        }

        if (selectedImageUris.size > CloudinaryHelper.MAX_REVIEW_IMAGES) {
            return Result.failure(IllegalArgumentException("Chỉ được tải lên tối đa 4 ảnh cho mỗi đánh giá"))
        }

        return try {
            // 1. Upload ảnh lên Cloudinary (nếu có chọn ảnh)
            val uploadedImageUrls = if (selectedImageUris.isNotEmpty()) {
                CloudinaryHelper.uploadMultipleReviewImages(
                    context = context,
                    uris = selectedImageUris.take(CloudinaryHelper.MAX_REVIEW_IMAGES)
                ).getOrElse { err ->
                    return Result.failure(err)
                }
            } else {
                emptyList()
            }

            // 2. Tạo document Review trên Firestore
            val now = Timestamp.now()
            val reviewDocId = "rev_${orderId}_${productId}_$uid"
            val review = Review(
                reviewId = reviewDocId,
                productId = productId,
                userId = uid,
                userName = currentUser?.fullName?.ifBlank { "Khách hàng" } ?: "Khách hàng",
                userAvatarUrl = currentUser?.avatarUrl,
                orderId = orderId,
                orderItemId = orderItemId,
                variantInfo = variantInfo,
                rating = rating.coerceIn(1, 5),
                comment = comment.trim(),
                imageUrls = uploadedImageUrls,
                status = "APPROVED",
                createdAt = now,
                updatedAt = now
            )

            db.collection("reviews").document(reviewDocId).set(review).await()

            // 3. Cập nhật rating trung bình và reviewCount trên Product
            try {
                val allReviews = getProductReviews(productId).getOrDefault(listOf(review))
                if (allReviews.isNotEmpty()) {
                    val avg = (allReviews.map { it.rating }.average() * 10.0).roundToInt() / 10.0
                    db.collection("products").document(productId).update(
                        mapOf(
                            "rating" to avg,
                            "reviewCount" to allReviews.size,
                            "updatedAt" to now
                        )
                    ).await()
                }
            } catch (_: Exception) {}

            // 4. Nếu đánh giá tiêu cực (1-2 sao), cảnh báo ngay cho bộ phận CSKH & Admin
            if (review.rating <= 2) {
                try {
                    NotificationRepository().notifyNegativeReviewToStaff(
                        reviewId = review.reviewId,
                        productId = productId,
                        userName = review.userName,
                        rating = review.rating,
                        comment = review.comment.orEmpty()
                    )
                } catch (_: Exception) {}
            }

            Result.success(review)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
