package org.simbrain.util

import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

open class Event(private val changeSupport: PropertyChangeSupport) {

    protected operator fun <T> String.invoke(old: T? = null, new: T? = null) {
        changeSupport.firePropertyChange(this, old, new)
    }

    protected operator fun String.invoke() {
        changeSupport.firePropertyChange(this, null, null)
    }

    protected fun String.event(handler: Runnable) {
        changeSupport.addPropertyChangeListener(this) {
            handler.run()
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T> String.itemAddedEvent(handler: Consumer<T>) {
        changeSupport.addPropertyChangeListener(this) {
            handler.accept(it.newValue as T)
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T> String.itemRemovedEvent(handler: Consumer<T>) {
        changeSupport.addPropertyChangeListener(this) {
            handler.accept(it.oldValue as T)
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T> String.itemChangedEvent(handler: BiConsumer<T, T>) {
        changeSupport.addPropertyChangeListener(this) {
            handler.accept(it.oldValue as T, it.newValue as T)
        }
    }

}