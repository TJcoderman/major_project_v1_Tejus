package com.securebank.app.data.local.dao

import androidx.room.*
import com.securebank.app.data.model.BehavioralProfile

/**
 * ============================================
 * BEHAVIORAL PROFILE DAO
 * ============================================
 * CRUD operations for per-user behavioral enrollment profiles.
 */
@Dao
interface BehavioralProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: BehavioralProfile)

    @Update
    suspend fun update(profile: BehavioralProfile)

    @Query("SELECT * FROM behavioral_profiles WHERE username = :username")
    suspend fun getByUsername(username: String): BehavioralProfile?

    @Query("DELETE FROM behavioral_profiles WHERE username = :username")
    suspend fun deleteByUsername(username: String)
}
