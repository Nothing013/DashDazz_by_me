package com.dashdazz.light

import android.os.IBinder
import android.util.Log
import com.dashdazz.light.lightsapi.ILightsManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object LightsBridge {
    private const val TAG = "DashDazz"
    private const val CAMERA_PKG = "com.android.camera"
    private const val STYLE_MUSIC = 3
    private const val STYLE_RESET = -1

    @Volatile
    private var cachedManager: ILightsManager? = null

    private val deathRecipient = IBinder.DeathRecipient {
        Log.w(TAG, "LightsManager binder died, clearing cache")
        cachedManager = null
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return isShizukuAvailable() &&
                Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun getLightsManager(): ILightsManager? {
        val current = cachedManager
        if (current != null && current.asBinder().isBinderAlive) {
            return current
        }

        synchronized(this) {
            val existing = cachedManager
            if (existing != null && existing.asBinder().isBinderAlive) {
                return existing
            }

            if (!isShizukuAvailable()) return null

            try {
                val raw = SystemServiceHelper.getSystemService(ILightsManager.DESCRIPTOR) ?: return null
                val wrapper = ShizukuBinderWrapper(raw)
                try {
                    wrapper.linkToDeath(deathRecipient, 0)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to linkToDeath on wrapper", e)
                }
                val manager = ILightsManager.Stub.asInterface(wrapper)
                cachedManager = manager
                return manager
            } catch (e: Exception) {
                Log.e(TAG, "Error acquiring ILightsManager", e)
                return null
            }
        }
    }

    private const val SYS_REG = "/sys/class/leds/aw21024_led/reg"
    private const val SYS_HWEN = "/sys/class/leds/aw21024_led/hwen"
    private const val SYS_EFFECT = "/sys/class/leds/aw21024_led/effect"

    @Volatile
    private var isRootCached: Boolean? = null

    fun checkRootFresh(): Boolean {
        val available = try {
            val p = ProcessBuilder("su", "-c", "id").start()
            val text = p.inputStream.bufferedReader().use { it.readText() }
            p.waitFor() == 0 && text.contains("uid=0")
        } catch (_: Throwable) {
            false
        }
        isRootCached = available
        return available
    }

    fun isRootAvailable(): Boolean {
        isRootCached?.let { return it }
        return checkRootFresh()
    }

    fun runRoot(cmd: String): Boolean {
        return try {
            val p = ProcessBuilder("su", "-c", cmd).start()
            p.waitFor() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Root command failed: $cmd", e)
            false
        }
    }

    private var fastShell: Process? = null
    private var fastWriter: java.io.OutputStream? = null

    @Synchronized
    fun runRootFast(cmd: String): Boolean {
        for (attempt in 0..1) {
            try {
                var p = fastShell
                if (p == null || !p.isAlive) {
                    p = ProcessBuilder("su").redirectErrorStream(true).start()
                    fastShell = p
                    fastWriter = p.outputStream

                    Thread {
                        try { p.inputStream.bufferedReader().use { r -> while (r.readLine() != null) { } } } catch (_: Throwable) {}
                    }.apply { isDaemon = true }.start()
                }
                fastWriter!!.write((cmd + "\n").toByteArray())
                fastWriter!!.flush()
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Fast root shell failed, recreating", e)
                try { fastShell?.destroy() } catch (_: Throwable) {}
                fastShell = null
                fastWriter = null
            }
        }
        return false
    }

    fun hardwareReset(): Boolean {
        return runRoot("echo 1 > $SYS_HWEN && echo 0 > $SYS_EFFECT")
    }

    fun hardwareOff(): Boolean {
        return runRoot("echo 0 > $SYS_EFFECT")
    }

    fun resetHardwareToGlobal(): Boolean {
        if (!isRootAvailable()) return false
        val cmd = "SYS=$SYS_REG; " +
                "echo 1 > $SYS_HWEN && " +
                "echo 2 > $SYS_EFFECT && " +
                "echo '0x00 0x11' > \$SYS && " +
                "echo '0x6e 0xff' > \$SYS && " +
                "echo '0x7a 0x00' > \$SYS && " +
                "echo '0xa0 0x00' > \$SYS && " +
                "echo '0xa1 0x00' > \$SYS && " +
                "echo '0xa7 0xcc' > \$SYS && " +
                "echo '0xab 0xff' > \$SYS"
        return runRoot(cmd)
    }

    fun setHardwareColorDirect(colorArgb: Int): Boolean {
        val r = (colorArgb shr 16) and 0xFF
        val g = (colorArgb shr 8) and 0xFF
        val b = colorArgb and 0xFF
        val cmd = "SYS=$SYS_REG; " +
                "echo '0xa8 ${String.format("0x%02x", r)}' > \$SYS; " +
                "echo '0xa9 ${String.format("0x%02x", g)}' > \$SYS; " +
                "echo '0xaa ${String.format("0x%02x", b)}' > \$SYS; " +
                "echo '0xab 0xff' > \$SYS"
        return runRootFast(cmd)
    }

    fun setHardwareBreath(r: Int, g: Int, b: Int): Boolean {
        val cmd = "SYS=$SYS_REG; " +
                "echo '0x00 0x11' > \$SYS && " +
                "echo '0x6e 0xff' > \$SYS && " +
                "echo '0x7a 0x00' > \$SYS && " +
                "echo '0xa0 0x07' > \$SYS && " +
                "echo '0xa2 0x44' > \$SYS && " +
                "echo '0xa3 0x44' > \$SYS && " +
                "echo '0xa6 0x66' > \$SYS && " +
                "echo '0xa7 0x00' > \$SYS && " +
                "echo '0xa8 ${String.format("0x%02x", r)}' > \$SYS && " +
                "echo '0xa9 ${String.format("0x%02x", g)}' > \$SYS && " +
                "echo '0xaa ${String.format("0x%02x", b)}' > \$SYS && " +
                "echo '0xab 0xff' > \$SYS && " +
                "echo '0xac 0x00' > \$SYS && " +
                "echo '0xa1 0x01' > \$SYS"
        return runRoot(cmd)
    }

    fun setHardwareSolid(r: Int, g: Int, b: Int): Boolean {
        val cmd = "SYS=$SYS_REG; " +
                "echo '0x00 0x11' > \$SYS && " +
                "echo '0x6e 0xff' > \$SYS && " +
                "echo '0x7a 0x00' > \$SYS && " +
                "echo '0xa0 0x00' > \$SYS && " +
                "echo '0xa1 0x00' > \$SYS && " +
                "echo '0xa7 0xcc' > \$SYS && " +
                "echo '0xa8 ${String.format("0x%02x", r)}' > \$SYS && " +
                "echo '0xa9 ${String.format("0x%02x", g)}' > \$SYS && " +
                "echo '0xaa ${String.format("0x%02x", b)}' > \$SYS && " +
                "echo '0xab 0xff' > \$SYS"
        return runRoot(cmd)
    }

    fun setDualSpotColors(colorA: Int, colorB: Int, full: Boolean = true): Boolean {
        val rA = (colorA shr 16) and 0xFF
        val gA = (colorA shr 8) and 0xFF
        val bA = colorA and 0xFF

        val rB = (colorB shr 16) and 0xFF
        val gB = (colorB shr 8) and 0xFF
        val bB = colorB and 0xFF

        val init = if (full) {
            "echo 1 > $SYS_HWEN; " +
                    "echo 2 > $SYS_EFFECT; " +
                    "echo '0x00 0x11' > \$SYS; " +
                    "echo '0x6e 0xff' > \$SYS; " +
                    "echo '0x7a 0x01' > \$SYS; " +
                    "echo '0xa0 0x00' > \$SYS; " +
                    "echo '0xa1 0x00' > \$SYS; " +
                    "echo '0xab 0x00' > \$SYS; " +
                    "echo '0xac 0x10' > \$SYS; " +
                    "for b in 0x01 0x02 0x03 0x04 0x05 0x06 0x07 0x08 0x09 0x0a 0x0b 0x0c 0x0d 0x0e 0x0f 0x10 0x11 0x12 0x13 0x14 0x15 0x16 0x17 0x18; do echo \"\$b 0xff\" > \$SYS; done; "
        } else ""

        val cmd = "SYS=$SYS_REG; " + init +
                "echo '0x5c ${String.format("0x%02x", rA)}' > \$SYS; " +
                "echo '0x5d ${String.format("0x%02x", gA)}' > \$SYS; " +
                "echo '0x5e ${String.format("0x%02x", bA)}' > \$SYS; " +
                "echo '0x53 ${String.format("0x%02x", rB)}' > \$SYS; " +
                "echo '0x54 ${String.format("0x%02x", gB)}' > \$SYS; " +
                "echo '0x55 ${String.format("0x%02x", bB)}' > \$SYS; " +
                "echo '0x49 0x00' > \$SYS"
        return if (full) runRoot(cmd) else runRootFast(cmd)
    }

    fun initSpinMode(): Boolean {
        val cmd = "SYS=$SYS_REG; " +
                "echo 1 > $SYS_HWEN; " +
                "echo 2 > $SYS_EFFECT; " +
                "echo '0x00 0x11' > \$SYS; " +
                "echo '0x6e 0xff' > \$SYS; " +
                "echo '0xa0 0x00' > \$SYS; " +
                "echo '0xa1 0x00' > \$SYS; " +
                "echo '0xa7 0xcc' > \$SYS"
        return runRoot(cmd)
    }

    fun setSpinFrame(isSpotA: Boolean, colorArgb: Int, brightness: Float = 1.0f): Boolean {
        val r = (((colorArgb shr 16) and 0xFF) * brightness).toInt().coerceIn(0, 255)
        val g = (((colorArgb shr 8) and 0xFF) * brightness).toInt().coerceIn(0, 255)
        val b = ((colorArgb and 0xFF) * brightness).toInt().coerceIn(0, 255)
        val mask = if (isSpotA) "0x40" else "0x08"

        val cmd = "SYS=$SYS_REG; " +
                "echo '0xa8 ${String.format("0x%02x", r)}' > \$SYS; " +
                "echo '0xa9 ${String.format("0x%02x", g)}' > \$SYS; " +
                "echo '0xaa ${String.format("0x%02x", b)}' > \$SYS; " +
                "echo '0xab $mask' > \$SYS"
        return runRootFast(cmd)
    }

    fun startSpin(
        colorAArgb: Int = 0xFFFF0000.toInt(),
        colorBArgb: Int = 0xFF0000FF.toInt(),
        delaySec: Float = 0.25f,
        rounds: Int = 15
    ): Boolean {
        val rA = (colorAArgb shr 16) and 0xFF
        val gA = (colorAArgb shr 8) and 0xFF
        val bA = colorAArgb and 0xFF
        val rB = (colorBArgb shr 16) and 0xFF
        val gB = (colorBArgb shr 8) and 0xFF
        val bB = colorBArgb and 0xFF

        val script = "SYS=$SYS_REG; " +
                "echo 1 > $SYS_HWEN; " +
                "echo 2 > $SYS_EFFECT; " +
                "echo '0x00 0x11' > \$SYS; " +
                "echo '0x6e 0xff' > \$SYS; " +
                "echo '0xa0 0x00' > \$SYS; " +
                "echo '0xa1 0x00' > \$SYS; " +
                "echo '0xa7 0xcc' > \$SYS; " +
                "i=0; while [ \$i -lt $rounds ]; do " +
                "echo '0xa8 ${String.format("0x%02x", rA)}' > \$SYS; echo '0xa9 ${String.format("0x%02x", gA)}' > \$SYS; echo '0xaa ${String.format("0x%02x", bA)}' > \$SYS; echo '0xab 0x40' > \$SYS; sleep $delaySec; " +
                "echo '0xa8 ${String.format("0x%02x", rB)}' > \$SYS; echo '0xa9 ${String.format("0x%02x", gB)}' > \$SYS; echo '0xaa ${String.format("0x%02x", bB)}' > \$SYS; echo '0xab 0x08' > \$SYS; sleep $delaySec; " +
                "i=\$((i + 1)); done; " +
                "echo '0xab 0xff' > \$SYS"
        Thread {
            runRoot(script)
        }.start()
        return true
    }

    fun setCameraRing(colorArgb: Int): Boolean {
        val finalColor = if (colorArgb == -1) 0xFFFEFEFE.toInt() else colorArgb
        if (isRootAvailable()) {
            return setHardwareColorDirect(finalColor)
        }
        val mgr = getLightsManager()
        if (mgr != null && hasShizukuPermission()) {
            return try {
                mgr.setColorCommon(finalColor, CAMERA_PKG, STYLE_MUSIC, 0)
                true
            } catch (e: Exception) {
                Log.e(TAG, "setCameraRing via Shizuku failed", e)
                cachedManager = null
                false
            }
        }
        return false
    }

    fun setCameraRing(colorArgb: Int, @Suppress("UNUSED_PARAMETER") onMs: Int): Boolean {
        return setCameraRing(colorArgb)
    }

    fun turnOff(): Boolean {
        if (isRootAvailable()) {
            hardwareOff()
        }
        return try {
            val mgr = getLightsManager() ?: return isRootAvailable()
            mgr.setColorCommon(0, CAMERA_PKG, STYLE_MUSIC, 0)
            mgr.setColorCommon(0, CAMERA_PKG, STYLE_RESET, 0)
            mgr.setColorfulLight(CAMERA_PKG, STYLE_RESET, 0)
            true
        } catch (e: Exception) {
            Log.e(TAG, "turnOff failed", e)
            cachedManager = null
            isRootAvailable()
        }
    }

    fun setDualColorGame(): Boolean {
        return try {
            val mgr = getLightsManager() ?: return false
            mgr.setColorfulLight(CAMERA_PKG, 2, 0)
            true
        } catch (e: Exception) {
            Log.e(TAG, "setDualColorGame failed", e)
            false
        }
    }

    fun resetScene(): Boolean {
        return try {
            val mgr = getLightsManager() ?: return false

            mgr.setColorCommon(0, CAMERA_PKG, STYLE_RESET, 0)

            mgr.setCustomLight(0, 0, 0, 0, 0, CAMERA_PKG, 12, 0)

            mgr.setCustomLight(0, 0, 0, 0, 0, CAMERA_PKG, STYLE_RESET, 0)
            mgr.setColorfulLight(CAMERA_PKG, STYLE_RESET, 0)
            true
        } catch (e: Exception) {
            Log.e(TAG, "resetScene failed", e)
            false
        }
    }

    fun logColorLightSurface() {
        try {
            val clazz = Class.forName("android.app.ColorLightManager")
            val names = clazz.methods.joinToString(", ") { it.name }
            Log.w(TAG, "ColorLightManager methods: $names")
        } catch (e: Throwable) {
            Log.w(TAG, "ColorLightManager unavailable: ${e.message}")
        }
    }
}
