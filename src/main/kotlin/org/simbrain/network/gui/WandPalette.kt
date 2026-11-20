/**
 * The wand's palette of actions. One action is selected and applied by an ordinary wand stroke; a second, the
 * secondary, is applied by a stroke made with Shift held. Unless one of the palette's actions is chosen for it, the
 * secondary is the opposite of the selected action and so changes with it.
 */
package org.simbrain.network.gui

import org.simbrain.util.FlowEvents
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
     * Index of the action chosen as the secondary. Any value that is not the index of an action, such as the -1 of
     * a palette where none has been chosen, makes the secondary automatic: see [secondaryAction].
     */
    var secondaryIndex: Int = -1
        set(value) {
            if (value != field) {
                field = value
                events.secondaryChanged.fire()
            }
        }

    /**
     * Whether the secondary action is worked out from the selected action rather than chosen.
     */
    val isSecondaryAutomatic: Boolean
        get() = secondaryIndex !in actions.indices

    /**
     * The action applied by a stroke made with Shift held. When one has been chosen with [secondaryIndex] it is that
     * action. Otherwise it is the opposite of the selected action, so that Shift takes back what the wand draws:
     * the palette's own action with that effect if it has one, and failing that an action made for the purpose,
     * which is not in [actions] and is a new object every time this is read. Null if the selected action has no
     * opposite.
     */
    val secondaryAction: WandAction?
        get() = actions.getOrNull(secondaryIndex) ?: oppositeOfSelected()

    /**
     * The opposite of the selected action when a secondary has been chosen that does something else, and null
     * otherwise. It is what [useSuggestedSecondary] would make the secondary.
     */
    val suggestedSecondary: WandAction?
        get() {
            val chosen = actions.getOrNull(secondaryIndex) ?: return null
            return oppositeOfSelected()?.takeUnless { chosen.hasSameEffectAs(it) }
        }

    /**
     * Give up the chosen secondary for the opposite of the selected action, now and as the selection changes.
     */
    fun useSuggestedSecondary() {
        secondaryIndex = -1
    }

    private fun oppositeOfSelected(): WandAction? = selectedAction?.opposite()?.let { opposite ->
        actions.firstOrNull { it.hasSameEffectAs(opposite) } ?: opposite
    }

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
            when {
                index < secondaryIndex -> secondaryIndex--
                index == secondaryIndex -> secondaryIndex = -1
            }
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
     * Cycle to the previous action in the palette.
     */
    fun cycleToPreviousAction() {
        if (actions.isNotEmpty()) {
            selectedIndex = (selectedIndex - 1 + actions.size) % actions.size
        }
    }

    /**
     * Make the secondary action the selected one and the selected action the secondary. Does nothing if there is no
     * secondary action or it is not one of the palette's actions. An automatic secondary stays automatic when that
     * alone makes the action that was selected the secondary.
     */
    fun swapSelectedAndSecondary() {
        val secondary = actions.indexOf(secondaryAction ?: return)
        if (secondary < 0) return
        val selected = selectedIndex
        val wasAutomatic = isSecondaryAutomatic
        selectedIndex = secondary
        if (!wasAutomatic || secondaryAction !== actions[selected]) secondaryIndex = selected
    }

    /**
     * Replace all actions with the given list, the secondary being the one at [newSecondaryIndex].
     */
    fun setActions(newActions: List<WandAction>, newSecondaryIndex: Int = -1) {
        actions.clear()
        actions.addAll(newActions)
        selectedIndex = if (actions.isNotEmpty()) 0 else 0
        secondaryIndex = newSecondaryIndex
        events.paletteChanged.fire()
    }

    /**
     * Create a copy of this palette.
     */
    fun copy(): WandPalette = WandPalette().also { newPalette ->
        newPalette.actions.addAll(actions.map { it.copy() as WandAction })
        newPalette.selectedIndex = selectedIndex
        newPalette.secondaryIndex = secondaryIndex
    }

    override val name: String get() = "Wand Palette"

    companion object {
        /**
         * Create a default palette with standard actions.
         */
        fun createDefault(): WandPalette = WandPalette().apply {
            // Neuron actions
            addAction(AdjustValueAction.activate())
            addAction(ForceSpikeAction())
            //addAction(AdjustValueAction.inhibit())
            addAction(AdjustValueAction.setValue(0.0))
            //addAction(AdjustValueAction.randomize())
            // Synapse actions - weight setting
            //addAction(AdjustValueAction.synapseStrength(1.0))
            //addAction(AdjustValueAction.synapseStrength(-1.0).apply {
            //    letter = "W-"
            //    color = Color(100, 150, 255, 220)  // Light blue
            //})
            addAction(AdjustValueAction.synapseStrength(0.0).apply {
                letter = "W0"
                color = Color(150, 150, 150, 220)  // Gray
            })
            // Connection actions
            addAction(ConnectToNeighborsAction())
            addAction(ConnectFromSourceAction())
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
class WandPaletteEvents : FlowEvents() {
    val selectionChanged = NoArgEvent()
    val secondaryChanged = NoArgEvent()
    val actionAdded = OneArgEvent<WandAction>()
    val actionRemoved = OneArgEvent<WandAction>()
    val actionEdited = OneArgEvent<WandAction>()
    val paletteChanged = NoArgEvent()
}
