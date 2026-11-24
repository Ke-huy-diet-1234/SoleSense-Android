package com.example.solesense.utils

import com.example.solesense.model.user.User
import com.example.solesense.model.user.toSafeUser
import com.example.solesense.push.FcmTokenManager
import com.example.solesense.repository.AuthRepositoryImpl
import com.example.solesense.repository.CartRepository
import com.example.solesense.repository.FavoriteRepository
import com.example.solesense.repository.NotificationRepository
import com.example.solesense.ui.checkout.CheckoutSessionHolder
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AppAuthSessionState {
    data object Checking : AppAuthSessionState
    data object Guest : AppAuthSessionState
    data class Authenticated(val user: User) : AppAuthSessionState
}

object UserSessionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()
    private val authRepository: AuthRepositoryImpl
        get() = AuthRepositoryImpl(FirebaseAuth.getInstance(), db)

    private var userDocListener: ListenerRegistration? = null

    private val _sessionState = MutableStateFlow<AppAuthSessionState>(AppAuthSessionState.Checking)
    val sessionState: StateFlow<AppAuthSessionState> = _sessionState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        refreshSession()
    }

    fun isAuthenticated(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null || _currentUser.value != null
    }

    fun getActiveUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?: _currentUser.value?.userId?.takeIf { it.isNotBlank() }
    }

    private fun attachRealtimeUserListener(uid: String) {
        if (uid.isBlank()) return
        userDocListener?.remove()
        userDocListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val updatedUser = snapshot.toSafeUser(uid)
                // Nếu Admin khóa tài khoản (BLOCKED) trong lúc người dùng đang sử dụng, lập tức thu hồi phiên đăng nhập
                if (updatedUser.status.uppercase() == "BLOCKED") {
                    signOut()
                    return@addSnapshotListener
                }
                val current = _currentUser.value
                val mergedUser = if (current != null) {
                    updatedUser.copy(
                        fullName = updatedUser.fullName.ifBlank { current.fullName },
                        email = updatedUser.email.ifBlank { current.email },
                        avatarUrl = updatedUser.avatarUrl?.takeIf { it.isNotBlank() }
                            ?: current.avatarUrl?.takeIf { it.isNotBlank() }
                    )
                } else {
                    updatedUser
                }
                val roleChanged = current != null && !current.role.equals(mergedUser.role, ignoreCase = true)
                _currentUser.value = mergedUser
                _sessionState.value = AppAuthSessionState.Authenticated(mergedUser)
                if (roleChanged) {
                    scope.launch {
                        FcmTokenManager.syncTokenAndTopicsForUser(mergedUser)
                    }
                }
            }
    }

    fun refreshSession() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            userDocListener?.remove()
            userDocListener = null
            _currentUser.value = null
            _sessionState.value = AppAuthSessionState.Guest
            CartRepository.resetBadgeCounts()
            FavoriteRepository.clearFavoritesState()
            NotificationRepository.updateUnreadCount(0)
            CheckoutSessionHolder.clearSession()
            return
        }

        attachRealtimeUserListener(firebaseUser.uid)

        scope.launch {
            val user = authRepository.getCurrentSessionUser()
            if (user != null) {
                _currentUser.value = user
                _sessionState.value = AppAuthSessionState.Authenticated(user)
                syncUserBadgesAndFavorites(user.userId)
                FcmTokenManager.syncTokenAndTopicsForUser(user)
            } else {
                _currentUser.value = null
                _sessionState.value = AppAuthSessionState.Guest
                CartRepository.resetBadgeCounts()
                FavoriteRepository.clearFavoritesState()
            }
        }
    }

    suspend fun refreshCurrentUser() {
        val uid = getActiveUserId() ?: return
        try {
            val snap = db.collection("users").document(uid).get().await()
            if (snap.exists()) {
                val current = _currentUser.value
                val fresh = snap.toSafeUser(uid)
                val merged = if (current != null) {
                    fresh.copy(
                        fullName = fresh.fullName.ifBlank { current.fullName },
                        email = fresh.email.ifBlank { current.email },
                        avatarUrl = fresh.avatarUrl?.takeIf { it.isNotBlank() }
                            ?: current.avatarUrl?.takeIf { it.isNotBlank() }
                    )
                } else {
                    fresh
                }
                _currentUser.value = merged
                _sessionState.value = AppAuthSessionState.Authenticated(merged)
                return
            }
        } catch (_: Exception) {}

        val user = authRepository.getCurrentSessionUser()
        if (user != null) {
            _currentUser.value = user
            _sessionState.value = AppAuthSessionState.Authenticated(user)
        }
    }

    fun updateLocalUser(transform: (User) -> User) {
        val fbUser = FirebaseAuth.getInstance().currentUser
        val current = _currentUser.value ?: fbUser?.let {
            User(
                userId = it.uid,
                fullName = it.displayName ?: "Khách hàng",
                email = it.email ?: "",
                avatarUrl = it.photoUrl?.toString().orEmpty()
            )
        } ?: return
        val updated = transform(current)
        _currentUser.value = updated
        _sessionState.value = AppAuthSessionState.Authenticated(updated)
    }

    /**
     * Cập nhật ảnh đại diện:
     * 1. Cập nhật ngay tức thì StateFlow local (_currentUser) để Home & Profile đổi ngay trên UI
     * 2. Ghi vào Firestore document users/{uid} (hỗ trợ avatarUrl, photoUrl, avatar)
     * 3. Đồng bộ vào Firebase Auth profile (photoUri)
     * 4. Refresh session
     */
    suspend fun updateAvatar(url: String): Result<Unit> {
        val uid = getActiveUserId() ?: return Result.failure(Exception("Chưa đăng nhập"))
        return try {
            // 1. Cập nhật StateFlow local
            updateLocalUser { it.copy(avatarUrl = url) }

            // 2. Ghi Firestore document users/{uid}
            val updates = mapOf(
                "userId" to uid,
                "avatarUrl" to url,
                "photoUrl" to url,
                "avatar" to url,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            db.collection("users").document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .await()

            // 3. Cập nhật Firebase Auth profile photoUri
            val fbUser = FirebaseAuth.getInstance().currentUser
            if (fbUser != null) {
                try {
                    val profileChange = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setPhotoUri(android.net.Uri.parse(url))
                        .build()
                    fbUser.updateProfile(profileChange).await()
                } catch (_: Exception) {}
            }

            refreshCurrentUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun onLoginSuccess(user: User) {
        _currentUser.value = user
        _sessionState.value = AppAuthSessionState.Authenticated(user)
        attachRealtimeUserListener(user.userId)
        syncUserBadgesAndFavorites(user.userId)
        scope.launch {
            FcmTokenManager.syncTokenAndTopicsForUser(user)
        }
    }

    private fun syncUserBadgesAndFavorites(userId: String) {
        scope.launch {
            CartRepository().getCartItems(userId)
            FavoriteRepository().loadFavoriteProductIds(userId)
        }
    }

    fun signOut() {
        val oldUid = getActiveUserId()
        userDocListener?.remove()
        userDocListener = null
        _currentUser.value = null
        _sessionState.value = AppAuthSessionState.Guest
        CartRepository.resetBadgeCounts()
        FavoriteRepository.clearFavoritesState()
        NotificationRepository.updateUnreadCount(0)
        CheckoutSessionHolder.clearSession()

        // Gỡ FCM token khỏi Firestore và hủy đăng ký toàn bộ Topic TRƯỚC khi gọi FirebaseAuth.signOut()
        // để Firestore Rules vẫn xác thực được request.auth.uid và tránh lộ thông báo chéo vai trò.
        scope.launch {
            FcmTokenManager.clearTokenAndTopicsOnSignOut(oldUid)
            try {
                LoginManager.getInstance().logOut()
            } catch (_: Exception) {
            }
            authRepository.signOut()
        }
    }
}
