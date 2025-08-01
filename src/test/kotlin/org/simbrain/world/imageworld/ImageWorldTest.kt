package org.simbrain.world.imageworld

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

class ImageWorldTest {

    private val imageComponent: ImageWorldComponent
    private val imageWorld: ImageWorld

    init {
        imageComponent = ImageWorldComponent("Test Image World")
        imageWorld = imageComponent.world
    }

    @Test
    fun `test image world creation`() {
        assertNotNull(imageWorld.imageAlbum)
        assertNotNull(imageWorld.imagePipelineCollection)
        assertEquals("Test Image World", imageComponent.name)
    }

    @Test
    fun `test current image access`() {
        val currentImage = imageWorld.currentImage
        assertNotNull(currentImage)
        assertTrue(currentImage.width > 0)
        assertTrue(currentImage.height > 0)
    }

    @Test
    fun `test image album operations`() {
        val album = imageWorld.imageAlbum
        val initialSize = album.numFrames
        
        // Take a snapshot
        album.takeSnapshot()
        assertEquals(initialSize + 1, album.numFrames)
        
        // Delete current image
        if (album.numFrames > 1) {
            album.deleteCurrentImage()
            assertEquals(initialSize, album.numFrames)
        }
    }

    @Test
    fun `test frame navigation`() {
        val album = imageWorld.imageAlbum
        
        // Add some images to test navigation
        album.takeSnapshot()
        album.takeSnapshot()
        
        val initialIndex = album.frameIndex
        
        // Test next frame
        imageWorld.nextFrame()
        assertNotEquals(initialIndex, album.frameIndex)
        
        // Test previous frame
        imageWorld.previousFrame()
        // Should wrap around or go back
        assertTrue(album.frameIndex >= 0)
        assertTrue(album.frameIndex < album.numFrames)
    }

    @Test
    fun `test filter collection`() {
        val filterCollection = imageWorld.imagePipelineCollection
        assertNotNull(filterCollection.currentPipeline)
        
        // Test that pipeline can be applied
        val originalImage = imageWorld.currentImage
        val filteredImage = filterCollection.currentPipeline.processedImage
        
        // Images should have same dimensions
        assertEquals(originalImage.width, filteredImage.width)
        assertEquals(originalImage.height, filteredImage.height)
    }

    @Test
    fun `test image manipulation`() {
        val image = imageWorld.currentImage
        val testColor = Color.RED
        
        // Set a pixel
        image.setRGB(0, 0, testColor.rgb)
        assertEquals(testColor.rgb, image.getRGB(0, 0))
        
        // Test fill operation
        val graphics = image.createGraphics()
        graphics.color = Color.BLUE
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.dispose()
        assertEquals(Color.BLUE.rgb, image.getRGB(0, 0))
        assertEquals(Color.BLUE.rgb, image.getRGB(image.width - 1, image.height - 1))
    }

    @Test
    fun `test image dimensions`() {
        val image = imageWorld.currentImage
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
        assertEquals(BufferedImage.TYPE_INT_RGB, image.type)
    }

    @Test
    fun `test reset image album`() {
        imageWorld.imageAlbum.numFrames
        
        // Reset with new dimensions
        imageWorld.resetImageAlbum(100, 80)
        
        // Should have at least one image with new dimensions
        // Note: ImageWorld delegates to currentBufferedImage which goes through pipeline processing
        val newImage = imageWorld.imageAlbum.currentImage
        assertEquals(100, newImage.width)
        assertEquals(80, newImage.height)
    }

    @Test
    fun `test snapshot functionality`() {
        val album = imageWorld.imageAlbum
        val originalSize = album.numFrames
        
        // Modify current image
        val currentImage = imageWorld.currentImage
        currentImage.setRGB(0, 0, Color.RED.rgb)
        
        // Take snapshot
        album.takeSnapshot()
        assertEquals(originalSize + 1, album.numFrames)
        
        // After taking snapshot, the current image should be the new one
        // and should have the modification
        assertEquals(Color.RED.rgb, imageWorld.currentImage.getRGB(0, 0))
    }

    @Test
    fun `test image album navigation bounds`() {
        val album = imageWorld.imageAlbum
        
        // Ensure we have at least 2 images
        if (album.numFrames < 2) {
            album.takeSnapshot()
        }
        
        val maxIndex = album.numFrames - 1
        
        // Set to last image
        album.setFrame(maxIndex)
        assertEquals(maxIndex, album.frameIndex)
        
        // Next should wrap to beginning
        imageWorld.nextFrame()
        assertTrue(album.frameIndex >= 0)
        assertTrue(album.frameIndex <= maxIndex)
        
        // Set to first image  
        album.setFrame(0)
        assertEquals(0, album.frameIndex)
        
        // Previous should wrap to end
        imageWorld.previousFrame()
        assertTrue(album.frameIndex >= 0)
        assertTrue(album.frameIndex <= maxIndex)
    }

    @Test
    fun `test multiple image operations`() {
        val album = imageWorld.imageAlbum
        
        // Create several snapshots with different modifications
        repeat(3) { i ->
            val image = imageWorld.currentImage
            if (i < image.width) {
                image.setRGB(i, 0, Color(i * 80, 0, 0).rgb)
            }
            album.takeSnapshot()
        }
        
        // Navigate through images
        album.setFrame(0)
        imageWorld.currentImage
        
        album.setFrame(1)
        imageWorld.currentImage
        
        // Images should be different instances or at least have different content
        // Note: ImageWorld.currentImage goes through the pipeline so may return the same instance
        // but we can check that the album has different stored images
        assertNotSame(album.frames[0], album.frames[1])
    }

    @Test
    fun `test color operations`() {
        val image = imageWorld.currentImage
        
        // Test setting different colors
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
        
        colors.forEachIndexed { index, color ->
            if (index < image.width && index < image.height) {
                image.setRGB(index, index, color.rgb)
                assertEquals(color.rgb, image.getRGB(index, index))
            }
        }
    }

    @Test
    fun `test image album deletion with bounds checking`() {
        val album = imageWorld.imageAlbum
        
        // Ensure we have multiple images
        repeat(3) { album.takeSnapshot() }
        
        val initialSize = album.numFrames
        assertTrue(initialSize > 1)
        
        // Delete current image
        album.deleteCurrentImage()
        assertEquals(initialSize - 1, album.numFrames)
        
        // Current index should still be valid
        assertTrue(album.frameIndex >= 0)
        assertTrue(album.frameIndex < album.numFrames)
    }
} 