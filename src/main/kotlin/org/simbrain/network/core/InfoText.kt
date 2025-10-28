package org.simbrain.network.core

/**
 * Use this to display info about the state of a network model.
 */
class InfoText(text: String) : NetworkTextObject(text) {

    /**
     * Configuration for InfoText positioning relative to subnetwork
     */
    enum class Position {
        BELOW_INTERACTION_BOX,
        ABOVE_INTERACTION_BOX,
        BELOW_OUTLINE;
        
        override fun toString(): String {
            return name.lowercase().split('_').joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }

    var position: Position = Position.BELOW_OUTLINE

    var spacing: Double = 10.0

    override suspend fun delete(): List<NetworkTextObject> {
        // prevent the default delete behavior
        // fire event directly if we want to delete
        return emptyList()
    }
}
