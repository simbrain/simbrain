package org.simbrain.util.piccolo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PPaintContext
import java.awt.Color
import java.awt.Rectangle
import java.awt.image.BufferedImage

class RasterCachedNodeTest {

    private fun paint(node: RasterCachedNode, scale: Double = 1.0): BufferedImage {
        // The app's paint cycle always validates first, turning deferred invalidation flags
        // into the repaintFrom calls the cache listens to.
        node.validateFullPaint()
        val img = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = Color.BLACK
        g.fillRect(0, 0, 100, 100)
        g.clip = Rectangle(0, 0, 100, 100)
        g.scale(scale, scale)
        val pc = PPaintContext(g)
        pc.setRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING)
        node.fullPaint(pc)
        g.dispose()
        return img
    }

    @Test
    fun `cached blit shows the children`() {
        val node = RasterCachedNode()
        val rect = PPath.createRectangle(10.0, 10.0, 40.0, 40.0).apply {
            paint = Color.RED
            strokePaint = null
        }
        node.addChild(rect)

        paint(node)
        val cached = paint(node)
        assertEquals(Color.RED.rgb, cached.getRGB(30, 30))
        assertEquals(Color.BLACK.rgb, cached.getRGB(80, 80))
    }

    @Test
    fun `a child change invalidates the cache`() {
        val node = RasterCachedNode()
        val rect = PPath.createRectangle(10.0, 10.0, 40.0, 40.0).apply {
            paint = Color.RED
            strokePaint = null
        }
        node.addChild(rect)
        paint(node)
        paint(node)

        rect.paint = Color.GREEN
        val repainted = paint(node)
        assertEquals(Color.GREEN.rgb, repainted.getRGB(30, 30))
    }

    @Test
    fun `a scale change re-renders at the new resolution`() {
        val node = RasterCachedNode()
        node.addChild(PPath.createRectangle(10.0, 10.0, 40.0, 40.0).apply {
            paint = Color.RED
            strokePaint = null
        })
        paint(node)
        paint(node)

        paint(node, scale = 2.0)
        val zoomed = paint(node, scale = 2.0)
        assertEquals(Color.RED.rgb, zoomed.getRGB(90, 90), "cell (45,45) content-space is red at 2x")
        assertEquals(Color.BLACK.rgb, paint(node, scale = 2.0).getRGB(15, 15), "(7.5,7.5) content-space is empty")
    }
}
