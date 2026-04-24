package org.simbrain.network.core

import org.simbrain.network.events.NeuronCollectionEvents
import org.simbrain.network.gui.nodes.ActivationSequenceProcessor
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.Layout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.trainers.StructuredProbe
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.updaterules.interfaces.DifferentiableUpdateRule
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.*
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import kotlin.math.min

/**
 * A collection of free neurons. Allows them to be labelled, moved around as a unit, coupled to, etc.
 * No special processing occurs in neuron collections. They are a convenience.
 * NeuronCollections can overlap each other in the sense of having neurons in common.
 *
 * Subclasses maintain lists of neurons and can copy their activations to matrices. To communicate with other
 * [Layer]s it can create output matrices and accept input matrices, but it will only create and cache these if
 * relevant methods are called. Matrix based layers should subclass [ArrayLayer].
 */
class NeuronCollection : Layer, CopyableObject {

    @Transient
    var incomingSgs: HashSet<SynapseGroup> = HashSet()
        private set

    @Transient
    var outgoingSg: HashSet<SynapseGroup> = HashSet()
        private set

    @Transient
    override val events: NeuronCollectionEvents = NeuronCollectionEvents()

    @get:Producible(arrayDescriptionMethod = "getLabelArray")
    @set:Consumable
    @UserParameter("Activation Array", "Activations", order = 10)
    override var activationArray: DoubleArray
        get() = neuronList
            .map { it.activation }
            .toDoubleArray()
        set(activations) {
            val size = min(activations.size, neuronList.size)
            for (i in 0 until size) {
                neuronList[i].activation = activations[i]
            }
        }

    @UserParameter("Bias Array", "Biases", order = 20)
    override var biasArray: DoubleArray
        get() = neuronList
            .map { it.bias }
            .toDoubleArray()
        set(biases) {
            val size = min(biases.size, neuronList.size)
            for (i in 0 until size) {
                neuronList[i].bias = biases[i]
            }
        }

    override var biases: Matrix
        get() = Matrix.column(biasArray)
        set(value) {
            biasArray = value.toDoubleArray()
        }

    @get:Producible
    override val spikes: DoubleArray
        get() = neuronList.map {
            if ((it.dataHolder as? SpikingScalarData)?.spiked == true) 1.0 else 0.0
        }.toDoubleArray()

    override val inputs: Matrix get() = Matrix.column(inputArray)

    /**
     * References to neurons in this collection
     */
    val neuronList: CopyOnWriteArrayList<Neuron> = CopyOnWriteArrayList()

    /**
     * Set up listeners for all neurons in the list. Call this after deserialization.
     */
    fun setupNeuronListeners() {
        neuronList.forEach { neuron ->
            addListener(neuron)
        }
    }

    /**
     * Space between neurons within a layer.
     */
    var betweenNeuronInterval: Int = 50

    /**
     * In method setLayoutBasedOnSize, this is used as the threshold number of neurons in the group, above which to use
     * grid layout instead of line layout.
     */
    var gridThreshold: Int = 9

    /**
     * The layout for the neurons in this group.
     */
    var layout: Layout = GridLayout()

    override var activations: Matrix
        get() = Matrix.column(this.activationArray)
        set(value) {
            setActivations(value.toDoubleArray())
        }

    override var isClamped: Boolean
        get() = isAllClamped
        set(value) {
            isAllClamped = value
        }

    override fun addInputs(inputs: Matrix) {
        addInputs(inputs.col(0))
    }

    @Consumable
    override fun setActivations(activations: DoubleArray) {
        this.activationArray = activations
    }

    /**
     * Set input values of neurons using an array of doubles.
     */
    @Consumable
    fun addInputs(inputs: DoubleArray) {
        val size = min(inputs.size.toDouble(), neuronList.size.toDouble()).toInt()
        for (i in 0 until size) {
            neuronList[i].addInputValue(inputs[i])
        }
        invalidateCachedInputs()
    }

    private var cachedInputsDirty = true
    private var _cachedInputs = DoubleArray(0)

    @get:Producible
    val inputArray: DoubleArray
        get() {
            if (cachedInputsDirty) {
                _cachedInputs = neuronList
                    .map { it.input }
                    .toDoubleArray()
            }
            return _cachedInputs
        }

    override val size: Int get() = activationArray.size

    val centerX: Double
        get() = neuronList.centerLocation.x

    val centerY: Double
        get() = neuronList.centerLocation.y

    override var location: Point2D
        get() = neuronList.centerLocation
        set(newLocation) {
            val delta = newLocation - location
            neuronList.forEach { it.location += delta }
            events.locationChanged.fire()
        }

    override val bound: Rectangle2D
        get() = neuronList.bound

    val sides: RectangleSides
        get() = neuronList.sides

    val maxDim: Double
        get() = if (width > height) width else height

