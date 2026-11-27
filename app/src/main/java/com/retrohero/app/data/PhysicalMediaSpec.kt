package com.retrohero.app.data

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/** Authentic physical retail box casing archetype per platform (modeled after LaunchBox 3D Box Settings). */
enum class RetailCaseStyle(
    val label: String,
    val defaultAspect: Float,
    val depthRatio: Float,
    val hasPlasticRim: Boolean,
    val glossyWrap: Boolean,
) {
    CARDBOARD_VERTICAL("Cardboard Retail Box", defaultAspect = 0.72f, depthRatio = 0.17f, hasPlasticRim = false, glossyWrap = false),
    CARDBOARD_LANDSCAPE("Wide Cardboard Box", defaultAspect = 1.38f, depthRatio = 0.18f, hasPlasticRim = false, glossyWrap = false),
    CARDBOARD_SQUARE("Handheld Cardboard Box", defaultAspect = 0.98f, depthRatio = 0.19f, hasPlasticRim = false, glossyWrap = false),
    CLAMSHELL_PLASTIC("Molded Clamshell Case", defaultAspect = 0.71f, depthRatio = 0.15f, hasPlasticRim = true, glossyWrap = true),
    JEWEL_CASE_CD("Acrylic CD Jewel Case", defaultAspect = 1.02f, depthRatio = 0.09f, hasPlasticRim = true, glossyWrap = true),
    KEEP_CASE_DVD("Retail Keep Case", defaultAspect = 0.71f, depthRatio = 0.10f, hasPlasticRim = true, glossyWrap = true),
    HANDHELD_SNAP_CASE("Handheld Snap Case", defaultAspect = 0.89f, depthRatio = 0.12f, hasPlasticRim = true, glossyWrap = true),
    ARCADE_SHOCKBOX("Arcade Shockbox", defaultAspect = 0.72f, depthRatio = 0.16f, hasPlasticRim = true, glossyWrap = true),
}

/** Authentic physical game medium inside the retail case (cartridge pak, game card, or optical disc). */
enum class PhysicalMediaKind(
    val tabLabel: String,
    val isOpticalDisc: Boolean,
    val defaultAspect: Float,
    val thicknessRatio: Float,
) {
    CART_NES("Cartridge", isOpticalDisc = false, defaultAspect = 0.86f, thicknessRatio = 0.11f),
    CART_SNES("Cartridge", isOpticalDisc = false, defaultAspect = 1.36f, thicknessRatio = 0.12f),
    CART_N64("Cartridge", isOpticalDisc = false, defaultAspect = 1.24f, thicknessRatio = 0.13f),
    CART_GAMEBOY("Cartridge", isOpticalDisc = false, defaultAspect = 0.88f, thicknessRatio = 0.10f),
    CART_GBA("Cartridge", isOpticalDisc = false, defaultAspect = 1.56f, thicknessRatio = 0.11f),
    CART_DS_SWITCH("Game Card", isOpticalDisc = false, defaultAspect = 0.90f, thicknessRatio = 0.06f),
    CART_SEGA("Cartridge", isOpticalDisc = false, defaultAspect = 1.32f, thicknessRatio = 0.11f),
    CART_HUCARD("Game Card", isOpticalDisc = false, defaultAspect = 0.68f, thicknessRatio = 0.04f),
    CART_ARCADE_PCB("ROM Board", isOpticalDisc = false, defaultAspect = 1.25f, thicknessRatio = 0.08f),
    OPTICAL_DISC("Disc", isOpticalDisc = true, defaultAspect = 1.0f, thicknessRatio = 0.025f),
    MINI_DISC_GC("Mini-Disc", isOpticalDisc = true, defaultAspect = 1.0f, thicknessRatio = 0.028f),
    UMD_DISC("UMD Disc", isOpticalDisc = true, defaultAspect = 1.0f, thicknessRatio = 0.055f),
}

/**
 * Measured size of a retail game case (North American release unless noted), standing upright:
 * width × height × depth in millimetres. These drive the case's shape in 3D, its spine thickness
 * and its height on the Physical Shelf.
 */
data class RealCaseDimensions(val widthMm: Float, val heightMm: Float, val depthMm: Float, val source: String) {
    /** Front width / height. */
    val aspect: Float get() = widthMm / heightMm
    /** Depth relative to the case's long side, as the 3D model measures it. */
    val depthRatio: Float get() = depthMm / maxOf(widthMm, heightMm)
    /** Share of a LaunchBox `Box - Full` wraparound scan ([back | spine | front]) that is the spine. */
    val wrapSpineFraction: Float get() = depthMm / (2f * widthMm + depthMm)

