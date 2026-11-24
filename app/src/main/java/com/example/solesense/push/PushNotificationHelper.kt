package com.example.solesense.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.solesense.R
import com.example.solesense.ui.MainActivity

/**
 * Bộ quản lý kênh thông báo (NotificationChannel) & hiển thị Thông báo màn hình Android
 * phân tách rõ theo 3 tác nhân: USER (Khách hàng), SUPPORT (Nhân viên CSKH), ADMIN (Quản trị viên).
 */
object PushNotificationHelper {

    // --- Kênh của USER (Khách hàng) ---
    const val CHANNEL_ORDERS = "channel_orders"
    const val CHANNEL_CHAT = "channel_chat"
    const val CHANNEL_PROMOTIONS = "channel_promotions"

    // --- Kênh của SUPPORT (Nhân viên CSKH) ---
    const val CHANNEL_SUPPORT_QUEUE = "channel_support_queue"
    const val CHANNEL_SUPPORT_CHAT = "channel_support_chat"

    // --- Kênh của ADMIN (Quản trị viên) ---
    const val CHANNEL_ADMIN_ORDERS = "channel_admin_orders"
    const val CHANNEL_ADMIN_ALERTS = "channel_admin_alerts"

    // --- Intent Extras cho Deep-Link ---
    const val EXTRA_DEEP_LINK_ROUTE = "extra_push_deep_link_route"
    const val EXTRA_TARGET_ROLE = "extra_push_target_role"
    const val EXTRA_TYPE = "extra_push_type"
    const val EXTRA_REFERENCE_ID = "extra_push_reference_id"
    const val EXTRA_CHAT_ID = "extra_push_chat_id"

    /**
     * Khởi tạo toàn bộ 7 NotificationChannel khi mở ứng dụng (`SoleSenseApplication.onCreate`).
     */
    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_ORDERS,
                "Đơn hàng & Thanh toán",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo trạng thái đơn hàng, giữ chỗ và xác nhận thanh toán VietQR"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_CHAT,
                "Tin nhắn Chăm sóc khách hàng",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi chuyên viên CSKH tiếp nhận và trả lời tin nhắn"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_PROMOTIONS,
                "Khuyến mãi & Mã giảm giá",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Chiến dịch ưu đãi, Flash Sale và Voucher mới từ SoleSense"
            },
            NotificationChannel(
                CHANNEL_SUPPORT_QUEUE,
                "Hàng chờ hỗ trợ (CSKH)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cảnh báo ngay khi có khách hàng yêu cầu gặp nhân viên hỗ trợ"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_SUPPORT_CHAT,
                "Tin nhắn khách hàng (CSKH)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tin nhắn mới từ khách hàng trong phiên CSKH bạn đang phụ trách"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_ADMIN_ORDERS,
                "Quản trị Đơn hàng & VietQR (Admin)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Đơn hàng COD mới, đơn VietQR chờ duyệt và đơn bị hủy cần hoàn tiền"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_ADMIN_ALERTS,
                "Cảnh báo Kho, Voucher & Đổi/Trả (Admin)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Cảnh báo sản phẩm sắp hết hàng, hết voucher và yêu cầu đổi trả mới"
            }
        )

        manager.createNotificationChannels(channels)
    }

    /**
     * Ánh xạ cặp (type, targetRole) -> đúng Android NotificationChannel ID.
     */
    fun resolveChannelId(type: String, targetRole: String): String {
        val normType = type.trim().uppercase()
        val normRole = targetRole.trim().uppercase()

        return when (normRole) {
            "ADMIN" -> when (normType) {
                "ADMIN_ORDER", "ORDER", "PAYMENT" -> CHANNEL_ADMIN_ORDERS
                else -> CHANNEL_ADMIN_ALERTS
            }
            "SUPPORT" -> when (normType) {
                "SUPPORT_CHAT" -> CHANNEL_SUPPORT_CHAT
                else -> CHANNEL_SUPPORT_QUEUE
            }
            else -> when (normType) {
                "CHAT", "SUPPORT" -> CHANNEL_CHAT
                "PROMOTION", "SYSTEM", "VOUCHER" -> CHANNEL_PROMOTIONS
                else -> CHANNEL_ORDERS
            }
        }
    }

    /**
     * Xây dựng đường dẫn Deep-Link chuẩn theo tác nhân nhận thông báo.
     */
    fun buildDeepLinkRoute(
        targetRole: String,
        type: String,
        referenceId: String? = null
    ): String {
        val normRole = targetRole.trim().uppercase()
        val normType = type.trim().uppercase()
        val cleanRef = referenceId?.trim().orEmpty()

        return when (normRole) {
            "ADMIN" -> when (normType) {
                "ADMIN_ORDER", "ORDER", "PAYMENT" -> "admin_order"
                "ADMIN_PRODUCT", "LOW_STOCK" -> "admin_product"
                "ADMIN_VOUCHER", "VOUCHER" -> "admin_voucher"
                "RETURN_REQUEST" -> "admin_ops_returns"
                "REVIEW_ALERT" -> "admin_ops_reviews"
                else -> "admin_notification"
            }
            "SUPPORT" -> when (normType) {
                "SUPPORT_CHAT" -> "support_assigned"
                "RETURN_REQUEST" -> "support_ops_returns"
                "REVIEW_ALERT" -> "support_ops_reviews"
                else -> "support_waiting"
            }
            else -> when (normType) {
                "ORDER", "PAYMENT", "RETURN_REVIEWED" -> {
                    if (cleanRef.isNotBlank()) "my_orders?orderId=$cleanRef" else "my_orders"
                }
                "PRODUCT_REVIEW" -> {
                    if (cleanRef.isNotBlank()) "product_detail/$cleanRef" else "notifications"
                }
                "CHAT", "SUPPORT" -> "chat"
                "PROMOTION", "VOUCHER" -> "explore"
                else -> "notifications"
            }
        }
    }

    /**
     * Hiển thị thông báo lên Thanh trạng thái / Heads-up Banner / Màn hình khóa của Android.
     */
    fun showSystemNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String,
        deepLinkRoute: String,
        targetRole: String,
        type: String,
        referenceId: String? = null,
        chatId: String? = null,
        notificationId: Int = (System.currentTimeMillis() and 0xFFFFFFF).toInt()
    ) {
        if (title.isBlank() && body.isBlank()) return

        // Kiểm tra quyền POST_NOTIFICATIONS trên Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK_ROUTE, deepLinkRoute)
            putExtra(EXTRA_TARGET_ROLE, targetRole)
            putExtra(EXTRA_TYPE, type)
            if (!referenceId.isNullOrBlank()) putExtra(EXTRA_REFERENCE_ID, referenceId)
            if (!chatId.isNullOrBlank()) putExtra(EXTRA_CHAT_ID, chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = if (channelId in setOf(
                CHANNEL_ORDERS,
                CHANNEL_CHAT,
                CHANNEL_SUPPORT_QUEUE,
                CHANNEL_SUPPORT_CHAT,
                CHANNEL_ADMIN_ORDERS
            )
        ) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shoe)
            .setContentTitle(title.ifBlank { "SoleSense" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setCategory(
                if (channelId == CHANNEL_CHAT || channelId == CHANNEL_SUPPORT_CHAT) {
                    NotificationCompat.CATEGORY_MESSAGE
                } else {
                    NotificationCompat.CATEGORY_STATUS
                }
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Người dùng vừa tắt quyền thông báo trong Cài đặt hệ thống
        }
    }
}
