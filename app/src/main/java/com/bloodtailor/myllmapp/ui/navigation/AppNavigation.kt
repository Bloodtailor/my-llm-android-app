package com.bloodtailor.myllmapp.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bloodtailor.myllmapp.ui.screens.ChatScreen
import com.bloodtailor.myllmapp.ui.screens.PromptsScreen
import com.bloodtailor.myllmapp.ui.screens.ParametersScreen
import com.bloodtailor.myllmapp.ui.state.UiStateManager
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel
import com.bloodtailor.myllmapp.util.AppConstants

/**
 * Main navigation component using HorizontalPager for swipeable screens
 */
@Composable
fun AppNavigation(
    pagerState: PagerState,
    viewModel: LlmViewModel,
    uiStateManager: UiStateManager,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            AppConstants.NAV_PROMPTS_INDEX -> {
                // Prompts Screen (left)
                PromptsScreen(
                    viewModel = viewModel,
                    uiStateManager = uiStateManager
                )
            }
            AppConstants.NAV_CHAT_INDEX -> {
                // Chat Screen (middle - default)
                ChatScreen(
                    viewModel = viewModel,
                    uiStateManager = uiStateManager
                )
            }
            AppConstants.NAV_PARAMETERS_INDEX -> {
                // Parameters Screen (right)
                ParametersScreen(
                    viewModel = viewModel,
                    onBackSwipe = {
                        // This will be handled by the bottom nav now
                    }
                )
            }
        }
    }
}