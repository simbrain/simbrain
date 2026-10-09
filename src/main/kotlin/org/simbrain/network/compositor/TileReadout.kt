/**
 * Reads the value an interior tile currently carries for one pinned layer and head, as a flat
 * vector: the data behind tile plots and recordings. It reads the model's tensors, not the tile's
 * display buffer, so a pinned readout keeps reading its own layer and head whatever the view flips
 * to.
 */
package org.simbrain.network.compositor

import org.simbrain.network.tensor.FloatTensor

/** Whether [readCurrent] has a value to give: everything but weights and gradients. */
val TensorTile.isReadable: Boolean
    get() = kind != TileKind.WEIGHT && kind != TileKind.GRADIENT

/** The layer a readout made now pins: the shown stack layer, else the tile's own [TensorTile.modelLayer]. */
val TensorTile.pinnableLayer: Int
    get() = (this as? LayerStacked)?.shownLayer?.takeIf { it >= 0 } ?: modelLayer

/** The head a readout made now pins: the shown head or deck slice, or -1 for a tile without heads. */
val TensorTile.pinnableHead: Int
    get() = when (this) {
        is AttentionTile -> selectedHead
        is DeckTile -> selectedSlice
        else -> -1
    }

/**
 * The tile's current value at [layer] and [head]: a vector for the token the model processed
 * last, which sits at [currentRow] in tiles laid out by sequence position. The length is fixed
 * for a given tile, so coupled plots keep their shape as tokens arrive. Null when the tile has no
 * data for that layer (a conv-only tile asked for an attention layer) or is not [isReadable].
 */
fun TensorTile.readCurrent(layer: Int, head: Int, currentRow: Int): DoubleArray? {
    if (!isReadable) return null
    return when (this) {
        is VectorHistoryTile -> ports.atLayer(stackLayers, layer)?.tensor?.let { it.rowValues(0, it.size) }
        is AttentionTile -> ports.atLayer(stackLayers, layer)?.tensor?.let {
            it.rowValues(head.coerceIn(0, numHeads - 1) * it.cols, cols)
        }
        is DeckTile -> tensors.atLayer(stackLayers, layer)?.let {
            val slice = head.coerceIn(0, slices - 1)
            val row = currentRow.coerceIn(0, rows - 1)
            if (columnSlices) it.rowValues(row * it.cols + slice * cols, cols)
            else it.rowValues((slice * rows + row) * it.cols, cols)
        }
        is MatrixTile -> tensors.atLayer(stackLayers, layer)?.let {
            if (tracksLiveRow) it.rowValues(currentRow.coerceIn(0, it.rows - 1) * it.cols, it.cols)
            else it.rowValues(0, it.size)
        }
        else -> null
    }
}

/**
 * The tensor behind the tile at [layer], for tiles that show one whole tensor per layer (the
 * residual checkpoints the logit lens reads, among others); null for head-sliced tiles.
 */
fun TensorTile.sourceTensor(layer: Int): FloatTensor? = when (this) {
    is VectorHistoryTile -> ports.atLayer(stackLayers, layer)?.tensor
    is MatrixTile -> tensors.atLayer(stackLayers, layer)
    else -> null
}

private fun <T> List<T>.atLayer(stackLayers: List<Int>, layer: Int): T? =
    if (stackLayers.isEmpty()) firstOrNull()
    else stackLayers.indexOf(layer).takeIf { it >= 0 }?.let(::get)

private fun FloatTensor.rowValues(offset: Int, length: Int) =
    DoubleArray(length) { data.get(offset + it).toDouble() }
