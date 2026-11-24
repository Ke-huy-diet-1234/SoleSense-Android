package com.example.solesense.push

import android.util.Log
import com.example.solesense.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Trình gửi yêu cầu đẩy Thông báo Màn hình (FCM HTTP v1) thông qua Push Relay Server miễn phí (Vercel / Render).
 *
 * - Mọi lệnh gọi đều chạy bất đồng bộ trên `Dispatchers.IO` (fire-and-forget, có try/catch an toàn),
 *   đảm bảo lỗi mạng hoặc khi chưa deploy Push Relay Server KHÔNG BAO GIỜ làm gián đoạn nghiệp vụ đặt hàng/chat.
 * - Đính kèm `Authorization: Bearer <Firebase ID Token>` để server xác thực đúng danh tính & vai trò của người gửi.
 */
object PushDispatchClient {

    private const val TAG = "PushDispatchClient"
    private val dispatchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isConfigured(): Boolean = BuildConfig.PUSH_RELAY_URL.isNotBlank()

    /**
     * Đẩy thông báo màn hình tới đích danh 1 tài khoản (USER hoặc nhân viên SUPPORT phụ trách).
     */
    fun pushToUser(
        targetUserId: String,
        title: String,
        body: String,
        type: String,
        targetRole: String = "CUSTOMER",
        referenceId: String? = null,
        chatId: String? = null,
        notificationId: String? = null
    ) {
        if (targetUserId.isBlank() || title.isBlank()) return
        dispatchAsync(
            targetUserId = targetUserId,
            topic = null,
            title = title,
            body = body,
            type = type,
            targetRole = targetRole,
            referenceId = referenceId,
            chatId = chatId,
            notificationId = notificationId
        )
    }

    /**
     * Đẩy thông báo màn hình tới toàn bộ một Topic theo vai trò (`role_admin`, `role_support`, `all_customers`).
     */
    fun pushToTopic(
        topic: String,
        title: String,
        body: String,
        type: String,
        targetRole: String,
        referenceId: String? = null,
        chatId: String? = null,
        notificationId: String? = null
    ) {
        if (topic.isBlank() || title.isBlank()) return
        dispatchAsync(
            targetUserId = null,
            topic = topic,
            title = title,
            body = body,
            type = type,
            targetRole = targetRole,
            referenceId = referenceId,
            chatId = chatId,
            notificationId = notificationId
        )
    }

    private fun dispatchAsync(
        targetUserId: String?,
        topic: String?,
        title: String,
        body: String,
        type: String,
        targetRole: String,
        referenceId: String?,
        chatId: String?,
        notificationId: String?
    ) {
        val baseUrl = BuildConfig.PUSH_RELAY_URL.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            Log.i(TAG, "PUSH_RELAY_URL chưa được cấu hình trong gradle.properties/local.properties — bỏ qua gửi Push ($title)")
            return
        }

        dispatchScope.launch {
            try {
                val idToken = FirebaseAuth.getInstance().currentUser
                    ?.getIdToken(false)
                    ?.await()
                    ?.token
                if (idToken.isNullOrBlank()) {
                    Log.w(TAG, "Bỏ qua gửi Push vì người dùng chưa có Firebase ID Token")
                    return@launch
                }

                val channelId = PushNotificationHelper.resolveChannelId(type = type, targetRole = targetRole)
                val deepLinkRoute = PushNotificationHelper.buildDeepLinkRoute(
                    targetRole = targetRole,
                    type = type,
                    referenceId = referenceId
                )

                val payload = JSONObject().apply {
                    if (!targetUserId.isNullOrBlank()) put("targetUserId", targetUserId)
                    if (!topic.isNullOrBlank()) put("topic", topic)
                    put("title", title)
                    put("body", body)
                    put("type", type)
                    put("targetRole", targetRole)
                    put("channelId", channelId)
                    put("deepLinkRoute", deepLinkRoute)
                    if (!referenceId.isNullOrBlank()) put("referenceId", referenceId)
                    if (!chatId.isNullOrBlank()) put("chatId", chatId)
                    if (!notificationId.isNullOrBlank()) put("notificationId", notificationId)
                }

                val endpoint = if (baseUrl.endsWith("/api/push")) baseUrl else "$baseUrl/api/push"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    doInput = true
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Authorization", "Bearer $idToken")
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    Log.d(TAG, "Gửi FCM thành công (HTTP $code): $title -> ${targetUserId ?: "topic:$topic"}")
                } else {
                    val err = try {
                        conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                    } catch (_: Exception) {
                        "HTTP $code"
                    }
                    Log.w(TAG, "Push Relay trả lỗi ($code): $err")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Không thể kết nối Push Relay Server: ${e.message}")
            }
        }
    }
}
