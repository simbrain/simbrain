package org.simbrain.util.piccolo

import org.piccolo2d.PNode
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A group node that paints its children from a raster cache: the visible part of the subtree is
 * rendered once into an offscreen image at the current device scale and blitted per frame, so expensive antialiased
 * vector content that changes rarely (edge chrome, large static decorations) stops being
 * re-rasterized on every repaint. Any repaint bubbling up from a descendant invalidates the
 * cache automatically; the cache rebuilds on the first paint whose content and scale are
 * unchanged since the previous one, so continuous mutation (drags) and zoom gestures paint
 * directly at status-quo cost instead of re-rendering into the cache per frame. The cache covers the
 * subtree's bounds clipped to the camera's view, so it stays viewport-sized at any zoom; panning
 * outside it rebuilds once the view settles. It is aligned to the device pixel grid, so cached
 * content lands exactly where direct painting would put it. Falls back to direct painting under
 * rotation/shear or when the visible region exceeds [MAX_RASTER_DIM] device pixels per axis.
 * Picking and bounds are unaffected.
 */
class RasterCachedNode : PNode() {

    private var cache: BufferedImage? = null
    private var cacheScaleX = 0.0
    private var cacheScaleY = 0.0
    private var cacheFracX = 0.0
    private var cacheFracY = 0.0

    /** The part of the subtree the cache holds, in local coordinates: its bounds clipped to the camera's view. */
    private var cacheRegion = PBounds()
    private var lastSeenScaleX = 0.0
    private var lastSeenScaleY = 0.0
    private var lastSeenRegion = PBounds()
    private var mutatedSinceLastPaint = false

    /** The previous cache image when it has the right size, else a new one, so rebuilds don't allocate each time. */
    private var spare: BufferedImage? = null

    /**
     * The opaque color directly beneath this node, when there is one (e.g. the canvas background under the bottom
     * tier). The cache is then rendered over that color rather than over transparency, which makes it blend exactly as
     * direct painting does; a transparent cache blends antialiased edges in a different order and can differ slightly.
     * Null for a node that sits over other content.
     */
    var background: (() -> Color?)? = null

    private var cacheBackground: Color? = null

    /** When false the subtree always paints directly; for comparing against the cache and for debugging. */
    var cachingEnabled = true
        set(value) {
            field = value
            dropCache()
            invalidatePaint()
        }

    /** Whether a cache is currently held, i.e. the next paint will blit rather than render. */
    internal val isCached get() = cache != null

    private fun dropCache() {
        cache?.let { spare = it }
        cache = null
    }

    override fun repaintFrom(localBounds: PBounds, childOrSelf: PNode) {
        if (childOrSelf !== this) {
            dropCache()
            mutatedSinceLastPaint = true
        }
        super.repaintFrom(localBounds, childOrSelf)
    }

    override fun fullPaint(paintContext: PPaintContext) {
        if (!visible || !fullIntersects(paintContext.localClip)) return
        val g2 = paintContext.graphics
        val t = g2.transform
        val b = fullBoundsReference
        if (!cachingEnabled || b.width <= 0 || b.height <= 0 ||
            t.shearX != 0.0 || t.shearY != 0.0 || t.scaleX <= 0 || t.scaleY <= 0) {
            super.fullPaint(paintContext)
            return
        }
        val sx = t.scaleX
        val sy = t.scaleY
        // Cache only what the camera shows, so the image never outgrows the viewport however far the view is zoomed
        val region = PBounds(b)
        paintContext.camera?.let { camera -> Rectangle2D.intersect(region, globalToLocal(PBounds(camera.viewBounds)), region) }
        if (region.isEmpty) return
        // The cache is aligned to the device pixel grid: its origin pixel is the device pixel containing the region's
        // corner, and the sub-pixel remainder is rendered into it, so cached content lands exactly where direct
        // painting would put it rather than up to half a pixel off
        val origin = t.transform(Point2D.Double(region.x, region.y), Point2D.Double())
        val originX = floor(origin.x)
        val originY = floor(origin.y)
        val fracX = origin.x - originX
        val fracY = origin.y - originY
        val pixelW = ceil(region.width * sx + fracX).toInt()
        val pixelH = ceil(region.height * sy + fracY).toInt()
        if (pixelW > MAX_RASTER_DIM || pixelH > MAX_RASTER_DIM || pixelW < 1 || pixelH < 1) {
            super.fullPaint(paintContext)
            return
        }
        // Rebuild only once the view and content have held still for a frame, so drags, zooms and pans paint
        // directly instead of re-rendering the cache every frame
        val settled = sx == lastSeenScaleX && sy == lastSeenScaleY && region == lastSeenRegion && !mutatedSinceLastPaint
        lastSeenScaleX = sx
        lastSeenScaleY = sy
        lastSeenRegion = PBounds(region)
        mutatedSinceLastPaint = false
        val clip = PBounds(paintContext.localClip)
        Rectangle2D.intersect(clip, b, clip)
        val underneath = background?.invoke()?.takeIf { it.alpha == 255 }
        val valid = cache != null && sx == cacheScaleX && sy == cacheScaleY && fracX == cacheFracX && fracY == cacheFracY &&
            region == cacheRegion && underneath == cacheBackground
        if (!valid || !cacheRegion.contains(clip)) {
            if (!settled) {
                super.fullPaint(paintContext)
                return
            }
            val img = reusableImage(pixelW, pixelH)
            val ig = img.createGraphics()
            if (underneath != null) {
                ig.color = underneath
                ig.fillRect(0, 0, pixelW, pixelH)
            } else {
                ig.composite = AlphaComposite.Clear
                ig.fillRect(0, 0, pixelW, pixelH)
                ig.composite = AlphaComposite.SrcOver
            }
            ig.setClip(0, 0, pixelW, pixelH)
            ig.translate(fracX, fracY)
            ig.scale(sx, sy)
            ig.translate(-region.x, -region.y)
            val pc = PPaintContext(ig)
            pc.setRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING)
            super.fullPaint(pc)
            ig.dispose()
            cache = img
            cacheScaleX = sx
            cacheScaleY = sy
            cacheFracX = fracX
            cacheFracY = fracY
            cacheRegion = PBounds(region)
            cacheBackground = underneath
        }
        val saved = g2.transform
        g2.transform = AffineTransform()
        g2.drawImage(cache, originX.toInt(), originY.toInt(), null)
        g2.transform = saved
    }

    private fun reusableImage(width: Int, height: Int): BufferedImage {
        val previous = cache ?: spare
        if (previous != null && previous.width == width && previous.height == height) return previous
        return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { spare = it }
    }

    companion object {
        const val MAX_RASTER_DIM = 4096
    }
}
