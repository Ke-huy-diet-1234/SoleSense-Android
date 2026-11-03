package com.example.solesense.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PromotionCarousel(modifier: Modifier = Modifier) {
    // Giả lập danh sách các Banner khuyến mãi (Sau này ViewModel sẽ truyền vào list URL ảnh)
    val banners = listOf(
        "50% Discount\nNike Air",
        "Sale Cuối Tuần\nGiảm 30%",
        "Freeship\nĐơn từ 500k"
    )

    // Quản lý trạng thái trang hiện tại của Băng chuyền
    val pagerState = rememberPagerState(pageCount = { banners.size })

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Khung vuốt Banner
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp) // Tạo khoảng cách nhẹ giữa các banner khi vuốt
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF6C5CE7)), // Màu tím chủ đạo từ bản thiết kế
                contentAlignment = Alignment.CenterStart
            ) {
                // TODO: Dùng AsyncImage (Coil) để hiển thị ảnh thực tế
                Text(
                    text = banners[page],
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dải dấu chấm (Indicator)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(banners.size) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val color = if (isSelected) Color(0xFF6C5CE7) else Color(0xFFE0E0E0)
                val width = if (isSelected) 16.dp else 6.dp // Chấm đang chọn sẽ dài ra thành hình viên thuốc

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = width, height = 6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}