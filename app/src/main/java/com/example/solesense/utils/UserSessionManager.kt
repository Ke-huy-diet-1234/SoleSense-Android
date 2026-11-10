package com.example.solesense.utils

import com.example.solesense.model.user.User
import com.example.solesense.repository.AuthRepositoryImpl
import com.example.solesense.repository.CartRepository
import com.example.solesense.repository.FavoriteRepository
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppAuthSessionState {
    data object Checking : AppAuthSessionState
    data object Guest : AppAuthSessionState
    data class Authenticated(val user: User) : AppAuthSessionState
}

object UserSessionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val authRepository = AuthRepositoryImpl(
        FirebaseAuth.getInstance(),
        FirebaseFirestore.getInstance()
    )

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
        return FirebaseAuth.getInstance().currentUser?.uid ?: _currentUser.value?.userId
    }

    fun refreshSession() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            _currentUser.value = null
            _sessionState.value = AppAuthSessionState.Guest
            CartRepository.resetBadgeCounts()
            FavoriteRepository.clearFavoritesState()
            return
        }

        scope.launch {
            val user = authRepository.getCurrentSessionUser()
            if (user != null) {
                _currentUser.value = user
                _sessionState.value = AppAuthSessionState.Authenticated(user)
                syncUserBadgesAndFavorites(user.userId)
            } else {
                _currentUser.value = null
                _sessionState.value = AppAuthSessionState.Guest
                CartRepository.resetBadgeCounts()
                FavoriteRepository.clearFavoritesState()
            }
        }
    }

    fun onLoginSuccess(user: User) {
        _currentUser.value = user
        _sessionState.value = AppAuthSessionState.Authenticated(user)
        syncUserBadgesAndFavorites(user.userId)
    }

    private fun syncUserBadgesAndFavorites(userId: String) {
        scope.launch {
            CartRepository().getCartItems(userId)
            FavoriteRepository().loadFavoriteProductIds(userId)
        }
    }

    fun signOut() {
        try {
            LoginManager.getInstance().logOut()
        } catch (_: Exception) {
        }
        authRepository.signOut()
        _currentUser.value = null
        _sessionState.value = AppAuthSessionState.Guest
        CartRepository.resetBadgeCounts()
        FavoriteRepository.clearFavoritesState()
    }
}
