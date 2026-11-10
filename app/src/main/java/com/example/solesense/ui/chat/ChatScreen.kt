package com.example.solesense.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.ui.components.LoginRequiredDialog
import com.example.solesense.utils.UserSessionManager

@Composable
fun ChatScreen(
    onNavigateToAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val isLoggedIn = currentUser != null
    var showLoginRequiredDialog by remember { mutableStateOf(!isLoggedIn) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tư vấn AI & Hỗ trợ khách hàng",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111111)
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoggedIn) {
            Text(
                text = "Xin chào ${currentUser?.fullName}. Lịch sử chat với AI tư vấn và nhân viên hỗ trợ đang được đồng bộ với tài khoản của bạn.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF616161),
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "Để lưu lịch sử trò chuyện theo tài khoản hoặc nhận tư vấn cá nhân hóa từ AI & nhân viên hỗ trợ, vui lòng đăng nhập.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF616161),
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { showLoginRequiredDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC3824))
            ) {
                Text("Đăng nhập để chat & lưu lịch sử", fontWeight = FontWeight.Bold)
            }
        }
    }

    LoginRequiredDialog(
        isVisible = showLoginRequiredDialog,
        onLoginClick = {
            showLoginRequiredDialog = false
            onNavigateToAuth()
        },
        onDismiss = {
            showLoginRequiredDialog = false
        }
    )
}
