package com.example.solesense.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Bộ tiện ích Upload ảnh trực tiếp lên Cloudinary (Unsigned Upload) rồi trả về secure_url để lưu vào Firebase Firestore.
 * - Cloud name: hbxjrmsz
 * - Upload preset: solesense_unsigned
 */
object CloudinaryHelper {
    const val CLOUD_NAME = "hbxjrmsz"
    const val UPLOAD_PRESET = "solesense_unsigned"
    const val MAX_REVIEW_IMAGES = 4

    suspend fun uploadImageUri(
        context: Context,
        imageUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IllegalArgumentException("Không thể đọc dữ liệu ảnh từ thiết bị"))

            val boundary = "----SoleSenseBoundary${UUID.randomUUID().toString().replace("-", "")}"
            val lineEnd = "\r\n"
            val twoHyphens = "--"

            val url = URL("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                doInput = true
                doOutput = true
                useCaches = false
                connectTimeout = 20000
                readTimeout = 20000
                requestMethod = "POST"
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            DataOutputStream(connection.outputStream).use { outputStream ->
                // 1. upload_preset field
                outputStream.writeBytes(twoHyphens + boundary + lineEnd)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"$lineEnd$lineEnd")
                outputStream.writeBytes(UPLOAD_PRESET + lineEnd)

                // 2. file field
                outputStream.writeBytes(twoHyphens + boundary + lineEnd)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"review_${System.currentTimeMillis()}.jpg\"$lineEnd")
                outputStream.writeBytes("Content-Type: image/jpeg$lineEnd$lineEnd")
                outputStream.write(bytes)
                outputStream.writeBytes(lineEnd)

                // End multipart
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
                outputStream.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseBody)
                val secureUrl = json.optString("secure_url").ifBlank { json.optString("url") }
                connection.disconnect()
                if (secureUrl.isNotBlank()) {
                    Result.success(secureUrl)
                } else {
                    Result.failure(IllegalStateException("Phản hồi từ Cloudinary không chứa link ảnh"))
                }
            } else {
                val errBody = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                } catch (_: Exception) { "HTTP $responseCode" }
                connection.disconnect()
                Result.failure(IllegalStateException("Tải ảnh lên Cloudinary thất bại: $errBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload danh sách tối đa 4 ảnh review lên Cloudinary
     */
    suspend fun uploadMultipleReviewImages(
        context: Context,
        uris: List<Uri>
    ): Result<List<String>> {
        val limitedUris = uris.take(MAX_REVIEW_IMAGES)
        val uploadedUrls = mutableListOf<String>()
        for (uri in limitedUris) {
            val res = uploadImageUri(context, uri)
            res.fold(
                onSuccess = { url -> uploadedUrls.add(url) },
                onFailure = { err -> return Result.failure(err) }
            )
        }
        return Result.success(uploadedUrls)
    }
}
