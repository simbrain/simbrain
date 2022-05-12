package org.simbrain.util

import org.pmw.tinylog.Logger
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * Kotlin wrapper for java PropertyChangeSupport functionality. Extend this class to create
 * events. Syntactic sugar to make it easy to create and invoke add, remove, and change events.
 *
 * This is the main doc for all classes using this event structures. Suggest reading this while inspecting a subclass
 * of this class.
 *
 * Events are organized into "fireX" functions to broadcast events and "onX" events
 * to handle them.  They are placed next to each other in subclasses so it is easy to track how event broadcasting
 * and event.
 *
 * Note that change events using "old" and "new" will only fire if old is different from new.
 *
 * Advantages of this design are: externally no need for strings, so all references can be autocompleted in the IDE.
 * Also, since the fireX and onX methods are (by convention) next to each other, it's easy
 * to get from the code where an event is fired in the code to where it is handled, and conversely.
 *
 * A live template that makes event creation easier is in `etc\event_shortcuts.zip` ("Import settings...")
 * Import this into intellij and then you can create these pairs of functions using these abbreviations, which
 * stand for "Simbrain event":
 *
 *  - `sevt0` (event with no argument)
 *  - `sevtn` (event to create something)
 *  - `sevto` (event to remove something)
 *  - `sevtc` (event to change something). Events are only fired when old and new are different.
 *  - `sevtar` (create both add & remove events).
 *
 *  They can be used just like other intellij built-in shortcuts, e.g `sout`.
 *
 *  @author Yulin Li
 */
open class Event(private val changeSupport: PropertyChangeSupport) {

    /**
     * Overload "operator" with an argument. Used for "firing" events with an argument
     */
    protected operator fun <T> String.invoke(old: T? = null, new: T? = null) {
        changeSupport.firePropertyChange(this@invoke, old, new)
        // changeSupport.firePropertyChange(this@invoke, old, new)
        Logger.debug("${this}Event")
    }

    /**
     * Overload operator with no argument. Used for "firing" events with no argument.
     */
    protected operator fun String.invoke() {
        changeSupport.firePropertyChange(this, null, null)
        Logger.debug("${this}Event")
    }

    /**
     * No-argument event handler.
     */
    protected fun String.event(handler: Runnable) {
        changeSupport.addPropertyChangeListener(this) {
            handler.run()
        }
    }

    /**
     * Handle a "new object" event, e.g. adding a neuron.
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T> String.itemAddedEvent(handler: Consumer<T>) {
        changeSupport.addPropertyChangeListener(this) {
            handler.accept(it.newValue as T)
        }
    }

    /**
     * Handle a "remove object" event, e.g. removing a neuron.
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T> String.itemRemovedEvent(handler: Consumer<T>) {
        changeSupport.addPropertyChangeListener(this) {
            handler.accept(it.oldValue as T)
        }
    }

    /**
     * Handle a change event.  If old and new states are the same no action is taken.
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T> String.itemChangedEvent(handler: BiConsumer<T, T>) {
        changeSupport.addPropertyChangeListener(this) {
            handler.accept(it.oldValue as T, it.newValue as T)
        }
    }

    /**
     * Handle a change event.  If old and new states are the same no action is taken.
     * Note: For Kotlin
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T> String.itemChangedEvent(handler: (T, T) -> Unit) {
        changeSupport.addPropertyChangeListener(this) {
            handler(it.oldValue as T, it.newValue as T)
        }
    }

}