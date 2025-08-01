package org.simbrain.world.imageworld

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

class ImageSourceTest {

    private lateinit var imageSource: TestableImageSource

    // Concrete implementation for testing
    private class TestableImageSource : ImageSource() {
        
        fun testSetCurrentImage(image: BufferedImage, fireEvents: Boolean = true) {
            setCurrentImage(image, fireEvents)
        }
        
        fun testSetCurrentImageCopy(image: BufferedImage) {
            currentImage = image
        }
    }

    @BeforeEach
    fun setUp() {
        imageSource = TestableImageSource()
    }

    @Test
    fun `test initial state`() {
        assertTrue(imageSource.isEnabled)
        assertNotNull(imageSource.currentImage)
        assertEquals(10, imageSource.width)
        assertEquals(10, imageSource.height)
        assertNotNull(imageSource.events)
    }

    @Test
    fun `test constructor with image`() {
        val testImage = BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB)
        testImage.setRGB(0, 0, Color.BLUE.rgb)
        
        val sourceWithImage = object : ImageSource(testImage) {}
        
        assertEquals(50, sourceWithImage.width)
        assertEquals(50, sourceWithImage.height)
        assertEquals(Color.BLUE.rgb, sourceWithImage.currentImage.getRGB(0, 0))
    }

    @Test
    fun `test enabled property`() {
        assertTrue(imageSource.isEnabled)

        imageSource.isEnabled = false
        assertFalse(imageSource.isEnabled)

        imageSource.isEnabled = true
        assertTrue(imageSource.isEnabled)
    }

    @Test
    fun `test set current image with events`() {
        var imageUpdateEventFired = false
        var resizeEventFired = false
        
        imageSource.events.imageUpdate.on { imageUpdateEventFired = true }
        imageSource.events.resize.on { resizeEventFired = true }
        
        val newImage = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        newImage.setRGB(0, 0, Color.RED.rgb)
        
        imageSource.testSetCurrentImage(newImage, true)
        
        assertEquals(30, imageSource.width)
        assertEquals(30, imageSource.height)
        assertEquals(Color.RED.rgb, imageSource.currentImage.getRGB(0, 0))
        assertTrue(imageUpdateEventFired)
        assertTrue(resizeEventFired)  // Size changed from 10x10 to 30x30
    }

    @Test
    fun `test set current image without events`() {
        var imageUpdateEventFired = false
        var resizeEventFired = false
        
        imageSource.events.imageUpdate.on { imageUpdateEventFired = true }
        imageSource.events.resize.on { resizeEventFired = true }
        
        val newImage = BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB)
        newImage.setRGB(0, 0, Color.GREEN.rgb)
        
        imageSource.testSetCurrentImage(newImage, false)
        
        assertEquals(40, imageSource.width)
        assertEquals(40, imageSource.height)
        assertEquals(Color.GREEN.rgb, imageSource.currentImage.getRGB(0, 0))
        assertFalse(imageUpdateEventFired)
        assertFalse(resizeEventFired)
    }

    @Test
    fun `test set current image same size no resize event`() {
        var imageUpdateEventFired = false
        var resizeEventFired = false
        
        imageSource.events.imageUpdate.on { imageUpdateEventFired = true }
        imageSource.events.resize.on { resizeEventFired = true }
        
        // Same size as initial image (10x10)
        val newImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        newImage.setRGB(0, 0, Color.YELLOW.rgb)
        
        imageSource.testSetCurrentImage(newImage, true)
        
        assertEquals(10, imageSource.width)
        assertEquals(10, imageSource.height)
        assertEquals(Color.YELLOW.rgb, imageSource.currentImage.getRGB(0, 0))
        assertTrue(imageUpdateEventFired)
        assertFalse(resizeEventFired)  // No size change
    }

    @Test
    fun `test set current image copy`() {
        val originalImage = BufferedImage(25, 25, BufferedImage.TYPE_INT_RGB)
        originalImage.setRGB(0, 0, Color.MAGENTA.rgb)
        
        imageSource.testSetCurrentImageCopy(originalImage)
        
        // Should be copied, not the same instance
        assertNotSame(originalImage, imageSource.currentImage)
        assertEquals(25, imageSource.width)
        assertEquals(25, imageSource.height)
        assertEquals(Color.MAGENTA.rgb, imageSource.currentImage.getRGB(0, 0))
        
        // Modify original - should not affect the copy
        originalImage.setRGB(0, 0, Color.CYAN.rgb)
        assertEquals(Color.MAGENTA.rgb, imageSource.currentImage.getRGB(0, 0))
    }

    @Test
    fun `test fire image update when enabled`() {
        var eventFired = false
        imageSource.events.imageUpdate.on { eventFired = true }
        
        assertTrue(imageSource.isEnabled)
        imageSource.fireImageUpdate()
        assertTrue(eventFired)
    }

    @Test
    fun `test fire image update when disabled`() {
        var eventFired = false
        imageSource.events.imageUpdate.on { eventFired = true }

        imageSource.isEnabled = false
        imageSource.fireImageUpdate()
        assertFalse(eventFired)
    }

    @Test
    fun `test clear current image`() {
        val coloredImage = BufferedImage(35, 35, BufferedImage.TYPE_INT_RGB)
        coloredImage.setRGB(0, 0, Color.ORANGE.rgb)
        
        imageSource.testSetCurrentImage(coloredImage)
        assertEquals(Color.ORANGE.rgb, imageSource.currentImage.getRGB(0, 0))
        
        imageSource.clearCurrentImage()
        
        // Should be cleared to black but same dimensions
        assertEquals(Color.BLACK.rgb, imageSource.currentImage.getRGB(0, 0))
        assertEquals(35, imageSource.width)
        assertEquals(35, imageSource.height)
    }

    @Test
    fun `test read resolve`() {
        // Test that deserialization properly recreates events
        val resolvedSource = imageSource.readResolve() as TestableImageSource
        
        assertNotNull(resolvedSource.events)
        assertSame(imageSource, resolvedSource)
    }

    @Test
    fun `test dimensions`() {
        assertEquals(10, imageSource.width)
        assertEquals(10, imageSource.height)
        
        val newImage = BufferedImage(75, 100, BufferedImage.TYPE_INT_RGB)
        imageSource.testSetCurrentImage(newImage)
        
        assertEquals(75, imageSource.width)
        assertEquals(100, imageSource.height)
    }

    @Test
    fun `test events object accessibility`() {
        val events = imageSource.events
        assertNotNull(events)
        assertNotNull(events.imageUpdate)
        assertNotNull(events.resize)
    }

    @Test
    fun `test multiple event listeners`() {
        var listener1Fired = false
        var listener2Fired = false
        
        imageSource.events.imageUpdate.on { listener1Fired = true }
        imageSource.events.imageUpdate.on { listener2Fired = true }
        
        imageSource.fireImageUpdate()
        
        assertTrue(listener1Fired)
        assertTrue(listener2Fired)
    }
}