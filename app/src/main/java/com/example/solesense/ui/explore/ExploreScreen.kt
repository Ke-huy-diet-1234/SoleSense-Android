package com.example.solesense.ui.explore

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solesense.R
import com.example.solesense.data.repository.ProductRepository
import com.example.solesense.model.catalog.Brand
import com.example.solesense.model.catalog.ProductBadge
import com.example.solesense.model.catalog.ProductModel
import com.example.solesense.repository.BrandRepository
import com.example.solesense.repository.FavoriteRepository
import com.example.solesense.repository.NotificationRepository
import com.example.solesense.ui.components.LoginRequiredDialog
import com.example.solesense.ui.components.ProductCard
import com.example.solesense.utils.SearchHistoryDatabase
import com.example.solesense.utils.UserSessionManager
import com.example.solesense.utils.removeVietnameseAccents
import com.example.solesense.utils.toProductModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Bộ quản lý trạng thái điều hướng sang màn hình Khám phá từ Trang chủ
 * (Khi bấm vào Thương hiệu, Xem tất cả Thịnh hành/Mới về, hoặc bấm thanh Tìm kiếm / Bộ lọc)
 */
object ExploreFilterStateHolder {
    private val _pendingBrandId = MutableStateFlow<String?>(null)
    val pendingBrandId: StateFlow<String?> = _pendingBrandId.asStateFlow()

    private val _pendingSortBy = MutableStateFlow<String?>(null)
    val pendingSortBy: StateFlow<String?> = _pendingSortBy.asStateFlow()

    private val _pendingOpenFilterSheet = MutableStateFlow(false)
    val pendingOpenFilterSheet: StateFlow<Boolean> = _pendingOpenFilterSheet.asStateFlow()

    fun navigateWithBrand(brandId: String) {
        _pendingBrandId.value = if (brandId.equals("new_balance", ignoreCase = true)) "newbalance" else brandId
    }

    fun navigateWithSort(sortBy: String) {
        _pendingSortBy.value = sortBy
    }

    fun navigateWithFilterSheet() {
        _pendingOpenFilterSheet.value = true
    }

    fun consumePendingBrand(): String? {
        val v = _pendingBrandId.value
        _pendingBrandId.value = null
        return v
    }

    fun consumePendingSort(): String? {
        val v = _pendingSortBy.value
        _pendingSortBy.value = null
        return v
    }

