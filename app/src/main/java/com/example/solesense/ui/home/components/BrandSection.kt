package com.example.solesense.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.solesense.model.catalog.Brand
import com.example.solesense.ui.components.BrandItem

@Composable
fun BrandSection(
    brands: List<Brand>,
    onBrandClick: (Brand) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp), // Khoảng cách giữa các logo
        // Không dùng contentPadding ở đây vì LazyVerticalGrid ở HomeScreen đã bọc padding ngoài cùng rồi
    ) {
        items(
            items = brands,
            key = { it.brandId }
        ) { brand ->
            BrandItem(
                brand = brand,
                onClick = { onBrandClick(brand) }
            )
        }
    }
}
