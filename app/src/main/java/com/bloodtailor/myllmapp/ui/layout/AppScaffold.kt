package com.bloodtailor.myllmapp.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bloodtailor.myllmapp.ui.navigation.AppBottomNavigation
import com.bloodtailor.myllmapp.ui.navigation.NavigationState
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

/**
 * Main app scaffold with top bar, bottom navigation, and content area
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    viewModel: LlmViewModel,
    navigationState: NavigationState,
    onSettingsClick: () -> Unit,
    onModelClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                viewModel = viewModel,
                onSettingsClick = onSettingsClick,
                onModelClick = onModelClick
            )
        },
        bottomBar = {
            AppBottomNavigation(
                currentPage = navigationState.currentPage,
                isParametersEnabled = viewModel.currentModelLoaded,
                onNavigateToChat = {
                    navigationState.navigateToChat()
                },
                onNavigateToParameters = {
                    navigationState.navigateToParameters()
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        content(innerPadding)
    }
}