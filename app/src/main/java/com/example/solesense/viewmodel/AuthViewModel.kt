package com.example.solesense.viewmodel

import android.util.Log
import androidx.credentials.Credential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solesense.model.user.User
import com.example.solesense.repository.AuthRepositoryImpl
import com.facebook.AccessToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "AuthViewModel"

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Success(val user: User) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(
    private val authRepository: AuthRepositoryImpl
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signInWithGoogle(
        credential: Credential,
        onSuccess: ((User) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            Log.d(TAG, "signInWithGoogle: Bắt đầu gọi repository...")
            val result = authRepository.authWithGoogle(credential)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    Log.d(TAG, "Đăng nhập thành công: ${user.fullName} - Role: ${user.role} - UID: ${user.userId}")
                    _authState.value = AuthState.Success(user)
                    onSuccess?.invoke(user)
                } else {
                    val errorMsg = "Không tìm thấy dữ liệu người dùng"
                    Log.e(TAG, errorMsg)
                    _authState.value = AuthState.Error(errorMsg)
                    onError?.invoke(errorMsg)
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Đăng nhập thất bại"
                Log.e(TAG, "Lỗi đăng nhập Firebase", result.exceptionOrNull())
                _authState.value = AuthState.Error(errorMsg)
                onError?.invoke(errorMsg)
            }
        }
    }

    fun signInWithFacebook(
        accessToken: AccessToken,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authRepository.authWithFacebook(accessToken)
            if (result.isSuccess) {
                result.getOrNull()?.let(onSuccess)
            } else {
                onError(result.exceptionOrNull()?.message ?: "Lỗi đăng nhập bằng Facebook")
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                    val repository = AuthRepositoryImpl(
                        FirebaseAuth.getInstance(),
                        FirebaseFirestore.getInstance()
                    )
                    return AuthViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
