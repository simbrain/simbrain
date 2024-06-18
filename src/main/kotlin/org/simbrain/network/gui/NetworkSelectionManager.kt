package org.simbrain.network.gui

import org.simbrain.network.core.NetworkModel
import org.simbrain.network.events.NetworkSelectionEvent
import org.simbrain.network.gui.nodes.ScreenElement
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Manges network selection. E.g. when you select a group of nodes it tracks which nodes were selected.
 * Keeps track of source vs. selected nodes. When [#modifySelection] or [#modifySourceSelection] are  called an
 * event is fired which is handled in [NetworkPanel] where the selection manager is set up.
 *
 * To get all selected nodes of a type, use filterSelectedModels.
 *
 * Kotlin Example: `val selectedNeurons = filterSelectedModels<Neuron>()`
 * Java example: `var selectedNeurons = filterSelectedModels(Neuron.class)`
 */
class NetworkSelectionManager(val networkPanel: NetworkPanel) {

    /**
     * Handle network selection events.
     */
    val events = NetworkSelectionEvent()

    /**
     * "Green" selection from lasso.
     */
    val selection: Set<ScreenElement> = CopyOnWriteArraySet()

    /**
     * "Red" source selection.
     */
    val sourceSelection: Set<ScreenElement> = CopyOnWriteArraySet()

    /**
     * Filter selected network models using a generic type.  With a helper for java code (which requires a class
     * object).
     */
    inline fun <reified T: NetworkModel> filterSelectedModels() = selectedModels.filterIsInstance<T>()
    fun <T: NetworkModel> filterSelectedModels(clazz: Class<T>) = selectedModels.filterIsInstance(clazz)

    /**
     * Filter selected network models.
     */
    inline fun <reified T: NetworkModel> filterSelectedSourceModels() = sourceModels.filterIsInstance<T>()
    fun <T: NetworkModel> filterSelectedSourceModels(clazz: Class<T>) = sourceModels.filterIsInstance(clazz)

    /**
     * Filter selected [ScreenElement]s
     */
    inline fun <reified T: ScreenElement> filterSelectedNodes() = selection.filterIsInstance<T>()
    fun <T: ScreenElement> filterSelectedNodes(clazz: Class<T>) = selection.filterIsInstance(clazz)

    /**
     * Filter selected [ScreenElement] source nodes.
     */
    inline fun <reified T: ScreenElement> filterSelectedSourceNodes() = sourceSelection.filterIsInstance<T>()
    fun <T: ScreenElement> filterSelectedSourceNodes(clazz: Class<T>) = sourceSelection.filterIsInstance(clazz)

    /**
     * Getter for selected models.
     */
    val selectedModels get() = selection.map { it.model }.toSet().toList()

    /**
     * Getter for source models.
     */
    val sourceModels get() = sourceSelection.map { it.model!! }.toSet().toList()

    val isEmpty get() = selection.isEmpty()
    val isNotEmpty get() = !isEmpty

    operator fun contains(screenElement: ScreenElement) = screenElement in selection

    /**
     * Clear the "green" selection
     */
    fun clear() = modifySelection {
        clear()
    }

    /**
     * Add a single node to the selection.
     */
    fun add(screenElement: ScreenElement) = modifySelection {
        add(screenElement)
    }

    /**
     * Add a collection of nodes to the selection.
     */
    fun add(screenElements: Collection<ScreenElement>) = modifySelection {
        addAll(screenElements)
    }

    /**
     * Remove a single node from the selection.
     */
    fun remove(screenElement: ScreenElement) = modifySelection {
        remove(screenElement)
    }

    /**
     * Remove a collection of screen elements
     */
    fun remove(screenElements: Collection<ScreenElement>) = modifySelection {
        removeAll(screenElements)
    }

    /**
     * Set the selection to a provided collection
     */
    fun set(screenElements: Collection<ScreenElement>) = modifySelection {
        clear()
        addAll(screenElements)
    }

    /**
     * Set the selection to a single provided node.
     */
    fun set(screenElement: ScreenElement) = modifySelection {
        clear()
        add(screenElement)
    }

    /**
     * Toggle a single node's selection.
     */
    fun toggle(screenElement: ScreenElement) = modifySelection {
        if (screenElement in selection) {
            remove(screenElement)
        } else {
            add(screenElement)
        }
    }

    /**
     * Toggle a collection of nodes.
     */
    fun toggle(screenElements: Collection<ScreenElement>) = screenElements.forEach { toggle(it) }

    /**
     * Unselect provided element
     */
    fun unselect(screenElement: ScreenElement) =  modifySelection { remove(screenElement) }

    /**
     * Unselect provided element
     */
    fun unselect(screenElements: Collection<ScreenElement>) = screenElements.forEach { unselect(it) }

    /**
     * Returns true if the provided screen element is selected.
     */
    fun isSelected(screenElement: ScreenElement) = screenElement in selection

    /**
     * Convert all selected nodes to selected source "red" nodes.
     */
    fun convertSelectedNodesToSourceNodes() = modifySourceSelection {
        clear()
        addAll(selection.filter { it.acceptsSourceHandle()})
    }

    /**
     * Clear all "red" source handles.
     */
    fun clearAllSource() = modifySourceSelection { clear() }

    /**
     * Select all selectable nodes in the network panel.
     */
    fun selectAll() {
        add(networkPanel.screenElements)
    }

    /**
     * Core function which tells the Network Panel to updated provided ScreenElements. Modifies the [selection].
     */
    private fun modifySourceSelection(block: CopyOnWriteArraySet<ScreenElement>.() -> Unit) {
        val old = HashSet(sourceSelection)
        (sourceSelection as CopyOnWriteArraySet).block()
        events.sourceSelection.fire(old, sourceSelection)
    }

    /**
     * Core function which tells the Network Panel to updated provided ScreenElements. Modifies the [selection].
     */
    private fun modifySelection(action: CopyOnWriteArraySet<ScreenElement>.() -> Unit) {
        val old = HashSet(selection)
        // Invoke provided action
        (selection as CopyOnWriteArraySet).action()
        events.selection.fire(old, selection)
    }


}