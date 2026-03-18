package org.simbrain.network.trainers

import org.simbrain.network.core.TensorShape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random


enum class ShapeType { CIRCLE, ELLIPSE, SQUARE, RECTANGLE }

/**
 * Draws a shape into a flat binary image (row-major, 0.0/1.0) of [height] × [width] pixels.
 *
 * The shape is placed at the given [centerRow]/[centerCol] with the given size parameter:
 * - CIRCLE / ELLIPSE: [size] is the radius / semi-major axis (semi-minor = size * [ellipseAspect])
 * - SQUARE / RECTANGLE: [size] is the half-side (full side = 2 * size)
 *   For RECTANGLE the height is [size] and width is size * [rectAspect].
 *
 * The returned array has length height * width and is compatible with:
 * - A flat neuron-array input of that size
 * - A single-channel tensor via TensorShape(height, width, 1)
 */
fun drawShape(
    type: ShapeType,
    height: Int,
    width: Int,
    centerRow: Double,
    centerCol: Double,
    size: Double,
    ellipseAspect: Double = 0.5,
    rectAspect: Double = 2.0,
): DoubleArray {
    val grid = DoubleArray(height * width)

    fun set(row: Int, col: Int) {
        if (row in 0 until height && col in 0 until width)
            grid[row * width + col] = 1.0
    }

    when (type) {
        ShapeType.CIRCLE -> {
            for (r in 0 until height)
                for (c in 0 until width)
                    if (sqrt((r - centerRow) * (r - centerRow) + (c - centerCol) * (c - centerCol)) <= size)
                        set(r, c)
        }

        ShapeType.ELLIPSE -> {
            val semiMajor = size
            val semiMinor = size * ellipseAspect
            for (r in 0 until height)
                for (c in 0 until width)
                    if ((c - centerCol) * (c - centerCol) / (semiMajor * semiMajor) +
                        (r - centerRow) * (r - centerRow) / (semiMinor * semiMinor) <= 1.0)
                        set(r, c)
        }

        ShapeType.SQUARE -> {
            val top = (centerRow - size).toInt().coerceAtLeast(0)
            val bottom = (centerRow + size).toInt().coerceAtMost(height - 1)
            val left = (centerCol - size).toInt().coerceAtLeast(0)
            val right = (centerCol + size).toInt().coerceAtMost(width - 1)
            for (r in top..bottom)
                for (c in left..right)
                    set(r, c)
        }

        ShapeType.RECTANGLE -> {
            val halfH = size
            val halfW = size * rectAspect
            val top = (centerRow - halfH).toInt().coerceAtLeast(0)
            val bottom = (centerRow + halfH).toInt().coerceAtMost(height - 1)
            val left = (centerCol - halfW).toInt().coerceAtLeast(0)
            val right = (centerCol + halfW).toInt().coerceAtMost(width - 1)
            for (r in top..bottom)
                for (c in left..right)
                    set(r, c)
        }
    }

    return grid
}

/**
 * Shifts the sequence up by removing the first element and padding the end with zeros.
 * This mimics the behavior of Matrix.shiftUpAndPadEndWithZero() for MutableList format.
 *
 * For temporal sequence learning, this creates targets from inputs by shifting the sequence.
 * Example: [a, b, c, d] becomes [b, c, d, 0]
 *
 * @param elementDimension The dimension of each element (required for empty lists)
 */
fun MutableList<MutableList<Double>>.shiftUpAndPadEndWithZero(elementDimension: Int? = null): MutableList<MutableList<Double>> {
    return mutableListOf<MutableList<Double>>().apply {
        addAll(this@shiftUpAndPadEndWithZero.drop(1))
        val dimension = elementDimension ?: if (this@shiftUpAndPadEndWithZero.isNotEmpty()) {
            this@shiftUpAndPadEndWithZero[0].size
        } else {
            throw IllegalArgumentException("Cannot determine element dimension from empty list. Please provide elementDimension parameter.")
        }
        val zeroRow = MutableList(dimension) { 0.0 }
        add(zeroRow)
    }
}