    companion object {
        private const val KEEP_CASE = "Wikipedia, Keep case (Wii/Wii U 190x135x14 mm); retail PS2 case listings"
        private const val JEWEL = "Standard CD jewel case, 142x125x10.4 mm (orphiq, cdrom2go)"

        /** Null for systems without a reliable measurement; those keep their case style's defaults. */
        fun forSystem(systemId: String): RealCaseDimensions? = when (systemId.lowercase()) {
            "nes" -> RealCaseDimensions(127f, 178f, 25f, "NES box protectors, 5 x 7 x 1 in (retroprotection, BOXPRO)")
            "snes", "n64" -> RealCaseDimensions(179f, 126f, 27f, "SNES/N64 NA box, 7 1/16 x 4 14/16 x ~1 in (NintendoAge forum; Pacsam protectors)")
            "gb", "gbc", "gba" -> RealCaseDimensions(127f, 127f, 25f, "Game Boy family box protectors, 5 x 5 x 1 in (BOXPRO, Bags Unlimited)")
            "nds" -> RealCaseDimensions(122f, 135f, 15f, "Wikipedia, Keep case: Nintendo DS (NA/JP) 135x122x15 mm, black")
            "3ds" -> RealCaseDimensions(122f, 135f, 12f, "Wikipedia, Keep case: Nintendo 3DS 135x122x12 mm, white")
            "switch" -> RealCaseDimensions(104f, 170f, 10f, "Wikipedia, Keep case: Nintendo Switch 170x104x10 mm")
            "psp" -> RealCaseDimensions(99f, 168f, 14f, "Wikipedia, Keep case: PlayStation Portable 168x99x14 mm")
            "psvita" -> RealCaseDimensions(105f, 135f, 12f, "Wikipedia, Keep case: PlayStation Vita 135x105x12 mm")
            "gc", "wii", "wiiu", "ps2" -> RealCaseDimensions(135f, 190f, 14f, KEEP_CASE)
            "genesis", "32x", "sms", "sg1000" -> RealCaseDimensions(128f, 198f, 25f, "Genesis clamshell, ~128x197-200x25 mm (protector listings)")
            "segacd", "saturn" -> RealCaseDimensions(144f, 206f, 21f, "Sega CD / Saturn NA case, 5 11/16 x 8 1/8 x 13/16 in (Sega-Age forum)")
            "psx", "dreamcast" -> RealCaseDimensions(142f, 125f, 10.4f, JEWEL)
            else -> null
        }
    }
}

