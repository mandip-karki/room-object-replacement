package com.roomswap.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roomswap.app.auth.AuthViewModel
import com.roomswap.app.auth.LoginScreen
import com.roomswap.app.data.model.User
import com.roomswap.app.data.model.UserRole
import com.roomswap.app.ui.clientadmin.SubAccountManagerScreen
import com.roomswap.app.ui.subaccount.ProductPickerScreen
import com.roomswap.app.ui.subaccount.ResultScreen
import com.roomswap.app.ui.subaccount.RoomPhotoScreen
import com.roomswap.app.ui.superadmin.CompanyListScreen
import com.roomswap.app.ui.superadmin.ProductCatalogManagerScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val authViewModel = AuthViewModel()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(viewModel = authViewModel) { user -> navigateForRole(navController, user) }
        }
        composable(Routes.SUPER_ADMIN_COMPANIES) { CompanyListScreen() }
        composable(Routes.SUPER_ADMIN_CATALOG) { ProductCatalogManagerScreen() }
        composable(Routes.CLIENT_ADMIN_SUB_ACCOUNTS) { SubAccountManagerScreen() }
        composable(Routes.ROOM_PHOTO) {
            RoomPhotoScreen { _, _, _, _ -> navController.navigate(Routes.PRODUCT_PICKER) }
        }
        composable(Routes.PRODUCT_PICKER) {
            ProductPickerScreen(
                onProductChosen = { navController.navigate(Routes.result("placeholder")) },
                onDevicePhotoChosen = { },
            )
        }
        composable(Routes.RESULT) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            ResultScreen(jobId = jobId)
        }
    }
}

private fun navigateForRole(navController: NavHostController, user: User) {
    val destination = when (user.role) {
        UserRole.SUPER_ADMIN -> Routes.SUPER_ADMIN_COMPANIES
        UserRole.CLIENT_ADMIN -> Routes.CLIENT_ADMIN_SUB_ACCOUNTS
        UserRole.SUB_ACCOUNT -> Routes.ROOM_PHOTO
    }
    navController.navigate(destination) {
        popUpTo(Routes.LOGIN) { inclusive = true }
    }
}
