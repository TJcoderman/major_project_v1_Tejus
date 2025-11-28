package com.securebank.app.di

import android.content.Context
import com.securebank.app.data.local.SecureBankDatabase
import com.securebank.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ============================================
 * HILT APP MODULE
 * ============================================
 * Provides application-wide dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // ========================
    // DATABASE
    // ========================
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SecureBankDatabase {
        return SecureBankDatabase.getInstance(context)
    }
    
    // ========================
    // DAOs
    // ========================
    
    @Provides
    @Singleton
    fun provideUserDao(database: SecureBankDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    @Singleton
    fun provideTransactionDao(database: SecureBankDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    @Singleton
    fun provideKeystrokeDao(database: SecureBankDatabase): KeystrokeDao {
        return database.keystrokeDao()
    }
    
    @Provides
    @Singleton
    fun provideTouchDao(database: SecureBankDatabase): TouchDao {
        return database.touchDao()
    }
    
    @Provides
    @Singleton
    fun provideMotionDao(database: SecureBankDatabase): MotionDao {
        return database.motionDao()
    }
    
    @Provides
    @Singleton
    fun provideBehavioralSessionDao(database: SecureBankDatabase): BehavioralSessionDao {
        return database.behavioralSessionDao()
    }
}

