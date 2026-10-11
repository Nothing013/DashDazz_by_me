package com.dashdazz.light

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoTrackerTest {

    @Test
    fun `median of a single interval`() {
        val t = TempoTracker()
        t.push(500f)
        assertEquals(500f, t.medianMs(), 1e-4f)
    }

    @Test
    fun `median is the middle value not the mean`() {
        val t = TempoTracker()

        t.push(150f)
        t.push(200f)
        t.push(1200f)
        assertEquals(200f, t.medianMs(), 1e-4f)
    }

    @Test
    fun `even count median averages the two middle values`() {
        val t = TempoTracker()
        t.push(400f)
        t.push(600f)
        assertEquals(500f, t.medianMs(), 1e-4f)
    }

    @Test
    fun `spacing below 150ms is rejected as impossible`() {
        val t = TempoTracker()
        t.push(100f)
        assertEquals(0f, t.medianMs(), 1e-4f)
    }

    @Test
    fun `spacing above 2000ms is rejected as not a beat`() {
        val t = TempoTracker()
        t.push(2500f)
        assertEquals(0f, t.medianMs(), 1e-4f)
    }

    @Test
    fun `slow but regular intervals are accepted for plausibility scoring`() {
        val t = TempoTracker()
        t.push(1500f)
        assertEquals(1500f, t.medianMs(), 1e-4f)
    }

    @Test
    fun `boundaries are inclusive`() {
        val low = TempoTracker().apply { push(150f) }
        val high = TempoTracker().apply { push(2000f) }
        assertEquals(150f, low.medianMs(), 1e-4f)
        assertEquals(2000f, high.medianMs(), 1e-4f)
    }

    @Test
    fun `capacity caps at eight intervals`() {
        val t = TempoTracker()
        for (i in 1..20) t.push(400f)
        assertEquals(400f, t.medianMs(), 1e-4f)

        assertEquals(150f, t.bpm().toFloat(), 1e-4f)
    }

    @Test
    fun `bpm derives from the median interval`() {
        val t = TempoTracker()
        t.push(500f)
        assertEquals(120, t.bpm())
        t.reset()
        t.push(250f)
        assertEquals(240, t.bpm())
    }

    @Test
    fun `bpm is zero when unknown`() {
        assertEquals(0, TempoTracker().bpm())
    }

    @Test
    fun `spread is zero for identical intervals`() {
        val t = TempoTracker()
        for (i in 1..5) t.push(400f)
        assertEquals(0f, t.spreadMs(), 1e-3f)
    }

    @Test
    fun `spread grows with jitter`() {
        val steady = TempoTracker().apply { for (i in 1..5) push(400f) }
        val jittery = TempoTracker().apply {
            push(200f); push(400f); push(600f); push(400f); push(400f)
        }
        assertEquals(0f, steady.spreadMs(), 1e-3f)
        assertTrue(jittery.spreadMs() > steady.spreadMs())
    }

    @Test
    fun `spread is zero with fewer than two samples`() {
        assertEquals(0f, TempoTracker().spreadMs(), 1e-4f)
        val t = TempoTracker()
        t.push(400f)
        assertEquals(0f, t.spreadMs(), 1e-4f)
    }

    @Test
    fun `reset clears everything`() {
        val t = TempoTracker()
        t.push(400f)
        t.push(400f)
        t.reset()
        assertEquals(0f, t.medianMs(), 1e-4f)
        assertEquals(0, t.bpm())
        assertEquals(0f, t.spreadMs(), 1e-4f)
    }
}
