package org.simbrain.network.trainers

import org.simbrain.util.toColumnVector
import smile.math.matrix.Matrix
import kotlin.math.min

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