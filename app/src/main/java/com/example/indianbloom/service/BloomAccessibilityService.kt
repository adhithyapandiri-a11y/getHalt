package com.example.indianbloom.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.indianbloom.BloomApplication
import com.example.indianbloom.data.database.FocusState
import com.example.indianbloom.ui.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BloomAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Access repository via Application context
            val app = application as? BloomApplication ?: return
            val repository = app.repository

            serviceScope.launch {
                // A. Check Strict Mode / Anti-Uninstall first (independent of focus session)
                val isStrictModeEnabled = getSharedPreferences("auren_prefs", MODE_PRIVATE)
                    .getBoolean("strict_mode", false)

                if (isStrictModeEnabled) {
                    val isInstallerOrSettings = packageName.contains("packageinstaller") ||
                            packageName == "com.android.settings" ||
                            packageName.contains("installer")

                    if (isInstallerOrSettings) {
                        val eventText = event.text.map { it.toString().lowercase() }
                        val contentDesc = event.contentDescription?.toString()?.lowercase() ?: ""

                        val targetsAuren = eventText.any { it.contains("auren") || it.contains("bloom") || it.contains("indian bloom") } ||
                                contentDesc.contains("auren") || contentDesc.contains("bloom") || contentDesc.contains("indian bloom")

                        val isAction = eventText.any {
                            it.contains("uninstall") || it.contains("force stop") ||
                            it.contains("clear data") || it.contains("deactivate") ||
                            it.contains("disable") || it.contains("delete") ||
                            it.contains("remove") || it.contains("clear storage") ||
                            it.contains("stop") || it.contains("turn off") ||
                            it.contains("clear cache") || it.contains("storage")
                        } || contentDesc.contains("uninstall") || contentDesc.contains("force stop") ||
                        contentDesc.contains("clear data") || contentDesc.contains("deactivate") ||
                        contentDesc.contains("stop") || contentDesc.contains("turn off") ||
                        contentDesc.contains("disable")

                        if (targetsAuren && isAction) {
                            // Intercept immediately!
                            val intent = Intent(this@BloomAccessibilityService, BlockerActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("blocked_package", packageName)
                                putExtra("session_id", -1L)
                                putExtra("anti_uninstall", true)
                            }
                            startActivity(intent)
                            return@launch
                        }
                    }
                }

                // B. Focus Session Blocking
                val activeSession = repository.getActiveSession()
                if (activeSession != null && activeSession.state == FocusState.ACTIVE) {
                    
                    // 1. Check if the app opened is in the blocked list
                    val isBlocked = repository.isAppBlocked(packageName)
                    
                    // 2. Prevent bypass via Settings -> Bloom -> Force Stop / Uninstall
                    val isSettingsBypass = packageName == "com.android.settings" && 
                            isTargetingBloomSettings(event)

                    if (isBlocked || isSettingsBypass) {
                        // Increment block count
                        repository.incrementBlockCount(activeSession.id)

                        // Launch full-screen blocker UI
                        val intent = Intent(this@BloomAccessibilityService, BlockerActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("blocked_package", packageName)
                            putExtra("session_id", activeSession.id)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun isTargetingBloomSettings(event: AccessibilityEvent): Boolean {
        // Inspect window content / text to see if user is looking at Auren/Bloom settings page
        val className = event.className?.toString() ?: ""
        val textList = event.text.map { it.toString().lowercase() }
        
        // If the screen contains "auren", "bloom" or "indian bloom" and is inside settings, intercept it
        return textList.any { it.contains("auren") || it.contains("bloom") || it.contains("indian bloom") }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
