package com.roomswap.app.data.repository

import com.roomswap.app.SupabaseClientProvider
import com.roomswap.app.data.model.Company
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class CompanyRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
) {
    // Only reachable for super_admin per the RLS policy on `companies`; the client never passes companyId itself.
    suspend fun listCompanies(): List<Company> =
        client.from("companies").select().decodeList()

    suspend fun createCompany(company: Company) {
        client.from("companies").insert(company)
    }
}
