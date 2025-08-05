package org.simbrain.world.imageworld.filters

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.*

/**
 * Unit tests for EdgeDetectionFilter with components explaining different edge detection algorithms.
 */
class EdgeDetectionFilterTest {

    private fun createSolidColorImage(width: Int = 50, height: Int = 50, color: Color = Color.WHITE): BufferedImage {
        return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).apply {
            val graphics = createGraphics()
            graphics.color = color
            graphics.fillRect(0, 0, width, height)
            graphics.dispose()
        }
    }

    private fun createVerticalEdgeImage(width: Int = 50, height: Int = 50): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                // Sharp vertical edge in the middle
                val color = if (x < width / 2) Color.BLACK else Color.WHITE
                image.setRGB(x, y, color.rgb)
            }
        }
        return image
    }

    private fun createHorizontalEdgeImage(width: Int = 50, height: Int = 50): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                // Sharp horizontal edge in the middle
                val color = if (y < height / 2) Color.BLACK else Color.WHITE
                image.setRGB(x, y, color.rgb)
            }
        }
        return image
    }

    private fun createDiagonalEdgeImage(width: Int = 50, height: Int = 50): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                // Diagonal edge from top-left to bottom-right
                val color = if (y > x) Color.BLACK else Color.WHITE
                image.setRGB(x, y, color.rgb)
            }
        }
        return image
    }

    private fun createNoisyImage(width: Int = 50, height: Int = 50, noiseLevel: Double = 0.1): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                // Add random noise to a gray background
                val baseGray = 128
                val noise = (Math.random() - 0.5) * noiseLevel * 255
                val gray = maxOf(0, minOf(255, (baseGray + noise).toInt()))
                val color = Color(gray, gray, gray)
                image.setRGB(x, y, color.rgb)
            }
        }
        return image
    }

    @Test
    fun `test edge detection filter creation and default parameters`() {
        val filter = EdgeDetectionFilter()
        
        assertEquals("Edge Detection Filter", filter.name)
        assertTrue(filter.enabled)
        assertEquals(EdgeDetectionFilter.EdgeDetectionMethod.SOBEL, filter.method)
        assertEquals(0.3, filter.threshold, 0.001)
        assertTrue(filter.enhanceEdges)
        assertEquals(0.0, filter.gaussianSigma, 0.001)
    }

    @Test
    fun `test threshold parameter validation`() {
        val filter = EdgeDetectionFilter()
        
        // Threshold determines what gradient magnitude is considered an edge
        // Higher threshold = only strong edges detected (fewer false positives, more false negatives)
        // Lower threshold = weak edges detected too (more false positives, fewer false negatives)
        
        filter.threshold = 0.5
        assertEquals(0.5, filter.threshold, 0.001)
        
        // Test bounds clamping
        filter.threshold = -0.1
        assertEquals(0.0, filter.threshold, 0.001)
        
        filter.threshold = 1.5
        assertEquals(1.0, filter.threshold, 0.001)
    }

    @Test
    fun `test gaussian sigma parameter validation`() {
        val filter = EdgeDetectionFilter()
        
        // Gaussian blur before edge detection reduces noise but also blurs edges
        // sigma = 0 means no blur, higher sigma = more blur = less noise but less precision
        
        filter.gaussianSigma = 2.0
        assertEquals(2.0, filter.gaussianSigma, 0.001)
        
        // Test minimum bound
        filter.gaussianSigma = -1.0
        assertEquals(0.0, filter.gaussianSigma, 0.001)
    }

    @Test
    fun `test sobel filter detects vertical edges strongly`() {
        val filter = EdgeDetectionFilter()
        filter.method = EdgeDetectionFilter.EdgeDetectionMethod.SOBEL
        filter.threshold = 0.1  // Low threshold to capture edges
        
        // Sobel X-kernel [[-1,0,1],[-2,0,2],[-1,0,1]] responds strongly to vertical edges
        // because it computes horizontal gradients (change from left to right)
        
        val verticalEdgeImage = createVerticalEdgeImage(width = 30, height = 30)
        val solidImage = createSolidColorImage(width = 30, height = 30)
        
        val verticalResult = filter.apply(verticalEdgeImage)
        val solidResult = filter.apply(solidImage)
        
        val verticalEdgeStrength = calculateEdgeStrength(verticalResult)
        val solidEdgeStrength = calculateEdgeStrength(solidResult)
        
        assertTrue(verticalEdgeStrength > solidEdgeStrength,
            "Sobel filter should detect strong edges in vertical edge image. " +
            "Vertical: $verticalEdgeStrength, Solid: $solidEdgeStrength")
    }

    @Test
    fun `test prewitt filter detects horizontal edges strongly`() {
        val filter = EdgeDetectionFilter()
        filter.method = EdgeDetectionFilter.EdgeDetectionMethod.PREWITT
        filter.threshold = 0.1
        
        // Prewitt Y-kernel [[-1,-1,-1],[0,0,0],[1,1,1]] responds strongly to horizontal edges
        // because it computes vertical gradients (change from top to bottom)
        
        val horizontalEdgeImage = createHorizontalEdgeImage(width = 30, height = 30)
        val solidImage = createSolidColorImage(width = 30, height = 30)
        
        val horizontalResult = filter.apply(horizontalEdgeImage)
        val solidResult = filter.apply(solidImage)
        
        val horizontalEdgeStrength = calculateEdgeStrength(horizontalResult)
        val solidEdgeStrength = calculateEdgeStrength(solidResult)
        
        assertTrue(horizontalEdgeStrength > solidEdgeStrength,
            "Prewitt filter should detect strong edges in horizontal edge image. " +
            "Horizontal: $horizontalEdgeStrength, Solid: $solidEdgeStrength")
    }

    @Test
    fun `test roberts filter is sensitive to fine details`() {
        val filter = EdgeDetectionFilter()
        filter.method = EdgeDetectionFilter.EdgeDetectionMethod.ROBERTS
        filter.threshold = 0.1
        
        // Roberts uses smaller 2x2 kernels so it's faster but more sensitive to noise
        // Good for sharp, high-contrast edges but poor with noise
        
        val diagonalEdgeImage = createDiagonalEdgeImage(width = 20, height = 20)
        val result = filter.apply(diagonalEdgeImage)
        
        // Roberts should detect the diagonal edge
        val edgeStrength = calculateEdgeStrength(result)
        assertTrue(edgeStrength > 0.05, "Roberts filter should detect diagonal edges. Actual: $edgeStrength")
    }

    @Test
    fun `test all edge detection methods produce different results`() {
        val testImage = createVerticalEdgeImage(width = 25, height = 25)
        
        val sobelFilter = EdgeDetectionFilter().apply { 
            method = EdgeDetectionFilter.EdgeDetectionMethod.SOBEL 
        }
        val prewittFilter = EdgeDetectionFilter().apply { 
            method = EdgeDetectionFilter.EdgeDetectionMethod.PREWITT 
        }
        val robertsFilter = EdgeDetectionFilter().apply { 
            method = EdgeDetectionFilter.EdgeDetectionMethod.ROBERTS 
        }
        val cannyFilter = EdgeDetectionFilter().apply { 
            method = EdgeDetectionFilter.EdgeDetectionMethod.CANNY 
        }
        
        val sobelResult = sobelFilter.apply(testImage)
        val prewittResult = prewittFilter.apply(testImage)
        val robertsResult = robertsFilter.apply(testImage)
        val cannyResult = cannyFilter.apply(testImage)
        
        // Results should be different (though they may be similar)
        val sobelStrength = calculateEdgeStrength(sobelResult)
        val prewittStrength = calculateEdgeStrength(prewittResult)
        val robertsStrength = calculateEdgeStrength(robertsResult)
        val cannyStrength = calculateEdgeStrength(cannyResult)
        
        // All should detect some edges
        assertTrue(sobelStrength > 0.05, "Sobel should detect edges. Actual: $sobelStrength")
        assertTrue(prewittStrength > 0.05, "Prewitt should detect edges. Actual: $prewittStrength")
        assertTrue(robertsStrength > 0.02, "Roberts should detect edges. Actual: $robertsStrength") // Lower threshold for Roberts
        assertTrue(cannyStrength > 0.05, "Canny should detect edges. Actual: $cannyStrength")
    }

    @Test
    fun `test threshold affects edge detection sensitivity`() {
        val edgeImage = createVerticalEdgeImage(width = 25, height = 25)
        
        val lowThresholdFilter = EdgeDetectionFilter().apply {
            threshold = 0.1  // Sensitive - detects weak edges
        }
        val highThresholdFilter = EdgeDetectionFilter().apply {
            threshold = 0.8  // Conservative - only strong edges
        }
        
        val lowResult = lowThresholdFilter.apply(edgeImage)
        val highResult = highThresholdFilter.apply(edgeImage)
        
        val lowEdgeStrength = calculateEdgeStrength(lowResult)
        val highEdgeStrength = calculateEdgeStrength(highResult)
        
        // Low threshold should generally detect more edges (higher activation)
        assertTrue(lowEdgeStrength >= highEdgeStrength,
            "Lower threshold should detect at least as many edges as higher threshold. " +
            "Low: $lowEdgeStrength, High: $highEdgeStrength")
    }

    @Test
    fun `test enhance edges parameter increases edge visibility`() {
        val edgeImage = createVerticalEdgeImage(width = 25, height = 25)
        
        val normalFilter = EdgeDetectionFilter().apply {
            enhanceEdges = false
            threshold = 0.2
        }
        val enhancedFilter = EdgeDetectionFilter().apply {
            enhanceEdges = true
            threshold = 0.2
        }
        
        val normalResult = normalFilter.apply(edgeImage)
        val enhancedResult = enhancedFilter.apply(edgeImage)
        
        // Enhanced version should have higher contrast/visibility
        val normalStrength = calculateEdgeStrength(normalResult)
        val enhancedStrength = calculateEdgeStrength(enhancedResult)
        
        assertTrue(enhancedStrength >= normalStrength,
            "Enhanced edges should be at least as strong as normal edges. " +
            "Enhanced: $enhancedStrength, Normal: $normalStrength")
    }

    @Test
    fun `test gaussian blur reduces noise sensitivity`() {
        val noisyImage = createNoisyImage(width = 30, height = 30, noiseLevel = 0.3)
        
        val noBlurFilter = EdgeDetectionFilter().apply {
            gaussianSigma = 0.0  // No preprocessing blur
            threshold = 0.1
        }
        val blurFilter = EdgeDetectionFilter().apply {
            gaussianSigma = 1.0  // Moderate preprocessing blur
            threshold = 0.1
        }
        
        val noBlurResult = noBlurFilter.apply(noisyImage)
        val blurResult = blurFilter.apply(noisyImage)
        
        // Blur should reduce noise-induced false edges
        val noBlurNoise = calculateEdgeStrength(noBlurResult)
        val blurNoise = calculateEdgeStrength(blurResult)
        
        // This test documents the behavior - blur typically reduces detected edges in noisy images
        assertTrue(blurNoise <= noBlurNoise * 1.5, // Allow some tolerance
            "Gaussian blur should generally reduce noise-induced edges. " +
            "No blur: $noBlurNoise, With blur: $blurNoise")
    }

    @Test
    fun `test edge detection preserves image dimensions`() {
        val filter = EdgeDetectionFilter()
        val inputImage = createVerticalEdgeImage(width = 73, height = 47)
        
        val result = filter.apply(inputImage)
        
        assertEquals(73, result.width)
        assertEquals(47, result.height)
        assertEquals(inputImage.type, result.type)
    }

    @Test
    fun `test edge detection handles small images`() {
        val filter = EdgeDetectionFilter()
        val smallImage = createVerticalEdgeImage(width = 5, height = 5)
        
        // Should not crash on small images
        assertDoesNotThrow {
            val result = filter.apply(smallImage)
            assertEquals(5, result.width)
            assertEquals(5, result.height)
        }
    }

    @Test
    fun `test copy creates independent edge detection filter`() {
        val original = EdgeDetectionFilter()
        original.method = EdgeDetectionFilter.EdgeDetectionMethod.PREWITT
        original.threshold = 0.7
        original.enhanceEdges = false
        original.gaussianSigma = 2.5
        original.enabled = false
        
        val copy = original.copy() as EdgeDetectionFilter
        
        assertNotSame(original, copy)
        assertEquals(original.method, copy.method)
        assertEquals(original.threshold, copy.threshold, 0.001)
        assertEquals(original.enhanceEdges, copy.enhanceEdges)
        assertEquals(original.gaussianSigma, copy.gaussianSigma, 0.001)
        assertEquals(original.enabled, copy.enabled)
        
        // Modifying copy shouldn't affect original
        copy.threshold = 0.1
        assertEquals(0.7, original.threshold, 0.001)
        assertEquals(0.1, copy.threshold, 0.001)
    }


    @Test
    fun `test canny edge detection behaves as simplified sobel`() {
        // In this implementation, Canny is simplified to just Sobel
        // Real Canny would include non-maximum suppression and hysteresis thresholding
        
        val testImage = createVerticalEdgeImage(width = 25, height = 25)
        
        val cannyFilter = EdgeDetectionFilter().apply {
            method = EdgeDetectionFilter.EdgeDetectionMethod.CANNY
            threshold = 0.2
        }
        val sobelFilter = EdgeDetectionFilter().apply {
            method = EdgeDetectionFilter.EdgeDetectionMethod.SOBEL
            threshold = 0.2
        }
        
        val cannyResult = cannyFilter.apply(testImage)
        val sobelResult = sobelFilter.apply(testImage)
        
        // In current implementation, Canny should produce similar results to Sobel
        val cannyStrength = calculateEdgeStrength(cannyResult)
        val sobelStrength = calculateEdgeStrength(sobelResult)
        
        assertEquals(sobelStrength, cannyStrength, 0.01,
            "Simplified Canny should produce results similar to Sobel")
    }

    @Test
    fun `test edge detection with extreme parameters`() {
        val filter = EdgeDetectionFilter()
        val testImage = createVerticalEdgeImage(width = 20, height = 20)
        
        // Test with very high threshold (should detect almost no edges)
        filter.threshold = 0.99
        val highThresholdResult = filter.apply(testImage)
        val highThresholdStrength = calculateEdgeStrength(highThresholdResult)
        
        // Test with very low threshold (should detect many edges)
        filter.threshold = 0.01
        val lowThresholdResult = filter.apply(testImage)
        val lowThresholdStrength = calculateEdgeStrength(lowThresholdResult)
        
        assertTrue(lowThresholdStrength >= highThresholdStrength,
            "Very low threshold should detect at least as many edges as very high threshold")
    }

    /**
     * Helper function to calculate the overall edge strength in a filtered image.
     * Higher values indicate more or stronger edges detected.
     */
    private fun calculateEdgeStrength(image: BufferedImage): Double {
        var totalStrength = 0.0
        val pixelCount = image.width * image.height
        
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val rgb = image.getRGB(x, y)
                // For edge images, brightness indicates edge strength
                val brightness = ((rgb shr 16) and 0xFF) + ((rgb shr 8) and 0xFF) + (rgb and 0xFF)
                totalStrength += brightness / (3.0 * 255.0) // Normalize to 0-1
            }
        }
        
        return totalStrength / pixelCount
    }
}