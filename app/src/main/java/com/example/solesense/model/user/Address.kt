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
)