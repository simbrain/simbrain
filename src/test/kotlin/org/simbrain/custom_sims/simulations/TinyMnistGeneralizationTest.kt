/** Checks the image transforms used to build the altered Tiny MNIST testing sets. */
package org.simbrain.custom_sims.simulations

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.simulations.backprop.addNoise
import org.simbrain.custom_sims.simulations.backprop.rotateImage
import org.simbrain.custom_sims.simulations.backprop.shiftImage
import org.simbrain.custom_sims.simulations.backprop.tinyMnistTestVariants

class TinyMnistGeneralizationTest {

    private fun imageWithPixel(x: Int, y: Int) = DoubleArray(400).also { it[y * 20 + x] = 1.0 }

    @Test
    fun `shift moves pixels right and down and drops pixels pushed off the edge`() {
        assertArrayEquals(imageWithPixel(7, 6), shiftImage(imageWithPixel(5, 5), 2, 1))
        assertEquals(0.0, shiftImage(imageWithPixel(19, 5), 1, 0).sum())
    }

    @Test
    fun `rotating by 90 degrees moves a pixel at the top to the right side`() {
        val rotated = rotateImage(imageWithPixel(10, 2), 90.0)
        assertEquals(1.0, rotated.sum())
        assertEquals(1.0, rotated[10 * 20 + 17])
    }

    @Test
    fun `noise stays in the pixel range and noisy variant is reproducible`() {
        val image = imageWithPixel(3, 3)
        val noisy = addNoise(image, 0.3, java.util.Random(1))
        assertTrue(noisy.all { it in 0.0..1.0 })
        val noisyVariant = tinyMnistTestVariants.first { it.label == "Noisy" }
        assertArrayEquals(noisyVariant.transform(image), noisyVariant.transform(image))
    }
}
