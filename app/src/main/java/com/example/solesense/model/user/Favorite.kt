package com.example.solesense.model.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Favorite(
    @DocumentId val productId: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
