package com.example.solesense.model.order

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Cart(
    @DocumentId val userId: String = "",
    val updatedAt: Timestamp = Timestamp.now()
)