package org.simbrain.network.gui

import org.simbrain.network.NetworkModel
import org.simbrain.network.events.NetworkSelectionEvent
import org.simbrain.network.gui.nodes.ScreenElement
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Manges network selection. E.g. when you select a group of nodes it tracks which nodes were selected.
 * Keeps track of source vs. selected nodes.
 */
class NetworkSelectionManager(val networkPanel: NetworkPanel) {

    val events = NetworkSelectionEvent(this)

    val selection: Set<ScreenElement> = CopyOnWriteArraySet()
    val sourceSelection: Set<ScreenElement> = CopyOnWriteArraySet()
    inline fun <reified T: ScreenElement> selectionOf() = selection.filterIsInstance<T>()
    inline fun <reified T: ScreenElement> sourceSelectionOf() = sourceSelection.filterIsInstance<T>()
    fun <T: ScreenElement> selectionOf(clazz: Class<T>) = selection.filterIsInstance(clazz)
    fun <T: ScreenElement> sourceSelectionOf(clazz: Class<T>) = sourceSelection.filterIsInstance(clazz)

    val selectedModels = selection.map { it.model!! }
    val sourceModels = sourceSelection.map { it.model!! }
    inline fun <reified T: NetworkModel> selectedModelsOf() = selectedModels.filterIsInstance<T>()
    inline fun <reified T: NetworkModel> sourceModelsOf() = sourceModels.filterIsInstance<T>()
    fun <T: NetworkModel> selectedModelsOf(clazz: Class<T>) = selectedModels.filterIsInstance(clazz)
    fun <T: NetworkModel> sourceModelsOf(clazz: Class<T>) = sourceModels.filterIsInstance(clazz)

    val isEmpty get() = selection.isEmpty()
    val isNotEmpty get() = !isEmpty

    operator fun contains(screenElement: ScreenElement) = screenElement in selection

    fun clear() = modifySelection {
        clear()
    }

    fun add(screenElement: ScreenElement) = modifySelection {
        add(screenElement)
    }

    fun add(screenElements: Collection<ScreenElement>) = modifySelection {
        addAll(screenElements.map { it.selectionTarget })
    }

    fun remove(screenElement: ScreenElement) = modifySelection {
        remove(screenElement)
    }

    fun remove(screenElements: Collection<ScreenElement>) = modifySelection {
        removeAll(screenElements)
    }

    fun set(screenElements: Collection<ScreenElement>) = modifySelection {
        clear()
        addAll(screenElements)
    }

    fun set(screenElement: ScreenElement) = modifySelection {
        clear()
        add(screenElement)
    }

    fun toggle(screenElement: ScreenElement) = modifySelection {
        if (screenElement in selection) {
            remove(screenElement)
        } else {
            add(screenElement)
        }
    }

    fun toggle(screenElements: Collection<ScreenElement>) = screenElements.forEach { toggle(it) }

    fun markAllAsSource() = modifySourceSelection { addAll(selection) }
    fun clearAllSource() = modifySourceSelection { clear() }

    private fun modifySourceSelection(block: CopyOnWriteArraySet<ScreenElement>.() -> Unit) {
        (sourceSelection as CopyOnWriteArraySet).block()
    }

    private fun modifySelection(block: CopyOnWriteArraySet<ScreenElement>.() -> Unit) {
        val old = HashSet(selection)
        (selection as CopyOnWriteArraySet).block()
        events.fireSelection(old, selection)
    }

    fun selectAll() {
        // TODO.  Consider a new method in network panel that gets all screen elements without filtering
    }

}