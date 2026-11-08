package com.example.solesense.repository

import com.example.solesense.model.order.Voucher
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class VoucherRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getActiveVouchers(): Result<List<Voucher>> {
        return try {
            val snapshot = db.collection("vouchers")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()
            val vouchers = snapshot.toObjects(Voucher::class.java)
                .filter { it.usageLimit == 0 || it.usedCount < it.usageLimit }
            Result.success(vouchers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

