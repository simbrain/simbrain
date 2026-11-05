/**
 * Nearest-neighbor image drawing that bypasses Java2D's scaled image blit, for nodes that draw enlarged pixel images.
 * Java2D snaps source pixel boundaries to integer device pixels relative to the clip, so an enlarged image's pixel
 * grid stretches and jumps as the view pans, zooms, or clips through it. These painters instead sample the image into
 * a cache at device resolution and draw that cache 1:1.
 *
 * [PixelExactImagePainter] serves images whose pixels change, such as [SimbrainImage]'s activations: it samples only
 * the visible part, axis-aligned with [SimbrainImage]'s original region cache or rotated through the inverse
 * transform, and resamples whenever the image moves on screen. [PixelExactSpriteCache] serves fixed images that
 * move, such as odor world sprites: it samples a whole image once per zoom and rotation, shares the result between
 * every node drawing the same image, and draws it at a whole device pixel, so moving costs only a copy.
 */
package org.simbrain.util.piccolo

import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.NoninvertibleTransformException
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a [BufferedImage] into a destination rectangle with exact nearest-neighbor sampling. Holds the cache for one
 * drawn image, so give each node its own painter. Call [invalidate] when the pixels of the same image object change;
 * a different image object, or any change of where it lands on screen, rebuilds the cache by itself.
 */
class PixelExactImagePainter {

    private var scaledCache: BufferedImage? = null
    private var contentDirty = true
    private var lastImage: BufferedImage? = null

    /** Int-pixel copy of a source image of another layout, remade when its pixels change. */
    private var converted: BufferedImage? = null

    // Previous visible region — skip rescaling when unchanged
    private var prevVisX0 = Double.NaN
    private var prevVisY0 = Double.NaN
    private var prevVisX1 = Double.NaN
    private var prevVisY1 = Double.NaN
    private var prevImgScreenX = Double.NaN
    private var prevImgScreenY = Double.NaN
    private var prevImgScreenW = Double.NaN
    private var prevImgScreenH = Double.NaN

