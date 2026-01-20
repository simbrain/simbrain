package org.simbrain.network.gui

import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.connections.getNeuronsInRadius
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
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
     * Radius of the wand effect area in pixels.
     */
    @UserParameter(label = "Radius", description = "Radius of the wand effect in pixels", minimumValue = 1.0, order = 1001)
    open var radius: Int = 40

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
    open fun undoDescription(count: Int): String = "Wand: $description on $count items"

    override fun getTypeList(): List<Class<out CopyableObject>> = listOf(
        AdjustValueAction::class.java,
        ConnectFromSourceAction::class.java,
        ConnectToNeighborsAction::class.java,
        PruneWeightsAction::class.java,
    )
}

/**
 * Sealed class for targets that can be adjusted by [AdjustValueAction].
 * Each target knows how to get/set a value and its bounds from a [NetworkModel].
 */
sealed class AdjustValueTarget : EditableObject {
    /** Check if this target applies to the given model */
    abstract fun matches(model: NetworkModel): Boolean
    /** Get the current value from the model */
    abstract fun getValue(model: NetworkModel): Double
    /** Set the value on the model */
    abstract fun setValue(model: NetworkModel, value: Double)
    /** Get the lower and upper bounds for the model */
    abstract fun getBounds(model: NetworkModel): Pair<Double, Double>
    /** Description for this target type */
    abstract val targetName: String

    /** Target neuron activations */
    object NeuronActivation : AdjustValueTarget() {
        override fun matches(model: NetworkModel) = model is Neuron
        override fun getValue(model: NetworkModel) = (model as Neuron).activation
        override fun setValue(model: NetworkModel, value: Double) { (model as Neuron).activation = value }
        override fun getBounds(model: NetworkModel) = (model as Neuron).let { it.lowerBound to it.upperBound }
        override val targetName = "activation"
        override val name = "Neuron Activation"
    }

    /** Target synapse strengths (weights) */
    object SynapseStrength : AdjustValueTarget() {
        override fun matches(model: NetworkModel) = model is Synapse
        override fun getValue(model: NetworkModel) = (model as Synapse).strength
        override fun setValue(model: NetworkModel, value: Double) { (model as Synapse).strength = value }
        override fun getBounds(model: NetworkModel) = (model as Synapse).let { it.lowerBound to it.upperBound }
        override val targetName = "strength"
        override val name = "Synapse Strength"
    }
}

/**
 * Sealed class for value operations. Each subclass defines how to combine
 * the target value with the current value.
 */
sealed class AdjustValueOperation : EditableObject {
    abstract fun apply(currentValue: Double, targetValue: Double): Double
    abstract val verb: String

    /** Replace the value with the target value */
    object Set : AdjustValueOperation() {
        override fun apply(currentValue: Double, targetValue: Double) = targetValue
        override val verb = "Set"
        override val name = "Set"
    }

    /** Add the target value to the current value */
    object Add : AdjustValueOperation() {
        override fun apply(currentValue: Double, targetValue: Double) = currentValue + targetValue
        override val verb = "Add"
        override val name = "Add"
    }
}

/**
 * Sealed class for determining the value amount. Each subclass encapsulates
 * its own parameters and computation logic.
 */
sealed class AdjustValueAmount : CopyableObject {
    abstract fun computeValue(lower: Double, upper: Double): Double
    abstract val description: String

    override fun getTypeList() = listOf(
        UpperBound::class.java,
        LowerBound::class.java,
        Value::class.java,
        Relative::class.java,
        Random::class.java
    )

    /** Use the upper bound */
    class UpperBound : AdjustValueAmount(), EditableObject {
        override fun computeValue(lower: Double, upper: Double) = upper
        override val description = "upper bound"
        override val name = "Upper Bound"
        override fun copy() = UpperBound()
    }

    /** Use the lower bound */
    class LowerBound : AdjustValueAmount(), EditableObject {
        override fun computeValue(lower: Double, upper: Double) = lower
        override val description = "lower bound"
        override val name = "Lower Bound"
        override fun copy() = LowerBound()
    }

    /** Use a custom fixed value */
    class Value(value: Double = 1.0) : AdjustValueAmount(), EditableObject {
        var value by GuiEditable(
            initValue = value,
            label = "Value",
            description = "The absolute value to use"
        )
        override fun computeValue(lower: Double, upper: Double) = this.value
        override val description get() = "$value"
        override val name = "Value"
        override fun copy() = Value(value)
    }

