package org.simbrain.network.core

import org.simbrain.network.events.LocationEvents
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
import java.awt.geom.Rectangle2D

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
                dataHolder = updateRule.createMatrixData(size())
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
            val proposedDataHolder = widgetValue(::updateRule).createMatrixData(size())
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
    var activations: Matrix = Matrix(inputSize, 1)
        set(newActivations) {
            field.copyFrom(newActivations)
            events.updated.fire()
        }

    /**
     * see [AbstractNeuronCollection.spikes]
     */
    @get:Producible
    override val spikes: DoubleArray
        get() = (dataHolder as? SpikingMatrixData)?.spikes?.map { if (it) 1.0 else 0.0 }?.toDoubleArray() ?: DoubleArray(size())

    @get:Producible
    override val outputs: Matrix get() = activations

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

    @UserParameter(label = "Biases Visible", description = "If true, show biases.", order = 11)
    var isShowBias = false
        set(showBias) {
            field = showBias
            (events as NeuronArrayEvents).visualPropertiesChanged.fire()
        }

    @Transient
    override var events: LocationEvents = NeuronArrayEvents()

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
        val copy = NeuronArray(outputSize())
        copy.location = location
        copy.gridMode = gridMode
        copy.activations.copyFrom(activations)
        copy.updateRule = updateRule
        copy.dataHolder = dataHolder!!.copy()
        return copy
    }

    @get:Producible(arrayDescriptionMethod = "getLabelArray")
    val activationArray: DoubleArray
        get() = activations.toDoubleArray()

    var targetValues: Matrix?
        get() = targets
        set(targets) {
            if (targets != null) {
                outputs.validateSameShape(targets)
            }
            this.targets = targets
            events.updated.fire()
        }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        activations = Matrix.rand(
            size(), 1,
            GaussianDistribution(0.0, 1.0)
        )
        events.updated.fire()
    }

    override val bound: Rectangle2D
        get() = Rectangle2D.Double(
            locationX - width / 2, locationY - height / 2,
            width, height
        )

    override fun onCommit() {
        events.labelChanged.fire("", label!!)
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
    override fun applyActivations(activations: DoubleArray) {
        this.activations = Matrix.column(activations)
    }

    /**
     * Set all activations in the array to the specified value.
     */
    fun fillActivations(value: Double) {
        activations.setCol(0, value)
    }


    fun fireLocationChange() {
        events.locationChanged.fire()
    }

    /**
     * Input and output size are the same for neuron arrays.
     */
    fun size(): Int {
        return outputs.size().toInt()
    }

    override fun inputSize(): Int {
        return size()
    }

    override fun outputSize(): Int {
        return outputs.size().toInt()
    }

    override fun toString(): String {
        return """
            $id with ${outputs.size()} components.
            Activations: ${Utils.getTruncatedArrayString(activationArray, 10)}
            $dataHolder
        """.trimIndent()
    }

    override fun clear() {
        outputs.mul(0.0)
        events.updated.fire()
    }

    override fun increment() {
        outputs.add(increment)
        events.updated.fire()
    }

    override fun decrement() {
        outputs.sub(increment)
        events.updated.fire()
    }


    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    override fun readResolve(): Any? {
        events = NeuronArrayEvents()
        return this
    }

    val excitatoryInputs: DoubleArray
        get() = incomingConnectors
            .filterIsInstance<WeightMatrix>()
            .map { it.excitatoryOutputs }
            .reduceOrNull { base, add -> SimbrainMath.addVector(base, add) }
            ?: DoubleArray(inputSize())

    val inhibitoryInputs: DoubleArray
        get() = incomingConnectors
            .filterIsInstance<WeightMatrix>()
            .map { it.inhibitoryOutputs }
            .reduceOrNull { base, add -> SimbrainMath.addVector(base, add) }
            ?: DoubleArray(inputSize())

    fun getLabelArray() = (0 until size()).map { it.toString() }.toTypedArray()
}