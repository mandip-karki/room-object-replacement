package com.roomswap.app.ui.superadmin

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.roomswap.app.data.model.Company
import com.roomswap.app.data.repository.CompanyRepository

@Composable
fun CompanyListScreen(repository: CompanyRepository = CompanyRepository()) {
    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }

    LaunchedEffect(Unit) {
        companies = repository.listCompanies()
    }

    LazyColumn {
        items(companies) { company ->
            ListItem(
                headlineContent = { Text(company.name) },
                supportingContent = { Text(company.type.name) },
            )
        }
    }
}
