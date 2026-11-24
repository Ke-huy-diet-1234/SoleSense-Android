package com.example.solesense.push

import android.util.Log
import com.example.solesense.utils.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Dịch vụ nhận Firebase Cloud Messaging (FCM) cho SoleSense.
 *
 * Khi Push Relay Server gửi gói tin Data-Only ưu tiên cao (`android.priority = "high"`),
 * Android sẽ LUÔN gọi `onMessageReceived()` kể cả khi ứng dụng đang mở (Foreground),
 * đang chạy nền (Background) hoặc đã bị vuốt tắt hoàn toàn (Force Killed).
 */
class SoleSenseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nhận FCM token mới từ Firebase")
        serviceScope.launch {
            FcmTokenManager.onNewTokenRefreshed(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data

        val title = data["title"]
            ?: remoteMessage.notification?.title
            ?: "SoleSense"
        val body = data["body"]
            ?: remoteMessage.notification?.body
            ?: ""

        if (title.isBlank() && body.isBlank()) return

        val type = data["type"].orEmpty()
        val targetRole = data["targetRole"].orEmpty()
        val referenceId = data["referenceId"]?.takeIf { it.isNotBlank() }
        val chatId = data["chatId"]?.takeIf { it.isNotBlank() }

        // 1. Bảo vệ chéo vai trò (Role Guard): nếu máy đã đăng xuất hoặc đang đăng nhập vai trò khác, bỏ qua gói tin
        val signedInUid = FirebaseAuth.getInstance().currentUser?.uid
        val activeUser = UserSessionManager.currentUser.value
        if (signedInUid == null && targetRole.isNotBlank()) {
            return
        }
        if (activeUser != null && targetRole.isNotBlank()) {
            val activeRole = activeUser.role.uppercase()
            val expectedRole = targetRole.uppercase()
            if (expectedRole in setOf("ADMIN", "SUPPORT") && activeRole != expectedRole) {
                Log.d(TAG, "Bỏ qua thông báo dành cho [$expectedRole] vì tài khoản hiện tại là [$activeRole]")
                return
            }
        }

        // 2. Chống bắn đè banner khi người dùng/CSKH đang mở trực tiếp đúng phòng chat đó
        if (!chatId.isNullOrBlank() && PushForegroundTracker.activeChatId == chatId) {
            return
        }

        // 3. Xác định kênh NotificationChannel và tuyến Deep-Link đích
        val channelId = data["channelId"]?.takeIf { it.isNotBlank() }
            ?: PushNotificationHelper.resolveChannelId(type = type, targetRole = targetRole)

        val deepLinkRoute = data["deepLinkRoute"]?.takeIf { it.isNotBlank() }
            ?: PushNotificationHelper.buildDeepLinkRoute(
                targetRole = targetRole,
                type = type,
                referenceId = referenceId
            )

        // Đảm bảo NotificationChannel đã tồn tại ngay cả khi Service được đánh thức lúc app đang tắt
        PushNotificationHelper.createNotificationChannels(applicationContext)

        val notifId = (data["notificationId"]?.hashCode()
            ?: "${type}_${referenceId}_${System.currentTimeMillis()}".hashCode()) and 0x7FFFFFFF

        PushNotificationHelper.showSystemNotification(
            context = applicationContext,
            title = title,
            body = body,
            channelId = channelId,
            deepLinkRoute = deepLinkRoute,
            targetRole = targetRole,
            type = type,
            referenceId = referenceId,
            chatId = chatId,
            notificationId = notifId
        )
    }

    companion object {
        private const val TAG = "SoleSenseFCM"
    }
}
