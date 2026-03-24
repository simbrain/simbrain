package org.simbrain.network.gui

import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.PoolingConnector
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.TensorConnectorNode
import org.simbrain.network.gui.nodes.TensorNode
import java.awt.Color
import java.lang.Math.floorDiv

/**
 * A box drawn on a [TensorNode] to visualize the receptive field during hover tracing.
 *
 * Coordinates are in tensor space (not pixel space).
 */
data class TraceBox(
    val row: Int,
    val col: Int,
    val height: Int,
    val width: Int,
    val color: Color
)

enum class HighlightMode { CELL, ROW, COLUMN }

/**
 * Highlight info set on a [TensorConnectorNode] during hover tracing.
 * In single-kernel view, navigates to (filter, inputChannel).
 * In grid view, highlights a cell, row (all channels for a filter), or column (all filters for a channel).
 */
data class ConnectorTraceHighlight(
    val filter: Int,
    val inputChannel: Int,
    val color: Color,
    val mode: HighlightMode = HighlightMode.CELL
)

private fun traceColor() = NetworkPreferences.receptiveFieldTraceColor.withAlpha(180)

private data class SavedConnector(val connector: ConvolutionConnector, val filter: Int, val inputChannel: Int)

/**
 * Per-panel trace state. Stored in a WeakHashMap so it doesn't leak if a panel is disposed.
 */
private class TraceState {
    val savedChannels = mutableListOf<Pair<TensorLayer, Int>>()
    val savedConnectorState = mutableListOf<SavedConnector>()
    val tracedTensorNodes = mutableListOf<TensorNode>()
    val tracedConnectorNodes = mutableListOf<TensorConnectorNode>()
}

private val traceStates = java.util.WeakHashMap<NetworkPanel, TraceState>()

private fun NetworkPanel.traceState() = traceStates.getOrPut(this) { TraceState() }

internal fun centeredTraceIndex(sourceIndex: Int, pad: Int, stride: Int, kernelSize: Int, targetSize: Int): Int {
    val kernelCenter = kernelSize / 2
    return floorDiv(sourceIndex + pad - kernelCenter, stride).coerceIn(0, targetSize - 1)
}

/**
 * Computes and distributes a bidirectional receptive field trace from a hover
 * at position ([hoverH], [hoverW]) on [sourceTensor].
 */
fun NetworkPanel.updateReceptiveFieldTrace(sourceTensor: TensorLayer, hoverH: Int, hoverW: Int) {
    clearReceptiveFieldTrace()

    traceForward(sourceTensor, hoverH, hoverW)
    traceBackward(sourceTensor, hoverH, hoverW)
}

/**
 * Clears all trace boxes and connector highlights, reverting temporary channel overrides.
 */
fun NetworkPanel.clearReceptiveFieldTrace() {
    val state = traceState()

    state.savedChannels.forEach { (layer, saved) ->
        layer.currentChannel = saved
    }
    state.savedChannels.clear()

    state.savedConnectorState.forEach { (conn, savedF, savedC) ->
        conn.currentFilter = savedF
        conn.currentInputChannel = savedC
    }
    state.savedConnectorState.clear()

    state.tracedConnectorNodes.forEach { node ->
        node.renderKernelImage()
        node.updateDetailLabel()
    }

    state.tracedTensorNodes.forEach { node ->
        node.traceBoxes.clear()
        node.repaint()
    }
    state.tracedTensorNodes.clear()

    state.tracedConnectorNodes.forEach { node ->
        node.traceHighlight = null
        node.repaint()
    }
    state.tracedConnectorNodes.clear()
}

