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
private fun backwardTraceColor() = NetworkPreferences.backwardTraceColor.withAlpha(180)

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
 * Computes a receptive field trace from a hover at ([hoverH], [hoverW]) on [sourceTensor].
 * Forward trace shows the immediate outgoing kernel footprint (one layer).
 * Backward trace expands the full receptive field through all preceding layers.
 */
fun NetworkPanel.updateReceptiveFieldTrace(sourceTensor: TensorLayer, hoverH: Int, hoverW: Int) {
    clearReceptiveFieldTrace()

    val state = traceState()

    // Draw origin indicator on the hovered cell
    val originNode = getNode(sourceTensor) as? TensorNode
    if (originNode != null) {
        originNode.traceBoxes.add(TraceBox(hoverH, hoverW, 1, 1, backwardTraceColor()))
        if (originNode !in state.tracedTensorNodes) state.tracedTensorNodes.add(originNode)
        originNode.repaint()
    }

    traceForward(sourceTensor, hoverH, hoverW)
    traceBackward(sourceTensor, hoverH, hoverW, 1, 1)
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

    state.tracedTensorNodes.forEach { node ->
        node.traceBoxes.clear()
        node.repaint()
    }
    state.tracedTensorNodes.clear()

    state.tracedConnectorNodes.forEach { node ->
        node.traceHighlight = null
        node.renderKernelImage()
        node.updateDetailLabel()
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

        // Show which target cell this maps to (1x1 indicator, no further recursion)
        val targetNode = getNode(conn.target) as? TensorNode
        if (targetNode != null) {
            targetNode.traceBoxes.add(TraceBox(outH, outW, 1, 1, color))
            if (targetNode !in state.tracedTensorNodes) state.tracedTensorNodes.add(targetNode)
            targetNode.repaint()
        }
    }
}

private fun NetworkPanel.traceBackward(
    layer: TensorLayer, row: Int, col: Int, height: Int, width: Int,
    visited: MutableSet<TensorLayer> = mutableSetOf()
) {
    if (!visited.add(layer)) return
    val state = traceState()

    for (conn in layer.incomingTensorConnectors) {
        val (srcRow, srcCol, boxH, boxW) = when (conn) {
            is ConvolutionConnector -> {
                val srcRow = row * conn.stride - conn.padH
                val srcCol = col * conn.stride - conn.padW
                val srcH = (height - 1) * conn.stride + conn.kernelSize
                val srcW = (width - 1) * conn.stride + conn.kernelSize
                BackwardResult(srcRow, srcCol, srcH, srcW)
            }
            is PoolingConnector -> {
                val srcRow = row * conn.stride
                val srcCol = col * conn.stride
                val srcH = (height - 1) * conn.stride + conn.poolSize
                val srcW = (width - 1) * conn.stride + conn.poolSize
                BackwardResult(srcRow, srcCol, srcH, srcW)
            }
            else -> continue
        }

        val color = backwardTraceColor()

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

        traceBackward(conn.source, srcRow, srcCol, boxH, boxW, visited)
    }
}

private data class ForwardResult(val outH: Int, val outW: Int, val kernelH: Int, val kernelW: Int, val boxRow: Int, val boxCol: Int)
private data class BackwardResult(val srcRow: Int, val srcCol: Int, val boxH: Int, val boxW: Int)

private fun Color.withAlpha(alpha: Int) = Color(red, green, blue, alpha)
