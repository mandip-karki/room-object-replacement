package com.roomswap.app.ui.subaccount

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
fun ProductPickerScreen(
    onProductChosen: (imageUrl: String) -> Unit,
    onDevicePhotoChosen: () -> Unit,
    repository: ProductRepository = ProductRepository(),
) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }

    LaunchedEffect(Unit) {
        products = repository.listCatalog()
    }

    // TODO: add a "choose from device" entry point wired to onDevicePhotoChosen.
    LazyColumn {
        items(products) { product ->
            ListItem(
                headlineContent = { Text(product.name) },
                modifier = androidx.compose.ui.Modifier,
            )
        }
    }
}
