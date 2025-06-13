package com.bloodtailor.myllmapp.data

import android.content.Context
import com.bloodtailor.myllmapp.network.ApiService
import com.bloodtailor.myllmapp.network.ModelLoadResult
import com.bloodtailor.myllmapp.network.ModelStatus
import com.bloodtailor.myllmapp.network.TokenCountResult
import com.bloodtailor.myllmapp.network.ModelParameters
import com.bloodtailor.myllmapp.network.LoadingParameters
import com.bloodtailor.myllmapp.network.InferenceParameters
import com.bloodtailor.myllmapp.util.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing LLM-related data and operations
 */
class LlmRepository(
    private val context: Context,
    private var serverUrl: String
) {
    // Initialize API service
    private var apiService = ApiService(serverUrl)

    init {
        // Load saved server URL from preferences
        val sharedPref = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        serverUrl = sharedPref.getString(AppConstants.SERVER_URL_KEY, AppConstants.DEFAULT_SERVER_URL)
            ?: AppConstants.DEFAULT_SERVER_URL
        apiService = ApiService(serverUrl)
    }

    /**
     * Fetch model prefix/suffix parameters
     */
    suspend fun getModelParameters(modelName: String? = null): Result<ModelParameters> =
        withContext(Dispatchers.IO) {
            apiService.fetchModelParameters(modelName)
        }

    /**
     * Fetch loading parameters from the server
     */
    suspend fun getLoadingParameters(): Result<LoadingParameters> =
        withContext(Dispatchers.IO) {
            apiService.fetchLoadingParameters()
        }

    /**
     * Update the server URL and save it to preferences
     */
    fun updateServerUrl(url: String) {
        serverUrl = url
        apiService = ApiService(url)

        // Save to preferences
        val sharedPref = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(AppConstants.SERVER_URL_KEY, url)
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
     * Load a model on the server with custom loading parameters
     */
    suspend fun loadModelWithParameters(
        modelName: String,
        loadingParams: Map<String, Any>
    ): Result<ModelLoadResult> = withContext(Dispatchers.IO) {
        apiService.loadModelWithParameters(modelName, loadingParams)
    }

    /**
     * Load a model on the server (legacy method for compatibility)
     */
    suspend fun loadModel(modelName: String, contextLength: Int? = null): Result<ModelLoadResult> =
        withContext(Dispatchers.IO) {
            val params = mutableMapOf<String, Any>("model" to modelName)
            if (contextLength != null) {
                params["n_ctx"] = contextLength
            }
            loadModelWithParameters(modelName, params)
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
     * Fetch inference parameters from the server
     */
    suspend fun getInferenceParameters(modelName: String? = null): Result<InferenceParameters> =
        withContext(Dispatchers.IO) {
            apiService.fetchInferenceParameters(modelName)
        }

    /**
     * Send a prompt with custom inference parameters
     */
    fun sendPromptWithInferenceParameters(
        prompt: String,
        systemPrompt: String = "",
        modelName: String,
        inferenceParams: Map<String, Float>,
        callback: (status: String, content: String) -> Unit
    ) {
        // Use the enhanced sendStreamingPrompt method that includes inference parameters
        apiService.sendStreamingPrompt(
            prompt = prompt,
            systemPrompt = systemPrompt,
            modelName = modelName,
            inferenceParams = inferenceParams,
            callback = callback
        )
    }
}