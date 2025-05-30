package com.bloodtailor.myllmapp.data

import android.content.Context
import com.bloodtailor.myllmapp.network.ApiService
import com.bloodtailor.myllmapp.network.ModelLoadResult
import com.bloodtailor.myllmapp.network.ModelStatus
import com.bloodtailor.myllmapp.network.ContextUsage
import com.bloodtailor.myllmapp.network.TokenCountResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing LLM-related data and operations
 */
class LlmRepository(
    private val context: Context,
    private var serverUrl: String
) {
    // Shared preferences name and keys
    private val PREFS_NAME = "LLMAppPreferences"
    private val SERVER_URL_KEY = "server_url"
    private val DEFAULT_SERVER_URL = "http://192.168.50.220:5000"

    // Initialize API service
    private var apiService = ApiService(serverUrl)

    init {
        // Load saved server URL from preferences
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        serverUrl = sharedPref.getString(SERVER_URL_KEY, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        apiService = ApiService(serverUrl)
    }

    /**
     * Update the server URL and save it to preferences
     */
    fun updateServerUrl(url: String) {
        serverUrl = url
        apiService = ApiService(url)

        // Save to preferences
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(SERVER_URL_KEY, url)
            apply()
        }
    }

    /**
     * Get the current server URL
     */
    fun getServerUrl(): String = serverUrl

    /**
     * Fetch available models from the server
     */
    suspend fun getAvailableModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        android.util.Log.d("LlmRepository", "Fetching models from $serverUrl")
        val result = apiService.fetchModels()

        result.fold(
            onSuccess = { models ->
                android.util.Log.d("LlmRepository", "Successfully fetched ${models.size} models: ${models.joinToString()}")
            },
            onFailure = { error ->
                android.util.Log.e("LlmRepository", "Failed to fetch models", error)
            }
        )

        return@withContext result
    }

    /**
     * Check the current model status
     */
    suspend fun checkModelStatus(): Result<ModelStatus> = withContext(Dispatchers.IO) {
        apiService.checkModelStatus()
    }

    /**
     * Load a model on the server
     */
    suspend fun loadModel(modelName: String, contextLength: Int? = null): Result<ModelLoadResult> =
        withContext(Dispatchers.IO) {
            apiService.loadModel(modelName, contextLength)
        }

    /**
     * Unload the current model
     */
    suspend fun unloadModel(): Result<String> = withContext(Dispatchers.IO) {
        apiService.unloadModel()
    }

    /**
     * Count tokens in text and get context usage (replaces formatPrompt)
     */
    suspend fun countTokens(text: String, modelName: String): Result<TokenCountResult> =
        withContext(Dispatchers.IO) {
            apiService.countTokens(text, modelName)
        }

    /**
     * Debugging method to check server connectivity
     */
    suspend fun checkServerConnectivity(): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = okhttp3.Request.Builder()
                .url("$serverUrl/server/ping")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    android.util.Log.d("LlmRepository", "Server ping successful")
                    Result.success(true)
                } else {
                    android.util.Log.w("LlmRepository", "Server ping failed with code: ${response.code}")
                    Result.success(false)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LlmRepository", "Server ping error", e)
            Result.failure(e)
        }
    }

    /**
     * Send a streaming prompt to the server (now sends raw prompts)
     * Note: This function doesn't use Dispatchers.IO as the ApiService
     * handles threading with OkHttp callback
     */
    fun sendStreamingPrompt(
        prompt: String,
        systemPrompt: String = "",
        modelName: String,
        callback: (status: String, content: String) -> Unit
    ) {
        // Remove formattedPrompt parameter - we're sending raw prompts now
        apiService.sendStreamingPrompt(
            prompt = prompt,
            systemPrompt = systemPrompt,
            modelName = modelName,
            callback = callback
        )
    }
}