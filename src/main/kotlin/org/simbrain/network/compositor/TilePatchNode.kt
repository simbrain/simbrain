package org.simbrain.network.compositor

import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import org.simbrain.util.NetworkTheme
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Paints a [TensorTile] by shading its value buffer straight into a screen-resolution patch via
 * [TensorTile.shadePatch] — no full-resolution intermediate image exists, so shading cost is
 * bounded by on-screen pixels (capped at [MAX_PATCH_DIM] per axis), never by tensor size. The
 * patch covers the tile's whole screen projection when that fits the cap (so pans and Swing's
 * shifting damage clips re-blit, and version bumps reshade only the dirty row band), falling
 * back to the clipped window at deep zoom; [markStale] forces a reshade after palette switches,
 * which don't move content versions. Drawing the patch at 1:1 in device space also sidesteps
 * Java2D's grid-shift scaling artifact (the same scheme as SimbrainImage, without its
 * pre-shaded source).
 */
class TilePatchNode(val tile: TensorTile) : PNode() {

    private var patch: BufferedImage? = null
    private var shadedVersion = Long.MIN_VALUE

    private var prevFullMode = false
    private var prevPatchW = -1
    private var prevPatchH = -1
    private var prevVisX0 = Double.NaN
    private var prevVisY0 = Double.NaN
    private var prevVisX1 = Double.NaN
    private var prevVisY1 = Double.NaN
    private var prevScreenX = Double.NaN
    private var prevScreenY = Double.NaN
    private var prevScreenW = Double.NaN
    private var prevScreenH = Double.NaN

    /** Forces the next paint to reshade — for palette/theme switches. */
    fun markStale() {
        shadedVersion = Long.MIN_VALUE
        invalidatePaint()
    }

    /** Schedules a repaint when the tile has published since the last shade. */
    fun syncContent() {
        if (tile.contentVersion != shadedVersion) invalidatePaint()
    }

