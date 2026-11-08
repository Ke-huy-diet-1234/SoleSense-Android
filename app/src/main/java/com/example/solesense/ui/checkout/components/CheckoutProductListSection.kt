package com.example.solesense.ui.checkout.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.solesense.model.order.OrderItem

@Composable
fun CheckoutProductListSection(
    items: List<OrderItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        CheckoutSectionHeader(
            title = "DANH SÁCH SẢN PHẨM",
            subtitle = "Sản phẩm (${items.sumOf { it.quantity }})"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { item ->
                CheckoutProductItem(item = item)
            }
        }
    }
}

