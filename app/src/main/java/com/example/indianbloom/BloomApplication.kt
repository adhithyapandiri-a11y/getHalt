package com.example.indianbloom

import android.app.Application
import android.provider.Settings
import com.example.indianbloom.data.database.AppDatabase
import com.example.indianbloom.data.repository.BloomRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BloomApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { BloomRepository(database) }

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Create default user profile and populate initial app blacklist offline
        applicationScope.launch {
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
            repository.getOrCreateUser(deviceId)

            if (repository.getBlockedApps().isEmpty()) {
                repository.setupDefaultBlocklist()
            }
        }
    }
}
