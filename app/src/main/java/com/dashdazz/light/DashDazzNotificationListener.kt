package com.dashdazz.light

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DashDazzNotificationListener : NotificationListenerService() {

    private val listenerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var mediaWasPlaying = false

    @Volatile
    private var mediaReclaimJob: kotlinx.coroutines.Job? = null

    @Volatile
    private var lastOffResetMs = 0L

    private val mediaSessionListener = object : MediaSessionManager.OnActiveSessionsChangedListener {
        override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
            handleMediaSessionChange(controllers)
        }
    }

    private fun handleMediaSessionChange(controllers: List<MediaController>?) {
        val anyPlaying = controllers?.any { ctrl ->
            val state = ctrl.playbackState
            state != null && state.state == PlaybackState.STATE_PLAYING
        } ?: false

        if (anyPlaying && !mediaWasPlaying) {

            Log.d(TAG, "Media playback started — scheduling recurring scene reclaims")
            mediaReclaimJob?.cancel()
            mediaReclaimJob = listenerScope.launch {

                val schedule = listOf(500L, 1500L, 3000L, 5000L, 8000L)
                for (waitMs in schedule) {
                    delay(waitMs)
                    if (!DashDazzService.isRunning) continue
                    val stillPlaying = runCatching {
                        val sessions = getSystemService(MediaSessionManager::class.java)
                            ?.getActiveSessions(ComponentName(this@DashDazzNotificationListener,
                                DashDazzNotificationListener::class.java))
                        sessions?.any { c ->
                            c.playbackState?.state == PlaybackState.STATE_PLAYING
                        } ?: false
                    }.getOrDefault(false)
                    if (stillPlaying && !DashDazzService.stateFlow.value.isScreenPaused) {
                        LightsBridge.resetScene()
                    } else if (!stillPlaying) {
                        break
                    }
                }
            }
        } else if (!anyPlaying && mediaWasPlaying) {

            Log.d(TAG, "Media playback stopped — reclaiming LED control")
            listenerScope.launch {
                delay(300)
                if (DashDazzService.isRunning && !DashDazzService.stateFlow.value.isScreenPaused) {
                    LightsBridge.resetScene()
                }
            }
        }
        mediaWasPlaying = anyPlaying
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val msm = getSystemService(MediaSessionManager::class.java)
            val component = ComponentName(this, DashDazzNotificationListener::class.java)
            msm?.addOnActiveSessionsChangedListener(mediaSessionListener, component)

            handleMediaSessionChange(msm?.getActiveSessions(component))
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot access MediaSessionManager — notification listener permission may be insufficient", e)
        } catch (e: Throwable) {
            Log.w(TAG, "MediaSessionManager setup failed", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            requestRebind(ComponentName(this, DashDazzNotificationListener::class.java))
        } catch (e: Throwable) {
            Log.w(TAG, "Listener rebind failed", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        if (sbn.packageName == packageName) return

        val notification = sbn.notification ?: return

        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return
        if ((notification.flags and Notification.FLAG_NO_CLEAR) != 0) return

        val allowedApps = Prefs.getNotifApps(this)
        if (allowedApps.isNotEmpty() && sbn.packageName !in allowedApps) return

        val notifMode = Prefs.getNotificationMode(this)
        when (notifMode) {
            Prefs.NOTIF_MODE_ACTIVATE -> {

                val durationSec = Prefs.getNotificationActivateDuration(this)
                val cfg = Prefs.getAppNotifConfig(this, sbn.packageName)
                Log.d(TAG, "Notification received from ${sbn.packageName}, activating lights for ${durationSec}s")
                if (cfg.color != null) {
                    val holdMs = ((cfg.durationSec ?: durationSec).coerceIn(1, 60)) * 1000L
                    DashDazzService.pulseNotification(this, cfg.color, holdMs)
                } else {
                    DashDazzService.activateForNotification(this, durationSec)
                }
            }
            else -> {

                listenerScope.launch {
                    kotlinx.coroutines.delay(3500)
                    LightsBridge.resetScene()
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        listenerScope.launch {
            kotlinx.coroutines.delay(500)
            LightsBridge.resetScene()
        }
    }

    override fun onDestroy() {
        try {
            val msm = getSystemService(MediaSessionManager::class.java)
            msm?.removeOnActiveSessionsChangedListener(mediaSessionListener)
        } catch (_: Throwable) {}
        mediaReclaimJob?.cancel()
        mediaReclaimJob = null
        listenerScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "DashDazzNotif"

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false

            val mine = ComponentName(context, DashDazzNotificationListener::class.java)
            return enabledListeners.split(":").any { entry ->
                runCatching {
                    ComponentName.unflattenFromString(entry) == mine
                }.getOrDefault(false)
            }
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
