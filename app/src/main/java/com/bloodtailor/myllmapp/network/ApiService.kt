package com.bloodtailor.myllmapp.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service for handling all API communication with the LLM server
 */
class ApiService(private val serverBaseUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)  // Increased timeout for streaming
        .build()


    /**
     * Fetch available models from the server with enhanced logging
     */
    suspend fun fetchModels(): Result<List<String>> {
        val tag = "ApiService"
        android.util.Log.d(tag, "Fetching models from: $serverBaseUrl/models")

        return try {
            val request = Request.Builder()
                .url("$serverBaseUrl/models")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    android.util.Log.d(tag, "Response body: $responseBody")

                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val models = jsonResponse.getJSONArray("models")

                        val modelsList = mutableListOf<String>()
                        for (i in 0 until models.length()) {
                            modelsList.add(models.getString(i))
                        }

                        android.util.Log.d(tag, "Parsed ${modelsList.size} models: ${modelsList.joinToString()}")
                        Result.success(modelsList)
                    } catch (e: Exception) {
                        android.util.Log.e(tag, "Error parsing JSON response", e)
                        Result.failure(Exception("Failed to parse models: ${e.message}"))
                    }
                } else {
                    android.util.Log.w(tag, "Server returned error: ${response.code} - ${response.message}")
                    Result.failure(Exception("Failed to fetch models: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(tag, "Network error fetching models", e)
            Result.failure(e)
        }
    }

    /**
     * Check the current model status on the server
     */
    suspend fun checkModelStatus(): Result<ModelStatus> {
        return try {
            val request = Request.Builder()
                .url("$serverBaseUrl/model/status")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonResponse = JSONObject(responseBody)

                    val modelStatus = ModelStatus(
                        loaded = jsonResponse.getBoolean("loaded"),
                        currentModel = if (jsonResponse.isNull("current_model")) null
                        else jsonResponse.getString("current_model"),
                        contextLength = if (jsonResponse.isNull("context_length")) null
                        else jsonResponse.getInt("context_length")
                    )
                    Result.success(modelStatus)
                } else {
                    Result.failure(Exception("Failed to check model status: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load a model on the server
     */
    suspend fun loadModel(modelName: String, contextLength: Int? = null): Result<ModelLoadResult> {
        return try {
            val jsonObject = JSONObject()
            jsonObject.put("model", modelName)
            if (contextLength != null) {
                jsonObject.put("context_length", contextLength)
            }
            val jsonRequest = jsonObject.toString()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonRequest.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$serverBaseUrl/model/load")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonResponse = JSONObject(responseBody)

                    val result = ModelLoadResult(
                        model = modelName,
                        contextLength = if (jsonResponse.has("context_length"))
                            jsonResponse.getInt("context_length")
                        else
                            contextLength,
                        message = jsonResponse.optString("message", "Model loaded successfully")
                    )
                    Result.success(result)
                } else {
                    Result.failure(Exception("Failed to load model: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unload the current model on the server
     */
    suspend fun unloadModel(): Result<String> {
        return try {
            val request = Request.Builder()
                .url("$serverBaseUrl/model/unload")
                .post(RequestBody.create(null, byteArrayOf()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonResponse = JSONObject(responseBody)
                    Result.success(jsonResponse.optString("message", "Model unloaded successfully"))
                } else {
                    Result.failure(Exception("Failed to unload model: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Count tokens in text and get context usage (replaces formatPrompt)
     */
    suspend fun countTokens(text: String, modelName: String): Result<TokenCountResult> {
        return try {
            val jsonObject = JSONObject()
            jsonObject.put("text", text)
            jsonObject.put("model", modelName)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$serverBaseUrl/count_tokens")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonResponse = JSONObject(responseBody)

                    val model = jsonResponse.optString("model", modelName)

                    // Parse context usage if available
                    val contextUsage = if (jsonResponse.has("context_usage")) {
                        val usageJson = jsonResponse.getJSONObject("context_usage")
                        ContextUsage(
                            tokenCount = usageJson.getInt("token_count"),
                            maxContext = usageJson.getInt("max_context"),
                            usagePercentage = usageJson.getDouble("usage_percentage").toFloat(),
                            remainingTokens = usageJson.getInt("remaining_tokens")
                        )
                    } else null

                    val result = TokenCountResult(
                        text = text,
                        model = model,
                        contextUsage = contextUsage
                    )

                    Result.success(result)
                } else {
                    Result.failure(Exception("Failed to count tokens: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch model prefix/suffix parameters from the server
     */
    suspend fun fetchModelParameters(modelName: String? = null): Result<ModelParameters> {
        return try {
            val url = if (modelName != null) {
                "$serverBaseUrl/model/parameters?model=$modelName"
            } else {
                "$serverBaseUrl/model/parameters"
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonResponse = JSONObject(responseBody)

                    val parameters = ModelParameters(
                        model = jsonResponse.getString("model"),
                        prePromptPrefix = jsonResponse.getString("pre_prompt_prefix"),
                        prePromptSuffix = jsonResponse.getString("pre_prompt_suffix"),
                        inputPrefix = jsonResponse.getString("input_prefix"),
                        inputSuffix = jsonResponse.getString("input_suffix"),
                        assistantPrefix = jsonResponse.getString("assistant_prefix"),
                        assistantSuffix = jsonResponse.getString("assistant_suffix")
                    )

                    Result.success(parameters)
                } else {
                    Result.failure(Exception("Failed to fetch model parameters: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a streaming prompt to the server with a callback for processing chunks
     * Now sends raw prompts without any formatting
     */
    fun sendStreamingPrompt(
        prompt: String,
        systemPrompt: String = "",
        modelName: String,
        callback: (status: String, content: String) -> Unit
    ) {
        try {
            // Create JSON request - removed formatted_prompt parameter
            val jsonObject = JSONObject()
            jsonObject.put("prompt", prompt)  // Send raw prompt exactly as typed
            jsonObject.put("system_prompt", systemPrompt)
            jsonObject.put("model", modelName)
            jsonObject.put("stream", true)

            val jsonRequest = jsonObject.toString()

            // Prepare the request
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonRequest.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$serverBaseUrl/query")
                .post(requestBody)
                .build()

            // Execute the request
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Handle failure
                    callback("error", e.message ?: "Network error")
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            callback("error", "Server error: ${response.code}")
                            return
                        }

                        // Read streaming responses
                        val responseBody = response.body
                        if (responseBody == null) {
                            callback("error", "Empty response")
                            return
                        }

                        responseBody.source().use { source ->
                            while (!source.exhausted()) {
                                // Read a line
                                source.readUtf8Line()?.let { line ->
                                    if (line.isNotEmpty()) {
                                        try {
                                            val jsonResponse = JSONObject(line)
                                            val status = jsonResponse.optString("status", "")

                                            when (status) {
                                                "processing" -> {
                                                    // Initial response, nothing to do
                                                }
                                                "generating" -> {
                                                    val partial = jsonResponse.optString("partial", "")
                                                    callback("generating", partial)
                                                }
                                                "complete" -> {
                                                    val fullResponse = jsonResponse.optString("response", "")
                                                    callback("complete", fullResponse)
                                                }
                                                "error" -> {
                                                    val error = jsonResponse.optString("error", "Unknown error")
                                                    callback("error", error)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            // Skip invalid JSON lines
                                        }
                                    }
                                }
                            }
                        }

                    } catch (e: Exception) {
                        callback("error", "Error processing response: ${e.message}")
                    } finally {
                        response.close()
                    }
                }
            })

        } catch (e: Exception) {
            callback("error", "Error: ${e.message}")
        }
    }
}

/**
 * Data class representing the model status
 */
data class ModelStatus(
    val loaded: Boolean,
    val currentModel: String?,
    val contextLength: Int?
)

/**
 * Data class representing the result of loading a model
 */
data class ModelLoadResult(
    val model: String,
    val contextLength: Int?,
    val message: String
)

/**
 * Data class representing context usage information
 */
data class ContextUsage(
    val tokenCount: Int,
    val maxContext: Int,
    val usagePercentage: Float,
    val remainingTokens: Int
)

/**
 * Data class representing the result of counting tokens (replaces PromptFormatResult)
 */
data class TokenCountResult(
    val text: String,
    val model: String,
    val contextUsage: ContextUsage?
)

/**
 * Data class for model parameters
 */
data class ModelParameters(
    val model: String,
    val prePromptPrefix: String,
    val prePromptSuffix: String,
    val inputPrefix: String,
    val inputSuffix: String,
    val assistantPrefix: String,
    val assistantSuffix: String
)

