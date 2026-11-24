package com.turbofloat.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
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
import android.widget.HorizontalScrollView
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
    private var appCardContainer: FrameLayout? = null
    private var selectedGameTv: TextView? = null

    private var isMenuOpen: Boolean = false
    private var isDndActive: Boolean = false

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
        refreshGameCard()

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
            .setContentText("Game Booster ready${if (targetName.isNotEmpty()) " • $targetName" else ""}")
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
            refreshGameCard()
        }
    }

    private fun buildFrontMenuBox() {
        if (menuBoxView != null) return

        val density = resources.displayMetrics.density
        val boxWidthPx = (340 * density).toInt()
        val boxMaxHeightPx = (310 * density).toInt()

        val outerBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
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
            text = "GAME BOOSTER MENU"
            setTextColor(0xFF00E5FF.toInt())
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val speedBtn = Button(this).apply {
            text = "🚀"
            textSize = 13f
            minWidth = 0
            minimumWidth = 0
            setOnClickListener {
                val freedMb = GameBoosterEngine.boostMemoryAndOptimize(this@FloatingOverlayService, isDndActive)
                Toast.makeText(
                    this@FloatingOverlayService,
                    "Speed Up 🚀: Freed ${freedMb}MB RAM for ${if (targetName.isNotEmpty()) targetName else "Game"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val closeBtn = Button(this).apply {
            text = "✕"
            textSize = 12f
            minWidth = 0
            minimumWidth = 0
            setOnClickListener {
                isMenuOpen = false
                menuBoxView?.visibility = View.GONE
            }
        }

        headerRow.addView(headerTitle)
        headerRow.addView(speedBtn)
        headerRow.addView(closeBtn)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                boxMaxHeightPx
            ).apply {
                topMargin = 10
            }
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val gameLabel = TextView(this).apply {
            text = "Selected Game (Anti-Lag):"
            setTextColor(0xFFB0BEC5.toInt())
            textSize = 11f
        }

        val gameTv = TextView(this).apply {
            text = if (targetName.isNotEmpty()) "$targetName ($targetPackage)" else "None selected — tap to pick game"
            setTextColor(Color.WHITE)
            textSize = 12f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 4, 0, 8)
            setOnClickListener {
                launchGamePickerActivity()
            }
        }
        selectedGameTv = gameTv

        val pickGameBtn = Button(this).apply {
            text = "SELECT GAME TO BOOST"
            textSize = 11f
            setOnClickListener {
                launchGamePickerActivity()
            }
        }

        val cardFrame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(0xFF080C12.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (115 * density).toInt()
            ).apply {
                topMargin = 8
            }
        }
        appCardContainer = cardFrame

        val quickToolsScroll = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
            }
        }

        val quickToolsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val dndBtn = Button(this).apply {
            text = if (isDndActive) "DND: ON" else "DND"
            textSize = 11f
            minWidth = 0
            minimumWidth = 0
            setOnClickListener {
                toggleDndMode(this)
            }
        }

        val lteOnlyBtn = Button(this).apply {
            text = "LTE ONLY (4G)"
            textSize = 11f
            minWidth = 0
            minimumWidth = 0
            setOnClickListener {
                openLteOnlySettings()
            }
        }

        val antiLagBtn = Button(this).apply {
            text = "ANTI-LAG & RUN"
            textSize = 11f
            minWidth = 0
            minimumWidth = 0
            setOnClickListener {
                runAntiLagAndLaunchGame()
            }
        }

        quickToolsRow.addView(dndBtn)
        quickToolsRow.addView(lteOnlyBtn)
        quickToolsRow.addView(antiLagBtn)
        quickToolsScroll.addView(quickToolsRow)

        contentLayout.addView(gameLabel)
        contentLayout.addView(gameTv)
        contentLayout.addView(pickGameBtn)
        contentLayout.addView(cardFrame)
        contentLayout.addView(quickToolsScroll)

        scrollView.addView(contentLayout)
        outerBox.addView(headerRow)
        outerBox.addView(scrollView)

        val params = WindowManager.LayoutParams(
            boxWidthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayLayoutFlag(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 75
        }

        windowManager?.addView(outerBox, params)
        menuBoxView = outerBox
    }

    private fun refreshGameCard() {
        selectedGameTv?.text = if (targetName.isNotEmpty()) {
            "$targetName ($targetPackage)"
        } else {
            "None selected — tap to pick game"
        }

        val container = appCardContainer ?: return
        container.removeAllViews()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 12, 14, 12)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val iconSize = (52 * resources.displayMetrics.density).toInt()
        val iconView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            scaleType = ImageView.ScaleType.FIT_CENTER
            if (targetPackage.isNotEmpty()) {
                try {
                    setImageDrawable(packageManager.getApplicationIcon(targetPackage))
                } catch (_: Exception) {
                    setImageResource(android.R.drawable.sym_def_app_icon)
                }
            } else {
                setImageResource(android.R.drawable.sym_def_app_icon)
            }
        }

        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = 12
            }
        }

        val titleTv = TextView(this).apply {
            text = if (targetName.isNotEmpty()) targetName else "No Game Selected"
            setTextColor(Color.WHITE)
            textSize = 14f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }

        val subTv = TextView(this).apply {
            text = if (targetPackage.isNotEmpty()) {
                "Anti-Lag & Boost Ready • Tap 🚀 or ANTI-LAG & RUN"
            } else {
                "Select a game to enable anti-lag boost"
            }
            setTextColor(0xFF00E5FF.toInt())
            textSize = 11f
            maxLines = 2
        }

        infoCol.addView(titleTv)
        infoCol.addView(subTv)

        row.addView(iconView)
        row.addView(infoCol)
        container.addView(row)
    }

    private fun launchGamePickerActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("OPEN_APP_PICKER", true)
        }
        startActivity(intent)
    }

    private fun toggleDndMode(btn: Button) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Grant Do Not Disturb access first", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(
            this,
            if (isDndActive) "Do Not Disturb enabled" else "Do Not Disturb disabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openLteOnlySettings() {
        val candidates = listOf(
            Intent("android.settings.DATA_ROAMING_SETTINGS"),
            Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS),
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                Toast.makeText(this, "Select LTE / 4G Only in network settings", Toast.LENGTH_SHORT).show()
                return
            }
        }
        Toast.makeText(this, "Unable to open network settings", Toast.LENGTH_SHORT).show()
    }

    private fun runAntiLagAndLaunchGame() {
        val freedMb = GameBoosterEngine.boostMemoryAndOptimize(this, isDndActive)
        Toast.makeText(
            this,
            "Anti-Lag Active: Freed ${freedMb}MB RAM",
            Toast.LENGTH_SHORT
        ).show()

        if (targetPackage.isEmpty()) {
            launchGamePickerActivity()
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
            Toast.makeText(this, "Game not found on device", Toast.LENGTH_SHORT).show()
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
