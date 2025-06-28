package org.simbrain.network.core

/**
 * Use this to display info about the state of a network model.
 */
class InfoText(text: String) : NetworkTextObject(text) {

    override suspend fun delete(): List<NetworkTextObject> {
        // prevent the default delete behavior
        // fire event directly if we want to delete
        return emptyList()
    }
}