/** Platform-specific physical case + cartridge/disc styling resolved for any RetroHero console. */
data class PhysicalMediaSpec(
    val systemId: String,
    val caseStyle: RetailCaseStyle,
    val mediaKind: PhysicalMediaKind,
    val casePlasticColor: Color,
    val mediaShellColor: Color,
    val spineHeaderColor: Color,
    /** PS1 iconic black polycarbonate data underside; PS2 early indigo tint; standard rainbow mirror otherwise. */
    val blackDiscUnderside: Boolean = false,
    val customAspectOverride: Float? = null,
) {
    /** Measured case size for this system, when known. */
    val realDimensions: RealCaseDimensions? get() = RealCaseDimensions.forSystem(systemId)
    /** Front width / height: measured when known, else the case style's typical shape. */
    val defaultBoxAspect: Float get() = realDimensions?.aspect ?: customAspectOverride ?: caseStyle.defaultAspect
    /** Case depth relative to its long side: measured when known, else the case style's typical depth. */
    val depthRatio: Float get() = realDimensions?.depthRatio ?: caseStyle.depthRatio
    /** Spine share of a LaunchBox `Box - Full` wraparound scan. */
    val fullWrapSpineFraction: Float get() =
        realDimensions?.wrapSpineFraction ?: (caseStyle.depthRatio * 0.55f).coerceIn(0.05f, 0.14f)

    companion object {
        fun forSystem(systemId: String, fallbackAccent: Color = Color(0xFF6750A4)): PhysicalMediaSpec = when (systemId) {
            // Nintendo Cartridge & Handheld Systems
            "nes" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CARDBOARD_VERTICAL,
                mediaKind = PhysicalMediaKind.CART_NES,
                casePlasticColor = Color(0xFF1C1C20),
                mediaShellColor = Color(0xFFA8A9AD),
                spineHeaderColor = Color(0xFFE52521),
            )
            "snes" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CARDBOARD_LANDSCAPE,
                mediaKind = PhysicalMediaKind.CART_SNES,
                casePlasticColor = Color(0xFF1A181E),
                mediaShellColor = Color(0xFFB7B5BE),
                spineHeaderColor = Color(0xFFE52521),
            )
            "n64" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CARDBOARD_LANDSCAPE,
                mediaKind = PhysicalMediaKind.CART_N64,
                casePlasticColor = Color(0xFF1E1E22),
                mediaShellColor = Color(0xFF9EA1A6),
                spineHeaderColor = Color(0xFFE52521),
            )
            "vb" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CARDBOARD_LANDSCAPE,
                mediaKind = PhysicalMediaKind.CART_GAMEBOY,
                casePlasticColor = Color(0xFF1A0D10),
                mediaShellColor = Color(0xFF232327),
                spineHeaderColor = Color(0xFFD50000),
            )
            "gb" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CARDBOARD_SQUARE,
                mediaKind = PhysicalMediaKind.CART_GAMEBOY,
                casePlasticColor = Color(0xFF25262B),
                mediaShellColor = Color(0xFF9B9E99),
                spineHeaderColor = Color(0xFF5C6BC0),
            )
            "gbc" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CARDBOARD_SQUARE,
                mediaKind = PhysicalMediaKind.CART_GAMEBOY,
                casePlasticColor = Color(0xFF202228),
                mediaShellColor = Color(0xFF2B2D34),
                spineHeaderColor = Color(0xFF7B1FA2),
            )
            "gba" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CARDBOARD_SQUARE,
                mediaKind = PhysicalMediaKind.CART_GBA,
                casePlasticColor = Color(0xFF1E2030),
                mediaShellColor = Color(0xFF474B54),
                spineHeaderColor = Color(0xFF3949AB),
            )
            "nds" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.HANDHELD_SNAP_CASE,
                mediaKind = PhysicalMediaKind.CART_DS_SWITCH,
                casePlasticColor = Color(0xFF2B2D32),
                mediaShellColor = Color(0xFF35383F),
                spineHeaderColor = Color(0xFFECEFF1),
                customAspectOverride = 0.89f,
            )
            "3ds" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.HANDHELD_SNAP_CASE,
                mediaKind = PhysicalMediaKind.CART_DS_SWITCH,
                casePlasticColor = Color(0xFFECEFF4),
                mediaShellColor = Color(0xFFE0E3E9),
                spineHeaderColor = Color(0xFFD50000),
                customAspectOverride = 0.90f,
            )
            "switch" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.HANDHELD_SNAP_CASE,
                mediaKind = PhysicalMediaKind.CART_DS_SWITCH,
                casePlasticColor = Color(0xFFC61A24),
                mediaShellColor = Color(0xFF282A30),
                spineHeaderColor = Color(0xFFE60012),
                customAspectOverride = 0.62f,
            )

            // Nintendo Optical Disc Systems
            "gc" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.KEEP_CASE_DVD,
                mediaKind = PhysicalMediaKind.MINI_DISC_GC,
                casePlasticColor = Color(0xFF26262E),
                mediaShellColor = Color(0xFF6A5ACD),
                spineHeaderColor = Color(0xFF5E35B1),
            )
            "wii" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.KEEP_CASE_DVD,
                mediaKind = PhysicalMediaKind.OPTICAL_DISC,
                casePlasticColor = Color(0xFFF2F4F8),
                mediaShellColor = Color(0xFFECEFF4),
                spineHeaderColor = Color(0xFF00B0FF),
            )
            "wiiu" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.KEEP_CASE_DVD,
                mediaKind = PhysicalMediaKind.OPTICAL_DISC,
                casePlasticColor = Color(0xFF0091EA),
                mediaShellColor = Color(0xFF0091EA),
                spineHeaderColor = Color(0xFF0091EA),
            )

            // Sega Systems
            "genesis", "32x" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CLAMSHELL_PLASTIC,
                mediaKind = PhysicalMediaKind.CART_SEGA,
                casePlasticColor = Color(0xFF18191E),
                mediaShellColor = Color(0xFF202228),
                spineHeaderColor = Color(0xFFC62828),
            )
            "sms", "sg1000" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CLAMSHELL_PLASTIC,
                mediaKind = PhysicalMediaKind.CART_SEGA,
                casePlasticColor = Color(0xFFE8EAEF),
                mediaShellColor = Color(0xFF22242A),
                spineHeaderColor = Color(0xFFD32F2F),
            )
            "gg" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CARDBOARD_VERTICAL,
                mediaKind = PhysicalMediaKind.CART_SEGA,
                casePlasticColor = Color(0xFF1C1E26),
                mediaShellColor = Color(0xFF2A2D35),
                spineHeaderColor = Color(0xFF1E88E5),
            )
            "segacd", "saturn" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.KEEP_CASE_DVD,
                mediaKind = PhysicalMediaKind.OPTICAL_DISC,
                casePlasticColor = Color(0xFF1D222B),
                mediaShellColor = Color(0xFF1E88E5),
                spineHeaderColor = Color(0xFF1565C0),
                customAspectOverride = 0.62f,
            )
            "dreamcast" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.JEWEL_CASE_CD,
                mediaKind = PhysicalMediaKind.OPTICAL_DISC,
                casePlasticColor = Color(0xFFD8DEE8),
                mediaShellColor = Color(0xFFFF6D00),
                spineHeaderColor = Color(0xFFFF6D00),
            )

            // Sony Systems
            "psx" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.JEWEL_CASE_CD,
                mediaKind = PhysicalMediaKind.OPTICAL_DISC,
                casePlasticColor = Color(0xFF2A2D34),
                mediaShellColor = Color(0xFF141518),
                spineHeaderColor = Color(0xFF111216),
                blackDiscUnderside = true,
            )
            "ps2" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.KEEP_CASE_DVD,
                mediaKind = PhysicalMediaKind.OPTICAL_DISC,
                // Black in North America; navy blue only in PAL regions from 2001 (Wikipedia, Keep case).
                casePlasticColor = Color(0xFF16171B),
                mediaShellColor = Color(0xFF1E3A8A),
                spineHeaderColor = Color(0xFF0D47A1),
            )
            "psp" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.HANDHELD_SNAP_CASE,
                mediaKind = PhysicalMediaKind.UMD_DISC,
                casePlasticColor = Color(0xFFDCE2EC),
                mediaShellColor = Color(0xFFECEFF4),
                spineHeaderColor = Color(0xFF263238),
                customAspectOverride = 0.58f,
            )
            "psvita" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.HANDHELD_SNAP_CASE,
                mediaKind = PhysicalMediaKind.CART_DS_SWITCH,
                casePlasticColor = Color(0xFF0D47A1),
                mediaShellColor = Color(0xFF1E222B),
                spineHeaderColor = Color(0xFF1565C0),
                customAspectOverride = 0.78f,
            )

            // NEC, SNK, Bandai, Atari & Optical Consoles
            "pce", "supergrafx" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.JEWEL_CASE_CD,
                mediaKind = PhysicalMediaKind.CART_HUCARD,
                casePlasticColor = Color(0xFF2A2D34),
                mediaShellColor = Color(0xFFF5F6F8),
                spineHeaderColor = Color(0xFFFF6D00),
            )
            "pcecd", "pcfx", "neocd" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.JEWEL_CASE_CD,
                mediaKind = PhysicalMediaKind.OPTICAL_DISC,
                casePlasticColor = Color(0xFF282B32),
                mediaShellColor = fallbackAccent,
                spineHeaderColor = fallbackAccent,
            )
            "3do", "cdi" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.KEEP_CASE_DVD,
                mediaKind = PhysicalMediaKind.OPTICAL_DISC,
                casePlasticColor = Color(0xFF23252C),
                mediaShellColor = fallbackAccent,
                spineHeaderColor = fallbackAccent,
            )
            "neogeo" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CLAMSHELL_PLASTIC,
                mediaKind = PhysicalMediaKind.CART_SEGA,
                casePlasticColor = Color(0xFF16171B),
                mediaShellColor = Color(0xFF18191D),
                spineHeaderColor = Color(0xFFD4AF37),
            )
            "ngp", "ws", "lynx", "supervision", "pokemini", "gw", "pico8", "j2me" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CARDBOARD_SQUARE,
                mediaKind = PhysicalMediaKind.CART_GAMEBOY,
                casePlasticColor = Color(0xFF22252C),
                mediaShellColor = Color(0xFF373B44),
                spineHeaderColor = fallbackAccent,
            )
            "arcade", "naomi" -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.ARCADE_SHOCKBOX,
                mediaKind = PhysicalMediaKind.CART_ARCADE_PCB,
                casePlasticColor = Color(0xFF17181E),
                mediaShellColor = Color(0xFF1B5E20),
                spineHeaderColor = Color(0xFFFFB300),
            )
            else -> PhysicalMediaSpec(
                systemId = systemId,
                caseStyle = RetailCaseStyle.CARDBOARD_VERTICAL,
                mediaKind = PhysicalMediaKind.CART_HUCARD,
                casePlasticColor = Color(0xFF24262E),
                mediaShellColor = Color(0xFF333640),
                spineHeaderColor = fallbackAccent,
            )
        }

        /**
         * Slices a LaunchBox `Box - Full` unfolded wraparound sleeve (`[Back | Spine | Front]`)
         * into separate `(backBitmap, spineBitmap, frontBitmap)` faces!
         */
        fun sliceFullBoxCover(full: Bitmap, spineFraction: Float = 0.075f): Triple<Bitmap, Bitmap, Bitmap>? {
            if (full.width < 64 || full.height < 64) return null
            val clampedSpine = spineFraction.coerceIn(0.025f, 0.18f)
            val wing = (1f - clampedSpine) * 0.5f
            val backW = (full.width * wing).toInt().coerceAtLeast(16)
            val spineW = (full.width * clampedSpine).toInt().coerceAtLeast(8)
            val frontX = (backW + spineW).coerceAtMost(full.width - 16)
            val frontW = (full.width - frontX).coerceAtLeast(16)
            return runCatching {
                val back = Bitmap.createBitmap(full, 0, 0, backW, full.height)
                val spine = Bitmap.createBitmap(full, backW, 0, spineW.coerceAtMost(full.width - backW), full.height)
                val front = Bitmap.createBitmap(full, frontX, 0, frontW, full.height)
                Triple(back, spine, front)
            }.getOrNull()
        }

        /** Auto-rotates horizontal LaunchBox `Box - Spine` scans 90° so they align cleanly along the vertical spine. */
        fun orientSpineVertical(spine: Bitmap): Bitmap {
            if (spine.width <= (spine.height * 1.35f).toInt()) return spine
            return runCatching {
                val m = Matrix().apply { postRotate(90f) }
                Bitmap.createBitmap(spine, 0, 0, spine.width, spine.height, m, true)
            }.getOrDefault(spine)
        }
    }
}

