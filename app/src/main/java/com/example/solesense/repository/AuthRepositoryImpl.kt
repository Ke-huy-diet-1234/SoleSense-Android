package com.example.solesense.repository

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.example.solesense.model.user.User
import com.facebook.AccessToken
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl (
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
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

            if (snapshot.exists()) {
                val existingUser = snapshot.toObject(User::class.java)!!
                Result.success(existingUser)
            } else {
                val newUser = User(
                    userId = firebaseUser.uid,
                    fullName = firebaseUser.displayName ?: "Khách hàng",
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                    role = "CUSTOMER",
                    status = "ACTIVE",
                    isVerified = true
                )
                userDocRef.set(newUser).await()
                Result.success(newUser)
            }
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

            if (snapshot.exists()) {
                val existingUser = snapshot.toObject(User::class.java)!!
                Result.success(existingUser)
            } else {
                val newUser = User(
                    userId = firebaseUser.uid,
                    fullName = firebaseUser.displayName ?: "Người dùng Facebook",
                    email = firebaseUser.email ?: "",
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                    role = "CUSTOMER", // Gán mặc định là CUSTOMER
                    status = "ACTIVE",
                    isVerified = true
                )
                userDocRef.set(newUser).await()
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

