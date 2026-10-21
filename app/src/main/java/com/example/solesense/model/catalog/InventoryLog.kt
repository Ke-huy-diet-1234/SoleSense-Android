package com.example.solesense.model.catalog

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class InventoryLog(
    @DocumentId val logId: String = "",
    val variantId: String = "",
    val productId: String = "",
    val type: String = "IMPORT", // IMPORT, SALE, RETURN, ADJUSTMENT
    val quantity: Int = 0,
    val previousStock: Int = 0,
    val newStock: Int = 0,
    val reason: String? = null,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now()
)