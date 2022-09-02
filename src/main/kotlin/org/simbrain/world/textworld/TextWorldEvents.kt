package org.simbrain.world.textworld

import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * See [Event].
 */
class TextWorldEvents(val world: TextWorld): Event(PropertyChangeSupport(world)) {

    fun onTextChanged(handler: Runnable) = "TextChanged".event(handler)
    fun fireTextChanged() = "TextChanged"()

    fun onTokenVectorMapChanged(handler: Runnable) = "TokenVectorMapChanged".event(handler)
    fun fireTokenVectorMapChanged() = "TokenVectorMapChanged"()

    fun onCurrentTokenChanged(handler: Consumer<TextWorld.TextItem?>) = "CurrentTokenChanged".itemAddedEvent(handler)
    fun fireCurrentTokenChanged(token: TextWorld.TextItem?) = "CurrentTokenChanged"(new = token)

    fun onCursorPositionChanged(handler: Runnable) = "CursorPositionChanged".event(handler)
    fun fireCursorPositionChanged() = "CursorPositionChanged"()
}