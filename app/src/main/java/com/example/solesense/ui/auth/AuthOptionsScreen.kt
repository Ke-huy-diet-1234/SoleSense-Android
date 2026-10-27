package com.example.solesense.ui.auth

import android.R.attr.contentDescription
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.solesense.R
import com.example.solesense.ui.components.OrDivider
import com.example.solesense.ui.components.SocialAuthButton
import com.google.common.io.Files.append


@Composable
fun AuthOptionsScreen(
    onBackClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit,
    onPasswordLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding() // Tránh bị đè bởi Status Bar và Navigation Bar
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nút Back căn trái
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.offset(x = (-12).dp) // Kéo dịch sang trái một chút để căn lề chuẩn với text
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ảnh minh họa
        Image(
            painter = painterResource(R.drawable.img_auth_illustration),
            contentDescription = "Auth Illustration",
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1.5f),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tiêu đề
        Text(
            text = "Chào mừng bạn",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Nút Đăng nhập Google
        SocialAuthButton(
            text = "Tiếp tục với Google",
            iconRes = R.drawable.ic_google_logo,
            onClick = onGoogleClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nút Đăng nhập Facebook
        SocialAuthButton(
            text = "Tiếp tục với Facebook",
            iconRes = R.drawable.ic_facebook_logo,
            onClick = onFacebookClick
        )

        // Dải phân cách "hoặc"
        OrDivider("hoặc")

        // Nút Đăng nhập bằng mật khẩu
        Button(
            onClick = onPasswordLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Đăng nhập bằng mật khẩu",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }

        // Đẩy phần "Đăng ký" xuống sát đáy màn hình
        Spacer(modifier = Modifier.weight(1f))

        // Text Đăng ký sử dụng AnnotatedString để format 2 kiểu chữ trên 1 dòng
        val registerText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Gray)) {
                append("Bạn chưa có tài khoản? ")
            }
            withStyle(style = SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
                append("Đăng ký")
            }
        }

        Text(
            text = registerText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .clickable { onRegisterClick() }
                .padding(8.dp) // Tăng vùng click (touch target)
        )
    }
}


@Preview (showBackground = true)
@Composable fun  test() {
    val emptyAction: () -> Unit = {}

    AuthOptionsScreen(
        onBackClick = emptyAction,
        onGoogleClick = emptyAction,
        onFacebookClick = emptyAction,
        onPasswordLoginClick = emptyAction,
        onRegisterClick = emptyAction)
}