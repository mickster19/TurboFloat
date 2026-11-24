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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var eyeView: ImageView? = null
    private var isOverlayVisible: Boolean = true
    private var targetPackage: String = ""
    private var targetName: String = ""
    private var useFreeform: Boolean = false
    private var enableDnd: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private var frameCount = 0
    private var lastFrameTimeNanos = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastFrameTimeNanos == 0L) {
                lastFrameTimeNanos = frameTimeNanos
            }
            frameCount++
            val elapsedNanos = frameTimeNanos - lastFrameTimeNanos
            if (elapsedNanos >= 1_000_000_000L) {
                frameCount = 0
                lastFrameTimeNanos = frameTimeNanos
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

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
        startFpsMonitor()

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
            .setContentText("Overlay & FPS monitor inaendelea ($targetName)")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun showEyeBubble() {
        if (eyeView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val bubbleSizePx = (44 * resources.displayMetrics.density).toInt()
        val bubble = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_view)
            setColorFilter(Color.WHITE)
            setPadding(20, 20, 20, 20)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xCC0D1B2A.toInt())
                setStroke(3, 0xFF00E5FF.toInt())
            }
            alpha = 0.85f
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 260
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
                        toggleOverlayAction()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(bubble, params)
        eyeView = bubble
        isOverlayVisible = true
    }

    private fun toggleOverlayAction() {
        isOverlayVisible = !isOverlayVisible
        eyeView?.alpha = if (isOverlayVisible) 0.85f else 0.4f

        if (isOverlayVisible) {
            launchTargetApp()
        } else {
            Toast.makeText(
                this,
                "Overlay imefichwa (touch pass-through active)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun launchTargetApp() {
        if (targetPackage.isEmpty()) {
            Toast.makeText(
                this,
                "Hakuna app iliyochaguliwa kwenye list",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            Toast.makeText(
                this,
                "App ($targetName) haijapatikana kwenye kifaa",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startFpsMonitor() {
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopFpsMonitor() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        stopFpsMonitor()
        eyeView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
            }
        }
        eyeView = null
        super.onDestroy()
    }
}
