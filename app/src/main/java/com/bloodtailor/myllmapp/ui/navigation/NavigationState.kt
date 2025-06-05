package com.bloodtailor.myllmapp.ui.navigation

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
            pagerState.animateScrollToPage(0)
        }
    }

    fun navigateToParameters() {
        coroutineScope.launch {
            pagerState.animateScrollToPage(1)
        }
    }

    val currentPage: Int
        get() = pagerState.currentPage

    val isOnChatScreen: Boolean
        get() = currentPage == 0

    val isOnParametersScreen: Boolean
        get() = currentPage == 1
}

/**
 * Remember navigation state across recompositions
 */
@Composable
fun rememberNavigationState(): NavigationState {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    return remember(pagerState, coroutineScope) {
        NavigationState(pagerState, coroutineScope)
    }
}