    override val updateRule: NeuronUpdateRule<*, *>
        get() = neuronList.firstNotNullOf { it.updateRule }

    // Constructors

    constructor(): super()

    constructor(neurons: List<Neuron>): super() {
        addNeurons(neurons.sortLeftRightTopBottom())
    }

    // Neuron management

    fun offset(offsetX: Double, offsetY: Double) {
        for (neuron in neuronList) {
            neuron.offset(offsetX, offsetY, false)
        }
        events.locationChanged.fire()
    }

    fun getNeuron(i: Int): Neuron = neuronList[i]

    fun addNeuron(neuron: Neuron) {
        neuronList.add(neuron)
        addListener(neuron)
    }

    private fun addNeurons(neurons: Collection<Neuron>) {
        neurons.forEach { this.addNeuron(it) }
    }

    internal fun addListener(n: Neuron) {
        n.events.locationChanged.on { events.locationChanged.fire() }
        n.events.deleted.on(wait = true) { neuron ->
            neuronList.remove(neuron)
            if (isEmpty) {
                delete()
            }
        }
    }

    fun removeNeuron(neuron: Neuron?) {
        neuronList.remove(neuron)
    }

    fun removeAllNeurons() {
        neuronList.clear()
    }

    fun containsNeuron(n: Neuron?): Boolean = neuronList.contains(n)

    // Update rule

    fun setNeuronType(base: NeuronUpdateRule<*, *>) {
        neuronList.forEach(Consumer { n: Neuron -> n.updateRule = base.copy() })
    }

