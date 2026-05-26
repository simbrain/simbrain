package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.simbrain.network.core.ActivationSequence
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.ActivationSequenceNode
import org.simbrain.network.gui.nodes.NeuronArrayNode
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.StandardDialog
import org.simbrain.util.UserParameter
import org.simbrain.util.createEditorDialog
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D

/**
 * Helpers for the per-pixel quick-edit feature on neuron arrays and weight matrices.
 *
 * Pixel selections live on the visual nodes ([NeuronArrayNode.pixelSelection],
 * [WeightMatrixNode.pixelSelection]); these utilities aggregate them across the panel and
 * route increment / decrement / clear / randomize requests to the underlying model.
 */

/**
 * Shared cell-hit logic for the nodes' `collectCellsInGlobalEllipse`. Converts [ellipse] (global
 * coordinates) into this node's local frame, then walks a [rows] x [cols] grid of [cellW] x [cellH]
 * cells, applying [map] to each cell whose rect intersects the brush. `null` results are skipped.
 */
inline fun <T> PNode.cellsIntersectingGlobalEllipse(
    ellipse: Ellipse2D,
    rows: Int,
    cols: Int,
    cellW: Double,
    cellH: Double,
    map: (row: Int, col: Int) -> T?
): List<T> {
    val gb = ellipse.bounds2D
    val local = Rectangle2D.Double(gb.x, gb.y, gb.width, gb.height)
    globalToLocal(local)
    val result = mutableListOf<T>()
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            if (local.intersects(col * cellW, row * cellH, cellW, cellH)) {
                map(row, col)?.let { result += it }
            }
        }
    }
    return result
}

/**
 * Iterate every pixel target — array or matrix — whose visual cell intersects [ellipse] in
 * global coordinates. Used by the wand handler so wand actions can apply to per-pixel values.
 */
fun NetworkPanel.pixelsInGlobalEllipse(ellipse: java.awt.geom.Ellipse2D): Sequence<PixelTarget> = sequence {
    filterScreenElements<NeuronArrayNode>().forEach { node ->
        node.collectCellsInGlobalEllipse(ellipse).forEach { idx ->
            yield(PixelTarget.ArrayPixel(node.neuronArray, idx))
        }
    }
    filterScreenElements<WeightMatrixNode>().forEach { node ->
        val wm = node.weightMatrix as? WeightMatrix ?: return@forEach
        node.collectCellsInGlobalEllipse(ellipse).forEach { (r, c) ->
            yield(PixelTarget.MatrixPixel(wm, r, c))
        }
    }
    filterScreenElements<ActivationSequenceNode>().forEach { node ->
        node.collectCellsInGlobalEllipse(ellipse).forEach { (r, c) ->
            yield(PixelTarget.SequencePixel(node.activationSequence, r, c))
        }
    }
}

/** True if any visible neuron-array, weight-matrix, or activation-sequence node has a non-empty pixel selection. */
fun NetworkPanel.hasAnyPixelSelection(): Boolean =
    filterScreenElements<NeuronArrayNode>().any { it.pixelSelection.isNotEmpty() } ||
    filterScreenElements<WeightMatrixNode>().any { it.pixelSelection.isNotEmpty() } ||
    filterScreenElements<ActivationSequenceNode>().any { it.pixelSelection.isNotEmpty() }

/** Drop all pixel selections (e.g. on Escape). */
fun NetworkPanel.clearAllPixelSelections() {
    filterScreenElements<NeuronArrayNode>().forEach {
        if (it.pixelSelection.isNotEmpty()) it.pixelSelection = emptySet()
    }
    filterScreenElements<WeightMatrixNode>().forEach {
        if (it.pixelSelection.isNotEmpty()) it.pixelSelection = emptySet()
    }
    filterScreenElements<ActivationSequenceNode>().forEach {
        if (it.pixelSelection.isNotEmpty()) it.pixelSelection = emptySet()
    }
}

/**
 * Wire pixel selection to the component selection: whenever a [NeuronArrayNode] or
 * [WeightMatrixNode] is removed from the panel selection, drop its pixel selection too.
 * Establishes the invariant that pixel selection only exists for currently-selected components,
 * which keeps the conditionally-enabled randomize action's affordance consistent.
 */
