package org.simbrain.world.odorworld.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.simbrain.network.gui.nodes.InteractionBox
import org.simbrain.network.gui.nodes.NodeHandle
import org.simbrain.util.Events
import org.simbrain.util.complement
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

class OdorWorldSelectionEvent: Events() {
    val selection = ChangedEvent<Set<PNode>>()
}

/**
 * World selection model.
 */
class WorldSelectionManager {
    val events = OdorWorldSelectionEvent().apply {
        selection.on(Dispatchers.Swing) { old, new ->
            val (removed, added) = old complement new
            removed.forEach { NodeHandle.removeSelectionHandleFrom(it) }
            added.forEach {
                if (it is InteractionBox) {
                    NodeHandle.addSelectionHandleTo(it, NodeHandle.INTERACTION_BOX_SELECTION_STYLE)
                } else {
                    NodeHandle.addSelectionHandleTo(it)
                }
            }
        }
    }

    private val _selection = CopyOnWriteArraySet<PNode>()

    var selection: MutableSet<PNode>
        get() = Collections.unmodifiableSet(_selection)
        set(elements) {
            if (_selection.isEmpty() && elements.isEmpty()) {
                return
            }

            isAdjusting = true
            val oldSelection = HashSet(_selection)
            _selection.clear()
            val rv = _selection.addAll(elements)
            isAdjusting = false

            if (rv || elements.isEmpty()) {
                events.selection.fireAndBlock(oldSelection, _selection)
            }
        }

    /**
     * Adjusting.
     */
    var isAdjusting: Boolean = false

    /**
     * Return the size of the selection.
     *
     * @return the size of the selection
     */
    fun size(): Int {
        return selection.size
    }

    /**
     * Clear the selection.
     */
    fun clear() {
        if (!isEmpty) {
            val oldSelection = HashSet(_selection)
            _selection.clear()
            events.selection.fireAndBlock(oldSelection, _selection)
        }
    }

    val isEmpty: Boolean
        /**
         * Return true if the selection is empty.
         *
         * @return true if the selection is empty
         */
        get() = _selection.isEmpty()

    /**
     * Add the specified element to the selection.
     *
     * @param element element to add
     */
    fun add(element: PNode) {
        val oldSelection = HashSet(_selection)
        val rv = _selection.add(element)
        if (rv) {
            events.selection.fireAndBlock(oldSelection, _selection)
        }
    }

    /**
     * Add all of the specified elements to the selection.
     *
     * @param elements elements to add
     */
    fun addAll(elements: Collection<PNode>) {
        isAdjusting = true
        val oldSelection = HashSet(_selection)
        val rv = _selection.addAll(elements)
        isAdjusting = false

        if (rv) {
            events.selection.fireAndBlock(oldSelection, _selection)
        }
    }

    /**
     * Remove the specified element from the selection.
     *
     * @param element element to remove
     */
    fun remove(element: PNode) {
        val oldSelection = HashSet(_selection)
        val rv = _selection.remove(element)
        if (rv) {
            events.selection.fireAndBlock(oldSelection, _selection)
        }
    }

    /**
     * Remove all of the specified elements from the selection.
     *
     * @param elements elements to remove
     */
    fun removeAll(elements: Collection<PNode>) {
        isAdjusting = true
        val oldSelection = HashSet(_selection)
        val rv = _selection.removeAll(elements.toSet())
        isAdjusting = false

        if (rv) {
            events.selection.fireAndBlock(oldSelection, _selection)
        }
    }

    /**
     * Return true if the specified element is selected.
     *
     * @param element element
     * @return true if the specified element is selected
     */
    fun isSelected(element: Any?): Boolean {
        return _selection.contains(element)
    }
}