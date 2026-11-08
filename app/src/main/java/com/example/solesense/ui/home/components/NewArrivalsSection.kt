package com.example.solesense.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.solesense.model.catalog.ProductModel
import com.example.solesense.ui.components.ProductCard
import com.example.solesense.ui.components.SectionHeader

@Composable
fun NewArrivalsSection(
    products: List<ProductModel>,
    onSeeAllClick: () -> Unit,
    onFavoriteClick: (ProductModel) -> Unit,
    onProductClick: (ProductModel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Tái sử dụng SectionHeader đã viết ở thư mục components/
        SectionHeader(
            title = "Hàng mới về",
            onSeeAllClick = onSeeAllClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp) // Tránh bị cắt viền shadow nếu có
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onFavoriteClick = { onFavoriteClick(product) },
                    onProductClick = { onProductClick(product) },
                    modifier = Modifier.width(160.dp) // Cố định chiều rộng để Card không bị méo khi lướt ngang
                )
            }
        }
    }
}