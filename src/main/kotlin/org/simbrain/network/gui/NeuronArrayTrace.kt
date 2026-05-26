package org.simbrain.network.gui

import org.simbrain.network.core.ActivationSequence
import org.simbrain.network.core.Layer
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.ActivationSequenceNode
import org.simbrain.network.gui.nodes.NeuronArrayNode
import org.simbrain.network.gui.nodes.WeightMatrixNode
import java.awt.Color
import java.util.*

/**
 * Indices to highlight on a [NeuronArrayNode] during hover tracing.
 */
data class NeuronArrayTraceHighlight(val indices: Set<Int>, val color: Color)

/**
 * Highlight info set on an [ActivationSequenceNode] during hover tracing. Rows are sequence
 * positions, columns are features. Hovering a sequence cell traces by [rows] (position is preserved
 * across a connection); hovering a weight-matrix cell traces by [cols] (a feature is a column).
 */
data class SequenceTraceHighlight(
    val rows: Set<Int> = emptySet(),
    val cols: Set<Int> = emptySet(),
    val color: Color
)

/**
 * Highlight info set on a [WeightMatrixNode] during hover tracing.
 * - `(row=i, col=null)` highlights the full row `i`.
 * - `(row=null, col=j)` highlights the full column `j`.
 * - `(row=i, col=j)` highlights a single cell.
 * Row/col are in target/source space (target = row, source = col), regardless of display transpose.
 */
data class WeightMatrixTraceHighlight(val row: Int?, val col: Int?, val color: Color)

internal fun naTraceColor() = NetworkPreferences.backwardTraceColor.withAlpha(180)

private class NaTraceState {
    val tracedArrayNodes = mutableListOf<NeuronArrayNode>()
    val tracedMatrixNodes = mutableListOf<WeightMatrixNode>()
    val tracedSequenceNodes = mutableListOf<ActivationSequenceNode>()
}

private val naTraceStates = WeakHashMap<NetworkPanel, NaTraceState>()

private fun NetworkPanel.naTraceState() = naTraceStates.getOrPut(this) { NaTraceState() }

/**
 * Hover over neuron [index] in [array]: highlight that neuron and the matching row of every
 * incoming weight matrix (one step backward).
 */
fun NetworkPanel.updateNeuronArrayTrace(array: NeuronArray, index: Int) {
    clearNeuronArrayTrace()
    val state = naTraceState()
    val color = naTraceColor()

    val arrayNode = getNode(array) as? NeuronArrayNode
    if (arrayNode != null) {
        arrayNode.traceHighlight = NeuronArrayTraceHighlight(setOf(index), color)
        state.tracedArrayNodes.add(arrayNode)
        arrayNode.repaint()
    }

    for (conn in array.incomingConnectors) {
        if (conn !is WeightMatrix) continue
        val wmNode = getNode(conn) as? WeightMatrixNode ?: continue
        wmNode.traceHighlight = WeightMatrixTraceHighlight(row = index, col = null, color = color)
        state.tracedMatrixNodes.add(wmNode)
        wmNode.repaint()
    }
}

/**
 * Hover over sequence cell ([row] = position, [col] = feature) of [sequence]: full (transitive)
 * trace in both directions, focused on structure (rows). Position [row] is preserved across every
 * connection, so the trace highlights row [row] of [sequence] and of every sequence reachable
 * forward or backward through weight matrices. The hovered feature is specific only at the first hop:
 * the hovered sequence's immediate incoming matrices highlight row [col] and its outgoing matrices
 * highlight column [col] (the weights producing / fed by that feature). Deeper matrices are not
 * highlighted — past the first hop a whole row feeds the next and the entire matrix is involved.
 */
fun NetworkPanel.updateActivationSequenceTrace(sequence: ActivationSequence, row: Int, col: Int) {
    clearNeuronArrayTrace()
    val state = naTraceState()
    val color = naTraceColor()

    traceSequencePosition(sequence, row, color, state, hashSetOf())

    for (conn in sequence.incomingConnectors) {
        if (conn is WeightMatrix) highlightWeightMatrix(conn, WeightMatrixTraceHighlight(row = col, col = null, color = color), state)
    }
    for (conn in sequence.outgoingConnectors) {
        if (conn is WeightMatrix) highlightWeightMatrix(conn, WeightMatrixTraceHighlight(row = null, col = col, color = color), state)
    }
}