private fun NetworkPanel.traceForward(layer: TensorLayer, h: Int, w: Int) {
    val state = traceState()

    for (conn in layer.outgoingTensorConnectors) {
        val (outH, outW, kernelH, kernelW, boxRow, boxCol) = when (conn) {
            is ConvolutionConnector -> {
                val clampedOutH = centeredTraceIndex(h, conn.padH, conn.stride, conn.kernelSize, conn.target.shape.height)
                val clampedOutW = centeredTraceIndex(w, conn.padW, conn.stride, conn.kernelSize, conn.target.shape.width)
                val boxRow = clampedOutH * conn.stride - conn.padH
                val boxCol = clampedOutW * conn.stride - conn.padW
                ForwardResult(clampedOutH, clampedOutW, conn.kernelSize, conn.kernelSize, boxRow, boxCol)
            }
            is PoolingConnector -> {
                val outH = h / conn.stride
                val outW = w / conn.stride
                val clampedOutH = outH.coerceIn(0, conn.target.shape.height - 1)
                val clampedOutW = outW.coerceIn(0, conn.target.shape.width - 1)
                val boxRow = clampedOutH * conn.stride
                val boxCol = clampedOutW * conn.stride
                ForwardResult(clampedOutH, clampedOutW, conn.poolSize, conn.poolSize, boxRow, boxCol)
            }
            else -> continue
        }

        val color = traceColor()

        val sourceNode = getNode(layer) as? TensorNode ?: continue
        sourceNode.traceBoxes.add(TraceBox(boxRow, boxCol, kernelH, kernelW, color))
        if (sourceNode !in state.tracedTensorNodes) state.tracedTensorNodes.add(sourceNode)
        sourceNode.repaint()

        val connNode = getNode(conn) as? TensorConnectorNode
        if (connNode != null && conn is ConvolutionConnector) {
            state.savedConnectorState.add(SavedConnector(conn, conn.currentFilter, conn.currentInputChannel))

            val displayedChannel = sourceNode.tensorLayer.currentChannel
            conn.currentInputChannel = displayedChannel

            connNode.traceHighlight = ConnectorTraceHighlight(conn.currentFilter, displayedChannel, color, HighlightMode.COLUMN)
            connNode.renderKernelImage()
            connNode.updateDetailLabel()
            state.tracedConnectorNodes.add(connNode)
            connNode.repaint()
        }

        if (conn is ConvolutionConnector && !conn.target.rgbComposite) {
            state.savedChannels.add(conn.target to conn.target.currentChannel)
            conn.target.currentChannel = conn.currentFilter
        }

        traceForward(conn.target, outH, outW)
    }
}

private fun NetworkPanel.traceBackward(layer: TensorLayer, h: Int, w: Int) {
    val state = traceState()

    for (conn in layer.incomingTensorConnectors) {
        val (srcRow, srcCol, boxH, boxW) = when (conn) {
            is ConvolutionConnector -> {
                val srcRow = h * conn.stride - conn.padH
                val srcCol = w * conn.stride - conn.padW
                BackwardResult(srcRow, srcCol, conn.kernelSize, conn.kernelSize)
            }
            is PoolingConnector -> {
                val srcRow = h * conn.stride
                val srcCol = w * conn.stride
                BackwardResult(srcRow, srcCol, conn.poolSize, conn.poolSize)
            }
            else -> continue
        }

        val color = traceColor()

        val sourceNode = getNode(conn.source) as? TensorNode ?: continue
        sourceNode.traceBoxes.add(TraceBox(srcRow, srcCol, boxH, boxW, color))
        if (sourceNode !in state.tracedTensorNodes) state.tracedTensorNodes.add(sourceNode)
        sourceNode.repaint()

        val connNode = getNode(conn) as? TensorConnectorNode
        if (connNode != null && conn is ConvolutionConnector) {
            state.savedConnectorState.add(SavedConnector(conn, conn.currentFilter, conn.currentInputChannel))

            val displayedChannel = layer.currentChannel
            conn.currentFilter = displayedChannel

            connNode.traceHighlight = ConnectorTraceHighlight(displayedChannel, conn.currentInputChannel, color, HighlightMode.ROW)
            connNode.renderKernelImage()
            connNode.updateDetailLabel()
            state.tracedConnectorNodes.add(connNode)
            connNode.repaint()
        }

        if (conn is PoolingConnector && !conn.source.rgbComposite) {
            state.savedChannels.add(conn.source to conn.source.currentChannel)
            conn.source.currentChannel = layer.currentChannel
        }

        val centerH = (srcRow + boxH / 2).coerceIn(0, conn.source.shape.height - 1)
        val centerW = (srcCol + boxW / 2).coerceIn(0, conn.source.shape.width - 1)

        traceBackward(conn.source, centerH, centerW)
    }
}

private data class ForwardResult(val outH: Int, val outW: Int, val kernelH: Int, val kernelW: Int, val boxRow: Int, val boxCol: Int)
private data class BackwardResult(val srcRow: Int, val srcCol: Int, val boxH: Int, val boxW: Int)

private fun Color.withAlpha(alpha: Int) = Color(red, green, blue, alpha)
