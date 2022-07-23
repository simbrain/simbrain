package org.simbrain.network.util

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import java.util.*

/**
 * Holders for matrix valued data used in array based update rules. E.g. [NeuronArray]
 */
interface MatrixDataHolder : CopyableObject {
    override fun copy(): MatrixDataHolder
}

class EmptyMatrixData : MatrixDataHolder {
    override fun copy() = EmptyMatrixData()
}

class BiasedMatrixData(var size: Int) : MatrixDataHolder {
    var biases = DoubleArray(size)
    override fun copy() = BiasedMatrixData(size).also {
        it.biases = biases.copyOf()
    }
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
    var a = DoubleArray(size)
    override fun copy() = NakaMatrixData(size).also {
        it.a = a.copyOf()
    }
}

/**
 * Holders for scalar data used in scalar update rules, like [NeuronUpdateRule] and [SynapseUpdateRule].
 */
interface ScalarDataHolder : CopyableObject {
    override fun copy(): ScalarDataHolder
}

class EmptyScalarData : ScalarDataHolder {
    override fun copy(): EmptyScalarData {
        return EmptyScalarData()
    }
}

class BiasedScalarData(
    @UserParameter(label = "bias")
    var bias: Double = 0.0
) : ScalarDataHolder {
    override fun copy(): BiasedScalarData {
        return BiasedScalarData(bias)
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

class NakaScalarData(@UserParameter(label = "a") var a: Double = 0.0) : ScalarDataHolder {
    override fun copy(): NakaScalarData {
        return NakaScalarData(a)
    }
}

class IzhikData(
    @UserParameter(label = "a") var a: Double = 0.0,
    @UserParameter(label = "b") var b: Double = 0.0,
    @UserParameter(label = "c") var c: Double = 0.0,
    @UserParameter(label = "d") var d: Double = 0.0
) : ScalarDataHolder {
    override fun copy(): IzhikData {
        return IzhikData(a, b, c, d)
    }
}


class MorrisLecarData(
    @UserParameter(label = "w_K", description = "Fraction of open potassium channels")
    var w_K: Double = 0.0,
) : ScalarDataHolder {
    override fun copy(): MorrisLecarData {
        return MorrisLecarData(w_K)
    }
}

class AdexData(
    @UserParameter(
        label = "w", description = "Adaptation variable: Roughly speaking amount of metabolite currently " +
                "in the cell. Expelled during spiking and then replenished."
    )
    var w: Double = 200.0,
    @UserParameter(label = "Inhibitory Conductance")
    var inhibConductance: Double = 0.0,
    @UserParameter(label = "Excitatory Conductance")
    var exConductance: Double = 0.0,
): SpikingScalarData() {
    override fun copy(): AdexData {
        return AdexData(w, inhibConductance, exConductance)
    }
}

