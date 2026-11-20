package com.example.solesense.repository

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.example.solesense.model.user.User
import com.example.solesense.model.user.toSafeUser
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
     * Kiểm tra phiên đăng nhập hiện tại và lấy thông tin User + Role + avatarUrl từ Firestore
     */
    suspend fun getCurrentSessionUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            val snapshot = userDocRef.get().await()
            if (snapshot.exists()) {
                val parsed = snapshot.toSafeUser(firebaseUser.uid)
                parsed.copy(
                    userId = firebaseUser.uid,
                    fullName = parsed.fullName.ifBlank { firebaseUser.displayName ?: "Khách hàng" },
                    email = parsed.email.ifBlank { firebaseUser.email ?: "" },
                    avatarUrl = parsed.avatarUrl?.takeIf { it.isNotBlank() } ?: firebaseUser.photoUrl?.toString().orEmpty()
                )
            } else {
                val emailQuery = if (!firebaseUser.email.isNullOrBlank()) {
                    firestore.collection("users")
                        .whereEqualTo("email", firebaseUser.email)
                        .get()
                        .await()
                } else null
                val matchedDoc = emailQuery?.documents?.firstOrNull()

                if (matchedDoc != null) {
                    val parsed = matchedDoc.toSafeUser(firebaseUser.uid)
                    val merged = parsed.copy(
                        userId = firebaseUser.uid,
                        fullName = parsed.fullName.ifBlank { firebaseUser.displayName ?: "Khách hàng" },
                        email = parsed.email.ifBlank { firebaseUser.email ?: "" },
                        avatarUrl = parsed.avatarUrl?.takeIf { it.isNotBlank() } ?: firebaseUser.photoUrl?.toString().orEmpty()
                    )
                    userDocRef.set(merged, com.google.firebase.firestore.SetOptions.merge()).await()
                    merged
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
            }
        } catch (e: Exception) {
            User(
                userId = firebaseUser.uid,
                fullName = firebaseUser.displayName ?: "Người dùng SoleSense",
                email = firebaseUser.email ?: "",
                avatarUrl = firebaseUser.photoUrl?.toString().orEmpty(),
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

            // 3. Đồng bộ vào Firestore (Giữ nguyên avatarUrl đã chọn trên Firestore nếu tài khoản đã tồn tại)
            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            val snapshot = userDocRef.get().await()

            val finalUser = if (snapshot.exists()) {
                val existing = snapshot.toSafeUser(firebaseUser.uid)
                existing.copy(
                    userId = firebaseUser.uid,
                    fullName = existing.fullName.ifBlank { firebaseUser.displayName ?: "Khách hàng" },
                    email = existing.email.ifBlank { firebaseUser.email ?: "" },
                    avatarUrl = existing.avatarUrl?.takeIf { it.isNotBlank() } ?: firebaseUser.photoUrl?.toString().orEmpty()
                )
            } else {
                val emailQuery = if (!firebaseUser.email.isNullOrBlank()) {
                    firestore.collection("users")
                        .whereEqualTo("email", firebaseUser.email)
                        .get()
                        .await()
                } else null
                val matchedDoc = emailQuery?.documents?.firstOrNull()

                if (matchedDoc != null) {
                    val existing = matchedDoc.toSafeUser(firebaseUser.uid)
                    val merged = existing.copy(
                        userId = firebaseUser.uid,
                        fullName = existing.fullName.ifBlank { firebaseUser.displayName ?: "Khách hàng" },
                        email = existing.email.ifBlank { firebaseUser.email ?: "" },
                        avatarUrl = existing.avatarUrl?.takeIf { it.isNotBlank() } ?: firebaseUser.photoUrl?.toString().orEmpty()
                    )
                    userDocRef.set(merged, com.google.firebase.firestore.SetOptions.merge()).await()
                    merged
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
            }
            if (finalUser.status.uppercase() == "BLOCKED") {
                auth.signOut()
                return Result.failure(Exception("Tài khoản của bạn đã bị khóa bởi Quản trị viên!"))
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

            // 3. Đồng bộ vào Firestore
            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            val snapshot = userDocRef.get().await()

            val finalUser = if (snapshot.exists()) {
                snapshot.toSafeUser(firebaseUser.uid)
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
            if (finalUser.status.uppercase() == "BLOCKED") {
                auth.signOut()
                return Result.failure(Exception("Tài khoản của bạn đã bị khóa bởi Quản trị viên!"))
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
                snapshot.toSafeUser(firebaseUser.uid)
            } else {
                val emailQuery = firestore.collection("users")
                    .whereEqualTo("email", email.trim())
                    .get()
                    .await()
                val matchedDoc = emailQuery.documents.firstOrNull()
                if (matchedDoc != null) {
                    matchedDoc.toSafeUser(firebaseUser.uid)
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
            if (finalUser.status.uppercase() == "BLOCKED") {
                auth.signOut()
                return Result.failure(Exception("Tài khoản của bạn đã bị khóa bởi Quản trị viên!"))
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