    /**
     * Use a value relative to bounds.
     * -1 = lower bound, 1 = upper bound, 0 = zero (clamped to bounds if outside range).
     */
    class Relative(relativeValue: Double = 0.0) : AdjustValueAmount(), EditableObject {
        var relativeValue by GuiEditable(
            initValue = relativeValue,
            min = -1.0,
            max = 1.0,
            increment = 0.1,
            label = "Relative Value",
            description = "Value from -1 (lower bound) to 1 (upper bound), with 0 = zero"
        )

        override fun computeValue(lower: Double, upper: Double): Double {
            val zeroPoint = 0.0.coerceIn(lower, upper)
            return if (relativeValue <= 0) {
                zeroPoint + relativeValue * (zeroPoint - lower)
            } else {
                zeroPoint + relativeValue * (upper - zeroPoint)
            }
        }

        override val description get() = "relative $relativeValue"
        override val name = "Relative"
        override fun copy() = Relative(relativeValue)
    }

    /** Use a random value from a probability distribution */
    class Random(randomizer: ProbabilityDistribution = UniformRealDistribution(-1.0, 1.0)) : AdjustValueAmount(), EditableObject {
        var randomizer by GuiEditable(
            initValue = randomizer,
            label = "Randomizer",
            description = "Probability distribution for random values",
            showDetails = false
        )
        override fun computeValue(lower: Double, upper: Double) = randomizer.sampleDouble()
        override val description get() = "random (${randomizer.name})"
        override val name = "Random"
        override fun copy() = Random(randomizer.copy())
    }
}

/**
 * Unified action for adjusting values on network models (neurons or synapses).
 * Combines activate, inhibit, set-to-value, and randomize into a single configurable action.
 *
 * Examples:
 * - Activate neuron: AdjustValueAction(target = NeuronActivation, amount = UpperBound())
 * - Set synapse weight: AdjustValueAction(target = SynapseStrength, amount = Value(0.5))
 * - Randomize: AdjustValueAction(amount = Random())
 * - Increment: AdjustValueAction(amount = Value(0.1), operation = Add)
 */
