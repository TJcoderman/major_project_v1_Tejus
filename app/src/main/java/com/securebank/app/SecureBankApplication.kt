package com.securebank.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * SecureBank Application class.
 * 
 * This is the entry point for Hilt dependency injection.
 * The @HiltAndroidApp annotation triggers Hilt's code generation.
 */
@HiltAndroidApp
class SecureBankApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Application-level initialization can be added here
    }
}

