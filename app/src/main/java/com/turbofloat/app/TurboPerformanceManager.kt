package com.turbofloat.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
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

    private fun isLikelyGame(appInfo: ApplicationInfo, label: String, pkg: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo.category == ApplicationInfo.CATEGORY_GAME) {
            return true
        }
        @Suppress("DEPRECATION")
        if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
            return true
        }
        val hay = "${label.lowercase()} ${pkg.lowercase()}"
        val gameHints = listOf(
            "pes", "efootball", "konami", "pubg", "freefire", "dts.", "cod", "callofduty",
            "bus", "bussid", "simulator", "truck", "racing", "game", "craft", "survival",
            "battle", "royale", "subway", "asphalt", "fifa", "fc2", "dreamleague", "dls"
        )
        return gameHints.any { hay.contains(it) }
    }

    fun getInstalledGamesWithIconAndName(): List<InstalledAppItem> {
        val pm = context.packageManager
        val appInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }

        val seenPackages = HashSet<String>()
        val result = ArrayList<InstalledAppItem>()

        for (appInfo in appInfos) {
            val pkg = appInfo.packageName ?: continue
            if (pkg == context.packageName || !seenPackages.add(pkg)) continue

            val label = pm.getApplicationLabel(appInfo)?.toString()?.trim().orEmpty().ifEmpty { pkg }
            if (!isLikelyGame(appInfo, label, pkg)) continue

            val icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (_: Exception) {
                pm.defaultActivityIcon
            }
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
