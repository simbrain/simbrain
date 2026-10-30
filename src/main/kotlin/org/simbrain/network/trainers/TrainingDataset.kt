/**
 * Supervised training data: input and target rows of fixed width, stored as one primitive array per example so that
 * trainers copy rows straight into batch matrices and large datasets take 8 bytes per value rather than a boxed
 * Double each.
 */
package org.simbrain.network.trainers

import org.simbrain.util.DoubleArrayConverter
import org.simbrain.util.WithXStreamPropertyConverter
import org.simbrain.util.createXStreamPropertyConverter
import org.simbrain.util.toColumnVector
import smile.math.matrix.Matrix
import java.util.*

class TrainingDataset(
    val inputs: MutableList<DoubleArray> = mutableListOf(),
    val targets: MutableList<DoubleArray> = mutableListOf(),
    val inputSize: Int = inferredWidth(inputs.firstOrNull()?.size, "input"),
    val targetSize: Int = inferredWidth(targets.firstOrNull()?.size, "target"),
    val inputRowNames: List<String>? = null,
    val targetRowNames: List<String>? = null,
    val inputColumnNames: List<String>? = null,
    val targetColumnNames: List<String>? = null
): Iterable<Pair<DoubleArray, DoubleArray>> {

    init {
        if (inputs.size != targets.size) {
            throw IllegalArgumentException("inputs and targets must be the same size")
        }
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

    override fun iterator(): Iterator<Pair<DoubleArray, DoubleArray>> = object : Iterator<Pair<DoubleArray, DoubleArray>> {
        private var index = 0
        override fun hasNext() = index < size
        override fun next(): Pair<DoubleArray, DoubleArray> {
            if (!hasNext()) throw NoSuchElementException()
            return (inputs[index] to targets[index]).also { index++ }
        }
    }

    fun getInputRow(index: Int): Matrix = inputs[index].toColumnVector()

    fun getTargetRow(index: Int): Matrix = targets[index].toColumnVector()

    /**
     * Customizes XStream marshalling for the two bulk fields only. `inputs` and `targets`
     * are written as a single base64-encoded binary blob (rows + cols + data) instead of
     * one `<double>X.X</double>` element per cell — the same shape used by
     * [org.simbrain.util.MatrixConverter] for smile matrices. Cuts the saved XML for
     * NETtalk-shaped data by ~4× and makes save/load O(1) XML nodes per matrix.
     *
     * Every other field falls through to the reflective converter, so adding a new
     * field to [TrainingDataset] requires no change here.
     *
     * Backward-compatible read: legacy `<list><double>…</double></list>` blocks are
     * still parsed correctly.
     */
    companion object: WithXStreamPropertyConverter {

        /**
         * Builds a dataset from rows given as lists, as many simulations write them. The rows are copied into
         * primitive arrays.
         */
        operator fun invoke(
            inputs: List<List<Double>>,
            targets: List<List<Double>>,
            inputSize: Int = inferredWidth(inputs.firstOrNull()?.size, "input"),
            targetSize: Int = inferredWidth(targets.firstOrNull()?.size, "target"),
            inputRowNames: List<String>? = null,
            targetRowNames: List<String>? = null,
            inputColumnNames: List<String>? = null,
            targetColumnNames: List<String>? = null
        ) = TrainingDataset(
            inputs.mapTo(ArrayList(inputs.size)) { it.toDoubleArray() },
            targets.mapTo(ArrayList(targets.size)) { it.toDoubleArray() },
            inputSize, targetSize, inputRowNames, targetRowNames, inputColumnNames, targetColumnNames
        )

        override val xStreamPropertyConverter = createXStreamPropertyConverter<TrainingDataset>(
            marshal = {
                on(TrainingDataset::inputs) { writer, _ -> writeMatrix(writer, "inputs", this) }
                on(TrainingDataset::targets) { writer, _ -> writeMatrix(writer, "targets", this) }
            },
            unmarshal = {
                on("inputs") { reader, _ ->
                    val parsed = readMatrix(reader)
                    withConstructedObject { inputs.addAll(parsed) }
                }
                on("targets") { reader, _ ->
                    val parsed = readMatrix(reader)
                    withConstructedObject { targets.addAll(parsed) }
                }
            }
        )

        private fun writeMatrix(
            writer: com.thoughtworks.xstream.io.HierarchicalStreamWriter,
            nodeName: String,
            rows: List<DoubleArray>,
        ) {
            val colCount = rows.firstOrNull()?.size ?: 0
            writer.startNode(nodeName)
            writer.startNode("rows"); writer.setValue(rows.size.toString()); writer.endNode()
            writer.startNode("cols"); writer.setValue(colCount.toString()); writer.endNode()
            val flat = DoubleArray(rows.size * colCount)
            rows.forEachIndexed { r, row -> row.copyInto(flat, r * colCount) }
            writer.startNode("data")
            // Always use raw base64. DoubleArrayConverter.arrayToString switches to
            // a precision-5 JSON form below 100 elements, which would truncate data.
            writer.setValue(Base64.getEncoder().encodeToString(DoubleArrayConverter.doubleArrayToByteArray(flat)))
            writer.endNode()
            writer.endNode()
        }

        private fun readMatrix(reader: com.thoughtworks.xstream.io.HierarchicalStreamReader): MutableList<DoubleArray> {
            var rows: Int? = null
            var cols: Int? = null
            var data: DoubleArray? = null
            val legacyRows = mutableListOf<DoubleArray>()
            var sawLegacy = false

            while (reader.hasMoreChildren()) {
                reader.moveDown()
                when (reader.nodeName) {
                    "rows" -> rows = reader.value.toInt()
                    "cols" -> cols = reader.value.toInt()
                    "data" -> data = DoubleArrayConverter.stringToArray(reader.value)
                    "list" -> {
                        sawLegacy = true
                        val row = mutableListOf<Double>()
                        while (reader.hasMoreChildren()) {
                            reader.moveDown()
                            if (reader.nodeName == "double") row.add(reader.value.toDouble())
                            reader.moveUp()
                        }
                        legacyRows.add(row.toDoubleArray())
                    }
                }
                reader.moveUp()
            }

            if (sawLegacy) return legacyRows
            val r = rows ?: 0
            val c = cols ?: 0
            val d = data
            if (d == null || r * c == 0) return mutableListOf()
            return MutableList(r) { row -> d.copyOfRange(row * c, (row + 1) * c) }
        }
    }
}

/** The width of the first row, [firstRowSize], for a dataset built without an explicit size. */
private fun inferredWidth(firstRowSize: Int?, what: String): Int = firstRowSize
    ?: throw IllegalArgumentException("Cannot infer $what size from empty data. Use constructor with explicit sizes.")
