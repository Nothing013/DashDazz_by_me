package com.dashdazz.light

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BandTrackerTest {

    private val eps = 1e-4f

    @Test
    fun `rises toward the input rather than jumping to it`() {
        val t = BandTracker(attackMs = 100f, releaseMs = 100f)
        t.push(1f, 10f)
        assertTrue("envelope should attack gradually", t.value > 0f && t.value < 1f)
    }

    @Test
    fun `converges to a sustained input`() {
        val t = BandTracker(attackMs = 100f, releaseMs = 100f)
        for (i in 0 until 50) t.push(1f, 20f)
        assertEquals(1f, t.value, eps)
    }

    @Test
    fun `attack is faster than release`() {
        val t = BandTracker(attackMs = 20f, releaseMs = 200f)

        val before = t.value
        t.push(1f, 20f)
        val attackRise = t.value - before

        t.reset()
        t.push(1f, 200f)
        val settled = t.value
        t.push(0f, 20f)
        val releaseFall = settled - t.value

        assertTrue(
            "attack rise=$attackRise should exceed release fall=$releaseFall",
            attackRise > releaseFall
        )
    }

    @Test
    fun `output stays within zero and one`() {
        val t = BandTracker()
        t.push(5f, 10f)
        assertTrue(t.value <= 1f)
        t.push(-3f, 10f)
        assertTrue(t.value >= 0f)
    }

    @Test
    fun `AGC normalises a sustained loud input to full scale`() {
        val t = BandTracker(attackMs = 20f, releaseMs = 20f, agcTauMs = 9000f)
        for (i in 0 until 200) t.push(0.4f, 20f)

        assertEquals(1f, t.value, 0.01f)
    }

    @Test
    fun `a single loud blip does not permanently shrink the scale`() {
        val t = BandTracker(attackMs = 20f, releaseMs = 20f, agcTauMs = 9000f)
        for (i in 0 until 100) t.push(0.2f, 20f)
        val baseline = t.value

        t.push(1f, 20f)
        for (i in 0 until 5) t.push(0.2f, 20f)

        assertTrue("level after spike ($t) should be below pre-spike ($baseline)",
            t.value < baseline)
        for (i in 0 until 1500) t.push(0.2f, 20f)
        assertEquals("level should recover as AGC peak decays", baseline, t.value, 0.01f)
    }

    @Test
    fun `frame rate does not change the trajectory`() {

        fun runOnce(stepMs: Float, steps: Int): Float {
            val t = BandTracker(attackMs = 40f, releaseMs = 200f, agcTauMs = 9000f)
            for (i in 0 until steps) t.push(1f, stepMs)
            return t.value
        }

        val coarse = runOnce(20f, 20)
        val fine = runOnce(8f, 50)
        assertEquals(coarse, fine, 0.02f)
    }

    @Test
    fun `a constant pre-AGC multiplier cancels out - the old gain bug`() {

        val plain = BandTracker(attackMs = 20f, releaseMs = 20f, agcTauMs = 9000f)
        val doubled = BandTracker(attackMs = 20f, releaseMs = 20f, agcTauMs = 9000f)
        for (i in 0 until 60) {
            plain.push(0.3f, 20f)
            doubled.push(0.6f, 20f)
        }
        assertEquals(
            "2x input must converge to the same normalised level",
            plain.value, doubled.value, 0.02f
        )
    }

    @Test
    fun `auto-gain leaves headroom above a steady level`() {
        val t = BandTracker(attackMs = 20f, releaseMs = 20f, agcTauMs = 9000f)

        for (i in 0 until 400) t.push(0.1f, 20f, autoGain = true)
        assertTrue("a steady band should be pulled back to leave headroom", t.gainScale < 1f)
        assertTrue(
            "typical level should land near the target (was ${t.value})",
            t.value in 0.15f..0.6f
        )
    }

    @Test
    fun `auto-gain lifts a spiky band whose typical level is low`() {
        val t = BandTracker(attackMs = 20f, releaseMs = 20f, agcTauMs = 9000f)

        var i = 0
        while (i < 600) {
            t.push(0.02f, 20f, autoGain = true)
            if (i % 20 == 0) t.push(1f, 20f, autoGain = true)
            i++
        }
        assertTrue("a spiky band should be lifted back up", t.gainScale > 1f)
    }

    @Test
    fun `auto-gain pulls a hot band back down`() {
        val t = BandTracker(attackMs = 20f, releaseMs = 20f, agcTauMs = 9000f)

        for (i in 0 until 200) t.push(1f, 20f, autoGain = true)
        for (i in 0 until 400) t.push(0.2f, 20f, autoGain = true)
        assertTrue("auto-gain should scale a hot reference down", t.gainScale < 1f)
    }

    @Test
    fun `auto-gain scale stays inside its bounds`() {
        val t = BandTracker(attackMs = 10f, releaseMs = 10f, agcTauMs = 9000f)
        for (i in 0 until 1000) t.push(0.001f, 20f, autoGain = true)
        assertTrue("scale must not explode on near-silence", t.gainScale <= 4f + 1e-3f)
        for (i in 0 until 1000) t.push(1f, 20f, autoGain = true)
        assertTrue("scale must not collapse on saturation", t.gainScale >= 0.25f - 1e-3f)
    }

    @Test
    fun `auto-gain off reports unity scale`() {
        val t = BandTracker()
        for (i in 0 until 50) t.push(0.5f, 20f)
        assertEquals(1f, t.gainScale, 1e-6f)
        t.reset()
        assertEquals(1f, t.gainScale, 1e-6f)
    }

    @Test
    fun `reset returns to silence`() {
        val t = BandTracker(attackMs = 20f, releaseMs = 20f)
        for (i in 0 until 10) t.push(1f, 20f)
        assertTrue(t.value > 0f)
        t.reset()
        assertEquals(0f, t.value, eps)
    }
}
