package com.dashdazz.light

import org.junit.Assert.*
import org.junit.Test

class QuietHoursTest {

    @Test
    fun testOvernightQuietHoursActiveDuringLateNight() {
        // Schedule: 23:00 to 07:00
        val isQuiet1 = SystemAutomationsManager.isWithinQuietHours(
            currentHour = 23, currentMinute = 30,
            startHour = 23, startMinute = 0,
            endHour = 7, endMinute = 0
        )
        assertTrue(isQuiet1)

        val isQuiet2 = SystemAutomationsManager.isWithinQuietHours(
            currentHour = 3, currentMinute = 15,
            startHour = 23, startMinute = 0,
            endHour = 7, endMinute = 0
        )
        assertTrue(isQuiet2)

        val isQuiet3 = SystemAutomationsManager.isWithinQuietHours(
            currentHour = 6, currentMinute = 59,
            startHour = 23, startMinute = 0,
            endHour = 7, endMinute = 0
        )
        assertTrue(isQuiet3)
    }

    @Test
    fun testOvernightQuietHoursInactiveDuringDay() {
        // Schedule: 23:00 to 07:00
        val isQuietNoon = SystemAutomationsManager.isWithinQuietHours(
            currentHour = 12, currentMinute = 0,
            startHour = 23, startMinute = 0,
            endHour = 7, endMinute = 0
        )
        assertFalse(isQuietNoon)

        val isQuietEvening = SystemAutomationsManager.isWithinQuietHours(
            currentHour = 22, currentMinute = 30,
            startHour = 23, startMinute = 0,
            endHour = 7, endMinute = 0
        )
        assertFalse(isQuietEvening)
    }

    @Test
    fun testSameDayQuietHours() {
        // Schedule: 13:00 to 15:00
        val inRange = SystemAutomationsManager.isWithinQuietHours(
            currentHour = 14, currentMinute = 0,
            startHour = 13, startMinute = 0,
            endHour = 15, endMinute = 0
        )
        assertTrue(inRange)

        val outOfRange = SystemAutomationsManager.isWithinQuietHours(
            currentHour = 16, currentMinute = 0,
            startHour = 13, startMinute = 0,
            endHour = 15, endMinute = 0
        )
        assertFalse(outOfRange)
    }

    @Test
    fun testDisabledScheduleWhenStartEqualsEnd() {
        val disabled = SystemAutomationsManager.isWithinQuietHours(
            currentHour = 10, currentMinute = 0,
            startHour = 8, startMinute = 0,
            endHour = 8, endMinute = 0
        )
        assertFalse(disabled)
    }
}
