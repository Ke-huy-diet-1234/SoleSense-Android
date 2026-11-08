package com.example.solesense.repository

import com.example.solesense.model.user.Address
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AddressRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "mmZhfijntCf7BW2U0pf9VQbm4Xi1"
    }

    suspend fun getAddresses(userId: String = getCurrentUserId()): Result<List<Address>> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("addresses")
                .get()
                .await()
            val addresses = snapshot.toObjects(Address::class.java)
                .sortedByDescending { it.isDefault }
            Result.success(addresses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDefaultAddress(userId: String = getCurrentUserId()): Result<Address?> {
        return try {
            val addresses = getAddresses(userId).getOrDefault(emptyList())
            val defaultAddr = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
            Result.success(defaultAddr)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAddress(
        address: Address,
        userId: String = getCurrentUserId()
    ): Result<Address> {
        return try {
            val addressesRef = db.collection("users").document(userId).collection("addresses")
            val docRef = if (address.addressId.isNotBlank()) {
                addressesRef.document(address.addressId)
            } else {
                addressesRef.document()
            }

            val updatedAddress = address.copy(
                addressId = docRef.id,
                updatedAt = Timestamp.now()
            )
            docRef.set(updatedAddress).await()
            Result.success(updatedAddress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

