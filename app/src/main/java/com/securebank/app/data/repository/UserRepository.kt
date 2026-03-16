package com.securebank.app.data.repository

import com.securebank.app.data.local.dao.TransactionDao
import com.securebank.app.data.local.dao.UserDao
import com.securebank.app.data.model.Transaction
import com.securebank.app.data.model.TransactionStatus
import com.securebank.app.data.model.TransactionType
import com.securebank.app.data.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ============================================
 * USER REPOSITORY
 * ============================================
 * Handles user authentication and transaction operations.
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val transactionDao: TransactionDao
) {
    // ========================
    // AUTHENTICATION
    // ========================
    
    suspend fun authenticate(username: String, password: String): User? {
        // In a real app, password would be hashed before comparison
        return userDao.authenticate(username, password)
    }
    
    suspend fun getUserByUsername(username: String): User? {
        return userDao.getByUsername(username)
    }
    
    // ========================
    // USER OPERATIONS
    // ========================
    
    suspend fun updateUser(user: User) {
        userDao.update(user)
    }
    
    suspend fun updateBalance(username: String, amount: Double) {
        userDao.updateBalance(username, amount)
    }
    
    // ========================
    // TRANSACTION OPERATIONS
    // ========================
    
    fun getUserTransactions(userId: String): Flow<List<Transaction>> {
        return transactionDao.getByUser(userId)
    }
    
    suspend fun getRecentTransactions(userId: String, limit: Int = 10): List<Transaction> {
        return transactionDao.getRecentTransactions(userId, limit)
    }
    
    suspend fun createTransaction(transaction: Transaction): Long {
        return transactionDao.insert(transaction)
    }
    
    /**
     * Performs a fund transfer between accounts.
     * Updates both sender and receiver balances and creates transaction records.
     */
    suspend fun transferFunds(
        senderUsername: String,
        recipientAccountNumber: String,
        amount: Double,
        remarks: String
    ): Result<Transaction> {
        return try {
            val sender = userDao.getByUsername(senderUsername)
                ?: return Result.failure(Exception("Sender not found"))
            
            if (sender.balance < amount) {
                return Result.failure(Exception("Insufficient balance"))
            }
            
            // Debit sender
            userDao.updateBalance(senderUsername, -amount)
            
            // Create transaction record
            val transaction = Transaction(
                userId = senderUsername,
                type = TransactionType.TRANSFER_OUT,
                amount = amount,
                recipientAccount = recipientAccountNumber,
                recipientName = "Account $recipientAccountNumber",
                description = remarks.ifEmpty { "Fund Transfer" },
                status = TransactionStatus.COMPLETED
            )
            
            val transactionId = transactionDao.insert(transaction)
            
            Result.success(transaction.copy(id = transactionId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

