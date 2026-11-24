package com.example.solesense.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sự kiện điều hướng chờ xử lý khi người dùng bấm vào một Thông báo màn hình (FCM Push Notification).
 */
data class PendingPushNavigation(
    val deepLinkRoute: String,
    val targetRole: String = "",
    val type: String = "",
    val referenceId: String? = null,
    val chatId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Kênh trung chuyển điều hướng (Deep-Link Bus) từ `MainActivity` (onCreate / onNewIntent)
 * sang `MainScreen`, `AdminDashboardScreen` và `SupportDashboardScreen`.
 */
object PushNavigationBus {
    private val _pendingNavigation = MutableStateFlow<PendingPushNavigation?>(null)
    val pendingNavigation: StateFlow<PendingPushNavigation?> = _pendingNavigation.asStateFlow()

    fun emit(navigation: PendingPushNavigation) {
        if (navigation.deepLinkRoute.isBlank()) return
        _pendingNavigation.value = navigation
    }

    fun consume() {
        _pendingNavigation.value = null
    }
}

/**
 * Theo dõi màn hình Chat đang mở trên foreground để tránh bắn banner thông báo
 * đè lên chính cuộc hội thoại mà người dùng / nhân viên đang xem trực tiếp.
 */
object PushForegroundTracker {
    @Volatile
    var activeChatId: String? = null
}
