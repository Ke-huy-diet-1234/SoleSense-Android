package com.example.solesense

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.example.solesense.utils.DatabaseSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoleSenseApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Tự động kiểm tra phiên bản và đồng bộ danh mục, voucher phong phú lên Firestore
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DatabaseSeeder().ensureRichCatalogSeeded()
            } catch (e: Exception) {
                Log.e("SoleSenseApplication", "Seeder error: ${e.message}")
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory()) // Đăng ký bộ giải mã SVG
            }
            .build()
    }
}
