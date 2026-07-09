package org.simbrain.network.compositor

import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import org.simbrain.util.NetworkTheme
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.max
import kotlin.math.min

/**
 * Paints a [TensorTile] by shading its value buffer straight into a screen-resolution patch via
 * [TensorTile.shadePatch] — no full-resolution intermediate image exists, so shading cost is
 * bounded by on-screen pixels (capped at [MAX_PATCH_DIM] per axis), never by tensor size. The
 * patch rebuilds when the tile's content version moves or the visible region changes (pan/zoom);
 * [markStale] forces one after palette switches, which don't move content versions. Drawing the
 * patch at 1:1 in device space also sidesteps Java2D's grid-shift scaling artifact (the same
 * scheme as SimbrainImage, without its pre-shaded source).
 */
class TilePatchNode(val tile: TensorTile) : PNode() {

    private var patch: BufferedImage? = null
    private var shadedVersion = Long.MIN_VALUE

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

        var visX0 = screenX
        var visY0 = screenY
        var visX1 = screenX + screenW
        var visY1 = screenY + screenH
        g2.clipBounds?.let { clip ->
            val clipTL = Point2D.Double()
            val clipBR = Point2D.Double()
            transform.transform(Point2D.Double(clip.x.toDouble(), clip.y.toDouble()), clipTL)
            transform.transform(
                Point2D.Double((clip.x + clip.width).toDouble(), (clip.y + clip.height).toDouble()), clipBR
            )
            visX0 = max(visX0, min(clipTL.x, clipBR.x))
            visY0 = max(visY0, min(clipTL.y, clipBR.y))
            visX1 = min(visX1, max(clipTL.x, clipBR.x))
            visY1 = min(visY1, max(clipTL.y, clipBR.y))
        }
        val visW = visX1 - visX0
        val visH = visY1 - visY0
        if (visW < 1 || visH < 1) return

        val cap = min(1.0, min(MAX_PATCH_DIM / visW, MAX_PATCH_DIM / visH))
        val patchW = max(1, (visW * cap).toInt())
        val patchH = max(1, (visH * cap).toInt())

        val regionChanged = visX0 != prevVisX0 || visY0 != prevVisY0 || visX1 != prevVisX1 || visY1 != prevVisY1 ||
            screenX != prevScreenX || screenY != prevScreenY || screenW != prevScreenW || screenH != prevScreenH
        if (tile.contentVersion != shadedVersion || regionChanged || patch == null) {
            var cache = patch
            if (cache == null || cache.width < patchW || cache.height < patchH) {
                cache = BufferedImage(patchW, patchH, BufferedImage.TYPE_INT_RGB)
                patch = cache
            }
            val version = tile.contentVersion
            val (neg, mid, pos) = palette()
            tile.shadePatch(
                (cache.raster.dataBuffer as DataBufferInt).data, cache.width, patchW, patchH,
                (visY0 - screenY) / screenH * tile.rows, (visY1 - screenY) / screenH * tile.rows,
                (visX0 - screenX) / screenW * tile.cols, (visX1 - screenX) / screenW * tile.cols,
                neg, mid, pos,
            )
            shadedVersion = version
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