    // Previous transformed draw — skip resampling when the transform, destination and device region are unchanged
    private val prevTransform = DoubleArray(6) { Double.NaN }
    private val prevDest = Rectangle2D.Double(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
    private var prevDeviceX0 = Int.MIN_VALUE
    private var prevDeviceY0 = Int.MIN_VALUE
    private var prevDeviceW = -1
    private var prevDeviceH = -1

    /** Marks the image's pixels as changed, so the next [paint] resamples them. */
    fun invalidate() {
        contentDirty = true
    }

    /** Draws [img] to fill [dest], given in the user space of [g2]. */
    fun paint(g2: Graphics2D, img: BufferedImage, dest: Rectangle2D) {
        if (img.width == 0 || img.height == 0 || dest.width <= 0 || dest.height <= 0) return
        if (img !== lastImage) {
            lastImage = img
            contentDirty = true
        }
        // Sampling reads one source pixel per device pixel, and getRGB on a byte-backed image (as loaded from a PNG)
        // costs far more than an int array read, so other layouts are converted once per content change
        val source = if (hasIntPixels(img)) {
            converted = null
            img
        } else {
            converted.takeIf { !contentDirty && it != null } ?: toIntArgb(img).also { converted = it }
        }
        if (rotatesOrShears(g2.transform)) {
            paintTransformed(g2, source, dest)
        } else {
            paintAxisAligned(g2, source, dest)
        }
    }

    private fun paintAxisAligned(g2: Graphics2D, img: BufferedImage, b: Rectangle2D) {
        val iw = img.width
        val ih = img.height
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

        // Keep the source's transparency: an opaque cache would paint a transparent overlay (such as a spiking
        // array's spike image) black. Source ints are copied as-is only when their layout matches the cache's;
        // anything else goes through getRGB, which returns non-premultiplied ARGB.
        val cacheType = when (img.type) {
            BufferedImage.TYPE_INT_RGB, BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_INT_ARGB_PRE -> img.type
            else -> if (img.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        }
        val copiesInts = cacheType == img.type

        if (contentDirty || regionChanged || scaledCache == null || scaledCache?.type != cacheType) {
            val cache = ensureCache(cacheW, cacheH, cacheType)

            // Nearest-neighbor scale: map each cache pixel back to source pixel
            val srcPixels = if (copiesInts) (img.raster.dataBuffer as? DataBufferInt)?.data else null
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
            prevDeviceW = -1
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

    /**
     * Samples [img] once per device pixel of the clipped device-space box around the transformed [dest], leaving
     * pixels outside the image transparent, and draws the result 1:1 at integer device coordinates.
     */
    private fun paintTransformed(g2: Graphics2D, img: BufferedImage, dest: Rectangle2D) {
        val transform = g2.transform
        val inverse = try {
            transform.createInverse()
        } catch (e: NoninvertibleTransformException) {
            return
        }

        val deviceBox = transformedBox(transform, dest)
        g2.clipBounds?.let { Rectangle2D.intersect(deviceBox, transformedBox(transform, it), deviceBox) }
        if (deviceBox.width <= 0 || deviceBox.height <= 0) return
        val x0 = floor(deviceBox.minX).toInt()
        val y0 = floor(deviceBox.minY).toInt()
        val w = ceil(deviceBox.maxX).toInt() - x0
        val h = ceil(deviceBox.maxY).toInt() - y0
        if (w <= 0 || h <= 0) return
        if (w > MAX_CACHE_DIM || h > MAX_CACHE_DIM) {
            drawWithJava2D(g2, img, dest)
            return
        }

        val matrix = DoubleArray(6).also { transform.getMatrix(it) }
        val unchanged = !contentDirty && scaledCache?.type == BufferedImage.TYPE_INT_ARGB &&
                matrix.contentEquals(prevTransform) && dest == prevDest &&
                x0 == prevDeviceX0 && y0 == prevDeviceY0 && w == prevDeviceW && h == prevDeviceH
        if (!unchanged) {
            val cache = ensureCache(w, h, BufferedImage.TYPE_INT_ARGB)
            sampleNearest(intPixels(img), img.width, img.height, inverse, dest, x0, y0, w, h, cache)

            matrix.copyInto(prevTransform)
            prevDest.setRect(dest)
            prevDeviceX0 = x0
            prevDeviceY0 = y0
            prevDeviceW = w
            prevDeviceH = h
            prevImgScreenW = Double.NaN
            contentDirty = false
        }

        val cache = scaledCache ?: return
        val savedTransform = g2.transform
        g2.transform = AffineTransform()
        g2.drawImage(cache, x0, y0, x0 + w, y0 + h, 0, 0, w, h, null)
        g2.transform = savedTransform
    }

    private fun ensureCache(w: Int, h: Int, type: Int): BufferedImage {
        val cache = scaledCache
        if (cache != null && cache.type == type && cache.width >= w && cache.height >= h) return cache
        return BufferedImage(w, h, type).also { scaledCache = it }
    }

    private fun drawWithJava2D(g2: Graphics2D, img: BufferedImage, dest: Rectangle2D) {
        val savedTransform = g2.transform
        g2.translate(dest.x, dest.y)
        g2.scale(dest.width / img.width, dest.height / img.height)
        g2.drawImage(img, 0, 0, null)
        g2.transform = savedTransform
    }

    companion object {
        /** Maximum cache dimension per axis. Prevents huge allocations at extreme zoom. */
        const val MAX_CACHE_DIM = 4096.0

        internal fun transformedBox(transform: AffineTransform, rect: Rectangle2D): Rectangle2D =
            transform.createTransformedShape(rect).bounds2D

        internal fun rotatesOrShears(transform: AffineTransform) = transform.type and
                (AffineTransform.TYPE_GENERAL_ROTATION or AffineTransform.TYPE_QUADRANT_ROTATION or
                        AffineTransform.TYPE_GENERAL_TRANSFORM) != 0

        private fun hasIntPixels(img: BufferedImage) = when (img.type) {
            BufferedImage.TYPE_INT_RGB, BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_INT_ARGB_PRE -> true
            else -> false
        }

        internal fun toIntArgb(img: BufferedImage): BufferedImage =
            BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB).also {
                it.setRGB(0, 0, img.width, img.height, img.getRGB(0, 0, img.width, img.height, null, 0, img.width), 0, img.width)
            }

        /** Non-premultiplied ARGB pixels of [img], read in bulk; the backing array itself for an INT_ARGB image. */
        internal fun intPixels(img: BufferedImage): IntArray =
            if (img.type == BufferedImage.TYPE_INT_ARGB) {
                (img.raster.dataBuffer as DataBufferInt).data
            } else {
                img.getRGB(0, 0, img.width, img.height, null, 0, img.width)
            }

        /**
         * Fills the [w] by [h] corner of [target] with the pixels of an [iw] by [ih] image drawn to fill [dest], one
         * nearest sample per device pixel of the box whose top left is device pixel ([x0], [y0]). [inverse] maps
         * device space back to the user space of [dest]. Pixels the image does not cover are left transparent.
         */
        internal fun sampleNearest(
            pixels: IntArray, iw: Int, ih: Int, inverse: AffineTransform, dest: Rectangle2D,
            x0: Int, y0: Int, w: Int, h: Int, target: BufferedImage
        ) {
            val dstPixels = (target.raster.dataBuffer as DataBufferInt).data
            val inv = DoubleArray(6).also { inverse.getMatrix(it) }
            // Inverse transform rows, rescaled so user-space offsets from dest's origin come out in image pixels
            val sx = iw / dest.width
            val sy = ih / dest.height
            val dudx = inv[0] * sx
            val dvdx = inv[1] * sy
            val dudy = inv[2] * sx
            val dvdy = inv[3] * sy
            val u0 = (inv[4] - dest.x) * sx
            val v0 = (inv[5] - dest.y) * sy

            for (py in 0 until h) {
                val deviceY = y0 + py + 0.5
                val deviceX = x0 + 0.5
                var u = u0 + dudx * deviceX + dudy * deviceY
                var v = v0 + dvdx * deviceX + dvdy * deviceY
                val dstRow = py * target.width
                for (px in 0 until w) {
                    dstPixels[dstRow + px] = if (u >= 0 && v >= 0 && u < iw && v < ih) {
                        pixels[v.toInt() * iw + u.toInt()]
                    } else {
                        0
                    }
                    u += dudx
                    v += dvdx
                }
            }
        }
    }
}

/**
 * Enlarged copies of fixed images, sampled once per image, destination rectangle, and zoom-and-rotation, and shared by
 * every node that draws the same image the same way. A copy is drawn at the nearest whole device pixel, so a sprite
 * that moves without turning or resizing costs one plain copy per frame. Only for images whose pixels never change:
 * there is no invalidation. Least recently used copies are dropped past [PIXEL_BUDGET] pixels in all. Use on the
 * event thread only.
 */
object PixelExactSpriteCache {

    /** Largest copy, per axis, worth caching; bigger draws go to a [PixelExactImagePainter], which samples only what is visible. */
    const val MAX_SIDE = 2048

    /** Total pixels kept across all copies, about 64 MB. */
    const val PIXEL_BUDGET = 16_000_000L

    private data class Key(
        val image: BufferedImage,
        val m00: Double, val m10: Double, val m01: Double, val m11: Double,
        val destX: Double, val destY: Double, val destW: Double, val destH: Double
    )

    private class Copy(val image: BufferedImage, val x0: Int, val y0: Int)

    private val copies = LinkedHashMap<Key, Copy>(64, 0.75f, true)
    private var pixels = 0L

    /** Number of cached copies, for tests. */
    internal val size get() = copies.size

    internal fun clear() {
        copies.clear()
        pixels = 0
    }

    /**
     * Draws [img] to fill [dest], given in the user space of [g2]. Returns false, drawing nothing, when the enlarged
     * image would be too big to cache.
     */
    fun paint(g2: Graphics2D, img: BufferedImage, dest: Rectangle2D): Boolean {
        if (img.width == 0 || img.height == 0 || dest.width <= 0 || dest.height <= 0) return true
        val m = DoubleArray(6).also { g2.transform.getMatrix(it) }
        val key = Key(img, m[0], m[1], m[2], m[3], dest.x, dest.y, dest.width, dest.height)
        val copy = copies[key] ?: build(key, img, dest) ?: return false
        val x = Math.round(m[4]).toInt() + copy.x0
        val y = Math.round(m[5]).toInt() + copy.y0
        val savedTransform = g2.transform
        g2.transform = AffineTransform()
        g2.drawImage(copy.image, x, y, null)
        g2.transform = savedTransform
        return true
    }

    private fun build(key: Key, img: BufferedImage, dest: Rectangle2D): Copy? {
        val linear = AffineTransform(key.m00, key.m10, key.m01, key.m11, 0.0, 0.0)
        val inverse = try {
            linear.createInverse()
        } catch (e: NoninvertibleTransformException) {
            return null
        }
        val box = PixelExactImagePainter.transformedBox(linear, dest)
        val x0 = floor(box.minX).toInt()
        val y0 = floor(box.minY).toInt()
        val w = ceil(box.maxX).toInt() - x0
        val h = ceil(box.maxY).toInt() - y0
        if (w <= 0 || h <= 0 || w > MAX_SIDE || h > MAX_SIDE) return null

        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        PixelExactImagePainter.sampleNearest(
            PixelExactImagePainter.intPixels(img), img.width, img.height, inverse, dest, x0, y0, w, h, image
        )
        val copy = Copy(image, x0, y0)
        copies[key] = copy
        pixels += w.toLong() * h
        val oldest = copies.entries.iterator()
        while (pixels > PIXEL_BUDGET && oldest.hasNext()) {
            val entry = oldest.next()
            if (entry.value === copy) break
            pixels -= entry.value.image.width.toLong() * entry.value.image.height
            oldest.remove()
        }
        return copy
    }
}
