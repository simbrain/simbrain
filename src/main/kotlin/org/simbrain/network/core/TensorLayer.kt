package org.simbrain.network.core

import org.simbrain.network.events.TensorEvents
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.propertyeditor.TensorDescriptor
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.awt.geom.Point2D
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.tanh

/**
 * Activation function applied element-wise to tensor activations after input accumulation.
 */
enum class TensorActivation {
    LINEAR {
        override fun apply(x: Double) = x
        override fun derivative(x: Double) = 1.0
    },
    RELU {
        override fun apply(x: Double) = max(0.0, x)
        override fun derivative(x: Double) = if (x > 0.0) 1.0 else 0.0
    },
    SIGMOID {
        override fun apply(x: Double) = 1.0 / (1.0 + exp(-x))
        override fun derivative(x: Double): Double {
            val s = apply(x)
            return s * (1.0 - s)
        }
    },
    TANH {
        override fun apply(x: Double) = tanh(x)
        override fun derivative(x: Double): Double {
            val t = tanh(x)
            return 1.0 - t * t
        }
    };

    abstract fun apply(x: Double): Double

    /**
     * Derivative of the activation function at [x] (the pre-activation value).
     */
    abstract fun derivative(x: Double): Double
}

/**
 * A node holding an n-dimensional activation tensor stored as a flat [DoubleArray] in HWC layout.
 * Part of a parallel hierarchy to [Layer]/[Connector] for CNN operations.
 *
 * Tensors are connected by [TensorConnector]s. During [accumulateInputs], each incoming connector's
 * [TensorConnector.propagate] is called, which accumulates into [inputs]. During [update], inputs
 * are summed with biases, an activation function is applied, and inputs are cleared.
 */
class TensorLayer(val shape: TensorShape) : LocatableModel(), EditableObject, AttributeContainer {

    @UserParameter(label = "Activation Function", description = "Element-wise activation", order = 10)
    var activationFunction: TensorActivation = TensorActivation.LINEAR

    @UserParameter(label = "Clamped", description = "If clamped, inputs are ignored during update", order = 20)
    var isClamped = false
        set(value) {
            field = value
            events.clampChanged.fire()
        }

    @UserParameter(label = "Thumbnail Strip", description = "Show all channels as a thumbnail strip", tab = "GUI", order = 30)
    var thumbnailStripMode = true
        set(value) {
            field = value
            events.visualPropertiesChanged.fire()
        }

    var rgbComposite = false
        set(value) {
            field = value
            if (value) thumbnailStripMode = false
            events.visualPropertiesChanged.fire()
        }

    var currentChannel = 0
        set(value) {
            field = value.coerceIn(0, shape.channels - 1)
            events.visualPropertiesChanged.fire()
        }

    /** Descriptor for how to display HWC tensor arrays in the property editor. */
    val hwcDescriptor get() = TensorDescriptor(
        intArrayOf(shape.height, shape.width, shape.channels),
        arrayOf("H", "W", "Channel")
    )

    /** Current activations (HWC flat array). */
    @set:Consumable(description = "Set activations from external source")
    var activations by GuiEditable(
        initValue = DoubleArray(shape.size),
        label = "Activations",
        tab = "Data",
        order = 100,
        tensorDescriptor = TensorLayer::hwcDescriptor,
        setter = { newArray ->
            System.arraycopy(newArray, 0, field, 0, minOf(newArray.size, field.size))
            baseObject.events.updated.fire()
        }
    )

    /** Accumulated inputs from incoming connectors (cleared each update). */
    val inputs = DoubleArray(shape.size)

    /** Per-element biases. */
    var biases by GuiEditable(
        initValue = DoubleArray(shape.size),
        label = "Biases",
        tab = "Data",
        order = 110,
        tensorDescriptor = TensorLayer::hwcDescriptor
    )

    /** Gradients for backpropagation (same layout as activations). */
    val gradients = DoubleArray(shape.size)

    /** Pre-activation values (input + bias, before activation function). Stored during update for backprop. */
    val preActivations = DoubleArray(shape.size)

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

    override val name: String get() = displayName

    // Indexed access

    operator fun get(h: Int, w: Int, c: Int): Double = activations[shape.index(h, w, c)]
    operator fun set(h: Int, w: Int, c: Int, value: Double) {
        activations[shape.index(h, w, c)] = value
    }

    // Producible / Consumable for coupling

    @get:Producible(description = "Activation array")
    val activationArray: DoubleArray get() = activations

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

    /**
     * Fill [buffer] with the values of channel [c] from [activations].
     */
    fun getChannel(c: Int, buffer: DoubleArray) {
        for (h in 0 until shape.height) {
            for (w in 0 until shape.width) {
                buffer[h * shape.width + w] = activations[shape.index(h, w, c)]
            }
        }
    }

    /**
     * Write [source] values into channel [c] of [activations].
     */
    fun setChannel(c: Int, source: DoubleArray) {
        val pixelCount = shape.height * shape.width
        val len = minOf(source.size, pixelCount)
        for (i in 0 until len) {
            val h = i / shape.width
            val w = i % shape.width
            activations[shape.index(h, w, c)] = source[i]
        }
    }

    // Per-channel coupling containers

    @Transient
    val channelContainers: List<ChannelContainer> = List(shape.channels) { ChannelContainer(it) }

    override val childrenContainers: List<AttributeContainer> get() = channelContainers

    /**
     * A lightweight view into a single channel of this tensor's [activations].
     * Exposes per-channel [Producible] and [Consumable] for coupling.
     */
    inner class ChannelContainer(val channelIndex: Int) : AttributeContainer {

        override val id: String get() = "Channel $channelIndex"

        private val buffer = DoubleArray(shape.height * shape.width)

        @get:Producible(description = "Channel values")
        val values: DoubleArray
            get() {
                getChannel(channelIndex, buffer)
                return buffer
            }

        @Consumable(description = "Set channel values")
        fun setValues(source: DoubleArray) {
            setChannel(channelIndex, source)
            events.updated.fire()
        }
    }

    // Connector management

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

    // Update

    context(Network)
    override fun accumulateInputs() {
        incomingTensorConnectors.forEach { it.propagate() }
    }

    context(Network)
    override fun update() {
        if (isClamped) return
        for (i in activations.indices) {
            val pre = inputs[i] + biases[i]
            preActivations[i] = pre
            activations[i] = activationFunction.apply(pre)
        }
        inputs.fill(0.0)
        events.updated.fire()
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        val rand = randomizer ?: NetworkPreferences.activationRandomizer
        for (i in activations.indices) {
            activations[i] = rand.sampleDouble()
        }
        events.updated.fire()
    }

    override fun toggleClamping() {
        isClamped = !isClamped
    }

    fun clearGradients() {
        gradients.fill(0.0)
    }

    override fun clear() {
        activations.fill(0.0)
        inputs.fill(0.0)
        events.updated.fire()
    }

    override suspend fun delete(): List<NetworkModel> {
        val connectors = LinkedHashSet<NetworkModel>(incomingTensorConnectors + outgoingTensorConnectors + outgoingFlattenConnectors)
        connectors.forEach { it.delete() }
        events.deleted.fire(this)
        return buildList {
            add(this@TensorLayer)
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

        fun create(): TensorLayer = TensorLayer(TensorShape(height, width, channels))

        override val name = "Tensor"
    }

}
