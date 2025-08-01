package org.simbrain.world.imageworld

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.ImageIcon

class ImageAlbumTest {

    private lateinit var imageAlbum: ImageAlbum

    @BeforeEach
    fun setUp() {
        imageAlbum = ImageAlbum()
    }

    @Test
    fun `test initial state`() {
        assertEquals(0, imageAlbum.numFrames)
        assertEquals(0, imageAlbum.frameIndex)
        assertNotNull(imageAlbum.currentImage)
        assertEquals(10, imageAlbum.currentImage.width)
        assertEquals(10, imageAlbum.currentImage.height)
    }

    @Test
    fun `test add single image`() {
        val testImage = BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB)
        testImage.setRGB(0, 0, Color.RED.rgb)
        
        imageAlbum.addImage(testImage)
        
        assertEquals(1, imageAlbum.numFrames)
        assertEquals(0, imageAlbum.frameIndex)
        assertEquals(Color.RED.rgb, imageAlbum.currentImage.getRGB(0, 0))
    }

    @Test
    fun `test add multiple images`() {
        val image1 = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        val image2 = BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB)
        val image3 = BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB)
        
        image1.setRGB(0, 0, Color.RED.rgb)
        image2.setRGB(0, 0, Color.GREEN.rgb)
        image3.setRGB(0, 0, Color.BLUE.rgb)
        
        imageAlbum.addImage(image1)
        imageAlbum.addImage(image2)
        imageAlbum.addImage(image3)
        
        assertEquals(3, imageAlbum.numFrames)
        assertEquals(2, imageAlbum.frameIndex)  // Should be on last added image
        assertEquals(Color.BLUE.rgb, imageAlbum.currentImage.getRGB(0, 0))
    }

    @Test
    fun `test frame navigation`() {
        val image1 = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        val image2 = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        val image3 = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        
        image1.setRGB(0, 0, Color.RED.rgb)
        image2.setRGB(0, 0, Color.GREEN.rgb)
        image3.setRGB(0, 0, Color.BLUE.rgb)
        
        imageAlbum.addImage(image1)
        imageAlbum.addImage(image2)
        imageAlbum.addImage(image3)
        
        // Test setFrame
        imageAlbum.setFrame(0)
        assertEquals(0, imageAlbum.frameIndex)
        assertEquals(Color.RED.rgb, imageAlbum.currentImage.getRGB(0, 0))
        
        imageAlbum.setFrame(1)
        assertEquals(1, imageAlbum.frameIndex)
        assertEquals(Color.GREEN.rgb, imageAlbum.currentImage.getRGB(0, 0))
        
        // Test nextFrame navigation
        imageAlbum.setFrame(0)
        imageAlbum.nextFrame()
        assertEquals(1, imageAlbum.frameIndex)
        
        imageAlbum.nextFrame()
        assertEquals(2, imageAlbum.frameIndex)
        
        // Test wrap around
        imageAlbum.nextFrame()
        assertEquals(0, imageAlbum.frameIndex)
        
        // Test previousFrame navigation
        imageAlbum.setFrame(2)
        imageAlbum.previousFrame()
        assertEquals(1, imageAlbum.frameIndex)
        
        imageAlbum.previousFrame()
        assertEquals(0, imageAlbum.frameIndex)
        
        // Test wrap around backwards
        imageAlbum.previousFrame()
        assertEquals(2, imageAlbum.frameIndex)
    }

    @Test
    fun `test set frame bounds checking`() {
        val image1 = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        val image2 = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        
        imageAlbum.addImage(image1)
        imageAlbum.addImage(image2)
        
        val initialFrame = imageAlbum.frameIndex
        
        // Test invalid indices are ignored
        imageAlbum.setFrame(-1)
        assertEquals(initialFrame, imageAlbum.frameIndex)
        
        imageAlbum.setFrame(10)
        assertEquals(initialFrame, imageAlbum.frameIndex)
    }

    @Test
    fun `test take snapshot`() {
        val originalImage = BufferedImage(25, 25, BufferedImage.TYPE_INT_RGB)
        originalImage.setRGB(0, 0, Color.YELLOW.rgb)
        
        imageAlbum.currentImage = originalImage
        val initialFrames = imageAlbum.numFrames
        
        imageAlbum.takeSnapshot()
        
        assertEquals(initialFrames + 1, imageAlbum.numFrames)
        assertEquals(Color.YELLOW.rgb, imageAlbum.currentImage.getRGB(0, 0))
        
        // Verify it's a copy, not the same instance
        assertNotSame(originalImage, imageAlbum.frames.last())
    }

    @Test
    fun `test delete current image with multiple images`() {
        val image1 = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        val image2 = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        val image3 = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        
        image1.setRGB(0, 0, Color.RED.rgb)
        image2.setRGB(0, 0, Color.GREEN.rgb)
        image3.setRGB(0, 0, Color.BLUE.rgb)
        
        imageAlbum.addImage(image1)
        imageAlbum.addImage(image2)
        imageAlbum.addImage(image3)
        
        // Delete middle image
        imageAlbum.setFrame(1)
        val initialFrames = imageAlbum.numFrames
        imageAlbum.deleteCurrentImage()
        
        assertEquals(initialFrames - 1, imageAlbum.numFrames)
        assertEquals(2, imageAlbum.numFrames)
        
        // Frame index should be adjusted
        assertTrue(imageAlbum.frameIndex >= 0)
        assertTrue(imageAlbum.frameIndex < imageAlbum.numFrames)
    }

    @Test
    fun `test delete last remaining image creates default`() {
        val testImage = BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB)
        imageAlbum.addImage(testImage)
        
        imageAlbum.deleteCurrentImage()
        
        assertEquals(1, imageAlbum.numFrames)
        assertEquals(0, imageAlbum.frameIndex)
        assertEquals(60, imageAlbum.currentImage.width)
        assertEquals(60, imageAlbum.currentImage.height)
    }

    @Test
    fun `test reset album`() {
        val image1 = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        val image2 = BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB)
        
        imageAlbum.addImage(image1)
        imageAlbum.addImage(image2)
        
        imageAlbum.reset(100, 80)
        
        assertEquals(1, imageAlbum.numFrames)
        assertEquals(0, imageAlbum.frameIndex)
        assertEquals(100, imageAlbum.currentImage.width)
        assertEquals(80, imageAlbum.currentImage.height)
    }

    @Test
    fun `test load image from empty filename`() {
        imageAlbum.loadImage("")
        
        assertEquals(10, imageAlbum.currentImage.width)
        assertEquals(10, imageAlbum.currentImage.height)
        assertEquals(0, imageAlbum.numFrames)  // loadImage clears frames
    }

    @Test
    fun `test load image from ImageIcon`() {
        val iconImage = BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB)
        iconImage.setRGB(0, 0, Color.MAGENTA.rgb)
        val icon = ImageIcon(iconImage)
        
        imageAlbum.loadImage(icon)
        
        assertEquals(32, imageAlbum.currentImage.width)
        assertEquals(32, imageAlbum.currentImage.height)
        assertEquals(Color.MAGENTA.rgb, imageAlbum.currentImage.getRGB(0, 0))
    }

    @Test
    fun `test save current frame`() {
        val originalImage = BufferedImage(25, 25, BufferedImage.TYPE_INT_RGB)
        originalImage.setRGB(0, 0, Color.CYAN.rgb)
        
        imageAlbum.addImage(originalImage)
        
        // Modify current image
        imageAlbum.currentImage.setRGB(1, 1, Color.ORANGE.rgb)
        
        // Save the frame
        imageAlbum.saveCurrentFrame()
        
        // Check that the frame was updated
        assertEquals(Color.ORANGE.rgb, imageAlbum.frames[imageAlbum.frameIndex].getRGB(1, 1))
    }

    @Test
    fun `test frame access`() {
        val image1 = BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB)
        val image2 = BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB)
        
        image1.setRGB(0, 0, Color.RED.rgb)
        image2.setRGB(0, 0, Color.GREEN.rgb)
        
        imageAlbum.addImage(image1)
        imageAlbum.addImage(image2)
        
        val frames = imageAlbum.frames
        assertEquals(2, frames.size)
        assertEquals(Color.RED.rgb, frames[0].getRGB(0, 0))
        assertEquals(Color.GREEN.rgb, frames[1].getRGB(0, 0))
        
        // Verify it's a read-only view
        assertThrows(UnsupportedOperationException::class.java) {
            (frames as MutableList<BufferedImage>).add(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB))
        }
    }

    @Test
    fun `test constructor with filename and image`() {
        val testImage = BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB)
        testImage.setRGB(0, 0, Color.PINK.rgb)
        
        val albumWithImage = ImageAlbum("test.png", testImage)
        
        assertEquals(40, albumWithImage.currentImage.width)
        assertEquals(40, albumWithImage.currentImage.height)
        assertEquals(Color.PINK.rgb, albumWithImage.currentImage.getRGB(0, 0))
    }

    @Test
    fun `test image dimensions`() {
        assertEquals(10, imageAlbum.width)
        assertEquals(10, imageAlbum.height)
        
        val largerImage = BufferedImage(100, 200, BufferedImage.TYPE_INT_RGB)
        imageAlbum.addImage(largerImage)
        
        assertEquals(100, imageAlbum.width)
        assertEquals(200, imageAlbum.height)
    }

    @Test
    fun `test clear current image`() {
        val coloredImage = BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB)
        coloredImage.setRGB(0, 0, Color.RED.rgb)
        
        imageAlbum.addImage(coloredImage)
        assertEquals(Color.RED.rgb, imageAlbum.currentImage.getRGB(0, 0))
        
        imageAlbum.clearCurrentImage()
        
        // Should be black (default for cleared image)
        assertEquals(Color.BLACK.rgb, imageAlbum.currentImage.getRGB(0, 0))
        assertEquals(50, imageAlbum.currentImage.width)
        assertEquals(50, imageAlbum.currentImage.height)
    }

}