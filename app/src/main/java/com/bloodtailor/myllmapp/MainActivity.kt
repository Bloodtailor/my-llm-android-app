package com.bloodtailor.myllmapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import com.bloodtailor.myllmapp.ui.*
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel
import com.bloodtailor.myllmapp.viewmodel.LlmViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LlmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get application's repository instance
        val repository = (application as LlmApplication).repository

        // Initialize ViewModel with SavedStateHandle support for rotation persistence
        val factory = LlmViewModelFactory(application, this)
        viewModel = ViewModelProvider(this, factory).get(LlmViewModel::class.java)

        // Only update server URL if this is a fresh start (not a rotation)
        if (savedInstanceState == null) {
            viewModel.updateServerUrl(repository.getServerUrl(), true)
        } else {
            repository.updateServerUrl(viewModel.serverUrl)
        }

        setContent {
            LLMAppUI()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LLMAppUI() {
        // UI state that persists across screen rotations
        var prompt by rememberSaveable { mutableStateOf("") }
        var showFormattedPrompt by rememberSaveable { mutableStateOf(false) }
        var localFormattedPrompt by rememberSaveable { mutableStateOf("") }
        var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
        var showModelDialog by rememberSaveable { mutableStateOf(false) }
        var showSettingsOnStart by rememberSaveable { mutableStateOf(true) }
        var showFullScreenInput by rememberSaveable { mutableStateOf(false) }
        var showFullScreenResponse by rememberSaveable { mutableStateOf(false) }

        // Pager state for swipeable screens
        val pagerState = rememberPagerState(pageCount = { 2 })
        val coroutineScope = rememberCoroutineScope()

        // Add connection status state
        val connectionStatus = remember {
            derivedStateOf {
                if (viewModel.availableModels.isNotEmpty()) "Connected" else "Not Connected"
            }
        }

        // Show settings dialog on first launch
        LaunchedEffect(key1 = showSettingsOnStart) {
            if (showSettingsOnStart && viewModel.availableModels.isEmpty()) {
                showSettingsDialog = true
                showSettingsOnStart = false
            }
        }

        MaterialTheme {
            if (showFullScreenInput) {
                FullScreenPromptEditor(
                    prompt = prompt,
                    onPromptChanged = { prompt = it },
                    onSend = {
                        if (pagerState.currentPage == 1) {
                            // We're on inference parameters page - use custom parameters
                            viewModel.sendPrompt(
                                prompt = prompt,
                                systemPrompt = ""
                            )
                        } else {
                            // Regular send
                            viewModel.sendPrompt(
                                prompt = prompt,
                                systemPrompt = ""
                            )
                        }
                    },
                    onClose = { showFullScreenInput = false },
                    viewModel = viewModel
                )
            } else if (showFullScreenResponse) {
                FullScreenResponseViewer(
                    response = viewModel.llmResponse,
                    onClose = { showFullScreenResponse = false }
                )
            } else {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("LLM App")

                                    // Page indicator
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        repeat(2) { index ->
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (pagerState.currentPage == index) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                // Connection status indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = if (connectionStatus.value == "Connected")
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.error,
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        text = connectionStatus.value,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                // Settings button
                                IconButton(onClick = { showSettingsDialog = true }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Server Settings"
                                    )
                                }

                                // Model settings button
                                IconButton(onClick = { showModelDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = "Model Settings"
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        // Enhanced bottom navigation with page tabs
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Chat tab
                                NavigationTab(
                                    icon = Icons.Default.Chat,
                                    label = "Chat",
                                    isSelected = pagerState.currentPage == 0,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    }
                                )

                                // Inference Parameters tab
                                NavigationTab(
                                    icon = Icons.Default.Tune,
                                    label = "Parameters",
                                    isSelected = pagerState.currentPage == 1,
                                    enabled = viewModel.currentModelLoaded,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(1)
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    // Swipeable content
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) { page ->
                        when (page) {
                            0 -> {
                                // Main chat screen
                                MainChatScreen(
                                    viewModel = viewModel,
                                    prompt = prompt,
                                    onPromptChanged = { prompt = it },
                                    showFormattedPrompt = showFormattedPrompt,
                                    onShowFormattedPromptChanged = { showFormattedPrompt = it },
                                    localFormattedPrompt = localFormattedPrompt,
                                    onFormattedPromptUpdated = { localFormattedPrompt = it },
                                    onExpandInputClick = { showFullScreenInput = true },
                                    onExpandResponseClick = { showFullScreenResponse = true }
                                )
                            }
                            1 -> {
                                // Inference parameters screen
                                InferenceParametersScreen(
                                    viewModel = viewModel,
                                    onBackSwipe = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Dialogs
                SettingsDialog(
                    showDialog = showSettingsDialog,
                    currentServerUrl = viewModel.serverUrl,
                    onDismiss = { showSettingsDialog = false },
                    onSave = { newUrl ->
                        viewModel.updateServerUrl(newUrl, true)
                    }
                )

                ModelSettingsDialog(
                    showDialog = showModelDialog,
                    viewModel = viewModel,
                    onDismiss = { showModelDialog = false }
                )
            }
        }
    }

    @Composable
    private fun NavigationTab(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        isSelected: Boolean,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    } else Color.Transparent
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .then(
                    if (enabled) {
                        Modifier.clickable { onClick() }
                    } else Modifier
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }

    @Composable
    private fun MainChatScreen(
        viewModel: LlmViewModel,
        prompt: String,
        onPromptChanged: (String) -> Unit,
        showFormattedPrompt: Boolean,
        onShowFormattedPromptChanged: (Boolean) -> Unit,
        localFormattedPrompt: String,
        onFormattedPromptUpdated: (String) -> Unit,
        onExpandInputClick: () -> Unit,
        onExpandResponseClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status message if needed
            if (viewModel.statusMessage.isNotEmpty()) {
                StatusMessage(viewModel.statusMessage)
            }

            // Current model indicator
            if (viewModel.currentModelLoaded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Using model: ${viewModel.currentModel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Prompt input
            PromptInput(
                prompt = prompt,
                onPromptChanged = onPromptChanged,
                showFormattedPrompt = showFormattedPrompt,
                onShowFormattedPromptChanged = onShowFormattedPromptChanged,
                formattedPrompt = localFormattedPrompt,
                viewModel = viewModel,
                onFormattedPromptUpdated = onFormattedPromptUpdated,
                onExpandClick = onExpandInputClick
            )

            // Response display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ResponseDisplay(
                    response = viewModel.llmResponse,
                    isLoading = viewModel.isLoading,
                    onExpandClick = onExpandResponseClick
                )
            }
        }
    }
}