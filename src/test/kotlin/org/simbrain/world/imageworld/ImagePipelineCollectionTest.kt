package org.simbrain.world.imageworld.transformations

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.world.imageworld.ImageAlbum
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import java.awt.Color
import java.awt.image.BufferedImage

class ImagePipelineCollectionTest {

    private lateinit var imageAlbum: ImageAlbum
    private lateinit var pipelineCollection: ImagePipelineCollection

    @BeforeEach
    fun setUp() {
        imageAlbum = ImageAlbum()
        pipelineCollection = ImagePipelineCollection(imageAlbum)
    }

    @Test
    fun `test initial state`() {
        assertNotNull(pipelineCollection.currentPipeline)
        assertTrue(pipelineCollection.pipelines.isNotEmpty())
    }

    @Test
    fun `test get pipelines`() {
        val pipelines = pipelineCollection.pipelines
        assertNotNull(pipelines)
        assertTrue(pipelines.isNotEmpty())
    }

    @Test
    fun `test current pipeline is valid`() {
        val currentPipeline = pipelineCollection.currentPipeline
        assertNotNull(currentPipeline)
        assertTrue(pipelineCollection.pipelines.contains(currentPipeline))
    }

    @Test
    fun `test set current pipeline`() = runBlocking {
        val pipelines = pipelineCollection.pipelines
        if (pipelines.size > 1) {
            val firstPipeline = pipelines[0]
            val secondPipeline = pipelines[1]
            
            pipelineCollection.setCurrentPipeline(firstPipeline)
            assertEquals(firstPipeline, pipelineCollection.currentPipeline)
            
            pipelineCollection.setCurrentPipeline(secondPipeline)
            assertEquals(secondPipeline, pipelineCollection.currentPipeline)
        }
    }

    @Test
    fun `test pipeline processed image`() = runBlocking {
        val testImage = BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB)
        testImage.setRGB(0, 0, Color.RED.rgb)
        
        imageAlbum.addImage(testImage)
        
        val processedImage = pipelineCollection.currentPipeline.processedImage
        assertNotNull(processedImage)
        assertEquals(20, processedImage.width)
        assertEquals(20, processedImage.height)
    }

    @Test
    fun `test pipeline source is image album`() {
        assertEquals(imageAlbum, pipelineCollection.currentPipeline.source)
    }

    @Test
    fun `test pipeline collection events`() = runBlocking {
        var pipelineAddedFired = false
        var pipelineRemovedFired = false
        var pipelineChangedFired = false
        var pipelineSelectionChangedFired = false
        
        pipelineCollection.events.pipelineAdded.on { pipelineAddedFired = true }
        pipelineCollection.events.pipelineRemoved.on { pipelineRemovedFired = true }
        pipelineCollection.events.pipelineChanged.on { _, _ -> pipelineChangedFired = true }
        pipelineCollection.events.pipelineSelectionChanged.on { pipelineSelectionChangedFired = true }
        
        // Test pipeline selection change
        val pipelines = pipelineCollection.pipelines
        if (pipelines.size > 1) {
            pipelineCollection.setCurrentPipeline(pipelines[0])
            assertTrue(pipelineSelectionChangedFired)
        }
    }

    @Test
    fun `test add pipeline`() = runBlocking {
        val initialCount = pipelineCollection.pipelines.size
        val newPipeline = ImageProcessingPipeline("Test Pipeline", imageAlbum)
        
        var eventFired = false
        pipelineCollection.events.pipelineAdded.on { eventFired = true }
        
        pipelineCollection.addPipeline(newPipeline)
        
        assertEquals(initialCount + 1, pipelineCollection.pipelines.size)
        assertTrue(pipelineCollection.pipelines.contains(newPipeline))
        assertTrue(eventFired)
    }

    @Test
    fun `test remove pipeline`() = runBlocking {
        val newPipeline = ImageProcessingPipeline("Removable Pipeline", imageAlbum)
        pipelineCollection.addPipeline(newPipeline)
        
        val beforeCount = pipelineCollection.pipelines.size
        
        var eventFired = false
        pipelineCollection.events.pipelineRemoved.on { eventFired = true }
        
        pipelineCollection.removePipeline(newPipeline)
        
        assertEquals(beforeCount - 1, pipelineCollection.pipelines.size)
        assertFalse(pipelineCollection.pipelines.contains(newPipeline))
        assertTrue(eventFired)
    }

    @Test
    fun `test cannot remove all pipelines`() = runBlocking {
        val pipelines = pipelineCollection.pipelines.toList()
        
        // Try to remove all but ensure at least one remains
        for (i in 0 until pipelines.size - 1) {
            pipelineCollection.removePipeline(pipelines[i])
        }
        
        // Should still have at least one pipeline
        assertTrue(pipelineCollection.pipelines.isNotEmpty())
        assertNotNull(pipelineCollection.currentPipeline)
    }

    @Test
    fun `test pipeline collection with different image sizes`() = runBlocking {
        val smallImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        val largeImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        
        imageAlbum.addImage(smallImage)
        val processedSmall = pipelineCollection.currentPipeline.processedImage
        assertEquals(10, processedSmall.width)
        assertEquals(10, processedSmall.height)
        
        imageAlbum.addImage(largeImage)
        val processedLarge = pipelineCollection.currentPipeline.processedImage
        assertEquals(100, processedLarge.width)
        assertEquals(100, processedLarge.height)
    }

    @Test
    fun `test pipeline names are unique or handled properly`() = runBlocking {
        val pipeline1 = ImageProcessingPipeline("Test", imageAlbum)
        val pipeline2 = ImageProcessingPipeline("Test", imageAlbum)
        
        pipelineCollection.addPipeline(pipeline1)
        pipelineCollection.addPipeline(pipeline2)
        
        // Both should be in the collection (implementation may handle duplicates differently)
        assertTrue(pipelineCollection.pipelines.contains(pipeline1))
        assertTrue(pipelineCollection.pipelines.contains(pipeline2))
    }

    @Test
    fun `test get brightness data`() = runBlocking {
        val testImage = BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB)
        
        // Create a gradient
        for (x in 0 until 5) {
            for (y in 0 until 5) {
                val gray = (x + y) * 25  // 0 to 200
                val color = Color(gray, gray, gray)
                testImage.setRGB(x, y, color.rgb)
            }
        }
        
        imageAlbum.addImage(testImage)
        
        val brightnessData = pipelineCollection.currentPipeline.brightness
        assertNotNull(brightnessData)
        assertEquals(25, brightnessData.size)  // 5x5 = 25 pixels
        
        // Check some specific brightness values
        assertTrue(brightnessData.first() >= 0.0)
        assertTrue(brightnessData.last() <= 1.0)
    }

    @Test
    fun `test pipeline collection id`() {
        assertEquals("Image album", pipelineCollection.id)
    }
}