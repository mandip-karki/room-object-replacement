package com.roomswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.roomswap.app.data.model.Product
import kotlinx.coroutines.tasks.await

class ProductRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun listCatalog(): List<Product> =
        firestore.collection("products").get().await().toObjects(Product::class.java)

    // Server (Firestore rules) restricts writes to the Main Company's super_admin.
    suspend fun addProduct(product: Product) {
        firestore.collection("products").document().set(product).await()
    }
}
