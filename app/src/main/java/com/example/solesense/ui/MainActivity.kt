package com.example.solesense.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import com.example.solesense.push.PendingPushNavigation
import com.example.solesense.push.PushNavigationBus
import com.example.solesense.push.PushNotificationHelper
import com.example.solesense.ui.auth.AuthOptionsRoute // Bắt buộc import Route thay vì Screen
import com.example.solesense.ui.navigation.MainScreen
import com.example.solesense.ui.onboarding.OnboardingScreen
import com.example.solesense.ui.theme.SoleSenseTheme

enum class AppScreen {
    ONBOARDING,
    AUTH_OPTIONS,
}

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Kết quả cấp quyền POST_NOTIFICATIONS trên Android 13+ */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
        handlePushNavigationIntent(intent)

        setContent {
            SoleSenseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushNavigationIntent(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handlePushNavigationIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val explicitRoute = extras.getString(PushNotificationHelper.EXTRA_DEEP_LINK_ROUTE)
            ?: extras.getString("deepLinkRoute")
        val targetRole = extras.getString(PushNotificationHelper.EXTRA_TARGET_ROLE)
            ?: extras.getString("targetRole")
            ?: ""
        val type = extras.getString(PushNotificationHelper.EXTRA_TYPE)
            ?: extras.getString("type")
            ?: ""
        val referenceId = extras.getString(PushNotificationHelper.EXTRA_REFERENCE_ID)
            ?: extras.getString("referenceId")
        val chatId = extras.getString(PushNotificationHelper.EXTRA_CHAT_ID)
            ?: extras.getString("chatId")

        val resolvedRoute = explicitRoute?.takeIf { it.isNotBlank() }
            ?: if (type.isNotBlank() || targetRole.isNotBlank()) {
                PushNotificationHelper.buildDeepLinkRoute(
                    targetRole = targetRole,
                    type = type,
                    referenceId = referenceId
                )
            } else {
                null
            }

        if (!resolvedRoute.isNullOrBlank()) {
            PushNavigationBus.emit(
                PendingPushNavigation(
                    deepLinkRoute = resolvedRoute,
                    targetRole = targetRole,
                    type = type,
                    referenceId = referenceId,
                    chatId = chatId
                )
            )
            // Xóa extras đã đọc để không kích hoạt lặp lại khi xoay màn hình
            intent.removeExtra(PushNotificationHelper.EXTRA_DEEP_LINK_ROUTE)
            intent.removeExtra("deepLinkRoute")
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