package org.simbrain.network.gui

import org.simbrain.util.Events
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.Color

/**
 * Manages a collection of wand actions. The user can select one action to be active.
 * The palette is persisted to user preferences.
 */
class WandPalette : EditableObject {

    /**
     * Events fired by this palette.
     */
    @Transient
    val events = WandPaletteEvents()

    /**
     * List of configured actions.
     */
    val actions: MutableList<WandAction> = mutableListOf()

    /**
     * Index of the currently selected action.
     */
    var selectedIndex: Int = 0
        set(value) {
            if (value != field && value >= 0 && value < actions.size) {
                field = value
                events.selectionChanged.fire()
            }
        }

    /**
     * The currently selected action, or null if no actions exist.
     */
    val selectedAction: WandAction?
        get() = actions.getOrNull(selectedIndex)

    /**
     * Add an action to the palette.
     */
    fun addAction(action: WandAction) {
        actions.add(action)
        events.actionAdded.fire(action)
        if (actions.size == 1) {
            selectedIndex = 0
        }
    }

    /**
     * Remove an action at the given index.
     */
    fun removeAction(index: Int) {
        if (index >= 0 && index < actions.size) {
            val action = actions.removeAt(index)
            events.actionRemoved.fire(action)
            if (selectedIndex >= actions.size) {
                selectedIndex = (actions.size - 1).coerceAtLeast(0)
            }
        }
    }

    /**
     * Select an action by index.
     */
    fun selectAction(index: Int) {
        selectedIndex = index
    }

    /**
     * Cycle to the next action in the palette.
     */
    fun cycleToNextAction() {
        if (actions.isNotEmpty()) {
            selectedIndex = (selectedIndex + 1) % actions.size
        }
    }

    /**
     * Replace all actions with the given list.
     */
    fun setActions(newActions: List<WandAction>) {
        actions.clear()
        actions.addAll(newActions)
        selectedIndex = if (actions.isNotEmpty()) 0 else 0
        events.paletteChanged.fire()
    }

    /**
     * Create a copy of this palette.
     */
    fun copy(): WandPalette = WandPalette().also { newPalette ->
        newPalette.actions.addAll(actions.map { it.copy() as WandAction })
        newPalette.selectedIndex = selectedIndex
    }

    override val name: String get() = "Wand Palette"

    companion object {
        /**
         * Create a default palette with standard actions.
         */
        fun createDefault(): WandPalette = WandPalette().apply {
            // Neuron actions
            addAction(AdjustValueAction.activate())
            addAction(AdjustValueAction.inhibit())
            addAction(AdjustValueAction.setValue(0.0))
            addAction(AdjustValueAction.randomize())
            // Synapse actions - weight setting
            addAction(AdjustValueAction.synapseStrength(1.0))
            addAction(AdjustValueAction.synapseStrength(-1.0).apply {
                letter = "W-"
                color = Color(100, 150, 255, 220)  // Light blue
            })
            addAction(AdjustValueAction.synapseStrength(0.0).apply {
                letter = "W0"
                color = Color(150, 150, 150, 220)  // Gray
            })
            // Connection actions
            addAction(ConnectFromSourceAction())
            addAction(ConnectToNeighborsAction())
            addAction(PruneWeightsAction())
            // Neuron increment/decrement
            addAction(AdjustValueAction.increment())
            addAction(AdjustValueAction.decrement())
        }
    }
}

/**
 * Events fired by [WandPalette].
 */
class WandPaletteEvents : Events() {
    val selectionChanged = NoArgEvent()
    val actionAdded = OneArgEvent<WandAction>()
    val actionRemoved = OneArgEvent<WandAction>()
    val paletteChanged = NoArgEvent()
}
