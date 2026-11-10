package com.example.solesense.repository

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.example.solesense.model.user.User
import com.facebook.AccessToken
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Kiểm tra phiên đăng nhập hiện tại và lấy thông tin User + Role từ Firestore
     */
    suspend fun getCurrentSessionUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            val snapshot = userDocRef.get().await()
            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                user?.copy(userId = firebaseUser.uid)
            } else {
                val fallbackRole = when {
                    firebaseUser.email?.contains("admin", ignoreCase = true) == true -> "ADMIN"
                    firebaseUser.email?.contains("support", ignoreCase = true) == true -> "SUPPORT"
                    else -> "CUSTOMER"
                }
                val newUser = User(
                    userId = firebaseUser.uid,
                    fullName = firebaseUser.displayName ?: "Khách hàng",
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                    role = fallbackRole,
                    status = "ACTIVE",
                    isVerified = true
                )
                userDocRef.set(newUser).await()
                newUser
            }
        } catch (e: Exception) {
            User(
                userId = firebaseUser.uid,
                fullName = firebaseUser.displayName ?: "Người dùng SoleSense",
                email = firebaseUser.email ?: "",
                role = "CUSTOMER"
            )
        }
    }

    suspend fun authWithGoogle(credential: Credential): Result<User> {
        return try {
            val googleIdTokenCredential = when (credential) {
                is GoogleIdTokenCredential -> credential
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
                    ) {
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } else {
                        return Result.failure(Exception("CustomCredential type không được hỗ trợ: ${credential.type}"))
                    }
                }
                else -> return Result.failure(Exception("Credential không hợp lệ: ${credential::class.java.name}"))
            }

            // 1. Chuyển Token của Google thành Token của Firebase
            val firebaseAuthCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

            // 2. Đăng nhập vào Firebase Auth
            val authResult = auth.signInWithCredential(firebaseAuthCredential).await()
            val firebaseUser = authResult.user ?: throw Exception("Lỗi lấy thông tin User")

            // 3. Đồng bộ vào Firestore (Luồng Check-then-Write)
            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            val snapshot = userDocRef.get().await()

            val finalUser = if (snapshot.exists()) {
                snapshot.toObject(User::class.java)!!.copy(userId = firebaseUser.uid)
            } else {
                val newUser = User(
                    userId = firebaseUser.uid,
                    fullName = firebaseUser.displayName ?: "Khách hàng",
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                    role = "CUSTOMER",
                    status = "ACTIVE",
                    isVerified = true,
                    provider = "google"
                )
                userDocRef.set(newUser).await()
                newUser
            }
            Result.success(finalUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun authWithFacebook(accessToken: AccessToken): Result<User> {
        return try {
            // 1. Đổi AccessToken của Facebook thành Firebase Credential
            val credential = FacebookAuthProvider.getCredential(accessToken.token)

            // 2. Đăng nhập vào Firebase Auth
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Lỗi lấy thông tin User từ Firebase")

            // 3. Đồng bộ vào Firestore (Luồng Check-then-Write)
            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            val snapshot = userDocRef.get().await()

            val finalUser = if (snapshot.exists()) {
                snapshot.toObject(User::class.java)!!.copy(userId = firebaseUser.uid)
            } else {
                val newUser = User(
                    userId = firebaseUser.uid,
                    fullName = firebaseUser.displayName ?: "Người dùng Facebook",
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                    role = "CUSTOMER",
                    status = "ACTIVE",
                    isVerified = true,
                    provider = "facebook"
                )
                userDocRef.set(newUser).await()
                newUser
            }
            Result.success(finalUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailPassword(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user ?: throw Exception("Không tìm thấy thông tin người dùng")

            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            val snapshot = userDocRef.get().await()

            val finalUser = if (snapshot.exists()) {
                snapshot.toObject(User::class.java)!!.copy(userId = firebaseUser.uid)
            } else {
                // Kiểm tra theo email trên Firestore nếu uid khác hoặc tự xác định role
                val emailQuery = firestore.collection("users")
                    .whereEqualTo("email", email.trim())
                    .get()
                    .await()
                val matchedDoc = emailQuery.documents.firstOrNull()
                if (matchedDoc != null) {
                    matchedDoc.toObject(User::class.java)!!.copy(userId = firebaseUser.uid)
                } else {
                    val inferredRole = when {
                        email.contains("admin", ignoreCase = true) -> "ADMIN"
                        email.contains("support", ignoreCase = true) -> "SUPPORT"
                        else -> "CUSTOMER"
                    }
                    val newUser = User(
                        userId = firebaseUser.uid,
                        fullName = firebaseUser.displayName ?: email.substringBefore("@"),
                        email = email.trim(),
                        role = inferredRole,
                        status = "ACTIVE",
                        isVerified = true,
                        provider = "password"
                    )
                    userDocRef.set(newUser).await()
                    newUser
                }
            }
            Result.success(finalUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmailPassword(
        fullName: String,
        email: String,
        phone: String,
        password: String
    ): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user ?: throw Exception("Không thể tạo tài khoản")

            val now = Timestamp.now()
            val newUser = User(
                userId = firebaseUser.uid,
                fullName = fullName.trim(),
                email = email.trim(),
                phone = phone.trim(),
                avatarUrl = "",
                role = "CUSTOMER",
                status = "ACTIVE",
                isVerified = true,
                provider = "password",
                createdAt = now,
                updatedAt = now
            )
            firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
