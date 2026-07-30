package com.roomswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.roomswap.app.data.model.Company
import kotlinx.coroutines.tasks.await

class CompanyRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    // Only reachable for super_admin per firestore.rules; client never passes companyId itself.
    suspend fun listCompanies(): List<Company> =
        firestore.collection("companies").get().await().toObjects(Company::class.java)

    suspend fun createCompany(company: Company) {
        firestore.collection("companies").document().set(company).await()
    }
}
