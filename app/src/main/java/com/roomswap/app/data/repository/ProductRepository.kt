package com.roomswap.app.data.repository

import com.roomswap.app.SupabaseClientProvider
import com.roomswap.app.data.model.Product
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class ProductRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
) {
    suspend fun listCatalog(): List<Product> =
        client.from("products").select().decodeList()

    // The `products` RLS policy restricts writes to the Main Company's super_admin.
    suspend fun addProduct(product: Product) {
        client.from("products").insert(product)
    }
}
