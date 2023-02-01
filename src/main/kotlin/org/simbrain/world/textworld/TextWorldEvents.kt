package org.simbrain.world.textworld

import org.simbrain.util.Events2

/**
 * See [Events2].
 */
class TextWorldEvents2: Events2() {

    val textChanged = NoArgEvent()
    val tokenVectorMapChanged = NoArgEvent()
    val currentTokenChanged = AddedEvent<TextWorld.TextItem?>()
    val cursorPositionChanged = NoArgEvent()

}