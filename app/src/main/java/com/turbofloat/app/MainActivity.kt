package com.turbofloat.app

import android.Manifest
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvOverlayStatus: TextView
    private lateinit var tvUsageStatus: TextView
    private lateinit var tvDndStatus: TextView
    private lateinit var tvSelectedApp: TextView
    private lateinit var tvRamInfo: TextView

    private var selectedPackage: String = ""
    private var selectedAppName: String = "None selected"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvOverlayStatus = findViewById(R.id.tvOverlayStatus)
        tvUsageStatus = findViewById(R.id.tvUsageStatus)
        tvDndStatus = findViewById(R.id.tvDndStatus)
        tvSelectedApp = findViewById(R.id.tvSelectedApp)
        tvRamInfo = findViewById(R.id.tvRamInfo)

        val btnPermOverlay = findViewById<Button>(R.id.btnPermOverlay)
        val btnPermUsage = findViewById<Button>(R.id.btnPermUsage)
        val btnPermDnd = findViewById<Button>(R.id.btnPermDnd)
        val btnStartOverlay = findViewById<Button>(R.id.btnStartOverlay)
        val btnStopOverlay = findViewById<Button>(R.id.btnStopOverlay)
        val btnBoostNow = findViewById<Button>(R.id.btnBoostNow)
        val switchDnd = findViewById<Switch>(R.id.switchDnd)
        val switchFreeform = findViewById<Switch>(R.id.switchFreeform)

        tvSelectedApp.text = "Selected Game: $selectedAppName"
        tvSelectedApp.setOnClickListener {
            showInstalledAppsDialog(switchDnd.isChecked, switchFreeform.isChecked)
        }

        val pickGameButton = Button(this).apply {
            text = "SELECT GAME FROM LIST"
            setOnClickListener {
                showInstalledAppsDialog(switchDnd.isChecked, switchFreeform.isChecked)
            }
        }
        (tvSelectedApp.parent as? android.view.ViewGroup)?.let { parent ->
            val idx = parent.indexOfChild(tvSelectedApp)
            if (idx >= 0) {
                parent.addView(pickGameButton, idx + 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        btnPermOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission already enabled", Toast.LENGTH_SHORT).show()
            }
        }

        btnPermUsage.setOnClickListener {
            if (!hasUsageStatsPermission()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } else {
                Toast.makeText(this, "Usage access already enabled", Toast.LENGTH_SHORT).show()
            }
        }

        btnPermDnd.setOnClickListener {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.isNotificationPolicyAccessGranted) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            } else {
                Toast.makeText(this, "Do Not Disturb access already enabled", Toast.LENGTH_SHORT).show()
            }
        }

        btnBoostNow.setOnClickListener {
            val freedMb = GameBoosterEngine.boostMemoryAndOptimize(this, switchDnd.isChecked)
            updateRamDisplay()
            Toast.makeText(
                this,
                "Speed Up 🚀: Freed ${freedMb}MB RAM",
                Toast.LENGTH_LONG
            ).show()
        }

        btnStartOverlay.setOnClickListener {
            if (selectedPackage.isEmpty()) {
                Toast.makeText(this, "Select a game from the list first", Toast.LENGTH_SHORT).show()
                showInstalledAppsDialog(switchDnd.isChecked, switchFreeform.isChecked)
                return@setOnClickListener
            }

            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Enable 'Display over other apps' first", Toast.LENGTH_LONG).show()
                btnPermOverlay.performClick()
                return@setOnClickListener
            }

            startBoosterService(switchDnd.isChecked, switchFreeform.isChecked)
            Toast.makeText(this, "Game Booster enabled for $selectedAppName", Toast.LENGTH_SHORT).show()
        }

        btnStopOverlay.setOnClickListener {
            stopService(Intent(this, FloatingOverlayService::class.java))
            GameBoosterEngine.restoreNormalMode(this)
            Toast.makeText(this, "Game Booster stopped", Toast.LENGTH_SHORT).show()
        }

        if (intent?.getBooleanExtra("OPEN_APP_PICKER", false) == true) {
            showInstalledAppsDialog(switchDnd.isChecked, switchFreeform.isChecked)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.getBooleanExtra("OPEN_APP_PICKER", false) == true) {
            val switchDnd = findViewById<Switch>(R.id.switchDnd)
            val switchFreeform = findViewById<Switch>(R.id.switchFreeform)
            showInstalledAppsDialog(switchDnd.isChecked, switchFreeform.isChecked)
        }
    }

    private fun startBoosterService(enableDnd: Boolean, useFreeform: Boolean) {
        GameBoosterEngine.boostMemoryAndOptimize(this, enableDnd)
        val serviceIntent = Intent(this, FloatingOverlayService::class.java).apply {
            putExtra("TARGET_PACKAGE", selectedPackage)
            putExtra("TARGET_NAME", selectedAppName)
            putExtra("USE_FREEFORM", useFreeform)
            putExtra("ENABLE_DND", enableDnd)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun showInstalledAppsDialog(enableDnd: Boolean, useFreeform: Boolean) {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(mainIntent, 0)
        }

        val appsList = resolveInfos
            .map { info ->
                val label = info.loadLabel(packageManager).toString()
                val pkg = info.activityInfo.packageName
                Pair(label, pkg)
            }
            .filter { (_, pkg) -> pkg != packageName }
            .distinctBy { (_, pkg) -> pkg }
            .sortedBy { (label, _) -> label.lowercase() }

        if (appsList.isEmpty()) {
            Toast.makeText(this, "No apps found", Toast.LENGTH_SHORT).show()
            return
        }

        val displayNames = appsList.map { (label, pkg) -> "$label ($pkg)" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Game / App")
            .setItems(displayNames) { _, which ->
                val chosen = appsList[which]
                selectedAppName = chosen.first
                selectedPackage = chosen.second
                tvSelectedApp.text = "Selected Game: $selectedAppName ($selectedPackage)"
                if (Settings.canDrawOverlays(this)) {
                    startBoosterService(enableDnd, useFreeform)
                }
                Toast.makeText(this, "Selected: $selectedAppName", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionBadges()
        updateRamDisplay()
    }

    private fun refreshPermissionBadges() {
        val overlayOk = Settings.canDrawOverlays(this)
        tvOverlayStatus.text = if (overlayOk) "ENABLED ✓" else "DISABLED ✗"
        tvOverlayStatus.setTextColor(if (overlayOk) 0xFF00E676.toInt() else 0xFFFF5252.toInt())

        val usageOk = hasUsageStatsPermission()
        tvUsageStatus.text = if (usageOk) "ENABLED ✓" else "DISABLED ✗"
        tvUsageStatus.setTextColor(if (usageOk) 0xFF00E676.toInt() else 0xFFFF5252.toInt())

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val dndOk = nm.isNotificationPolicyAccessGranted
        tvDndStatus.text = if (dndOk) "ENABLED ✓" else "OPTIONAL (DND)"
        tvDndStatus.setTextColor(if (dndOk) 0xFF00E676.toInt() else 0xFFFFAB40.toInt())
    }

    private fun updateRamDisplay() {
        val stats = GameBoosterEngine.getRamStats(this)
        tvRamInfo.text = "RAM Usage: ${stats.usedMb} MB / ${stats.totalMb} MB (${stats.usedPercent}%)"
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
