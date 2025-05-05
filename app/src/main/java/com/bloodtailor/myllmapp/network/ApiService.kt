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
     * Fetch available models from the server
     */
    suspend fun fetchModels(): Result<List<String>> {
        return try {
            val request = Request.Builder()
                .url("$serverBaseUrl/models")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonResponse = JSONObject(responseBody)
                    val models = jsonResponse.getJSONArray("models")

                    val modelsList = mutableListOf<String>()
                    for (i in 0 until models.length()) {
                        modelsList.add(models.getString(i))
                    }
                    Result.success(modelsList)
                } else {
                    Result.failure(Exception("Failed to fetch models: ${response.code}"))
                }
            }
        } catch (e: Exception) {
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
     * Format a prompt using the server's template
     */
    suspend fun formatPrompt(prompt: String, modelName: String): Result<String> {
        return try {
            val jsonObject = JSONObject()
            jsonObject.put("prompt", prompt)
            jsonObject.put("model", modelName)
            
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url("$serverBaseUrl/format_prompt")
                .post(requestBody)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonResponse = JSONObject(responseBody)
                    val formatted = jsonResponse.optString("formatted_prompt", "")
                    Result.success(formatted)
                } else {
                    Result.failure(Exception("Failed to format prompt: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send a streaming prompt to the server with a callback for processing chunks
     */
    fun sendStreamingPrompt(
        prompt: String, 
        systemPrompt: String = "", 
        formattedPromptOverride: String? = null,
        modelName: String,
        callback: (status: String, content: String) -> Unit
    ) {
        try {
            // Create JSON request
            val jsonObject = JSONObject()
            jsonObject.put("prompt", prompt)
            jsonObject.put("system_prompt", systemPrompt)
            jsonObject.put("model", modelName)
            jsonObject.put("stream", true)
            
            // Add formatted prompt if provided
            if (formattedPromptOverride != null) {
                jsonObject.put("formatted_prompt", formattedPromptOverride)
            }
            
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