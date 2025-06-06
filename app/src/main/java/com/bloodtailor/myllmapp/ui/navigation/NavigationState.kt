package com.bloodtailor.myllmapp.ui.navigation

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.bloodtailor.myllmapp.util.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Navigation state management using HorizontalPager
 */
class NavigationState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) {
    fun navigateToChat() {
        coroutineScope.launch {
            pagerState.animateScrollToPage(AppConstants.NAV_CHAT_INDEX)
        }
    }

    fun navigateToPrompts() {
        coroutineScope.launch {
            pagerState.animateScrollToPage(AppConstants.NAV_PROMPTS_INDEX)
        }
    }

    fun navigateToParameters() {
        coroutineScope.launch {
            pagerState.animateScrollToPage(AppConstants.NAV_PARAMETERS_INDEX)
        }
    }

    val currentPage: Int
        get() = pagerState.currentPage

    val isOnChatScreen: Boolean
        get() = currentPage == AppConstants.NAV_CHAT_INDEX

    val isOnPromptsScreen: Boolean
        get() = currentPage == AppConstants.NAV_PROMPTS_INDEX

    val isOnParametersScreen: Boolean
        get() = currentPage == AppConstants.NAV_PARAMETERS_INDEX
}

/**
 * Remember navigation state across recompositions
 */
@Composable
fun rememberNavigationState(): NavigationState {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    return remember(pagerState, coroutineScope) {
        NavigationState(pagerState, coroutineScope)
    }
}