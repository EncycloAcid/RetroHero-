package com.retrohero.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class ChallengeSnapshotTest {

    @Test
    fun ghostRleCodecRoundTripsAndCompressesHeavily() {
        val frames = buildList {
            repeat(600) { add(GhostFrame(1 shl RetroPad.RIGHT)) }
            repeat(120) { add(GhostFrame((1 shl RetroPad.RIGHT) or (1 shl RetroPad.B))) }
            repeat(480) { add(GhostFrame(0)) }
        }
        val encoded = ChallengeGhost.encode(frames)
        assertTrue("1200 frames should RLE-compress under 64 bytes (got ${encoded.size})", encoded.size < 64)
        val decoded = ChallengeGhost.decode(encoded)
        assertEquals(frames, decoded)
    }

    @Test
    fun ramWinConditionEvaluates8BitAnd16BitValues() {
        val ram = ByteArray(1024)
        ram[0x40] = 0 // Boss HP == 0
        ram[0x80] = 0x20; ram[0x81] = 0x01 // 16-bit LE = 0x0120 (288)

        val bossDead = ChallengeWinCondition(
            type = ChallengeWinType.RAM_GOAL,
            ramOffset = 0x40,
            ramSize = 1,
            op = ChallengeRamOp.EQ,
            targetValue = 0,
        )
        assertTrue(bossDead.evaluateRam(ram))
        ram[0x40] = 5
        assertFalse(bossDead.evaluateRam(ram))

        val scoreGoal = ChallengeWinCondition(
            type = ChallengeWinType.RAM_GOAL,
            ramOffset = 0x80,
            ramSize = 2,
            op = ChallengeRamOp.GTE,
            targetValue = 250,
        )
        assertTrue(scoreGoal.evaluateRam(ram))
    }

    @Test
    fun momentCapsuleV2RoundTripsGhostAndWinCondition() {
        val tmpDir = Files.createTempDirectory("rhclip-v2-test").toFile()
        try {
            val stateFile = File(tmpDir, "state.bin").apply { writeBytes(ByteArray(256) { it.toByte() }) }
            val ghostFrames = List(180) { GhostFrame(if (it % 20 < 10) (1 shl RetroPad.A) else 0) }
            val ghostFile = File(tmpDir, "ghost.bin").apply { writeBytes(ChallengeGhost.encode(ghostFrames)) }
            val win = ChallengeWinCondition(
                type = ChallengeWinType.RAM_GOAL,
                ramOffset = 0x10,
                ramSize = 1,
                op = ChallengeRamOp.EQ,
                targetValue = 0,
                label = "Defeat Ridley",
            )
            val manifest = MomentManifest(
                title = "Ridley Boss Rush",
                note = "Beat my 14.2s ghost!",
                systemId = "snes",
                coreId = "snes9x",
                gameTitle = "Super Metroid",
                gameBaseName = "Super Metroid (USA)",
                ghostFrames = ghostFrames.size,
                targetDurationMs = 14_200L,
                winCondition = win,
            )

            val zipOut = ByteArrayOutputStream()
            MomentCapsule.export(zipOut, manifest, stateFile, previewFile = null, ghostFile = ghostFile)

            val extractDir = File(tmpDir, "out")
            val extracted = MomentCapsule.extract(ByteArrayInputStream(zipOut.toByteArray()), extractDir)
            assertEquals("Ridley Boss Rush", extracted.manifest.title)
            assertEquals(180, extracted.manifest.ghostFrames)
            assertEquals(14_200L, extracted.manifest.targetDurationMs)
            assertEquals(win, extracted.manifest.winCondition)
            assertNotNull(extracted.ghostFile)
            assertEquals(ghostFrames, ChallengeGhost.decode(extracted.ghostFile!!.readBytes()))
        } finally {
            tmpDir.deleteRecursively()
        }
    }
}
