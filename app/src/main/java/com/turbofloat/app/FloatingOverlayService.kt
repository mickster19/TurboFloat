package com.turbofloat.appimport android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.absclass FloatingOverlayService : Service() {    private lateinit var windowManager: WindowManager
    private lateinit var eyeBubbleView: View
    private lateinit var overlayPanelView: View    private lateinit var eyeParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams    private var isPanelVisible = false
    private var targetPackage = "com.whatsapp"
    private var targetName = "WhatsApp"
    private var useFreeform = true    // Live FPS & RAM Monitor
    private val handler = Handler(Looper.getMainLooper())
    private var frameCount = 0
    private var lastFpsTime = 0L
    private var currentFps = 60    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            if (lastFpsTime == 0L) {
                lastFpsTime = frameTimeNanos
            } else {
                val diffMs = (frameTimeNanos - lastFpsTime) / 1_000_000L
                if (diffMs >= 1000L) {
                    currentFps = ((frameCount * 1000L) / diffMs).toInt().coerceIn(24, 120)
                    frameCount = 0
                    lastFpsTime = frameTimeNanos
                    updateHudMetrics()
                }
            }
            if (isPanelVisible) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    
    fun bindTargetAppControls(panelView: android.view.View) {
        val prefs = getSharedPreferences("turbofloat_prefs", MODE_PRIVATE)
        val targetPkg = prefs.getString("target_package", null)
        val targetName = prefs.getString("target_app_name", "Hakuna App")
        panelView.findViewById<android.widget.TextView>(R.id.tv_target_app)?.text = targetName
        panelView.findViewById<android.view.View>(R.id.btn_launch_target_app)?.setOnClickListener {
            if (!targetPkg.isNullOrBlank()) {
                val launchIntent = packageManager.getLaunchIntentForPackage(targetPkg)?.apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
            }
        }
    }
}    override fun onBind(intent: Intent?): IBinder? = null    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        setupEyeBubble()
        setupOverlayPanel()
    
}    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            targetPackage = it.getStringExtra("TARGET_PACKAGE") ?: "com.whatsapp"
            targetName = it.getStringExtra("TARGET_NAME") ?: "WhatsApp"
            useFreeform = it.getBooleanExtra("USE_FREEFORM", true)
            updatePanelHeader()
        }
        return START_STICKY
    
}    private fun startForegroundNotification() {
        val channelId = "turbofloat_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TurboFloat Game Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        
}        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TurboFloat Imewashwa")
            .setContentText("Gusa ka-icon ka Jicho kuficha au kuonyesha $targetName")
            .setSmallIcon(R.drawable.ic_eye_open)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()        startForeground(1001, notification)
    
}    private fun getOverlayLayoutType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    
}    /**
     * 1. FLOATING EYE BUBBLE (1-Tap Toggle + Movable)
     * Uses FLAG_NOT_FOCUSABLE so it NEVER steals touches from eFootball!
     */
    private fun setupEyeBubble() {
        eyeBubbleView = LayoutInflater.from(this).inflate(R.layout.overlay_eye_bubble, null)        eyeParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 260
        
}        val ivEyeIcon = eyeBubbleView.findViewById<ImageView>(R.id.ivEyeIcon)
        val tvEyeLabel = eyeBubbleView.findViewById<TextView>(R.id.tvEyeLabel)        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false        eyeBubbleView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = eyeParams.x
                    initialY = eyeParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                        eyeParams.x = initialX + dx
                        eyeParams.y = initialY + dy
                        windowManager.updateViewLayout(eyeBubbleView, eyeParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // 1-TAP TOGGLE: Show / Hide Overlay Panel instantly!
                        toggleOverlayPanel(ivEyeIcon, tvEyeLabel)
                    }
                    true
                }
                else -> false
            }
        
}        windowManager.addView(eyeBubbleView, eyeParams)
    
}    private fun toggleOverlayPanel(ivEyeIcon: ImageView, tvEyeLabel: TextView) {
        isPanelVisible = !isPanelVisible
        if (isPanelVisible) {
            overlayPanelView.visibility = View.VISIBLE
            ivEyeIcon.setImageResource(R.drawable.ic_eye_open)
            tvEyeLabel.text = "Ficha"
            lastFpsTime = 0L
            frameCount = 0
            Choreographer.getInstance().postFrameCallback(frameCallback)
            updateHudMetrics()
        } else {
            overlayPanelView.visibility = View.GONE
            ivEyeIcon.setImageResource(R.drawable.ic_eye_closed)
            tvEyeLabel.text = "Rudisha"
        }
    
}    /**
     * 2. MOVABLE & RESIZABLE OVERLAY PANEL
     * Contains:
     * - Top Drag Handle (Move anywhere)
     * - Real Live RAM & FPS HUD + 1-Tap RAM Booster
     * - Floating App Launcher (Launches WhatsApp in Android Freeform Floating Window over eFootball!)
     * - Opacity Slider & Size Presets (S / M / L) + Corner Resize Handle
     */
    private fun setupOverlayPanel() {
        overlayPanelView = LayoutInflater.from(this).inflate(R.layout.overlay_floating_panel, null)
        overlayPanelView.visibility = View.GONE        val density = resources.displayMetrics.density
        val defaultWidth = (290 * density).toInt()
        val defaultHeight = (330 * density).toInt()        panelParams = WindowManager.LayoutParams(
            defaultWidth,
            defaultHeight,
            getOverlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 120
            y = 180
        
}        val headerDrag = overlayPanelView.findViewById<LinearLayout>(R.id.panelDragHeader)
        val btnHideEye = overlayPanelView.findViewById<Button>(R.id.btnPanelHide)
        val btnBoostHud = overlayPanelView.findViewById<Button>(R.id.btnHudBoost)
        val btnLaunchFloatApp = overlayPanelView.findViewById<Button>(R.id.btnLaunchFloatingApp)
        val btnSizeS = overlayPanelView.findViewById<Button>(R.id.btnSizeS)
        val btnSizeM = overlayPanelView.findViewById<Button>(R.id.btnSizeM)
        val btnSizeL = overlayPanelView.findViewById<Button>(R.id.btnSizeL)
        val seekOpacity = overlayPanelView.findViewById<SeekBar>(R.id.seekPanelOpacity)
        val resizeCorner = overlayPanelView.findViewById<View>(R.id.viewResizeCorner)        val ivEyeIcon = eyeBubbleView.findViewById<ImageView>(R.id.ivEyeIcon)
        val tvEyeLabel = eyeBubbleView.findViewById<TextView>(R.id.tvEyeLabel)        // Hide button inside panel
        btnHideEye.setOnClickListener {
            if (isPanelVisible) toggleOverlayPanel(ivEyeIcon, tvEyeLabel)
        
}        // 1-Tap In-Game RAM Boost
        btnBoostHud.setOnClickListener {
            val freed = GameBoosterEngine.boostMemoryAndOptimize(this, true)
            updateHudMetrics()
            Toast.makeText(this, "Boosted! +${freed}MB RAM huru", Toast.LENGTH_SHORT).show()
        
}        // Launch Selected App (WhatsApp / Telegram / YouTube) as Floating Freeform Window!
        btnLaunchFloatApp.setOnClickListener {
            launchAppInFloatingMode(targetPackage, targetName)
        
}        // Size Presets (S, M, L)
        btnSizeS.setOnClickListener {
            panelParams.width = (230 * density).toInt()
            panelParams.height = (260 * density).toInt()
            windowManager.updateViewLayout(overlayPanelView, panelParams)
        }
        btnSizeM.setOnClickListener {
            panelParams.width = (290 * density).toInt()
            panelParams.height = (330 * density).toInt()
            windowManager.updateViewLayout(overlayPanelView, panelParams)
        }
        btnSizeL.setOnClickListener {
            panelParams.width = (340 * density).toInt()
            panelParams.height = (410 * density).toInt()
            windowManager.updateViewLayout(overlayPanelView, panelParams)
        
}        // Opacity / Transparency Control (See game clearly behind)
        seekOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val alpha = (progress.coerceIn(30, 100)) / 100f
                overlayPanelView.alpha = alpha
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })        // Move Panel by Dragging Header
        var initX = 0
        var initY = 0
        var touchX = 0f
        var touchY = 0f
        headerDrag.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = panelParams.x
                    initY = panelParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    panelParams.x = initX + (event.rawX - touchX).toInt()
                    panelParams.y = initY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(overlayPanelView, panelParams)
                    true
                }
                else -> false
            }
        
}        // Custom Drag-to-Resize Corner Handle
        var initW = 0
        var initH = 0
        var resTouchX = 0f
        var resTouchY = 0f
        val minW = (210 * density).toInt()
        val minH = (230 * density).toInt()
        val maxW = (420 * density).toInt()
        val maxH = (520 * density).toInt()        resizeCorner.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initW = panelParams.width
                    initH = panelParams.height
                    resTouchX = event.rawX
                    resTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newW = (initW + (event.rawX - resTouchX).toInt()).coerceIn(minW, maxW)
                    val newH = (initH + (event.rawY - resTouchY).toInt()).coerceIn(minH, maxH)
                    panelParams.width = newW
                    panelParams.height = newH
                    windowManager.updateViewLayout(overlayPanelView, panelParams)
                    true
                }
                else -> false
            }
        
}        updatePanelHeader()
        windowManager.addView(overlayPanelView, panelParams)
    
}    private fun updatePanelHeader() {
        if (!::overlayPanelView.isInitialized) return
        val tvTitle = overlayPanelView.findViewById<TextView>(R.id.tvPanelTitle)
        val btnLaunch = overlayPanelView.findViewById<Button>(R.id.btnLaunchFloatingApp)
        tvTitle.text = "TURBOFLOAT • $targetName"
        btnLaunch.text = "FUNGUA $targetName (FLOATING)"
    
}    private fun updateHudMetrics() {
        if (!::overlayPanelView.isInitialized || !isPanelVisible) return
        val stats = GameBoosterEngine.getRamStats(this)
        val tvFps = overlayPanelView.findViewById<TextView>(R.id.tvHudFps)
        val tvRam = overlayPanelView.findViewById<TextView>(R.id.tvHudRam)
        tvFps.text = "$currentFps FPS"
        tvRam.text = "${stats.usedPercent}% (${stats.availableMb}MB Free)"
    
}    /**
     * Launches WhatsApp / target app in Android Freeform Floating Window mode
     * so it floats directly over eFootball without closing the game!
     */
    private fun launchAppInFloatingMode(pkg: String, appLabel: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent == null) {
            Toast.makeText(this, "$appLabel haijasakinishwa kwenye simu hii!", Toast.LENGTH_SHORT).show()
            return
        
}        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )        try {
            if (useFreeform && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val options = ActivityOptions.makeBasic()
                // Set Freeform windowing mode (WINDOWING_MODE_FREEFORM = 5) via reflection for broad OEM support
                try {
                    val setWindowingMode = ActivityOptions::class.java.getMethod(
                        "setLaunchWindowingMode",
                        Int::class.javaPrimitiveType
                    )
                    setWindowingMode.invoke(options, 5)
                } catch (_: Exception) {
                
}                val density = resources.displayMetrics.density
                val bounds = Rect(
                    (40 * density).toInt(),
                    (120 * density).toInt(),
                    (340 * density).toInt(),
                    (520 * density).toInt()
                )
                options.launchBounds = bounds
                startActivity(launchIntent, options.toBundle())
            } else {
                startActivity(launchIntent)
            }
        } catch (e: Exception) {
            startActivity(launchIntent)
        }
    
}    override fun onDestroy() {
        super.onDestroy()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        if (::eyeBubbleView.isInitialized) windowManager.removeView(eyeBubbleView)
        if (::overlayPanelView.isInitialized) windowManager.removeView(overlayPanelView)
    }

}
