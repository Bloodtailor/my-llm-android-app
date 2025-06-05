package com.bloodtailor.myllmapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.bloodtailor.myllmapp.ui.LLMApp
import com.bloodtailor.myllmapp.ui.theme.MyLLMAppTheme
import com.bloodtailor.myllmapp.viewmodel.LlmViewModel
import com.bloodtailor.myllmapp.viewmodel.LlmViewModelFactory

/**
 * Main activity - handles only activity lifecycle and ViewModel initialization
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LlmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel(savedInstanceState)

        setContent {
            MyLLMAppTheme {
                LLMApp(viewModel = viewModel)
            }
        }
    }

    /**
     * Initialize ViewModel with proper state restoration handling
     */
    private fun initializeViewModel(savedInstanceState: Bundle?) {
        // Get application's repository instance
        val repository = (application as LlmApplication).repository

        // Initialize ViewModel with SavedStateHandle support for rotation persistence
        val factory = LlmViewModelFactory(application, this)
        viewModel = ViewModelProvider(this, factory)[LlmViewModel::class.java]

        // Handle server URL initialization based on whether this is a fresh start or rotation
        if (savedInstanceState == null) {
            // Fresh start - update ViewModel with repository URL and auto-connect
            viewModel.updateServerUrl(repository.getServerUrl(), true)
        } else {
            // Rotation - update repository with ViewModel URL (no auto-connect)
            repository.updateServerUrl(viewModel.serverUrl)
        }
    }
}