    fun consumePendingFilterSheet(): Boolean {
        val v = _pendingOpenFilterSheet.value
        _pendingOpenFilterSheet.value = false
        return v
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onProductClick: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val productRepository = remember { ProductRepository() }
    val brandRepository = remember { BrandRepository() }
    val favoriteRepository = remember { FavoriteRepository() }
    val searchHistoryDb = remember { SearchHistoryDatabase.getInstance(context) }

    val currentUser by UserSessionManager.currentUser.collectAsState()
    val isLoggedIn = currentUser != null
    val userRole = currentUser?.role?.uppercase() ?: ""
    val isReadOnlyPreview = userRole in setOf("ADMIN", "SUPPORT")

    val unreadNotifCount by NotificationRepository.unreadCount.collectAsState()
    val recentSearches by SearchHistoryDatabase.recentSearches.collectAsState()

    var showLoginRequiredDialog by remember { mutableStateOf(false) }

    var allProducts by remember { mutableStateOf<List<ProductModel>>(emptyList()) }
    var brands by remember { mutableStateOf<List<Brand>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Trạng thái Tìm kiếm & Bộ lọc
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedBrandId by remember { mutableStateOf("ALL") }
    var selectedCategoryId by remember { mutableStateOf("ALL") }
    var selectedSortBy by remember { mutableStateOf("POPULAR") } // POPULAR, NEWEST, PRICE_LOW, PRICE_HIGH, RATING_HIGH
    var minPriceFilter by remember { mutableFloatStateOf(0f) }
    var maxPriceFilter by remember { mutableFloatStateOf(6000000f) }
    var minRatingFilter by remember { mutableIntStateOf(0) } // 0 = Tất cả, 5, 4, 3, 2
    var onlyInStockFilter by remember { mutableStateOf(false) }

    var isFilterSheetOpen by remember { mutableStateOf(false) }

    // Lắng nghe yêu cầu lọc chuyển sang từ HomeScreen
    val pendingBrand by ExploreFilterStateHolder.pendingBrandId.collectAsState()
    val pendingSort by ExploreFilterStateHolder.pendingSortBy.collectAsState()
    val pendingSheet by ExploreFilterStateHolder.pendingOpenFilterSheet.collectAsState()

    LaunchedEffect(pendingBrand, pendingSort, pendingSheet) {
        ExploreFilterStateHolder.consumePendingBrand()?.let { brandId ->
            selectedBrandId = brandId
            searchQuery = ""
        }
        ExploreFilterStateHolder.consumePendingSort()?.let { sort ->
            selectedSortBy = sort
        }
        if (ExploreFilterStateHolder.consumePendingFilterSheet()) {
            isFilterSheetOpen = true
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        productRepository.getProducts().onSuccess { raw ->
            allProducts = raw.map { it.toProductModel() }
        }
        brandRepository.getActiveBrands().onSuccess { bList ->
            brands = bList
        }
        isLoading = false
    }

    // Áp dụng điều kiện Tìm kiếm tiếng Việt (có dấu & không dấu) + Thương hiệu + Danh mục + Khoảng giá + Số sao + Còn hàng + Sắp xếp
    val filteredProducts = remember(
        allProducts,
        searchQuery,
        selectedBrandId,
        selectedCategoryId,
        selectedSortBy,
        minPriceFilter,
        maxPriceFilter,
        minRatingFilter,
        onlyInStockFilter
    ) {
        val qRaw = searchQuery.trim().lowercase(Locale.getDefault())
        val qNorm = qRaw.removeVietnameseAccents()
        val rawTokens = qRaw.split(Regex("\\s+")).filter { it.isNotBlank() }
        val normTokens = qNorm.split(Regex("\\s+")).filter { it.isNotBlank() }

        val normSelectedBrand = if (selectedBrandId.equals("new_balance", ignoreCase = true)) {
            "newbalance"
        } else {
            selectedBrandId.lowercase(Locale.getDefault())
        }

        val filtered = allProducts.filter { p ->
            val matchQuery = if (qRaw.isEmpty()) {
                true
            } else {
                val blob = p.searchKeywords
                blob.contains(qRaw) ||
                    blob.contains(qNorm) ||
                    (rawTokens.isNotEmpty() && rawTokens.all { tok -> blob.contains(tok) }) ||
                    (normTokens.isNotEmpty() && normTokens.all { tok -> blob.contains(tok) })
            }

            val matchBrand = selectedBrandId == "ALL" || p.brandId.equals(normSelectedBrand, ignoreCase = true)
            val matchCat = selectedCategoryId == "ALL" || p.categoryId.equals(selectedCategoryId, ignoreCase = true)
            val matchPrice = p.currentPrice >= minPriceFilter.toLong() && p.currentPrice <= maxPriceFilter.toLong()
            val matchRating = minRatingFilter == 0 || p.rating >= minRatingFilter.toDouble()
            val matchStock = !onlyInStockFilter || (!p.isOutOfStock && p.stockQuantity > 0)

            matchQuery && matchBrand && matchCat && matchPrice && matchRating && matchStock
        }

        when (selectedSortBy) {
            "NEWEST" -> filtered.sortedWith(
                compareByDescending<ProductModel> { it.badge == ProductBadge.NEW }
                    .thenByDescending { it.createdAtSeconds }
            )
            "PRICE_LOW" -> filtered.sortedBy { it.currentPrice }
            "PRICE_HIGH" -> filtered.sortedByDescending { it.currentPrice }
            "RATING_HIGH" -> filtered.sortedByDescending { it.rating }
            else -> filtered.sortedByDescending { it.rawSoldCount } // POPULAR
        }
    }

    val categories = listOf(
        "ALL" to "🔥 Tất cả",
        "running" to "🏃 Chạy bộ",
        "training" to "🏋️ Tập luyện",
        "lifestyle" to "⭐ Thời trang",
        "basketball" to "🏀 Bóng rổ"
    )

    val quickSortOptions = listOf(
        "POPULAR" to "🔥 Bán chạy",
        "PRICE_LOW" to "🏷️ Giá tốt",
        "NEWEST" to "👑 Mới nhất"
    )

    fun handleFavoriteToggle(product: ProductModel) {
        if (isReadOnlyPreview) {
            Toast.makeText(context, "Chế độ Chỉ xem ($userRole)", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isLoggedIn) {
            showLoginRequiredDialog = true
        } else {
            scope.launch {
                favoriteRepository.toggleFavorite(product.id).onSuccess { isFav ->
                    val msg = if (isFav) "Đã thêm ${product.name} vào yêu thích" else "Đã bỏ khỏi yêu thích"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 1. HEADER KHÁM PHÁ & CHUÔNG THÔNG BÁO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Khám phá",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 23.sp,
                    color = Color(0xFF111827)
                )
            )

            IconButton(
                onClick = {
                    if (!isLoggedIn) {
                        showLoginRequiredDialog = true
                    } else {
                        onNavigateToNotifications()
                    }
                }
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Thông báo",
                        tint = Color(0xFF111827),
                        modifier = Modifier.size(24.dp)
                    )
                    if (isLoggedIn && unreadNotifCount > 0) {
                        val badgeLabel = if (unreadNotifCount > 9) "9+" else unreadNotifCount.toString()
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-1).dp, y = 2.dp)
                                .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                                .border(1.5.dp, Color.White, CircleShape)
                                .padding(horizontal = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeLabel,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 2. THANH TÌM KIẾM + NÚT BỘ LỌC
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color(0xFFF3F4F6))
                    .border(
                        1.dp,
                        if (isSearchFocused) Color(0xFF111827) else Color.Transparent,
                        RoundedCornerShape(23.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            // Khi người dùng gõ từ khóa tìm kiếm, tự động mở rộng tìm trên toàn bộ hãng
                            // để không bị kẹt bởi hãng đã bấm chọn trước đó
                            if (it.isNotBlank() && selectedBrandId != "ALL") {
                                selectedBrandId = "ALL"
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    searchHistoryDb.addSearchKeyword(searchQuery)
                                }
                                isSearchFocused = false
                                focusManager.clearFocus()
                            }
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF111827),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { state ->
                                isSearchFocused = state.isFocused
                            },
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Tìm giày, thương hiệu, chạy bộ,...",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 13.5.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty() || isSearchFocused) {
                        IconButton(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    searchQuery = ""
                                } else {
                                    isSearchFocused = false
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Nút mở BottomSheet Sắp xếp & Bộ lọc
            Surface(
                shape = CircleShape,
                color = Color(0xFFF3F4F6),
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable {
                        isSearchFocused = false
                        focusManager.clearFocus()
                        isFilterSheetOpen = true
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter_list),
                        contentDescription = "Bộ lọc",
                        tint = Color(0xFF111827),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. NẾU ĐANG FOCUS Ô TÌM KIẾM & CHƯA NHẬP GÌ -> HIỂN THỊ LỊCH SỬ TÌM KIẾM
        if (isSearchFocused && searchQuery.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tìm kiếm gần đây",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF111827)
                        )
                    )
                    if (recentSearches.isNotEmpty()) {
                        TextButton(onClick = { searchHistoryDb.clearAllHistory() }) {
                            Text(
                                text = "Xóa tất cả",
                                color = Color(0xFF111827),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF3F4F6))
                Spacer(modifier = Modifier.height(6.dp))

                if (recentSearches.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có lịch sử tìm kiếm gần đây",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(recentSearches, key = { it }) { keyword ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        searchQuery = keyword
                                        selectedBrandId = "ALL"
                                        searchHistoryDb.addSearchKeyword(keyword)
                                        isSearchFocused = false
                                        focusManager.clearFocus()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = keyword,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color(0xFF4B5563),
                                        fontSize = 15.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(1.2.dp, Color(0xFF9CA3AF), CircleShape)
                                        .clickable {
                                            searchHistoryDb.deleteSearchKeyword(keyword)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa",
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 4. NỘI DUNG KHÁM PHÁ CHÍNH & DANH SÁCH KẾT QUẢ
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Hàng chọn Thương hiệu: Mục "Tất cả" + Các Icon Thương hiệu
                if (searchQuery.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                val isAllSelected = selectedBrandId == "ALL"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(62.dp)
                                        .clickable { selectedBrandId = "ALL" }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(if (isAllSelected) Color(0xFF111827) else Color(0xFFF5F5F7))
                                            .border(
                                                width = if (isAllSelected) 2.dp else 1.dp,
                                                color = if (isAllSelected) Color(0xFF111827) else Color(0xFFE5E7EB),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_category),
                                            contentDescription = "Tất cả",
                                            tint = if (isAllSelected) Color.White else Color(0xFF111827),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tất cả",
                                        fontSize = 12.sp,
                                        fontWeight = if (isAllSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isAllSelected) Color(0xFF111827) else Color(0xFF6B7280),
                                        maxLines = 1
                                    )
                                }
                            }

                            items(brands, key = { it.brandId }) { brand ->
                                val isSelected = selectedBrandId.equals(brand.brandId, ignoreCase = true)
                                val resolvedLogo = when (brand.brandId.lowercase()) {
                                    "nike" -> "file:///android_asset/brands/Logo_NIKE.svg"
                                    "adidas" -> "file:///android_asset/brands/Adidas_Logo.svg"
                                    "asics" -> "file:///android_asset/brands/Asics_Logo.svg"
                                    "newbalance", "new_balance" -> "file:///android_asset/brands/New_Balance_Logo.svg"
                                    "converse" -> "file:///android_asset/brands/Converse_Logo.svg"
                                    "puma" -> "file:///android_asset/brands/Puma_Logo.svg"
                                    "reebok" -> "file:///android_asset/brands/Reebok_Logo.svg"
                                    else -> brand.logoUrl
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(62.dp)
                                        .clickable {
                                            selectedBrandId = if (isSelected) "ALL" else brand.brandId
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF5F5F7))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF111827) else Color(0xFFE5E7EB),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = resolvedLogo,
                                            contentDescription = brand.name,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(13.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = brand.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF111827) else Color(0xFF6B7280),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Hàng chọn Danh mục + Hàng Sắp xếp nhanh
                item(span = { GridItemSpan(2) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { (catCode, catLabel) ->
                                val isSelected = selectedCategoryId.equals(catCode, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFF111827) else Color(0xFFF3F4F6),
                                    modifier = Modifier.clickable { selectedCategoryId = catCode }
                                ) {
                                    Text(
                                        text = catLabel,
                                        color = if (isSelected) Color.White else Color(0xFF374151),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.5.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(quickSortOptions) { (sortCode, sortLabel) ->
                                val isSelected = selectedSortBy == sortCode
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFFFFF7ED) else Color.White,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFFF97316) else Color(0xFFE5E7EB)
                                    ),
                                    modifier = Modifier.clickable { selectedSortBy = sortCode }
                                ) {
                                    Text(
                                        text = sortLabel,
                                        color = if (isSelected) Color(0xFFC2410C) else Color(0xFF4B5563),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                    modifier = Modifier.clickable { isFilterSheetOpen = true }
                                ) {
                                    Text(
                                        text = "⚙️ Bộ lọc khác ›",
                                        color = Color(0xFF374151),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tiêu đề số lượng kết quả
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val headerText = if (searchQuery.isNotBlank()) {
                            "Kết quả cho \"${searchQuery.trim()}\""
                        } else {
                            "Danh sách sản phẩm"
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFF111827)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${filteredProducts.size} sản phẩm",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4B5563),
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                if (isLoading) {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF111827))
                        }
                    }
                } else if (filteredProducts.isEmpty()) {
                    // GIAO DIỆN KHÔNG TÌM THẤY KẾT QUẢ
                    item(span = { GridItemSpan(2) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 44.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_not_found),
                                contentDescription = "Không tìm thấy kết quả",
                                modifier = Modifier.size(135.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Không tìm thấy kết quả",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF111827),
                                    fontSize = 19.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Rất tiếc, từ khóa hoặc bộ lọc bạn chọn chưa có sản phẩm phù hợp. Vui lòng kiểm tra lại hoặc thử từ khóa khác.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF6B7280),
                                    lineHeight = 20.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedBrandId = "ALL"
                                    selectedCategoryId = "ALL"
                                    minPriceFilter = 0f
                                    maxPriceFilter = 6000000f
                                    minRatingFilter = 0
                                },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Đặt lại toàn bộ bộ lọc", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    items(filteredProducts, key = { p -> p.id }) { product ->
                        ProductCard(
                            product = product,
                            onFavoriteClick = { handleFavoriteToggle(product) },
                            onProductClick = {
                                if (searchQuery.isNotBlank()) {
                                    searchHistoryDb.addSearchKeyword(searchQuery)
                                }
                                onProductClick(product.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // BOTTOM SHEET SẮP XẾP & BỘ LỌC (Đã thu gọn khoảng cách và sửa lỗi nút bấm Ảnh 3 & Ảnh 4)
    if (isFilterSheetOpen) {
        SortAndFilterBottomSheet(
            brands = brands,
            initialBrandId = selectedBrandId,
            initialCategoryId = selectedCategoryId,
            initialSortBy = selectedSortBy,
            initialMinPrice = minPriceFilter,
            initialMaxPrice = maxPriceFilter,
            initialMinRating = minRatingFilter,
            initialOnlyInStock = onlyInStockFilter,
            onDismiss = { isFilterSheetOpen = false },
            onApply = { brandId, catId, sortBy, minP, maxP, minR, inStockOnly ->
                selectedBrandId = brandId
                selectedCategoryId = catId
                selectedSortBy = sortBy
                minPriceFilter = minP
                maxPriceFilter = maxP
                minRatingFilter = minR
                onlyInStockFilter = inStockOnly
                isFilterSheetOpen = false
            },
            onReset = {
                selectedBrandId = "ALL"
                selectedCategoryId = "ALL"
                selectedSortBy = "POPULAR"
                minPriceFilter = 0f
                maxPriceFilter = 6000000f
                minRatingFilter = 0
                onlyInStockFilter = false
                isFilterSheetOpen = false
            }
        )
    }

    LoginRequiredDialog(
        isVisible = showLoginRequiredDialog,
        onLoginClick = {
            showLoginRequiredDialog = false
            onNavigateToLogin()
        },
        onDismiss = { showLoginRequiredDialog = false }
    )
}

/**
 * Bottom Sheet "Sắp xếp & Bộ lọc" gọn gàng, mở trực tiếp ở độ cao chuẩn (skipPartiallyExpanded = true),
 * khoảng cách giữa các mục vừa vặn không chiếm hết màn hình (Ảnh 3) và chữ trên nút không bao giờ bị gãy dòng (Ảnh 4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortAndFilterBottomSheet(
    brands: List<Brand>,
    initialBrandId: String,
    initialCategoryId: String,
    initialSortBy: String,
    initialMinPrice: Float,
    initialMaxPrice: Float,
    initialMinRating: Int,
    initialOnlyInStock: Boolean = false,
    onDismiss: () -> Unit,
    onApply: (String, String, String, Float, Float, Int, Boolean) -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempBrand by remember { mutableStateOf(initialBrandId) }
    var tempCategory by remember { mutableStateOf(initialCategoryId) }
    var tempSort by remember { mutableStateOf(initialSortBy) }
    var priceRange by remember { mutableStateOf(initialMinPrice..initialMaxPrice) }
    var tempRating by remember { mutableIntStateOf(initialMinRating) }
    var tempOnlyInStock by remember { mutableStateOf(initialOnlyInStock) }

    val numFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Sắp xếp & Bộ lọc",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF111827)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(10.dp))

            // 1. THƯƠNG HIỆU
            Text(
                text = "Thương hiệu",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterPillOption(
                        label = "Tất cả",
                        isSelected = tempBrand == "ALL",
                        onClick = { tempBrand = "ALL" }
                    )
                }
                items(brands) { b ->
                    FilterPillOption(
                        label = b.name,
                        isSelected = tempBrand.equals(b.brandId, ignoreCase = true),
                        onClick = { tempBrand = b.brandId }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. DANH MỤC
            Text(
                text = "Danh mục",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            val catOptions = listOf(
                "ALL" to "Tất cả",
                "running" to "Chạy bộ",
                "training" to "Tập luyện",
                "lifestyle" to "Thời trang",
                "basketball" to "Bóng rổ"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(catOptions) { (code, label) ->
                    FilterPillOption(
                        label = label,
                        isSelected = tempCategory.equals(code, ignoreCase = true),
                        onClick = { tempCategory = code }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. KHOẢNG GIÁ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Khoảng giá",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                )
                Text(
                    text = "${numFormat.format(priceRange.start.toLong())}đ – ${numFormat.format(priceRange.endInclusive.toLong())}đ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = Color(0xFF111827)
                )
            }

            RangeSlider(
                value = priceRange,
                onValueChange = { priceRange = it },
                valueRange = 0f..6000000f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF111827),
                    activeTrackColor = Color(0xFF111827),
                    inactiveTrackColor = Color(0xFFE5E7EB)
                ),
                modifier = Modifier.height(36.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. SẮP XẾP THEO
            Text(
                text = "Sắp xếp theo",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            val sortOptions = listOf(
                "POPULAR" to "Phổ biến",
                "NEWEST" to "Mới nhất",
                "PRICE_HIGH" to "Giá cao",
                "PRICE_LOW" to "Giá thấp"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sortOptions) { (code, label) ->
                    FilterPillOption(
                        label = label,
                        isSelected = tempSort == code,
                        onClick = { tempSort = code }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. ĐÁNH GIÁ & TÌNH TRẠNG KHO
            Text(
                text = "Đánh giá",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            val ratingOptions = listOf(0 to "★ Tất cả", 5 to "★ 5", 4 to "★ 4", 3 to "★ 3", 2 to "★ 2")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ratingOptions) { (stars, label) ->
                    FilterPillOption(
                        label = label,
                        isSelected = tempRating == stars,
                        onClick = { tempRating = stars }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chỉ hiện sản phẩm còn hàng",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                )
                Switch(
                    checked = tempOnlyInStock,
                    onCheckedChange = { tempOnlyInStock = it }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(12.dp))

            // Nút Đặt lại & Áp dụng (Ảnh 4 - Chữ 1 dòng gọn gàng, không bao giờ bị cắt)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onReset,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = Color(0xFF111827)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Đặt lại",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Button(
                    onClick = {
                        onApply(
                            tempBrand,
                            tempCategory,
                            tempSort,
                            priceRange.start,
                            priceRange.endInclusive,
                            tempRating,
                            tempOnlyInStock
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111827),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Áp dụng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPillOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF111827) else Color.White,
        border = BorderStroke(1.2.dp, if (isSelected) Color(0xFF111827) else Color(0xFFD1D5DB)),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF111827),
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
