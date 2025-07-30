package org.simbrain.world.imageworld.filters

import org.simbrain.util.UserParameter
import java.awt.image.BufferedImage
import kotlin.math.*

/**
 * Edge detection filter with multiple algorithms for detecting edges in images.
 */
class EdgeDetectionFilter : ImageFilter() {

    enum class EdgeDetectionMethod {
        SOBEL, PREWITT, ROBERTS, CANNY;
        
        override fun toString(): String {
            return name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    @UserParameter(label = "Method", description = "Edge detection algorithm to use", order = 2)
    var method: EdgeDetectionMethod = EdgeDetectionMethod.SOBEL

    @UserParameter(label = "Threshold", description = "Threshold for edge detection (0.0 - 1.0)", order = 3)
    var threshold: Double = 0.3
        set(value) {
            field = maxOf(0.0, minOf(1.0, value))
        }

    @UserParameter(label = "Enhance Edges", description = "Whether to enhance detected edges", order = 4)
    var enhanceEdges: Boolean = true

    @UserParameter(label = "Gaussian Blur Sigma", description = "Sigma for pre-processing Gaussian blur (0 = no blur)", order = 5)
    var gaussianSigma: Double = 0.0
        set(value) {
            field = maxOf(0.0, value)
        }

    init {
        name = "Edge Detection Filter"
    }

    override fun applyFilter(input: BufferedImage): BufferedImage {
        var processedImage = input
        
        // Apply Gaussian blur if requested
        if (gaussianSigma > 0) {
            processedImage = applyGaussianBlur(processedImage, gaussianSigma)
        }

        return when (method) {
            EdgeDetectionMethod.SOBEL -> applySobelFilter(processedImage)
            EdgeDetectionMethod.PREWITT -> applyPrewittFilter(processedImage)
            EdgeDetectionMethod.ROBERTS -> applyRobertsFilter(processedImage)
            EdgeDetectionMethod.CANNY -> applyCannyFilter(processedImage)
        }
    }

    private fun applyGaussianBlur(input: BufferedImage, sigma: Double): BufferedImage {
        val kernelSize = (sigma * 6).toInt().let { if (it % 2 == 0) it + 1 else it }
        val kernel = createGaussianKernel(kernelSize, sigma)
        return applyConvolution(input, kernel)
    }

    private fun createGaussianKernel(size: Int, sigma: Double): Array<DoubleArray> {
        val kernel = Array(size) { DoubleArray(size) }
        val center = size / 2
        var sum = 0.0

        for (y in 0 until size) {
            for (x in 0 until size) {
                val xDist = x - center
                val yDist = y - center
                val value = exp(-(xDist * xDist + yDist * yDist) / (2 * sigma * sigma))
                kernel[y][x] = value
                sum += value
            }
        }

        // Normalize
        for (y in 0 until size) {
            for (x in 0 until size) {
                kernel[y][x] /= sum
            }
        }

        return kernel
    }

    private fun applySobelFilter(input: BufferedImage): BufferedImage {
        val sobelX = arrayOf(
            doubleArrayOf(-1.0, 0.0, 1.0),
            doubleArrayOf(-2.0, 0.0, 2.0),
            doubleArrayOf(-1.0, 0.0, 1.0)
        )
        
        val sobelY = arrayOf(
            doubleArrayOf(-1.0, -2.0, -1.0),
            doubleArrayOf(0.0, 0.0, 0.0),
            doubleArrayOf(1.0, 2.0, 1.0)
        )

        return applyGradientFilter(input, sobelX, sobelY)
    }

    private fun applyPrewittFilter(input: BufferedImage): BufferedImage {
        val prewittX = arrayOf(
            doubleArrayOf(-1.0, 0.0, 1.0),
            doubleArrayOf(-1.0, 0.0, 1.0),
            doubleArrayOf(-1.0, 0.0, 1.0)
        )
        
        val prewittY = arrayOf(
            doubleArrayOf(-1.0, -1.0, -1.0),
            doubleArrayOf(0.0, 0.0, 0.0),
            doubleArrayOf(1.0, 1.0, 1.0)
        )

        return applyGradientFilter(input, prewittX, prewittY)
    }

    private fun applyRobertsFilter(input: BufferedImage): BufferedImage {
        val robertsX = arrayOf(
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(0.0, -1.0)
        )
        
        val robertsY = arrayOf(
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(-1.0, 0.0)
        )

        return applyGradientFilter(input, robertsX, robertsY)
    }

    private fun applyGradientFilter(input: BufferedImage, kernelX: Array<DoubleArray>, kernelY: Array<DoubleArray>): BufferedImage {
        val width = input.width
        val height = input.height
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        // Convert to grayscale first
        val grayImage = convertToGrayscale(input)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val gx = applyKernel(grayImage, x, y, kernelX)
                val gy = applyKernel(grayImage, x, y, kernelY)
                
                val magnitude = sqrt(gx * gx + gy * gy)
                val normalizedMagnitude = magnitude / (255.0 * sqrt(2.0))
                
                val edgeStrength = if (normalizedMagnitude > threshold) {
                    if (enhanceEdges) min(1.0, normalizedMagnitude * 2.0) else normalizedMagnitude
                } else {
                    0.0
                }

                val pixelValue = (edgeStrength * 255).toInt()
                val rgb = (pixelValue shl 16) or (pixelValue shl 8) or pixelValue
                output.setRGB(x, y, rgb)
            }
        }

