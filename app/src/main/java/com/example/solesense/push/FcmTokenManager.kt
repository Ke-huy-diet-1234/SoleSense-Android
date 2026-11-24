package com.example.solesense.push

import android.util.Log
import com.example.solesense.model.user.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Quản lý vòng đời FCM Device Token và đăng ký FCM Topic theo vai trò (Role-Scoped Topics):
 * - CUSTOMER -> subscribe `all_customers`, hủy `role_support` & `role_admin`
 * - SUPPORT  -> subscribe `role_support`,  hủy `all_customers` & `role_admin`
 * - ADMIN    -> subscribe `role_admin`,    hủy `all_customers` & `role_support`
 *
 * Khi đăng xuất, xóa token hiện tại khỏi `users/{uid}` và hủy đăng ký toàn bộ Topic
 * để tuyệt đối không xảy ra rò rỉ thông báo chéo khi đổi tài khoản trên cùng một thiết bị.
 */
object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    const val TOPIC_ALL_CUSTOMERS = "all_customers"
    const val TOPIC_ROLE_SUPPORT = "role_support"
    const val TOPIC_ROLE_ADMIN = "role_admin"

    val ALL_TOPICS = listOf(TOPIC_ALL_CUSTOMERS, TOPIC_ROLE_SUPPORT, TOPIC_ROLE_ADMIN)

    /**
     * Trả về Topic tương ứng với vai trò của người dùng.
     */
    fun topicForRole(role: String): String {
        return when (role.trim().uppercase()) {
            "ADMIN" -> TOPIC_ROLE_ADMIN
            "SUPPORT" -> TOPIC_ROLE_SUPPORT
            else -> TOPIC_ALL_CUSTOMERS
        }
    }

    /**
     * Lấy FCM Token của thiết bị, lưu vào `users/{uid}` trên Firestore và đồng bộ Topic theo vai trò.
     */
    suspend fun syncTokenAndTopicsForUser(user: User) {
        if (user.userId.isBlank()) return
        try {
            val messaging = FirebaseMessaging.getInstance()
            val token = messaging.token.await()
            if (token.isNotBlank()) {
                saveTokenToFirestore(user.userId, token)
            }

            val targetTopic = topicForRole(user.role)
            ALL_TOPICS.forEach { topic ->
                try {
                    if (topic == targetTopic) {
                        messaging.subscribeToTopic(topic).await()
                    } else {
                        messaging.unsubscribeFromTopic(topic).await()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Topic sync ($topic) warning: ${e.message}")
                }
            }
            Log.d(TAG, "Synced FCM token & topic [$targetTopic] for uid=${user.userId}")
        } catch (e: Exception) {
            Log.w(TAG, "Không thể đồng bộ FCM token: ${e.message}")
        }
    }

    /**
     * Được gọi từ `SoleSenseMessagingService.onNewToken` khi Firebase cấp lại token mới.
     */
    suspend fun onNewTokenRefreshed(newToken: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (newToken.isBlank()) return
        try {
            saveTokenToFirestore(uid, newToken)
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi lưu token mới: ${e.message}")
        }
    }

    /**
     * Xóa token của thiết bị này khỏi tài khoản đang đăng xuất và hủy toàn bộ Topic.
     * Lưu ý: Phải gọi TRƯỚC khi gọi `FirebaseAuth.signOut()` để Firestore Rules còn xác thực `request.auth.uid`.
     */
    suspend fun clearTokenAndTopicsOnSignOut(userId: String?) {
        val messaging = FirebaseMessaging.getInstance()
        try {
            val token = try {
                messaging.token.await()
            } catch (_: Exception) {
                ""
            }

            if (!userId.isNullOrBlank() && token.isNotBlank()) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(userId).update(
                        mapOf(
                            "fcmTokens" to FieldValue.arrayRemove(token),
                            "fcmToken" to "",
                            "fcmUpdatedAt" to Timestamp.now()
                        )
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Không thể gỡ FCM token trên Firestore: ${e.message}")
                }
            }

            ALL_TOPICS.forEach { topic ->
                try {
                    messaging.unsubscribeFromTopic(topic).await()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi dọn dẹp FCM khi đăng xuất: ${e.message}")
        }
    }

    private suspend fun saveTokenToFirestore(userId: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        val payload = mapOf(
            "fcmToken" to token,
            "fcmTokens" to FieldValue.arrayUnion(token),
            "fcmUpdatedAt" to Timestamp.now()
        )
        db.collection("users").document(userId)
            .set(payload, SetOptions.merge())
            .await()
    }
}
