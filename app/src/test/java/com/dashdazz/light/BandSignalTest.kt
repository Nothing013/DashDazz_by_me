package com.dashdazz.light

import com.dashdazz.light.AudioReactiveEngine.BAND_BROADBAND
import com.dashdazz.light.AudioReactiveEngine.BAND_COUNT
import com.dashdazz.light.AudioReactiveEngine.BAND_HAT
import com.dashdazz.light.AudioReactiveEngine.BAND_KICK
import com.dashdazz.light.AudioReactiveEngine.BAND_MANUAL
import com.dashdazz.light.AudioReactiveEngine.BAND_SNARE
import com.dashdazz.light.AudioReactiveEngine.BAND_VOCAL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BandSignalTest {

    private var clock = 0L

    private fun frame(s: BandSignal, raw: Float, dtMs: Float = 20f): Boolean {
        clock += dtMs.toLong()
        s.push(raw, dtMs)
        return s.detect(clock, threshold = 1.18f, refractoryMs = 150L)
    }

    private fun frames(s: BandSignal, raw: Float, count: Int): Int {
        var hits = 0
        repeat(count) { if (frame(s, raw)) hits++ }
        return hits
    }

    @Test
    fun `a band that is never fed data never fires - regression`() {
        val s = BandSignal(BAND_BROADBAND)
        var fired = false
        for (i in 0 until 100) {
            clock += 20L
            if (s.detect(clock, 1.18f, 150L)) fired = true
        }
        assertFalse("a band with no input must not produce beats", fired)
        assertEquals("envelope must stay at zero", 0f, s.value, 1e-6f)
    }

    @Test
    fun `fires on a clear onset above the recent baseline`() {
        val s = BandSignal(BAND_KICK, attackMs = 5f, releaseMs = 800f)
        frames(s, 0.4f, 20)
        frames(s, 0.1f, 60)
        val baselineHits = frames(s, 0.1f, 10)
        assertEquals("quiet level should not trip the gate", 0, baselineHits)

        assertTrue("onset should trip the gate (value=${s.value})", frame(s, 0.4f))
    }

    @Test
    fun `does not fire on a level sitting at its own peak`() {
        val s = BandSignal(BAND_SNARE, attackMs = 10f, releaseMs = 10f)

        val hits = frames(s, 0.5f, 80)
        assertEquals("a band parked at its own AGC peak must not fire", 0, hits)
    }

    @Test
    fun `respects the refractory period`() {
        val s = BandSignal(BAND_KICK, attackMs = 5f, releaseMs = 800f)
        frames(s, 0.4f, 20)
        frames(s, 0.1f, 60)

        assertTrue("first hit should fire", frame(s, 0.4f))

        clock += 20L
        frame(s, 0.1f)
        assertFalse("hit inside refractory must be suppressed", frame(s, 0.4f))
    }

    @Test
    fun `reset stops the band from firing`() {
        val s = BandSignal(BAND_VOCAL, attackMs = 5f, releaseMs = 800f)
        frames(s, 0.4f, 20)
        frames(s, 0.1f, 60)
        assertTrue(frame(s, 0.4f))

        s.reset()
        var fired = false
        repeat(20) { if (frame(s, 0.5f)) fired = true }
        assertFalse("reset must clear beat state", fired)
    }

    @Test
    fun `hits are counted only inside the window`() {
        val s = BandSignal(BAND_HAT, attackMs = 5f, releaseMs = 300f)
        var anyHit = false
        repeat(40) { if (frame(s, if (it % 10 == 0) 0.4f else 0.02f)) anyHit = true }
        assertTrue("the synthetic train should produce hits", anyHit)

        val lastHitAt = clock
        clock += 10_000L
        assertEquals("hits older than 4s must not count", 0, s.hitsWithin(4000L, clock))
        assertTrue(s.hitsWithin(20_000L, clock) >= 1)
        assertTrue("lastHitAt should be well before now", lastHitAt < clock)
    }

    @Test
    fun `a band that never fires scores zero`() {
        val s = BandSignal(BAND_BROADBAND)
        s.updateScore(clock)
        assertEquals(0f, s.score, 1e-6f)
    }

    @Test
    fun `a band idle beyond the cutoff scores zero`() {
        val s = BandSignal(BAND_KICK, attackMs = 5f, releaseMs = 300f)
        val train = beatTrain(gaps = List(6) { 500L })
        assertTrue(train.score > 0f)

        clock += BS_AUTO_IDLE_MS + 1
        train.updateScore(clock)
        assertEquals("idle band must score zero", 0f, train.score, 1e-6f)
    }

    @Test
    fun `steady tempo scores higher than jittery tempo at the same rate`() {

        val steady = beatTrain(gaps = List(8) { 500L })
        val jittery = beatTrain(gaps = List(8) { if (it % 2 == 0) 300L else 700L })

        assertTrue(
            "steady=$steady.score should beat jittery=$jittery.score",
            steady.score > jittery.score
        )
    }

    @Test
    fun `real beat tempo scores higher than a slow regular blip`() {

        val beat = beatTrain(gaps = List(10) { 500L })

        val slow = beatTrain(gaps = List(4) { 3000L })

        assertTrue(
            "beat score (${beat.score}) must beat the slow blip (${slow.score})",
            beat.score > slow.score
        )
    }

    @Test
    fun `a fast blip is rate-limited by the refractory floor`() {

        val fast = beatTrain(gaps = List(10) { 60L })
        val elapsed = clock
        val hits = fast.hitsWithin(60_000L, elapsed)

        assertTrue(
            "refractory should clamp 10 nominal beats down, got $hits over ${elapsed}ms",
            hits in 1..4
        )
    }

    @Test
    fun `plausibility window matches the supported BPM range`() {
        assertEquals(120, TempoTracker().apply { push(500f) }.bpm())
        assertEquals(240, TempoTracker().apply { push(250f) }.bpm())
        assertEquals(60, TempoTracker().apply { push(1000f) }.bpm())
    }

    @Test
    fun `band ids used by auto mode are distinct and in range`() {
        val ids = setOf(BAND_BROADBAND, BAND_KICK, BAND_SNARE, BAND_VOCAL, BAND_HAT)
        assertEquals(5, ids.size)
        assertEquals(BAND_COUNT, ids.size)
        assertTrue(BAND_MANUAL >= BAND_COUNT)
    }

    private fun beatTrain(
        gaps: List<Long>,
        id: Int = BAND_KICK,
        loud: Float = 1f,
        quiet: Float = 0.02f
    ): BandSignal {
        val s = BandSignal(id, attackMs = 5f, releaseMs = 300f)
        clock = 0L
        for (gap in gaps) {
            val quietSteps = ((gap - REFRACTORY_MS) / 20L).toInt().coerceAtLeast(1)
            repeat(quietSteps) { frame(s, quiet) }
            frame(s, loud)
        }
        s.updateScore(clock)
        return s
    }

    companion object {
        private const val REFRACTORY_MS = 20L
    }
}
