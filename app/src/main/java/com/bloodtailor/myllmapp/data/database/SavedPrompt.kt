package com.bloodtailor.myllmapp.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity for saved prompts
 */
@Entity(tableName = "saved_prompts")
data class SavedPrompt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val content: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

/**
 * Data class for creating new saved prompts (without ID)
 */
data class SavedPromptCreate(
    val name: String,
    val content: String
)