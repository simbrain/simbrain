package org.simbrain.world.textworld

import org.simbrain.util.FlowEvents
import org.simbrain.util.TokenizerResult

/**
 * See [FlowEvents].
 */
class TextWorldEvents: FlowEvents() {

    val textChanged = NoArgAwaitableEvent()
    val tokenVectorMapChanged = NoArgEvent()
    val currentTokenChanged = AwaitableEvent<TokenizerResult>()
    val cursorPositionChanged = NoArgAwaitableEvent()
    val atEnd = NoArgAwaitableEvent()
    val preferencesChanged = NoArgEvent()
    val statusChanged = NoArgEvent()

}
