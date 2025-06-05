package com.bloodtailor.myllmapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bloodtailor.myllmapp.ui.screens.ChatScreen
import com.bloodtailor.myllmapp.ui.screens.ParametersScreen
import com.bloodtailor.myllmapp.ui.state.UiStateManager
import com.bloodtailor.myllmapp.util.AppConstants
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

/**
 * Main navigation component using Navigation Compose
 */
@Composable
fun AppNavigation(
    navigationState: NavigationState,
    viewModel: LlmViewModel,
    uiStateManager: UiStateManager,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navigationState.navController,
        startDestination = AppConstants.ROUTE_CHAT,
        modifier = modifier
    ) {
        // Chat Screen
        composable(AppConstants.ROUTE_CHAT) {
            ChatScreen(
                viewModel = viewModel,
                uiStateManager = uiStateManager,
                onNavigateToParameters = {
                    navigationState.navigateToParameters()
                }
            )
        }

        // Parameters Screen
        composable(AppConstants.ROUTE_PARAMETERS) {
            ParametersScreen(
                viewModel = viewModel,
                onBackSwipe = {
                    navigationState.navigateBack()
                }
            )
        }
    }
}