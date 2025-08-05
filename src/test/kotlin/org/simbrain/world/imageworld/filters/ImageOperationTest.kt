package org.simbrain.world.imageworld.filters

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * Unit tests for the ImageOperation base class with educational components about the operation pipeline.
 */
class ImageOperationTest {

    // Test implementation of ImageOperation for testing base functionality
    private class TestImageOperation(private val shouldInvert: Boolean = true) : ImageOperation() {
        var applyOperationCalled = false
        var lastInput: BufferedImage? = null
        
        init {
            name = "Test Operation"
        }
        
        override fun applyOperation(input: BufferedImage): BufferedImage {
            applyOperationCalled = true
            lastInput = input
            
            if (!shouldInvert) {
                // Return copy of input unchanged
                val output = BufferedImage(input.width, input.height, input.type)
                val graphics = output.createGraphics()
                graphics.drawImage(input, 0, 0, null)
                graphics.dispose()
                return output
            }
            
            // Simple operation: invert colors
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
        
        override fun copy(): ImageOperation {
            val copy = TestImageOperation(shouldInvert)
            copy.enabled = this.enabled
            copy.name = this.name
            return copy
        }
    }

    private fun createTestImage(width: Int = 20, height: Int = 20, color: Color = Color.BLUE): BufferedImage {
        return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).apply {
            val graphics = createGraphics()
            graphics.color = color
            graphics.fillRect(0, 0, width, height)
            graphics.dispose()
        }
    }

    @Test
    fun `test image operation default properties`() {
        val operation = TestImageOperation()
        
        assertTrue(operation.enabled, "Operations should be enabled by default")
        assertEquals("Test Operation", operation.name)
        assertEquals("Test Operation", operation.toString())
    }

    @Test
    fun `test image operation enabled flag controls execution`() {
        val operation = TestImageOperation()
        val inputImage = createTestImage(color = Color.RED)
        
        // Test enabled operation
        operation.enabled = true
        val enabledResult = operation.apply(inputImage)
        
        assertTrue(operation.applyOperationCalled, "applyOperation should be called when enabled")
        assertSame(inputImage, operation.lastInput, "Input should be passed to applyOperation")
        assertNotSame(inputImage, enabledResult, "Should return new image when enabled")
        
        // Reset state
        operation.applyOperationCalled = false
        operation.lastInput = null
        
        // Test disabled operation
        operation.enabled = false
        val disabledResult = operation.apply(inputImage)
        
        assertFalse(operation.applyOperationCalled, "applyOperation should NOT be called when disabled")
        assertNull(operation.lastInput, "Input should not be passed when disabled")
        assertSame(inputImage, disabledResult, "Should return original image when disabled")
    }

    @Test
    fun `test image operation preserves input when disabled`() {
        val operation = TestImageOperation()
        val inputImage = createTestImage(color = Color.GREEN)
        val originalRgb = inputImage.getRGB(0, 0)
        
        operation.enabled = false
        val result = operation.apply(inputImage)
        
        // When disabled, operations should return the exact same image object
        // This is important for performance - no copying occurs
        assertSame(inputImage, result)
        assertEquals(originalRgb, result.getRGB(0, 0))
        assertEquals(Color.GREEN.rgb, result.getRGB(0, 0))
    }

    @Test
    fun `test image operation creates new image when enabled`() {
        val operation = TestImageOperation()
        val inputImage = createTestImage(color = Color.BLUE)
        
        operation.enabled = true
        val result = operation.apply(inputImage)
        
        // Operations should create new images, not modify the input
        // This allows multiple operations to use the same input independently
        assertNotSame(inputImage, result)
        assertEquals(inputImage.width, result.width)
        assertEquals(inputImage.height, result.height)
        assertEquals(inputImage.type, result.type)
        
        // Should have inverted blue to yellow
        assertEquals(Color.BLUE.rgb, inputImage.getRGB(0, 0)) // Input unchanged
        assertEquals(Color.YELLOW.rgb, result.getRGB(0, 0))   // Output inverted
    }

    @Test
    fun `test image operation copy functionality`() {
        val original = TestImageOperation()
        original.enabled = false
        original.name = "Custom Name"
        
        val copy = original.copy()
        
        assertNotSame(original, copy)
        assertEquals(original.enabled, copy.enabled)
        assertEquals(original.name, copy.name)
        
        // Modifying copy should not affect original
        copy.enabled = true
        copy.name = "Different Name"
        
        assertFalse(original.enabled)
        assertEquals("Custom Name", original.name)
        assertTrue(copy.enabled)
        assertEquals("Different Name", copy.name)
    }

