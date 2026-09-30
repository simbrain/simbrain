/**
 * Tile normalization scales: the O(n) quantile selection must pick exactly the entry a full sort
 * would, and weight tiles must reuse a cached scale across resets while still re-deriving it when
 * the source tensor changes.
 */
package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.FloatTensor
import java.awt.Color
import kotlin.random.Random

class MagnitudeQuantileScaleTest {

    private fun sortedScale(magnitudes: FloatArray): Float {
        val sorted = magnitudes.sortedArray()
        val quantile = sorted[(sorted.size.toLong() * 995 / 1000).toInt().coerceAtMost(sorted.size - 1)]
        return if (quantile > 0f) quantile else sorted[sorted.size - 1]
    }

    @Test
    fun `selection matches the sorted quantile across sizes and distributions`() {
        val random = Random(7)
        val generators = listOf<(Int) -> FloatArray>(
            { n -> FloatArray(n) { random.nextFloat() } },
            { n -> FloatArray(n) { (random.nextInt(4)).toFloat() } },
            { n -> FloatArray(n) { it.toFloat() } },
            { n -> FloatArray(n) { (n - it).toFloat() } },
            { n -> FloatArray(n) { if (random.nextInt(1000) == 0) 50f else 0f } },
            { n -> FloatArray(n) { 0f } },
        )
        for (n in listOf(1, 2, 3, 10, 199, 200, 201, 1000, 12_345)) {
            for (generate in generators) {
                val magnitudes = generate(n)
                assertEquals(sortedScale(magnitudes), magnitudeQuantileScale(magnitudes.copyOf()), "n=$n")
            }
        }
    }

    @Test
    fun `a nan magnitude sorts last like the full sort`() {
        val magnitudes = FloatArray(10) { 1f } + Float.NaN
        assertEquals(sortedScale(magnitudes), magnitudeQuantileScale(magnitudes.copyOf()))
    }

    private fun shade(tile: MatrixTile): IntArray {
        val dest = IntArray(tile.cols)
        tile.shadePatch(dest, tile.cols, tile.cols, 1, 0.0, 1.0, 0.0, tile.cols.toDouble(),
            Color.BLUE, Color.WHITE, Color.RED)
        return dest
    }

    @Test
    fun `a reset weight tile republishes with the same scale`() {
        val weights = FloatTensor(1, 4)
        weights.copyFrom(floatArrayOf(0.1f, -0.5f, 0.25f, 1f))
        val tile = MatrixTile("w", "w", weights, kind = TileKind.WEIGHT, quantileNorm = true)
        tile.publish(-1)
        val before = shade(tile)

        tile.reset()
        tile.publish(-1)
        assertArrayEquals(before, shade(tile))
    }

    @Test
    fun `mutating the source re-derives the cached scale`() {
        val weights = FloatTensor(1, 2)
        weights.copyFrom(floatArrayOf(0.5f, 1f))
        val tile = MatrixTile("w", "w", weights, kind = TileKind.WEIGHT)
        tile.publish(-1)
        val before = shade(tile)[0]

        weights[0, 1] = 4f
        tile.publish(-1)
        assertNotEquals(before, shade(tile)[0])
    }
}