// ---- Pure 3D Rectangular Prism Projection & Back-Face Culling Math ------------------------------

internal enum class BoxFaceId { FRONT, BACK, LEFT_SPINE, RIGHT_OPENING, TOP, BOTTOM }

internal data class ProjectedQuad(
    val face: BoxFaceId,
    /** Screen-space corners in clockwise `TL, TR, BR, BL` order (`floatArrayOf(x0,y0, x1,y1, x2,y2, x3,y3)`). */
    val dstPoints: FloatArray,
    /** Average camera-space Z of the face (larger = closer to viewer). */
    val avgZ: Float,
    /** Lighting intensity `[0.35f..1.18f]` from directional studio key + rim lights. */
    val shade: Float,
    /** Specular sheen factor `[0f..1f]` when angled near the glossy reflection vector. */
    val specular: Float,
)

internal object Box3dMath {
    private data class Vec3(val x: Float, val y: Float, val z: Float)

    /**
     * Projects a 3D box centered at `(centerX, centerY)` with half-extents `(halfW, halfH, halfD)`
     * rotated by [yawDeg] around +Y and [pitchDeg] around +X under perspective focal length [focalPx].
     *
     * Returns only front-facing, non-degenerate faces sorted back-to-front.
     */
    fun projectBox(
        halfW: Float,
        halfH: Float,
        halfD: Float,
        yawDeg: Float,
        pitchDeg: Float,
        centerX: Float,
        centerY: Float,
        focalPx: Float,
    ): List<ProjectedQuad> {
        val yaw = Math.toRadians(yawDeg.toDouble())
        val pitch = Math.toRadians(pitchDeg.toDouble())
        val cosY = cos(yaw).toFloat()
        val sinY = sin(yaw).toFloat()
        val cosP = cos(pitch).toFloat()
        val sinP = sin(pitch).toFloat()

        fun rotate(v: Vec3): Vec3 {
            val x1 = v.x * cosY + v.z * sinY
            val z1 = -v.x * sinY + v.z * cosY
            val y2 = v.y * cosP - z1 * sinP
            val z2 = v.y * sinP + z1 * cosP
            return Vec3(x1, y2, z2)
        }

        fun project(v: Vec3): Pair<Float, Float> {
            val denom = (focalPx - v.z).coerceAtLeast(focalPx * 0.22f)
            val s = focalPx / denom
            return (centerX + v.x * s) to (centerY + v.y * s)
        }

        // Studio key light (upper-left-front)
        val lx = -0.36f
        val ly = -0.42f
        val lz = 0.83f

        data class RawFace(val id: BoxFaceId, val corners: Array<Vec3>, val normal: Vec3)

        val faces = arrayOf(
            RawFace(
                BoxFaceId.FRONT,
                arrayOf(
                    Vec3(-halfW, -halfH, +halfD),
                    Vec3(+halfW, -halfH, +halfD),
                    Vec3(+halfW, +halfH, +halfD),
                    Vec3(-halfW, +halfH, +halfD),
                ),
                Vec3(0f, 0f, 1f),
            ),
            RawFace(
                BoxFaceId.BACK,
                arrayOf(
                    Vec3(+halfW, -halfH, -halfD),
                    Vec3(-halfW, -halfH, -halfD),
                    Vec3(-halfW, +halfH, -halfD),
                    Vec3(+halfW, +halfH, -halfD),
                ),
                Vec3(0f, 0f, -1f),
            ),
            RawFace(
                BoxFaceId.LEFT_SPINE,
                arrayOf(
                    Vec3(-halfW, -halfH, -halfD),
                    Vec3(-halfW, -halfH, +halfD),
                    Vec3(-halfW, +halfH, +halfD),
                    Vec3(-halfW, +halfH, -halfD),
                ),
                Vec3(-1f, 0f, 0f),
            ),
            RawFace(
                BoxFaceId.RIGHT_OPENING,
                arrayOf(
                    Vec3(+halfW, -halfH, +halfD),
                    Vec3(+halfW, -halfH, -halfD),
                    Vec3(+halfW, +halfH, -halfD),
                    Vec3(+halfW, +halfH, +halfD),
                ),
                Vec3(1f, 0f, 0f),
            ),
            RawFace(
                BoxFaceId.TOP,
                arrayOf(
                    Vec3(-halfW, -halfH, -halfD),
                    Vec3(+halfW, -halfH, -halfD),
                    Vec3(+halfW, -halfH, +halfD),
                    Vec3(-halfW, -halfH, +halfD),
                ),
                Vec3(0f, -1f, 0f),
            ),
            RawFace(
                BoxFaceId.BOTTOM,
                arrayOf(
                    Vec3(-halfW, +halfH, +halfD),
                    Vec3(+halfW, +halfH, +halfD),
                    Vec3(+halfW, +halfH, -halfD),
                    Vec3(-halfW, +halfH, -halfD),
                ),
                Vec3(0f, 1f, 0f),
            ),
        )

        val visible = ArrayList<ProjectedQuad>(3)
        for (f in faces) {
            val r0 = rotate(f.corners[0])
            val r1 = rotate(f.corners[1])
            val r2 = rotate(f.corners[2])
            val r3 = rotate(f.corners[3])

            val (x0, y0) = project(r0)
            val (x1, y1) = project(r1)
            val (x2, y2) = project(r2)
            val (x3, y3) = project(r3)

            // Clockwise signed area in screen space (+X right, +Y down); <= 4 px^2 means back-facing or edge-on
            val doubleArea = ((x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)) +
                ((x2 - x0) * (y3 - y0) - (x3 - x0) * (y2 - y0))
            if (doubleArea <= 4f) continue

            val rn = rotate(f.normal)
            val ndotl = (rn.x * lx + rn.y * ly + rn.z * lz).coerceIn(-1f, 1f)
            val shade = (0.68f + 0.36f * ndotl.coerceAtLeast(0f) + 0.08f * rn.z.coerceAtLeast(0f)).coerceIn(0.45f, 1.12f)

            // Half-vector between view (0,0,1) and studio light for glossy case specular glint
            val hx = -0.20f
            val hy = -0.23f
            val hz = 0.95f
            val ndoth = (rn.x * hx + rn.y * hy + rn.z * hz).coerceIn(0f, 1f)
            val spec = (ndoth * ndoth * ndoth * ndoth * ndoth).coerceIn(0f, 1f)

            val avgZ = (r0.z + r1.z + r2.z + r3.z) * 0.25f
            visible += ProjectedQuad(
                face = f.id,
                dstPoints = floatArrayOf(x0, y0, x1, y1, x2, y2, x3, y3),
                avgZ = avgZ,
                shade = shade,
                specular = spec,
            )
        }
        visible.sortBy { it.avgZ }
        return visible
    }
}
