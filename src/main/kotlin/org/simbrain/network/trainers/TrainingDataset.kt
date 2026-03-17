package org.simbrain.network.trainers

import org.simbrain.network.core.TensorShape
import org.simbrain.util.toColumnVector
import smile.math.matrix.Matrix
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class TrainingDataset(
    val inputs: MutableList<MutableList<Double>>,
    val targets: MutableList<MutableList<Double>>,
    val inputSize: Int = if (inputs.isNotEmpty()) inputs[0].size else throw IllegalArgumentException("Cannot infer input size from empty data. Use constructor with explicit sizes."),
    val targetSize: Int = if (targets.isNotEmpty()) targets[0].size else throw IllegalArgumentException("Cannot infer target size from empty data. Use constructor with explicit sizes."),
    val inputRowNames: List<String>? = null,
    val targetRowNames: List<String>? = null,
    val inputColumnNames: List<String>? = null,
    val targetColumnNames: List<String>? = null
): Iterable<Pair<List<Double>, List<Double>>> {

    init {
        if (inputs.size != targets.size) {
            throw IllegalArgumentException("inputs and targets must be the same size")
        }
        // Validate dimensions for non-empty datasets
        if (inputs.isNotEmpty()) {
            inputs.forEach { input ->
                if (input.size != inputSize) {
                    throw IllegalArgumentException("Input row has ${input.size} columns, expected $inputSize")
                }
            }
        }
        if (targets.isNotEmpty()) {
            targets.forEach { target ->
                if (target.size != targetSize) {
                    throw IllegalArgumentException("Target row has ${target.size} columns, expected $targetSize")
                }
            }
        }
    }

    val size get() = inputs.size

    override fun iterator(): Iterator<Pair<List<Double>, List<Double>>> = object : Iterator<Pair<List<Double>, List<Double>>> {
        private var index = 0
        override fun hasNext() = index < size
        override fun next(): Pair<List<Double>, List<Double>> {
            if (!hasNext()) throw NoSuchElementException()
            return (inputs[index] to targets[index]).also { index++ }
        }
    }

    // Helper method to get input row as Matrix (for compatibility during transition)
    fun getInputRow(index: Int): Matrix = inputs[index].toDoubleArray().toColumnVector()
    
    // Helper method to get target row as Matrix (for compatibility during transition)
    fun getTargetRow(index: Int): Matrix = targets[index].toDoubleArray().toColumnVector()

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
        // Create input row with 1.0 at diagonal position, 0.0 elsewhere
        val inputRow = MutableList(nInputs) { col -> if (col == row) 1.0 else 0.0 }
        inputs.add(inputRow)
        
        // Create target row with 1.0 at shifted diagonal position, 0.0 elsewhere
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
        // Enumerate 1..(2^n - 1) to exclude the all-zero vector
        val total = 1 shl n
        for (mask in 1 until total) {
            val row = MutableList(n) { bit -> if (((mask shr bit) and 1) == 1) 1.0 else 0.0 }
            rows.add(row)
        }
        // Shuffle for nicer viewing order
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
        // circularly shift the copied segment into output
        for (i in 0 until copyLen) {
            val src = input[i]
            val dst = (i + (shift % outSize + outSize) % outSize) % outSize
            out[dst] = src
        }
        return out
    }

    // Decide how to build inputs
    val inputs: MutableList<MutableList<Double>> = when {
        // Big enough and auto-associative: 20 random binary patterns
        nInputs >= 5 && nInputs == nOutputs -> randomBinaryPatterns(nInputs, 20)

        // Tiny but auto-associative: use all non-zero patterns; if too few, repeat
        nInputs < 5 && nInputs == nOutputs -> {
            val base = allNonZeroBinaryPatterns(nInputs)
            if (base.size >= 6) base
            else {
                // nInputs==1 -> 1 pattern; nInputs==2 -> 3 patterns; repeat to 6–8 rows
                val need = 6
                val out = mutableListOf<MutableList<Double>>()
                while (out.size < need) out.addAll(base)
                out.take(need).toMutableList()
            }
        }

        // Non-square nets: prefer 20 random if inputs are large enough, else all-nonzero
        nInputs >= 5 -> randomBinaryPatterns(nInputs, 20)
        else -> {
            val base = allNonZeroBinaryPatterns(nInputs)
            // Ensure at least 6 rows for tiny inputs
            if (base.size >= 6) base else {
                val need = 6
                val out = mutableListOf<MutableList<Double>>()
                while (out.size < need) out.addAll(base)
                out.take(need).toMutableList()
            }
        }
    }

    // Build targets
    val targets: MutableList<MutableList<Double>> = when {
        nInputs == nOutputs -> {
            // Auto-associative: targets = inputs (classic)
            inputs.map { it.toMutableList() }.toMutableList()
        }
        else -> {
            // Map inputs to outputs with a simple shift/truncate/pad rule
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

/**
 * Creates simple synthetic tensor classification data for CNN-style models.
 *
 * Produces `nOutputs * samplesPerClass` samples.
 *
 * Inputs are image-like tensors in HWC layout containing simple spatial primitives whose positions
 * and sizes vary across samples. Each class is assigned a primitive family cyclically from a fixed
 * set (horizontal, vertical, diagonals, box, plus, blob), and when multiple channels are present
 * the main pattern is drawn in one primary channel chosen by class index. The same pattern is then
 * copied to the other channels at reduced intensity according to [ghostingPercent], and a small
 * amount of random noise is added to the primary channel.
 *
 * Targets are one-hot encoded class labels.
 *
 * This generator does not guarantee that every class has a globally unique visual pattern. If
 * `nOutputs` is large relative to the tensor size, number of channels, and primitive families,
 * classes may become visually similar or overlap.
 *
 * Example: if `nOutputs = 10` and `samplesPerClass = 3`, the dataset will contain 30 total samples.
 * Since there are 7 primitive families, some primitive types will repeat across classes (for example,
 * the first 7 classes will use 7 different primitive families, and classes 8-10 will wrap around to
 * the start of that list).
 */
fun createSimpleTensorClassificationDataset(
    inputShape: TensorShape,
    nOutputs: Int,
    samplesPerClass: Int = 12,
    ghostingPercent: Int = 50,
    rngSeed: Long = 42L,
): TrainingDataset {

    require(nOutputs >= 1) { "nOutputs must be >= 1" }
    require(samplesPerClass >= 1) { "samplesPerClass must be >= 1" }
    require(ghostingPercent in 0..100) { "ghostingPercent must be between 0 and 100" }

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

    repeat(nOutputs) { classIndex ->
        val primitive = primitiveFamilies[classIndex % primitiveFamilies.size]
        repeat(samplesPerClass) {
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
        // Add all elements except the first (shift up)
        addAll(this@shiftUpAndPadEndWithZero.drop(1))
        // Pad the end with zeros
        val dimension = elementDimension ?: if (this@shiftUpAndPadEndWithZero.isNotEmpty()) {
            this@shiftUpAndPadEndWithZero[0].size
        } else {
            throw IllegalArgumentException("Cannot determine element dimension from empty list. Please provide elementDimension parameter.")
        }
        val zeroRow = MutableList(dimension) { 0.0 }
        add(zeroRow)
    }
}
