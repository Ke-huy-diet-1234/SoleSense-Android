package com.example.solesense.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class PromoBannerItem(
    val id: String,
    val imageUrl: String?,
    val title: String,
    val subtitle: String,
    val tag: String,
    val bgStartHex: Long = 0xFF6C5CE7,
    val bgEndHex: Long = 0xFFA29BFE
)

object AssetConfig {
    /**
     * Danh sách Avatar có sẵn trên kho tài nguyên GitHub solesense-assets
     */
    val PRESET_AVATARS = listOf(
        "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/ava/ava_1.jpg",
        "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/ava/ava_2.png",
        "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/ava/ava_3.jpg",
        "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/ava/ava_4.jpg",
        "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/ava/ava_6.jpg",
        "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/ava/ava_7.jpg",
        "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/ava/ava_8.png"
    )

    /**
     * 3 Banner thiết kế riêng của ứng dụng (thay thế cho vị trí 3, 4, 5 cũ)
     */
    val DESIGNED_BANNERS = listOf(
        PromoBannerItem(
            id = "designed_1",
            imageUrl = "https://static.nike.com/a/images/t_PDP_1728_v1/f_auto,q_auto:eco/887859e5-9b50-4c19-a9bb-733f55f88cb2/AIR+ZOOM+PEGASUS+41.png",
            title = "Giảm đến 50%",
            subtitle = "Ưu đãi đặc biệt dòng giày chạy bộ Nike & Adidas",
            tag = "ƯU ĐÃI HOT",
            bgStartHex = 0xFF6C5CE7,
            bgEndHex = 0xFF8E7CFF
        ),
        PromoBannerItem(
            id = "designed_2",
            imageUrl = "https://assets.adidas.com/images/w_600,f_auto,q_auto/ff67e3405f7f46eca232d3aa4e0b78e9_9366/Giay_Ultraboost_5_trang_ID8810_HM1.jpg",
            title = "Voucher 200K",
            subtitle = "Nhập mã SOLE200 giảm ngay 200.000đ cho đơn từ 1 triệu",
            tag = "SOLE200",
            bgStartHex = 0xFF111827,
            bgEndHex = 0xFF374151
        ),
        PromoBannerItem(
            id = "designed_3",
            imageUrl = "https://images.asics.com/is/image/asics/1011B794_400_SR_RT_GLB",
            title = "Freeship Toàn Quốc",
            subtitle = "Miễn phí vận chuyển cho mọi đơn hàng từ 200.000đ",
            tag = "FREESHIP",
            bgStartHex = 0xFF059669,
            bgEndHex = 0xFF10B981
        )
    )

    /**
     * Danh sách Banner mặc định đã được xáo trộn xen kẽ giữa ảnh banner GitHub (1, 2, 6, 7, 8)
     * và 3 banner thiết kế riêng (thay thế cho ảnh 3, 4, 5)
     */
    val DEFAULT_BANNERS = listOf(
        PromoBannerItem(
            id = "gh_intro_1",
            imageUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/banner/intro/img_1.png",
            title = "",
            subtitle = "",
            tag = ""
        ),
        DESIGNED_BANNERS[0],
        PromoBannerItem(
            id = "gh_intro_2",
            imageUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/banner/intro/img_2.png",
            title = "",
            subtitle = "",
            tag = ""
        ),
        DESIGNED_BANNERS[1],
        PromoBannerItem(
            id = "gh_intro_6",
            imageUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/banner/intro/img_6.png",
            title = "",
            subtitle = "",
            tag = ""
        ),
        DESIGNED_BANNERS[2],
        PromoBannerItem(
            id = "gh_intro_7",
            imageUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/banner/intro/img_7.png",
            title = "",
            subtitle = "",
            tag = ""
        ),
        PromoBannerItem(
            id = "gh_intro_8",
            imageUrl = "https://raw.githubusercontent.com/Ke-huy-diet-1234/solesense-assets/main/banner/intro/img_8.png",
            title = "",
            subtitle = "",
            tag = ""
        )
    )

    private var cachedGitHubBannerItems: List<PromoBannerItem>? = null

    /**
     * Tải banner từ GitHub (loại bỏ img_3, img_4, img_5), sau đó xáo trộn xen kẽ
     * với 3 banner thiết kế sẵn để không bị lặp cùng mẫu cạnh nhau.
     */
    suspend fun fetchGitHubIntroBanners(): List<PromoBannerItem> = withContext(Dispatchers.IO) {
        try {
            val cached = cachedGitHubBannerItems
            if (!cached.isNullOrEmpty()) {
                return@withContext cached
            }

            val discovered = mutableListOf<Pair<String, String>>()
            val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp")
            val excludedNames = setOf("img_3.png", "img_4.png", "img_5.png")

            try {
                val apiUrl = URL("https://api.github.com/repos/Ke-huy-diet-1234/solesense-assets/contents/banner/intro")
                val conn = (apiUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }
                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(responseText)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.optString("name", "").lowercase()
                        val downloadUrl = obj.optString("download_url", "")
                        if (downloadUrl.isNotBlank() &&
                            name !in excludedNames &&
                            imageExtensions.any { name.endsWith(it) }
                        ) {
                            discovered.add(name to downloadUrl)
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {}

            val sortedUrls = discovered
                .distinctBy { it.second }
                .sortedBy { (name, _) ->
                    Regex("(\\d+)").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 999
                }
                .map { it.second }

            val ghBanners = sortedUrls.take(5).mapIndexed { index, url ->
                PromoBannerItem(
                    id = "gh_banner_$index",
                    imageUrl = url,
                    title = "",
                    subtitle = "",
                    tag = ""
                )
            }

            if (ghBanners.isNotEmpty()) {
                // Xáo trộn xen kẽ: Ảnh GitHub <-> Banner thiết kế của app
                val mixed = buildList {
                    val maxLen = maxOf(ghBanners.size, DESIGNED_BANNERS.size)
                    for (i in 0 until maxLen) {
                        if (i < ghBanners.size) add(ghBanners[i])
                        if (i < DESIGNED_BANNERS.size) add(DESIGNED_BANNERS[i])
                    }
                }
                cachedGitHubBannerItems = mixed
                mixed
            } else {
                DEFAULT_BANNERS
            }
        } catch (_: Exception) {
            DEFAULT_BANNERS
        }
    }
}
