package com.example.solesense.ui.product

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.solesense.ui.components.LoginRequiredDialog
import com.example.solesense.ui.product.components.*
import com.example.solesense.utils.AppAuthSessionState
import com.example.solesense.utils.UserSessionManager

@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onBackClick: () -> Unit,
    onCartClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNavigateToCheckout: (variantId: String) -> Unit,
    onNavigateToAuth: () -> Unit = {},
    onNavigateToChat: ((productId: String, name: String, price: Double, imageUrl: String, color: String, size: String) -> Unit)? = null,
    onReturnToRoleDashboard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by UserSessionManager.sessionState.collectAsState()
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val userRole = currentUser?.role?.uppercase() ?: ""
    val isReadOnlyPreview = userRole in setOf("ADMIN", "SUPPORT")
    val context = LocalContext.current

    // Tự động tiếp tục thao tác (Thêm vào giỏ / Mua ngay / Yêu thích) ngay khi đăng nhập xong và quay lại ProductDetail!
    LaunchedEffect(sessionState, uiState.pendingAuthAction) {
        if (!isReadOnlyPreview && sessionState is AppAuthSessionState.Authenticated && uiState.pendingAuthAction != null) {
            viewModel.consumeAndExecutePendingAction(
                onAddedToCart = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onNavigateToCheckout = onNavigateToCheckout
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            ProductTopBar(
                cartItemCount = if (isReadOnlyPreview) 0 else uiState.cartItemCount,
                onBackClick = onBackClick,
                onSearchClick = onSearchClick,
                onCartClick = {
                    if (isReadOnlyPreview) {
                        Toast.makeText(
                            context,
                            "Chế độ Chỉ xem ($userRole): Không sử dụng Giỏ hàng của khách",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onCartClick()
                    }
                },
                onMenuClick = {}
            )
        },
        floatingActionButton = {
            if (!isReadOnlyPreview) {
                ProductChatFab(
                    onClick = {
                        if (!UserSessionManager.isAuthenticated()) {
                            viewModel.openChatBottomSheet()
                        } else {
                            val product = uiState.product
                            if (product != null && onNavigateToChat != null) {
                                val color = uiState.selectedColor ?: uiState.selectedVariant?.color ?: "Trắng"
                                val size = uiState.selectedSize ?: uiState.selectedVariant?.size ?: "41"
                                val price = product.salePrice ?: product.basePrice
                                val img = product.imageUrls.firstOrNull() ?: ""
                                onNavigateToChat(product.productId, product.name, price, img, color, size)
                            } else {
                                viewModel.openChatBottomSheet()
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            val isVariantValid = (uiState.selectedVariant?.stock ?: 0) > 0

            ProductBottomBar(
                isBuyEnabled = isVariantValid,
                onAddToCartClick = {
                    if (!isReadOnlyPreview) {
                        viewModel.openBottomSheet(BottomSheetActionType.ADD_TO_CART)
                    }
                },
                onBuyNowClick = {
                    if (!isReadOnlyPreview) {
                        viewModel.openBottomSheet(BottomSheetActionType.BUY_NOW)
                    }
                },
                isReadOnlyPreview = isReadOnlyPreview,
                previewRoleName = userRole,
                onReturnToDashboardClick = onReturnToRoleDashboard
            )
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE53935))
            }
            return@Scaffold
        }

        if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Đã xảy ra lỗi: ${uiState.errorMessage}", color = Color.Red)
            }
            return@Scaffold
        }

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
                        isFavorite = if (isReadOnlyPreview) false else uiState.isFavorite,
                        onFavoriteClick = {
                            if (isReadOnlyPreview) {
                                Toast.makeText(
                                    context,
                                    "Chế độ Chỉ xem ($userRole): Không thao tác Yêu thích",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.toggleFavorite()
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp).fillMaxWidth().background(Color(0xFFF5F5F5)))
                }

                item {
                    VariantSelectorSection(
                        colors = uiState.availableColors,
                        sizes = uiState.availableSizesForSelectedColor,
                        selectedColorName = uiState.selectedColor,
                        selectedSizeName = uiState.selectedSize,
                        onColorSelected = viewModel::selectColor,
                        onSizeSelected = viewModel::selectSize,
                        onSizeGuideClick = viewModel::openSizeGuide
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp).fillMaxWidth().background(Color(0xFFF5F5F5)))
                }

                item {
                    ProductInfoSection(
                        description = product.description.ifEmpty { "Chưa có mô tả sản phẩm." },
                        features = emptyList()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp).fillMaxWidth().background(Color(0xFFF5F5F5)))
                }

                item {
                    ProductReviewSection(
                        averageRating = 4.8,
                        totalReviews = 120,
                        reviews = emptyList(),
                        onSeeAllClick = {}
                    )
                }

                item {
                    RelatedProductsSection(
                        products = emptyList(),
                        onProductClick = {}
                    )
                }

                item {
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
            onSizeGuideClick = viewModel::openSizeGuide,
            onActionClick = {
                viewModel.handleVariantActionClick(
                    onAddedToCart = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onNavigateToCheckout = onNavigateToCheckout
                )
            }
        )

        // BottomSheet Hướng dẫn chọn cỡ giày (Khách Guest & Customer đều dùng được)
        SizeGuideBottomSheet(
            isVisible = uiState.isSizeGuideVisible,
            onDismissRequest = viewModel::closeSizeGuide
        )

        // Hộp thoại yêu cầu đăng nhập khi Khách chưa đăng nhập bấm Thêm vào giỏ / Mua ngay / Yêu thích
        LoginRequiredDialog(
            isVisible = uiState.isLoginRequiredDialogVisible,
            onLoginClick = {
                viewModel.hideLoginDialogKeepPendingAction()
                onNavigateToAuth()
            },
            onDismiss = viewModel::dismissLoginRequiredDialog
        )

        // BottomSheet Chat tư vấn khi bấm vào nút FAB bên phải màn hình
        ProductChatBottomSheet(
            isVisible = uiState.isChatBottomSheetVisible,
            onDismissRequest = viewModel::closeChatBottomSheet
        )
    }
}
