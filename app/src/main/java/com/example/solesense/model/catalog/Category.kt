package com.example.solesense.model.catalog

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Category(
    @DocumentId val categoryId: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val description: String? = null,
    val status: String = "ACTIVE",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
