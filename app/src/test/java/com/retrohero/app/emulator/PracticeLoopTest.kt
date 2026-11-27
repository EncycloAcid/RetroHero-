package com.retrohero.app.emulator

import com.retrohero.app.data.RetroPad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeLoopTest {

    @Test
    fun splitAndDeltaFormattingIsReadableAndAccurate() {
        assertEquals("0.00s", PracticeLoopMath.formatSplit(0L))
        assertEquals("12.45s", PracticeLoopMath.formatSplit(12_450L))
        assertEquals("1:04.22", PracticeLoopMath.formatSplit(64_220L))

        assertEquals("-0.84s", PracticeLoopMath.formatDelta(-840L))
        assertEquals("+1.15s", PracticeLoopMath.formatDelta(1_150L))
        assertEquals("-0.00s", PracticeLoopMath.formatDelta(0L))
    }

    @Test
    fun autoResetTriggersOnlyWhenTimerIsEnabledAndReached() {
        assertFalse(PracticeLoopMath.shouldAutoReset(99_999L, autoResetSeconds = 0))
        assertFalse(PracticeLoopMath.shouldAutoReset(14_950L, autoResetSeconds = 15))
        assertTrue(PracticeLoopMath.shouldAutoReset(15_000L, autoResetSeconds = 15))
        assertTrue(PracticeLoopMath.shouldAutoReset(15_250L, autoResetSeconds = 15))
    }

    @Test
    fun binarySearchGhostSamplerReturnsExactMaskAtAnyTimestamp() {
        val jumpMask = 1 shl RetroPad.A
        val rightJumpMask = (1 shl RetroPad.RIGHT) or (1 shl RetroPad.A)
        val frames = listOf(
            GhostSample(0L, 0),
            GhostSample(500L, jumpMask),
            GhostSample(1200L, rightJumpMask),
            GhostSample(2000L, 0),
        )

        assertEquals(0, PracticeLoopMath.sampleGhostMask(emptyList(), 500L))
        assertEquals(0, PracticeLoopMath.sampleGhostMask(frames, 250L))
        assertEquals(jumpMask, PracticeLoopMath.sampleGhostMask(frames, 500L))
        assertEquals(jumpMask, PracticeLoopMath.sampleGhostMask(frames, 1199L))
        assertEquals(rightJumpMask, PracticeLoopMath.sampleGhostMask(frames, 1200L))
        assertEquals(rightJumpMask, PracticeLoopMath.sampleGhostMask(frames, 1999L))
        assertEquals(0, PracticeLoopMath.sampleGhostMask(frames, 2500L))
    }

    @Test
    fun recordClearTracksPersonalBestDeltaAndPromotesFastestGhost() {
        val ghost1 = listOf(GhostSample(100L, 1 shl RetroPad.B))
        val ghost2 = listOf(GhostSample(100L, 1 shl RetroPad.A))
        val ghost3 = listOf(GhostSample(100L, 1 shl RetroPad.Y))

        // First clear sets initial PB and stores ghost1
        val initial = PracticeLoopState(active = true, attempts = 3, elapsedMs = 15_000L)
        val afterFirst = PracticeLoopMath.recordClear(initial, ghost1)
        assertEquals(1, afterFirst.clears)
        assertEquals(15_000L, afterFirst.bestSplitMs)
        assertNull(afterFirst.lastDeltaMs)
        assertTrue(afterFirst.justHitPb)
        assertEquals(ghost1, afterFirst.pbGhostFrames)

        // Retry increments attempt counter and clears elapsedMs / justHitPb
        val retry = PracticeLoopMath.startRetry(afterFirst)
        assertEquals(4, retry.attempts)
        assertEquals(0L, retry.elapsedMs)
        assertFalse(retry.justHitPb)

        // Slower clear does NOT overwrite PB or PB ghost
        val slower = PracticeLoopMath.recordClear(retry.copy(elapsedMs = 16_200L), ghost2)
        assertEquals(2, slower.clears)
        assertEquals(15_000L, slower.bestSplitMs)
        assertEquals(1_200L, slower.lastDeltaMs)
        assertFalse(slower.justHitPb)
        assertEquals(ghost1, slower.pbGhostFrames)

        // Faster clear beats PB, records negative delta, and promotes ghost3
        val faster = PracticeLoopMath.recordClear(slower.copy(elapsedMs = 13_500L), ghost3)
        assertEquals(3, faster.clears)
        assertEquals(13_500L, faster.bestSplitMs)
        assertEquals(-1_500L, faster.lastDeltaMs)
        assertTrue(faster.justHitPb)
        assertEquals(ghost3, faster.pbGhostFrames)
    }
}
