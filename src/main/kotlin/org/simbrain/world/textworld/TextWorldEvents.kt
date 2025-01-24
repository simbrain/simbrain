package org.simbrain.world.textworld

import org.simbrain.util.Events
import org.simbrain.util.TokenizerResult

/**
 * See [Events].
 */
class TextWorldEvents: Events() {

    val textChanged = NoArgEvent()
    val tokenVectorMapChanged = NoArgEvent()
    val currentTokenChanged = OneArgEvent<TokenizerResult>()
    val cursorPositionChanged = NoArgEvent()
    val atEnd = NoArgEvent()
    val preferencesChanged = NoArgEvent()

}