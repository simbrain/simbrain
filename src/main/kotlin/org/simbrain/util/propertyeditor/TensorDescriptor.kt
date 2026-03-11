package org.simbrain.util.propertyeditor

/**
 * Describes how a flat [DoubleArray] should be displayed as a multi-dimensional tensor in a [TensorWidget].
 *
 * The [dimensions] array gives the size of each axis. By default, the first two dimensions are used as
 * table rows and columns, and all remaining dimensions become tabs. This can be overridden with [rowAxis]
 * and [colAxis].
 *
 * Example for HWC activations (28x28x3):
 * ```
 * TensorDescriptor(intArrayOf(28, 28, 3), arrayOf("H", "W", "Channel"))
 * ```
 * Shows 3 tabs (one per channel), each with a 28x28 table.
 *
 * Example for convolution kernels (16 filters, 3 input channels, 5x5):
 * ```
 * TensorDescriptor(intArrayOf(16, 3, 5, 5), arrayOf("Filter", "Channel", "H", "W"), rowAxis = 2, colAxis = 3)
 * ```
 * Shows 48 tabs (16 filters x 3 channels), each with a 5x5 table.
 */
class TensorDescriptor(
    val dimensions: IntArray,
    val dimensionLabels: Array<String>? = null,
    val rowAxis: Int = 0,
    val colAxis: Int = 1
) {
    val ndim get() = dimensions.size

    /** Row-major strides for the flat array. */
    val strides: IntArray = IntArray(dimensions.size).also { s ->
        if (dimensions.isNotEmpty()) {
            s[dimensions.lastIndex] = 1
            for (i in dimensions.lastIndex - 1 downTo 0) {
                s[i] = s[i + 1] * dimensions[i + 1]
            }
        }
    }

    /** Axes that become tabs (everything except rowAxis and colAxis). */
    val tabAxes: IntArray = (0 until dimensions.size).filter { it != rowAxis && it != colAxis }.toIntArray()

    /** Number of rows in each 2D table slice. */
    val numRows get() = dimensions[rowAxis]

    /** Number of columns in each 2D table slice. */
    val numCols get() = dimensions[colAxis]

    /** Total number of tab slices (product of all tab axis dimensions). */
    val numSlices: Int get() = tabAxes.fold(1) { acc, axis -> acc * dimensions[axis] }

    /**
     * Compute flat-array index given full N-D indices.
     */
    fun index(indices: IntArray): Int {
        var idx = 0
        for (i in indices.indices) idx += indices[i] * strides[i]
        return idx
    }

    /**
     * Get the tab indices for a given slice number.
     * E.g., for dimensions [16, 3, 5, 5] with tabAxes [0, 1], slice 5 → [1, 2] (filter 1, channel 2).
     */
    fun sliceToTabIndices(sliceIndex: Int): IntArray {
        val result = IntArray(tabAxes.size)
        var remaining = sliceIndex
        for (i in tabAxes.indices.reversed()) {
            val axisSize = dimensions[tabAxes[i]]
            result[i] = remaining % axisSize
            remaining /= axisSize
        }
        return result
    }

    /**
     * Generate a human-readable label for a slice/tab.
     */
    fun sliceLabel(sliceIndex: Int): String {
        if (tabAxes.isEmpty()) return "Data"
        val tabIndices = sliceToTabIndices(sliceIndex)
        return tabAxes.indices.joinToString(" – ") { i ->
            val axisIdx = tabAxes[i]
            val label = dimensionLabels?.getOrNull(axisIdx) ?: "Dim$axisIdx"
            "$label ${tabIndices[i] + 1}"
        }
    }

    /**
     * Label for a single tab axis value (e.g., "Filter 3").
     */
    fun axisLabel(tabAxisIndex: Int, value: Int): String {
        val axisIdx = tabAxes[tabAxisIndex]
        val label = dimensionLabels?.getOrNull(axisIdx) ?: "Dim$axisIdx"
        return "$label ${value + 1}"
    }

    /**
     * Extract a 2D slice from a flat array for the given slice index.
     */
    fun extractSlice(flatArray: DoubleArray, sliceIndex: Int): Array<DoubleArray> {
        val tabIndices = sliceToTabIndices(sliceIndex)
        val indices = IntArray(ndim)
        // Fill in tab axis values
        for (i in tabAxes.indices) {
            indices[tabAxes[i]] = tabIndices[i]
        }
        return Array(numRows) { r ->
            DoubleArray(numCols) { c ->
                indices[rowAxis] = r
                indices[colAxis] = c
                flatArray[index(indices)]
            }
        }
    }

    /**
     * Write a 2D slice back into the flat array at the given slice index.
     */
    fun writeSlice(flatArray: DoubleArray, sliceIndex: Int, slice: Array<DoubleArray>) {
        val tabIndices = sliceToTabIndices(sliceIndex)
        val indices = IntArray(ndim)
        for (i in tabAxes.indices) {
            indices[tabAxes[i]] = tabIndices[i]
        }
        for (r in 0 until numRows) {
            for (c in 0 until numCols) {
                indices[rowAxis] = r
                indices[colAxis] = c
                flatArray[index(indices)] = slice[r][c]
            }
        }
    }
}
