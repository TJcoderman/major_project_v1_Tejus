package com.securebank.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.securebank.app.data.local.dao.*
import com.securebank.app.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ============================================
 * SECURE BANK DATABASE
 * ============================================
 * Room database for storing all app data including
 * behavioral data, user information, and transactions.
 */
@Database(
    entities = [
        User::class,
        Transaction::class,
        KeystrokeData::class,
        TouchData::class,
        MotionData::class,
        BehavioralSession::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(
    TouchTypeConverter::class
)
abstract class SecureBankDatabase : RoomDatabase() {
    
    // DAOs
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun keystrokeDao(): KeystrokeDao
    abstract fun touchDao(): TouchDao
    abstract fun motionDao(): MotionDao
    abstract fun behavioralSessionDao(): BehavioralSessionDao
    
    companion object {
        private const val DATABASE_NAME = "securebank_db"
        
        @Volatile
        private var INSTANCE: SecureBankDatabase? = null
        
        fun getInstance(context: Context): SecureBankDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): SecureBankDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SecureBankDatabase::class.java,
                DATABASE_NAME
            )
            .addCallback(DatabaseCallback())
            .fallbackToDestructiveMigration()
            .build()
        }
    }
    
    /**
     * Callback to seed the database with initial mock data.
     */
    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedDatabase(database)
                }
            }
        }
        
        private suspend fun seedDatabase(database: SecureBankDatabase) {
            // Seed mock users
            val mockUsers = listOf(
                User(
                    username = "demo",
                    passwordHash = "demo123", // In real app, this would be hashed
                    fullName = "Demo User",
                    accountNumber = "1234567890",
                    balance = 50000.0
                ),
                User(
                    username = "john",
                    passwordHash = "john123",
                    fullName = "John Doe",
                    accountNumber = "9876543210",
                    balance = 75000.0
                ),
                User(
                    username = "jane",
                    passwordHash = "jane123",
                    fullName = "Jane Smith",
                    accountNumber = "5555555555",
                    balance = 120000.0
                )
            )
            database.userDao().insertAll(mockUsers)
            
            // Seed mock transactions for demo user
            val mockTransactions = listOf(
                Transaction(
                    userId = "demo",
                    type = TransactionType.CREDIT,
                    amount = 5000.0,
                    description = "Salary Credit",
                    timestamp = System.currentTimeMillis() - 86400000 * 5
                ),
                Transaction(
                    userId = "demo",
                    type = TransactionType.DEBIT,
                    amount = 1200.0,
                    description = "Amazon Purchase",
                    timestamp = System.currentTimeMillis() - 86400000 * 4
                ),
                Transaction(
                    userId = "demo",
                    type = TransactionType.TRANSFER_OUT,
                    amount = 2500.0,
                    recipientAccount = "9876543210",
                    recipientName = "John Doe",
                    description = "Rent Payment",
                    timestamp = System.currentTimeMillis() - 86400000 * 3
                ),
                Transaction(
                    userId = "demo",
                    type = TransactionType.BILL_PAYMENT,
                    amount = 850.0,
                    description = "Electricity Bill",
                    timestamp = System.currentTimeMillis() - 86400000 * 2
                ),
                Transaction(
                    userId = "demo",
                    type = TransactionType.CREDIT,
                    amount = 15000.0,
                    description = "Freelance Payment",
                    timestamp = System.currentTimeMillis() - 86400000
                ),
                Transaction(
                    userId = "demo",
                    type = TransactionType.DEBIT,
                    amount = 450.0,
                    description = "Grocery Shopping",
                    timestamp = System.currentTimeMillis() - 3600000
                )
            )
            database.transactionDao().insertAll(mockTransactions)
        }
    }
}

