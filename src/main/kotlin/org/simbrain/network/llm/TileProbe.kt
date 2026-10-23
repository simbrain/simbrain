/**
 * Coupling endpoints for the interior tiles of a [GenerativeModel]. Tiles are view objects the
 * scene rebuilds freely, so plots and recordings couple to a [TileProbe] instead: a small saved
 * record naming the tile by id, pinned when it is made to the layer and head the tile showed then.
 * The model owns its probes and lists them as child attribute containers, which is how the network
 * component finds them for coupling menus and serialization.
 */
package org.simbrain.network.llm

import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producible

/**
 * Reads one interior tile at a pinned layer and head. Pinning at creation mirrors image world
 * filter pipelines: a plot made while layer 3 was shown keeps reading layer 3 after the view
 * flips to layer 7. [pinnedLayer] and [pinnedHead] are -1 for tiles without layers or heads.
 */
class TileProbe(
    val tileId: String,
    val pinnedLayer: Int,
    val pinnedHead: Int,
    val tileTitle: String,
) : AttributeContainer {

    /** The owning model; reattached whenever the model lists its probes, since it is not saved. */
    @Transient
    var host: GenerativeModel? = null

    private val pinSuffix: String
        get() = (if (pinnedLayer >= 0) "@L$pinnedLayer" else "") + (if (pinnedHead >= 0) "/H$pinnedHead" else "")

    override val id: String
        get() = "${host?.id}/$tileId$pinSuffix"

    override val attributeName: String
        get() {
            val pins = listOfNotNull(
                pinnedLayer.takeIf { it >= 0 }?.let { "layer $it" },
                pinnedHead.takeIf { it >= 0 }?.let { "head $it" },
            )
            val title = host?.tileTitle(this) ?: tileTitle
            return (listOf("${host?.displayName ?: ""} $title".trim()) + pins).joinToString()
        }

    /** The tile's value for the last processed token; empty until the model has an interior. */
    @get:Producible(description = "Tile values")
    val values: DoubleArray
        get() = host?.readProbe(this) ?: DoubleArray(0)

    /**
     * What the logit lens predicts from this tile's residual checkpoint for the last processed
     * token, for labeling plotted points. Null with the lens off, or for tiles the lens doesn't read.
     */
    @get:Producible(description = "Logit lens token")
    val lensToken: String?
        get() = host?.readLensToken(this)
}

/** The hand edits a weight tile's menu and keys apply to the matrix the tile shows. */
enum class WeightEdit { CLEAR, RANDOMIZE, INCREMENT, DECREMENT }
