package com.retrohero.app.emulator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.retrohero.app.data.DeltaSkin
import java.io.File
import kotlin.math.roundToInt

/** Loads PNG and vector PDF artwork from an extracted [DeltaSkin]. */
object DeltaSkinAssets {
    private const val MAX_RENDER_EDGE = 2400

    /**
     * Loads [file] (either `.pdf` or raster image like `.png`) into an ARGB_8888 [Bitmap].
     * PDFs are rasterized at [targetWidthPx] × [targetHeightPx] with a transparent background
     * so screen cutouts stay transparent.
     */
    fun loadBitmap(file: File, targetWidthPx: Int, targetHeightPx: Int): Bitmap? {
        if (!file.isFile) return null
        return if (file.extension.equals("pdf", ignoreCase = true)) {
            renderPdf(file, targetWidthPx, targetHeightPx)
        } else {
            runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        }
    }

    private fun renderPdf(file: File, targetWidthPx: Int, targetHeightPx: Int): Bitmap? = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount <= 0) return null
                renderer.openPage(0).use { page ->
                    val pageW = page.width.coerceAtLeast(1)
                    val pageH = page.height.coerceAtLeast(1)
                    val desiredW = targetWidthPx.coerceAtLeast(1)
                    val desiredH = targetHeightPx.coerceAtLeast(1)
                    val scale = minOf(
                        desiredW.toFloat() / pageW,
                        desiredH.toFloat() / pageH,
                        MAX_RENDER_EDGE.toFloat() / maxOf(pageW, pageH),
                    ).coerceAtLeast(0.1f)
                    val w = (pageW * scale).roundToInt().coerceAtLeast(1)
                    val h = (pageH * scale).roundToInt().coerceAtLeast(1)
                    // ARGB_8888 starts fully transparent (alpha 0), preserving screen cutouts.
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
        }
    }.getOrNull()
}
