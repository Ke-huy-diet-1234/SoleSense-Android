package com.example.solesense.model.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User (
    @DocumentId val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String? = null,
    val avatarUrl: String? = null,
    val role: String = "CUSTOMER",
    val status: String = "ACTIVE",
    val isVerified: Boolean = false,
    val provider: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)