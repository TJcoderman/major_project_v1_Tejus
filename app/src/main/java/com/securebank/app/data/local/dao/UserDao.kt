package com.securebank.app.data.local.dao

import androidx.room.*
import com.securebank.app.data.model.Transaction
import com.securebank.app.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * ============================================
 * USER DAO
 * ============================================
 */
@Dao
interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>)
    
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getByUsername(username: String): User?
    
    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :passwordHash")
    suspend fun authenticate(username: String, passwordHash: String): User?
    
    @Update
    suspend fun update(user: User)
    
    @Query("UPDATE users SET balance = balance + :amount WHERE username = :username")
    suspend fun updateBalance(username: String, amount: Double)
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
}

/**
 * ============================================
 * TRANSACTION DAO
 * ============================================
 */
@Dao
interface TransactionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)
    
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getByUser(userId: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTransactions(userId: String, limit: Int): List<Transaction>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'CREDIT'")
    suspend fun getTotalCredits(userId: String): Double?
    
    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'DEBIT'")
    suspend fun getTotalDebits(userId: String): Double?
    
    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}

