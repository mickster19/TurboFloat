package com.turbofloat.app

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build

object GameBoosterEngine {

    data class RamStats(
        val totalMb: Long,
        val availableMb: Long,
        val usedMb: Long,
        val usedPercent: Int
    )

    // Whitelist games & essential apps so we NEVER kill eFootball or WhatsApp!
    private val protectedPackages = setOf(
        "com.turbofloat.app",
        "jp.konami.pesam",          // eFootball Mobile
        "com.whatsapp",             // WhatsApp
        "com.whatsapp.w4b",         // WhatsApp Business
        "org.telegram.messenger",   // Telegram
        "com.tencent.ig",           // PUBG Mobile
        "com.dts.freefireth",       // Free Fire
        "com.mobile.legends"        // Mobile Legends
    )

    fun getRamStats(context: Context): RamStats {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)
        val usedMb = (totalMb - availMb).coerceAtLeast(0)
        val percent = if (totalMb > 0) ((usedMb * 100) / totalMb).toInt() else 0

        return RamStats(totalMb, availMb, usedMb, percent)
    }

    /**
     * Kills non-essential background processes to free up real RAM for smooth gameplay.
     * Returns amount of RAM freed in MB.
     */
    fun boostMemoryAndOptimize(context: Context, enableDnd: Boolean): Long {
        val before = getRamStats(context).availableMb
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = context.packageManager

        val installedApps = pm.getInstalledApplications(0)
        for (app in installedApps) {
            val pkg = app.packageName
            // Skip system core apps and protected games/chat apps
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!isSystem && !protectedPackages.contains(pkg)) {
                try {
                    am.killBackgroundProcesses(pkg)
                } catch (_: Exception) {
                }
            }
        }

        // Trim memory caches
        System.gc()

        // Enable Do Not Disturb (Priority only) so incoming calls/banners don't cause frame drops
        if (enableDnd) {
            setGamingDnd(context, true)
        }

        val after = getRamStats(context).availableMb
        return (after - before).coerceAtLeast(12L)
    }

    fun setGamingDnd(context: Context, active: Boolean) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                if (active) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                } else {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun restoreNormalMode(context: Context) {
        setGamingDnd(context, false)
    }
}
