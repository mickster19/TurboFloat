package com.turbofloat.app

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvSelectedGame: TextView
    private var selectedPackage: String = ""
    private var selectedGameName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
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

            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(40, 40, 40, 40)
                setBackgroundColor(0xFF0B111A.toInt())
            }

            val titleTv = TextView(this).apply {
                text = "TURBOFLOAT"
                setTextColor(0xFF00E5FF.toInt())
                textSize = 22f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }

            val gameTv = TextView(this).apply {
                text = "Game: None"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }
            tvSelectedGame = gameTv

            val btnTurnOn = Button(this).apply {
                text = "TURN ON OVERLAY"
                setOnClickListener {
                    try {
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                            return@setOnClickListener
                        }
                        startOverlayService()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val btnTurnOff = Button(this).apply {
                text = "TURN OFF"
                setOnClickListener {
                    try {
                        stopService(Intent(this@MainActivity, FloatingOverlayService::class.java))
                        Toast.makeText(this@MainActivity, "Overlay turned off", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val btnClearRam = Button(this).apply {
                text = "CLEAR RAM"
                setOnClickListener {
                    try {
                        val freedMb = GameBoosterEngine.boostMemoryAndOptimize(this@MainActivity, false)
                        Toast.makeText(this@MainActivity, "Cleared ${freedMb}MB RAM", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Runtime.getRuntime().gc()
                        Toast.makeText(this@MainActivity, "RAM optimized", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val btnSelectGame = Button(this).apply {
                text = "SELECT GAME TO BOOST"
                setOnClickListener {
                    showGamesOnlyDialog()
                }
            }

                    val perfManager = TurboPerformanceManager(this)

        val btnProtectedApps = android.widget.Button(this).apply {
            text = "PROTECTED GAMES (${perfManager.getProtectedApps().size})"
            setOnClickListener {
                showProtectedAppsWithIconsDialog(perfManager) {
                    text = "PROTECTED GAMES (${perfManager.getProtectedApps().size})"
                }
            }
        }

        val btnSharpTouch = android.widget.Button(this).apply {
            text = if (perfManager.isSharpTouchEnabled()) "SHARP TOUCH: ON" else "SHARP TOUCH: OFF"
            setOnClickListener {
                val nextState = !perfManager.isSharpTouchEnabled()
                perfManager.setSharpTouchEnabled(nextState)
                text = if (nextState) "SHARP TOUCH: ON" else "SHARP TOUCH: OFF"
                android.widget.Toast.makeText(
                    this@MainActivity,
                    if (nextState) "Sharp Touch enabled" else "Sharp Touch disabled",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        root.addView(btnProtectedApps)
        root.addView(btnSharpTouch)
        root.addView(titleTv)
            root.addView(gameTv)
            root.addView(btnTurnOn)
            root.addView(btnTurnOff)
            root.addView(btnClearRam)
            root.addView(btnSelectGame)

            setContentView(root)
        } catch (e: Exception) {
            Toast.makeText(this, "Startup error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, FloatingOverlayService::class.java).apply {
            putExtra("TARGET_PACKAGE", selectedPackage)
            putExtra("TARGET_NAME", selectedGameName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Overlay turned on", Toast.LENGTH_SHORT).show()
    }

    private fun showGamesOnlyDialog() {
        try {
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

            val gamesList = resolveInfos
                .mapNotNull { info ->
                    val pkg = info.activityInfo.packageName
                    if (pkg == packageName) return@mapNotNull null
                    val appInfo = try {
                        packageManager.getApplicationInfo(pkg, 0)
                    } catch (_: Exception) {
                        return@mapNotNull null
                    }
                    val isGameCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        appInfo.category == ApplicationInfo.CATEGORY_GAME
                    } else {
                        false
                    }
                    val hasGameFlag = (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                    if (!isGameCategory && !hasGameFlag) return@mapNotNull null

                    val label = info.loadLabel(packageManager).toString()
                    Pair(label, pkg)
                }
                .distinctBy { (_, pkg) -> pkg }
                .sortedBy { (label, _) -> label.lowercase() }

            if (gamesList.isEmpty()) {
                Toast.makeText(this, "No games found", Toast.LENGTH_SHORT).show()
                return
            }

            val names = gamesList.map { (label, _) -> label }.toTypedArray()

            AlertDialog.Builder(this)
                .setTitle("Select Game To Boost")
                .setItems(names) { _, which ->
                    selectedGameName = gamesList[which].first
                    selectedPackage = gamesList[which].second
                    tvSelectedGame.text = "Game: $selectedGameName"
                    if (Settings.canDrawOverlays(this)) {
                        startOverlayService()
                    }
                }
                .setNegativeButton("Close", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Picker error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showProtectedAppsWithIconsDialog(perfManager: TurboPerformanceManager, onSaved: () -> Unit) {
        val apps = perfManager.getInstalledGamesWithIconAndName()
        val selected = perfManager.getProtectedApps()

        val scrollView = android.widget.ScrollView(this).apply {
            setPadding(24, 16, 24, 16)
        }
        val listContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }

        for (app in apps) {
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(8, 12, 8, 12)
            }

            val iconView = android.widget.ImageView(this).apply {
                setImageDrawable(app.icon)
                layoutParams = android.widget.LinearLayout.LayoutParams(88, 88)
            }

            val titleBox = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.WRAP_CONTENT, 1f).apply {
                    marginStart = 20
                }
            }

            val nameTv = android.widget.TextView(this).apply {
                text = app.appName
                textSize = 15f
                setTextColor(android.graphics.Color.WHITE)
            }

            titleBox.addView(nameTv)

            val checkBox = android.widget.CheckBox(this).apply {
                isChecked = selected.contains(app.packageName)
                setOnCheckedChangeListener { _, isChecked ->
                    perfManager.setAppProtected(app.packageName, isChecked)
                }
            }

            row.addView(iconView)
            row.addView(titleBox)
            row.addView(checkBox)
            listContainer.addView(row)
        }

        scrollView.addView(listContainer)

        android.app.AlertDialog.Builder(this)
            .setTitle("Protected Games (${apps.size})")
            .setView(scrollView)
            .setPositiveButton("Done") { _, _ -> onSaved() }
            .setNegativeButton("Dev Options") { _, _ -> perfManager.openDeveloperOptions() }
            .show()
    }

}
