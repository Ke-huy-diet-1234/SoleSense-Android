package com.example.solesense.ui.product.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.R

private data class SizeGuideStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val imageRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SizeGuideBottomSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val steps = listOf(
        SizeGuideStep(
            stepNumber = 1,
            title = "Đặt bàn chân lên giấy",
            description = "Đứng thẳng, gót sát tường, đặt bàn chân lên tờ giấy trắng.",
            imageRes = R.drawable.img_step_1
        ),
        SizeGuideStep(
            stepNumber = 2,
            title = "Đánh dấu chiều dài",
            description = "Dùng bút đánh dấu điểm đầu và cuối của bàn chân.",
            imageRes = R.drawable.img_step_2
        ),
        SizeGuideStep(
            stepNumber = 3,
            title = "Đo chiều dài",
            description = "Dùng thước đo khoảng cách giữa hai điểm vừa đánh dấu.",
            imageRes = R.drawable.img_step_
        ),
        SizeGuideStep(
            stepNumber = 4,
            title = "Đối chiếu bảng size",
            description = "So sánh với bảng size bên dưới để chọn kích thước phù hợp.",
            imageRes = R.drawable.img_step_4
        )
    )

    val euSizes = listOf("36", "37", "38", "39", "40", "41", "42", "43", "44")
    val footLengthsCm = listOf("22.5", "23", "23.5", "24.5", "25", "25.5", "26.5", "27", "27.5")

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // 1. Tiêu đề: Cách chọn size giày
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shoe),
                        contentDescription = "Cách chọn size giày",
                        tint = Color(0xFF1A1A1A),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cách chọn size giày",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = Color(0xFF1A1A1A)
                        )
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Đóng hướng dẫn",
                    tint = Color(0xFF616161),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onDismissRequest() }
                        .padding(2.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Khung Mẹo nhỏ
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF5F5F5),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEBEBEB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_idea),
                            contentDescription = "Mẹo nhỏ",
                            tint = Color(0xFF212121),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mẹo nhỏ",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1A1A1A)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nên đo chiều dài bàn chân vào cuối ngày, khi bàn chân đã giãn nở hoàn toàn để có kết quả chính xác nhất.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF616161)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Quy trình 4 bước (Xếp ngang 4 cột đúng theo ảnh thiết kế)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                steps.forEach { step ->
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = step.imageRes),
                            contentDescription = step.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFF1A1A1A)
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 13.5.sp,
                                color = Color(0xFF757575)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Bảng size tham khảo
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF9F9F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ruler),
                            contentDescription = "Bảng size tham khảo",
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bảng size tham khảo",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1A1A1A)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bảng 2 hàng: Size (EU) và Chiều dài chân (cm)
                    val tableScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(8.dp))
                            .horizontalScroll(tableScrollState)
                    ) {
                        // Hàng 1: Size (EU)
                        Row(
                            modifier = Modifier.height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableLabelCell(text = "Size (EU)")
                            euSizes.forEach { size ->
                                VerticalCellDivider()
                                TableValueCell(text = size, isHeader = true)
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFEFEFEF))

                        // Hàng 2: Chiều dài chân (cm)
                        Row(
                            modifier = Modifier.height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableLabelCell(text = "Chiều dài chân (cm)")
                            footLengthsCm.forEach { cm ->
                                VerticalCellDivider()
                                TableValueCell(text = cm, isHeader = false)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Dòng Lưu ý ở cuối
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_attention),
                        contentDescription = "Lưu ý",
                        tint = Color(0xFF424242),
                        modifier = Modifier.size(13.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Lưu ý: Nếu bạn đang phân vân giữa 2 size, hãy chọn size lớn hơn để có cảm giác thoải mái hơn.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF757575)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TableLabelCell(text: String) {
    Box(
        modifier = Modifier
            .width(118.dp)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = Color(0xFF212121)
            )
        )
    }
}

@Composable
private fun TableValueCell(
    text: String,
    isHeader: Boolean
) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp,
                color = Color(0xFF212121)
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VerticalCellDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(Color(0xFFEFEFEF))
    )
}

