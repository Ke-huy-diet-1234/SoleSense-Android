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
fun TrendingProductsSection(
    products: List<ProductModel>,
    onSeeAllClick: () -> Unit,
    onFavoriteClick: (ProductModel) -> Unit,
    onProductClick: (ProductModel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Thịnh hành",
            onSeeAllClick = onSeeAllClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onFavoriteClick = { onFavoriteClick(product) },
                    onProductClick = { onProductClick(product) },
                    modifier = Modifier.width(160.dp)
                )
            }
        }
    }
}