/**
 * Creates a dataset where the inputs and targets are both diagonal patterns.
 * A shift amount can be provided to shift the target pattern to the right.
 *
 * Provides a simple default training set
 */
fun createDiagonalDataset(nInputs: Int, nOutputs: Int, shiftAmount: Int = 0): TrainingDataset {
    val nrows = min(nInputs, nOutputs)
    
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<MutableList<Double>>()
    
    repeat(nrows) { row ->
        val inputRow = MutableList(nInputs) { col -> if (col == row) 1.0 else 0.0 }
        inputs.add(inputRow)
        
        val targetRow = MutableList(nOutputs) { col -> 
            val shiftedCol = (row + shiftAmount) % nOutputs
            if (col == shiftedCol) 1.0 else 0.0 
        }
        targets.add(targetRow)
    }
    
    return TrainingDataset(
        inputs = inputs,
        targets = targets,
        inputSize = nInputs,
        targetSize = nOutputs
    )
}

fun createBouncingDataset(size: Int): TrainingDataset {

    val inputs = buildList {
        (0 until size).forEach { i ->
            add(MutableList(size) { j -> if (i == j) 1.0 else 0.0 })
        }
        (size - 2 downTo 1).forEach { i ->
            add(MutableList(size) { j -> if (i == j) 1.0 else 0.0 })
        }
    }

    val outputs = inputs.drop(1) + inputs.take(1)

    return TrainingDataset(
        inputs = inputs.toMutableList(),
        targets = outputs.toMutableList(),
        inputSize = size,
        targetSize = size
    )

}

/**
 * Creates a simple default training dataset that will look good in the Simbrain GUI.
 *
 * Behavior:
 *  - If nInputs >= 5 and nInputs == nOutputs (auto-associative): 20 random binary patterns; targets = inputs.
 *  - If nInputs < 5 and nInputs == nOutputs:
 *       * Use all non-zero binary patterns (length nInputs), shuffled.
 *       * If that yields < 6 rows (only possible for nInputs <= 2), repeat patterns to reach 6–8 rows.
 *  - If nInputs != nOutputs:
 *       * Generate inputs as above (20 random if nInputs >= 5, otherwise all non-zero).
 *       * Map inputs to outputs by a simple, visual permutation:
 *            - copy min(nInputs, nOutputs) bits with a circular shift (shiftAmount)
 *            - pad with zeros if nOutputs > nInputs, or truncate if nOutputs < nInputs
 */
fun createSimpleBinaryDataset(
    nInputs: Int,
    nOutputs: Int,
    shiftAmount: Int = 0,
    rngSeed: Long? = null
): TrainingDataset {

    require(nInputs >= 1 && nOutputs >= 1) { "nInputs and nOutputs must be >= 1" }
    val rng = rngSeed?.let { java.util.Random(it) } ?: java.util.Random()

    fun randomBinaryRow(n: Int) = MutableList(n) { if (rng.nextBoolean()) 1.0 else 0.0 }

    fun randomBinaryPatterns(n: Int, k: Int): MutableList<MutableList<Double>> =
        MutableList(k) { randomBinaryRow(n) }

    fun allNonZeroBinaryPatterns(n: Int): MutableList<MutableList<Double>> {
        val rows = mutableListOf<MutableList<Double>>()
        val total = 1 shl n
        for (mask in 1 until total) {
            val row = MutableList(n) { bit -> if (((mask shr bit) and 1) == 1) 1.0 else 0.0 }
            rows.add(row)
        }
        rows.shuffle(rng)
        return rows
    }

    fun mapInputToOutput(
        input: List<Double>,
        outSize: Int,
        shift: Int
    ): MutableList<Double> {
        val copyLen = minOf(input.size, outSize)
        val out = MutableList(outSize) { 0.0 }
        for (i in 0 until copyLen) {
            val src = input[i]
            val dst = (i + (shift % outSize + outSize) % outSize) % outSize
            out[dst] = src
        }
        return out
    }

    val inputs: MutableList<MutableList<Double>> = when {
        nInputs >= 5 && nInputs == nOutputs -> randomBinaryPatterns(nInputs, 20)

        nInputs < 5 && nInputs == nOutputs -> {
            val base = allNonZeroBinaryPatterns(nInputs)
            if (base.size >= 6) base
            else {
                val need = 6
                val out = mutableListOf<MutableList<Double>>()
                while (out.size < need) out.addAll(base)
                out.take(need).toMutableList()
            }
        }

        nInputs >= 5 -> randomBinaryPatterns(nInputs, 20)
        else -> {
            val base = allNonZeroBinaryPatterns(nInputs)
            if (base.size >= 6) base else {
                val need = 6
                val out = mutableListOf<MutableList<Double>>()
                while (out.size < need) out.addAll(base)
                out.take(need).toMutableList()
            }
        }
    }

    val targets: MutableList<MutableList<Double>> = when {
        nInputs == nOutputs -> {
            inputs.map { it.toMutableList() }.toMutableList()
        }
        else -> {
            inputs.map { mapInputToOutput(it, nOutputs, shiftAmount) }.toMutableList()
        }
    }

    return TrainingDataset(
        inputs = inputs,
        targets = targets,
        inputSize = nInputs,
        targetSize = nOutputs
    )
}

