package com.securebank.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ============================================
 * USER MODEL
 * ============================================
 * Represents a user in the mock banking system.
 * For this demo, we use hardcoded users.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val username: String,
    val passwordHash: String,        // In real app, this would be properly hashed
    val fullName: String,
    val accountNumber: String,
    val balance: Double = 50000.0,   // Mock balance
    val pin: String = "",            // 6-digit PIN for behavioral enrollment
    val enrollmentComplete: Boolean = false, // True after guided behavioral enrollment
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * ============================================
 * TRANSACTION MODEL
 * ============================================
 * Represents a banking transaction (mock data).
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val type: TransactionType,
    val amount: Double,
    val recipientAccount: String? = null,
    val recipientName: String? = null,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: TransactionStatus = TransactionStatus.COMPLETED
)

enum class TransactionType {
    CREDIT,
    DEBIT,
    TRANSFER_IN,
    TRANSFER_OUT,
    BILL_PAYMENT
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * ============================================
 * AUTH STATE
 * ============================================
 * Represents the current authentication state.
 */
data class AuthState(
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val sessionId: String? = null,
    val loginTimestamp: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ============================================
 * LOGIN CREDENTIALS
 * ============================================
 * Data class for login form input.
 */
data class LoginCredentials(
    val username: String,
    val password: String
)

