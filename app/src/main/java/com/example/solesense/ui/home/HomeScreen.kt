package com.example.solesense.ui.home

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solesense.model.catalog.Brand
import com.example.solesense.model.catalog.ProductModel
import com.example.solesense.ui.components.NetworkStatusBanner
import com.example.solesense.ui.components.SearchBar
import com.example.solesense.ui.home.components.BrandSection
import com.example.solesense.ui.home.components.HomeHeader
import com.example.solesense.ui.home.components.NewArrivalsSection
import com.example.solesense.ui.home.components.TrendingProductsSection
import com.example.solesense.ui.home.components.recommendedSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExplore: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {

        NetworkStatusBanner(isOnline = isOnline)

        // 2. Nội dung chính hỗ trợ Kéo để làm mới (Pull-to-refresh)
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
    onProductClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLoggedIn by remember { mutableStateOf(false) }
    val userName by remember { mutableStateOf("Nguyễn Văn A") }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header & Search
        item(span = { GridItemSpan(2) }) {
            Column {
                HomeHeader(
                    isLoggedIn = isLoggedIn,
                    userName = userName,
                    onLoginClick = onNavigateToLogin,
                    onNotificationClick = {},
                    onFavoriteClick = {}
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

        // 3. Danh mục Brand (Cuộn ngang)
        item(span = { GridItemSpan(2) }) {
            BrandSection(
                brands = brands, // List<Brand> nhận từ HomeUiState.Success
                onBrandClick = { clickedBrand ->
                    // TODO: Chuyển sang màn hình Lọc sản phẩm theo Hãng
                }
            )
        }

        // 4. TrendingProductsSection
        item(span = { GridItemSpan(2) }) {
            TrendingProductsSection(
                products = trendingProducts,
                onSeeAllClick = onNavigateToExplore,
                onFavoriteClick = {},
                onProductClick = { product -> onProductClick(product.id) }
            )
        }

        // 5. NewArrivalsSection
        item(span = { GridItemSpan(2) }) {
            NewArrivalsSection(
                products = newArrivals,
                onSeeAllClick = onNavigateToExplore,
                onFavoriteClick = {},
                onProductClick = { product -> onProductClick(product.id) }
            )
        }

        // 6. RecommendedSection
        recommendedSection(
            products = recommendedProducts,
            onFavoriteClick = {},
            onProductClick = { product -> onProductClick(product.id) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TestHomeScreenUi() {
    HomeScreen(onNavigateToExplore = {})
}
