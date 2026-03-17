package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TrainingDatasetTest {

    @Test
    fun `circle is drawn centered with correct radius`() {
        val grid = drawShape(ShapeType.CIRCLE, 50, 50, centerRow = 25.0, centerCol = 25.0, size = 10.0)
        // Center pixel must be on
        assertEquals(1.0, grid[25 * 50 + 25])
        // Pixel just inside radius must be on
        assertEquals(1.0, grid[25 * 50 + 34])   // col 34, distance = 9 < 10
        // Pixel just outside radius must be off
        assertEquals(0.0, grid[25 * 50 + 36])   // col 36, distance = 11 > 10
    }

    @Test
    fun `square fills correct rectangular region`() {
        val grid = drawShape(ShapeType.SQUARE, 50, 50, centerRow = 25.0, centerCol = 25.0, size = 5.0)
        // Corners of the expected [20..30] × [20..30] region must be on
        for (r in 20..30) for (c in 20..30) assertEquals(1.0, grid[r * 50 + c], "expected on at ($r,$c)")
        // Just outside the region must be off
        assertEquals(0.0, grid[19 * 50 + 25])
        assertEquals(0.0, grid[31 * 50 + 25])
    }

    @Test
    fun `ellipse is wider than tall with default aspect`() {
        // ellipseAspect = 0.5 → semiMinor = 5, semiMajor = 10
        val grid = drawShape(ShapeType.ELLIPSE, 50, 50, centerRow = 25.0, centerCol = 25.0, size = 10.0)
        // Along major axis (horizontal): col 34 should be inside (distance/10 = 0.9 ≤ 1)
        assertEquals(1.0, grid[25 * 50 + 34])
        // Along minor axis (vertical): row 30 should be inside (distance/5 = 1.0 ≤ 1)
        assertEquals(1.0, grid[30 * 50 + 25])
        // Along minor axis: row 31 should be outside (distance/5 = 1.2 > 1)
        assertEquals(0.0, grid[31 * 50 + 25])
    }

    @Test
    fun `rectangle is wider than tall with default aspect`() {
        // size=5, rectAspect=2.0 → halfH=5, halfW=10
        val grid = drawShape(ShapeType.RECTANGLE, 50, 50, centerRow = 25.0, centerCol = 25.0, size = 5.0)
        val litPixels = grid.count { it == 1.0 }
        val expectedH = 11  // rows 20..30
        val expectedW = 21  // cols 15..35
        assertEquals(expectedH * expectedW, litPixels)
    }

    @Test
    fun `output array has correct length`() {
        ShapeType.entries.forEach { type ->
            val grid = drawShape(type, 30, 40, centerRow = 15.0, centerCol = 20.0, size = 5.0)
            assertEquals(30 * 40, grid.size)
        }
    }

    @Test
    fun `shape stays within bounds when placed near edge`() {
        // Place a large circle near the corner — clipping should keep all indices valid
        val grid = drawShape(ShapeType.CIRCLE, 50, 50, centerRow = 2.0, centerCol = 2.0, size = 10.0)
        assertEquals(50 * 50, grid.size)
        assertTrue(grid.all { it == 0.0 || it == 1.0 })
    }

    @Test
    fun `dataset has correct number of rows`() {
        val ds = createShapeDataset(samplesPerClass = 8, rngSeed = 0L)
        assertEquals(ShapeType.entries.size * 8, ds.size)
    }

    @Test
    fun `input and target sizes match grid dimensions`() {
        val ds = createShapeDataset(height = 50, width = 50, samplesPerClass = 4, rngSeed = 0L)
        assertEquals(2500, ds.inputSize)
        assertEquals(2500, ds.targetSize)
        ds.inputs.forEach { assertEquals(2500, it.size) }
        ds.targets.forEach { assertEquals(2500, it.size) }
    }

    @Test
    fun `targets for same class are identical`() {
        val ds = createShapeDataset(samplesPerClass = 5, rngSeed = 0L)
        val n = 5
        // All CIRCLE targets (first n rows) should be the same centered prototype
        val firstTarget = ds.targets[0]
        for (i in 1 until n) assertEquals(firstTarget, ds.targets[i])
        // ELLIPSE targets (next n rows) should differ from CIRCLE targets
        assertNotEquals(firstTarget, ds.targets[n])
    }

    @Test
    fun `inputs for same class differ from each other`() {
        val ds = createShapeDataset(samplesPerClass = 5, rngSeed = 0L)
        // Different random placements should produce different inputs
        assertNotEquals(ds.inputs[0], ds.inputs[1])
    }

    @Test
    fun `target shapes are non-empty`() {
        val ds = createShapeDataset(samplesPerClass = 4, targetSize = 10.0, rngSeed = 0L)
        ds.targets.forEach { row ->
            assertTrue(row.any { it == 1.0 }, "target row should contain at least one lit pixel")
        }
    }
}
