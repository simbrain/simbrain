package org.simbrain.world.imageworld.filters

import org.simbrain.util.UserParameter
import java.awt.image.BufferedImage
import kotlin.math.*

/**
 * Gabor filter for detecting edges and textures at specific orientations and frequencies.
 * Gabor filters are commonly used in computer vision and image processing for feature detection.
 */
class GaborFilter : ImageFilter() {

    @UserParameter(label = "Frequency", description = "Spatial frequency of the sinusoidal component", order = 2)
    var frequency: Double = 0.1
        set(value) {
            field = maxOf(0.01, minOf(1.0, value))
        }

    @UserParameter(label = "Orientation (degrees)", description = "Orientation of the filter in degrees", order = 3)
    var orientationDegrees: Double = 0.0
        set(value) {
            field = value % 360.0
        }

    @UserParameter(label = "Sigma X", description = "Standard deviation in X direction", order = 4)
    var sigmaX: Double = 2.0
        set(value) {
            field = maxOf(0.1, value)
        }

    @UserParameter(label = "Sigma Y", description = "Standard deviation in Y direction", order = 5)
    var sigmaY: Double = 2.0
        set(value) {
            field = maxOf(0.1, value)
        }

    @UserParameter(label = "Phase", description = "Phase offset of the sinusoidal component", order = 6)
    var phase: Double = 0.0

    @UserParameter(label = "Kernel Size", description = "Size of the filter kernel (must be odd)", order = 7)
    var kernelSize: Int = 15
        set(value) {
            field = if (value % 2 == 0) value + 1 else value
            field = maxOf(3, field)
        }

    init {
        name = "Gabor Filter"
    }

    override fun applyFilter(input: BufferedImage): BufferedImage {
        val kernel = createGaborKernel()
        return applyConvolution(input, kernel)
    }

    private fun createGaborKernel(): Array<DoubleArray> {
        val kernel = Array(kernelSize) { DoubleArray(kernelSize) }
        val center = kernelSize / 2
        val theta = Math.toRadians(orientationDegrees)
        val cosTheta = cos(theta)
        val sinTheta = sin(theta)

        for (y in 0 until kernelSize) {
            for (x in 0 until kernelSize) {
                val xOffset = x - center
                val yOffset = y - center

                // Rotate coordinates
                val xRotated = xOffset * cosTheta + yOffset * sinTheta
                val yRotated = -xOffset * sinTheta + yOffset * cosTheta

                // Gaussian envelope
                val gaussian = exp(-0.5 * (xRotated * xRotated / (sigmaX * sigmaX) + 
                                           yRotated * yRotated / (sigmaY * sigmaY)))

                // Sinusoidal component
                val sinusoidal = cos(2 * PI * frequency * xRotated + phase)

                kernel[y][x] = gaussian * sinusoidal
            }
        }

        return kernel
    }

    private fun applyConvolution(input: BufferedImage, kernel: Array<DoubleArray>): BufferedImage {
        val width = input.width
        val height = input.height
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        
        val center = kernelSize / 2

        for (y in 0 until height) {
            for (x in 0 until width) {
                var red = 0.0
                var green = 0.0
                var blue = 0.0

                // Apply convolution
                for (ky in 0 until kernelSize) {
                    for (kx in 0 until kernelSize) {
                        val px = x + kx - center
                        val py = y + ky - center

                        // Handle boundaries by clamping
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

                // Normalize and clamp values
                val finalRed = maxOf(0, minOf(255, (red + 128).toInt()))
                val finalGreen = maxOf(0, minOf(255, (green + 128).toInt()))
                val finalBlue = maxOf(0, minOf(255, (blue + 128).toInt()))

                val finalRgb = (finalRed shl 16) or (finalGreen shl 8) or finalBlue
                output.setRGB(x, y, finalRgb)
            }
        }

        return output
    }

    override fun copy(): ImageFilter {
        return GaborFilter().apply {
            enabled = this@GaborFilter.enabled
            name = this@GaborFilter.name
            frequency = this@GaborFilter.frequency
            orientationDegrees = this@GaborFilter.orientationDegrees
            sigmaX = this@GaborFilter.sigmaX
            sigmaY = this@GaborFilter.sigmaY
            phase = this@GaborFilter.phase
            kernelSize = this@GaborFilter.kernelSize
        }
    }
}