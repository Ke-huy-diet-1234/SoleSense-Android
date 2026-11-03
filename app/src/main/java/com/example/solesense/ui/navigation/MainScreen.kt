package com.example.solesense.ui.navigation

import android.net.http.SslCertificate.restoreState
import android.net.http.SslCertificate.saveState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
// Import 5 màn hình rỗng
import com.example.solesense.ui.cart.CartScreen
import com.example.solesense.ui.chat.ChatScreen
import com.example.solesense.ui.explore.ExploreScreen
import com.example.solesense.ui.home.HomeScreen
import com.example.solesense.ui.profile.ProfileScreen

@Composable
fun MainScreen() {
    // 1. Khởi tạo Controller - Bộ não quản lý toàn bộ luồng điều hướng
    val navController = rememberNavController()

    // 2. Cấu trúc Scaffold tạo vỏ bọc giao diện chuẩn
    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        // 3. Vùng chứa nội dung (AppNavHost)
        // Modifier.padding(innerPadding) cực kỳ quan trọng: nó đẩy nội dung lên trên BottomNav
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    val screens = listOf(
        Screen.Home,
        Screen.Explore,
        Screen.Cart,
        Screen.Chat,
        Screen.Profile
    )

    // Theo dõi trạng thái của BackStack để biết màn hình nào đang Active
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Column {
        NavigationBar(
            modifier = Modifier.height(62.dp), // Ép chiều cao phần nội dung chính nhỏ lại (từ 80dp xuống 60dp hoặc 56dp tùy ý)
            containerColor = Color.White,
            tonalElevation = 8.dp,
            windowInsets = WindowInsets(0) // QUAN TRỌNG: Tắt phần đệm tự động của Material 3 để nó không cãi lại lệnh height
        ) {
            screens.forEach { screen ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = screen.icon), // Hoặc imageVector nếu bạn đã đổi
                            contentDescription = screen.title,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = screen.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp, // Kích thước chữ nhỏ gọn
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color.Black,
                        indicatorColor = Color(0xFFEEEEEE),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
        )
    }
}

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) { HomeScreen({},{}) }
        composable(Screen.Explore.route) { ExploreScreen() }
        composable(Screen.Cart.route) { CartScreen() }
        composable(Screen.Chat.route) { ChatScreen() }
        composable(Screen.Profile.route) { ProfileScreen() }
    }
}