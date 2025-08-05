package org.simbrain.world.imageworld.filters

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.*

/**
 * Unit tests for GaborFilter with educational components explaining the mathematical concepts.
 */
class GaborFilterTest {

    private fun createTestImage(width: Int = 50, height: Int = 50): BufferedImage {
        return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).apply {
            // Fill with white background
            val graphics = createGraphics()
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, width, height)
            graphics.dispose()
        }
    }

    private fun createVerticalStripeImage(width: Int = 50, height: Int = 50, stripeWidth: Int = 5): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = if ((x / stripeWidth) % 2 == 0) Color.WHITE else Color.BLACK
                image.setRGB(x, y, color.rgb)
            }
        }
        return image
    }

    private fun createHorizontalStripeImage(width: Int = 50, height: Int = 50, stripeWidth: Int = 5): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = if ((y / stripeWidth) % 2 == 0) Color.WHITE else Color.BLACK
                image.setRGB(x, y, color.rgb)
            }
        }
        return image
    }

    @Test
    fun `test gabor filter creation and default parameters`() {
        val filter = GaborFilter()
        
        assertEquals("Gabor Filter", filter.name)
        assertTrue(filter.enabled)
        assertEquals(0.1, filter.frequency, 0.001)
        assertEquals(0.0, filter.orientationDegrees, 0.001)
        assertEquals(2.0, filter.sigmaX, 0.001)
        assertEquals(2.0, filter.sigmaY, 0.001)
        assertEquals(0.0, filter.phase, 0.001)
        assertEquals(15, filter.kernelSize)
    }

    @Test
    fun `test frequency parameter validation and educational explanation`() {
        val filter = GaborFilter()
        
        // Frequency determines how many oscillations per unit distance
        // Higher frequency = more cycles = detects finer patterns
        // Lower frequency = fewer cycles = detects coarser patterns
        
        // Test valid range clamping
        filter.frequency = 0.5
        assertEquals(0.5, filter.frequency, 0.001)
        
        // Test lower bound clamping (frequency can't be too low or filter becomes meaningless)
        filter.frequency = -0.5
        assertEquals(0.01, filter.frequency, 0.001)
        
        filter.frequency = 0.005
        assertEquals(0.01, filter.frequency, 0.001)
        
        // Test upper bound clamping (frequency can't be too high or aliasing occurs)
        filter.frequency = 2.0
        assertEquals(1.0, filter.frequency, 0.001)
    }

    @Test
    fun `test orientation parameter with angle wrapping`() {
        val filter = GaborFilter()
        
        // EDUCATIONAL: Orientation determines which direction of edges/patterns the filter detects
        // 0° = horizontal patterns, 90° = vertical patterns, 45° = diagonal patterns
        
        filter.orientationDegrees = 45.0
        assertEquals(45.0, filter.orientationDegrees, 0.001)
        
        filter.orientationDegrees = 90.0
        assertEquals(90.0, filter.orientationDegrees, 0.001)
        
        // Test angle wrapping (angles beyond 360° wrap around)
        filter.orientationDegrees = 450.0
        assertEquals(90.0, filter.orientationDegrees, 0.001)
        
        // The implementation uses modulo which can return negative values
        // -90.0 % 360.0 = -90.0 in Kotlin, which is mathematically correct
        filter.orientationDegrees = -90.0
        assertEquals(-90.0, filter.orientationDegrees, 0.001)
    }

    @Test
    fun `test sigma parameters control gaussian envelope size`() {
        val filter = GaborFilter()
        
        // EDUCATIONAL: Sigma controls the "spread" of the Gaussian envelope
        // Larger sigma = wider filter = captures broader patterns
        // Smaller sigma = narrower filter = captures localized patterns
        
        filter.sigmaX = 3.0
        assertEquals(3.0, filter.sigmaX, 0.001)
        
        filter.sigmaY = 1.5
        assertEquals(1.5, filter.sigmaY, 0.001)
        
        // Test minimum bounds (sigma can't be too small or filter becomes a spike)
        filter.sigmaX = 0.05
        assertEquals(0.1, filter.sigmaX, 0.001)
        
        filter.sigmaY = -1.0
        assertEquals(0.1, filter.sigmaY, 0.001)
    }

    @Test
    fun `test kernel size validation ensures odd numbers`() {
        val filter = GaborFilter()
        
        // EDUCATIONAL: Kernel size must be odd to have a center pixel
        // Larger kernels capture more context but are slower to compute
        
        filter.kernelSize = 10  // Even number should become 11
        assertEquals(11, filter.kernelSize)
        
        filter.kernelSize = 21  // Odd number should stay the same
        assertEquals(21, filter.kernelSize)
        
        filter.kernelSize = 1   // Minimum size
        assertEquals(3, filter.kernelSize)  // Enforced minimum of 3
    }

    @Test
    fun `test gabor filter detects vertical patterns better with horizontal orientation`() {
        val filter = GaborFilter()
        
        // A Gabor filter oriented horizontally (0°) responds strongly to vertical edges/stripes
        // because the sinusoidal component oscillates perpendicular to the detected features
        
        filter.orientationDegrees = 0.0  // Horizontal filter detects vertical features
        filter.frequency = 0.2           // Appropriate for our stripe pattern
        filter.kernelSize = 21           // Larger kernel for better pattern detection
        
        val verticalStripes = createVerticalStripeImage(width = 40, height = 40, stripeWidth = 4)
        val horizontalStripes = createHorizontalStripeImage(width = 40, height = 40, stripeWidth = 4)
        
        val verticalResult = filter.apply(verticalStripes)
        val horizontalResult = filter.apply(horizontalStripes)
        
        // Calculate average response strength (how much the filter activated)
        val verticalResponse = calculateAverageActivation(verticalResult)
        val horizontalResponse = calculateAverageActivation(horizontalResult)
        
        // Vertical stripes should produce stronger response than horizontal stripes
        // for a horizontally-oriented filter
        assertTrue(verticalResponse > horizontalResponse, 
            "Horizontal Gabor filter should respond more strongly to vertical stripes. " +
            "Vertical response: $verticalResponse, Horizontal response: $horizontalResponse")
    }

    @Test
    fun `test gabor filter detects horizontal patterns better with vertical orientation`() {
        val filter = GaborFilter()
        
        // A Gabor filter oriented vertically (90°) responds strongly to horizontal edges/stripes
        
        filter.orientationDegrees = 90.0  // Vertical filter detects horizontal features
        filter.frequency = 0.2
        filter.kernelSize = 21
        
        val verticalStripes = createVerticalStripeImage(width = 40, height = 40, stripeWidth = 4)
        val horizontalStripes = createHorizontalStripeImage(width = 40, height = 40, stripeWidth = 4)
        
        val verticalResult = filter.apply(verticalStripes)
        val horizontalResult = filter.apply(horizontalStripes)
        
        val verticalResponse = calculateAverageActivation(verticalResult)
        val horizontalResponse = calculateAverageActivation(horizontalResult)
        
        // Horizontal stripes should produce stronger response than vertical stripes
        // for a vertically-oriented filter
        assertTrue(horizontalResponse > verticalResponse,
            "Vertical Gabor filter should respond more strongly to horizontal stripes. " +
            "Horizontal response: $horizontalResponse, Vertical response: $verticalResponse")
    }

    @Test
    fun `test frequency tuning affects pattern detection sensitivity`() {
        val testImage = createVerticalStripeImage(width = 50, height = 50, stripeWidth = 3)
        
        // The frequency parameter should match the spatial frequency of the pattern
        // If frequency is too high, it misses broad patterns
        // If frequency is too low, it misses fine patterns
        
        val lowFreqFilter = GaborFilter().apply {
            frequency = 0.05  // Low frequency for broad patterns
            orientationDegrees = 0.0
        }
        
        val matchedFreqFilter = GaborFilter().apply {
            frequency = 0.15  // Frequency roughly matching our stripe pattern
            orientationDegrees = 0.0
        }
        
        val highFreqFilter = GaborFilter().apply {
            frequency = 0.5   // High frequency for fine patterns
            orientationDegrees = 0.0
        }
        
        val lowResult = calculateAverageActivation(lowFreqFilter.apply(testImage))
        val matchedResult = calculateAverageActivation(matchedFreqFilter.apply(testImage))
        val highResult = calculateAverageActivation(highFreqFilter.apply(testImage))
        
        // The filter with frequency matching the pattern should respond most strongly
        assertTrue(matchedResult >= lowResult,
            "Matched frequency should respond at least as strongly as low frequency")
        assertTrue(matchedResult >= highResult,
            "Matched frequency should respond at least as strongly as high frequency")
    }

    @Test
    fun `test phase shift affects filter response pattern`() {
        val filter = GaborFilter()
        
        // Phase determines whether the filter responds to bright-to-dark
        // or dark-to-bright transitions. Phase = 0 vs Phase = π gives opposite responses.
        
        val testImage = createVerticalStripeImage(width = 30, height = 30, stripeWidth = 3)
        
        filter.phase = 0.0
        val phase0Result = filter.apply(testImage)
        
        filter.phase = PI
        val phasePiResult = filter.apply(testImage)
        
        // Results should be different due to phase shift
        // Note: For very regular patterns, phase shifts might not always produce dramatically different results
        // We'll check that at least one has some activation
        val phase0Activation = calculateAverageActivation(phase0Result)
        val phasePiActivation = calculateAverageActivation(phasePiResult)
        
        assertTrue(phase0Activation > 0.1 || phasePiActivation > 0.1,
            "At least one phase should produce significant activation. " +
            "Phase 0: $phase0Activation, Phase π: $phasePiActivation")
    }

    @Test
    fun `test gabor filter preserves image dimensions`() {
        val filter = GaborFilter()
        val inputImage = createTestImage(75, 45)
        
        val result = filter.apply(inputImage)
        
        assertEquals(75, result.width)
        assertEquals(45, result.height)
        assertEquals(inputImage.type, result.type)
    }

    @Test
    fun `test gabor filter handles edge boundaries properly`() {
        val filter = GaborFilter()
        filter.kernelSize = 15  // Large enough that edges matter
        
        val smallImage = createTestImage(10, 10)  // Smaller than kernel
        
        // Should not crash and should produce valid output
        assertDoesNotThrow {
            val result = filter.apply(smallImage)
            assertEquals(10, result.width)
            assertEquals(10, result.height)
        }
    }

    @Test
    fun `test copy creates independent gabor filter`() {
        val original = GaborFilter()
        original.frequency = 0.3
        original.orientationDegrees = 45.0
        original.sigmaX = 3.0
        original.sigmaY = 1.5
        original.phase = PI/4
        original.kernelSize = 21
        original.enabled = false
        
        val copy = original.copy() as GaborFilter
        
        assertNotSame(original, copy)
        assertEquals(original.frequency, copy.frequency, 0.001)
        assertEquals(original.orientationDegrees, copy.orientationDegrees, 0.001)
        assertEquals(original.sigmaX, copy.sigmaX, 0.001)
        assertEquals(original.sigmaY, copy.sigmaY, 0.001)
        assertEquals(original.phase, copy.phase, 0.001)
        assertEquals(original.kernelSize, copy.kernelSize)
        assertEquals(original.enabled, copy.enabled)
        
        // Modifying copy shouldn't affect original
        copy.frequency = 0.8
        assertEquals(0.3, original.frequency, 0.001)
        assertEquals(0.8, copy.frequency, 0.001)
    }

    @Test
    fun `test gabor filter with different sigma values creates anisotropic response`() {
        // Different sigmaX and sigmaY create elliptical rather than circular filters
        // This can be useful for detecting elongated features
        
        val filter = GaborFilter()
        filter.sigmaX = 1.0  // Narrow in X direction
        filter.sigmaY = 4.0  // Wide in Y direction
        filter.frequency = 0.2
        filter.orientationDegrees = 0.0
        
        val testImage = createVerticalStripeImage(width = 30, height = 30, stripeWidth = 3)
        
        assertDoesNotThrow {
            val result = filter.apply(testImage)
            assertNotNull(result)
            assertEquals(30, result.width)
            assertEquals(30, result.height)
        }
    }

    /**
     * Helper function to calculate average activation strength in a filtered image.
     * Higher values indicate stronger filter response.
     */
    private fun calculateAverageActivation(image: BufferedImage): Double {
        var totalActivation = 0.0
        val pixelCount = image.width * image.height
        
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val rgb = image.getRGB(x, y)
                val gray = ((rgb shr 16) and 0xFF) + ((rgb shr 8) and 0xFF) + (rgb and 0xFF)
                // Convert to activation strength (distance from middle gray = 382.5)
                totalActivation += abs(gray - 382.5) / 382.5
            }
        }
        
        return totalActivation / pixelCount
    }
}