private enum class TensorPatternPrimitive {
    HORIZONTAL,
    VERTICAL,
    DIAGONAL_DOWN,
    DIAGONAL_UP,
    BOX,
    PLUS,
    BLOB
}


/**
 * Creates simple synthetic tensor classification data for CNN-style models.
 *
 * Produces samples with image-like tensor inputs containing simple spatial primitives (lines, boxes, etc.)
 * and one-hot encoded target vectors.
 *
 *  Example: if `nOutputs = 10` and `samplesPerClass = 3`, the dataset will contain 30 total samples.
 *  Since there are 7 primitive families (lines, blobs, etc.), so some primitive types will repeat across classes (for example,
 *  the first 7 classes will use 7 different primitive families, and classes 8-10 will wrap around to
 *  the start of that list).
 *
 * @param inputShape Shape of the input tensor (height × width × channels)
 * @param nOutputs Size of target vectors (number of output neurons)
 * @param samplesPerClass Number of samples per class (used when nOutputs <= maxClasses)
 * @param ghostingPercent Intensity (0-100) at which patterns are copied to non-primary channels
 * @param rngSeed Random seed for reproducibility
 * @param maxClasses Maximum number of distinct classes to generate. When nOutputs exceeds this,
 *                   only maxClasses classes are created with reduced samples to avoid slow generation.
 */
