package com.turbofloat.appimport android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompatclass MainActivity : AppCompatActivity() {    private lateinit var tvOverlayStatus: TextView
    private lateinit var tvUsageStatus: TextView
    private lateinit var tvDndStatus: TextView
    private lateinit var tvSelectedApp: TextView
    private lateinit var tvRamInfo: TextView    private var selectedPackage: String = "com.whatsapp"
    private var selectedAppName: String = "WhatsApp"    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)        tvOverlayStatus = findViewById(R.id.tvOverlayStatus)
        tvUsageStatus = findViewById(R.id.tvUsageStatus)
        tvDndStatus = findViewById(R.id.tvDndStatus)
        tvSelectedApp = findViewById(R.id.tvSelectedApp)
        tvRamInfo = findViewById(R.id.tvRamInfo)        val btnPermOverlay = findViewById<Button>(R.id.btnPermOverlay)
        val btnPermUsage = findViewById<Button>(R.id.btnPermUsage)
        val btnPermDnd = findViewById<Button>(R.id.btnPermDnd)
        val btnStartOverlay = findViewById<Button>(R.id.btnStartOverlay)
        val btnStopOverlay = findViewById<Button>(R.id.btnStopOverlay)
        val btnBoostNow = findViewById<Button>(R.id.btnBoostNow)
        val rgApps = findViewById<RadioGroup>(R.id.rgTargetApps)
        val switchDnd = findViewById<Switch>(R.id.switchDnd)
        val switchFreeform = findViewById<Switch>(R.id.switchFreeform)        // Request Notification permission for Android 13+
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
        
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}        // 1. Overlay Permission Button
        btnPermOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Ruhusa ya Overlay tayari imewashwa!", Toast.LENGTH_SHORT).show()
            }
        
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}        // 2. Usage Access Permission (Needed for Real RAM & Background App Optimization)
        btnPermUsage.setOnClickListener {
            if (!hasUsageStatsPermission()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } else {
                Toast.makeText(this, "Ruhusa ya Usage Access tayari ipo!", Toast.LENGTH_SHORT).show()
            }
        
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}        // 3. Do Not Disturb Access (Stops calls/notifications from lagging game)
        btnPermDnd.setOnClickListener {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.isNotificationPolicyAccessGranted) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            } else {
                Toast.makeText(this, "Ruhusa ya Gaming DND tayari ipo!", Toast.LENGTH_SHORT).show()
            }
        
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}        // App Selection RadioGroup
        rgApps.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbWhatsapp -> {
                    selectedPackage = "com.whatsapp"
                    selectedAppName = "WhatsApp"
                }
                R.id.rbTelegram -> {
                    selectedPackage = "org.telegram.messenger"
                    selectedAppName = "Telegram"
                }
                R.id.rbYoutube -> {
                    selectedPackage = "com.google.android.youtube"
                    selectedAppName = "YouTube"
                }
                R.id.rbChrome -> {
                    selectedPackage = "com.android.chrome"
                    selectedAppName = "Browser / Web"
                }
            }
            tvSelectedApp.text = "App Iliyochaguliwa: $selectedAppName ($selectedPackage)"
        
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}        // Real RAM Boost Button
        btnBoostNow.setOnClickListener {
            val freedMb = GameBoosterEngine.boostMemoryAndOptimize(this, switchDnd.isChecked)
            updateRamDisplay()
            Toast.makeText(
                this,
                "RAM Imesafishwa! Imeokoa ${freedMb}MB kwa ajili ya Game!",
                Toast.LENGTH_LONG
            ).show()
        
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}        // Start Floating Eye Bubble + HUD Service
        btnStartOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    this,
                    "Tafadhali washa ruhusa ya 'Display over other apps' kwanza!",
                    Toast.LENGTH_LONG
                ).show()
                btnPermOverlay.performClick()
                return@setOnClickListener
            
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}            // Auto-boost before launching overlay
            GameBoosterEngine.boostMemoryAndOptimize(this, switchDnd.isChecked)            val serviceIntent = Intent(this, FloatingOverlayService::class.java).apply {
                putExtra("TARGET_PACKAGE", selectedPackage)
                putExtra("TARGET_NAME", selectedAppName)
                putExtra("USE_FREEFORM", switchFreeform.isChecked)
                putExtra("ENABLE_DND", switchDnd.isChecked)
            
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}            Toast.makeText(
                this,
                "Ka-icon ka Jicho kamewashwa! Ingia kwenye eFootball sasa.",
                Toast.LENGTH_LONG
            ).show()
        
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}        // Stop Service
        btnStopOverlay.setOnClickListener {
            stopService(Intent(this, FloatingOverlayService::class.java))
            GameBoosterEngine.restoreNormalMode(this)
            Toast.makeText(this, "TurboFloat imezimwa.", Toast.LENGTH_SHORT).show()
        }
    
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}    override fun onResume() {
        super.onResume()
        refreshPermissionBadges()
        updateRamDisplay()
    
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}    private fun refreshPermissionBadges() {
        val overlayOk = Settings.canDrawOverlays(this)
        tvOverlayStatus.text = if (overlayOk) "IMEWASHWA ✓" else "HAIJAWASHWA ✗"
        tvOverlayStatus.setTextColor(if (overlayOk) 0xFF00E676.toInt() else 0xFFFF5252.toInt())        val usageOk = hasUsageStatsPermission()
        tvUsageStatus.text = if (usageOk) "IMEWASHWA ✓" else "HAIJAWASHWA ✗"
        tvUsageStatus.setTextColor(if (usageOk) 0xFF00E676.toInt() else 0xFFFF5252.toInt())        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val dndOk = nm.isNotificationPolicyAccessGranted
        tvDndStatus.text = if (dndOk) "IMEWASHWA ✓" else "HIARI (DND)"
        tvDndStatus.setTextColor(if (dndOk) 0xFF00E676.toInt() else 0xFFFFAB40.toInt())
    
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}    private fun updateRamDisplay() {
        val stats = GameBoosterEngine.getRamStats(this)
        tvRamInfo.text = "RAM Inayotumika: ${stats.usedMb} MB / ${stats.totalMb} MB (${stats.usedPercent}%)"
    
    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}    private fun hasUsageStatsPermission(): Boolean {
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

    fun showAppPickerDialog() {
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = resolveInfos.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chagua App ya Floating")
            .setItems(labels) { _, which ->
                val chosen = resolveInfos[which]
                val pkgName = chosen.activityInfo.packageName
                val appLabel = chosen.loadLabel(packageManager).toString()
                getSharedPreferences("turbofloat_prefs", MODE_PRIVATE).edit()
                    .putString("target_package", pkgName)
                    .putString("target_app_name", appLabel)
                    .apply()
            }
            .show()
    }
}