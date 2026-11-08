package com.example.solesense.ui.product.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solesense.R

@Composable
fun ProductInfoSection(
    description: String,
    // Danh sách các tính năng nổi bật (nếu DB của bạn có lưu tách rời).
    // Nếu không, bạn có thể truyền list rỗng hoặc xóa tham số này.
    features: List<Pair<String, String>> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Trạng thái lưu trữ việc mở/đóng Accordion (mặc định đóng để tiết kiệm diện tích)
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 300)) // Hiệu ứng xổ xuống mượt mà
    ) {
        // Tiêu đề có thể click để mở rộng/thu gọn
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_description),
                contentDescription = "Icon văn bản",
                tint = Color.DarkGray,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Thông tin sản phẩm",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                tint = Color.Gray
            )
        }

        // Nội dung chi tiết (Chỉ hiển thị khi isExpanded == true)
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
            ) {
                // Đoạn văn bản mô tả chung
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.DarkGray,
                        lineHeight = 22.sp
                    )
                )

                // Danh sách tính năng (Bullet points) giống thiết kế ảnh số 5
                if (features.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    features.forEach { feature ->
                        FeatureItem(title = feature.first, detail = feature.second)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// Component phụ để vẽ từng dòng tính năng có icon dấu tick
@Composable
private fun FeatureItem(
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Check",
            tint = Color.DarkGray,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            )
        }
    }
}


@Preview(
    showBackground = true,

    name = "Welcome Screen"
)
@Composable
fun WelcomeScreenPreview3() {
    ProductInfoSection("abc", emptyList())
}