package com.example.solesense.model.order

import com.google.firebase.firestore.DocumentId

data class OrderItem(
    @DocumentId val itemId: String = "",
    val productId: String = "",
    val variantId: String = "",
    val skuSnapshot: String = "",
    val productName: String = "",
    val imageSnapshot: String? = null,
    val size: String = "",
    val color: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 1,
    val subtotal: Double = 0.0
)
