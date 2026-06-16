package com.example.indianbloom.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.indianbloom.BloomApplication
import com.example.indianbloom.data.database.FocusState
import com.example.indianbloom.ui.BlockerActivity
import kotlinx.coroutines.*
import java.util.*

class BloomForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var pollingJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "bloom_focus_channel"
        private const val NOTIFICATION_ID = 8808

        fun startService(context: Context) {
            val intent = Intent(context, BloomForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BloomForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Build and display sticky foreground notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start fallback polling loop
        startFallbackMonitor()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startFallbackMonitor() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            val app = application as? BloomApplication ?: return@launch
            val repository = app.repository
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            while (isActive) {
                val activeSession = repository.getActiveSession()
                if (activeSession == null || activeSession.state != FocusState.ACTIVE) {
                    // Session ended, stop service
                    stopSelf()
                    break
                }

                // If accessibility service is NOT running, run the UsageStats polling fallback
                if (!isAccessibilityServiceEnabled()) {
                    val foregroundPackage = getForegroundPackageName(usageStatsManager)
                    if (foregroundPackage != null && repository.isAppBlocked(foregroundPackage)) {
                        repository.incrementBlockCount(activeSession.id)
                        
                        // Launch blocker activity
                        val blockerIntent = Intent(this@BloomForegroundService, BlockerActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("blocked_package", foregroundPackage)
                            putExtra("session_id", activeSession.id)
                        }
                        startActivity(blockerIntent)
                    }
                }

                delay(500) // Poll every 500ms
            }
        }
    }

    private fun getForegroundPackageName(usageStatsManager: UsageStatsManager): String? {
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 10,
            time
        )
        if (stats != null && stats.isNotEmpty()) {
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            return sortedStats[0].packageName
        }
        return null
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "${packageName}/${BloomAccessibilityService::class.java.name}"
        val enabledServicesSetting = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServicesSetting.split(':').any {
            it.equals(expectedComponentName, ignoreCase = true)
        }
    }

    private fun createNotification(): Notification {
        val title = "Auren Active"
        val message = "Your distractions are locked in your Auren card."

        // Clicking notification opens the dashboard to tap/unlock
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val notificationIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auren Focus Notification",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Auren focus blocker active in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
