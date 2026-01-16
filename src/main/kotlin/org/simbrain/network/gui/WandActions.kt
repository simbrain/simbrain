package org.simbrain.network.gui

import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import java.awt.Color

/**
 * Base class for wand actions. Wand actions are applied to network models as the wand is dragged over them.
 * Each action can have its own configuration parameters and color.
 * Implementations should check the model type and handle appropriately.
 */
abstract class WandAction : CopyableObject {

    /**
     * Single letter displayed on the toolbar button when this action is selected.
     */
    @UserParameter(label = "Letter", description = "Single letter shown on toolbar button", order = 999)
    open var letter: String = "W"

    /**
     * Color used for the wand cursor when this action is active.
     */
    @UserParameter(label = "Color", description = "Wand cursor color for this action", order = 1000)
    open var color: Color = Color.YELLOW

    /**
     * Short description shown in the palette.
     */
    abstract val description: String

    /**
     * Apply this action to a network model.
     * Implementations should check the model type and handle appropriately.
     * @param model The network model to operate on
     * @param networkPanel Access to network panel for selection, undo tracking, etc.
     * @param undoState Map to store original values for undo support
     */
    abstract fun apply(model: NetworkModel, networkPanel: NetworkPanel, undoState: MutableMap<Any, Any?>)

    /**
     * Called when wand drag starts. Override to capture initial state for undo.
     */
    open fun beginAction(networkPanel: NetworkPanel) {}

    /**
     * Called when wand drag ends. Override to finalize undo action.
     */
    open fun endAction(networkPanel: NetworkPanel) {}

    /**
     * Create undo description for this action.
     */
    open fun undoDescription(count: Int): String = "Wand: $description on $count neurons"

    override fun getTypeList(): List<Class<out CopyableObject>> = listOf(
        ActivateAction::class.java,
        InhibitAction::class.java,
        SetToValueAction::class.java,
        RandomizeAction::class.java,
        ConnectFromSourceAction::class.java,
    )
}

/**
 * Sets neurons to their upper bound (activates them).
 */
class ActivateAction : WandAction() {

    @UserParameter(label = "Use upper bound", description = "If true, set to upper bound. If false, use custom value.", order = 10)
    var useUpperBound: Boolean = true

    @UserParameter(label = "Custom value", description = "Value to set when not using upper bound", order = 20)
    var customValue: Double = 1.0

    override val description: String
        get() = if (useUpperBound) "Activate (upper bound)" else "Activate ($customValue)"

    override var letter: String = "A"
    override var color: Color = Color(255, 230, 0, 220)  // Yellow

    override fun apply(model: NetworkModel, networkPanel: NetworkPanel, undoState: MutableMap<Any, Any?>) {
        if (model !is Neuron) return
        undoState.putIfAbsent(model, model.activation)
        model.activation = if (useUpperBound) model.upperBound else customValue
    }

    override fun copy(): CopyableObject = ActivateAction().also {
        it.letter = letter
        it.color = color
        it.useUpperBound = useUpperBound
        it.customValue = customValue
    }

    override val name: String get() = "Activate"
}

/**
 * Sets neurons to their lower bound (inhibits them).
 */
class InhibitAction : WandAction() {

    @UserParameter(label = "Use lower bound", description = "If true, set to lower bound. If false, use custom value.", order = 10)
    var useLowerBound: Boolean = true

    @UserParameter(label = "Custom value", description = "Value to set when not using lower bound", order = 20)
    var customValue: Double = -1.0

    override val description: String
        get() = if (useLowerBound) "Inhibit (lower bound)" else "Inhibit ($customValue)"

    override var letter: String = "I"
    override var color: Color = Color(100, 100, 255, 220)  // Blue

    override fun apply(model: NetworkModel, networkPanel: NetworkPanel, undoState: MutableMap<Any, Any?>) {
        if (model !is Neuron) return
        undoState.putIfAbsent(model, model.activation)
        model.activation = if (useLowerBound) model.lowerBound else customValue
    }

    override fun copy(): CopyableObject = InhibitAction().also {
        it.letter = letter
        it.color = color
        it.useLowerBound = useLowerBound
        it.customValue = customValue
    }

    override val name: String get() = "Inhibit"
}

/**
 * Sets neurons to a specific value.
 */
class SetToValueAction : WandAction() {

