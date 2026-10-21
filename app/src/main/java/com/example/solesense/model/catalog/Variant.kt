package com.example.solesense.model.catalog

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Variant(
    @DocumentId val variantId: String = "",
    val sku: String = "",
    val color: String = "",
    val size: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val status: String = "ACTIVE",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)