    override fun paint(paintContext: PPaintContext) {
        val b = boundsReference
        val g2 = paintContext.graphics
        val transform = g2.transform

        val topLeft = Point2D.Double()
        val bottomRight = Point2D.Double()
        transform.transform(Point2D.Double(b.x, b.y), topLeft)
        transform.transform(Point2D.Double(b.x + b.width, b.y + b.height), bottomRight)
        val screenX = min(topLeft.x, bottomRight.x)
        val screenY = min(topLeft.y, bottomRight.y)
        val screenW = kotlin.math.abs(bottomRight.x - topLeft.x)
        val screenH = kotlin.math.abs(bottomRight.y - topLeft.y)
        if (screenW < 1 || screenH < 1) return

        var clipX0 = screenX
        var clipY0 = screenY
        var clipX1 = screenX + screenW
        var clipY1 = screenY + screenH
        g2.clipBounds?.let { clip ->
            val clipTL = Point2D.Double()
            val clipBR = Point2D.Double()
            transform.transform(Point2D.Double(clip.x.toDouble(), clip.y.toDouble()), clipTL)
            transform.transform(
                Point2D.Double((clip.x + clip.width).toDouble(), (clip.y + clip.height).toDouble()), clipBR
            )
            clipX0 = max(clipX0, min(clipTL.x, clipBR.x))
            clipY0 = max(clipY0, min(clipTL.y, clipBR.y))
            clipX1 = min(clipX1, max(clipTL.x, clipBR.x))
            clipY1 = min(clipY1, max(clipTL.y, clipBR.y))
        }
        if (clipX1 - clipX0 < 1 || clipY1 - clipY0 < 1) return

        // When the whole projection fits under the patch cap (any tile at overview zooms), shade
        // it all and let the clip gate only the blit: Swing repaints clip to the damage rect,
        // which shifts every token, and a clip-keyed region would demote every paint to a full
        // reshade. The full patch is also translation-independent, so pans just re-blit. Only a
        // deep-zoomed tile (projection past the cap) falls back to shading the clipped window.
        val fullMode = screenW <= MAX_PATCH_DIM && screenH <= MAX_PATCH_DIM
        val visX0 = if (fullMode) screenX else clipX0
        val visY0 = if (fullMode) screenY else clipY0
        val visX1 = if (fullMode) screenX + screenW else clipX1
        val visY1 = if (fullMode) screenY + screenH else clipY1
        val visW = visX1 - visX0
        val visH = visY1 - visY0

        val cap = min(1.0, min(MAX_PATCH_DIM / visW, MAX_PATCH_DIM / visH))
        val patchW = max(1, (visW * cap).toInt())
        val patchH = max(1, (visH * cap).toInt())

        val regionChanged = if (fullMode) {
            !prevFullMode || patchW != prevPatchW || patchH != prevPatchH
        } else {
            prevFullMode ||
                visX0 != prevVisX0 || visY0 != prevVisY0 || visX1 != prevVisX1 || visY1 != prevVisY1 ||
                screenX != prevScreenX || screenY != prevScreenY || screenW != prevScreenW || screenH != prevScreenH
        }
        if (tile.contentVersion != shadedVersion || regionChanged || patch == null) {
            var cache = patch
            val cacheReusable = cache != null && cache.width >= patchW && cache.height >= patchH
            if (!cacheReusable) {
                cache = BufferedImage(patchW, patchH, BufferedImage.TYPE_INT_RGB)
                patch = cache
            }
            val version = tile.contentVersion
            val dirtyRows = tile.consumeDirtyRows()
            val (neg, mid, pos) = palette()
            val dest = (cache!!.raster.dataBuffer as DataBufferInt).data
            val rowFrom = (visY0 - screenY) / screenH * tile.rows
            val rowTo = (visY1 - screenY) / screenH * tile.rows
            val colFrom = (visX0 - screenX) / screenW * tile.cols
            val colTo = (visX1 - screenX) / screenW * tile.cols
            val banded = dirtyRows != null && cacheReusable && !regionChanged && shadedVersion != Long.MIN_VALUE
            if (banded) {
                // Reshade only the pixel band covering the dirtied cell rows, padded a pixel so
                // boundary pixels pool the same cell window a full shade would give them.
                val rowSpan = rowTo - rowFrom
                val y0 = (((dirtyRows!!.first - rowFrom) / rowSpan * patchH).toInt() - 1).coerceIn(0, patchH)
                val y1 = (ceil((dirtyRows.last + 1 - rowFrom) / rowSpan * patchH).toInt() + 1).coerceIn(y0, patchH)
                if (y1 > y0) {
                    tile.shadePatch(
                        dest, cache.width, patchW, y1 - y0,
                        rowFrom + rowSpan * y0 / patchH, rowFrom + rowSpan * y1 / patchH,
                        colFrom, colTo, neg, mid, pos,
                        destOffset = y0 * cache.width,
                    )
                }
            } else {
                tile.shadePatch(dest, cache.width, patchW, patchH, rowFrom, rowTo, colFrom, colTo, neg, mid, pos)
            }
            shadedVersion = version
            prevFullMode = fullMode
            prevPatchW = patchW
            prevPatchH = patchH
            prevVisX0 = visX0
            prevVisY0 = visY0
            prevVisX1 = visX1
            prevVisY1 = visY1
            prevScreenX = screenX
            prevScreenY = screenY
            prevScreenW = screenW
            prevScreenH = screenH
        }

        val cache = patch ?: return
        val savedTransform = g2.transform
        g2.transform = AffineTransform()
        g2.drawImage(
            cache, visX0.toInt(), visY0.toInt(), visX1.toInt(), visY1.toInt(),
            0, 0, patchW, patchH, null
        )
        g2.transform = savedTransform
    }

    private fun palette(): Triple<Color, Color, Color> = NetworkTheme.current.let { p ->
        if (tile.kind == TileKind.WEIGHT) Triple(p.inhibitorySynapse, p.zeroWeight, p.excitatorySynapse)
        else Triple(p.coolNode, p.neutralMidpoint, p.hotNode)
    }

    companion object {
        /** Maximum patch dimension per axis, bounding work and allocation at extreme zoom. */
        private const val MAX_PATCH_DIM = 4096.0
    }
}
