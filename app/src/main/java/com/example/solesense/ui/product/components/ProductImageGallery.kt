package com.example.solesense.ui.product.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ProductImageGallery(
    imageUrls: List<String>,
    discountPercent: Int?,
    selectedImageIndex: Int = 0,
    imageScrollTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    // Xử lý trường hợp mảng ảnh rỗng để tránh crash Pager
    if (imageUrls.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Tỷ lệ vuông 1:1 chuẩn cho ảnh sản phẩm
                .background(Color(0xFFF5F5F5))
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    // Tự động focus/cuộn tới ảnh tương ứng khi người dùng chọn màu sắc
    LaunchedEffect(selectedImageIndex, imageScrollTrigger) {
        if (selectedImageIndex in imageUrls.indices && pagerState.currentPage != selectedImageIndex) {
            pagerState.animateScrollToPage(selectedImageIndex)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Vùng chứa ảnh và các Badge overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFF5F5F5))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = "Hình ảnh sản phẩm ${page + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit // Giữ nguyên tỷ lệ, không cắt cúp mất góc giày
                )
            }

            // Badge hiển thị thứ tự ảnh (Ví dụ: 1/15) - Góc dưới bên trái
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${imageUrls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Nhãn phần trăm giảm giá - Góc trên bên phải
            if (discountPercent != null && discountPercent > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "-$discountPercent%",
                        color = Color(0xFFE53935), // Màu đỏ nổi bật
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Thanh chỉ báo (Dots Indicator) - Thiết kế co giãn động
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(imageUrls.size) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val color = if (isSelected) Color.Black else Color.LightGray
                val width = if (isSelected) 16.dp else 6.dp // Chấm đang chọn sẽ dài ra thành hình viên thuốc

                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clip(CircleShape)
                        .background(color)
                        .height(6.dp)
                        .width(width)
                )
            }
        }
    }
}


@Preview(
    showBackground = true,

    name = "Welcome Screen"
)
@Composable
fun WelcomeScreenPreview1() {
    ProductImageGallery(emptyList(), 20  )
}