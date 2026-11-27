package com.turbofloat.app

import android.content.Context
import android.content.Intent
import android.provider.Settings

class TurboPerformanceManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("turbofloat_perf_prefs", Context.MODE_PRIVATE)

    fun getProtectedApps(): MutableSet<String> {
        return prefs.getStringSet("protected_apps", setOf("jp.konami.pesam", "com.whatsapp"))?.toMutableSet()
            ?: mutableSetOf("jp.konami.pesam", "com.whatsapp")
    }

    fun setAppProtected(packageName: String, protected: Boolean) {
        val updated = getProtectedApps()
        if (protected) updated.add(packageName) else updated.remove(packageName)
        prefs.edit().putStringSet("protected_apps", updated).apply()
    }

    fun isAppProtected(packageName: String): Boolean {
        return getProtectedApps().contains(packageName)
    }

    fun isSharpTouchEnabled(): Boolean {
        return prefs.getBoolean("sharp_touch_enabled", false)
    }

    fun setSharpTouchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sharp_touch_enabled", enabled).apply()
    }

    fun launchProtectedApp(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(launchIntent)
        return true
    }

    fun openDeveloperOptions() {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
