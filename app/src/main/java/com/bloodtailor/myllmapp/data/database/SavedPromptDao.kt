package com.bloodtailor.myllmapp.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SavedPrompt operations
 */
@Dao
interface SavedPromptDao {

    /**
     * Get all saved prompts ordered by update time (most recent first)
     */
    @Query("SELECT * FROM saved_prompts ORDER BY updatedAt DESC")
    fun getAllPrompts(): Flow<List<SavedPrompt>>

    /**
     * Get a specific prompt by ID
     */
    @Query("SELECT * FROM saved_prompts WHERE id = :id")
    suspend fun getPromptById(id: Long): SavedPrompt?

    /**
     * Insert a new prompt
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: SavedPrompt): Long

    /**
     * Update an existing prompt
     */
    @Update
    suspend fun updatePrompt(prompt: SavedPrompt)

    /**
     * Delete specific prompts by ID
     */
    @Query("DELETE FROM saved_prompts WHERE id IN (:ids)")
    suspend fun deletePrompts(ids: List<Long>)

    /**
     * Delete a single prompt
     */
    @Delete
    suspend fun deletePrompt(prompt: SavedPrompt)

    /**
     * Search prompts by name or content
     */
    @Query("SELECT * FROM saved_prompts WHERE name LIKE :search OR content LIKE :search ORDER BY updatedAt DESC")
    fun searchPrompts(search: String): Flow<List<SavedPrompt>>

    /**
     * Get count of saved prompts
     */
    @Query("SELECT COUNT(*) FROM saved_prompts")
    suspend fun getPromptCount(): Int
}