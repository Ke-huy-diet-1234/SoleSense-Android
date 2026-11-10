package com.example.solesense.ui.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solesense.R
import com.example.solesense.model.user.User
import com.example.solesense.utils.UserSessionManager
import com.example.solesense.viewmodel.AuthState
import com.example.solesense.viewmodel.AuthViewModel

@Composable
fun EmailLoginScreen(
    onBackClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (User) -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isLoading = authState is AuthState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_solesense_logo),
            contentDescription = "SoleSense Logo",
            modifier = Modifier.size(72.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Đăng nhập SoleSense",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Nhập email và mật khẩu tài khoản của bạn",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF757575)
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            placeholder = { Text("Nhập địa chỉ email (vd: admin@solesense.com)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Mật khẩu") },
            placeholder = { Text("Nhập mật khẩu") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Vui lòng nhập đầy đủ email và mật khẩu"
                    return@Button
                }
                viewModel.signInWithEmailPassword(
                    email = email,
                    password = password,
                    onSuccess = { user ->
                        Toast.makeText(
                            context,
                            "Đăng nhập thành công (${user.role}): ${user.fullName}",
                            Toast.LENGTH_SHORT
                        ).show()
                        onLoginSuccess(user)
                    },
                    onError = { err ->
                        errorMessage = err
                    }
                )
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "Đăng nhập",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Hỗ trợ chọn nhanh vai trò để kiểm thử luồng Phân quyền (CUSTOMER / ADMIN / SUPPORT)
        HorizontalDivider(color = Color(0xFFEEEEEE))
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Đăng nhập nhanh theo vai trò (Kiểm thử Phân quyền)",
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color(0xFF757575),
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val demoCustomer = User(
                        userId = "mmZhfijntCf7BW2U0pf9VQbm4Xi1",
                        fullName = "Vương Khắc Hoàn",
                        email = "customer@solesense.com",
                        phone = "0987654321",
                        role = "CUSTOMER",
                        status = "ACTIVE",
                        isVerified = true
                    )
                    UserSessionManager.onLoginSuccess(demoCustomer)
                    Toast.makeText(context, "Đã đăng nhập với vai trò CUSTOMER", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(demoCustomer)
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Customer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            }

            OutlinedButton(
                onClick = {
                    val demoAdmin = User(
                        userId = "mmZhfijntCf7BW2U0pf9VQbm4Xi1",
                        fullName = "Quản trị viên",
                        email = "admin@solesense.com",
                        role = "ADMIN",
                        status = "ACTIVE",
                        isVerified = true
                    )
                    UserSessionManager.onLoginSuccess(demoAdmin)
                    Toast.makeText(context, "Đã đăng nhập với vai trò ADMIN", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(demoAdmin)
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE53935)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
            }

            OutlinedButton(
                onClick = {
                    val demoSupport = User(
                        userId = "support_staff_01",
                        fullName = "Nhân viên Hỗ trợ",
                        email = "support@solesense.com",
                        role = "SUPPORT",
                        status = "ACTIVE",
                        isVerified = true
                    )
                    UserSessionManager.onLoginSuccess(demoSupport)
                    Toast.makeText(context, "Đã đăng nhập với vai trò SUPPORT", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(demoSupport)
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF1E88E5)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Support", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onNavigateToRegister() }
                .padding(8.dp)
        ) {
            Text(
                text = "Bạn chưa có tài khoản? ",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
            Text(
                text = "Đăng ký ngay",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

