package org.simbrain.world.textworld

import org.simbrain.util.Events

/**
 * See [Events].
 */
class TextWorldEvents: Events() {

    val textChanged = NoArgEvent()
    val tokenVectorMapChanged = NoArgEvent()
    val currentTokenChanged = OneArgEvent<TextWorld.TextItem?>()
    val cursorPositionChanged = NoArgEvent()
    val atEnd = NoArgEvent()

}