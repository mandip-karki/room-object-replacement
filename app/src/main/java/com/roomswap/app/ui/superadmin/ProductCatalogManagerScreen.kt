package com.roomswap.app.ui.superadmin

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
import com.roomswap.app.data.model.Product
import com.roomswap.app.data.repository.ProductRepository

@Composable
fun ProductCatalogManagerScreen(repository: ProductRepository = ProductRepository()) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }

    LaunchedEffect(Unit) {
        products = repository.listCatalog()
    }

    // TODO: wire up an "upload product" form (name, category, image picker -> Storage).
    LazyColumn {
        items(products) { product ->
            ListItem(
                headlineContent = { Text(product.name) },
                supportingContent = { Text(product.category) },
            )
        }
    }
}