fun createSimpleTensorClassificationDataset(
    inputShape: TensorShape,
    nOutputs: Int,
    samplesPerClass: Int = 12,
    ghostingPercent: Int = 50,
    rngSeed: Long = 42L,
    maxClasses: Int = 20,
): TrainingDataset {

    require(nOutputs >= 1) { "nOutputs must be >= 1" }
    require(samplesPerClass >= 1) { "samplesPerClass must be >= 1" }
    require(ghostingPercent in 0..100) { "ghostingPercent must be between 0 and 100" }
    require(maxClasses >= 1) { "maxClasses must be >= 1" }

    val numClasses = nOutputs.coerceAtMost(maxClasses)
    val effectiveSamplesPerClass = if (nOutputs > maxClasses) 2 else samplesPerClass

    val rng = Random(rngSeed)
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<MutableList<Double>>()
    val ghostingLevel = ghostingPercent / 100.0

    val primitiveFamilies = listOf(
        TensorPatternPrimitive.HORIZONTAL,
        TensorPatternPrimitive.VERTICAL,
        TensorPatternPrimitive.DIAGONAL_DOWN,
        TensorPatternPrimitive.DIAGONAL_UP,
        TensorPatternPrimitive.BOX,
        TensorPatternPrimitive.PLUS,
        TensorPatternPrimitive.BLOB
    )

    fun DoubleArray.setPixel(row: Int, col: Int, channel: Int, value: Double = 1.0) {
        if (row in 0 until inputShape.height && col in 0 until inputShape.width && channel in 0 until inputShape.channels) {
            val index = inputShape.index(row, col, channel)
            this[index] = max(this[index], value)
        }
    }

    fun DoubleArray.addNoise(channel: Int) {
        val noisePoints = max(1, inputShape.size / 200)
        repeat(noisePoints) {
            if (rng.nextDouble() < 0.35) {
                val row = rng.nextInt(inputShape.height)
                val col = rng.nextInt(inputShape.width)
                setPixel(row, col, channel, 0.15)
            }
        }
    }

    fun DoubleArray.drawPrimitive(primitive: TensorPatternPrimitive, classIndex: Int) {
        val channel = if (inputShape.channels == 1) 0 else classIndex % inputShape.channels
        val minDim = min(inputShape.height, inputShape.width)
        val length = max(2, minDim / 3)
        val variableLength = min(minDim, length + rng.nextInt(0, max(1, minDim / 5) + 1))
        val boxSize = max(2, min(minDim - 1, variableLength))
        val centerRow = if (inputShape.height <= 2) inputShape.height / 2 else rng.nextInt(1, inputShape.height - 1)
        val centerCol = if (inputShape.width <= 2) inputShape.width / 2 else rng.nextInt(1, inputShape.width - 1)

        when (primitive) {
            TensorPatternPrimitive.HORIZONTAL -> {
                val row = rng.nextInt(inputShape.height)
                val startCol = rng.nextInt(max(1, inputShape.width - variableLength + 1))
                repeat(variableLength) { dc -> setPixel(row, startCol + dc, channel) }
            }
            TensorPatternPrimitive.VERTICAL -> {
                val col = rng.nextInt(inputShape.width)
                val startRow = rng.nextInt(max(1, inputShape.height - variableLength + 1))
                repeat(variableLength) { dr -> setPixel(startRow + dr, col, channel) }
            }
            TensorPatternPrimitive.DIAGONAL_DOWN -> {
                val diagLength = min(variableLength, minDim)
                val startRow = rng.nextInt(max(1, inputShape.height - diagLength + 1))
                val startCol = rng.nextInt(max(1, inputShape.width - diagLength + 1))
                repeat(diagLength) { d -> setPixel(startRow + d, startCol + d, channel) }
            }
            TensorPatternPrimitive.DIAGONAL_UP -> {
                val diagLength = min(variableLength, minDim)
                val startRow = if (inputShape.height == diagLength) diagLength - 1 else rng.nextInt(diagLength - 1, inputShape.height)
                val startCol = rng.nextInt(max(1, inputShape.width - diagLength + 1))
                repeat(diagLength) { d -> setPixel(startRow - d, startCol + d, channel) }
            }
            TensorPatternPrimitive.BOX -> {
                val half = max(1, boxSize / 2)
                val top = (centerRow - half).coerceAtLeast(0)
                val bottom = (centerRow + half).coerceAtMost(inputShape.height - 1)
                val left = (centerCol - half).coerceAtLeast(0)
                val right = (centerCol + half).coerceAtMost(inputShape.width - 1)
                for (col in left..right) {
                    setPixel(top, col, channel)
                    setPixel(bottom, col, channel)
                }
                for (row in top..bottom) {
                    setPixel(row, left, channel)
                    setPixel(row, right, channel)
                }
            }
            TensorPatternPrimitive.PLUS -> {
                val arm = max(1, variableLength / 2)
                for (offset in -arm..arm) {
                    setPixel(centerRow, centerCol + offset, channel)
                    setPixel(centerRow + offset, centerCol, channel)
                }
            }
            TensorPatternPrimitive.BLOB -> {
                val radius = max(1, variableLength / 3)
                for (row in (centerRow - radius)..(centerRow + radius)) {
                    for (col in (centerCol - radius)..(centerCol + radius)) {
                        if (abs(row - centerRow) + abs(col - centerCol) <= radius + 1) {
                            setPixel(row, col, channel)
                        }
                    }
                }
            }
        }

        if (inputShape.channels > 1 && ghostingLevel > 0.0) {
            for (ghostChannel in 0 until inputShape.channels) {
                if (ghostChannel == channel) continue
                for (row in 0 until inputShape.height) {
                    for (col in 0 until inputShape.width) {
                        val base = this[inputShape.index(row, col, channel)]
                        if (base > 0.0) {
                            setPixel(row, col, ghostChannel, base * ghostingLevel)
                        }
                    }
                }
            }
        }

        addNoise(channel)
    }

    repeat(numClasses) { classIndex ->
        val primitive = primitiveFamilies[classIndex % primitiveFamilies.size]
        repeat(effectiveSamplesPerClass) {
            val sample = DoubleArray(inputShape.size)
            sample.drawPrimitive(primitive, classIndex)
            inputs.add(sample.toMutableList())
            targets.add(MutableList(nOutputs) { outputIndex -> if (outputIndex == classIndex) 1.0 else 0.0 })
        }
    }

    return TrainingDataset(
        inputs = inputs,
        targets = targets,
        inputSize = inputShape.size,
        targetSize = nOutputs
    )
}