    fun setNeuronType(rule: String) {
        try {
            val newRule =
                Class.forName("org.simbrain.network.neuron_update_rules.$rule").newInstance() as NeuronUpdateRule<*, *>
            setNeuronType(newRule)
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    // Randomization

    override fun randomize(randomizer: ProbabilityDistribution?) {
        neuronList.forEach { it.randomize(randomizer) }
    }

    fun randomizeBiases() {
        for (neuron in neuronList) {
            neuron.randomizeBias()
        }
    }

    val incomingWeights: List<Synapse>
        get() = neuronList.flatMap { it.fanIn }

    val outgoingWeights: List<Synapse>
        get() = neuronList.flatMap { it.fanOut.values }

    open fun randomizeIncomingWeights(randomizer: ProbabilityDistribution? = null) {
        for (neuron in neuronList) {
            neuron.randomizeFanIn(randomizer)
        }
    }

    context(Network)
    fun randomizeOutgoingWeights() {
        for (neuron in neuronList) {
            neuron.randomizeFanOut()
        }
    }

    // Synapse group management

    fun removeIncomingSg(sg: SynapseGroup): Boolean = incomingSgs.remove(sg)

    fun removeOutgoingSg(sg: SynapseGroup): Boolean = outgoingSg.remove(sg)

    // Delete

    override suspend fun delete(): List<NetworkModel> {
        return buildList {
            addAll(super.delete())
            addAll(outgoingSg.flatMap { it.delete() })
            addAll(incomingSgs.flatMap { it.delete() })
            val customInfo = customInfo
            customInfo?.events?.deleted?.fire(customInfo)?.await()
        }
    }

    // Accumulate inputs

    context(Network)
    override fun accumulateInputs() {
        super.accumulateInputs()
        val wtdInputs = DoubleArray(size)
        for (c in incomingConnectors) {
            val summedPSRs = c.getSummedPSRs()
            wtdInputs.addi(summedPSRs)
        }
        addInputs(wtdInputs)

        // Only add bias if individual neurons are not being updated separately
        val freeNeuronsInNetwork = getModels<Neuron>()
        val hasDuplicateNeurons = neuronList.any { neuron -> freeNeuronsInNetwork.contains(neuron) }
        if (!hasDuplicateNeurons) {
            addInputs(biasArray)
        }
    }

    // Clamping

    var isAllClamped: Boolean
        get() = neuronList.none { !it.clamped }
        set(value) {
            neuronList.forEach { it.clamped = value }
        }

    val isAllUnclamped: Boolean
        get() = neuronList.none { it.clamped }

    fun setLowerBound(lb: Double) {
        for (neuron in neuronList) { neuron.lowerBound = lb }
    }

    fun setUpperBound(ub: Double) {
        for (neuron in neuronList) { neuron.upperBound = ub }
    }

    fun setIncrement(increment: Double) {
        for (neuron in neuronList) { neuron.increment = increment }
    }

    // Labels

    fun getLabelsOfActiveNeurons(threshold: Double): String {
        val strBuilder = StringBuilder("")
        for (neuron in neuronList) {
            if ((neuron.activation > threshold) && (!neuron.label.isNullOrBlank())) {
                strBuilder.append(neuron.label + " ")
            }
        }
        return strBuilder.toString()
    }

    val mostActiveNeuron: String
        get() = (neuronList.maxBy { it.activation }.label ?: "") + " "

    fun setPolarity(p: Polarity) {
        neuronList.forEach { it.polarity = p }
    }

    fun getNeuronByLabel(label: String?) = neuronList.firstOrNull { it.label.equals(label, ignoreCase = true) }

    private fun invalidateCachedInputs() {
        cachedInputsDirty = true
    }

    val labelArray: Array<String?>
        get() = neuronList
            .map { if (it.label.isNullOrEmpty()) it.id else it.label }
            .toTypedArray()

    val isEmpty: Boolean
        get() = neuronList.isEmpty()

    val minX: Double get() = neuronList.minX
    val maxX: Double get() = neuronList.maxX
    val minY: Double get() = neuronList.minY
    val maxY: Double get() = neuronList.maxY

    override val name = "Neuron Collection"

    override fun onCommit() {}

    override fun toString(): String {
        return """
            Name: $displayName ($size neurons)
            Activations: ${Utils.getTruncatedArrayString(activationArray, 10)}
            Location: ${location.format(2)}
        """.trimIndent()
    }

    override fun clearInputs() {
        neuronList.forEach { it.clearInput() }
        invalidateCachedInputs()
    }

    override fun clear() {
        for (n in neuronList) { n.clear() }
    }

    override fun increment() {
        neuronList.forEach { it.increment() }
    }

    override fun decrement() {
        neuronList.forEach { it.decrement() }
    }

    override fun toggleClamping() {
        neuronList.forEach { it.toggleClamping() }
    }

    // Layout

    @JvmOverloads
    fun setLayoutBasedOnSize(initialPosition: Point2D = point(0, 0)) {
        val lineLayout = LineLayout(betweenNeuronInterval.toDouble(), LineLayout.LineOrientation.HORIZONTAL)
        val gridLayout = GridLayout(betweenNeuronInterval.toDouble(), betweenNeuronInterval.toDouble())
        if (neuronList.size < gridThreshold) {
            lineLayout.setInitialLocation(initialPosition)
            layout = lineLayout
        } else {
            gridLayout.setInitialLocation(initialPosition)
            layout = gridLayout
        }
        layout.layoutNeurons(neuronList)
    }

    val topLeftLocation: Point2D.Double
        get() = neuronList.topLeftLocation

    fun applyLayout() {
        layout.setInitialLocation(this.topLeftLocation)
        layout.layoutNeurons(neuronList)
    }

    fun applyLayout(initialPosition: Point2D?) {
        layout.setInitialLocation(initialPosition)
        layout.layoutNeurons(neuronList)
    }

    fun applyLayout(x: Int, y: Int) {
        applyLayout(Point2D.Double(x.toDouble(), y.toDouble()))
    }

    fun applyLayout(newLayout: Layout) {
        layout = newLayout
        applyLayout(location)
    }

    // Convenience layout method

    fun layout(layout: Layout) {
        layout.layoutNeurons(neuronList)
    }

    // Duplicate detection

    val summedNeuronHash: Int
        get() = neuronList.stream().mapToInt { obj: Neuron -> obj.hashCode() }.sum()

    context(Network)
    override fun shouldAdd(): Boolean {
        val hashCode = summedNeuronHash
        for (other in getModels(NeuronCollection::class.java)) {
            if (hashCode == other.summedNeuronHash) {
                return false
            }
        }
        return true
    }

    // Backprop

    override fun processError(
        error: Matrix,
        signalSource: Layer,
        biasesAccumulator: HashMap<Layer, Matrix>,
        rawMatrixAccumulator: HashMap<Matrix, Matrix>,
        probe: StructuredProbe?
    ): Matrix {
        if (signalSource is ActivationSequenceProcessor) {
            throw UnsupportedOperationException("ActivationSequenceProcessor not supported")
        }
        val processErrorProbe = probe?.createMapProbe("processError")
        (neuronList.firstNotNullOf { it.updateRule } as? DifferentiableUpdateRule)?.getDerivative(inputs)?.let { deriv ->
            processErrorProbe?.write("deriv", deriv)
            error.mul(deriv)
        }
        processErrorProbe?.write("error") { error.clone() }
        biasesAccumulator.getOrPut(this) {
            Matrix(size, 1)
        }.add(error)
        return error
    }

    // Custom info

    open val customInfo: NetworkModel?
        get() = null

    // Serialization

    override suspend fun afterRestore(context: Any?) {
        super.afterRestore(context)
        val sortedNeuronList = neuronList.sortLeftRightTopBottom()
        neuronList.clear()
        neuronList.addAll(sortedNeuronList)
    }

    // Copy

    override fun copy(): NeuronCollection {
        return NeuronCollection(neuronList.map(Neuron::copy)).also {
            it.commonCopyFrom(this)
        }
    }
}
