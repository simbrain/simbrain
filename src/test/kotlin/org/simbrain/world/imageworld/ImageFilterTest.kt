package org.simbrain.world.imageworld.filters

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

class ImageFilterTest {

    private lateinit var testFilter: TestableImageFilter
    
    // Concrete implementation for testing
    private class TestableImageFilter : ImageFilter() {
        var applyCalled = false
        var lastInputImage: BufferedImage? = null
        
        init {
            name = "Test Filter"
        }
        
        override fun applyFilter(input: BufferedImage): BufferedImage {
            applyCalled = true
            lastInputImage = input
            
            // Simple filter: invert colors
            val output = BufferedImage(input.width, input.height, input.type)
            for (x in 0 until input.width) {
                for (y in 0 until input.height) {
                    val rgb = input.getRGB(x, y)
                    val r = 255 - (rgb shr 16 and 0xFF)
                    val g = 255 - (rgb shr 8 and 0xFF)
                    val b = 255 - (rgb and 0xFF)
                    output.setRGB(x, y, (r shl 16) or (g shl 8) or b)
                }
            }
            return output
        }
        
        override fun copy(): ImageFilter {
            val copy = TestableImageFilter()
            copy.enabled = this.enabled
            copy.name = this.name
            return copy
        }
    }

    @BeforeEach
    fun setUp() {
        testFilter = TestableImageFilter()
    }

    @Test
    fun `test filter name`() {
        assertEquals("Test Filter", testFilter.name)
    }

    @Test
    fun `test filter enabled by default`() {
        assertTrue(testFilter.enabled)
    }

    @Test
    fun `test apply operation delegates to apply filter`() {
        val inputImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        inputImage.setRGB(0, 0, Color.RED.rgb)
        
        val result = testFilter.apply(inputImage)
        
        assertTrue(testFilter.applyCalled)
        assertSame(inputImage, testFilter.lastInputImage)
        assertNotNull(result)
        
        // Should have inverted the red color to cyan
        val expectedCyan = Color.CYAN.rgb
        assertEquals(expectedCyan, result.getRGB(0, 0))
    }

    @Test
    fun `test apply filter creates proper output`() {
        val inputImage = BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB)
        
        // Set some known colors
        inputImage.setRGB(0, 0, Color.RED.rgb)
        inputImage.setRGB(1, 0, Color.GREEN.rgb)
        inputImage.setRGB(2, 0, Color.BLUE.rgb)
        inputImage.setRGB(3, 0, Color.WHITE.rgb)
        inputImage.setRGB(4, 0, Color.BLACK.rgb)
        
        val result = testFilter.apply(inputImage)
        
        // Check inverted colors
        assertEquals(Color.CYAN.rgb, result.getRGB(0, 0))     // Red -> Cyan
        assertEquals(Color.MAGENTA.rgb, result.getRGB(1, 0))  // Green -> Magenta
        assertEquals(Color.YELLOW.rgb, result.getRGB(2, 0))   // Blue -> Yellow
        assertEquals(Color.BLACK.rgb, result.getRGB(3, 0))    // White -> Black
        assertEquals(Color.WHITE.rgb, result.getRGB(4, 0))    // Black -> White
    }

    @Test
    fun `test filter preserves image dimensions`() {
        val inputImage = BufferedImage(25, 15, BufferedImage.TYPE_INT_RGB)
        
        val result = testFilter.apply(inputImage)
        
        assertEquals(25, result.width)
        assertEquals(15, result.height)
        assertEquals(inputImage.type, result.type)
    }

    @Test
    fun `test copy creates independent filter`() {
        testFilter.enabled = false
        
        val copy = testFilter.copy()
        
        assertNotSame(testFilter, copy)
        assertFalse(copy.enabled)
        assertEquals(testFilter.name, copy.name)
        
        // Modifying copy shouldn't affect original
        copy.enabled = true
        assertFalse(testFilter.enabled)
        assertTrue(copy.enabled)
    }

    @Test
    fun `test filter with different image types`() {
        val rgbImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        val argbImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        
        rgbImage.setRGB(0, 0, Color.RED.rgb)
        argbImage.setRGB(0, 0, Color.RED.rgb)
        
        val rgbResult = testFilter.apply(rgbImage)
        val argbResult = testFilter.apply(argbImage)
        
        assertEquals(BufferedImage.TYPE_INT_RGB, rgbResult.type)
        assertEquals(BufferedImage.TYPE_INT_ARGB, argbResult.type)
        
        // Both should invert red to cyan
        assertEquals(Color.CYAN.rgb, rgbResult.getRGB(0, 0))
        assertEquals(Color.CYAN.rgb, argbResult.getRGB(0, 0))
    }

    @Test
    fun `test filter with large image`() {
        val largeImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        
        // Fill with a pattern
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                val color = if ((x + y) % 2 == 0) Color.WHITE.rgb else Color.BLACK.rgb
                largeImage.setRGB(x, y, color)
            }
        }
        
        val result = testFilter.apply(largeImage)
        
        assertEquals(100, result.width)
        assertEquals(100, result.height)
        
        // Check that pattern is inverted
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                val expectedColor = if ((x + y) % 2 == 0) Color.BLACK.rgb else Color.WHITE.rgb
                assertEquals(expectedColor, result.getRGB(x, y))
            }
        }
    }

    @Test
    fun `test filter does not modify input image`() {
        val inputImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        inputImage.setRGB(0, 0, Color.BLUE.rgb)
        
        val originalRgb = inputImage.getRGB(0, 0)
        
        testFilter.apply(inputImage)
        
        // Input should be unchanged
        assertEquals(originalRgb, inputImage.getRGB(0, 0))
        assertEquals(Color.BLUE.rgb, inputImage.getRGB(0, 0))
    }
    
    @Test
    fun `test filter disabled returns original image`() {
        val inputImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        inputImage.setRGB(0, 0, Color.RED.rgb)
        
        testFilter.enabled = false
        val result = testFilter.apply(inputImage)
        
        // Should return original image unchanged when disabled
        assertSame(inputImage, result)
        assertEquals(Color.RED.rgb, result.getRGB(0, 0))
        assertFalse(testFilter.applyCalled)  // applyFilter should not be called when disabled
    }
}