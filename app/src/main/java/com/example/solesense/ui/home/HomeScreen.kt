package com.example.solesense.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solesense.model.catalog.Brand
import com.example.solesense.model.catalog.ProductModel
import com.example.solesense.repository.FavoriteRepository
import com.example.solesense.ui.components.LoginRequiredDialog
import com.example.solesense.ui.components.NetworkStatusBanner
import com.example.solesense.ui.components.SearchBar
import com.example.solesense.ui.home.components.BrandSection
import com.example.solesense.ui.home.components.HomeHeader
import com.example.solesense.ui.home.components.NewArrivalsSection
import com.example.solesense.ui.home.components.TrendingProductsSection
import com.example.solesense.ui.home.components.recommendedSection
import com.example.solesense.utils.UserSessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExplore: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {

        NetworkStatusBanner(isOnline = isOnline)

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6C5CE7))
                }
            }

            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = state.message, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.fetchHomeData(isForceRefresh = true) }) {
                        Text("Thử lại")
                    }
                }
            }

            is HomeUiState.Success -> {
                val pullToRefreshState = rememberPullToRefreshState()

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.fetchHomeData(isForceRefresh = true) },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    HomeScreenContent(
                        brands = state.brands,
                        trendingProducts = state.trendingProducts,
                        newArrivals = state.newArrivals,
                        recommendedProducts = state.recommendedProducts,
                        onNavigateToExplore = onNavigateToExplore,
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToFavorites = onNavigateToFavorites,
                        onProductClick = onProductClick
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    brands: List<Brand> = emptyList(),
    trendingProducts: List<ProductModel>,
    newArrivals: List<ProductModel>,
    recommendedProducts: List<ProductModel>,
    onNavigateToExplore: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val isLoggedIn = currentUser != null
    val userName = currentUser?.fullName ?: "Khách hàng"
    val avatarUrl = currentUser?.avatarUrl
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteRepository = remember { FavoriteRepository() }

    var showLoginRequiredDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.userId) {
        if (currentUser != null) {
            favoriteRepository.loadFavoriteProductIds(currentUser!!.userId)
        }
    }

    fun handleProductFavoriteToggle(product: ProductModel) {
        if (!isLoggedIn) {
            showLoginRequiredDialog = true
        } else {
            scope.launch {
                favoriteRepository.toggleFavorite(product.id).onSuccess { isFav ->
                    val msg = if (isFav) "Đã thêm ${product.name} vào yêu thích" else "Đã bỏ khỏi danh sách yêu thích"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header & Search (Ấn tìm kiếm chuyển sang tab Khám phá)
        item(span = { GridItemSpan(2) }) {
            Column {
                HomeHeader(
                    isLoggedIn = isLoggedIn,
                    userName = userName,
                    avatarUrl = avatarUrl,
                    onLoginClick = onNavigateToLogin,
                    onProfileClick = onNavigateToProfile,
                    onNotificationClick = {
                        if (!isLoggedIn) {
                            showLoginRequiredDialog = true
                        } else {
                            Toast.makeText(context, "Chức năng thông báo sẽ cập nhật sau", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFavoriteClick = {
                        if (!isLoggedIn) {
                            showLoginRequiredDialog = true
                        } else {
                            onNavigateToFavorites()
                        }
                    }
                )
                SearchBar(onClick = onNavigateToExplore)
            }
        }

        // 2. Banner Khuyến mãi
        item(span = { GridItemSpan(2) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF6C5CE7)),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("50 %", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Discount", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // 3. Danh mục Brand (Ấn vào chuyển sang tab Khám phá)
        item(span = { GridItemSpan(2) }) {
            BrandSection(
                brands = brands,
                onBrandClick = { _ ->
                    onNavigateToExplore()
                }
            )
        }

        // 4. TrendingProductsSection
        item(span = { GridItemSpan(2) }) {
            TrendingProductsSection(
                products = trendingProducts,
                onSeeAllClick = onNavigateToExplore,
                onFavoriteClick = { product -> handleProductFavoriteToggle(product) },
                onProductClick = { product -> onProductClick(product.id) }
            )
        }

        // 5. NewArrivalsSection
        item(span = { GridItemSpan(2) }) {
            NewArrivalsSection(
                products = newArrivals,
                onSeeAllClick = onNavigateToExplore,
                onFavoriteClick = { product -> handleProductFavoriteToggle(product) },
                onProductClick = { product -> onProductClick(product.id) }
            )
        }

        // 6. RecommendedSection
        recommendedSection(
            products = recommendedProducts,
            onFavoriteClick = { product -> handleProductFavoriteToggle(product) },
            onProductClick = { product -> onProductClick(product.id) }
        )
    }

    LoginRequiredDialog(
        isVisible = showLoginRequiredDialog,
        onLoginClick = {
            showLoginRequiredDialog = false
            onNavigateToLogin()
        },
        onDismiss = {
            showLoginRequiredDialog = false
        }
    )
}

@Preview(showBackground = true)
@Composable
fun TestHomeScreenUi() {
    HomeScreen(onNavigateToExplore = {})
}