class AdjustValueAction(
    amount: AdjustValueAmount = AdjustValueAmount.UpperBound(),
    operation: AdjustValueOperation = AdjustValueOperation.Set,
    target: AdjustValueTarget = AdjustValueTarget.NeuronActivation
) : WandAction(), EditableObject {

    /**
     * How to determine the value.
     */
    var amount by GuiEditable(
        initValue = amount,
        label = "Amount",
        description = "How to determine the value",
        order = 10
    )

    /**
     * The operation to perform: Set replaces the value, Add adds to it.
     */
    var operation by GuiEditable(
        initValue = operation,
        label = "Operation",
        description = "Set replaces value, Add adds to current value",
        order = 20
    )

    /**
     * What to target: neuron activation or synapse strength.
     */
    var target by GuiEditable(
        initValue = target,
        label = "Target",
        description = "What property to adjust",
        order = 30
    )

    /**
     * Whether to clamp the result within the bounds.
     */
    var clampToBounds by GuiEditable(
        initValue = false,
        label = "Clamp to Bounds",
        description = "If true, result is clamped to lower and upper bounds",
        order = 50
    )

    override val description: String
        get() = "${operation.verb} ${target.targetName} ${amount.description}"

    override var letter: String = "A"
    override var color: Color = Color(255, 230, 0, 220)  // Yellow

    override fun apply(model: NetworkModel, networkPanel: NetworkPanel, undoState: MutableMap<Any, Any?>) {
        if (!target.matches(model)) return
        undoState.putIfAbsent(model, target.getValue(model))

        val (lower, upper) = target.getBounds(model)
        val targetValue = amount.computeValue(lower, upper)
        val currentValue = target.getValue(model)
        val newValue = operation.apply(currentValue, targetValue)

        target.setValue(model, if (clampToBounds) newValue.coerceIn(lower, upper) else newValue)
    }

    override fun copy(): CopyableObject = AdjustValueAction(
        amount.copy() as AdjustValueAmount,
        operation,
        target
    ).also {
        it.letter = letter
        it.color = color
        it.radius = radius
        it.clampToBounds = clampToBounds
    }

    override val name: String get() = "Adjust Value"

    companion object {
        /** Convenience factory for activate action (set neuron to upper bound) */
        fun activate() = AdjustValueAction().apply {
            letter = "A"
            color = Color(255, 230, 0, 220)  // Yellow
        }

        /** Convenience factory for inhibit action (set neuron to lower bound) */
        fun inhibit() = AdjustValueAction(
            amount = AdjustValueAmount.LowerBound()
        ).apply {
            letter = "I"
            color = Color(100, 100, 255, 220)  // Blue
        }

        /** Convenience factory for set-to-value action */
        fun setValue(v: Double = 0.0) = AdjustValueAction(
            amount = AdjustValueAmount.Value(v)
        ).apply {
            letter = "S"
            color = Color(150, 150, 150, 220)  // Gray
        }

        /** Convenience factory for relative value action (-1 = lower, 0 = zero, 1 = upper) */
        fun setRelative(rel: Double = 0.0) = AdjustValueAction(
            amount = AdjustValueAmount.Relative(rel.coerceIn(-1.0, 1.0))
        ).apply {
            letter = "~"
            color = Color(180, 150, 200, 220)  // Light purple
        }

        /** Convenience factory for randomize action */
        fun randomize(dist: ProbabilityDistribution = UniformRealDistribution(-1.0, 1.0)) = AdjustValueAction(
            amount = AdjustValueAmount.Random(dist)
        ).apply {
            letter = "R"
            color = Color(100, 200, 100, 220)  // Green
        }

        /** Convenience factory for increment action (add a value) */
        fun increment(v: Double = 0.1) = AdjustValueAction(
            amount = AdjustValueAmount.Value(v),
            operation = AdjustValueOperation.Add
        ).apply {
            letter = "+"
            color = Color(200, 200, 100, 220)  // Light yellow
        }

        /** Convenience factory for decrement action (subtract a value) */
        fun decrement(v: Double = 0.1) = AdjustValueAction(
            amount = AdjustValueAmount.Value(-v),
            operation = AdjustValueOperation.Add
        ).apply {
            letter = "-"
            color = Color(200, 100, 100, 220)  // Light red
        }

        /** Convenience factory for synapse strength action */
        fun synapseStrength(v: Double = 1.0) = AdjustValueAction(
            amount = AdjustValueAmount.Value(v),
            target = AdjustValueTarget.SynapseStrength
        ).apply {
            letter = "W"
            color = Color(255, 180, 100, 220)  // Orange
        }
    }
}

/**
 * Creates synapses from currently selected neurons to the neurons the wand touches.
 * Uses a configurable connection strategy to determine which connections to make and how to initialize weights.
 */
