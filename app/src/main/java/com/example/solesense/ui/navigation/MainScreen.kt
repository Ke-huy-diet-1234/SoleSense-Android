package com.example.solesense.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.example.solesense.data.repository.ProductRepository
import com.example.solesense.repository.BrandRepository
import com.example.solesense.repository.CartRepository
import com.example.solesense.ui.address.ShippingAddressScreen
import com.example.solesense.ui.admin.AdminDashboardScreen
import com.example.solesense.ui.auth.AuthOptionsRoute
import com.example.solesense.ui.auth.EmailLoginScreen
import com.example.solesense.ui.auth.RegisterScreen
import com.example.solesense.ui.cart.CartScreen
import com.example.solesense.ui.chat.ChatScreen
import com.example.solesense.ui.chat.ChatViewModel
import com.example.solesense.ui.checkout.CheckoutScreen
import com.example.solesense.ui.explore.ExploreScreen
import com.example.solesense.ui.favorite.FavoriteScreen
import com.example.solesense.ui.home.HomeScreen
import com.example.solesense.ui.notification.NotificationScreen
import com.example.solesense.ui.onboarding.OnboardingScreen
import com.example.solesense.ui.onboarding.WelcomeScreen
import com.example.solesense.ui.order.MyOrdersScreen
import com.example.solesense.ui.product.ProductDetailScreen
import com.example.solesense.ui.product.ProductDetailViewModel
import com.example.solesense.ui.profile.ProfileScreen
import com.example.solesense.ui.profile.SetupProfileScreen
import com.example.solesense.ui.splash.SplashScreen
import com.example.solesense.ui.support.SupportDashboardScreen
import com.example.solesense.utils.AppAuthSessionState
import com.example.solesense.utils.AssetConfig
import com.example.solesense.utils.UserSessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("solesense_prefs", Context.MODE_PRIVATE) }

    var isSplashLoading by rememberSaveable { mutableStateOf(true) }
    var hasSeenOnboarding by rememberSaveable {
        mutableStateOf(prefs.getBoolean("has_seen_onboarding", false))
    }
    // 0 = WelcomeScreen (lướt nhanh), 1 = OnboardingScreen (3 hình có Next)
    var onboardingSubStep by rememberSaveable { mutableIntStateOf(0) }

    // Mở ứng dụng: Hiện màn hình Splash, trong lúc đó preload dữ liệu Trang chủ luôn
    LaunchedEffect(Unit) {
        val minDisplayJob = async { delay(1000) }
        val preloadJob = async {
            withTimeoutOrNull(3500) {
                val pJob = async { runCatching { ProductRepository().getProducts() } }
                val bJob = async { runCatching { BrandRepository().getActiveBrands() } }
                val bannerJob = async { runCatching { AssetConfig.fetchGitHubIntroBanners() } }
                pJob.await()
                bJob.await()
                bannerJob.await()
            }
        }
        minDisplayJob.await()
        preloadJob.await()
        isSplashLoading = false
    }

    if (isSplashLoading) {
        SplashScreen()
        return
    }

    // Chỉ hiển thị lần đầu sau khi cài app: WelcomeScreen (lướt nhanh) -> 3 hình Onboarding có Next -> Trang chủ
    if (!hasSeenOnboarding) {
        if (onboardingSubStep == 0) {
            WelcomeScreen(
                onContinue = { onboardingSubStep = 1 }
            )
        } else {
            OnboardingScreen(
                onComplete = {
                    prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                    hasSeenOnboarding = true
                }
            )
        }
        return
    }

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

    val currentRole = (sessionState as? AppAuthSessionState.Authenticated)?.user?.role?.uppercase() ?: ""
    val isStaffOrAdminPreview = previewCustomerStorefront && currentRole in setOf("ADMIN", "SUPPORT")
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes && !isImeVisible) {
                AppBottomNavigationBar(
                    navController = navController,
                    isReadOnlyPreview = isStaffOrAdminPreview,
                    previewRoleName = currentRole
                )
            }
        }
    ) { innerPadding ->
        val contentModifier = if (currentRoute in bottomBarRoutes) {
            if (isImeVisible) {
                Modifier
                    .padding(top = innerPadding.calculateTopPadding())
                    .imePadding()
            } else {
                Modifier.padding(innerPadding)
            }
        } else {
            Modifier
        }

        Column(modifier = contentModifier.fillMaxSize()) {
            if (isStaffOrAdminPreview && currentRoute in bottomBarRoutes) {
                StorefrontPreviewBanner(
                    roleName = currentRole,
                    onExitPreviewClick = { previewCustomerStorefront = false }
                )
            }

            AppNavHost(
                navController = navController,
                onReturnToRoleDashboard = { previewCustomerStorefront = false },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Thanh thông báo cố định ở trên đầu khi Admin / Support chuyển sang chế độ xem giao diện cửa hàng.
 * Giúp nhận biết rõ trạng thái "Chỉ xem" và quay về Bảng điều khiển chỉ với 1 chạm ở bất kỳ tab nào.
 */
@Composable
fun StorefrontPreviewBanner(
    roleName: String,
    onExitPreviewClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1E1E1E),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFB300))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = roleName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF1A1A1A),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Chế độ Chỉ xem giao diện (Read-Only)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onExitPreviewClick() },
                color = Color(0xFFEC3824)
            ) {
                Text(
                    text = if (roleName == "ADMIN") "Về trang Admin" else "Về trang Support",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * Thanh BottomBar được thiết kế lại thủ công:
 * - Thu hẹp khoảng cách thừa giữa icon, text và khung viền ngoài để cân đối, hiện đại.
 * - Hiển thị Badge đếm số dòng sản phẩm khác nhau trong Giỏ hàng (không cộng dồn số lượng từng đôi).
 * - Hiển thị Badge đếm số tin nhắn chưa đọc trên mục Chat.
 */
@Composable
fun AppBottomNavigationBar(
    navController: NavHostController,
    isReadOnlyPreview: Boolean = false,
    previewRoleName: String = ""
) {
    val context = LocalContext.current
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
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val badgeCount = if (isReadOnlyPreview) {
                        0
                    } else {
                        when (screen) {
                            Screen.Cart -> distinctCartCount
                            Screen.Chat -> unreadChatCount
                            else -> 0
                        }
                    }

                    BottomNavTabItem(
                        screen = screen,
                        isSelected = isSelected,
                        badgeCount = badgeCount,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isReadOnlyPreview && (screen == Screen.Cart || screen == Screen.Chat)) {
                                Toast.makeText(
                                    context,
                                    "Chế độ Chỉ xem ($previewRoleName): Không sử dụng Giỏ hàng & Chat khách hàng",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@BottomNavTabItem
                            }
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
                .fillMaxWidth()
                .padding(horizontal = 3.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) Color(0xFFF3F4F6) else Color.Transparent)
                .padding(horizontal = 2.dp, vertical = 4.dp)
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
                softWrap = false,
                overflow = TextOverflow.Visible,
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
    val chatViewModel: ChatViewModel = viewModel()

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
                onNavigateToNotifications = {
                    navController.navigate("notifications")
                },
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                }
            )
        }

        composable(Screen.Explore.route) {
            ExploreScreen(
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                },
                onNavigateToNotifications = {
                    navController.navigate("notifications")
                },
                onNavigateToLogin = {
                    navController.navigate("auth_options")
                }
            )
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
                viewModel = chatViewModel,
                onNavigateToAuth = {
                    navController.navigate("auth_options")
                },
                onBackClick = {
                    navController.popBackStack()
                },
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToAuth = {
                    navController.navigate("auth_options")
                },
                onNavigateToSetupProfile = {
                    navController.navigate("setup_profile?isOnboarding=false")
                },
                onNavigateToAddresses = {
                    navController.navigate("shipping_addresses")
                },
                onNavigateToOrders = {
                    navController.navigate("my_orders")
                },
                onNavigateToNotifications = {
                    navController.navigate("notifications")
                },
                onNavigateToFavorites = {
                    navController.navigate("favorites")
                },
                onSwitchToRoleDashboard = onReturnToRoleDashboard
            )
        }

        composable(
            route = "setup_profile?isOnboarding={isOnboarding}",
            arguments = listOf(
                navArgument("isOnboarding") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val isOnboarding = backStackEntry.arguments?.getBoolean("isOnboarding") ?: false
            SetupProfileScreen(
                isInitialOnboardingSetup = isOnboarding,
                onBackClick = {
                    if (isOnboarding) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onCompleted = {
                    if (isOnboarding) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(
            route = "my_orders?orderId={orderId}",
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            MyOrdersScreen(
                initialOrderIdToTrack = orderId,
                onBackClick = { navController.popBackStack() },
                onBuyAgainProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                }
            )
        }

        composable(route = "notifications") {
            NotificationScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToOrder = { relatedOrderId ->
                    if (relatedOrderId.isNotBlank()) {
                        navController.navigate("my_orders?orderId=$relatedOrderId")
                    } else {
                        navController.navigate("my_orders")
                    }
                },
                onNavigateToChat = {
                    navController.navigate(Screen.Chat.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToExplore = {
                    navController.navigate(Screen.Explore.route) {
                        launchSingleTop = true
                    }
                }
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
                },
                onNavigateToChat = { productId, name, price, imageUrl, color, size ->
                    chatViewModel.setPinnedProductContext(productId, name, price, imageUrl, color, size)
                    navController.navigate(Screen.Chat.route) {
                        launchSingleTop = true
                    }
                },
                onRelatedProductClick = { relatedProductId ->
                    navController.navigate("product_detail/$relatedProductId")
                },
                onReturnToRoleDashboard = onReturnToRoleDashboard
            )
        }

        composable(route = "checkout") {
            CheckoutScreen(
                onBackClick = { navController.popBackStack() },
                onOrderSuccess = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onTrackOrderClick = { orderId ->
                    navController.navigate("my_orders?orderId=$orderId") {
                        popUpTo("checkout") { inclusive = true }
                    }
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
                    if (user.role.uppercase() == "CUSTOMER" && !user.isProfileCompleted && user.defaultAddress.isNullOrBlank()) {
                        navController.navigate("setup_profile?isOnboarding=true") {
                            popUpTo("auth_options") { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(route = "email_login") {
            EmailLoginScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = { user ->
                    UserSessionManager.onLoginSuccess(user)
                    if (user.role.uppercase() == "CUSTOMER" && !user.isProfileCompleted && user.defaultAddress.isNullOrBlank()) {
                        navController.navigate("setup_profile?isOnboarding=true") {
                            popUpTo("auth_options") { inclusive = true }
                        }
                    } else {
                        navController.popBackStack("auth_options", inclusive = true)
                    }
                }
            )
        }

        composable(route = "register") {
            RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = { user ->
                    UserSessionManager.onLoginSuccess(user)
                    // Khi mới tạo tài khoản, đi đến phần cập nhật thông tin & lưu làm mặc định
                    navController.navigate("setup_profile?isOnboarding=true") {
                        popUpTo("auth_options") { inclusive = true }
                    }
                }
            )
        }
    }
}
