package com.example.solesense.ui.product.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.solesense.ui.product.BottomSheetActionType
import com.example.solesense.ui.product.ColorOptionUiModel
import com.example.solesense.ui.product.SizeOptionUiModel
import com.example.solesense.ui.product.components.variant.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductVariantBottomSheet(
    isVisible: Boolean,
    actionType: BottomSheetActionType,
    imageUrl: String?,
    displayPrice: Double,
    displayMinPrice: Double?,
    displayMaxPrice: Double?,
    displayOriginalPrice: Double?,
    stockCount: Int?,
    colors: List<ColorOptionUiModel>,
    sizes: List<SizeOptionUiModel>,
    selectedQuantity: Int,
    isActionEnabled: Boolean,
    onDismissRequest: () -> Unit,
    onColorSelected: (String) -> Unit,
    onSizeSelected: (String) -> Unit,
    onIncreaseQuantity: () -> Unit,
    onDecreaseQuantity: () -> Unit,
    onActionClick: () -> Unit,
    onSizeGuideClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // 1. Nút Đóng góc trên phải
            VariantSheetHeader(onCloseClick = onDismissRequest)

            // Vùng nội dung cuộn
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // 2. Xem trước ảnh, giá, kho
                VariantProductPreview(
                    imageUrl = imageUrl,
                    displayPrice = displayPrice,
                    displayMinPrice = displayMinPrice,
                    displayMaxPrice = displayMaxPrice,
                    displayOriginalPrice = displayOriginalPrice,
                    stockCount = stockCount
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))

                // 3. Chọn Màu sắc
                VariantColorSelector(
                    colors = colors,
                    onColorSelected = onColorSelected
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))

                // 4. Chọn Kích cỡ
                VariantSizeSelector(
                    sizes = sizes,
                    onSizeSelected = onSizeSelected,
                    onSizeGuideClick = onSizeGuideClick
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))

                // 5. Chọn Số lượng
                VariantQuantitySelector(
                    quantity = selectedQuantity,
                    maxStock = stockCount ?: 99,
                    onIncrease = onIncreaseQuantity,
                    onDecrease = onDecreaseQuantity
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 6. Nút Hành động cố định ở đáy (Thêm vào giỏ / Mua ngay)
            VariantActionButton(
                actionType = actionType,
                isEnabled = isActionEnabled,
                onClick = onActionClick
            )
        }
    }
}

