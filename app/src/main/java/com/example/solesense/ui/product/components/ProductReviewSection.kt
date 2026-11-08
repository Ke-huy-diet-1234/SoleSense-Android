package com.example.solesense.ui.product.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.collections.take

// Data class tạm thời để vẽ UI (Sau này sẽ thay bằng Model từ Firebase)
data class ReviewUiModel(
    val id: String,
    val userName: String,
    val userAvatarUrl: String?,
    val rating: Int,
    val date: String,
    val content: String,
    val variantInfo: String
)

@Composable
fun ProductReviewSection(
    averageRating: Double,
    totalReviews: Int,
    reviews: List<ReviewUiModel>,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 300))
    ) {
        // 1. Header: Đánh giá sản phẩm & Nút Xem tất cả
        Row(
            modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Đánh giá sản phẩm",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "$averageRating",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black, fontWeight = FontWeight.Bold)
                )
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Star",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(16.dp).padding(horizontal = 2.dp)
                )
                Text(
                    text = "($totalReviews đánh giá)",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Xem tất cả",
                    tint = Color.Gray,
                    //modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Danh sách Preview các đánh giá (Hiển thị tối đa 2 cái)
        reviews.take(2).forEach { review ->
            ReviewItem(review = review)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Preview(
    showBackground = true,

    name = "Welcome Screen"
)
@Composable
fun WelcomeScreenPreview2() {
   ProductReviewSection(3.4,1200,emptyList(),{})
}
