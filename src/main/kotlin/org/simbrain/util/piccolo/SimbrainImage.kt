package org.simbrain.util.piccolo

import org.piccolo2d.nodes.PImage
import org.piccolo2d.util.PPaintContext
import org.simbrain.util.piccolo.SimbrainImage.Companion.MAX_CACHE_DIM
import java.awt.Image
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A [PImage] subclass that fixes a rendering artifact where the pixel grid shifts when
 * the image partially extends beyond the viewport. Java2D's nearest-neighbor interpolation
 * snaps source pixel boundaries to integer device pixels, causing the grid to shift when
 * the viewport clips through a scaled-up source pixel.
 *
 * This subclass pre-scales only the **visible portion** of the source image to screen pixel
 * resolution, then draws the cache at 1:1 in device space. This completely bypasses Java2D's
 * image scaling, eliminating the artifact while keeping memory bounded to viewport size.
 *
 * The cache is capped at [MAX_CACHE_DIM] pixels per axis. At extreme zoom levels where only
 * a few source pixels are visible, this is more than enough resolution.
 */
class SimbrainImage : PImage {

    constructor() : super()
    constructor(image: Image) : super(image)

    private var scaledCache: BufferedImage? = null
    private var contentDirty = true

    // Previous visible region — skip rescaling when unchanged
    private var prevVisX0 = Double.NaN
    private var prevVisY0 = Double.NaN
    private var prevVisX1 = Double.NaN
    private var prevVisY1 = Double.NaN
    private var prevImgScreenX = Double.NaN
    private var prevImgScreenY = Double.NaN
    private var prevImgScreenW = Double.NaN
    private var prevImgScreenH = Double.NaN

    override fun invalidatePaint() {
        contentDirty = true
        super.invalidatePaint()
    }

    override fun paint(paintContext: PPaintContext) {
        val img = image ?: return
        if (img !is BufferedImage) {
            super.paint(paintContext)
            return
        }
        val iw = img.width
        val ih = img.height
        if (iw == 0 || ih == 0) return

        val b = boundsReference
        val g2 = paintContext.graphics
        val fullTransform = g2.transform

        // Transform node-space image bounds to screen-space
        val topLeft = Point2D.Double()
        val bottomRight = Point2D.Double()
        fullTransform.transform(Point2D.Double(b.x, b.y), topLeft)
        fullTransform.transform(Point2D.Double(b.x + b.width, b.y + b.height), bottomRight)

        val imgScreenX = min(topLeft.x, bottomRight.x)
        val imgScreenY = min(topLeft.y, bottomRight.y)
        val imgScreenW = abs(bottomRight.x - topLeft.x)
        val imgScreenH = abs(bottomRight.y - topLeft.y)
        if (imgScreenW < 1 || imgScreenH < 1) return

        // Compute visible region in screen-space by intersecting image bounds with clip
        val clipNodeSpace = g2.clipBounds
        val visX0d: Double
        val visY0d: Double
        val visX1d: Double
        val visY1d: Double
        if (clipNodeSpace != null) {
            val clipTL = Point2D.Double()
            val clipBR = Point2D.Double()
            fullTransform.transform(
                Point2D.Double(clipNodeSpace.x.toDouble(), clipNodeSpace.y.toDouble()), clipTL
            )
            fullTransform.transform(
                Point2D.Double(
                    (clipNodeSpace.x + clipNodeSpace.width).toDouble(),
                    (clipNodeSpace.y + clipNodeSpace.height).toDouble()
                ), clipBR
            )
            val clipX0 = min(clipTL.x, clipBR.x)
            val clipY0 = min(clipTL.y, clipBR.y)
            val clipX1 = max(clipTL.x, clipBR.x)
            val clipY1 = max(clipTL.y, clipBR.y)

            visX0d = max(imgScreenX, clipX0)
            visY0d = max(imgScreenY, clipY0)
            visX1d = min(imgScreenX + imgScreenW, clipX1)
            visY1d = min(imgScreenY + imgScreenH, clipY1)
        } else {
            visX0d = imgScreenX
            visY0d = imgScreenY
            visX1d = imgScreenX + imgScreenW
            visY1d = imgScreenY + imgScreenH
        }

        val visWd = visX1d - visX0d
        val visHd = visY1d - visY0d
        if (visWd < 1 || visHd < 1) return

        // Cap cache size to avoid huge allocations at extreme zoom
        val scale = min(1.0, min(MAX_CACHE_DIM / visWd, MAX_CACHE_DIM / visHd))
        val cacheW = max(1, (visWd * scale).toInt())
        val cacheH = max(1, (visHd * scale).toInt())

        // Only rebuild cache if content changed or visible region moved
        val regionChanged = visX0d != prevVisX0 || visY0d != prevVisY0
                || visX1d != prevVisX1 || visY1d != prevVisY1
                || imgScreenX != prevImgScreenX || imgScreenY != prevImgScreenY
                || imgScreenW != prevImgScreenW || imgScreenH != prevImgScreenH

        if (contentDirty || regionChanged || scaledCache == null) {
            var cache = scaledCache
            if (cache == null || cache.width < cacheW || cache.height < cacheH) {
                cache = BufferedImage(cacheW, cacheH, BufferedImage.TYPE_INT_RGB)
                scaledCache = cache
            }

            // Nearest-neighbor scale: map each cache pixel back to source pixel
            val srcPixels = (img.raster.dataBuffer as? DataBufferInt)?.data
            val dstPixels = (cache.raster.dataBuffer as DataBufferInt).data

            for (y in 0 until cacheH) {
                val screenY = visY0d + (y + 0.5) * visHd / cacheH
                val srcY = ((screenY - imgScreenY) / imgScreenH * ih).toInt().coerceIn(0, ih - 1)
                val srcRow = srcY * iw
                val dstRow = y * cache.width
                for (x in 0 until cacheW) {
                    val screenX = visX0d + (x + 0.5) * visWd / cacheW
                    val srcX = ((screenX - imgScreenX) / imgScreenW * iw).toInt().coerceIn(0, iw - 1)
                    dstPixels[dstRow + x] = if (srcPixels != null) {
                        srcPixels[srcRow + srcX]
                    } else {
                        img.getRGB(srcX, srcY)
                    }
                }
            }

            prevVisX0 = visX0d
            prevVisY0 = visY0d
            prevVisX1 = visX1d
            prevVisY1 = visY1d
            prevImgScreenX = imgScreenX
            prevImgScreenY = imgScreenY
            prevImgScreenW = imgScreenW
            prevImgScreenH = imgScreenH
            contentDirty = false
        }

        val cache = scaledCache ?: return

        // Draw in screen space, stretching cache to fill the visible region
        val savedTransform = g2.transform
        g2.transform = AffineTransform()
        g2.drawImage(cache, visX0d.toInt(), visY0d.toInt(), visX1d.toInt(), visY1d.toInt(),
            0, 0, cacheW, cacheH, null)
        g2.transform = savedTransform
    }

    companion object {
        /** Maximum cache dimension per axis. Prevents huge allocations at extreme zoom. */
        private const val MAX_CACHE_DIM = 4096.0
    }
}
