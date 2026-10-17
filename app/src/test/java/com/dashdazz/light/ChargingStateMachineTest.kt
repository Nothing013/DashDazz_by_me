package com.dashdazz.light

import org.junit.Assert.*
import org.junit.Test

class ChargingStateMachineTest {

    @Test
    fun testFastChargePulseSpeedIsFaster() {
        val fastStep = ChargingEngine.getPulseStepMs(isFastCharge = true)
        val standardStep = ChargingEngine.getPulseStepMs(isFastCharge = false)
        assertTrue(fastStep < standardStep)
        assertEquals(12L, fastStep)
        assertEquals(24L, standardStep)
    }

    @Test
    fun testGradientColorsTransitionAcrossPercentages() {
        val lowBatteryColor = ChargingEngine.getGradientColorForPercentage(10)
        val midBatteryColor = ChargingEngine.getGradientColorForPercentage(50)
        val fullBatteryColor = ChargingEngine.getGradientColorForPercentage(98)

        // Verify valid ARGB colors
        assertNotEquals(0, lowBatteryColor)
        assertNotEquals(0, midBatteryColor)
        assertNotEquals(0, fullBatteryColor)

        // Low battery should be reddish (high red component)
        val redComp = (lowBatteryColor shr 16) and 0xFF
        val blueComp = lowBatteryColor and 0xFF
        assertTrue(redComp > blueComp)
    }

    @Test
    fun testBatteryStateDefaultModel() {
        val state = ChargingEngine.BatteryState(
            isCharging = true,
            percentage = 85,
            isFastCharge = true,
            isFull = false,
            isLowBattery = false
        )
        assertTrue(state.isCharging)
        assertEquals(85, state.percentage)
        assertTrue(state.isFastCharge)
        assertFalse(state.isFull)
        assertFalse(state.isLowBattery)
    }
}
