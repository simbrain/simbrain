/**
 * A Piccolo canvas that renders into a device-resolution software image and copies it to the screen in one draw.
 *
 * On macOS, Java2D's Metal pipeline rasterizes every anti-aliased shape on the CPU and then uploads that shape's
 * coverage mask to the GPU as a separate operation. A scene of many small shapes, such as a network's neurons, pays
 * that per-shape round trip hundreds of times a frame: the Cortical Layers canvas took ~65 ms per frame on screen
 * versus ~10 ms drawn into an image. Rendering into an image and uploading it once keeps the same resolution and
 * anti-aliasing while costing one texture upload per frame.
 */
package org.simbrain.util.piccolo

import org.piccolo2d.PCanvas
import java.awt.AlphaComposite
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsDevice
import java.awt.Rectangle
import java.awt.image.BufferedImage

open class BufferedPCanvas : PCanvas() {

    private var buffer: BufferedImage? = null

    /**
     * Repaints only the damaged region into the image, so partial repaints stay partial, then draws the image through
     * the screen graphics' clip.
     */
    override fun paintComponent(g: Graphics) {
        if (width <= 0 || height <= 0) return
        // Render at the destination's own resolution (2x on a Retina screen, 1x into a plain image); anything but a
        // plain scale, such as a rotated print transform, is painted directly
        val transform = (g as Graphics2D).transform
        // Drawing into an image is already software rendering (e.g. a parent's offscreen pass, or an export), so a
        // second buffer would only add a copy
        val toImage = g.deviceConfiguration.device.type == GraphicsDevice.TYPE_IMAGE_BUFFER
        if (toImage || transform.shearX != 0.0 || transform.shearY != 0.0 || transform.scaleX != transform.scaleY || transform.scaleX <= 0) {
            super.paintComponent(g)
            return
        }
        val scale = transform.scaleX
        val image = bufferFor(scale)
        val clip = g.clipBounds ?: Rectangle(0, 0, width, height)
        val offscreen = image.createGraphics()
        try {
            offscreen.scale(scale, scale)
            offscreen.clip = clip
            if (!isOpaque) {
                offscreen.composite = AlphaComposite.Clear
                offscreen.fillRect(clip.x, clip.y, clip.width, clip.height)
                offscreen.composite = AlphaComposite.SrcOver
            }
            super.paintComponent(offscreen)
        } finally {
            offscreen.dispose()
        }
        // Drawn in device pixels, so the image lands pixel for pixel with no resampling at any display scale
        val screen = g.create() as Graphics2D
        try {
            screen.scale(1 / scale, 1 / scale)
            screen.drawImage(image, 0, 0, null)
        } finally {
            screen.dispose()
        }
    }

    private fun bufferFor(scale: Double): BufferedImage {
        val pixelWidth = Math.ceil(width * scale).toInt()
        val pixelHeight = Math.ceil(height * scale).toInt()
        buffer?.let { if (it.width == pixelWidth && it.height == pixelHeight) return it }
        return BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB_PRE).also { buffer = it }
    }
}
