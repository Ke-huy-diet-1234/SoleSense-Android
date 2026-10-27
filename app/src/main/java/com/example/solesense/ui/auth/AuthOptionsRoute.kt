package com.example.solesense.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
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


    // BẮT ĐẦU PHẦN CỦA FACEBOOK
    val callbackManager = remember { CallbackManager.Factory.create() }
    val loginManager = remember { LoginManager.getInstance() }

    // Launcher mở màn hình uỷ quyền FB mà không cần đụng vào MainActivity
    val facebookLoginLauncher = rememberLauncherForActivityResult(
        contract = loginManager.createLogInActivityResultContract(callbackManager, null)
    ) { /* Kết quả sẽ được đẩy tự động vào CallbackManager bên dưới */ }


    DisposableEffect(Unit) {
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d(TAG, "Lấy AccessToken FB thành công: ${result.accessToken.token}")
                viewModel.signInWithFacebook(
                    accessToken = result.accessToken,
                    onSuccess = { user ->
                        Toast.makeText(context, "Đăng nhập thành công! Xin chào ${user.fullName}", Toast.LENGTH_SHORT).show()
                        onLoginSuccess?.invoke(user)
                    },
                    onError = { error ->
                        Toast.makeText(context, "Lỗi Firebase FB: $error", Toast.LENGTH_SHORT).show()
                        Log.e(TAG,"Lỗi Firebase FB: $error" )
                    }
                )
            }

            override fun onCancel() {
                Log.d(TAG, "Người dùng đã huỷ chọn tài khoản Facebook")
            }

            override fun onError(error: FacebookException) {
                Log.e(TAG, "Lỗi Facebook Login", error)
                Toast.makeText(context, "Lỗi FB: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
        onDispose {
            loginManager.unregisterCallback(callbackManager)
        }
    }

    AuthOptionsScreen(
        onBackClick = onBack,
        onPasswordLoginClick = onNavigateToLogin,
        onRegisterClick = onNavigateToRegister,
        onGoogleClick = {
            coroutineScope.launch {
                try {
                    // Sử dụng GetSignInWithGoogleOption cho hành động bấm nút "Tiếp tục với Google"
                    // để hiển thị hộp thoại chọn tài khoản Google (Bottom Sheet Account Chooser với avatar, tên, email)
                    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(SERVER_CLIENT_ID)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(signInWithGoogleOption)
                        .build()

                    Log.d(TAG, "Bắt đầu gọi credentialManager.getCredential...")
                    val result = try {
                        credentialManager.getCredential(context, request)
                    } catch (e: NoCredentialException) {
                        Log.w(TAG, "GetSignInWithGoogleOption trả về NoCredentialException, thử fallback GetGoogleIdOption...", e)
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(SERVER_CLIENT_ID)
                            .setAutoSelectEnabled(false)
                            .build()
                        val fallbackRequest = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
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
            loginManager.logOut()
            loginManager.setAuthType("reauthenticate")
            loginManager.setLoginBehavior(com.facebook.login.LoginBehavior.WEB_ONLY)
            facebookLoginLauncher.launch(listOf("email", "public_profile"))
        }
    )
}
