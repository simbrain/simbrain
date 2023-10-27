package org.simbrain.network.matrix

import org.simbrain.network.core.ArrayLayer
import org.simbrain.network.core.Connector
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.network.events.LocationEvents
import org.simbrain.network.events.NeuronArrayEvents
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.*
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import smile.stat.distribution.GaussianDistribution
import java.awt.geom.Rectangle2D

/**
 * A "neuron array" backed by a Smile Matrix. Stored as a column vector.
 */
class NeuronArray(parent: Network, inputSize: Int) : ArrayLayer(parent, inputSize), EditableObject, AttributeContainer {
    @UserParameter(label = "Update Rule", order = 100)
    var updateRule: NeuronUpdateRule<ScalarDataHolder, MatrixDataHolder> = LinearRule()
        set(value) {
            val typeChanged = field::class != value::class
            field = value
            if (typeChanged) {
                dataHolder = updateRule.createMatrixData(size())
            }
            events.updated.fireAndForget()
        }

    /**
     * Holds data for prototype rule.
     */
    var dataHolder: MatrixDataHolder by GuiEditable(
        initValue = updateRule.createMatrixData(inputSize),
        order = 99,
        onUpdate = {
            onChange(NeuronArray::updateRule) {
                val proposedDataHolder = updateRule.createMatrixData(size())
                if (widgetValue(NeuronArray::dataHolder)::class != proposedDataHolder::class) {
                    refreshValue(proposedDataHolder)
                }
            }
        }
    )

    /**
     * Array to hold activation values. These are also the outputs that are consumed by
     * other network components via [Layer].
     */
    @UserParameter(label = "Activations", description = "Neuron activations", order = 1)
    var activations: Matrix = Matrix(inputSize, 1)
        set(newActivations) {
            field.copyFrom(newActivations)
            events.updated.fireAndForget()
        }

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
            (events as NeuronArrayEvents).visualPropertiesChanged.fireAndBlock()
        }

    @UserParameter(label = "Biases Visible", description = "If true, show biases.", order = 11)
    var isShowBias = false
        set(showBias) {
            field = showBias
            (events as NeuronArrayEvents).visualPropertiesChanged.fireAndBlock()
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
    fun deepCopy(newParent: Network): NeuronArray {
        val copy = NeuronArray(newParent, outputSize())
        copy.location = location
        copy.gridMode = gridMode
        copy.activations.copyFrom(activations)
        copy.updateRule = updateRule
        copy.dataHolder = dataHolder!!.copy()
        return copy
    }

    @get:Producible
    val activationArray: DoubleArray
        get() = activations.toDoubleArray()

    var targetValues: Matrix?
        get() = targets
        set(targets) {
            if (targets != null) {
                outputs.validateSameShape(targets)
            }
            this.targets = targets
            events.updated.fireAndBlock()
        }

    override fun randomize() {
        activations = Matrix.rand(
            size(), 1,
            GaussianDistribution(0.0, 1.0)
        )
        events.updated.fireAndForget()
    }

    override val bound: Rectangle2D
        get() = Rectangle2D.Double(
            x - width / 2, y - height / 2,
            width, height
        )

    override fun onCommit() {
        events.labelChanged.fireAndForget("", label!!)
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
        setLocation(x + offsetX, y + offsetY)
        events.updated.fireAndForget()
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
        fun create(network: Network): NeuronArray {
            return NeuronArray(network, numNodes)
        }

        override val name = "Neuron Array"

    }

    override fun update() {
        if (isClamped) {
            return
        }
        updateRule.apply(this, dataHolder)
        inputs.mul(0.0) // clear inputs
        events.updated.fireAndForget()
    }

    fun setActivations(newActivations: DoubleArray) {
        activations = Matrix.column(newActivations)
    }

    fun fireLocationChange() {
        events.locationChanged.fireAndForget()
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
        return """$id with ${outputs.size()} components. 
Activations: ${Utils.getTruncatedArrayString(activationArray, 10)}
$dataHolder"""
    }

    override fun clear() {
        outputs.mul(0.0)
        events.updated.fireAndForget()
    }

    override fun increment() {
        outputs.add(increment)
        events.updated.fireAndForget()
    }

    override fun decrement() {
        outputs.sub(increment)
        events.updated.fireAndForget()
    }


    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    override fun readResolve(): Any? {
        events = NeuronArrayEvents()
        return this
    }

    val excitatoryInputs: DoubleArray
        get() = incomingConnectors.stream()
            .filter { wm: Connector? -> wm is WeightMatrix }
            .map { wm: Connector -> (wm as WeightMatrix).getExcitatoryOutputs() }
            .reduce { base: DoubleArray?, add: DoubleArray? -> SimbrainMath.addVector(base, add) }
            .orElse(DoubleArray(inputSize()))
    val inhibitoryInputs: DoubleArray
        get() = incomingConnectors.stream()
            .filter { wm: Connector? -> wm is WeightMatrix }
            .map { wm: Connector -> (wm as WeightMatrix).getInhibitoryOutputs() }
            .reduce { base: DoubleArray?, add: DoubleArray? -> SimbrainMath.addVector(base, add) }
            .orElse(DoubleArray(inputSize()))
}