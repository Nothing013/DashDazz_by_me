package com.dashdazz.light

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TriggerPriorityManagerTest {

    private lateinit var manager: TriggerPriorityManager
    private var lastTransitionFrom: TriggerPriorityManager.TriggerType? = null
    private var lastTransitionTo: TriggerPriorityManager.TriggerType? = null

    @Before
    fun setUp() {
        manager = TriggerPriorityManager { newActive, _ ->
            lastTransitionTo = newActive
        }
    }

    @Test
    fun testAmbientBaselineActivation() {
        val active = manager.setTriggerActive(TriggerPriorityManager.TriggerType.AMBIENT, true)
        assertEquals(TriggerPriorityManager.TriggerType.AMBIENT, active)
        assertEquals(TriggerPriorityManager.TriggerType.AMBIENT, manager.resolveActiveTrigger())
    }

    @Test
    fun testNotificationOverridesAmbientAndCharging() {
        manager.setTriggerActive(TriggerPriorityManager.TriggerType.AMBIENT, true)
        manager.setTriggerActive(TriggerPriorityManager.TriggerType.CHARGING, true)
        assertEquals(TriggerPriorityManager.TriggerType.CHARGING, manager.resolveActiveTrigger())

        // Notification arrives - priority level 1
        manager.setTriggerActive(
            TriggerPriorityManager.TriggerType.NOTIFICATION,
            true,
            TriggerPriorityManager.TriggerPayload(color = 0xFFFF0000.toInt(), tag = "CallAlert")
        )
        assertEquals(TriggerPriorityManager.TriggerType.NOTIFICATION, manager.resolveActiveTrigger())
        assertEquals("CallAlert", manager.getPayload(TriggerPriorityManager.TriggerType.NOTIFICATION)?.tag)

        // Notification ends - returns cleanly to Charging
        manager.setTriggerActive(TriggerPriorityManager.TriggerType.NOTIFICATION, false)
        assertEquals(TriggerPriorityManager.TriggerType.CHARGING, manager.resolveActiveTrigger())

        // Charging ends - returns to Ambient
        manager.setTriggerActive(TriggerPriorityManager.TriggerType.CHARGING, false)
        assertEquals(TriggerPriorityManager.TriggerType.AMBIENT, manager.resolveActiveTrigger())
    }

    @Test
    fun testLockUnlockFlashPriorityOverAudio() {
        manager.setTriggerActive(TriggerPriorityManager.TriggerType.AUDIO_REACTIVE, true)
        assertEquals(TriggerPriorityManager.TriggerType.AUDIO_REACTIVE, manager.resolveActiveTrigger())

        // Screen unlock flash fires (Level 2)
        manager.setTriggerActive(TriggerPriorityManager.TriggerType.LOCK_UNLOCK_FLASH, true)
        assertEquals(TriggerPriorityManager.TriggerType.LOCK_UNLOCK_FLASH, manager.resolveActiveTrigger())

        // Unlock flash ends - seamlessly resumes audio reactive
        manager.setTriggerActive(TriggerPriorityManager.TriggerType.LOCK_UNLOCK_FLASH, false)
        assertEquals(TriggerPriorityManager.TriggerType.AUDIO_REACTIVE, manager.resolveActiveTrigger())
    }

    @Test
    fun testClearAll() {
        manager.setTriggerActive(TriggerPriorityManager.TriggerType.NOTIFICATION, true)
        manager.setTriggerActive(TriggerPriorityManager.TriggerType.CHARGING, true)
        manager.clearAll()
        assertNull(manager.resolveActiveTrigger())
        assertTrue(manager.getActiveTriggersSnapshot().isEmpty())
    }
}
