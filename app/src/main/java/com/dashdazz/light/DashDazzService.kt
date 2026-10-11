package com.dashdazz.light

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.sin

data class DashDazzState(
    val isRunning: Boolean = false,
    val mode: Int = DashDazzService.MODE_RAINBOW,
    val color: Int = Color.RED,
    val speedMs: Long = 25L,
    val brightness: Float = 1.0f,
    val screenMode: Int = DashDazzService.SCREEN_ON_ONLY,
    val blinkOnNotification: Boolean = false,
    val isScreenPaused: Boolean = false,
    val palette: Int = Palettes.SPECTRUM,
    val dualStyle: Int = DashDazzService.DUAL_HARDWARE_SPLIT,
    val splitColorA: Int = Color.RED,
    val splitColorB: Int = Color.BLUE,
    val spinColorA: Int = Color.RED,
    val spinColorB: Int = Color.BLUE,
    val duoHueOffset: Int = 90,
    val notifMode: Int = 0,
    val notifActivateDuration: Int = 5,

    val audioReactive: Boolean = false
)

class DashDazzService : Service() {

    companion object {
        private const val TAG = "DashDazzService"

        const val ACTION_START = "com.dashdazz.light.action.START"
        const val ACTION_STOP = "com.dashdazz.light.action.STOP"
        const val ACTION_UPDATE = "com.dashdazz.light.action.UPDATE"
        const val ACTION_PULSE = "com.dashdazz.light.action.PULSE"
        const val ACTION_ACTIVATE = "com.dashdazz.light.action.ACTIVATE"

        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_COLOR = "extra_color"
        const val EXTRA_SPEED_MS = "extra_speed_ms"
        const val EXTRA_BRIGHTNESS = "extra_brightness"
        const val EXTRA_SCREEN_MODE = "extra_screen_mode"
        const val EXTRA_BLINK_NOTIFICATION = "extra_blink_notification"
        const val EXTRA_PALETTE = "extra_palette"
        const val EXTRA_DUAL_STYLE = "extra_dual_style"
        const val EXTRA_SPLIT_COLOR_A = "extra_split_color_a"
        const val EXTRA_SPLIT_COLOR_B = "extra_split_color_b"
        const val EXTRA_SPIN_COLOR_A = "extra_spin_color_a"
        const val EXTRA_SPIN_COLOR_B = "extra_spin_color_b"
        const val EXTRA_DUO_HUE_OFFSET = "extra_duo_hue_offset"
        const val EXTRA_NOTIF_MODE = "extra_notif_mode"
        const val EXTRA_NOTIF_ACTIVATE_DURATION = "extra_notif_activate_duration"
        const val EXTRA_ACTIVATE_DURATION = "extra_activate_duration"

        const val MODE_RAINBOW = 0
        const val MODE_BREATHE = 1
        const val MODE_SOLID = 2
        const val MODE_DUAL = 3

        const val MODE_AUDIO_REACTIVE = 4

        const val SCREEN_ON_ONLY = 0
        const val SCREEN_OFF_ONLY = 1
        const val SCREEN_ALWAYS = 2
        const val SCREEN_NOTIF_ONLY = 3

        const val DUAL_HARDWARE_SPLIT = 0
        const val DUAL_SPIN = 1
        const val DUAL_POLICE = 2

        const val DUAL_ALTERNATE = 3

        const val DUAL_FIRE_ICE_LEGACY = 4
        const val DUAL_RAINBOW_DUO = 5

        const val DUAL_BEAT_RAINBOW = 6
        const val DUAL_SPLIT_BEAT = 7

        val AUDIO_DUAL_STYLES = intArrayOf(DUAL_BEAT_RAINBOW, DUAL_SPLIT_BEAT)

        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "dashdazz_channel"

        private val _stateFlow = MutableStateFlow(DashDazzState())
        val stateFlow: StateFlow<DashDazzState> = _stateFlow.asStateFlow()

        @Volatile
        private var activeService: DashDazzService? = null

        val isRunning: Boolean
            get() = _stateFlow.value.isRunning

        val currentMode: Int
            get() = _stateFlow.value.mode

        val currentColor: Int
            get() = _stateFlow.value.color

        val currentSpeedMs: Long
            get() = _stateFlow.value.speedMs

        val currentBrightness: Float
            get() = _stateFlow.value.brightness

        val currentDuoHueOffset: Int
            get() = _stateFlow.value.duoHueOffset

        fun start(
            context: Context,
            mode: Int = _stateFlow.value.mode,
            color: Int = _stateFlow.value.color,
            speedMs: Long = _stateFlow.value.speedMs,
            brightness: Float = _stateFlow.value.brightness,
            screenMode: Int = _stateFlow.value.screenMode,
            blinkOnNotification: Boolean = _stateFlow.value.blinkOnNotification,
            palette: Int = _stateFlow.value.palette,
            dualStyle: Int = _stateFlow.value.dualStyle,
            splitColorA: Int = _stateFlow.value.splitColorA,
            splitColorB: Int = _stateFlow.value.splitColorB,
            spinColorA: Int = _stateFlow.value.spinColorA,
            spinColorB: Int = _stateFlow.value.spinColorB,
            duoHueOffset: Int = _stateFlow.value.duoHueOffset,
            notifMode: Int = _stateFlow.value.notifMode,
            notifActivateDuration: Int = _stateFlow.value.notifActivateDuration
        ) {
            val intent = Intent(context, DashDazzService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_COLOR, color)
                putExtra(EXTRA_SPEED_MS, speedMs)
                putExtra(EXTRA_BRIGHTNESS, brightness)
                putExtra(EXTRA_SCREEN_MODE, screenMode)
                putExtra(EXTRA_BLINK_NOTIFICATION, blinkOnNotification)
                putExtra(EXTRA_PALETTE, palette)
                putExtra(EXTRA_DUAL_STYLE, dualStyle)
                putExtra(EXTRA_SPLIT_COLOR_A, splitColorA)
                putExtra(EXTRA_SPLIT_COLOR_B, splitColorB)
                putExtra(EXTRA_SPIN_COLOR_A, spinColorA)
                putExtra(EXTRA_SPIN_COLOR_B, spinColorB)
                putExtra(EXTRA_DUO_HUE_OFFSET, duoHueOffset)
                putExtra(EXTRA_NOTIF_MODE, notifMode)
                putExtra(EXTRA_NOTIF_ACTIVATE_DURATION, notifActivateDuration)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DashDazzService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateSettings(
            context: Context,
            mode: Int? = null,
            color: Int? = null,
            speedMs: Long? = null,
            brightness: Float? = null,
            screenMode: Int? = null,
            blinkOnNotification: Boolean? = null,
            palette: Int? = null,
            dualStyle: Int? = null,
            splitColorA: Int? = null,
            splitColorB: Int? = null,
            spinColorA: Int? = null,
            spinColorB: Int? = null,
            duoHueOffset: Int? = null,
            notifMode: Int? = null,
            notifActivateDuration: Int? = null,
            audioReactive: Boolean? = null
        ) {
            val current = _stateFlow.value
            val newMode = mode ?: current.mode
            val newColor = color ?: current.color
            val newSpeed = speedMs ?: current.speedMs
            val newBrightness = brightness ?: current.brightness
            val newScreenMode = screenMode ?: current.screenMode
            val newBlink = blinkOnNotification ?: current.blinkOnNotification
            val newPalette = palette ?: current.palette
            val newDualStyle = dualStyle ?: current.dualStyle
            val newSplitColorA = splitColorA ?: current.splitColorA
            val newSplitColorB = splitColorB ?: current.splitColorB
            val newSpinColorA = spinColorA ?: current.spinColorA
            val newSpinColorB = spinColorB ?: current.spinColorB
            val newDuoHueOffset = duoHueOffset ?: current.duoHueOffset
            val newNotifMode = notifMode ?: current.notifMode
            val newNotifActivateDuration = notifActivateDuration ?: current.notifActivateDuration
            val newAudioReactive = audioReactive ?: current.audioReactive

            Prefs.setMode(context, newMode)
            Prefs.setColor(context, newColor)
            Prefs.setSpeedMs(context, newSpeed)
            Prefs.setBrightness(context, newBrightness)
            Prefs.setScreenMode(context, newScreenMode)
            Prefs.setBlinkOnNotification(context, newBlink)
            Prefs.setPalette(context, newPalette)
            Prefs.setDualStyle(context, newDualStyle)
            Prefs.setSplitColorA(context, newSplitColorA)
            Prefs.setSplitColorB(context, newSplitColorB)
            Prefs.setSpinColorA(context, newSpinColorA)
            Prefs.setSpinColorB(context, newSpinColorB)
            Prefs.setDuoHueOffset(context, newDuoHueOffset)
            Prefs.setNotificationMode(context, newNotifMode)
            Prefs.setNotificationActivateDuration(context, newNotifActivateDuration)
            Prefs.setAudioReactive(context, newAudioReactive)

            val modeChanged = (newMode != current.mode)
            val dualStyleChanged = (newDualStyle != current.dualStyle)
            val audioChanged = (newAudioReactive != current.audioReactive)
            val splitColorsChanged = (newSplitColorA != current.splitColorA || newSplitColorB != current.splitColorB)

            _stateFlow.update {
                it.copy(
                    mode = newMode,
                    color = newColor,
                    speedMs = newSpeed,
                    brightness = newBrightness,
                    screenMode = newScreenMode,
                    blinkOnNotification = newBlink,
                    palette = newPalette,
                    dualStyle = newDualStyle,
                    splitColorA = newSplitColorA,
                    splitColorB = newSplitColorB,
                    spinColorA = newSpinColorA,
                    spinColorB = newSpinColorB,
                    duoHueOffset = newDuoHueOffset,
                    notifMode = newNotifMode,
                    notifActivateDuration = newNotifActivateDuration,
                    audioReactive = newAudioReactive
                )
            }

            if (current.isRunning && newMode == MODE_DUAL && newDualStyle == DUAL_HARDWARE_SPLIT && splitColorsChanged) {
                if (LightsBridge.isRootAvailable()) {
                    LightsBridge.setDualSpotColors(newSplitColorA, newSplitColorB, full = false)
                }
            }

            if (current.isRunning &&
                (modeChanged || audioChanged || (newMode == MODE_DUAL && dualStyleChanged))
            ) {
                if (newScreenMode == SCREEN_NOTIF_ONLY) {

                    _stateFlow.update { it.copy(isScreenPaused = true) }
                    activeService?.pauseAnimationForScreenOff()
                } else {
                    activeService?.launchAnimationLoop()
                }
            } else if (current.isRunning && newScreenMode != current.screenMode) {
                if (newScreenMode == SCREEN_NOTIF_ONLY) {

                    _stateFlow.update { it.copy(isScreenPaused = true) }
                    activeService?.pauseAnimationForScreenOff()
                } else {
                    activeService?.evaluateScreenPolicy()
                }
            }
        }

        fun restoreStateFromPrefs(context: Context) {

            val migratedMode = Prefs.needsAudioModifierMigration(context)
            if (migratedMode != null) {
                Prefs.setMode(context, migratedMode)
                Prefs.setAudioReactive(context, true)
            }

            if (Prefs.getDualStyle(context, -1) == DUAL_FIRE_ICE_LEGACY) {
                Prefs.setDualStyle(context, DUAL_ALTERNATE)
            }

            val saved = Prefs.loadState(context)
            _stateFlow.update { current ->
                current.copy(
                    mode = saved.mode,
                    color = saved.color,
                    speedMs = saved.speedMs,
                    brightness = saved.brightness,
                    screenMode = saved.screenMode,
                    blinkOnNotification = saved.blinkOnNotification,
                    palette = saved.palette,
                    dualStyle = saved.dualStyle,
                    splitColorA = saved.splitColorA,
                    splitColorB = saved.splitColorB,
                    spinColorA = saved.spinColorA,
                    spinColorB = saved.spinColorB,
                    duoHueOffset = saved.duoHueOffset,
                    notifMode = saved.notifMode,
                    notifActivateDuration = saved.notifActivateDuration,
                    audioReactive = saved.audioReactive
                )
            }
        }

        fun pulseNotification(context: Context, color: Int) {
            val intent = Intent(context, DashDazzService::class.java).apply {
                action = ACTION_PULSE
                putExtra(EXTRA_COLOR, color)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun activateForNotification(context: Context, durationSec: Int) {
            val intent = Intent(context, DashDazzService::class.java).apply {
                action = ACTION_ACTIVATE
                putExtra(EXTRA_ACTIVATE_DURATION, durationSec)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        @Volatile
        private var activateJob: Job? = null

        fun notifyTileState(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    TileService.requestListeningState(
                        context.applicationContext,
                        ComponentName(context.applicationContext, DashDazzTileService::class.java)
                    )
                } catch (e: Exception) {
                    Log.d(TAG, "Tile state request ignored: ${e.message}")
                }
            }
        }

        fun relaunchAnimation(context: Context) {
            val service = activeService ?: return
            if (!_stateFlow.value.isRunning) return
            service.launchAnimationLoop()
        }

        fun refreshForegroundType(context: Context) {
            val service = activeService ?: return
            if (!_stateFlow.value.isRunning) return
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
            try {
                ServiceCompat.startForeground(
                    service,
                    NOTIFICATION_ID,
                    service.buildNotification(),
                    service.foregroundServiceType()
                )
            } catch (e: Exception) {
                Log.d(TAG, "Foreground type refresh ignored: ${e.message}")
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    private var pulseJob: Job? = null
    private var screenReceiverRegistered = false
    @Volatile
    private var screenInteractive = true

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenInteractive = false
                    if (_stateFlow.value.isRunning) {
                        Log.d(TAG, "Screen OFF received: applying screen policy")
                        evaluateScreenPolicy()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenInteractive = true
                    if (_stateFlow.value.isRunning) {
                        Log.d(TAG, "Screen ON received: applying screen policy")
                        evaluateScreenPolicy()
                    }
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    if (stillRunning()) {
                        Log.d(TAG, "Power connected: taking the ring back once")
                        launchAnimationLoop()
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    if (stillRunning()) {
                        Log.d(TAG, "Power disconnected: taking the ring back once")
                        launchAnimationLoop()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        activeService = this
        screenInteractive = (getSystemService(android.os.PowerManager::class.java)?.isInteractive) ?: true
        createNotificationChannel()

        LightsBridge.logColorLightSurface()

        restoreStateFromPrefs(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(screenReceiver, filter)
        screenReceiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopLight()
                return START_NOT_STICKY
            }
            ACTION_PULSE -> {
                val pulseColor = intent?.getIntExtra(EXTRA_COLOR, _stateFlow.value.color) ?: _stateFlow.value.color
                handleNotificationPulse(pulseColor)
                return START_STICKY
            }
            ACTION_ACTIVATE -> {
                val durationSec = intent?.getIntExtra(EXTRA_ACTIVATE_DURATION, Prefs.getNotificationActivateDuration(this))
                    ?: Prefs.getNotificationActivateDuration(this)
                handleActivateForNotification(durationSec)
                return START_STICKY
            }
            ACTION_START, ACTION_UPDATE -> {
                val newMode = intent?.getIntExtra(EXTRA_MODE, _stateFlow.value.mode) ?: _stateFlow.value.mode
                val newColor = intent?.getIntExtra(EXTRA_COLOR, _stateFlow.value.color) ?: _stateFlow.value.color
                val newSpeed = intent?.getLongExtra(EXTRA_SPEED_MS, _stateFlow.value.speedMs) ?: _stateFlow.value.speedMs
                val newBrightness = intent?.getFloatExtra(EXTRA_BRIGHTNESS, _stateFlow.value.brightness) ?: _stateFlow.value.brightness
                val newScreenMode = intent?.getIntExtra(EXTRA_SCREEN_MODE, _stateFlow.value.screenMode) ?: _stateFlow.value.screenMode
                val newBlinkNotif = intent?.getBooleanExtra(EXTRA_BLINK_NOTIFICATION, _stateFlow.value.blinkOnNotification) ?: _stateFlow.value.blinkOnNotification
                val newPalette = intent?.getIntExtra(EXTRA_PALETTE, _stateFlow.value.palette) ?: _stateFlow.value.palette
                val newDualStyle = intent?.getIntExtra(EXTRA_DUAL_STYLE, _stateFlow.value.dualStyle) ?: _stateFlow.value.dualStyle
                val newSplitColorA = intent?.getIntExtra(EXTRA_SPLIT_COLOR_A, Prefs.getSplitColorA(this)) ?: Prefs.getSplitColorA(this)
                val newSplitColorB = intent?.getIntExtra(EXTRA_SPLIT_COLOR_B, Prefs.getSplitColorB(this)) ?: Prefs.getSplitColorB(this)
                val newSpinColorA = intent?.getIntExtra(EXTRA_SPIN_COLOR_A, Prefs.getSpinColorA(this)) ?: Prefs.getSpinColorA(this)
                val newSpinColorB = intent?.getIntExtra(EXTRA_SPIN_COLOR_B, Prefs.getSpinColorB(this)) ?: Prefs.getSpinColorB(this)
                val newDuoHueOffset = intent?.getIntExtra(EXTRA_DUO_HUE_OFFSET, Prefs.getDuoHueOffset(this)) ?: Prefs.getDuoHueOffset(this)
                val newNotifMode = intent?.getIntExtra(EXTRA_NOTIF_MODE, Prefs.getNotificationMode(this)) ?: Prefs.getNotificationMode(this)
                val newNotifActivateDuration = intent?.getIntExtra(EXTRA_NOTIF_ACTIVATE_DURATION, Prefs.getNotificationActivateDuration(this)) ?: Prefs.getNotificationActivateDuration(this)

                val wasRunning = _stateFlow.value.isRunning
                val modeChanged = (newMode != _stateFlow.value.mode)
                val dualStyleChanged = (newDualStyle != _stateFlow.value.dualStyle)

                Prefs.setMode(this, newMode)
                Prefs.setColor(this, newColor)
                Prefs.setSpeedMs(this, newSpeed)
                Prefs.setBrightness(this, newBrightness)
                Prefs.setScreenMode(this, newScreenMode)
                Prefs.setBlinkOnNotification(this, newBlinkNotif)
                Prefs.setPalette(this, newPalette)
                Prefs.setDualStyle(this, newDualStyle)
                Prefs.setSplitColorA(this, newSplitColorA)
                Prefs.setSplitColorB(this, newSplitColorB)
                Prefs.setSpinColorA(this, newSpinColorA)
                Prefs.setSpinColorB(this, newSpinColorB)
                Prefs.setDuoHueOffset(this, newDuoHueOffset)
                Prefs.setNotificationMode(this, newNotifMode)
                Prefs.setNotificationActivateDuration(this, newNotifActivateDuration)

                _stateFlow.update {
                    it.copy(
                        isRunning = true,
                        isScreenPaused = false,
                        mode = newMode,
                        color = newColor,
                        speedMs = newSpeed,
                        brightness = newBrightness,
                        screenMode = newScreenMode,
                        blinkOnNotification = newBlinkNotif,
                        palette = newPalette,
                        dualStyle = newDualStyle,
                        splitColorA = newSplitColorA,
                        splitColorB = newSplitColorB,
                        spinColorA = newSpinColorA,
                        spinColorB = newSpinColorB,
                        duoHueOffset = newDuoHueOffset,
                        notifMode = newNotifMode,
                        notifActivateDuration = newNotifActivateDuration
                    )
                }

                startAsForeground()

                if (newScreenMode == SCREEN_NOTIF_ONLY) {

                    _stateFlow.update { it.copy(isScreenPaused = true) }
                    pauseAnimationForScreenOff()
                    notifyTileState(this)
                    return START_STICKY
                }

                if (!wasRunning || modeChanged || (newMode == MODE_DUAL && dualStyleChanged) || job == null || !job!!.isActive) {
                    launchAnimationLoop()
                } else {
                    evaluateScreenPolicy()
                }
                notifyTileState(this)
                return START_STICKY
            }
        }

        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                foregroundServiceType()
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun foregroundServiceType(): Int {
        val special = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return special
        val needsMic = _stateFlow.value.audioReactive &&
            AudioReactiveEngine.audioSource == AudioReactiveEngine.SOURCE_MIC
        return if (needsMic) special or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else special
    }

    private fun updateNotificationText() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun pauseAnimationForScreenOff() {
        job?.cancel()
        scope.launch {
            withContext(NonCancellable) {
                LightsBridge.turnOff()
            }
        }
        updateNotificationText()
    }

    private fun resumeAnimationForScreenOn() {
        launchAnimationLoop()
        updateNotificationText()
    }

    private fun shouldRunNow(): Boolean = when (_stateFlow.value.screenMode) {
        SCREEN_ON_ONLY -> screenInteractive
        SCREEN_OFF_ONLY -> !screenInteractive
        SCREEN_NOTIF_ONLY -> false
        else -> true
    }

    private fun evaluateScreenPolicy() {
        if (!_stateFlow.value.isRunning) return
        if (shouldRunNow()) {
            if (_stateFlow.value.isScreenPaused || job?.isActive != true) {
                launchAnimationLoop()
                updateNotificationText()
            }
        } else if (!_stateFlow.value.isScreenPaused) {
            _stateFlow.update { it.copy(isScreenPaused = true) }
            pauseAnimationForScreenOff()
        }
    }

    private fun launchAnimationLoop() {
        if (!shouldRunNow()) {
            if (!_stateFlow.value.isScreenPaused) {
                _stateFlow.update { it.copy(isScreenPaused = true) }
                pauseAnimationForScreenOff()
            }
            return
        }
        _stateFlow.update { it.copy(isScreenPaused = false) }
        job?.cancel()
        job = scope.launch {
            startAudioEngineIfNeeded()
            try {
                dispatchAnimationLoop()
            } finally {
                AudioReactiveEngine.stop()
            }
        }
    }

    private suspend fun CoroutineScope.dispatchAnimationLoop() {
        val mode = _stateFlow.value.mode
        if (mode != MODE_DUAL) {

            LightsBridge.resetHardwareToGlobal()
        }
        LightsBridge.resetScene()
        when (mode) {
            MODE_RAINBOW -> runRainbowLoop()
            MODE_BREATHE -> runBreatheLoop()
            MODE_SOLID -> runSolidLoop()
            MODE_DUAL -> runDualLoop()
            else -> runRainbowLoop()
        }
    }

    private suspend fun CoroutineScope.startAudioEngineIfNeeded() {
        if (_stateFlow.value.audioReactive) {
            AudioReactiveEngine.loadFromPrefs(this@DashDazzService)
            AudioReactiveEngine.restart()
        } else {
            AudioReactiveEngine.stop()
        }
    }

    private fun launchAnimationLoopBypassScreenCheck() {
        _stateFlow.update { it.copy(isScreenPaused = false) }
        job?.cancel()
        job = scope.launch {
            startAudioEngineIfNeeded()
            try {
                dispatchAnimationLoop()
            } finally {
                AudioReactiveEngine.stop()
            }
        }
    }

    private fun reactive(): Boolean = _stateFlow.value.audioReactive

    private suspend fun CoroutineScope.runRainbowLoop() {
        var hue = 0f
        var paletteProgress = 0f
        var audioHue = 0f
        var lastBeatCount = AudioReactiveEngine.beatCountOf()
        val hueIncrement = 1.5f

        while (isActive) {
            val stepMs = _stateFlow.value.speedMs.coerceIn(10L, 250L)
            val brightness = _stateFlow.value.brightness.coerceIn(0.05f, 1f)
            val paletteId = _stateFlow.value.palette
            val palette = Palettes.ALL.firstOrNull { it.id == paletteId } ?: Palettes.ALL[0]

            if (reactive()) {
                val now = System.currentTimeMillis()
                val frame = AudioReactiveEngine.sample(now)
                val beats = (frame.beatCount - lastBeatCount).toInt()
                if (beats > 0) {
                    lastBeatCount = frame.beatCount
                    audioHue = (audioHue + AudioReactiveEngine.degreesPerBeat * beats.coerceAtMost(3)) % 360f
                }
                val bright = (0.08f + frame.beat * 0.92f) * brightness
                LightsBridge.setCameraRing(hsv(audioHue, 1f, bright))
                delay(20L)
                continue
            }

            val userBright = brightness

            when (paletteId) {
                Palettes.SPECTRUM -> {
                    val wave = (sin(hue * Math.PI / 180.0).toFloat() * 0.10f)
                    val dynBright = (userBright * (0.90f + wave)).coerceIn(0.05f, 1f)
                    val dynSat = (0.92f + 0.08f * kotlin.math.cos(hue * Math.PI / 90.0).toFloat()).coerceIn(0.7f, 1f)
                    LightsBridge.setCameraRing(Color.HSVToColor(floatArrayOf(hue, dynSat, dynBright)))
                    hue = (hue + hueIncrement) % 360f
                }
                Palettes.FIRE -> {
                    val colors = palette.colors ?: intArrayOf(0xFFFF0000.toInt())
                    val total = colors.size
                    val idx1 = paletteProgress.toInt() % total
                    val idx2 = (idx1 + 1) % total
                    val fraction = paletteProgress - paletteProgress.toInt()

                    val flicker = (kotlin.random.Random.nextFloat() * 0.28f - 0.14f)
                    val fireBright = (userBright + flicker).coerceIn(0.08f, 1f)
                    val satMod = 0.88f + (kotlin.random.Random.nextFloat() * 0.12f)

                    val color = Palettes.interpolate(colors[idx1], colors[idx2], fraction, fireBright, satMod)
                    LightsBridge.setCameraRing(color)

                    val flameAdvance = 0.018f + (kotlin.random.Random.nextFloat() * 0.024f)
                    paletteProgress = (paletteProgress + flameAdvance) % total
                }
                Palettes.SUNSET -> {
                    val colors = palette.colors ?: intArrayOf(0xFFFF1744.toInt())
                    val total = colors.size
                    val idx1 = paletteProgress.toInt() % total
                    val idx2 = (idx1 + 1) % total
                    val fraction = paletteProgress - paletteProgress.toInt()

                    val wave = sin(paletteProgress * Math.PI * 2 / total).toFloat()
                    val dynBright = (userBright * (0.88f + 0.12f * wave)).coerceIn(0.05f, 1f)
                    val dynSat = (0.90f + 0.10f * (1f - wave)).coerceIn(0.75f, 1f)

                    val color = Palettes.interpolate(colors[idx1], colors[idx2], fraction, dynBright, dynSat)
                    LightsBridge.setCameraRing(color)
                    paletteProgress = (paletteProgress + 0.025f) % total
                }
                Palettes.OCEAN -> {
                    val colors = palette.colors ?: intArrayOf(0xFF00E5FF.toInt())
                    val total = colors.size
                    val idx1 = paletteProgress.toInt() % total
                    val idx2 = (idx1 + 1) % total
                    val fraction = paletteProgress - paletteProgress.toInt()

                    val wave = sin(paletteProgress * Math.PI * 3 / total).toFloat()
                    val dynBright = (userBright * (0.85f + 0.15f * wave)).coerceIn(0.05f, 1f)
                    val dynSat = (0.88f + 0.12f * (1f - wave)).coerceIn(0.75f, 1f)

                    val color = Palettes.interpolate(colors[idx1], colors[idx2], fraction, dynBright, dynSat)
                    LightsBridge.setCameraRing(color)
                    paletteProgress = (paletteProgress + 0.025f) % total
                }
                Palettes.CYBERPUNK -> {
                    val colors = palette.colors ?: intArrayOf(0xFFFF007F.toInt())
                    val total = colors.size
                    val idx1 = paletteProgress.toInt() % total
                    val idx2 = (idx1 + 1) % total
                    val fraction = paletteProgress - paletteProgress.toInt()

                    val surge = if (kotlin.random.Random.nextFloat() > 0.94f) 0.14f else 0.0f
                    val dynBright = (userBright + surge).coerceIn(0.05f, 1f)

                    val color = Palettes.interpolate(colors[idx1], colors[idx2], fraction, dynBright, 1.0f)
                    LightsBridge.setCameraRing(color)
                    paletteProgress = (paletteProgress + 0.028f) % total
                }
                Palettes.PASTEL -> {
                    val colors = palette.colors ?: intArrayOf(0xFFFF4081.toInt())
                    val total = colors.size
                    val idx1 = paletteProgress.toInt() % total
                    val idx2 = (idx1 + 1) % total
                    val fraction = paletteProgress - paletteProgress.toInt()

                    val wave = ((sin(paletteProgress * Math.PI * 2 / total) + 1.0) / 2.0).toFloat()
                    val dynBright = (userBright * (0.90f + 0.10f * wave)).coerceIn(0.05f, 1f)
                    val dynSat = (0.72f + 0.22f * wave).coerceIn(0.60f, 0.95f)

                    val color = Palettes.interpolate(colors[idx1], colors[idx2], fraction, dynBright, dynSat)
                    LightsBridge.setCameraRing(color)
                    paletteProgress = (paletteProgress + 0.025f) % total
                }
                else -> {
                    val colors = palette.colors ?: intArrayOf(0xFFFF0000.toInt())
                    val total = colors.size
                    val idx1 = paletteProgress.toInt() % total
                    val idx2 = (idx1 + 1) % total
                    val fraction = paletteProgress - paletteProgress.toInt()

                    val color = Palettes.interpolate(colors[idx1], colors[idx2], fraction, userBright)
                    LightsBridge.setCameraRing(color)
                    paletteProgress = (paletteProgress + 0.025f) % total
                }
            }

            delay(stepMs)
        }
    }

    private suspend fun CoroutineScope.runBreatheLoop() {
        var angle = 0.0

        while (isActive) {
            val stepMs = _stateFlow.value.speedMs.coerceIn(10L, 250L)
            val baseColor = _stateFlow.value.color
            val hsv = FloatArray(3)
            Color.colorToHSV(baseColor, hsv)
            val userBright = _stateFlow.value.brightness.coerceIn(0.1f, 1f)

            if (reactive()) {
                val frame = AudioReactiveEngine.sample(System.currentTimeMillis())
                val ceiling = (userBright * (0.35f + 0.65f * frame.level)).coerceIn(0.1f, 1f)
                val centre = 0.05f + (ceiling - 0.05f) * 0.5f
                val depth = ((ceiling - 0.05f) * 0.4f * frame.beat).coerceAtMost(ceiling - centre)
                val maxVal = (centre + (ceiling - centre) * 0.85f + depth).coerceIn(0.1f, 1f)
                val minVal = (centre - depth).coerceAtLeast(0.04f)
                val factor = ((sin(angle) + 1.0) / 2.0).toFloat()
                val breathHsv = floatArrayOf(hsv[0], hsv[1], (minVal + factor * (maxVal - minVal)))
                LightsBridge.setCameraRing(Color.HSVToColor(breathHsv))
                angle += 0.06
                delay(20L)
                continue
            }

            val factor = ((sin(angle) + 1.0) / 2.0).toFloat()
            val breathHsv = floatArrayOf(hsv[0], hsv[1], (0.05f + factor * (userBright - 0.05f)))
            LightsBridge.setCameraRing(Color.HSVToColor(breathHsv))
            angle += 0.06
            delay(stepMs)
        }
    }

    private suspend fun CoroutineScope.runSolidLoop() {
        var lastColor: Int? = null
        if (reactive()) {
            val baseHsv = FloatArray(3)
            while (isActive) {
                val state = _stateFlow.value
                val userBright = state.brightness.coerceIn(0.05f, 1f)
                Color.colorToHSV(state.color, baseHsv)
                val frame = AudioReactiveEngine.sample(System.currentTimeMillis())
                val bright = (0.15f + frame.level * 0.70f + frame.beat * 0.30f) * userBright
                val targetColor = hsv(baseHsv[0], baseHsv[1], bright)
                if (targetColor != lastColor) {
                    lastColor = targetColor
                    LightsBridge.setCameraRing(targetColor)
                }
                delay(20L)
            }
            return
        }

        _stateFlow.collect { state ->
            val hsv = FloatArray(3)
            Color.colorToHSV(state.color, hsv)
            hsv[2] = state.brightness.coerceIn(0.05f, 1f)
            val targetColor = Color.HSVToColor(hsv)
            if (targetColor != lastColor) {
                lastColor = targetColor
                LightsBridge.setCameraRing(targetColor)
            }
        }
    }

    private fun hsv(hue: Float, saturation: Float, value: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val argb = Color.HSVToColor(floatArrayOf(h, saturation.coerceIn(0f, 1f), value.coerceIn(0.05f, 1f)))
        return if (argb == -1) 0xFFFEFEFE.toInt() else argb
    }

    private val hsvScratch = FloatArray(3)

    private fun hsvFromArgb(argb: Int, brightness: Float): Int {
        AndroidColor.colorToHSV(argb, hsvScratch)
        return hsv(hsvScratch[0], hsvScratch[1], brightness)
    }

    private fun colorToHsv(argb: Int): FloatArray {
        AndroidColor.colorToHSV(argb, hsvScratch)
        return hsvScratch
    }

    private suspend fun CoroutineScope.runDualLoop() {
        var stepIndex = 0
        var subStep = 0
        var lastDualStyle = -1
        var spinInited = false
        var splitInited = false
        var duoHue = 0f
        var beatHue = 0f
        var lastBeatCount = AudioReactiveEngine.beatCountOf()

        while (isActive) {
            val speedMs = _stateFlow.value.speedMs.coerceIn(15L, 500L)
            val brightness = _stateFlow.value.brightness.coerceIn(0.05f, 1f)
            val dualStyle = _stateFlow.value.dualStyle

            if (dualStyle != lastDualStyle) {
                lastDualStyle = dualStyle
                spinInited = false
                splitInited = false
            }

            when (dualStyle) {
                DUAL_HARDWARE_SPLIT -> {
                    if (LightsBridge.isRootAvailable()) {
                        val state = _stateFlow.value
                        val needFull = !splitInited
                        LightsBridge.setDualSpotColors(state.splitColorA, state.splitColorB, full = needFull)
                        splitInited = true
                        delay(if (needFull) 400L else 1000L)
                    } else {
                        LightsBridge.setDualColorGame()
                        delay(3500L)
                    }
                }
                DUAL_SPIN -> {
                    if (LightsBridge.isRootAvailable()) {
                        if (!spinInited) {
                            LightsBridge.initSpinMode()
                            spinInited = true
                        }
                        val isSpotA = (stepIndex % 2 == 0)
                        val activeColor = if (isSpotA) _stateFlow.value.spinColorA else _stateFlow.value.spinColorB
                        LightsBridge.setSpinFrame(isSpotA, activeColor, brightness)
                        stepIndex++
                        val spinDelay = speedMs.coerceIn(50L, 500L)
                        delay(spinDelay)
                    } else {
                        val isSpotA = (stepIndex % 2 == 0)
                        val activeColor = if (isSpotA) _stateFlow.value.spinColorA else _stateFlow.value.spinColorB
                        LightsBridge.setCameraRing(activeColor)
                        stepIndex++
                        delay(speedMs.coerceIn(80L, 500L))
                    }
                }
                DUAL_POLICE -> {
                    val red = Color.HSVToColor(floatArrayOf(0f, 1f, brightness))
                    val blue = Color.HSVToColor(floatArrayOf(235f, 1f, brightness))
                    when (subStep % 6) {
                        0, 1 -> LightsBridge.setCameraRing(red)
                        2 -> LightsBridge.setCameraRing(0)
                        3, 4 -> LightsBridge.setCameraRing(blue)
                        5 -> LightsBridge.setCameraRing(0)
                    }
                    subStep++
                    delay(speedMs * 3L)
                }
                DUAL_ALTERNATE -> {

                    val state = _stateFlow.value
                    val color0 = hsvFromArgb(state.spinColorA, brightness)
                    val color1 = hsvFromArgb(state.spinColorB, brightness)
                    val activeColor = if (stepIndex % 2 == 0) color0 else color1
                    LightsBridge.setCameraRing(activeColor)
                    stepIndex++
                    delay(speedMs * 8L)
                }
                DUAL_RAINBOW_DUO -> {

                    if (LightsBridge.isRootAvailable()) {
                        val offset = _stateFlow.value.duoHueOffset.toFloat()
                        val hueB = ((duoHue + offset) % 360f + 360f) % 360f

                        val colorA = Color.HSVToColor(floatArrayOf(duoHue, 1f, brightness))
                        val colorB = Color.HSVToColor(floatArrayOf(hueB, 1f, brightness))
                        val needFull = !splitInited
                        LightsBridge.setDualSpotColors(colorA, colorB, full = needFull)
                        splitInited = true
                        duoHue = (duoHue + 3f) % 360f
                        delay(speedMs.coerceIn(30L, 250L))
                    } else {
                        LightsBridge.setDualColorGame()
                        delay(3500L)
                    }
                }
                DUAL_BEAT_RAINBOW -> {

                    val frame = AudioReactiveEngine.sample(System.currentTimeMillis())
                    val beats = (frame.beatCount - lastBeatCount).toInt()
                    if (beats > 0) {
                        lastBeatCount = frame.beatCount
                        beatHue = (beatHue + AudioReactiveEngine.degreesPerBeat * beats.coerceAtMost(3)) % 360f
                    }
                    val bright = (0.08f + frame.beat * 0.92f) * brightness
                    val colorA = hsv(beatHue, 1f, bright)
                    val colorB = hsv(beatHue + _stateFlow.value.duoHueOffset, 1f, bright)
                    if (LightsBridge.isRootAvailable()) {
                        val needFull = !splitInited
                        LightsBridge.setDualSpotColors(colorA, colorB, full = needFull)
                        splitInited = true
                    } else {
                        LightsBridge.setCameraRing(colorA)
                    }
                    delay(20L)
                }
                DUAL_SPLIT_BEAT -> {

                    val frame = AudioReactiveEngine.sample(System.currentTimeMillis())
                    val even = frame.beatCount % 2 == 0L
                    val state = _stateFlow.value
                    val brightA = (0.10f + (if (even) frame.beat else frame.level) * 0.9f) * brightness
                    val brightB = (0.10f + (if (even) frame.level else frame.beat) * 0.9f) * brightness
                    val colorA = hsv(colorToHsv(state.splitColorA)[0], 1f, brightA)
                    val colorB = hsv(colorToHsv(state.splitColorB)[0], 1f, brightB)
                    if (LightsBridge.isRootAvailable()) {
                        val needFull = !splitInited
                        LightsBridge.setDualSpotColors(colorA, colorB, full = needFull)
                        splitInited = true
                    } else {
                        LightsBridge.setCameraRing(colorA)
                    }
                    delay(20L)
                }
                else -> {
                    LightsBridge.setDualColorGame()
                    delay(3500L)
                }
            }
        }
    }

    private fun handleNotificationPulse(pulseColor: Int) {
        pulseJob?.cancel()
        val wasRunningBeforePulse = _stateFlow.value.isRunning && !_stateFlow.value.isScreenPaused
        val isNotifOnlyMode = _stateFlow.value.screenMode == SCREEN_NOTIF_ONLY

        if (!_stateFlow.value.isRunning) {
            startAsForeground()
        }

        pulseJob = scope.launch {
            try {
                if (wasRunningBeforePulse) {
                    job?.cancel()
                }

                val hsv = FloatArray(3)
                Color.colorToHSV(pulseColor, hsv)
                val maxBright = _stateFlow.value.brightness.coerceIn(0.1f, 1f)

                for (cycle in 0 until 2) {
                    val steps = 25
                    for (i in 0..steps) {
                        val progress = sin(Math.PI * i / steps).toFloat()
                        val pulseHsv = floatArrayOf(hsv[0], hsv[1], progress * maxBright)
                        LightsBridge.setCameraRing(Color.HSVToColor(pulseHsv))
                        delay(18)
                    }
                    delay(60)
                }
            } finally {
                withContext(NonCancellable) {
                    LightsBridge.resetScene()
                    if (wasRunningBeforePulse) {
                        launchAnimationLoop()
                    } else if (isNotifOnlyMode) {

                        LightsBridge.turnOff()
                        _stateFlow.update { it.copy(isScreenPaused = true) }
                    } else {
                        LightsBridge.turnOff()

                        stopLight()
                    }
                }
            }
        }
    }

    private fun handleActivateForNotification(durationSec: Int) {
        activateJob?.cancel()

        val savedMode = Prefs.getMode(this)
        val savedColor = Prefs.getColor(this)
        val savedSpeed = Prefs.getSpeedMs(this)
        val savedBrightness = Prefs.getBrightness(this)
        val savedScreenMode = Prefs.getScreenMode(this)
        val savedPalette = Prefs.getPalette(this)
        val savedDualStyle = Prefs.getDualStyle(this)
        val savedSplitColorA = Prefs.getSplitColorA(this)
        val savedSplitColorB = Prefs.getSplitColorB(this)
        val savedSpinColorA = Prefs.getSpinColorA(this)
        val savedSpinColorB = Prefs.getSpinColorB(this)
        val savedDuoHueOffset = Prefs.getDuoHueOffset(this)
        val savedNotifMode = Prefs.getNotificationMode(this)
        val savedNotifActivateDuration = Prefs.getNotificationActivateDuration(this)

        _stateFlow.update {
            it.copy(
                isRunning = true,
                isScreenPaused = false,
                mode = savedMode,
                color = savedColor,
                speedMs = savedSpeed,
                brightness = savedBrightness,
                screenMode = savedScreenMode,
                palette = savedPalette,
                dualStyle = savedDualStyle,
                splitColorA = savedSplitColorA,
                splitColorB = savedSplitColorB,
                spinColorA = savedSpinColorA,
                spinColorB = savedSpinColorB,
                duoHueOffset = savedDuoHueOffset,
                notifMode = savedNotifMode,
                notifActivateDuration = savedNotifActivateDuration
            )
        }

        startAsForeground()

        val isNotifOnlyMode = savedScreenMode == SCREEN_NOTIF_ONLY
        if (!isNotifOnlyMode) {
            launchAnimationLoop()
        } else {

            launchAnimationLoopBypassScreenCheck()
        }
        notifyTileState(this)

        activateJob = scope.launch {
            try {
                delay(durationSec * 1000L)
            } finally {
                withContext(NonCancellable) {
                    Log.d(TAG, "Notification activate duration expired, stopping")
                    if (isNotifOnlyMode) {

                        job?.cancel()
                        LightsBridge.turnOff()
                        _stateFlow.update { it.copy(isScreenPaused = true) }
                    } else {
                        stopLight()
                    }
                }
            }
        }
    }

    private fun stillRunning(): Boolean =
        _stateFlow.value.isRunning && !_stateFlow.value.isScreenPaused

    private fun stopLight() {
        job?.cancel()
        pulseJob?.cancel()
        activateJob?.cancel()
        AudioReactiveEngine.stop()
        scope.launch {
            withContext(NonCancellable) {
                LightsBridge.turnOff()
            }
        }
        _stateFlow.update { it.copy(isRunning = false, isScreenPaused = false) }
        notifyTileState(this)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (activeService == this) {
            activeService = null
        }
        if (screenReceiverRegistered) {
            try {
                unregisterReceiver(screenReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Unregistering screenReceiver failed", e)
            }
            screenReceiverRegistered = false
        }
        stopLight()
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr?.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_desc)
                    setShowBadge(false)
                }
                mgr?.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, DashDazzService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val openActivityIntent = Intent(this, MainActivity::class.java)
        val openActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val state = _stateFlow.value
        val modeName = when (state.mode) {
            MODE_RAINBOW -> getString(R.string.mode_rainbow)
            MODE_BREATHE -> getString(R.string.mode_breathe)
            MODE_SOLID -> getString(R.string.mode_solid)
            MODE_DUAL -> getString(R.string.mode_dual)
            else -> getString(R.string.mode_rainbow)
        }

        val audioLabel = if (state.audioReactive) " ${getString(R.string.notif_audio_suffix)}" else ""
        val labelledMode = modeName + audioLabel
        val activateLabel = getString(R.string.notif_activate_running)
        val baseText = if (activateJob?.isActive == true) "$activateLabel: $labelledMode" else labelledMode

        val contentText = if (state.isScreenPaused) {
            if (state.screenMode == SCREEN_NOTIF_ONLY) {
                getString(R.string.notification_notif_only)
            } else {
                getString(R.string.notification_paused)
            }
        } else {
            baseText
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_qs_camera_ring)
            .setContentIntent(openActivityPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                0,
                getString(R.string.action_turn_off),
                stopPendingIntent
            )
            .build()
    }
}
