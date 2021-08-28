package org.simbrain.network.events

import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * @see Event
 */
class TrainerEvents(val trainer: Any) : Event(PropertyChangeSupport(trainer)) {

    fun onBeginTraining(handler: Runnable) = "BeginTraining".event(handler)
    fun fireBeginTraining() = "BeginTraining"()

    fun onEndTraining(handler: Runnable) = "EndTraining".event(handler)
    fun fireEndTraining() = "EndTraining"()

    // TODO: Reconsider these choices after using things more

    fun onErrorUpdated(handler: Runnable) = "ErrorUpdated".event(handler)
    fun fireErrorUpdated() = "ErrorUpdated"()

    fun onErrorUpdated(handler: Consumer<Double>) = "ErrorUpdated".itemAddedEvent(handler)
    fun fireErrorUpdated(error: Double) = "ErrorUpdated"(new =error)

    fun onProgressUpdated(handler: Consumer<Pair<String, Int>>) = "ProgressUpdated".itemAddedEvent(handler)
    fun fireProgressUpdated(message:String, percent:Int) = "ProgressUpdated"(new = message to percent)

}