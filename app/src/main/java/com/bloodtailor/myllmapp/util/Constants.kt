package com.bloodtailor.myllmapp.util

/**
 * App-wide constants and configuration values
 */
object AppConstants {
    // Server Configuration
    const val DEFAULT_SERVER_URL = "http://localhost:5000"
    const val DEFAULT_SERVER_PORT = 5000

    // Shared Preferences
    const val PREFS_NAME = "LLMAppPreferences"
    const val SERVER_URL_KEY = "server_url"

    // Navigation Routes
    const val ROUTE_CHAT = "chat"
    const val ROUTE_PARAMETERS = "parameters"

    // Network Timeouts (in seconds)
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 60L
    const val WRITE_TIMEOUT = 30L
}