    @UserParameter(label = "Value", description = "Value to set neurons to", order = 10)
    var value: Double = 0.0

    override val description: String get() = "Set to $value"

    override var letter: String = "S"
    override var color: Color = Color(150, 150, 150, 220)  // Gray

    override fun apply(model: NetworkModel, networkPanel: NetworkPanel, undoState: MutableMap<Any, Any?>) {
        if (model !is Neuron) return
        undoState.putIfAbsent(model, model.activation)
        model.activation = value
    }

    override fun copy(): CopyableObject = SetToValueAction().also {
        it.letter = letter
        it.color = color
        it.value = value
    }

    override val name: String get() = "Set to Value"
}

/**
 * Sets neurons to a random value within their bounds.
 */
class RandomizeAction : WandAction() {

    @UserParameter(label = "Use neuron bounds", description = "If true, randomize within neuron bounds. If false, use custom range.", order = 10)
    var useNeuronBounds: Boolean = true

    @UserParameter(label = "Min value", description = "Minimum value for randomization", order = 20)
    var minValue: Double = -1.0

    @UserParameter(label = "Max value", description = "Maximum value for randomization", order = 30)
    var maxValue: Double = 1.0

    override val description: String
        get() = if (useNeuronBounds) "Randomize (within bounds)" else "Randomize ($minValue to $maxValue)"

    override var letter: String = "R"
    override var color: Color = Color(100, 200, 100, 220)  // Green

    override fun apply(model: NetworkModel, networkPanel: NetworkPanel, undoState: MutableMap<Any, Any?>) {
        if (model !is Neuron) return
        undoState.putIfAbsent(model, model.activation)
        val min = if (useNeuronBounds) model.lowerBound else minValue
        val max = if (useNeuronBounds) model.upperBound else maxValue
        model.activation = min + Math.random() * (max - min)
    }

    override fun copy(): CopyableObject = RandomizeAction().also {
        it.letter = letter
        it.color = color
        it.useNeuronBounds = useNeuronBounds
        it.minValue = minValue
        it.maxValue = maxValue
    }

    override val name: String get() = "Randomize"
}

/**
 * Creates synapses from currently selected neurons to the neurons the wand touches.
 */
class ConnectFromSourceAction : WandAction() {

    @UserParameter(label = "Weight", description = "Weight for new synapses", order = 10)
    var weight: Double = 1.0

    @UserParameter(label = "Only if no existing synapse", description = "Skip if synapse already exists from source to target", order = 20)
    var skipExisting: Boolean = true

    override val description: String get() = "Connect from selection (w=$weight)"

    override var letter: String = "C"
    override var color: Color = Color(200, 100, 200, 220)  // Purple

    @Transient
    private val createdSynapses = mutableListOf<Synapse>()

    override fun beginAction(networkPanel: NetworkPanel) {
        createdSynapses.clear()
    }

    override fun apply(model: NetworkModel, networkPanel: NetworkPanel, undoState: MutableMap<Any, Any?>) {
        if (model !is Neuron) return
        val sourceNeurons = networkPanel.selectionManager.filterSelectedModels<Neuron>()
        val network = networkPanel.network

        for (source in sourceNeurons) {
            if (source == model) continue  // Don't connect to self

            // Check if synapse already exists (fanOut maps target neuron -> synapse)
            if (skipExisting && source.fanOut.containsKey(model)) {
                continue
            }

            val synapse = Synapse(source, model, weight)
            network.addNetworkModelAsync(synapse)
            createdSynapses.add(synapse)
        }
    }

    override fun endAction(networkPanel: NetworkPanel) {
        if (createdSynapses.isNotEmpty()) {
            val synapses = createdSynapses.toList()
            networkPanel.undoManager.addUndoableAction(
                description = "Wand: Created ${synapses.size} synapses",
                undo = {
                    synapses.forEach { it.deleteBlocking() }
                },
                redo = {
                    synapses.forEach { networkPanel.network.addNetworkModelAsync(Synapse(it.source, it.target, it.strength)) }
                }
            )
        }
        createdSynapses.clear()
    }

    override fun undoDescription(count: Int): String = "Wand: Connect from selection"

    override fun copy(): CopyableObject = ConnectFromSourceAction().also {
        it.letter = letter
        it.color = color
        it.weight = weight
        it.skipExisting = skipExisting
    }

    override val name: String get() = "Connect from Selection"
}
