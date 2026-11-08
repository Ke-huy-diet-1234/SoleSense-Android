package com.example.solesense.ui.product.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.model.catalog.Product

@Composable
fun RelatedProductsSection(
    products: List<Product>,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) return // Không hiển thị nếu không có sản phẩm liên quan

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Có thể bạn cũng thích",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Phân mảng 1D thành mảng 2D (mỗi nhóm 2 item) để vẽ giả lập Grid 2 cột
        val chunkedProducts = products.chunked(2)

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            chunkedProducts.forEach { rowProducts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Vẽ từng item trong cột
                    rowProducts.forEach { product ->
                        Box(modifier = Modifier.weight(1f)) {
                            // TODO: THAY THẾ TEXT BẰNG COMPONENT PRODUCT CARD CỦA BẠN TỪ HOMESCREEN
                            Text(text = "Card: ${product.name}") // Xóa dòng placeholder này
                        }
                    }

                    // Xử lý góc chết: Nếu hàng cuối chỉ có 1 sản phẩm, nhét 1 Spacer vào để sản phẩm không bị giãn to gấp đôi
                    if (rowProducts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
