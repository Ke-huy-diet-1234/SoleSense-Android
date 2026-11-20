package com.example.solesense.model.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.PropertyName

data class User(
    var userId: String = "",
    var fullName: String = "",
    var nickname: String? = null,
    var email: String = "",
    var phone: String? = null,
    var dateOfBirth: String? = null,
    var gender: String? = null,
    var defaultAddress: String? = null,
    var avatarUrl: String? = null,
    var role: String = "CUSTOMER",
    var status: String = "ACTIVE",
    @get:PropertyName("isVerified")
    @set:PropertyName("isVerified")
    var isVerified: Boolean = false,
    @get:PropertyName("isProfileCompleted")
    @set:PropertyName("isProfileCompleted")
    var isProfileCompleted: Boolean = false,
    var provider: String? = null,
    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now()
)

/**
 * Đọc an toàn dữ liệu User từ DocumentSnapshot của Firestore,
 * tránh hoàn toàn lỗi crash/mất dữ liệu do xung đột @DocumentId hoặc lệch kiểu trường.
 */
fun DocumentSnapshot.toSafeUser(fallbackUid: String = ""): User {
    val data = this.data ?: emptyMap<String, Any?>()
    val resolvedId = (data["userId"] as? String)?.takeIf { it.isNotBlank() }
        ?: this.id.ifBlank { fallbackUid }
    val fullName = (data["fullName"] as? String)?.takeIf { it.isNotBlank() }
        ?: (data["name"] as? String)?.takeIf { it.isNotBlank() }
        ?: "Khách hàng"
    val nickname = (data["nickname"] as? String)?.takeIf { it.isNotBlank() }
    val email = (data["email"] as? String) ?: ""
    val phone = (data["phone"] as? String)?.takeIf { it.isNotBlank() }
    val dateOfBirth = (data["dateOfBirth"] as? String)?.takeIf { it.isNotBlank() }
    val gender = (data["gender"] as? String)?.takeIf { it.isNotBlank() }
    val defaultAddress = (data["defaultAddress"] as? String)?.takeIf { it.isNotBlank() }
    val avatarUrl = (data["avatarUrl"] as? String)?.takeIf { it.isNotBlank() }
        ?: (data["photoUrl"] as? String)?.takeIf { it.isNotBlank() }
        ?: (data["avatar"] as? String)?.takeIf { it.isNotBlank() }
        ?: (data["photoURL"] as? String)?.takeIf { it.isNotBlank() }
    val role = (data["role"] as? String)?.takeIf { it.isNotBlank() } ?: "CUSTOMER"
    val status = (data["status"] as? String)?.takeIf { it.isNotBlank() } ?: "ACTIVE"
    val isVerified = (data["isVerified"] as? Boolean)
        ?: (data["verified"] as? Boolean)
        ?: true
    val isProfileCompleted = (data["isProfileCompleted"] as? Boolean)
        ?: (data["profileCompleted"] as? Boolean)
        ?: false
    val provider = data["provider"] as? String
    val createdAt = (data["createdAt"] as? Timestamp) ?: Timestamp.now()
    val updatedAt = (data["updatedAt"] as? Timestamp) ?: Timestamp.now()

    return User(
        userId = resolvedId,
        fullName = fullName,
        nickname = nickname,
        email = email,
        phone = phone,
        dateOfBirth = dateOfBirth,
        gender = gender,
        defaultAddress = defaultAddress,
        avatarUrl = avatarUrl,
        role = role,
        status = status,
        isVerified = isVerified,
        isProfileCompleted = isProfileCompleted,
        provider = provider,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
