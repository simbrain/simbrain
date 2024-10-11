package org.simbrain.network.core

import org.simbrain.network.events.NeuronArrayEvents
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.*
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import smile.stat.distribution.GaussianDistribution

/**
 * A "neuron array" backed by a Smile Matrix. Stored as a column vector.
 */
class NeuronArray(inputSize: Int) : ArrayLayer(inputSize), EditableObject, AttributeContainer {

    var updateRule: NeuronUpdateRule<ScalarDataHolder, MatrixDataHolder> by GuiEditable(
        initValue = LinearRule(),
        order = 100,
        typeMapProvider = NeuronUpdateRule<*, *>::getNeuronArrayTypeMap,
        setter = {
            val typeChanged = field::class != it::class
            field = it
            if (typeChanged) {
                dataHolder = updateRule.createMatrixData(size)
            }
            events.updated.fire()
        }
    )

    /**
     * Holds data for prototype rule.
     */
    var dataHolder: MatrixDataHolder by GuiEditable(
        initValue = updateRule.createMatrixData(inputSize),
        order = 99,
        onUpdate = {
            val proposedDataHolder = widgetValue(::updateRule).createMatrixData(size)
            if (widgetValue(::dataHolder)::class != proposedDataHolder::class) {
                refreshValue(proposedDataHolder)
            }
        }
    )

    /**
     * Array to hold activation values. These are also the outputs that are consumed by
     * other network components via [Layer]. A column vector.
     */
    @UserParameter(label = "Activations", description = "Neuron activations", order = 1)
    @get:Producible
    override var activations: Matrix = Matrix(inputSize, 1)
        set(newActivations) {
            field.copyFrom(newActivations)
            events.updated.fire()
        }

    @get:Producible
    @UserParameter("Bias Array", "Biases", order = 10)
    override var biases: Matrix = Matrix(inputSize, 1)
        set(newBiases) {
            field.copyFrom(newBiases)
            events.updated.fire()
        }

    @get:Producible
    override val biasArray: DoubleArray
        get() = biases.toDoubleArray()

    /**
     * see [AbstractNeuronCollection.spikes]
     */
    @get:Producible
    override val spikes: DoubleArray
        get() = (dataHolder as? SpikingMatrixData)?.spikes?.map { if (it) 1.0 else 0.0 }?.toDoubleArray() ?: DoubleArray(
            size
        )

    private var targets: Matrix? = null

    /**
     * Render an image showing each activation when true.
     */
    @UserParameter(label = "Show activations", description = "Whether to show activations as a pixel image", order = 4)
    var isRenderActivations = true

    @UserParameter(
        label = "Grid Mode", description = "If true, show activations as a grid, " +
                "otherwise show them as a line", order = 10
    )
    var gridMode = false
        set(value) {
            field = value
            (events as NeuronArrayEvents).visualPropertiesChanged.fire()
        }

    @UserParameter(
        label = "Vertical Layout",
        description = "If true, orient the array vertically, otherwise horizontally",
        order = 11
    )
    var verticalLayout = false
        set(value) {
            field = value
            (events as NeuronArrayEvents).visualPropertiesChanged.fire()
        }

    @UserParameter(label = "Biases Visible", description = "If true, show biases.", order = 12)
    var isShowBias = false
        set(showBias) {
            field = showBias
            events.visualPropertiesChanged.fire()
        }

    @Transient
    override var events: NeuronArrayEvents = NeuronArrayEvents()

    /**
     * Construct a neuron array.
     *
     * @param net  parent net
     * @param size number of components in the array
     */
    init {
        randomize()
    }

    /**
     * Make a deep copy of this array.
     *
     * @param newParent the new parent network
     * @return the deep copy
     */
    fun copy(): NeuronArray {
        val copy = NeuronArray(size)
        copy.location = location
        copy.gridMode = gridMode
        copy.verticalLayout = verticalLayout
        copy.activations.copyFrom(activations)
        copy.biases.copyFrom(biases)
        copy.updateRule = updateRule
        copy.dataHolder = dataHolder.copy()
        return copy
    }

