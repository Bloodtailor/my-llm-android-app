package com.bloodtailor.myllmapp

import android.app.Application
import com.bloodtailor.myllmapp.data.LlmRepository

/**
 * Application class for managing application-wide state and initialization
 */
class LlmApplication : Application() {

    // Application-wide repository instance
    lateinit var repository: LlmRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize repository with application context
        repository = LlmRepository(applicationContext, "")

        // Log that application has been initialized
        android.util.Log.d("LlmApplication", "Application initialized")
    }

    /**
     * Get the repository instance from anywhere in the app
     */
    companion object {
        fun getRepository(application: Application): LlmRepository {
            return (application as LlmApplication).repository
        }
    }
}