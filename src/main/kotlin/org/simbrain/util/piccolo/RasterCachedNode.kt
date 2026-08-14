package org.simbrain.util.piccolo

import org.piccolo2d.PNode
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * A group node that paints its children from a raster cache: the subtree is rendered once into
 * an offscreen image at the current device scale and blitted per frame, so expensive antialiased
 * vector content that changes rarely (edge chrome, large static decorations) stops being
 * re-rasterized on every repaint. Any repaint bubbling up from a descendant invalidates the
 * cache automatically; the cache rebuilds on the first paint whose content and scale are
 * unchanged since the previous one, so continuous mutation (drags) and zoom gestures paint
 * directly at status-quo cost instead of re-rendering into the cache per frame. Falls back to
 * direct painting under rotation/shear or when the subtree exceeds [MAX_RASTER_DIM] device
 * pixels per axis (deep zoom). Picking and bounds are unaffected.
 */
class RasterCachedNode : PNode() {

    private var cache: BufferedImage? = null
    private var cacheScaleX = 0.0
    private var cacheScaleY = 0.0
    private var cacheBounds = PBounds()
    private var lastSeenScaleX = 0.0
    private var lastSeenScaleY = 0.0
    private var mutatedSinceLastPaint = false

    override fun repaintFrom(localBounds: PBounds, childOrSelf: PNode) {
        if (childOrSelf !== this) {
            cache = null
            mutatedSinceLastPaint = true
        }
        super.repaintFrom(localBounds, childOrSelf)
    }

    override fun fullPaint(paintContext: PPaintContext) {
        if (!visible || !fullIntersects(paintContext.localClip)) return
        val g2 = paintContext.graphics
        val t = g2.transform
        val b = fullBoundsReference
        if (b.width <= 0 || b.height <= 0) return
        if (t.shearX != 0.0 || t.shearY != 0.0 || t.scaleX <= 0 || t.scaleY <= 0) {
            super.fullPaint(paintContext)
            return
        }
        val sx = t.scaleX
        val sy = t.scaleY
        val pixelW = ceil(b.width * sx).toInt()
        val pixelH = ceil(b.height * sy).toInt()
        if (pixelW > MAX_RASTER_DIM || pixelH > MAX_RASTER_DIM || pixelW < 1 || pixelH < 1) {
            super.fullPaint(paintContext)
            return
        }
        val settled = sx == lastSeenScaleX && sy == lastSeenScaleY && !mutatedSinceLastPaint
        lastSeenScaleX = sx
        lastSeenScaleY = sy
        mutatedSinceLastPaint = false
        val valid = cache != null && sx == cacheScaleX && sy == cacheScaleY && b == cacheBounds
        if (!valid) {
            if (!settled) {
                super.fullPaint(paintContext)
                return
            }
            val img = BufferedImage(pixelW, pixelH, BufferedImage.TYPE_INT_ARGB)
            val ig = img.createGraphics()
            ig.setClip(0, 0, pixelW, pixelH)
            ig.scale(sx, sy)
            ig.translate(-b.x, -b.y)
            val pc = PPaintContext(ig)
            pc.setRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING)
            super.fullPaint(pc)
            ig.dispose()
            cache = img
            cacheScaleX = sx
            cacheScaleY = sy
            cacheBounds = PBounds(b)
        }
        val origin = t.transform(Point2D.Double(b.x, b.y), Point2D.Double())
        val saved = g2.transform
        g2.transform = AffineTransform()
        g2.drawImage(cache, origin.x.roundToInt(), origin.y.roundToInt(), null)
        g2.transform = saved
    }

    companion object {
        const val MAX_RASTER_DIM = 4096
    }
}