fun NetworkPanel.bindPixelSelectionToComponentSelection() {
    selectionManager.events.selection.on { old, new ->
        (old - new).forEach { el ->
            when (el) {
                is NeuronArrayNode -> if (el.pixelSelection.isNotEmpty()) el.pixelSelection = emptySet()
                is WeightMatrixNode -> if (el.pixelSelection.isNotEmpty()) el.pixelSelection = emptySet()
                is ActivationSequenceNode -> if (el.pixelSelection.isNotEmpty()) el.pixelSelection = emptySet()
            }
        }
    }
}

/** Apply [op] to every selected pixel and fire [updated] once per affected component. */
private fun NetworkPanel.forEachPixel(
    onNeuron: (NeuronArray, Int) -> Unit,
    onWeight: (WeightMatrix, Int, Int) -> Unit,
    onSequence: (ActivationSequence, Int, Int) -> Unit,
) {
    filterScreenElements<NeuronArrayNode>().forEach { node ->
        val sel = node.pixelSelection
        if (sel.isEmpty()) return@forEach
        sel.forEach { idx -> onNeuron(node.neuronArray, idx) }
        node.neuronArray.events.updated.fire()
    }
    filterScreenElements<WeightMatrixNode>().forEach { node ->
        val sel = node.pixelSelection
        if (sel.isEmpty()) return@forEach
        val wm = node.weightMatrix as? WeightMatrix ?: return@forEach
        sel.forEach { (r, c) -> onWeight(wm, r, c) }
        wm.events.updated.fire()
    }
    filterScreenElements<ActivationSequenceNode>().forEach { node ->
        val sel = node.pixelSelection
        if (sel.isEmpty()) return@forEach
        sel.forEach { (r, c) -> onSequence(node.activationSequence, r, c) }
        node.activationSequence.events.updated.fire()
    }
}

fun NetworkPanel.incrementSelectedPixels() = forEachPixel(
    onNeuron = { array, idx -> array.activations[idx, 0] = array.activations[idx, 0] + array.increment },
    onWeight = { wm, r, c -> wm.weights[r, c] = wm.weights[r, c] + wm.increment },
    onSequence = { seq, r, c -> seq.activations[r, c] = seq.activations[r, c] + seq.increment },
)

fun NetworkPanel.decrementSelectedPixels() = forEachPixel(
    onNeuron = { array, idx -> array.activations[idx, 0] = array.activations[idx, 0] - array.increment },
    onWeight = { wm, r, c -> wm.weights[r, c] = wm.weights[r, c] - wm.increment },
    onSequence = { seq, r, c -> seq.activations[r, c] = seq.activations[r, c] - seq.increment },
)

fun NetworkPanel.clearSelectedPixels() = forEachPixel(
    onNeuron = { array, idx -> array.activations[idx, 0] = 0.0 },
    onWeight = { wm, r, c -> wm.weights[r, c] = 0.0 },
    onSequence = { seq, r, c -> seq.activations[r, c] = 0.0 },
)

fun NetworkPanel.randomizeSelectedPixels() = forEachPixel(
    onNeuron = { array, idx -> array.activations[idx, 0] = NetworkPreferences.activationRandomizer.sampleDouble() },
    onWeight = { wm, r, c -> wm.weights[r, c] = NetworkPreferences.weightRandomizer.sampleDouble() },
    onSequence = { seq, r, c -> seq.activations[r, c] = NetworkPreferences.activationRandomizer.sampleDouble() },
)

/**
 * Addressable pixel inside a [NeuronArray] or [WeightMatrix]. Used by both the pixel batch-edit
 * dialog flow and by wand-mode actions that operate on per-cell values.
 */
sealed class PixelTarget {

    /** Current numeric value of the pixel. */
    abstract fun read(): Double

    /** Overwrite the pixel's numeric value. Does not fire `events.updated` — call [fireUpdated] when done. */
    abstract fun write(value: Double)

    /** Lower/upper bounds for the pixel — used by wand amount calculations and clamping. */
    abstract fun bounds(): Pair<Double, Double>

    /** Fire the underlying component's `events.updated` so the view repaints. */
    abstract fun fireUpdated()

