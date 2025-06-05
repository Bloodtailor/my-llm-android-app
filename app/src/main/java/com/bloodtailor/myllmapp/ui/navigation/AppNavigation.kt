package com.bloodtailor.myllmapp.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bloodtailor.myllmapp.ui.screens.ChatScreen
import com.bloodtailor.myllmapp.ui.screens.ParametersScreen
import com.bloodtailor.myllmapp.ui.state.UiStateManager
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel

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
            0 -> {
                // Chat Screen
                ChatScreen(
                    viewModel = viewModel,
                    uiStateManager = uiStateManager
                )
            }
            1 -> {
                // Parameters Screen
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