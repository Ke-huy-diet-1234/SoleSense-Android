package com.example.solesense.ui.home.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solesense.utils.AssetConfig
import com.example.solesense.utils.PromoBannerItem
import kotlinx.coroutines.delay

@Composable
fun PromotionCarousel(
    banners: List<PromoBannerItem> = AssetConfig.DEFAULT_BANNERS,
    onBannerClick: (PromoBannerItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeBanners = if (banners.isNotEmpty()) banners else AssetConfig.DEFAULT_BANNERS
    val pagerState = rememberPagerState(pageCount = { activeBanners.size })

    // Tự động lướt tuần tự qua từng banner từ trái sang phải và lặp lại liên tục
    LaunchedEffect(activeBanners.size) {
        if (activeBanners.size > 1) {
            while (true) {
                delay(3200L)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % activeBanners.size
                    runCatching {
                        pagerState.animateScrollToPage(
                            page = nextPage,
                            animationSpec = tween(durationMillis = 550)
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val banner = activeBanners[page]
            val isFullImageBanner = banner.title.isBlank() && !banner.imageUrl.isNullOrBlank()

            if (isFullImageBanner) {
                // Sử dụng ContentScale.Fit với khung tỷ lệ ngang chuẩn để hiển thị trọn vẹn 100% nội dung banner, không bị cắt chữ ở hai bên
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onBannerClick(banner) },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = banner.imageUrl,
                        contentDescription = "Banner khuyến mãi",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(banner.bgStartHex),
                                    Color(banner.bgEndHex)
                                )
                            )
                        )
                        .clickable { onBannerClick(banner) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (banner.tag.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.22f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = banner.tag,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                            Text(
                                text = banner.title,
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = banner.subtitle,
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                maxLines = 2
                            )
                        }

                        if (!banner.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = banner.imageUrl,
                                contentDescription = banner.title,
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .padding(6.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dải dấu chấm (Indicator)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(activeBanners.size) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val color = if (isSelected) Color(0xFF111111) else Color(0xFFE0E0E0)
                val width = if (isSelected) 18.dp else 6.dp

                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = width, height = 6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
