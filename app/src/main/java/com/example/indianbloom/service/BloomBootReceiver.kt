package com.example.indianbloom.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.indianbloom.BloomApplication
import com.example.indianbloom.data.database.FocusState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BloomBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? BloomApplication ?: return
            val repository = app.repository

            // Launch in IO/Default dispatcher thread
            CoroutineScope(Dispatchers.Default).launch {
                val activeSession = repository.getActiveSession()
                if (activeSession != null && activeSession.state == FocusState.ACTIVE) {
                    // Restart background service to block apps
                    BloomForegroundService.startService(context)
                }
            }
        }
    }
}
