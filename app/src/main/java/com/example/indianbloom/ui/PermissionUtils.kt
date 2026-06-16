package com.example.indianbloom.ui

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import com.example.indianbloom.service.BloomAccessibilityService

object PermissionUtils {

    /**
     * Checks if the System Alert Window (Overlay / Draw Over Other Apps) permission is granted.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Checks if the Usage Statistics (Usage Access) permission is granted.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Checks if the Bloom Accessibility Service is active and bound.
     */
    fun hasAccessibilityPermission(context: Context): Boolean {
        val expectedComponentName = "${context.packageName}/${BloomAccessibilityService::class.java.name}"
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServicesSetting.split(':').any {
            it.equals(expectedComponentName, ignoreCase = true)
        }
    }

    /**
     * Checks if all mandatory permissions are granted.
     */
    fun hasAllMandatoryPermissions(context: Context): Boolean {
        return hasOverlayPermission(context) && hasUsageStatsPermission(context)
    }
}
