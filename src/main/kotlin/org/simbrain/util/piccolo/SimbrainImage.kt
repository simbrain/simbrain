package org.simbrain.util.piccolo

import org.piccolo2d.nodes.PImage
import org.piccolo2d.util.PPaintContext
import org.simbrain.util.piccolo.PixelExactImagePainter.Companion.MAX_CACHE_DIM
import java.awt.Image
import java.awt.image.BufferedImage

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
 * a few source pixels are visible, this is more than enough resolution. The drawing itself is
 * done by a [PixelExactImagePainter], which other nodes can use directly.
 */
class SimbrainImage : PImage {

    constructor() : super()
    constructor(image: Image) : super(image)

    private val painter = PixelExactImagePainter()

    override fun invalidatePaint() {
        // PImage's constructor invalidates before this subclass's fields are initialized
        @Suppress("SENSELESS_COMPARISON")
        if (painter != null) painter.invalidate()
        super.invalidatePaint()
    }

    override fun paint(paintContext: PPaintContext) {
        val img = image ?: return
        if (img !is BufferedImage) {
            super.paint(paintContext)
            return
        }
        painter.paint(paintContext.graphics, img, boundsReference)
    }
}
