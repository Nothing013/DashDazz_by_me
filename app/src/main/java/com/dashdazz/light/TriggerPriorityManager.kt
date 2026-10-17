package com.dashdazz.light

/**
 * Non-Blocking Priority Manager for concurrent DashDazz triggers.
 *
 * Priority Matrix (Level 1 is highest priority):
 * - Level 1 (Highest): Notification Alerts (Temporary Overrides, pulses, urgent calls)
 * - Level 2: Lock / Unlock / Screen Flash Effects
 * - Level 3: Charging Animations & Battery Full Alerts
 * - Level 4: Special Event Alerts (Pocket Mode, DND, Low Battery Warning)
 * - Level 5: React to Audio (Per-App Allowlisted)
 * - Level 6 (Lowest): Ambient / Always-On Ring Baseline
 *
 * When a higher priority trigger activates, it suspends lower priority rendering.
 * Once the high-priority event completes, the manager resolves the next highest
 * active trigger and restores it without leaving the ring in an undefined state.
 */
class TriggerPriorityManager(
    private val onTriggerChanged: (TriggerType?, TriggerPayload?) -> Unit = { _, _ -> }
) {
    companion object {
        private const val TAG = "TriggerPriority"

        private fun logD(tag: String, msg: String) {
            try {
                android.util.Log.d(tag, msg)
            } catch (_: Throwable) {
                // Ignore in local JVM unit test environment
            }
        }

        private fun logI(tag: String, msg: String) {
            try {
                android.util.Log.i(tag, msg)
            } catch (_: Throwable) {
                // Ignore in local JVM unit test environment
            }
        }
    }

    enum class TriggerType(val priority: Int) {
        NOTIFICATION(1),
        LOCK_UNLOCK_FLASH(2),
        CHARGING(3),
        SPECIAL_EVENT(4),
        AUDIO_REACTIVE(5),
        AMBIENT(6);

        fun isHigherPriorityThan(other: TriggerType?): Boolean {
            if (other == null) return true
            return this.priority < other.priority
        }
    }

    data class TriggerPayload(
        val color: Int? = null,
        val secondaryColor: Int? = null,
        val durationMs: Long? = null,
        val effectStyle: Int? = null,
        val brightness: Float = 1.0f,
        val tag: String = ""
    )

    private val activeTriggers = mutableMapOf<TriggerType, TriggerPayload>()

    @Synchronized
    fun setTriggerActive(type: TriggerType, active: Boolean, payload: TriggerPayload? = null): TriggerType? {
        val previousActive = resolveActiveTrigger()
        if (active) {
            activeTriggers[type] = payload ?: TriggerPayload()
            logD(TAG, "Trigger ACTIVATED: $type (prio=${type.priority}) | tag=${payload?.tag}")
        } else {
            activeTriggers.remove(type)
            logD(TAG, "Trigger DEACTIVATED: $type (prio=${type.priority})")
        }

        val newActive = resolveActiveTrigger()
        if (newActive != previousActive) {
            val activePayload = newActive?.let { activeTriggers[it] }
            logI(TAG, "Active trigger transitioned: $previousActive -> $newActive")
            onTriggerChanged(newActive, activePayload)
        }
        return newActive
    }

    @Synchronized
    fun resolveActiveTrigger(): TriggerType? {
        return activeTriggers.keys.minByOrNull { it.priority }
    }

    @Synchronized
    fun getPayload(type: TriggerType): TriggerPayload? {
        return activeTriggers[type]
    }

    @Synchronized
    fun isTriggerActive(type: TriggerType): Boolean {
        return activeTriggers.containsKey(type)
    }

    @Synchronized
    fun clearAll() {
        activeTriggers.clear()
        onTriggerChanged(null, null)
    }

    @Synchronized
    fun getActiveTriggersSnapshot(): Map<TriggerType, TriggerPayload> {
        return activeTriggers.toMap()
    }
}
