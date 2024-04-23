package org.simbrain.network.util

import org.simbrain.util.UserParameter
import org.simbrain.util.Utils
import org.simbrain.util.copyFrom
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.toDoubleArray
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import java.util.*

/**
 * Holders for matrix valued data used in array based update rules. E.g. [NeuronArray]
 */
interface MatrixDataHolder : CopyableObject {
    override fun copy(): MatrixDataHolder
}

object EmptyMatrixData : MatrixDataHolder {
    override fun copy() = this
    override fun toString(): String = ""
}

class BiasedMatrixData(var size: Int) : MatrixDataHolder, AttributeContainer {
    @UserParameter(label = "Biases", description = "Biases for each neuron")
    var biases = Matrix(size, 1)
        set(value) {
            field.copyFrom(value)
        }

    @get:Producible
    val biasesArray: DoubleArray
        get() = biases.toDoubleArray()

    override fun copy() = BiasedMatrixData(size).also {
        it.biases = biases.clone()
    }

    override fun toString(): String {
        return "Biases: ${Utils.getTruncatedArrayString(biases.toDoubleArray(), 10)}"
    }

    override val id: String
        get() = "Biases"
}

open class SpikingMatrixData(var size: Int) : MatrixDataHolder {
    var spikes = BooleanArray(size) // TODO: Possibly use int Smile array of binary ints for perf
        private set
    var lastSpikeTimes = DoubleArray(size)
    override fun copy() = SpikingMatrixData(size).also {
        it.spikes = spikes.copyOf()
        it.lastSpikeTimes = lastSpikeTimes.copyOf()
    }

    fun commonCopy(toCopy: SpikingMatrixData) {
        toCopy.spikes = spikes.copyOf()
        toCopy.lastSpikeTimes = lastSpikeTimes.copyOf()
    }

    fun setHasSpiked(i: Int, hasSpiked: Boolean, networkTime: Double) {
        spikes[i] = hasSpiked
        if (hasSpiked) {
            lastSpikeTimes[i] = networkTime
        }

    }
}

class NakaMatrixData(var size: Int) : MatrixDataHolder {
    var a = Matrix(size, 1)
    override fun copy() = NakaMatrixData(size).also {
        it.a = a.clone()
    }
}

/**
 * Holders for scalar data used in scalar update rules, like [NeuronUpdateRule] and [SynapseUpdateRule].
 */
interface ScalarDataHolder : CopyableObject {
    override fun copy(): ScalarDataHolder
}

object EmptyScalarData : ScalarDataHolder {
    override fun copy(): EmptyScalarData {
        return this
    }
    override fun toString(): String = ""
}

class BiasedScalarData(
    @UserParameter(label = "bias")
    var bias: Double = 0.0
) : ScalarDataHolder {
    override fun copy(): BiasedScalarData {
        return BiasedScalarData(bias)
    }
}

class PointNeuronScalarData(

    @UserParameter(
        label = "Membrane potential",
        minimumValue = 0.0
    )
    var membranePotential: Double = .15,

    @UserParameter(
        label = "Excitatory Conductance",
        description = "Current excitatory conductance.Proportion of channels open",
        minimumValue = 0.0
    )
    var excitatoryConductance: Double = 0.0,

    @UserParameter(
        label = "Inhibitory Conductance",
        description = "Current inhibitory conductance. Proportion of channels open",
        minimumValue = 0.0
    )
    var inhibitoryConductance: Double = 0.0,

) : ScalarDataHolder {
    override fun copy(): PointNeuronScalarData {
        return PointNeuronScalarData(membranePotential, excitatoryConductance, inhibitoryConductance)
    }
}

open class SpikingScalarData(
    /**
     * Set to true at end of iteration when spike occurs, then set to false.
     */
    spiked: Boolean = false,
    /**
     * Time of last spike. Default assumes no spikes have occurred when simulation begins.
     */
    var lastSpikeTime: Double = Double.NEGATIVE_INFINITY
) : ScalarDataHolder {

    var spiked: Boolean = spiked
        private set

    /**
     * Indicate a spike occurred, and if it has, set the last spike time.
     */
    fun setHasSpiked(hasSpiked: Boolean, networkTime: Double) {
        spiked = hasSpiked
        if (spiked) {
            lastSpikeTime = networkTime
        }
    }

    override fun copy(): SpikingScalarData {
        return SpikingScalarData(spiked, lastSpikeTime)
    }
}

// In java they can't be stored in the same file as the update rule
class NakaScalarData(@UserParameter(label = "a") var a: Double = 0.0) : ScalarDataHolder {
    override fun copy(): NakaScalarData {
        return NakaScalarData(a)
    }
}


