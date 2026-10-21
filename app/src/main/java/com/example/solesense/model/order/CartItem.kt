package com.example.solesense.model.order

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class CartItem(
    @DocumentId val itemId: String = "",
    val productId: String = "",
    val variantId: String = "",
    val productName: String = "",
    val imageUrl: String? = null,
    val size: String = "",
    val color: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 1,
    val addedAt: Timestamp = Timestamp.now()
)