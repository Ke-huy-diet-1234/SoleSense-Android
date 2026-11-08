package com.example.solesense.ui.home.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.model.catalog.ProductModel
import com.example.solesense.ui.components.ProductCard

fun LazyGridScope.recommendedSection(
    products: List<ProductModel>,
    onFavoriteClick: (ProductModel) -> Unit,
    onProductClick: (ProductModel) -> Unit = {}
) {
    // Tiêu đề Section (Chiếm trọn 2 cột)
    item(span = { GridItemSpan(2) }) {
        Text(
            text = "Gợi ý dành cho bạn",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = Color.Black,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    // Danh sách sản phẩm gợi ý (Mỗi item tự động chiếm 1 cột)
    items(products, key = { it.id }) { product ->
        ProductCard(
            product = product,
            onFavoriteClick = { onFavoriteClick(product) },
            onProductClick = { onProductClick(product) }
        )
    }
}