        return output
    }

    private fun applyCannyFilter(input: BufferedImage): BufferedImage {
        // Simplified Canny implementation (for full Canny, would need non-maximum suppression and hysteresis)
        return applySobelFilter(input)
    }

    private fun convertToGrayscale(input: BufferedImage): Array<IntArray> {
        val width = input.width
        val height = input.height
        val gray = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = input.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                gray[y][x] = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
            }
        }

        return gray
    }

    private fun applyKernel(grayImage: Array<IntArray>, centerX: Int, centerY: Int, kernel: Array<DoubleArray>): Double {
        var sum = 0.0
        val kernelHeight = kernel.size
        val kernelWidth = kernel[0].size
        val offsetY = kernelHeight / 2
        val offsetX = kernelWidth / 2

        for (ky in 0 until kernelHeight) {
            for (kx in 0 until kernelWidth) {
                val x = centerX + kx - offsetX
                val y = centerY + ky - offsetY

                val clampedX = maxOf(0, minOf(grayImage[0].size - 1, x))
                val clampedY = maxOf(0, minOf(grayImage.size - 1, y))

                sum += grayImage[clampedY][clampedX] * kernel[ky][kx]
            }
        }

        return sum
    }

    private fun applyConvolution(input: BufferedImage, kernel: Array<DoubleArray>): BufferedImage {
        val width = input.width
        val height = input.height
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val kernelSize = kernel.size
        val center = kernelSize / 2

        for (y in 0 until height) {
            for (x in 0 until width) {
                var red = 0.0
                var green = 0.0
                var blue = 0.0

                for (ky in 0 until kernelSize) {
                    for (kx in 0 until kernelSize) {
                        val px = x + kx - center
                        val py = y + ky - center

                        val clampedX = maxOf(0, minOf(width - 1, px))
                        val clampedY = maxOf(0, minOf(height - 1, py))

                        val rgb = input.getRGB(clampedX, clampedY)
                        val r = (rgb shr 16) and 0xFF
                        val g = (rgb shr 8) and 0xFF
                        val b = rgb and 0xFF

                        val kernelValue = kernel[ky][kx]
                        red += r * kernelValue
                        green += g * kernelValue
                        blue += b * kernelValue
                    }
                }

                val finalRed = maxOf(0, minOf(255, red.toInt()))
                val finalGreen = maxOf(0, minOf(255, green.toInt()))
                val finalBlue = maxOf(0, minOf(255, blue.toInt()))

                val finalRgb = (finalRed shl 16) or (finalGreen shl 8) or finalBlue
                output.setRGB(x, y, finalRgb)
            }
        }

        return output
    }

    override fun copy(): ImageFilter {
        return EdgeDetectionFilter().apply {
            enabled = this@EdgeDetectionFilter.enabled
            name = this@EdgeDetectionFilter.name
            method = this@EdgeDetectionFilter.method
            threshold = this@EdgeDetectionFilter.threshold
            enhanceEdges = this@EdgeDetectionFilter.enhanceEdges
            gaussianSigma = this@EdgeDetectionFilter.gaussianSigma
        }
    }
}