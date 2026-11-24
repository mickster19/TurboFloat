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
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var eyeView: ImageView? = null
    private var topBoxView: LinearLayout? = null
    private var appContainer: FrameLayout? = null
    private var isEyeHidden: Boolean = false
    private var isBoxVisible: Boolean = false

    private var targetPackage: String = ""
    private var targetName: String = ""
    private var useFreeform: Boolean = false
    private var enableDnd: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            targetPackage = intent.getStringExtra("TARGET_PACKAGE") ?: ""
            targetName = intent.getStringExtra("TARGET_NAME") ?: "App"
            useFreeform = intent.getBooleanExtra("USE_FREEFORM", false)
            enableDnd = intent.getBooleanExtra("ENABLE_DND", false)
        }

        startForegroundNotification()
        showEyeBubble()
        buildTopBox()

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "turbofloat_overlay_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TurboFloat Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TurboFloat Active")
            .setContentText("Overlay box & speed boost tayari ($targetName)")
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
        val sizePx = (46 * resources.displayMetrics.density).toInt()

        val bubble = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_view)
            setColorFilter(Color.WHITE)
            setPadding(20, 20, 20, 20)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE60D1B2A.toInt())
                setStroke(3, 0xFF00E5FF.toInt())
            }
            alpha = 0.9f
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
                        toggleEyeAndBox()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(bubble, params)
        eyeView = bubble
    }

    private fun toggleEyeAndBox() {
        isEyeHidden = !isEyeHidden
        if (isEyeHidden) {
            isBoxVisible = false
            topBoxView?.visibility = View.GONE
            eyeView?.alpha = 0.35f
            Toast.makeText(this, "Icon ya jicho na kabox vimefichwa", Toast.LENGTH_SHORT).show()
        } else {
            eyeView?.alpha = 0.95f
            toggleTopBox()
        }
    }

    private fun buildTopBox() {
        if (topBoxView != null) return

        val density = resources.displayMetrics.density
        val boxWidthPx = (320 * density).toInt()
        val boxHeightPx = (240 * density).toInt()

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(0xF2101820.toInt())
                setStroke(3, 0xFF00E5FF.toInt())
            }
            visibility = View.GONE
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleTv = TextView(this).apply {
            text = if (targetName.isNotEmpty()) targetName else "Overlay Box"
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val speedBtn = Button(this).apply {
            text = "🚀"
            textSize = 14f
            setOnClickListener {
                val freedMb = GameBoosterEngine.boostMemoryAndOptimize(this@FloatingOverlayService, enableDnd)
                Toast.makeText(
                    this@FloatingOverlayService,
                    "Speed Up 🚀: RAM imesafishwa (+${freedMb}MB)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val openInnerBtn = Button(this).apply {
            text = "Run"
            textSize = 12f
            setOnClickListener {
                runTargetInsideBox()
            }
        }

        val closeBoxBtn = Button(this).apply {
            text = "✕"
            textSize = 12f
            setOnClickListener {
                isBoxVisible = false
                topBoxView?.visibility = View.GONE
            }
        }

        headerRow.addView(titleTv)
        headerRow.addView(speedBtn)
        headerRow.addView(openInnerBtn)
        headerRow.addView(closeBoxBtn)

        val container = FrameLayout(this).apply {
            setBackgroundColor(0xFF080C10.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                boxHeightPx
            ).apply {
                topMargin = 12
            }
        }

        val statusTv = TextView(this).apply {
            text = "Gusa 'Run' kufungua ${if (targetName.isNotEmpty()) targetName else "app uliyochagua"} hapa ndani au gusa 🚀 ku-boost."
            setTextColor(0xFFB0BEC5.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }
        container.addView(statusTv)

        box.addView(headerRow)
        box.addView(container)

        val params = WindowManager.LayoutParams(
            boxWidthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayLayoutFlag(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 70
        }

        windowManager?.addView(box, params)
        topBoxView = box
        appContainer = container
    }

    private fun toggleTopBox() {
        isBoxVisible = !isBoxVisible
        if (isBoxVisible) {
            topBoxView?.visibility = View.VISIBLE
            runTargetInsideBox()
        } else {
            topBoxView?.visibility = View.GONE
        }
    }

    private fun runTargetInsideBox() {
        if (targetPackage.isEmpty()) {
            Toast.makeText(this, "Chagua app kwanza kwenye list ya TurboFloat", Toast.LENGTH_SHORT).show()
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && useFreeform) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            }
            startActivity(launchIntent)
            appContainer?.removeAllViews()
            val runningTv = TextView(this).apply {
                text = "Inarun: $targetName ($targetPackage)\nBonyeza 🚀 juu ku-speed up muda wowote."
                setTextColor(0xFF00E5FF.toInt())
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
            }
            appContainer?.addView(runningTv)
        } else {
            Toast.makeText(this, "App ($targetName) haikupatikana", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        topBoxView?.let { view ->
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
        topBoxView = null
        eyeView = null
        super.onDestroy()
    }
}