    data class ArrayPixel(val array: NeuronArray, val idx: Int) : PixelTarget() {
        override fun read(): Double = array.activations[idx, 0]
        override fun write(value: Double) { array.activations[idx, 0] = value }
        override fun bounds(): Pair<Double, Double> = array.updateRule.graphicalBounds.let { it.start to it.endInclusive }
        override fun fireUpdated() { array.events.updated.fire() }
    }

    data class MatrixPixel(val wm: WeightMatrix, val row: Int, val col: Int) : PixelTarget() {
        override fun read(): Double = wm.weights[row, col]
        override fun write(value: Double) { wm.weights[row, col] = value }
        override fun bounds(): Pair<Double, Double> = -1.0 to 1.0
        override fun fireUpdated() { wm.events.updated.fire() }
    }

    data class SequencePixel(val seq: ActivationSequence, val row: Int, val col: Int) : PixelTarget() {
        override fun read(): Double = seq.activations[row, col]
        override fun write(value: Double) { seq.activations[row, col] = value }
        override fun bounds(): Pair<Double, Double> = seq.updateRule.graphicalBounds.let { it.start to it.endInclusive }
        override fun fireUpdated() { seq.events.updated.fire() }
    }
}

/**
 * Single-field wrapper used by the pixel batch-edit dialog. One wrapper per selected pixel; the
 * [AnnotatedPropertyEditor]'s multi-object consistency check shows the common value (or "...") and
 * commits a single user-entered value back to every wrapper.
 */
class PixelValueWrapper(initValue: Double = 0.0) : EditableObject {
    @UserParameter(label = "Value", description = "Value to assign to each selected pixel", order = 1)
    var value: Double = initValue
    override val name = "Pixel"
}

/**
 * Build a batch-edit dialog over every pixel currently selected on any visible neuron array,
 * weight matrix, or activation sequence. Returns null if nothing is selected.
 */
fun NetworkPanel.createPixelEditDialog(): StandardDialog? {
    data class ArrayEntry(val array: NeuronArray, val idx: Int, val wrapper: PixelValueWrapper)
    data class MatrixEntry(val wm: WeightMatrix, val row: Int, val col: Int, val wrapper: PixelValueWrapper)
    data class SequenceEntry(val seq: ActivationSequence, val row: Int, val col: Int, val wrapper: PixelValueWrapper)

    val arrayEntries = mutableListOf<ArrayEntry>()
    val matrixEntries = mutableListOf<MatrixEntry>()
    val sequenceEntries = mutableListOf<SequenceEntry>()

    filterScreenElements<NeuronArrayNode>().forEach { node ->
        node.pixelSelection.sorted().forEach { idx ->
            arrayEntries += ArrayEntry(node.neuronArray, idx, PixelValueWrapper(node.neuronArray.activations[idx, 0]))
        }
    }
    filterScreenElements<WeightMatrixNode>().forEach { node ->
        val wm = node.weightMatrix as? WeightMatrix ?: return@forEach
        node.pixelSelection.forEach { (r, c) ->
            matrixEntries += MatrixEntry(wm, r, c, PixelValueWrapper(wm.weights[r, c]))
        }
    }
    filterScreenElements<ActivationSequenceNode>().forEach { node ->
        node.pixelSelection.forEach { (r, c) ->
            sequenceEntries += SequenceEntry(node.activationSequence, r, c, PixelValueWrapper(node.activationSequence.activations[r, c]))
        }
    }

    val wrappers: List<PixelValueWrapper> =
        arrayEntries.map { it.wrapper } + matrixEntries.map { it.wrapper } + sequenceEntries.map { it.wrapper }
    if (wrappers.isEmpty()) return null

    val title = "Edit ${wrappers.size} pixel${if (wrappers.size != 1) "s" else ""}"
    return wrappers.createEditorDialog(titleName = title) {
        arrayEntries.forEach { it.array.activations[it.idx, 0] = it.wrapper.value }
        matrixEntries.forEach { it.wm.weights[it.row, it.col] = it.wrapper.value }
        sequenceEntries.forEach { it.seq.activations[it.row, it.col] = it.wrapper.value }
        arrayEntries.map { it.array }.distinct().forEach { it.events.updated.fire() }
        matrixEntries.map { it.wm }.distinct().forEach { it.events.updated.fire() }
        sequenceEntries.map { it.seq }.distinct().forEach { it.events.updated.fire() }
    }
}
