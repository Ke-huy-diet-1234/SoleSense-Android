package com.example.solesense.ui.product

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.solesense.ui.product.components.*

@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onBackClick: () -> Unit,
    onCartClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNavigateToCheckout: (variantId: String) -> Unit, // Điều hướng sang trang thanh toán
    modifier: Modifier = Modifier
) {
    // Thu thập trạng thái từ ViewModel một cách an toàn (Lifecycle-aware)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            ProductTopBar(
                cartItemCount = uiState.cartItemCount,
                onBackClick = onBackClick,
                onSearchClick = onSearchClick,
                onCartClick = onCartClick,
                onMenuClick = { /* TODO: Hiện BottomSheet các tùy chọn phụ */ }
            )
        },
        floatingActionButton = {
            ProductChatFab(
                onClick = viewModel::openChatBottomSheet
            )
        },
        bottomBar = {
            // Khóa/Mở nút Mua dựa vào tính hợp lệ của biến thể được chọn
            val isVariantValid = (uiState.selectedVariant?.stock ?: 0) > 0

            ProductBottomBar(
                isBuyEnabled = isVariantValid,
                onAddToCartClick = {
                    if (uiState.selectedVariant != null) {
                        viewModel.openBottomSheet(BottomSheetActionType.ADD_TO_CART)
                    } else {
                        // Nếu chưa chọn biến thể, mở bottomsheet để chọn
                        viewModel.openBottomSheet(BottomSheetActionType.ADD_TO_CART)
                    }
                },
                onBuyNowClick = {
                    viewModel.openBottomSheet(BottomSheetActionType.BUY_NOW)
                }
            )
        }
    ) { paddingValues ->

        // 1. Xử lý trạng thái Loading
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE53935))
            }
            return@Scaffold
        }

        // 2. Xử lý trạng thái Lỗi
        if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Đã xảy ra lỗi: ${uiState.errorMessage}", color = Color.Red)
            }
            return@Scaffold
        }

        // 3. Render giao diện chính khi có dữ liệu
        uiState.product?.let { product ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
            ) {
                item {
                    ProductImageGallery(
                        imageUrls = uiState.selectedColorImages,
                        discountPercent = uiState.discountPercent,
                        selectedImageIndex = uiState.selectedImageIndex,
                        imageScrollTrigger = uiState.imageScrollTrigger
                    )
                }

                item {
                    ProductSummarySection(
                        productName = product.name,
                        displayPrice = uiState.displayPrice,
                        displayMinPrice = uiState.displayMinPrice,
                        displayMaxPrice = uiState.displayMaxPrice,
                        displayOriginalPrice = uiState.displayOriginalPrice,
                        soldCount = product.totalSoldCount,
                        isFavorite = uiState.isFavorite,
                        onFavoriteClick = viewModel::toggleFavorite
                    )
                }

                item {
                    // Dải phân cách mờ xám
                    Spacer(modifier = Modifier.height(8.dp).fillMaxWidth().background(Color(0xFFF5F5F5)))
                }

                item {
                    VariantSelectorSection(
                        colors = uiState.availableColors,
                        sizes = uiState.availableSizesForSelectedColor,
                        selectedColorName = uiState.selectedColor,
                        selectedSizeName = uiState.selectedSize,
                        onColorSelected = viewModel::selectColor, // Truyền trực tiếp reference function
                        onSizeSelected = viewModel::selectSize
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp).fillMaxWidth().background(Color(0xFFF5F5F5)))
                }

                item {
                    ProductInfoSection(
                        description = product.description.ifEmpty { "Chưa có mô tả sản phẩm." },
                        features = emptyList() // Truyền list mock data nếu cần test UI gạch đầu dòng
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp).fillMaxWidth().background(Color(0xFFF5F5F5)))
                }

                item {
                    // Tạm thời truyền mảng rỗng hoặc tạo vài object ReviewUiModel giả lập vào đây
                    ProductReviewSection(
                        averageRating = 4.8, // Mock data
                        totalReviews = 120, // Mock data
                        reviews = emptyList(), // Thay bằng mock data để test UI
                        onSeeAllClick = { /* Điều hướng sang màn Tất cả đánh giá */ }
                    )
                }

                item {
                    RelatedProductsSection(
                        products = emptyList(), // uiState.relatedProducts
                        onProductClick = { /* Điều hướng đè lên màn chi tiết hiện tại bằng ID mới */ }
                    )
                }

                item {
                    // Khoảng trống an toàn đẩy nội dung lên trên BottomBar
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // BottomSheet lựa chọn biến thể & số lượng
        val selectedThumb = uiState.availableColors.firstOrNull { it.colorName == uiState.selectedColor }?.thumbnailUrl
            ?: uiState.selectedColorImages.firstOrNull()

        ProductVariantBottomSheet(
            isVisible = uiState.isBottomSheetVisible,
            actionType = uiState.bottomSheetActionType,
            imageUrl = selectedThumb,
            displayPrice = uiState.displayPrice,
            displayMinPrice = uiState.displayMinPrice,
            displayMaxPrice = uiState.displayMaxPrice,
            displayOriginalPrice = uiState.displayOriginalPrice,
            stockCount = uiState.selectedVariant?.stock,
            colors = uiState.availableColors,
            sizes = uiState.availableSizesForSelectedColor,
            selectedQuantity = uiState.selectedQuantity,
            isActionEnabled = (uiState.selectedVariant?.stock ?: 0) > 0,
            onDismissRequest = viewModel::closeBottomSheet,
            onColorSelected = viewModel::selectColor,
            onSizeSelected = viewModel::selectSize,
            onIncreaseQuantity = viewModel::increaseQuantity,
            onDecreaseQuantity = viewModel::decreaseQuantity,
            onActionClick = {
                uiState.selectedVariant?.let { variant ->
                    when (uiState.bottomSheetActionType) {
                        BottomSheetActionType.ADD_TO_CART -> {
                            viewModel.addToCart(variant.variantId)
                            Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                        }
                        BottomSheetActionType.BUY_NOW -> {
                            if (viewModel.prepareDirectCheckout()) {
                                onNavigateToCheckout(variant.variantId)
                            }
                        }
                    }
                }
            }
        )

        // BottomSheet Chat tư vấn khi bấm vào nút FAB bên phải màn hình
        ProductChatBottomSheet(
            isVisible = uiState.isChatBottomSheetVisible,
            onDismissRequest = viewModel::closeChatBottomSheet
        )
    }
}
