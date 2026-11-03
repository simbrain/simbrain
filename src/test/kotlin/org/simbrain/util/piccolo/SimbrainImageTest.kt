/**
 * SimbrainImage pre-scales the visible part of its image for crisp nearest-neighbor pixels; the scaled copy must keep
 * the source's transparency, so overlays such as a spiking array's spike image show what is beneath them.
 */
package org.simbrain.util.piccolo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.piccolo2d.util.PPaintContext
import org.simbrain.util.toOverlay
import java.awt.Color
import java.awt.image.BufferedImage

class SimbrainImageTest {

    private fun paintOverWhite(node: SimbrainImage, scale: Double, width: Int, height: Int): BufferedImage {
        val target = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = target.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, width, height)
        g.scale(scale, scale)
        node.fullPaint(PPaintContext(g))
        g.dispose()
        return target
    }

    @Test
    fun `transparent pixels of a scaled overlay stay transparent`() {
        val spikes = booleanArrayOf(true, false, false, false)
        val node = SimbrainImage(spikes.toOverlay(4, 1, Color.RED)).apply { setBounds(0.0, 0.0, 4.0, 1.0) }

        val painted = paintOverWhite(node, scale = 10.0, width = 40, height = 10)

        assertEquals(Color.RED.rgb, painted.getRGB(5, 5), "spiking pixel")
        assertEquals(Color.WHITE.rgb, painted.getRGB(25, 5), "a non-spiking pixel was not transparent")
    }

    @Test
    fun `an opaque image still draws its own colors`() {
        val source = BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB).apply {
            setRGB(0, 0, Color.BLUE.rgb)
            setRGB(1, 0, Color.BLACK.rgb)
        }
        val node = SimbrainImage(source).apply { setBounds(0.0, 0.0, 2.0, 1.0) }

        val painted = paintOverWhite(node, scale = 10.0, width = 20, height = 10)

        assertEquals(Color.BLUE.rgb, painted.getRGB(5, 5))
        assertEquals(Color.BLACK.rgb, painted.getRGB(15, 5))
    }
}
