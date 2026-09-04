package org.simbrain.network.trainers

import org.simbrain.util.DoubleArrayConverter
import org.simbrain.util.WithXStreamPropertyConverter
import org.simbrain.util.createXStreamPropertyConverter
import org.simbrain.util.toColumnVector
import smile.math.matrix.Matrix
import java.util.*

class TrainingDataset(
    val inputs: MutableList<MutableList<Double>> = mutableListOf(),
    val targets: MutableList<MutableList<Double>> = mutableListOf(),
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

    fun getInputRow(index: Int): Matrix = inputs[index].toDoubleArray().toColumnVector()

    fun getTargetRow(index: Int): Matrix = targets[index].toDoubleArray().toColumnVector()

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
            rows: List<List<Double>>,
        ) {
            val colCount = rows.firstOrNull()?.size ?: 0
            writer.startNode(nodeName)
            writer.startNode("rows"); writer.setValue(rows.size.toString()); writer.endNode()
            writer.startNode("cols"); writer.setValue(colCount.toString()); writer.endNode()
            val flat = DoubleArray(rows.size * colCount)
            var k = 0
            for (row in rows) for (v in row) flat[k++] = v
            writer.startNode("data")
            // Always use raw base64. DoubleArrayConverter.arrayToString switches to
            // a precision-5 JSON form below 100 elements, which would truncate data.
            writer.setValue(Base64.getEncoder().encodeToString(DoubleArrayConverter.doubleArrayToByteArray(flat)))
            writer.endNode()
            writer.endNode()
        }

        private fun readMatrix(reader: com.thoughtworks.xstream.io.HierarchicalStreamReader): MutableList<MutableList<Double>> {
            var rows: Int? = null
            var cols: Int? = null
            var data: DoubleArray? = null
            val legacyRows = mutableListOf<MutableList<Double>>()
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
                        legacyRows.add(row)
                    }
                }
                reader.moveUp()
            }

            if (sawLegacy) return legacyRows
            val r = rows ?: 0
            val c = cols ?: 0
            val d = data
            if (d == null || r * c == 0) return mutableListOf()
            val out = ArrayList<MutableList<Double>>(r)
            var k = 0
            repeat(r) {
                val row = ArrayList<Double>(c)
                repeat(c) { row.add(d[k++]) }
                out.add(row)
            }
            return out
        }
    }
}
