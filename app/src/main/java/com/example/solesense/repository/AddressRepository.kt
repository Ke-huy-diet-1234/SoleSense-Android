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
                .sortedWith(
                    compareByDescending<Address> { it.isDefault }
                        .thenByDescending { it.updatedAt.seconds }
                )
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

    /**
     * Tạo mới hoặc cập nhật địa chỉ trên Firebase.
     * Nếu địa chỉ này được đặt làm mặc định (isDefault = true), tự động cập nhật
     * địa chỉ mặc định trước đó trên Firebase thành false và đổi địa chỉ mới này thành true.
     */
    suspend fun saveAddress(
        address: Address,
        userId: String = getCurrentUserId()
    ): Result<Address> {
        return try {
            val addressesRef = db.collection("users").document(userId).collection("addresses")
            val existingSnapshot = addressesRef.get().await()
            val existingAddresses = existingSnapshot.toObjects(Address::class.java)

            val docRef = if (address.addressId.isNotBlank()) {
                addressesRef.document(address.addressId)
            } else {
                addressesRef.document()
            }

            // Nếu đây là địa chỉ đầu tiên của người dùng thì mặc định gán isDefault = true
            val shouldBeDefault = address.isDefault || existingAddresses.none { it.addressId != docRef.id }

            val now = Timestamp.now()
            val updatedAddress = address.copy(
                addressId = docRef.id,
                isDefault = shouldBeDefault,
                createdAt = if (address.addressId.isBlank()) now else address.createdAt,
                updatedAt = now
            )

            val batch = db.batch()
            if (shouldBeDefault) {
                for (doc in existingSnapshot.documents) {
                    if (doc.id != docRef.id && doc.getBoolean("isDefault") == true) {
                        batch.update(doc.reference, mapOf("isDefault" to false, "updatedAt" to now))
                    }
                }
            }
            batch.set(docRef, updatedAddress)
            batch.commit().await()

            Result.success(updatedAddress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(
        addressId: String,
        userId: String = getCurrentUserId()
    ): Result<Unit> {
        return try {
            if (addressId.isBlank()) {
                return Result.failure(IllegalArgumentException("Không tìm thấy mã địa chỉ để xóa"))
            }
            val addressesRef = db.collection("users").document(userId).collection("addresses")
            val docRef = addressesRef.document(addressId)
            val snapshot = docRef.get().await()
            val wasDefault = snapshot.getBoolean("isDefault") == true

            docRef.delete().await()

            // Nếu xóa địa chỉ đang là mặc định thì tự động chọn địa chỉ đầu tiên còn lại làm mặc định
            if (wasDefault) {
                val remaining = addressesRef.get().await()
                val firstRemaining = remaining.documents.firstOrNull()
                if (firstRemaining != null) {
                    firstRemaining.reference.update(
                        mapOf(
                            "isDefault" to true,
                            "updatedAt" to Timestamp.now()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