    @Test
    fun `test image operation with different image types`() {
        val operation = TestImageOperation()
        
        val rgbImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        val argbImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        val grayImage = BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY)
        
        rgbImage.setRGB(0, 0, Color.RED.rgb)
        argbImage.setRGB(0, 0, Color.RED.rgb)
        grayImage.setRGB(0, 0, Color.RED.rgb)
        
        val rgbResult = operation.apply(rgbImage)
        val argbResult = operation.apply(argbImage)
        val grayResult = operation.apply(grayImage)
        
        // Operations should preserve image type
        assertEquals(BufferedImage.TYPE_INT_RGB, rgbResult.type)
        assertEquals(BufferedImage.TYPE_INT_ARGB, argbResult.type)
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, grayResult.type)
    }

    @Test
    fun `test image operation pipeline simulation`() {
        // EDUCATIONAL: Simulate how multiple operations work in a pipeline
        val operation1 = TestImageOperation(shouldInvert = true)  // Invert colors
        val operation2 = TestImageOperation(shouldInvert = false) // Pass through
        val operation3 = TestImageOperation(shouldInvert = true)  // Invert again
        
        val inputImage = createTestImage(color = Color.BLUE)
        
        // Apply operations in sequence (like a pipeline would)
        val stage1 = operation1.apply(inputImage)  // Blue -> Yellow
        val stage2 = operation2.apply(stage1)      // Yellow -> Yellow (pass through)
        val stage3 = operation3.apply(stage2)      // Yellow -> Blue (invert back)
        
        // Should end up back to original color after two inversions
        assertEquals(Color.BLUE.rgb, inputImage.getRGB(0, 0))
        assertEquals(Color.YELLOW.rgb, stage1.getRGB(0, 0))
        assertEquals(Color.YELLOW.rgb, stage2.getRGB(0, 0))
        assertEquals(Color.BLUE.rgb, stage3.getRGB(0, 0))
    }

    @Test
    fun `test image operation with disabled operation in pipeline`() {
        // Show how disabled operations are skipped in pipelines
        val operation1 = TestImageOperation()
        val operation2 = TestImageOperation()
        val operation3 = TestImageOperation()
        
        operation2.enabled = false  // Disable middle operation
        
        val inputImage = createTestImage(color = Color.RED)
        
        val stage1 = operation1.apply(inputImage)  // Red -> Cyan
        val stage2 = operation2.apply(stage1)      // Cyan -> Cyan (disabled, no change)
        val stage3 = operation3.apply(stage2)      // Cyan -> Red
        
        assertEquals(Color.RED.rgb, inputImage.getRGB(0, 0))
        assertEquals(Color.CYAN.rgb, stage1.getRGB(0, 0))
        assertSame(stage1, stage2) // stage2 should be same object as stage1 (no processing)
        assertEquals(Color.RED.rgb, stage3.getRGB(0, 0))
    }

    @Test
    fun `test image operation name property`() {
        val operation = TestImageOperation()
        
        assertEquals("Test Operation", operation.name)
        assertEquals("Test Operation", operation.toString())
        
        operation.name = "Custom Filter Name"
        assertEquals("Custom Filter Name", operation.name)
        assertEquals("Custom Filter Name", operation.toString())
    }

    @Test
    fun `test image operation with large image`() {
        val operation = TestImageOperation()
        val largeImage = createTestImage(width = 500, height = 300, color = Color.MAGENTA)
        
        val result = operation.apply(largeImage)
        
        assertEquals(500, result.width)
        assertEquals(300, result.height)
        assertEquals(largeImage.type, result.type)
        assertNotSame(largeImage, result)
        
        // Should invert magenta to green
        assertEquals(Color.GREEN.rgb, result.getRGB(0, 0))
        assertEquals(Color.GREEN.rgb, result.getRGB(499, 299))
    }

    @Test
    fun `test image operation preserves input image integrity`() {
        val operation = TestImageOperation()
        val inputImage = createTestImage(color = Color.CYAN)
        
        // Store original values
        val originalRgb00 = inputImage.getRGB(0, 0)
        val originalWidth = inputImage.width
        val originalHeight = inputImage.height
        
        val result = operation.apply(inputImage)
        
        // Input should be completely unchanged
        assertEquals(originalRgb00, inputImage.getRGB(0, 0))
        assertEquals(originalWidth, inputImage.width)
        assertEquals(originalHeight, inputImage.height)
        assertEquals(Color.CYAN.rgb, inputImage.getRGB(0, 0))
        
        // But output should be different
        assertEquals(Color.RED.rgb, result.getRGB(0, 0)) // Cyan inverted to Red
    }
}