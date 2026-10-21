package com.example.solesense.ui

import com.example.solesense.ui.splash.SplashScreen
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.solesense.ui.auth.OnboardingScreen
import com.example.solesense.ui.theme.SoleSenseTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bật chế độ tràn viền (Edge-to-Edge) để UI hiện đại, kéo dài qua cả Status Bar
        enableEdgeToEdge()

        setContent {
            // Bao bọc toàn bộ ứng dụng bằng Theme đã thiết lập (chứa Typography BeVietnamPro)
            SoleSenseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OnboardingScreen(
                        onComplete = {
                            // Bắt sự kiện khi người dùng nhấn Next ở trang cuối cùng
                            Toast.makeText(
                                this@MainActivity,
                                "Chuyển hướng sang màn hình Đăng nhập!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}