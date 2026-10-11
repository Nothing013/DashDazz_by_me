package com.dashdazz.light

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatDetectorTest {

    @Test
    fun `average covers every sample inside the window`() {
        val d = BeatDetector()
        d.push(1f, 1000L)
        d.push(3f, 1100L)
        assertEquals(2f, d.averageWithin(1000L, 1100L), 1e-4f)
    }

    @Test
    fun `samples older than the window are excluded`() {
        val d = BeatDetector()
        d.push(10f, 0L)
        d.push(2f, 9000L)
        assertEquals(2f, d.averageWithin(500L, 9000L), 1e-4f)
    }

    @Test
    fun `empty detector reports zero`() {
        assertEquals(0f, BeatDetector().averageWithin(1000L, 5000L), 1e-4f)
    }

    @Test
    fun `ring buffer wraps without losing recent samples`() {
        val d = BeatDetector(capacity = 4)
        for (i in 1..10) d.push(i.toFloat(), i * 100L)

        assertEquals(8.5f, d.averageWithin(400L, 1000L), 1e-4f)
    }

    @Test
    fun `reset empties the history`() {
        val d = BeatDetector()
        d.push(5f, 1000L)
        d.reset()
        assertEquals(0f, d.averageWithin(10_000L, 5000L), 1e-4f)
    }

    @Test
    fun `baseline follows a ramp, which is what keeps gating stable`() {
        val d = BeatDetector()
        var now = 0L

        for (i in 1..40) {
            now += 20L
            d.push(i / 40f, now)
        }
        val baseline = d.averageWithin(1000L, now)
        assertTrue("baseline should track the ramp, was $baseline", baseline > 0.4f)
    }
}