class ConnectFromSourceAction(
    connectionStrategy: ConnectionStrategy = AllToAll()
) : WandAction(), EditableObject {

    /**
     * The connection strategy determines which synapses to create and how to initialize their weights.
     */
    var connectionStrategy by GuiEditable(
        initValue = connectionStrategy,
        label = "Connection Strategy",
        description = "Strategy for creating connections from source neurons to touched neurons",
        order = 10
    )

    override val description: String get() = "Connect from selection (${connectionStrategy.name})"

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

        // Use the connection strategy to create synapses from sources to this target
        val newSynapses = connectionStrategy.connectNeurons(sourceNeurons, listOf(model))

        // Filter and add synapses
        for (synapse in newSynapses) {
            // Skip self-connections (strategy may allow them but wand typically shouldn't)
            if (synapse.source == synapse.target) continue

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

    override fun copy(): CopyableObject = ConnectFromSourceAction(connectionStrategy.copy()).also {
        it.letter = letter
        it.color = color
        it.radius = radius
    }

    override val name: String get() = "Connect from Selection"
}

/**
 * Creates synapses from the touched neuron to neighboring neurons within a radius.
 * Uses a configurable connection strategy to determine which connections to make and how to initialize weights.
 */
class ConnectToNeighborsAction(
    connectionStrategy: ConnectionStrategy = AllToAll(),
    connectionRadius: Double = 100.0
) : WandAction(), EditableObject {

    /**
     * The connection strategy determines which synapses to create and how to initialize their weights.
     */
    var connectionStrategy by GuiEditable(
        initValue = connectionStrategy,
        label = "Connection Strategy",
        description = "Strategy for creating connections to neighboring neurons",
        order = 10
    )

    /**
     * Radius within which to find target neurons for connection.
     */
    var connectionRadius by GuiEditable(
        initValue = connectionRadius,
        label = "Connection Radius",
        description = "Radius within which to connect to neighboring neurons",
        min = 1.0,
        order = 20
    )

    override val description: String get() = "Connect to neighbors (r=${"%.0f".format(connectionRadius)})"

    override var letter: String = "N"
    override var color: Color = Color(100, 200, 200, 220)  // Cyan

    @Transient
    private val createdSynapses = mutableListOf<Synapse>()

    override fun beginAction(networkPanel: NetworkPanel) {
        createdSynapses.clear()
    }

    override fun apply(model: NetworkModel, networkPanel: NetworkPanel, undoState: MutableMap<Any, Any?>) {
        if (model !is Neuron) return
        val network = networkPanel.network

        // Find all neurons within connectionRadius of the touched neuron
        val neighbors = model.getNeuronsInRadius(network.flatNeuronList, connectionRadius)

        // Use the connection strategy to create synapses from this neuron to neighbors
        val newSynapses = connectionStrategy.connectNeurons(listOf(model), neighbors)

        // Filter and add synapses
        for (synapse in newSynapses) {
            // Skip self-connections
            if (synapse.source == synapse.target) continue

            network.addNetworkModelAsync(synapse)
            createdSynapses.add(synapse)
        }
    }

    override fun endAction(networkPanel: NetworkPanel) {
        if (createdSynapses.isNotEmpty()) {
            val synapses = createdSynapses.toList()
            networkPanel.undoManager.addUndoableAction(
                description = "Wand: Created ${synapses.size} synapses to neighbors",
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

    override fun undoDescription(count: Int): String = "Wand: Connect to neighbors"

    override fun copy(): CopyableObject = ConnectToNeighborsAction(connectionStrategy.copy(), connectionRadius).also {
        it.letter = letter
        it.color = color
        it.radius = radius
    }

    override val name: String get() = "Connect to Neighbors"
}

/**
 * Prunes (deletes) synapses based on an absolute threshold.
 * Synapses are deleted immediately as the wand touches them.
 */
class PruneWeightsAction(
    threshold: Double = 0.5
) : WandAction(), EditableObject {

    var threshold by GuiEditable(
        initValue = threshold,
        label = "Threshold",
        description = "Remove synapses where |strength| < threshold",
        min = 0.0,
        order = 10
    )

    override val description: String
        get() = "Prune weights (|w| < ${"%.2f".format(threshold)})"

    override var letter: String = "P"
    override var color: Color = Color(200, 50, 50, 220)

    @Transient
    private val prunedSynapses = mutableListOf<Triple<Neuron, Neuron, Double>>()

    override fun beginAction(networkPanel: NetworkPanel) {
        prunedSynapses.clear()
    }

    override fun apply(model: NetworkModel, networkPanel: NetworkPanel, undoState: MutableMap<Any, Any?>) {
        if (model !is Synapse) return
        
        if (Math.abs(model.strength) < threshold) {
            prunedSynapses.add(Triple(model.source, model.target, model.strength))
            model.deleteBlocking()
        }
    }

    override fun endAction(networkPanel: NetworkPanel) {
        if (prunedSynapses.isNotEmpty()) {
            val synapseData = prunedSynapses.toList()
            networkPanel.undoManager.addUndoableAction(
                description = "Wand: Pruned ${synapseData.size} synapses",
                undo = {
                    synapseData.forEach { (source, target, strength) ->
                        networkPanel.network.addNetworkModelAsync(Synapse(source, target, strength))
                    }
                },
                redo = {
                    synapseData.forEach { (source, target, _) ->
                        source.fanOut[target]?.deleteBlocking()
                    }
                }
            )
            prunedSynapses.clear()
        }
    }

    override fun undoDescription(count: Int): String = "Wand: Prune weights"

    override fun copy(): CopyableObject = PruneWeightsAction(threshold).also {
        it.letter = letter
        it.color = color
    }

    override val name: String get() = "Prune Weights"
}
