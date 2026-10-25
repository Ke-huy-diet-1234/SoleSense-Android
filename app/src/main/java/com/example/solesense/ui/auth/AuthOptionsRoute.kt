package com.example.solesense.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solesense.model.user.User
import com.example.solesense.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import kotlinx.coroutines.launch

private const val TAG = "AuthOptionsRoute"
private const val SERVER_CLIENT_ID = "709271649676-95dosmcpiqjdtimt2m8gt40vgh9gjj77.apps.googleusercontent.com"

@Composable
fun AuthOptionsRoute(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    onLoginSuccess: ((User) -> Unit)? = null
) {
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    AuthOptionsScreen(
        onBackClick = onBack,
        onPasswordLoginClick = onNavigateToLogin,
        onRegisterClick = onNavigateToRegister,
        onGoogleClick = {
            coroutineScope.launch {
                try {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(SERVER_CLIENT_ID)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    Log.d(TAG, "Bắt đầu gọi credentialManager.getCredential...")
                    val result = try {
                        credentialManager.getCredential(context, request)
                    } catch (e: NoCredentialException) {
                        Log.w(TAG, "GetGoogleIdOption trả về NoCredentialException, thử fallback GetSignInWithGoogleOption...", e)
                        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(SERVER_CLIENT_ID)
                            .build()
                        val fallbackRequest = GetCredentialRequest.Builder()
                            .addCredentialOption(signInWithGoogleOption)
                            .build()
                        credentialManager.getCredential(context, fallbackRequest)
                    }

                    Log.d(TAG, "Lấy credential thành công: ${result.credential::class.java.simpleName}")
                    viewModel.signInWithGoogle(
                        credential = result.credential,
                        onSuccess = { user ->
                            Toast.makeText(
                                context,
                                "Đăng nhập thành công! Xin chào ${user.fullName}",
                                Toast.LENGTH_SHORT
                            ).show()
                            onLoginSuccess?.invoke(user)
                        },
                        onError = { error ->
                            Toast.makeText(
                                context,
                                "Đăng nhập thất bại: $error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                } catch (e: GetCredentialCancellationException) {
                    Log.d(TAG, "Người dùng đã huỷ chọn tài khoản Google: ${e.message}")
                } catch (e: NoCredentialException) {
                    Log.e(TAG, "Không tìm thấy tài khoản Google hoặc chưa cấu hình SHA-1 trên Firebase Console", e)
                    Toast.makeText(
                        context,
                        "Không tìm thấy tài khoản Google hoặc chưa cấu hình SHA-1 trên Firebase",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi gọi Google Prompt", e)
                    Toast.makeText(
                        context,
                        "Lỗi đăng nhập Google: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        onFacebookClick = {
            // Sẽ làm ở bước sau
        }
    )
}
