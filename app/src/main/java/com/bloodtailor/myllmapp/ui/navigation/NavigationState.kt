package com.bloodtailor.myllmapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.bloodtailor.myllmapp.util.AppConstants

/**
 * Navigation state management and helper functions
 */
class NavigationState(
    val navController: NavHostController
) {
    fun navigateToChat() {
        navController.navigate(AppConstants.ROUTE_CHAT) {
            popUpTo(AppConstants.ROUTE_CHAT) { inclusive = true }
        }
    }

    fun navigateToParameters() {
        navController.navigate(AppConstants.ROUTE_PARAMETERS) {
            popUpTo(AppConstants.ROUTE_CHAT)
        }
    }

    fun navigateBack() {
        navController.popBackStack()
    }

    val currentRoute: String?
        get() = navController.currentDestination?.route
}

/**
 * Remember navigation state across recompositions
 */
@Composable
fun rememberNavigationState(
    navController: NavHostController = rememberNavController()
): NavigationState {
    return remember(navController) {
        NavigationState(navController)
    }
}