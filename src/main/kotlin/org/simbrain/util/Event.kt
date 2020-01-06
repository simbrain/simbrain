package org.simbrain.util

import org.pmw.tinylog.Logger
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * Kotlin wrapper for java PropertyChangeSupport functionality. Extend this class to create
 * events. Syntactic sugar to make it easy to create and invoke add, remove, and change events.
 */
open class Event(private val changeSupport: PropertyChangeSupport) {

    /**
     * Overload "operator" with an argument. Used for "firing" events with an argument
     */
    protected operator fun <T> String.invoke(old: T? = null, new: T? = null) {
        changeSupport.firePropertyChange(this, old, new)
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

}