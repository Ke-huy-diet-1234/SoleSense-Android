package com.example.solesense.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

data class OnboardingData(
    val imageRes: Int,
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingData(
            imageRes = R.drawable.img_onboarding_1,
            title = "Phong cách đỉnh cao,\nthoải mái trong từng bước chân",
            subtitle = "Bộ sưu tập giày chính hãng tuyển chọn từ Nike, Adidas, Puma, New Balance, Asics..."
        ),
        OnboardingData(
            imageRes = R.drawable.img_onboarding_2,
            title = "Trợ lý AI tư vấn\nchọn giày chuẩn gu & chuẩn size",
            subtitle = "Gợi ý mẫu giày phù hợp nhu cầu vận động, phối đồ và ngân sách của riêng bạn."
        ),
        OnboardingData(
            imageRes = R.drawable.img_onboarding_3,
            title = "Khẳng định cá tính,\ntự tin sải bước cùng SoleSense!",
            subtitle = "Đặt hàng nhanh chóng, nhiều mã giảm giá hấp dẫn và theo dõi đơn hàng trực tuyến."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { position ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    ) {
                        Image(
                            painter = painterResource(id = pages[position].imageRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = pages[position].title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 32.sp,
                            color = Color(0xFF111111)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = pages[position].subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF757575)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Cụm điều khiển: Indicator + Nút Next
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(pages.size) { index ->
                        val isSelected = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(7.dp)
                                .width(if (isSelected) 28.dp else 7.dp)
                                .clip(if (isSelected) RoundedCornerShape(4.dp) else CircleShape)
                                .background(if (isSelected) Color(0xFF111111) else Color(0xFFD6D6D6))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111)),
                    shape = RoundedCornerShape(27.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) "Tiếp tục" else "Bắt đầu ngay",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }

        // Nút Bỏ qua ở góc phải trên
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onComplete() },
            color = Color.Black.copy(alpha = 0.45f)
        ) {
            Text(
                text = "Bỏ qua",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}