/**
 * Transitively highlight position [row] of every sequence reachable from [sequence] in either
 * direction (position is preserved across each connection). [visited] guards against cycles.
 */
private fun NetworkPanel.traceSequencePosition(
    sequence: ActivationSequence,
    row: Int,
    color: Color,
    state: NaTraceState,
    visited: MutableSet<ActivationSequence>
) {
    if (!visited.add(sequence)) return
    highlightSequenceRow(sequence, row, color, state)
    for (conn in sequence.incomingConnectors) {
        val src = (conn as? WeightMatrix)?.source as? ActivationSequence ?: continue
        traceSequencePosition(src, row, color, state, visited)
    }
    for (conn in sequence.outgoingConnectors) {
        val tgt = (conn as? WeightMatrix)?.target as? ActivationSequence ?: continue
        traceSequencePosition(tgt, row, color, state, visited)
    }
}

private fun NetworkPanel.highlightWeightMatrix(wm: WeightMatrix, highlight: WeightMatrixTraceHighlight, state: NaTraceState) {
    val wmNode = getNode(wm) as? WeightMatrixNode ?: return
    wmNode.traceHighlight = highlight
    state.tracedMatrixNodes.add(wmNode)
    wmNode.repaint()
}

/**
 * Hover over cell ([row], [col]) of [wm] (target row = [row], source col = [col]):
 * highlight that cell, plus target feature [row] and source feature [col] in the connected layers
 * (a neuron index for neuron arrays, a column for activation sequences).
 */
fun NetworkPanel.updateWeightMatrixCellTrace(wm: WeightMatrix, row: Int, col: Int) {
    clearNeuronArrayTrace()
    val state = naTraceState()
    val color = naTraceColor()

    val wmNode = getNode(wm) as? WeightMatrixNode
    if (wmNode != null) {
        wmNode.traceHighlight = WeightMatrixTraceHighlight(row = row, col = col, color = color)
        state.tracedMatrixNodes.add(wmNode)
        wmNode.repaint()
    }

    highlightLayerFeature(wm.target, row, color, state)
    highlightLayerFeature(wm.source, col, color, state)
}

/** Highlight feature [index] of [layer]: a neuron for neuron arrays, a column for activation sequences. */
private fun NetworkPanel.highlightLayerFeature(layer: Layer, index: Int, color: Color, state: NaTraceState) {
    if (index !in 0 until layer.size) return
    when (val node = getNode(layer)) {
        is NeuronArrayNode -> {
            node.traceHighlight = NeuronArrayTraceHighlight(setOf(index), color)
            state.tracedArrayNodes.add(node)
            node.repaint()
        }
        is ActivationSequenceNode -> {
            node.traceHighlight = SequenceTraceHighlight(cols = setOf(index), color = color)
            state.tracedSequenceNodes.add(node)
            node.repaint()
        }
    }
}

private fun NetworkPanel.highlightSequenceRow(sequence: ActivationSequence, row: Int, color: Color, state: NaTraceState) {
    val node = getNode(sequence) as? ActivationSequenceNode ?: return
    if (row !in 0 until sequence.sequenceSize) return
    node.traceHighlight = SequenceTraceHighlight(rows = setOf(row), color = color)
    state.tracedSequenceNodes.add(node)
    node.repaint()
}

fun NetworkPanel.clearNeuronArrayTrace() {
    val state = naTraceState()
    state.tracedArrayNodes.forEach { node ->
        node.traceHighlight = null
        node.repaint()
    }
    state.tracedArrayNodes.clear()
    state.tracedMatrixNodes.forEach { node ->
        node.traceHighlight = null
        node.repaint()
    }
    state.tracedMatrixNodes.clear()
    state.tracedSequenceNodes.forEach { node ->
        node.traceHighlight = null
        node.repaint()
    }
    state.tracedSequenceNodes.clear()
}

private fun Color.withAlpha(alpha: Int) = Color(red, green, blue, alpha)
