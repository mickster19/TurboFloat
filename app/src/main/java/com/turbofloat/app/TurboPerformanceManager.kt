package com.turbofloat.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)

class TurboPerformanceManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("turbofloat_perf_prefs", Context.MODE_PRIVATE)

    fun getProtectedApps(): MutableSet<String> {
        return prefs.getStringSet("protected_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
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

    fun getInstalledLaunchableApps(): List<InstalledAppItem> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val seenPackages = HashSet<String>()
        val result = ArrayList<InstalledAppItem>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || !seenPackages.add(pkg)) continue
            val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
            val icon = info.loadIcon(pm)
            result.add(InstalledAppItem(label, pkg, icon))
        }

        return result.sortedBy { it.appName.lowercase() }
    }

    fun openDeveloperOptions() {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
