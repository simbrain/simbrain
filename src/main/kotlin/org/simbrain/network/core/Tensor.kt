package org.simbrain.network.core

import org.simbrain.network.events.TensorEvents
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.awt.geom.Point2D
import kotlin.math.max

/**
 * Activation function applied element-wise to tensor activations after input accumulation.
 */
enum class TensorActivation {
    IDENTITY {
        override fun apply(x: Double) = x
    },
    RELU {
        override fun apply(x: Double) = max(0.0, x)
    },
    SIGMOID {
        override fun apply(x: Double) = 1.0 / (1.0 + Math.exp(-x))
    },
    TANH {
        override fun apply(x: Double) = Math.tanh(x)
    };

    abstract fun apply(x: Double): Double
}

/**
 * A node holding an n-dimensional activation tensor stored as a flat [DoubleArray] in HWC layout.
 * Part of a parallel hierarchy to [Layer]/[Connector] for CNN operations.
 *
 * Tensors are connected by [TensorConnector]s. During [accumulateInputs], each incoming connector's
 * [TensorConnector.propagate] is called, which accumulates into [inputs]. During [update], inputs
 * are summed with biases, an activation function is applied, and inputs are cleared.
 */
class Tensor(val shape: TensorShape) : LocatableModel(), EditableObject, AttributeContainer {

    @UserParameter(label = "Activation Function", description = "Element-wise activation", order = 10)
    var activationFunction: TensorActivation = TensorActivation.IDENTITY

    @UserParameter(label = "Clamped", description = "If clamped, inputs are ignored during update", order = 20)
    var isClamped = false
        set(value) {
            field = value
            events.clampChanged.fire()
        }

    /** Current activations (HWC flat array). */
    val activations = DoubleArray(shape.size)

    /** Accumulated inputs from incoming connectors (cleared each update). */
    val inputs = DoubleArray(shape.size)

    /** Per-element biases. */
    val biases = DoubleArray(shape.size)

    @Transient
    var incomingTensorConnectors: MutableList<TensorConnector> = mutableListOf()

    @Transient
    var outgoingTensorConnectors: MutableList<TensorConnector> = mutableListOf()

    @Transient
    var outgoingFlattenConnectors: MutableList<FlattenConnector> = mutableListOf()

    @Transient
    override var events: TensorEvents = TensorEvents()

    /** Width/height used by GUI for arrow placement. */
    var renderWidth = 0.0
        set(value) {
            field = value
            events.locationChanged.fire()
        }
    var renderHeight = 0.0
        set(value) {
            field = value
            events.locationChanged.fire()
        }

    override var location: Point2D = Point2D.Double()
        set(value) {
            field.setLocation(value)
            events.locationChanged.fire()
        }

    override val name: String get() = "Tensor"

    // --- Indexed access ---

    operator fun get(h: Int, w: Int, c: Int): Double = activations[shape.index(h, w, c)]
    operator fun set(h: Int, w: Int, c: Int, value: Double) {
        activations[shape.index(h, w, c)] = value
    }

    // --- Producible / Consumable for coupling ---

    @get:Producible(description = "Activation array")
    val activationArray: DoubleArray get() = activations

    @Consumable(description = "Set activations from external source")
    fun setActivations(source: DoubleArray) {
        System.arraycopy(source, 0, activations, 0, minOf(source.size, activations.size))
        events.updated.fire()
    }

    /**
     * Extract a single channel as a flat array of size height*width.
     */
    fun getChannel(c: Int): DoubleArray {
        val result = DoubleArray(shape.height * shape.width)
        for (h in 0 until shape.height) {
            for (w in 0 until shape.width) {
                result[h * shape.width + w] = activations[shape.index(h, w, c)]
            }
        }
        return result
    }

    // --- Connector management ---

    fun addIncomingConnector(connector: TensorConnector) {
        incomingTensorConnectors.add(connector)
    }

    fun removeIncomingConnector(connector: TensorConnector) {
        incomingTensorConnectors.remove(connector)
    }

    fun addOutgoingConnector(connector: TensorConnector) {
        outgoingTensorConnectors.add(connector)
    }

    fun removeOutgoingConnector(connector: TensorConnector) {
        outgoingTensorConnectors.remove(connector)
    }

    fun addOutgoingFlattenConnector(connector: FlattenConnector) {
        outgoingFlattenConnectors.add(connector)
    }

    fun removeOutgoingFlattenConnector(connector: FlattenConnector) {
        outgoingFlattenConnectors.remove(connector)
    }

    // --- Update ---

    context(Network)
    override fun accumulateInputs() {
        incomingTensorConnectors.forEach { it.propagate() }
    }

    context(Network)
    override fun update() {
        if (isClamped) return
        for (i in activations.indices) {
            activations[i] = activationFunction.apply(inputs[i] + biases[i])
        }
        inputs.fill(0.0)
        events.updated.fire()
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        val rand = randomizer ?: org.simbrain.network.gui.dialogs.NetworkPreferences.activationRandomizer
        for (i in activations.indices) {
            activations[i] = rand.sampleDouble()
        }
        events.updated.fire()
    }

    override fun clear() {
        activations.fill(0.0)
        inputs.fill(0.0)
        events.updated.fire()
    }

    override suspend fun delete(): List<NetworkModel> {
        val connectors = LinkedHashSet<NetworkModel>(incomingTensorConnectors + outgoingTensorConnectors + outgoingFlattenConnectors)
        connectors.forEach { it.delete() }
        events.deleted.fire(this).await()
        return buildList {
            add(this@Tensor)
            addAll(connectors)
        }
    }

    override fun toString(): String = "$displayName ($shape)"

    /**
     * Template for creating Tensors from GUI dialogs.
     */
    class CreationTemplate : EditableObject {
        @UserParameter(label = "Height", description = "Height of the tensor", order = 1)
        var height = 28

        @UserParameter(label = "Width", description = "Width of the tensor", order = 2)
        var width = 28

        @UserParameter(label = "Channels", description = "Number of channels", order = 3)
        var channels = 1

        fun create(): Tensor = Tensor(TensorShape(height, width, channels))

        override val name = "Tensor"
    }

}
