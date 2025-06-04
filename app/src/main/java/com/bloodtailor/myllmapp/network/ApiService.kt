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
     * Fetch loading parameters from the server
     */
    suspend fun fetchLoadingParameters(): Result<LoadingParameters> {
        val tag = "ApiService"
        android.util.Log.d(tag, "Fetching loading parameters from: $serverBaseUrl/model/loading-parameters")

        return try {
            val request = Request.Builder()
                .url("$serverBaseUrl/model/loading-parameters")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    android.util.Log.d(tag, "Loading parameters response: $responseBody")

                    try {
                        val jsonResponse = JSONObject(responseBody)

                        // Parse global defaults
                        val globalDefaults = mutableMapOf<String, LoadingParameter>()
                        val globalDefaultsJson = jsonResponse.getJSONObject("global_defaults")
                        for (key in globalDefaultsJson.keys()) {
                            val paramJson = globalDefaultsJson.getJSONObject(key)
                            globalDefaults[key] = LoadingParameter(
                                default = paramJson.get("default"),
                                min = if (paramJson.has("min")) paramJson.get("min") else null,
                                max = if (paramJson.has("max")) paramJson.get("max") else null,
                                type = paramJson.optString("type", "number"),
                                description = paramJson.optString("description", "")
                            )
                        }

                        // Parse model-specific parameters
                        val modelSpecific = mutableMapOf<String, Map<String, LoadingParameter>>()
                        if (jsonResponse.has("model_specific")) {
                            val modelSpecificJson = jsonResponse.getJSONObject("model_specific")
                            for (modelName in modelSpecificJson.keys()) {
                                val modelParams = mutableMapOf<String, LoadingParameter>()
                                val modelParamsJson = modelSpecificJson.getJSONObject(modelName)
                                for (paramName in modelParamsJson.keys()) {
                                    val paramJson = modelParamsJson.getJSONObject(paramName)
                                    modelParams[paramName] = LoadingParameter(
                                        default = paramJson.get("default"),
                                        min = if (paramJson.has("min")) paramJson.get("min") else null,
                                        max = if (paramJson.has("max")) paramJson.get("max") else null,
                                        type = paramJson.optString("type", "number"),
                                        description = paramJson.optString("description", "")
                                    )
                                }
                                modelSpecific[modelName] = modelParams
                            }
                        }

                        val loadingParameters = LoadingParameters(
                            globalDefaults = globalDefaults,
                            modelSpecific = modelSpecific
                        )

                        android.util.Log.d(tag, "Parsed loading parameters successfully")
                        Result.success(loadingParameters)
                    } catch (e: Exception) {
                        android.util.Log.e(tag, "Error parsing loading parameters JSON", e)
                        Result.failure(Exception("Failed to parse loading parameters: ${e.message}"))
                    }
                } else {
                    android.util.Log.w(tag, "Server returned error: ${response.code} - ${response.message}")
                    Result.failure(Exception("Failed to fetch loading parameters: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(tag, "Network error fetching loading parameters", e)
            Result.failure(e)
        }
    }

    /**
     * Load a model with custom loading parameters
     */
    suspend fun loadModelWithParameters(
        modelName: String,
        loadingParams: Map<String, Any>
    ): Result<ModelLoadResult> {
        return try {
            val jsonObject = JSONObject()
            jsonObject.put("model", modelName)

            // Add all loading parameters
            for ((key, value) in loadingParams) {
                if (key != "model") { // Don't duplicate the model key
                    jsonObject.put(key, value)
                }
            }

            val jsonRequest = jsonObject.toString()
            android.util.Log.d("ApiService", "Loading model with params: $jsonRequest")

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
                        contextLength = if (jsonResponse.has("loading_parameters")) {
                            val loadingParamsJson = jsonResponse.getJSONObject("loading_parameters")
                            if (loadingParamsJson.has("n_ctx")) loadingParamsJson.getInt("n_ctx") else null
                        } else null,
                        message = jsonResponse.optString("message", "Model loaded successfully")
                    )
                    Result.success(result)
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    android.util.Log.e("ApiService", "Load model failed: $errorBody")
                    Result.failure(Exception("Failed to load model: ${response.code} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiService", "Exception loading model", e)
            Result.failure(e)
        }
    }

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
     * Fetch inference parameters from the server
     */
    suspend fun fetchInferenceParameters(modelName: String? = null): Result<InferenceParameters> {
        val tag = "ApiService"

        val url = if (modelName != null) {
            "$serverBaseUrl/model/inference-parameters?model=$modelName"
        } else {
            "$serverBaseUrl/model/inference-parameters"
        }

        android.util.Log.d(tag, "Fetching inference parameters from: $url")

        return try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    android.util.Log.d(tag, "Inference parameters response: $responseBody")

                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val model = jsonResponse.getString("model")
                        val parametersJson = jsonResponse.getJSONObject("parameters")

                        val parameters = mutableMapOf<String, InferenceParameter>()

                        for (key in parametersJson.keys()) {
                            val paramJson = parametersJson.getJSONObject(key)
                            parameters[key] = InferenceParameter(
                                current = paramJson.getDouble("current").toFloat(),
                                default = paramJson.getDouble("default").toFloat(),
                                min = paramJson.optDouble("min", 0.0).toFloat(),
                                max = paramJson.optDouble("max", 2.0).toFloat(),
                                type = paramJson.optString("type", "float"),
                                description = paramJson.optString("description", "")
                            )
                        }

                        val inferenceParameters = InferenceParameters(
                            model = model,
                            parameters = parameters
                        )

                        android.util.Log.d(tag, "Parsed inference parameters successfully")
                        Result.success(inferenceParameters)
                    } catch (e: Exception) {
                        android.util.Log.e(tag, "Error parsing inference parameters JSON", e)
                        Result.failure(Exception("Failed to parse inference parameters: ${e.message}"))
                    }
                } else {
                    android.util.Log.w(tag, "Server returned error: ${response.code} - ${response.message}")
                    Result.failure(Exception("Failed to fetch inference parameters: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(tag, "Network error fetching inference parameters", e)
            Result.failure(e)
        }
    }

    /**
     * Send a streaming prompt to the server with a callback for processing chunks
     * Now sends raw prompts without any formatting
     */
    /**
     * Send a streaming prompt to the server with optional inference parameters
     */
    fun sendStreamingPrompt(
        prompt: String,
        systemPrompt: String = "",
        modelName: String,
        inferenceParams: Map<String, Float> = emptyMap(),
        callback: (status: String, content: String) -> Unit
    ) {
        try {
            // Create JSON request with inference parameters
            val jsonObject = JSONObject()
            jsonObject.put("prompt", prompt)
            jsonObject.put("system_prompt", systemPrompt)
            jsonObject.put("model", modelName)
            jsonObject.put("stream", true)

            // Add inference parameters to the request
            for ((key, value) in inferenceParams) {
                jsonObject.put(key, value)
            }

            val jsonRequest = jsonObject.toString()
            android.util.Log.d("ApiService", "Sending streaming request with inference params: $jsonRequest")

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
                    callback("error", e.message ?: "Network error")
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            callback("error", "Server error: ${response.code}")
                            return
                        }

                        val responseBody = response.body
                        if (responseBody == null) {
                            callback("error", "Empty response")
                            return
                        }

                        responseBody.source().use { source ->
                            while (!source.exhausted()) {
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

/**
 * Represents a single loading parameter with its constraints and metadata
 */
data class LoadingParameter(
    val default: Any,
    val min: Any? = null,
    val max: Any? = null,
    val type: String = "number", // "number", "boolean", "integer", "float"
    val description: String
)

/**
 * Represents all available loading parameters from the server
 */
data class LoadingParameters(
    val globalDefaults: Map<String, LoadingParameter>,
    val modelSpecific: Map<String, Map<String, LoadingParameter>>
)

/**
 * Represents the current values for loading parameters
 */
data class LoadingParameterValues(
    val values: MutableMap<String, Any> = mutableMapOf()
) {
    fun setValue(key: String, value: Any) {
        values[key] = value
    }

    fun getValue(key: String): Any? = values[key]

    fun getValueOrDefault(key: String, parameter: LoadingParameter): Any {
        return values[key] ?: parameter.default
    }

    fun toMap(): Map<String, Any> = values.toMap()
}

/**
 * Represents a single inference parameter with its current value and constraints
 */
data class InferenceParameter(
    val current: Float,
    val default: Float,
    val min: Float,
    val max: Float,
    val type: String = "float",
    val description: String
)

/**
 * Represents all inference parameters for a model
 */
data class InferenceParameters(
    val model: String,
    val parameters: Map<String, InferenceParameter>
)

/**
 * Represents the current values for inference parameters
 */
data class InferenceParameterValues(
    val values: MutableMap<String, Float> = mutableMapOf()
) {
    fun setValue(key: String, value: Float) {
        values[key] = value
    }

    fun getValue(key: String): Float? = values[key]

    fun getValueOrDefault(key: String, parameter: InferenceParameter): Float {
        return values[key] ?: parameter.current
    }

    fun toMap(): Map<String, Float> = values.toMap()
}