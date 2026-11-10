package com.example.solesense.model.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Address(
    @DocumentId val addressId: String = "",
    val receiverName: String = "",
    val phone: String = "",
    val province: String = "",
    val district: String = "",
    val ward: String = "",
    val detail: String = "",
    val isDefault: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    /**
     * Ghép Tỉnh/Thành phố và Phường/Xã thành chuỗi hiển thị ở ô input hoặc dòng 2
     */
    fun provinceAndWardText(): String {
        return listOf(ward.trim(), district.trim(), province.trim())
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
    }

    /**
     * Định dạng địa chỉ thành nhiều hàng nếu địa chỉ dài hoặc có cả Số nhà/Đường và Phường xã/Tỉnh thành.
     * Ví dụ:
     * Số nhà 83, Thôn Lại Đà
     * Xã Đông Anh, Thành phố Hà Nội
     */
    fun toMultiLineAddress(): String {
        val line1 = detail.trim()
        val line2 = provinceAndWardText()

        if (line1.isNotBlank() && line2.isNotBlank()) {
            // Nếu line1 đã vô tình chứa line2 thì không lặp lại
            return if (line1.endsWith(line2, ignoreCase = true)) {
                val stripped = line1.removeSuffix(line2).trim().trimEnd(',').trim()
                if (stripped.isNotBlank()) "$stripped\n$line2" else line1
            } else {
                "$line1\n$line2"
            }
        }

        val combined = if (line1.isNotBlank()) line1 else line2
        // Nếu chỉ có 1 chuỗi dài (ví dụ > 32 ký tự và có dấu phẩy), tự động tách thành 2 hàng tại dấu phẩy hợp lý
        if (combined.length > 32 && combined.contains(",")) {
            val parts = combined.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 3) {
                val splitIndex = parts.size / 2
                val firstLine = parts.subList(0, splitIndex).joinToString(", ")
                val secondLine = parts.subList(splitIndex, parts.size).joinToString(", ")
                return "$firstLine\n$secondLine"
            } else if (parts.size == 2) {
                return "${parts[0]}\n${parts[1]}"
            }
        }
        return combined
    }

    fun toFullSingleLineAddress(): String {
        return toMultiLineAddress().replace("\n", ", ")
    }
}