    @get:Producible(arrayDescriptionMethod = "getLabelArray")
    override val activationArray: DoubleArray
        get() = activations.toDoubleArray()

    var targetValues: Matrix?
        get() = targets
        set(targets) {
            if (targets != null) {
                activations.validateSameShape(targets)
            }
            this.targets = targets
            events.updated.fire()
        }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        activations = Matrix.rand(
            size, 1,
            GaussianDistribution(0.0, 1.0)
        )
        events.updated.fire()
    }

    override val name: String
        get() = "Neuron Array"

    /**
     * Offset this neuron array
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    fun offset(offsetX: Double, offsetY: Double) {
        setLocation(locationX + offsetX, locationY + offsetY)
        events.updated.fire()
    }

    /**
     * Since Neuron Array is immutable, this object will be used in the creation dialog.
     */
    class CreationTemplate : EditableObject {
        /**
         * Size of the neuron array.
         */
        @UserParameter(label = "Nodes", description = "Number of nodes", order = 1)
        var numNodes = 100

        /**
         * Add a neuron array to network created from field values which should be setup by an Annotated Property
         * Editor.
         *
         * @param network the network this neuron array adds to
         * @return the created neuron array
         */
        fun create(): NeuronArray {
            return NeuronArray(numNodes)
        }

        override val name = "Neuron Array"

    }

    context(Network)
    override fun update() {
        if (isClamped) {
            return
        }
        updateRule.apply(this, dataHolder)
        inputs.mul(0.0) // clear inputs
        events.updated.fire()
    }

    @Consumable
    override fun setActivations(activations: DoubleArray) {
        this.activations = Matrix.column(activations)
    }

    /**
     * Set all activations in the array to the specified value.
     */
    fun fillActivations(value: Double) {
        this.activations.setColConstant(0, value)
    }


    fun fireLocationChange() {
        events.locationChanged.fire()
    }

    override fun toString(): String {
        return """
            $id with ${this.activations.size()} components.
            Activations: ${Utils.getTruncatedArrayString(activationArray, 10)}
            $dataHolder
        """.trimIndent()
    }

    override fun clear() {
        activations.setColConstant(0, 0.0)
        inputs.setColConstant(0, 0.0)
        events.updated.fire()
    }

    override fun increment() {
        activations.add(increment)
        events.updated.fire()
    }

    override fun decrement() {
        activations.sub(increment)
        events.updated.fire()
    }


    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    override fun readResolve(): Any? {
        events = NeuronArrayEvents()
        return this
    }

    /**
     * For each incoming psr matrix, filter out entries that correspond to excitatory synapses and return the sum of those for
     * each row (i.e. a vector of excitatory inputs; summed PSRs for each “dendrite”).
     */
    val excitatoryInputs: DoubleArray
        get() = incomingConnectors
            .filterIsInstance<WeightMatrix>()
            .map { it.psrMatrix.clone().mul(it.excitatoryMask).rowSums() }
            .reduceOrNull { base, add -> SimbrainMath.addVector(base, add) }
            ?: DoubleArray(size)

    /**
     * For each incoming psr matrix, filter out entries that correspond to inhibitory synapses and return the sum of those for
     * each row (i.e. a vector of inhibitory inputs; summed PSRs for each “dendrite”).
     */
    val inhibitoryInputs: DoubleArray
        get() = incomingConnectors
            .filterIsInstance<WeightMatrix>()
            .map { it.psrMatrix.clone().mul(it.inhibitoryMask).rowSums() }
            .reduceOrNull { base, add -> SimbrainMath.addVector(base, add) }
            ?: DoubleArray(size)

    fun getLabelArray() = (0 until size).map { it.toString() }.toTypedArray()
}