package org.simbrain.network.gui

import org.simbrain.network.core.Layer
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.NeuronArrayNode
import org.simbrain.network.gui.nodes.WeightMatrixNode
import java.awt.Color
import java.util.*

/**
 * Indices to highlight on a [NeuronArrayNode] during hover tracing.
 */
data class NeuronArrayTraceHighlight(val indices: Set<Int>, val color: Color)

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
 * Hover over cell ([row], [col]) of [wm] (target row = [row], source col = [col]):
 * highlight that cell, plus target neuron [row] and source neuron [col].
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

    highlightNeuron(wm.target, row, color, state)
    highlightNeuron(wm.source, col, color, state)
}

private fun NetworkPanel.highlightNeuron(layer: Layer, index: Int, color: Color, state: NaTraceState) {
    val node = getNode(layer) as? NeuronArrayNode ?: return
    if (index !in 0 until layer.size) return
    node.traceHighlight = NeuronArrayTraceHighlight(setOf(index), color)
    state.tracedArrayNodes.add(node)
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
}

private fun Color.withAlpha(alpha: Int) = Color(red, green, blue, alpha)
