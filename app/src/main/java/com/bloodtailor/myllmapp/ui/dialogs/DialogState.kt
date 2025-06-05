package com.bloodtailor.myllmapp.ui.dialogs

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Dialog state management for the entire app
 */
@Stable
class DialogState {
    var showSettings by mutableStateOf(false)
        private set

    var showModel by mutableStateOf(false)
        private set

    var showSettingsOnStart by mutableStateOf(true)
        private set

    fun showSettingsDialog() {
        showSettings = true
    }

    fun hideSettingsDialog() {
        showSettings = false
    }

    fun showModelDialog() {
        showModel = true
    }

    fun hideModelDialog() {
        showModel = false
    }

    fun checkInitialSetup(hasModels: Boolean) {
        if (showSettingsOnStart && !hasModels) {
            showSettingsDialog()
            showSettingsOnStart = false
        }
    }

    fun dismissSettingsOnStart() {
        showSettingsOnStart = false
    }
}