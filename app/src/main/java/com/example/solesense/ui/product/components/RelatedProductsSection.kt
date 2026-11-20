package com.example.solesense.ui.product.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.model.catalog.Product
import com.example.solesense.repository.FavoriteRepository
import com.example.solesense.ui.components.ProductCard
import com.example.solesense.utils.toProductModel
import kotlinx.coroutines.launch

@Composable
fun RelatedProductsSection(
    products: List<Product>,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) return

    val scope = rememberCoroutineScope()
    val favoriteRepository = remember { FavoriteRepository() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Sản phẩm tương tự dành cho bạn",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = Color(0xFF111827)
            ),
            modifier = Modifier.padding(bottom = 14.dp)
        )

        val chunkedProducts = products.chunked(2)

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            chunkedProducts.forEach { rowProducts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowProducts.forEach { product ->
                        Box(modifier = Modifier.weight(1f)) {
                            val model = product.toProductModel()
                            ProductCard(
                                product = model,
                                onFavoriteClick = {
                                    scope.launch {
                                        favoriteRepository.toggleFavorite(product.productId)
                                    }
                                },
                                onProductClick = { onProductClick(product.productId) }
                            )
                        }
                    }

                    if (rowProducts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
