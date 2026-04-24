package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.TensorShape

class TrainingDatasetUtilsTest {

    @Test
    fun `circle is drawn centered with correct radius`() {
        val grid = drawShape(ShapeType.CIRCLE, 50, 50, centerRow = 25.0, centerCol = 25.0, size = 10.0)
        assertEquals(1.0, grid[25 * 50 + 25])
        assertEquals(1.0, grid[25 * 50 + 34])
        assertEquals(0.0, grid[25 * 50 + 36])
    }

    @Test
    fun `square fills correct rectangular region`() {
        val grid = drawShape(ShapeType.SQUARE, 50, 50, centerRow = 25.0, centerCol = 25.0, size = 5.0)
        for (r in 20..30) for (c in 20..30) assertEquals(1.0, grid[r * 50 + c], "expected on at ($r,$c)")
        assertEquals(0.0, grid[19 * 50 + 25])
        assertEquals(0.0, grid[31 * 50 + 25])
    }

    @Test
    fun `ellipse is taller than wide with default aspect`() {
        val grid = drawShape(ShapeType.ELLIPSE, 50, 50, centerRow = 25.0, centerCol = 25.0, size = 10.0)
        assertEquals(1.0, grid[30 * 50 + 25])
        assertEquals(1.0, grid[34 * 50 + 25])
        assertEquals(0.0, grid[25 * 50 + 31])
    }

    @Test
    fun `rectangle is wider than tall with default aspect`() {
        val grid = drawShape(ShapeType.RECTANGLE, 50, 50, centerRow = 25.0, centerCol = 25.0, size = 5.0)
        val litPixels = grid.count { it == 1.0 }
        val expectedH = 11
        val expectedW = 21
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
        val firstTarget = ds.targets[0]
        for (i in 1 until n) assertEquals(firstTarget, ds.targets[i])
        assertNotEquals(firstTarget, ds.targets[n])
    }

    @Test
    fun `inputs for same class differ from each other`() {
        val ds = createShapeDataset(samplesPerClass = 5, rngSeed = 0L)
        assertNotEquals(ds.inputs[0], ds.inputs[1])
    }

    @Test
    fun `target shapes are non-empty`() {
        val ds = createShapeDataset(samplesPerClass = 4, targetSize = 10.0, rngSeed = 0L)
        ds.targets.forEach { row ->
            assertTrue(row.any { it == 1.0 }, "target row should contain at least one lit pixel")
        }
    }

    @Test
    fun `simple tensor classification dataset has expected sizes and one hot targets`() {
        val shape = TensorShape(height = 5, width = 4, channels = 1)
        val dataset = createSimpleTensorClassificationDataset(
            inputShape = shape,
            nOutputs = 3,
            samplesPerClass = 2,
            rngSeed = 7L
        )

        assertEquals(6, dataset.size)
        assertEquals(shape.size, dataset.inputSize)
        assertEquals(3, dataset.targetSize)

        dataset.inputs.forEach { input ->
            assertEquals(shape.size, input.size)
            assertTrue(input.any { it > 0.0 }, "Each sample should contain at least one active pixel")
        }

        dataset.targets.forEach { target ->
            assertEquals(1, target.count { it == 1.0 }, "Targets should be one-hot")
            assertEquals(2, target.count { it == 0.0 }, "Targets should be one-hot")
        }
    }

    @Test
    fun `simple tensor classification dataset is deterministic for a fixed seed`() {
        val shape = TensorShape(height = 6, width = 6, channels = 2)

        val first = createSimpleTensorClassificationDataset(shape, nOutputs = 4, samplesPerClass = 3, rngSeed = 123L)
        val second = createSimpleTensorClassificationDataset(shape, nOutputs = 4, samplesPerClass = 3, rngSeed = 123L)

        assertEquals(first.inputs, second.inputs)
        assertEquals(first.targets, second.targets)
    }

    @Test
    fun `simple tensor classification dataset draws each class strongly in one channel and ghosts to others`() {
        val shape = TensorShape(height = 7, width = 7, channels = 3)
        val dataset = createSimpleTensorClassificationDataset(
            inputShape = shape,
            nOutputs = 3,
            samplesPerClass = 1,
            rngSeed = 42L
        )

        fun maxByChannel(sample: List<Double>): List<Double> =
            (0 until shape.channels).map { channel ->
                (0 until shape.height).maxOf { row ->
                    (0 until shape.width).maxOf { col ->
                        sample[shape.index(row, col, channel)]
                    }
                }
            }

        val classChannels = dataset.inputs.map(::maxByChannel)

        assertEquals(listOf(1.0, 0.5, 0.5), classChannels[0])
        assertEquals(listOf(0.5, 1.0, 0.5), classChannels[1])
        assertEquals(listOf(0.5, 0.5, 1.0), classChannels[2])
    }

    @Test
    fun `simple tensor classification dataset with 100 percent ghosting replicates shapes across all channels`() {
        val shape = TensorShape(height = 7, width = 7, channels = 3)
        val dataset = createSimpleTensorClassificationDataset(
            inputShape = shape,
            nOutputs = 3,
            samplesPerClass = 1,
            ghostingPercent = 100,
            rngSeed = 42L
        )

        fun strongShapePixels(sample: List<Double>, channel: Int): Set<Pair<Int, Int>> =
            buildSet {
                for (row in 0 until shape.height) {
                    for (col in 0 until shape.width) {
                        if (sample[shape.index(row, col, channel)] > 0.9) {
                            add(row to col)
                        }
                    }
                }
            }

        dataset.inputs.forEach { sample ->
            val firstChannel = strongShapePixels(sample, 0)
            assertEquals(firstChannel, strongShapePixels(sample, 1))
            assertEquals(firstChannel, strongShapePixels(sample, 2))
        }
    }

    @Test
    fun `simple tensor classification dataset caps classes for large nOutputs`() {
        val shape = TensorShape(height = 8, width = 8, channels = 1)
        val nOutputs = 100

        val dataset = createSimpleTensorClassificationDataset(
            inputShape = shape,
            nOutputs = nOutputs,
            samplesPerClass = 10,
            maxClasses = 20
        )

        assertEquals(nOutputs, dataset.targetSize)
        assertEquals(40, dataset.size)

        val classesWithSamples = dataset.targets.map { target -> target.indexOfFirst { it == 1.0 } }.toSet()
        assertEquals(20, classesWithSamples.size)
        assertTrue(classesWithSamples.all { it in 0 until 20 })

        dataset.targets.forEach { target ->
            assertEquals(nOutputs, target.size)
            assertEquals(1, target.count { it == 1.0 })
        }
    }

}
