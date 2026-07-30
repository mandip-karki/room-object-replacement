package com.roomswap.app.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.roomswap.app.data.model.User
import com.roomswap.app.data.model.UserRole
import kotlinx.coroutines.tasks.await

/**
 * Role and companyId are read from the user's Firestore profile doc, not trusted from the
 * client elsewhere. Server-side Cloud Functions re-derive them from custom claims on every call.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun signIn(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: error("Sign-in succeeded but no uid returned")
        return loadProfile(uid)
    }

    suspend fun currentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return loadProfile(uid)
    }

    fun signOut() = auth.signOut()

    private suspend fun loadProfile(uid: String): User {
        val doc = firestore.collection("users").document(uid).get().await()
        return User(
            id = uid,
            companyId = doc.getString("companyId") ?: "",
            role = UserRole.valueOf(doc.getString("role") ?: UserRole.SUB_ACCOUNT.name),
            email = doc.getString("email") ?: "",
            createdAt = doc.getLong("createdAt") ?: 0L,
        )
    }
}
