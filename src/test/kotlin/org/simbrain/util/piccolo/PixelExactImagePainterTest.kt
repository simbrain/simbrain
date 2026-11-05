/**
 * The pixel-exact painters: source pixels land where the inverse transform puts them, the area outside the image stays
 * untouched, and a clipped draw paints exactly the same device pixels as an unclipped one, which is what keeps an
 * enlarged image's pixel grid from jumping as the view clips through it. The sprite cache also reuses one enlarged
 * copy for every position of a moving sprite.
 */
package org.simbrain.util.piccolo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage

class PixelExactImagePainterTest {

    /** A 4×2 image whose pixels all differ, so any mis-sampled pixel shows. */
    private fun distinctPixels() = BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB).apply {
        for (y in 0 until 2) for (x in 0 until 4) {
            setRGB(x, y, Color(40 + 50 * x, 60 + 120 * y, 200).rgb)
        }
    }

    private fun canvas(size: Int = 120) = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB).apply {
        createGraphics().apply {
            color = Color.WHITE
            fillRect(0, 0, size, size)
            dispose()
        }
    }

    private fun draw(target: BufferedImage, image: BufferedImage, setup: Graphics2D.() -> Unit) {
        val g = target.createGraphics()
        g.setup()
        PixelExactImagePainter().paint(g, image, Rectangle2D.Double(-2.0, -1.0, 4.0, 2.0))
        g.dispose()
    }

    private val rotated: Graphics2D.() -> Unit = {
        translate(60.3, 60.7)
        rotate(Math.toRadians(30.0))
        scale(13.7, 13.7)
    }

    @Test
    fun `a quarter turn puts each source pixel where the rotation sends it`() {
        val image = distinctPixels()
        val target = canvas()
        draw(target, image) {
            translate(60.0, 60.0)
            rotate(Math.PI / 2)
            scale(10.0, 10.0)
        }

        // Rotating a quarter turn clockwise on screen sends image x to screen y and image y to screen -x
        for (y in 0 until 2) for (x in 0 until 4) {
            val screenX = 60 - ((y - 1) * 10 + 5)
            val screenY = 60 + ((x - 2) * 10 + 5)
            assertEquals(image.getRGB(x, y), target.getRGB(screenX, screenY), "source pixel ($x, $y)")
        }
        assertEquals(Color.WHITE.rgb, target.getRGB(5, 5), "outside the image")
    }

    @Test
    fun `a clipped rotated draw matches the unclipped draw pixel for pixel`() {
        val image = distinctPixels()
        val full = canvas()
        draw(full, image, rotated)

        for (clip in listOf(Rectangle2D.Double(55.0, 40.0, 30.0, 50.0), Rectangle2D.Double(20.0, 61.0, 70.0, 13.0))) {
            val clipped = canvas()
            draw(clipped, image) {
                this.clip = clip
                rotated()
            }
            for (y in 0 until full.height) for (x in 0 until full.width) {
                val expected = if (clip.contains(x + 0.5, y + 0.5)) full.getRGB(x, y) else Color.WHITE.rgb
                assertEquals(expected, clipped.getRGB(x, y), "device pixel ($x, $y) with clip $clip")
            }
        }
    }

    @Test
    fun `changed pixels in the same image are redrawn only after invalidate`() {
        val image = distinctPixels()
        val painter = PixelExactImagePainter()
        fun paint(): BufferedImage = canvas().also { target ->
            val g = target.createGraphics()
            g.rotated()
            painter.paint(g, image, Rectangle2D.Double(-2.0, -1.0, 4.0, 2.0))
            g.dispose()
        }
        val center = paint().getRGB(60, 60)

        image.createGraphics().apply {
            color = Color.MAGENTA
            fillRect(0, 0, 4, 2)
            dispose()
        }
        assertEquals(center, paint().getRGB(60, 60), "an unchanged view reuses its cache")

        painter.invalidate()
        assertEquals(Color.MAGENTA.rgb, paint().getRGB(60, 60))
    }

    private fun drawSprite(target: BufferedImage, image: BufferedImage, setup: Graphics2D.() -> Unit): Boolean {
        val g = target.createGraphics()
        g.setup()
        val drawn = PixelExactSpriteCache.paint(g, image, Rectangle2D.Double(-2.0, -1.0, 4.0, 2.0))
        g.dispose()
        return drawn
    }

    @Test
    fun `a sprite moved by whole pixels is the same copy shifted`() {
        PixelExactSpriteCache.clear()
        val image = distinctPixels()
        val here = canvas()
        val there = canvas()
        drawSprite(here, image) { translate(50.0, 50.0); scale(9.5, 9.5) }
        drawSprite(there, image) { translate(57.0, 46.0); scale(9.5, 9.5) }

        assertEquals(1, PixelExactSpriteCache.size, "moving reuses the cached copy")
        for (y in 20 until 80) for (x in 20 until 80) {
            assertEquals(here.getRGB(x, y), there.getRGB(x + 7, y - 4), "device pixel ($x, $y)")
        }
    }

    @Test
    fun `a sprite at a fractional position snaps to a whole device pixel`() {
        PixelExactSpriteCache.clear()
        val image = distinctPixels()
        val snapped = canvas()
        val fractional = canvas()
        drawSprite(snapped, image) { translate(50.0, 50.0); rotate(0.4); scale(9.5, 9.5) }
        drawSprite(fractional, image) { translate(50.3, 49.8); rotate(0.4); scale(9.5, 9.5) }

        for (y in 0 until snapped.height) for (x in 0 until snapped.width) {
            assertEquals(snapped.getRGB(x, y), fractional.getRGB(x, y), "device pixel ($x, $y)")
        }
    }

    @Test
    fun `a clipped sprite matches the unclipped sprite pixel for pixel`() {
        PixelExactSpriteCache.clear()
        val image = distinctPixels()
        val full = canvas()
        drawSprite(full, image, rotated)
        val clip = Rectangle2D.Double(55.0, 40.0, 30.0, 50.0)
        val clipped = canvas()
        drawSprite(clipped, image) {
            this.clip = clip
            rotated()
        }
        for (y in 0 until full.height) for (x in 0 until full.width) {
            val expected = if (clip.contains(x + 0.5, y + 0.5)) full.getRGB(x, y) else Color.WHITE.rgb
            assertEquals(expected, clipped.getRGB(x, y), "device pixel ($x, $y)")
        }
    }

    @Test
    fun `an enlargement too big to cache is left to the caller`() {
        PixelExactSpriteCache.clear()
        val drawn = drawSprite(canvas(), distinctPixels()) { scale(1000.0, 1000.0) }
        assertEquals(false, drawn)
        assertEquals(0, PixelExactSpriteCache.size)
    }

    @Test
    fun `a byte backed image paints the same colors as an int backed one`() {
        val ints = distinctPixels()
        val bytes = BufferedImage(4, 2, BufferedImage.TYPE_4BYTE_ABGR).apply {
            createGraphics().apply { drawImage(ints, 0, 0, null); dispose() }
        }
        val fromInts = canvas()
        val fromBytes = canvas()
        draw(fromInts, ints, rotated)
        draw(fromBytes, bytes, rotated)
        for (y in 0 until fromInts.height) for (x in 0 until fromInts.width) {
            assertEquals(fromInts.getRGB(x, y), fromBytes.getRGB(x, y), "device pixel ($x, $y)")
        }
    }
}
