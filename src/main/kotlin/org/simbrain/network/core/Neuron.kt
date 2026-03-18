package org.simbrain.network.core

import org.simbrain.network.events.NeuronEvents
import org.simbrain.network.gui.createTooltipText
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.updaterules.interfaces.ClippedUpdateRule
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.UserParameter
import org.simbrain.util.format
import org.simbrain.util.plus
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import org.simbrain.workspace.couplings.HIGH_PRIORITY
import org.simbrain.workspace.couplings.LOW_PRIORITY
import java.awt.geom.Point2D

/**
 * **Neuron** represents a node in the neural network. Most of the "logic" of
 * the neural network occurs here, in the update function. Subclasses must
 * override update and duplicate (for copy / paste) and cloning generally.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
class Neuron : LocatableModel, EditableObject, AttributeContainer {

    @XStreamConstructor
    @JvmOverloads
    constructor(updateRule: NeuronUpdateRule<*, *> = LinearRule()) {
        this.updateRule = updateRule
    }

    /**
     * Copy constructor.
     */
    constructor(n: Neuron) {
        priority = n.priority
        updateRule = n.updateRule.copy()
        dataHolder = n.dataHolder.copy()
        clamped = n.clamped
        increment = n.increment
        activation = n.activation
        bias = n.bias
        polarity = n.polarity
        x = n.x
        y = n.y
        label = n.label
    }

    /**
     * The update method of this neuron, which corresponds to what kind of
     * neuron it is.
     */
    @UserParameter(
        label = "Update rule",
        description = "Neuron update rule, which determines how activations evolve and respond to inputs.",
        order = 100
    )
    var updateRule: NeuronUpdateRule<*, *> = LinearRule()
        set(value) {
            val oldRule = field
            field = value
            if (oldRule::class != value::class) {
                dataHolder = value.createScalarData()
            }
            events.updateRuleChanged.fire(oldRule, value)
        }

    /**
     * Activation value of the neuron. The main state variable.
     */
    @UserParameter(
        label = "Activation",
        description = "Neuron activation. If you want a value greater" +
                " than upper bound or less than lower bound you must set those first, and close this dialog.",
        increment = .5,
        probDist = "Normal",
        order = 1
    )
    @get:Producible
    @set:Consumable(defaultVisibility = false, customPriorityMethod = "setActivationCouplingPriority")
    var activation = 0.0
        set(value) {
            val lastActivation = field
            field = value
            events.activationChanged.fire(lastActivation, value)
        }

    @get:Producible(defaultVisibility = false)
    @set:Consumable(defaultVisibility = false)
    @UserParameter("Bias", "Constant value added to weighted input", order = 10)
    var bias = 0.0

    /**
     * Amount to increment/decrement activation when manually adjusted.
     */
    @UserParameter(
        label = "Increment",
        description = "Amount that a neuron is incremented / decremented when it is manually adjusted.",
        increment = .5,
        order = 6
    )
    var increment: Double = 0.1

    /**
     * Whether or not this neuron has spiked. Specifically if the result of
     * integration of a spiking neuron update rule at time t, produced an action
     * potential at time t+1. True on t+1 in that case. Always false for
     * non-spiking neuron update rules.
     */
    context(Network)
    var isSpike: Boolean
        get() = (dataHolder as? SpikingScalarData)?.spiked ?: false
        set(spike) {
            (dataHolder as? SpikingScalarData)?.setHasSpiked(spike)
            events.spiked.fire(spike)
        }

    /**
     * Aggregates all inputs (from other nodes, couplings, or scripts) to this node.
     *
     * See description at [addInputValue].
     *
     * Note that when [accumulateInputs] is called, [weightedInputs] are added to input.
     */
    var input: Double = 0.0
        private set

    /**
     * Fan-out in the form of a map from target neurons to synapses.
     */
    @Transient
    var fanOut: MutableMap<Neuron, Synapse> = HashMap()
        private set

    /**
     * List of synapses attaching to this neuron.
     */
    @Transient
    var fanIn: ArrayList<Synapse> = ArrayList()
        private set

    /**
     * Central x-coordinate of this neuron in 2-space.
     */
    var x = 0.0

    /**
     * Central y-coordinate of this neuron in 2-space.
     */
    var y = 0.0

    /**
     * z-coordinate of this neuron in 3-space. Currently no GUI implementation,
     * but fully useable for scripting. Like polarity this will get a full
     * implementation in the next development cycle... probably by 4.0.
     */
    var z = 0.0

    /**
     * If true then do not update this neuron.
     */
    @UserParameter(
        label = "Clamped",
        description = "In general, a clamped neuron will not change over time; it is \"clamped\" to its current value.",
        order = 3
    )
    var clamped = false
        set(value) {
            field = value
            events.clampChanged.fire()
        }

    /**
     * The polarity of this neuron (excitatory, inhibitory, or none, which is
     * null). Used in synapse randomization, and in adding synapses.
     */
    @UserParameter(label = "Polarity", order = 10)
    var polarity: Polarity = Polarity.BOTH
        set(value) {
            field = value
            fanOut.values.filterNotNull().forEach { s -> s.strength = field.value(s.strength) }
            events.colorChanged.fire()
        }

    /**
     * An auxiliary value associated with a neuron. Getting and setting these
     * values can be useful in scripts.
     */
    var auxValue: Double = 0.0

    /**
     * Support for property change events.
     */
    @Transient
    override var events: NeuronEvents = NeuronEvents()
        private set

    /**
     * Local data holder for neuron update rule.
     */
    var dataHolder: ScalarDataHolder by GuiEditable(
        initValue = updateRule.createScalarData(),
        label = "State variables",
        showDetails = false,
        order = 100,
        onUpdate = {
            val proposedDataHolder = widgetValue(::updateRule).createScalarData()
            if (widgetValue(::dataHolder)::class != proposedDataHolder::class) {
                refreshValue(proposedDataHolder)
            }
        }
    )

    fun copy(): Neuron = Neuron(this)

    /**
     * Returns the time type of this neuron's update rule.
     *
     * @return the time type.
     */
    val timeType: Network.TimeType
        get() = updateRule.timeType

    /**
     * Returns the current update rule's description (name).
     *
     * @return the neuronUpdateRule's description
     */
    val updateRuleDescription: String
        get() = updateRule.name

    /**
     * Change the current update rule but perform no other initialization.
     */
    fun changeUpdateRule(updateRule: NeuronUpdateRule<*, *>, data: ScalarDataHolder) {
        this.updateRule = updateRule
        this.dataHolder = data
    }

    fun clip() {
        if (updateRule is ClippedUpdateRule) {
            activation = (updateRule as ClippedUpdateRule).clip(activation)
        }
    }

    context(Network)
    override fun accumulateInputs() {
        fanIn.forEach { it.updatePSR() }
        addInputValue(weightedInputs)
        addInputValue(bias)
    }

    /**
     * Accumulate only fanIn (synapse) inputs without adding bias.
     * Used when bias is already added via NeuronCollection.accumulateInputs().
     */
    context(Network)
    fun accumulateFanInInputs() {
        fanIn.forEach { it.updatePSR() }
        addInputValue(weightedInputs)
    }

    context(Network)
    override fun update() {
        if (isSpike) {
            isSpike = false
        }
        if (clamped) {
            return
        }
        updateRule.apply(this, dataHolder)
        input = 0.0
    }

    /**
     * @return the fan out map. Unsafe because the fan out map and the returned map are the same and thus modifications
     * to one will affect the other. Here for performance reasons.
     */
    val fanOutUnsafe: Map<Neuron, Synapse?>
        get() = fanOut

    /**
     * @return the fan in list. Unsafe because the fan in list and the returned list are the same and thus modifications
     * to one will affect the other. Here for performance reasons.
     */
    val fanInUnsafe: List<Synapse>
        get() = fanIn

    /**
     * Adds an efferent (outgoing) synapse to this neuron, i.e. adds a synapse to
     * [.fanOut]. Used when constructing synapses. Should not be called directly
     *
     * Does **NOT** add this synapse to the network or any
     * intermediate bodies. If the connection is a duplicate connection the
     * original synapse connecting this neuron to a target neuron will be
     * removed and replaced by *Synapse s*.
     *
     * @param synapse the synapse for which this neuron is a source to add.
     */
    fun addToFanOut(synapse: Synapse) {
        if (fanOut != null) {
            fanOut[synapse.target] = synapse
        }
    }

    /**
     * Remove an efferent (outgoing) weight from this neuron. Used by synapse but should not generally be called
     * directly.
     */
    fun removeFromFanOut(synapse: Synapse) {
        if (fanOut != null) {
            fanOut.remove(synapse.target)
        }
    }

    /**
     * Adds an afferent (incoming) synapse to this neuron, i.e. adds a synapse to
     * [.fanIn]. Used when constructing synapses. Should not be called directly
     *
     * Does **NOT** add this synapse to the network or any intermediate bodies.
     */
    fun addToFanIn(source: Synapse) {
        if (fanIn != null) {
            fanIn.add(source)
        }
    }

    /**
     * Remove an afferent (incoming) weight from this neuron. Used by synapse but should not generally be called
     * directly.
     */
    fun removeFromFanIn(synapse: Synapse) {
        fanIn.remove(synapse)
    }

    /**
     * Sums the weighted inputs to this node, by summing the ouptut from incoming synapses,
     * which can either be connectionist (weight times source activation) or the output of a spike responder.
     *
     * Does not incorporate bias, though that is often in the definition of weighted inputs.
     * That occurs at [accumulateInputs]
     */
    val weightedInputs: Double
        get() {
            var wtdSum = 0.0
            for (synapse in fanIn) {
                wtdSum += synapse.psr
            }
            return wtdSum
        }

    /**
     * Returns the sum of post-synaptic responses of all incoming neurons connected to this one by negative weights.
     * This automatically includes neurons whose polarity is excitatory, since they only produce positive outgoing
     * weights.
     */
    val excitatoryInputs: Double
        get() = fanIn.stream()
            .filter { s: Synapse -> s.strength > 0.0 }
            .map { obj: Synapse -> obj.psr }
            .reduce { a: Double, b: Double -> java.lang.Double.sum(a, b) }.orElse(0.0)

    /**
     * Returns the sum of post-synaptic responses of all incoming neurons connected to this one by negative weights.
     * This automatically includes neurons whose polarity is inhibitory, since they only produce negative outgoing
     * weights.
     */
    val inhibitoryInputs: Double
        get() = fanIn.filter { it.strength < 0.0 }
            .sumOf { it.psr }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        activation = updateRule.getRandomValue(randomizer)
    }

    /**
     * Sends relevant information about the network to standard output.
     */
    fun debug() {
        println("neuron $id")
        println("fan in")

        fanIn.forEachIndexed { i, syn -> println("fanIn [$i]:$syn") }

        println("fan out")

        fanOut.values.forEachIndexed { i, syn -> println("fanOut [$i]:$syn") }
    }

    /**
     * Add to the input value of the neuron. When external components (like input tables) send activation to the
     * network they should use this. Called in couplings (by reflection) to allow multiple values to be added each
     * time step to a neuron. Inputs are cleared each time step.
     */
    @Consumable(description = "Add activation", customPriorityMethod = "addInputValueCouplingPriority")
    fun addInputValue(toAdd: Double) {
        input += toAdd
    }

    val type: String
        /**
         * The name of the update rule of this neuron; it's "type". Used via
         * reflection for consistency checking in the gui. (Open multiple neurons
         * and if they are of the different types the dialog is different).
         *
         * @return the name of the class of this network.
         */
        get() = updateRule.javaClass.simpleName

    val summedIncomingWeights: Double
        /**
         * Returns the sum of the strengths of the weights attaching to this neuron.
         *
         * @return the sum of the incoming weights to this neuron.
         */
        get() {
            var ret = 0.0

            for (i in fanIn.indices) {
                val tempRef = fanIn[i]
                ret += tempRef.strength
            }

            return ret
        }


    /**
     * @return the average activation of neurons connecting to this neuron
     */
    val averageInput: Double get() = totalInput / fanIn.size

    /**
     * @return the total activation of neurons connecting to this neuron
     */
    val totalInput: Double get() = fanIn.sumOf { it.source.activation }

    /**
     * True if the synapse is connected to this neuron, false otherwise.
     *
     * @param s the synapse to check.
     * @return true if synapse is connected, false otherwise.
     */
    fun isConnected(s: Synapse): Boolean {
        return (fanIn.contains(s) || fanOut[s.target] != null)
    }

    /**
     * Delete connected synapses and remove them from the network and any other
     * structures.
     */
    private suspend fun deleteConnectedSynapses(): List<Synapse> {
        return buildList {
            addAll(deleteFanIn())
            addAll(deleteFanOut())
        }
    }

    /**
     * Removes all synapses from fanOut and from the network or any intermediate
     * structures.
     */
    private suspend fun deleteFanOut(): List<Synapse> {
        return fanOut.toList().also {
            it.forEach { (target, synapse) ->
                synapse.delete()
                fanOut.remove(target)
            }
        }.map { it.second }
    }

    /**
     * Removes all synapses from fanIn and from the network or any intermediate
     * structures.
     */
    private suspend fun deleteFanIn(): List<Synapse> {
        return fanIn.toList().also { it.forEach { synapse ->
            synapse.delete()
            fanIn.remove(synapse)
        } }
    }

    override fun toString(): String {
        return """
            Name: $displayName
            Update rule: $type 
            Activation: ${activation.format(NetworkPreferences.tooltipDecimalPlaces)}
        """.trimIndent()
    }

    override fun clear() {
        input = 0.0
        activation = 0.0
        dataHolder.clear()
        // Clears any delayed psr's
        fanIn.forEach { s -> s.clear() }
    }

    fun clearInput() {
        input = 0.0
    }

    override fun increment() {
        updateRule.contextualIncrement(this)
    }

    override fun decrement() {
        updateRule.contextualDecrement(this)
    }

    override fun toggleClamping() {
        clamped = !clamped
    }

    /**
     * Forward to update rule's tool tip method, which returns string for tool
     * tip or short description.
     *
     * @return tool tip text
     */
    val toolTipText: String get() = createTooltipText(this) {
        updateRule.getToolTipText(this)
    }

    /**
     * Randomize all synapses that attach to this neuron.
     */
    fun randomizeFanIn(randomizer: ProbabilityDistribution?) {
        for (synapse in fanIn) {
            synapse.randomize(randomizer)
        }
    }

    /**
     * Randomize all synapses that attach to this neuron.
     */
    context(Network)
    fun randomizeFanOut() {
        for (synapse in fanOut.values) {
            synapse.randomize()
        }
    }

    var upperBound: Double
        /**
         * Return the upper bound for the underlying rule, if it is bounded.
         * Else it simply returns a "graphical" upper bound. Used to color neuron
         * activations.
         *
         * @return the upper bound, if applicable, and 1 otherwise.
         */
        get() = if (updateRule is BoundedUpdateRule) {
            (updateRule as BoundedUpdateRule).upperBound
        } else {
            updateRule.graphicalUpperBound
        }
        /**
         * Convenience method to set upper bound on the neuron's update rule, if it
         * is a bounded update rule.
         *
         * @param upperBound upper bound to set.
         */
        set(upperBound) {
            if (updateRule is BoundedUpdateRule) {
                (updateRule as BoundedUpdateRule).upperBound = upperBound
            } else {
                throw IllegalStateException("Cannot set upper bound on unbounded update rule.")
            }
        }

    var lowerBound: Double
        /**
         * Return the lower bound for the underlying rule, if it is bounded.
         * Else it simply returns the "graphical" lower bound. Used to color neuron
         * activations.
         *
         * @return the upper bound, if applicable, and -1 otherwise.
         */
        get() = if (updateRule is BoundedUpdateRule) {
            (updateRule as BoundedUpdateRule).lowerBound
        } else {
            updateRule.graphicalLowerBound
        }
        /**
         * Convenience method to set lower bound on the neuron's update rule, if it
         * is a bounded update rule.
         *
         * @param lowerBound lower bound to set.
         */
        set(lowerBound) {
            if (updateRule is BoundedUpdateRule) {
                (updateRule as BoundedUpdateRule).lowerBound = lowerBound
            } else {
                throw IllegalStateException("Cannot set lower bound on unbounded update rule.")
            }
        }

    val lastSpikeTime: Double
        get() = (dataHolder as SpikingScalarData).lastSpikeTime

    override var location: Point2D
        get() = Point2D.Double(x, y)
        set(position) {
            setLocation(position, true)
        }

    fun setLocation(position: Point2D, fireEvent: Boolean) {
        x = position.x
        y = position.y
        if (fireEvent) {
            events.locationChanged.fire()
        }
    }

    fun setLocation(x: Double, y: Double, fireEvent: Boolean) {
        setLocation(point(x, y), fireEvent)
    }

    var position3D: DoubleArray
        get() = doubleArrayOf(x, y, z)
        /**
         * Convenience method for setting the xyz coordinates from
         * an array with (at least) 3 values. Elements beyond position
         * 2 will be ignored.
         *
         * @param xyz - array of coordinate values {x, y, z}
         */
        set(xyz) {
            setPosition3D(xyz[0], xyz[1], xyz[2])
        }

    /**
     * Convenience method for setting location in 3D rather than just 2D
     * space.
     */
    fun setPosition3D(x: Double, y: Double, z: Double) {
        this.x = x
        this.y = y
        this.z = z
    }

    /**
     * Translate the neuron by a specified amount.
     *
     * @param deltaX x amount to translate neuron
     * @param deltaY y amount to translate neuron
     */
    @JvmOverloads
    fun offset(deltaX: Double, deltaY: Double, fireEvent: Boolean = true) {
        val delta = point(deltaX, deltaY)
        setLocation(location.plus(delta), fireEvent)
    }

    override val name: String
        get() = id!!

    override suspend fun delete(): List<NetworkModel> {
        val synapses = deleteConnectedSynapses()
        events.deleted.fire(this).await()
        return buildList<NetworkModel> {
            add(this@Neuron)
            addAll(synapses)
        }
    }

    /**
     * When the neuron is not clamped, couplings should use add inputs.  Called by reflection using
     * [Consumable.customPriorityMethod]
     */
    fun addInputValueCouplingPriority(): Int {
        return if (clamped) {
            LOW_PRIORITY
        } else {
            HIGH_PRIORITY
        }
    }


    /**
     * When the neuron is clamped, couplings should use force set activation.  Called by reflection using
     * [Consumable.customPriorityMethod]
     */
    fun setActivationCouplingPriority(): Int {
        return if (clamped) {
            HIGH_PRIORITY
        } else {
            LOW_PRIORITY
        }
    }

}
