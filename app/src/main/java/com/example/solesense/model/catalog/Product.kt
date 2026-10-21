package com.example.solesense.model.catalog

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Product(
    @DocumentId val productId: String = "",
    val name: String = "",
    val brandId: String = "",
    val categoryId: String = "",
    val description: String = "",
    val basePrice: Double = 0.0,
    val salePrice: Double? = null,
    val gender: String = "",
    val purpose: String = "",
    val material: String? = null,
    val weight: Double? = null,
    val cushioning: String? = null,
    val terrain: String? = null,
    val imageUrls: List<String> = emptyList(),
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val status: String = "ACTIVE",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)