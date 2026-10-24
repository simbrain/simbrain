/**
 * The raster cache must be invisible: it paints its children, re-renders on child and scale changes, matches direct
 * painting pixel for pixel once settled (over a known background, at device scale and fractional offsets), and caches
 * only the visible region, so a subtree far larger than the view still caches.
 */
package org.simbrain.util.piccolo

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.piccolo2d.PCamera
import org.piccolo2d.PLayer
import org.piccolo2d.PRoot
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PPaintContext
import java.awt.BasicStroke
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


    private val background = Color(250, 250, 250)

    /** A camera over a layer holding [node], viewing [viewWidth] x [viewHeight] at the given view offset. */
    private fun scene(node: RasterCachedNode, viewWidth: Double, viewHeight: Double, offsetX: Double, offsetY: Double): PCamera {
        val root = PRoot()
        val layer = PLayer()
        val camera = PCamera()
        root.addChild(layer)
        root.addChild(camera)
        camera.addLayer(layer)
        layer.addChild(node)
        camera.setBounds(0.0, 0.0, viewWidth, viewHeight)
        camera.setViewOffset(offsetX, offsetY)
        return camera
    }

    private fun paintView(camera: PCamera, scale: Double): BufferedImage {
        // As the canvas does before each paint: turn invalidated paint into repaints
        camera.root.validateFullPaint()
        val width = (camera.width * scale).toInt()
        val height = (camera.height * scale).toInt()
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = background
        g.fillRect(0, 0, width, height)
        g.scale(scale, scale)
        val context = PPaintContext(g)
        context.setRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING)
        camera.fullPaint(context)
        g.dispose()
        return image
    }

    private fun differingPixels(a: BufferedImage, b: BufferedImage): Int {
        var n = 0
        for (y in 0 until a.height) for (x in 0 until a.width) if (a.getRGB(x, y) != b.getRGB(x, y)) n++
        return n
    }

    private fun lines(count: Int, span: Double) = List(count) { i ->
        PPath.createLine(0.0, i * span / count, span, span - i * span / count).apply {
            stroke = BasicStroke(1.5f)
            strokePaint = Color(60, 60, 60)
        }
    }

    @Test
    fun `a settled cache paints exactly what direct painting does`() {
        val node = RasterCachedNode().apply { background = { this@RasterCachedNodeTest.background } }
        lines(40, 300.0).forEach { node.addChild(it) }
        val camera = scene(node, 320.0, 320.0, 10.3, 7.7)

        node.cachingEnabled = false
        val direct = paintView(camera, 2.0)
        node.cachingEnabled = true
        paintView(camera, 2.0)
        val cached = paintView(camera, 2.0)

        assertTrue(node.isCached, "the second settled frame should have built the cache")
        assertEquals(0, differingPixels(direct, cached))
    }

    @Test
    fun `a change beneath the cache shows on the next settled frame`() {
        val node = RasterCachedNode().apply { background = { this@RasterCachedNodeTest.background } }
        val children = lines(10, 200.0)
        children.forEach { node.addChild(it) }
        val camera = scene(node, 220.0, 220.0, 0.0, 0.0)
        paintView(camera, 2.0)
        paintView(camera, 2.0)
        assertTrue(node.isCached)

        children[3].strokePaint = Color.RED
        camera.root.validateFullPaint()
        assertFalse(node.isCached, "a child's repaint should drop the cache")
        paintView(camera, 2.0)
        val cached = paintView(camera, 2.0)
        node.cachingEnabled = false
        val direct = paintView(camera, 2.0)

        assertEquals(0, differingPixels(direct, cached))
    }

    @Test
    fun `a subtree far larger than the view caches only the visible region`() {
        val node = RasterCachedNode().apply { background = { this@RasterCachedNodeTest.background } }
        // 20,000 px across at 2x is far past the raster size limit, so caching the whole subtree is impossible
        lines(50, 10_000.0).forEach { node.addChild(it) }
        val camera = scene(node, 300.0, 300.0, -4000.0, -4000.0)

        paintView(camera, 2.0)
        val cached = paintView(camera, 2.0)
        assertTrue(node.isCached, "only the visible region should need caching")
        node.cachingEnabled = false
        val direct = paintView(camera, 2.0)

        assertEquals(0, differingPixels(direct, cached))
    }
}
