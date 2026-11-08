package com.example.solesense.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solesense.ui.auth.AuthOptionsRoute // Bắt buộc import Route thay vì Screen
import com.example.solesense.ui.checkout.CheckoutScreen
import com.example.solesense.ui.navigation.MainScreen
import com.example.solesense.ui.onboarding.OnboardingScreen
import com.example.solesense.ui.product.ProductDetailScreen
import com.example.solesense.ui.product.ProductDetailViewModel
import com.example.solesense.ui.theme.SoleSenseTheme
import com.google.android.gms.common.wrappers.Wrappers.packageManager
import java.security.MessageDigest

enum class AppScreen {
    ONBOARDING,
    AUTH_OPTIONS,
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SoleSenseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }

    @Composable
    fun MainAppNavigation(
        onPasswordLogin: () -> Unit = {},
        onRegister: () -> Unit = {},
    ) {
        var currentScreen by remember { mutableStateOf(AppScreen.ONBOARDING) }

        // Xử lý nút Back của thiết bị khi đang ở AuthOptions
        BackHandler(enabled = currentScreen == AppScreen.AUTH_OPTIONS) {
            currentScreen = AppScreen.ONBOARDING
        }

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState == AppScreen.AUTH_OPTIONS) {
                    (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "ScreenTransition",
        ) { screen ->
            when (screen) {
                AppScreen.ONBOARDING -> {
                    OnboardingScreen(
                        onComplete = {
                            currentScreen = AppScreen.AUTH_OPTIONS
                        },
                    )
                }

                AppScreen.AUTH_OPTIONS -> {
                    AuthOptionsRoute(
                        onBack = {
                            currentScreen = AppScreen.ONBOARDING
                        },
                        onNavigateToLogin = onPasswordLogin,
                        onNavigateToRegister = onRegister
                    )
                }
            }
        }
    }
}