/**
 * Creates a dataset of geometric shapes for categorical perception tasks.
 *
 * Each sample pair consists of:
 * - **Input**: a shape drawn at a random position and random size within [[minSize], [maxSize]],
 *   on a grid of [height] × [width] pixels
 * - **Target**: the same shape type drawn centered at a fixed canonical [targetSize],
 *   on a grid of [targetHeight] × [targetWidth] pixels (defaults to input grid size)
 *
 * Inputs and targets are flat binary images compatible with:
 * - A flat neuron-array of the corresponding size
 * - A single-channel tensor via TensorShape(height, width, 1)
 *
 * @param height          Input grid height in pixels
 * @param width           Input grid width in pixels
 * @param targetHeight    Target grid height in pixels (defaults to [height])
 * @param targetWidth     Target grid width in pixels (defaults to [width])
 * @param samplesPerClass Number of random samples per shape class
 * @param minSize         Minimum shape size (radius / half-side) for inputs
 * @param maxSize         Maximum shape size (radius / half-side) for inputs
 * @param targetSize      Fixed size used for all centered target shapes
 * @param ellipseAspect   Ratio of semi-minor to semi-major axis for ellipses (< 1.0)
 * @param rectAspect      Ratio of width half-extent to height half-extent for rectangles (> 1.0)
 * @param rngSeed         Optional seed for reproducibility
 */
fun createShapeDataset(
    height: Int = 50,
    width: Int = 50,
    targetHeight: Int = height,
    targetWidth: Int = width,
    samplesPerClass: Int = 10,
    minSize: Double = 5.0,
    maxSize: Double = 25.0,
    targetSize: Double = 10.0,
    ellipseAspect: Double = 0.5,
    rectAspect: Double = 2.0,
    rngSeed: Long? = null
): TrainingDataset {
    val rng = rngSeed?.let { Random(it) } ?: Random.Default
    val shapeTypes = ShapeType.entries
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<MutableList<Double>>()

    val inputCenterRow = height / 2.0
    val inputCenterCol = width / 2.0
    val targetCenterRow = targetHeight / 2.0
    val targetCenterCol = targetWidth / 2.0

    shapeTypes.forEach { type ->
        val target = drawShape(type, targetHeight, targetWidth, targetCenterRow, targetCenterCol, targetSize, ellipseAspect, rectAspect)

        repeat(samplesPerClass) {
            val size = rng.nextDouble(minSize, maxSize)
            val margin = size * if (type == ShapeType.RECTANGLE) rectAspect else 1.0
            val rMin = margin; val rMax = height - margin
            val cMin = margin; val cMax = width - margin
            val cr = if (rMax > rMin) rng.nextDouble(rMin, rMax) else inputCenterRow
            val cc = if (cMax > cMin) rng.nextDouble(cMin, cMax) else inputCenterCol

            inputs.add(drawShape(type, height, width, cr, cc, size, ellipseAspect, rectAspect).toMutableList())
            targets.add(target.toMutableList())
        }
    }

    return TrainingDataset(
        inputs = inputs,
        targets = targets,
        inputSize = height * width,
        targetSize = targetHeight * targetWidth
    )
}
