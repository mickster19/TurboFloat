package com.turbofloat.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var eyeView: ImageView? = null
    private var menuBoxView: LinearLayout? = null
    private var menuBoxParams: WindowManager.LayoutParams? = null
    private var boxScrollView: ScrollView? = null
    private var gameGridContainer: LinearLayout? = null
    private var selectedGameBanner: TextView? = null
    private var selectedGameIcon: ImageView? = null

    private var isMenuOpen: Boolean = false
    private var isGamePickerVisible: Boolean = false
    private var isDndActive: Boolean = false
    private var boxWidthDp: Int = 340
    private var boxHeightDp: Int = 220

    private var targetPackage: String = ""
    private var targetName: String = ""
    private var useFreeform: Boolean = false
    private var enableDnd: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            targetPackage = intent.getStringExtra("TARGET_PACKAGE") ?: targetPackage
            targetName = intent.getStringExtra("TARGET_NAME") ?: targetName
            useFreeform = intent.getBooleanExtra("USE_FREEFORM", useFreeform)
            enableDnd = intent.getBooleanExtra("ENABLE_DND", enableDnd)
            isDndActive = enableDnd
        }

        startForegroundNotification()
        showEyeBubble()
        buildFrontMenuBox()
        refreshSelectedGameDisplay()

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "turbofloat_game_booster_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TurboFloat Game Booster",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("device connected with TurboFloat")
            .setContentText(if (targetName.isNotEmpty()) "Target: $targetName" else "Game Booster active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun overlayLayoutFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun showEyeBubble() {
        if (eyeView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val sizePx = (48 * resources.displayMetrics.density).toInt()

        val bubble = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_view)
            setColorFilter(Color.WHITE)
            setPadding(20, 20, 20, 20)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xF00B1320.toInt())
                setStroke(3, 0xFF00E5FF.toInt())
            }
            alpha = 0.92f
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            overlayLayoutFlag(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 220
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        bubble.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                        isMoving = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(v, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoving) {
                        toggleFrontMenuBox()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(bubble, params)
        eyeView = bubble
    }

    private fun toggleFrontMenuBox() {
        isMenuOpen = !isMenuOpen
        menuBoxView?.visibility = if (isMenuOpen) View.VISIBLE else View.GONE
        eyeView?.alpha = if (isMenuOpen) 1.0f else 0.9f
        if (isMenuOpen) {
            refreshSelectedGameDisplay()
        }
    }

    private fun applyBoxDimensions() {
        val params = menuBoxParams ?: return
        val density = resources.displayMetrics.density
        params.width = (boxWidthDp * density).toInt()
        boxScrollView?.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (boxHeightDp * density).toInt()
        ).apply {
            topMargin = 8
        }
        windowManager?.updateViewLayout(menuBoxView, params)
    }

    private fun buildFrontMenuBox() {
        if (menuBoxView != null) return

        val density = resources.displayMetrics.density

        val outerBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 14, 14, 14)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 22f
                setColor(0xF50E1621.toInt())
                setStroke(3, 0xFF00E5FF.toInt())
            }
            visibility = View.GONE
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val headerTitle = TextView(this).apply {
            text = "TURBO BOX"
            setTextColor(0xFF00E5FF.toInt())
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val shrinkBtn = Button(this).apply {
            text = "-"
            textSize = 12f
            minWidth = 0
            minimumWidth = 0
            setOnClickListener {
                boxWidthDp = (boxWidthDp - 30).coerceAtLeast(250)
                boxHeightDp = (boxHeightDp - 35).coerceAtLeast(150)
                applyBoxDimensions()
            }
        }

        val expandBtn = Button(this).apply {
            text = "+"
            textSize = 12f
            minWidth = 0
            minimumWidth = 0
            setOnClickListener {
                boxWidthDp = (boxWidthDp + 30).coerceAtMost(440)
                boxHeightDp = (boxHeightDp + 35).coerceAtMost(380)
                applyBoxDimensions()
            }
        }

        val closeBtn = Button(this).apply {
            text = "X"
            textSize = 12f
            minWidth = 0
            minimumWidth = 0
            setOnClickListener {
                isMenuOpen = false
                menuBoxView?.visibility = View.GONE
            }
        }

        headerRow.addView(headerTitle)
        headerRow.addView(shrinkBtn)
        headerRow.addView(expandBtn)
        headerRow.addView(closeBtn)

        val params = WindowManager.LayoutParams(
            (boxWidthDp * density).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayLayoutFlag(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 85
        }
        menuBoxParams = params

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        headerRow.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(outerBox, params)
                    true
                }
                else -> false
            }
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (boxHeightDp * density).toInt()
            ).apply {
                topMargin = 8
            }
        }
        boxScrollView = scrollView

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val activeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 8)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f
                setColor(0xFF080C12.toInt())
            }
        }

        val iconSize = (42 * density).toInt()
        val gameIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(android.R.drawable.sym_def_app_icon)
        }
        selectedGameIcon = gameIcon

        val gameLabel = TextView(this).apply {
            text = "No game selected"
            setTextColor(Color.WHITE)
            textSize = 12f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = 10
            }
        }
        selectedGameBanner = gameLabel

        activeRow.addView(gameIcon)
        activeRow.addView(gameLabel)

        val chooseGameBtn = Button(this).apply {
            text = "CHOOSE GAME TO BOOST"
            textSize = 11f
            setOnClickListener {
                toggleGamesGridInOverlay()
            }
        }

        val gamesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 6, 0, 6)
        }
        gameGridContainer = gamesContainer

        val dndBtn = Button(this).apply {
            text = if (isDndActive) "DND: ON" else "DND"
            textSize = 11f
            setOnClickListener {
                toggleDnd(this)
            }
        }

        val speedUpBtn = Button(this).apply {
            text = "SPEED UP 🚀"
            textSize = 11f
            setOnClickListener {
                val freedMb = GameBoosterEngine.boostMemoryAndOptimize(this@FloatingOverlayService, isDndActive)
                Toast.makeText(
                    this@FloatingOverlayService,
                    "Speed Up 🚀: Freed ${freedMb}MB RAM",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val antiLagBtn = Button(this).apply {
            text = "ANTI-LAG"
            textSize = 11f
            setOnClickListener {
                val freedMb = GameBoosterEngine.boostMemoryAndOptimize(this@FloatingOverlayService, true)
                Toast.makeText(
                    this@FloatingOverlayService,
                    "Anti-Lag enabled (${freedMb}MB freed)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val runBtn = Button(this).apply {
            text = "RUN"
            textSize = 11f
            setOnClickListener {
                launchSelectedGame()
            }
        }

        contentLayout.addView(activeRow)
        contentLayout.addView(chooseGameBtn)
        contentLayout.addView(gamesContainer)
        contentLayout.addView(dndBtn)
        contentLayout.addView(speedUpBtn)
        contentLayout.addView(antiLagBtn)
        contentLayout.addView(runBtn)

        scrollView.addView(contentLayout)
        outerBox.addView(headerRow)
        outerBox.addView(scrollView)

        windowManager?.addView(outerBox, params)
        menuBoxView = outerBox
    }

    private fun refreshSelectedGameDisplay() {
        selectedGameBanner?.text = if (targetName.isNotEmpty()) targetName else "No game selected"
        if (targetPackage.isNotEmpty()) {
            try {
                selectedGameIcon?.setImageDrawable(packageManager.getApplicationIcon(targetPackage))
            } catch (_: Exception) {
                selectedGameIcon?.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        } else {
            selectedGameIcon?.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    private fun getInstalledGamesOnly(): List<Triple<String, String, android.graphics.drawable.Drawable>> {
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

        return resolveInfos
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
                val icon = info.loadIcon(packageManager)
                Triple(label, pkg, icon)
            }
            .distinctBy { (_, pkg, _) -> pkg }
            .sortedBy { (label, _, _) -> label.lowercase() }
    }

    private fun toggleGamesGridInOverlay() {
        val container = gameGridContainer ?: return
        isGamePickerVisible = !isGamePickerVisible
        if (!isGamePickerVisible) {
            container.visibility = View.GONE
            return
        }

        container.removeAllViews()
        val games = getInstalledGamesOnly()
        if (games.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No games detected on device"
                setTextColor(0xFFB0BEC5.toInt())
                textSize = 11f
                setPadding(6, 6, 6, 6)
            }
            container.addView(emptyTv)
            container.visibility = View.VISIBLE
            return
        }

        val density = resources.displayMetrics.density
        val iconPx = (36 * density).toInt()

        for ((label, pkg, icon) in games) {
            val itemRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 6, 8, 6)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 12f
                    setColor(if (pkg == targetPackage) 0xFF16283A.toInt() else 0xFF0B111A.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 6
                }
                setOnClickListener {
                    targetName = label
                    targetPackage = pkg
                    refreshSelectedGameDisplay()
                    startForegroundNotification()
                    isGamePickerVisible = false
                    container.visibility = View.GONE
                    Toast.makeText(
                        this@FloatingOverlayService,
                        "Selected game: $label",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val iconView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(iconPx, iconPx)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageDrawable(icon)
            }

            val titleView = TextView(this).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 12f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = 10
                }
            }

            itemRow.addView(iconView)
            itemRow.addView(titleView)
            container.addView(itemRow)
        }

        container.visibility = View.VISIBLE
    }

    private fun toggleDnd(btn: Button) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Grant Do Not Disturb access", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            return
        }

        isDndActive = !isDndActive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nm.setInterruptionFilter(
                if (isDndActive) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                else NotificationManager.INTERRUPTION_FILTER_ALL
            )
        }
        btn.text = if (isDndActive) "DND: ON" else "DND"
    }

    private fun launchSelectedGame() {
        if (targetPackage.isEmpty()) {
            toggleGamesGridInOverlay()
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && useFreeform) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            }
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Game not found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        menuBoxView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
            }
        }
        eyeView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
            }
        }
        menuBoxView = null
        eyeView = null
        super.onDestroy()
    }
}
