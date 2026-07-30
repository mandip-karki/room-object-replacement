package com.roomswap.app.ui.clientadmin

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SubAccountManagerScreen() {
    // TODO: list sub-accounts scoped to the signed-in Client Admin's companyId (server-enforced),
    // with create/deactivate actions. Creation should call a Cloud Function that sets the
    // sub_account role + companyId as custom claims rather than trusting client input.
    Column {
        Text("Sub-accounts")
    }
}
