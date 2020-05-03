package org.simbrain.network.gui

import org.simbrain.network.gui.nodes.ScreenElement
import java.util.concurrent.CopyOnWriteArraySet

class NetworkSelectionModel(val networkPanel: NetworkPanel) {

    val selection: Set<ScreenElement> = CopyOnWriteArraySet()

    val isEmpty get() = selection.isEmpty()

    private val mutableSelection get() = selection as CopyOnWriteArraySet

    fun clear() {
        mutableSelection.clear()
    }

    fun add(screenElement: ScreenElement) {
        mutableSelection.add(screenElement)
    }

    fun add(screenElements: Collection<ScreenElement>) {
        mutableSelection.addAll(screenElements.map { it.selectionTarget })
    }

    fun remove(screenElement: ScreenElement) {
        mutableSelection.remove(screenElement)
    }

    fun removeAll(screenElements: Collection<ScreenElement>) {
        mutableSelection.removeAll(screenElements)
    }

    operator fun contains(screenElement: ScreenElement) = screenElement in selection

    fun setSelection(screenElements: Collection<ScreenElement>) {
        clear()
        mutableSelection.addAll(screenElements)
    }

    fun setSelection(screenElement: ScreenElement) {
        clear()
        mutableSelection.add(screenElement)
    }

}