package com.example.solesense.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.solesense.repository.CartRepository
import com.example.solesense.ui.address.ShippingAddressScreen
import com.example.solesense.ui.admin.AdminDashboardScreen
import com.example.solesense.ui.auth.AuthOptionsRoute
import com.example.solesense.ui.auth.EmailLoginScreen
import com.example.solesense.ui.auth.RegisterScreen
import com.example.solesense.ui.cart.CartScreen
import com.example.solesense.ui.chat.ChatScreen
import com.example.solesense.ui.checkout.CheckoutScreen
import com.example.solesense.ui.explore.ExploreScreen
import com.example.solesense.ui.favorite.FavoriteScreen
import com.example.solesense.ui.home.HomeScreen
import com.example.solesense.ui.product.ProductDetailScreen
import com.example.solesense.ui.product.ProductDetailViewModel
import com.example.solesense.ui.profile.ProfileScreen
import com.example.solesense.ui.support.SupportDashboardScreen
import com.example.solesense.utils.AppAuthSessionState
import com.example.solesense.utils.UserSessionManager

@Composable
fun MainScreen() {
    val sessionState by UserSessionManager.sessionState.collectAsState()
    var previewCustomerStorefront by remember { mutableStateOf(false) }

    // Khi đổi tài khoản hoặc đăng xuất thì reset chế độ xem cửa hàng
    LaunchedEffect(sessionState) {
        previewCustomerStorefront = false
    }

    // Phân luồng theo sơ đồ AuthState -> Guest / Authenticated -> Check Role (CUSTOMER / ADMIN / SUPPORT)
    when (val authState = sessionState) {
        is AppAuthSessionState.Authenticated -> {
            val role = authState.user.role.uppercase()
            if (!previewCustomerStorefront && role == "ADMIN") {
                AdminDashboardScreen(
                    user = authState.user,
                    onSwitchToCustomerView = { previewCustomerStorefront = true },
                    onLogoutClick = { UserSessionManager.signOut() }
                )
                return
            }
            if (!previewCustomerStorefront && role == "SUPPORT") {
                SupportDashboardScreen(
                    user = authState.user,
                    onSwitchToCustomerView = { previewCustomerStorefront = true },
                    onLogoutClick = { UserSessionManager.signOut() }
                )
                return
            }
        }
        else -> {
            // Guest hoặc đang kiểm tra -> hiển thị Customer Home
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = setOf(
        Screen.Home.route,
        Screen.Explore.route,
        Screen.Cart.route,
        Screen.Chat.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                AppBottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        val contentModifier = if (currentRoute in bottomBarRoutes) {
            Modifier.padding(innerPadding)
        } else {
            Modifier
        }

        AppNavHost(
            navController = navController,
            onReturnToRoleDashboard = { previewCustomerStorefront = false },
            modifier = contentModifier
        )
    }
}

/**
 * Thanh BottomBar được thiết kế lại thủ công:
 * - Thu hẹp khoảng cách thừa giữa icon, text và khung viền ngoài để cân đối, hiện đại.
 * - Hiển thị Badge đếm số dòng sản phẩm khác nhau trong Giỏ hàng (không cộng dồn số lượng từng đôi).
 * - Hiển thị Badge đếm số tin nhắn chưa đọc trên mục Chat.
 */
@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    val screens = listOf(
        Screen.Home,
        Screen.Explore,
        Screen.Cart,
        Screen.Chat,
        Screen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val distinctCartCount by CartRepository.distinctCartItemCount.collectAsState()
    val unreadChatCount by CartRepository.unreadMessageCount.collectAsState()

    Surface(
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                color = Color(0xFFF0F0F0),
                thickness = 0.8.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val badgeCount = when (screen) {
                        Screen.Cart -> distinctCartCount
                        Screen.Chat -> unreadChatCount
                        else -> 0
                    }

                    BottomNavTabItem(
                        screen = screen,
                        isSelected = isSelected,
                        badgeCount = badgeCount,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (screen == Screen.Chat) {
                                CartRepository.setUnreadMessageCount(0)
                            }
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
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
}

@Composable
private fun BottomNavTabItem(
    screen: Screen,
    isSelected: Boolean,
    badgeCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (isSelected) Color(0xFF111111) else Color(0xFF8E8E93)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) Color(0xFFF3F4F6) else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    painter = painterResource(id = screen.icon),
                    contentDescription = screen.title,
                    tint = contentColor,
                    modifier = Modifier.size(21.dp)
                )

                if (badgeCount > 0) {
                    val badgeText = if (badgeCount > 99) "99+" else badgeCount.toString()
                    Box(
                        modifier = Modifier
                            .offset(x = 8.dp, y = (-5).dp)
                            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                            .border(1.5.dp, Color.White, CircleShape)
                            .padding(horizontal = 3.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = screen.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    onReturnToRoleDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToExplore = {
                    navController.navigate(Screen.Explore.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("auth_options")
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToFavorites = {
                    navController.navigate("favorites")
                },
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                }
            )
        }

        composable(Screen.Explore.route) {
            ExploreScreen()
        }

        composable(Screen.Cart.route) {
            CartScreen(
                onNavigateToAuth = {
                    navController.navigate("auth_options")
                },
                onNavigateToCheckout = {
                    navController.navigate("checkout")
                },
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                }
            )
        }

        composable(Screen.Chat.route) {
            LaunchedEffect(Unit) {
                CartRepository.setUnreadMessageCount(0)
            }
            ChatScreen(
                onNavigateToAuth = {
                    navController.navigate("auth_options")
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToAuth = {
                    navController.navigate("auth_options")
                },
                onNavigateToAddresses = {
                    navController.navigate("shipping_addresses")
                },
                onNavigateToFavorites = {
                    navController.navigate("favorites")
                },
                onSwitchToRoleDashboard = onReturnToRoleDashboard
            )
        }

        composable(route = "favorites") {
            FavoriteScreen(
                onBackClick = { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                }
            )
        }

        composable(
            route = "product_detail/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) {
            val detailViewModel: ProductDetailViewModel = viewModel()
            ProductDetailScreen(
                viewModel = detailViewModel,
                onBackClick = { navController.popBackStack() },
                onCartClick = {
                    navController.navigate(Screen.Cart.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSearchClick = {
                    navController.navigate(Screen.Explore.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToCheckout = { _ ->
                    navController.navigate("checkout")
                },
                onNavigateToAuth = {
                    navController.navigate("auth_options")
                }
            )
        }

        composable(route = "checkout") {
            CheckoutScreen(
                onBackClick = { navController.popBackStack() },
                onOrderSuccess = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }

        composable(route = "shipping_addresses") {
            ShippingAddressScreen(
                onBackClick = { navController.popBackStack() },
                onAddressApplied = { navController.popBackStack() }
            )
        }

        composable(route = "auth_options") {
            AuthOptionsRoute(
                onBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate("email_login") },
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = { user ->
                    UserSessionManager.onLoginSuccess(user)
                    navController.popBackStack()
                }
            )
        }

        composable(route = "email_login") {
            EmailLoginScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = { _ ->
                    navController.popBackStack("auth_options", inclusive = true)
                }
            )
        }

        composable(route = "register") {
            RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = { _ ->
                    navController.popBackStack("auth_options", inclusive = true)
                }
            )
        }
    }
}
