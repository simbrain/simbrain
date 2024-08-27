package org.simbrain.network.util

import org.simbrain.network.core.Network
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.CopyableObject
import java.util.*

/**
 * Holders for matrix valued data used in array based update rules. E.g. [NeuronArray]
 */
interface MatrixDataHolder : CopyableObject {
    override fun copy(): MatrixDataHolder
    fun clear() {}
}

object EmptyMatrixData : MatrixDataHolder {
    override fun copy() = this
    override fun toString(): String = ""
}

open class SpikingMatrixData(val size: Int) : MatrixDataHolder {

    @UserParameter(label = "Spikes", description = "Spikes for each neuron")
    var spikes = BooleanArray(size) { false }

    @UserParameter(label = "Last Spike Times", description = "Time of last spike for each neuron")
    var lastSpikeTimes = DoubleArray(size) { Double.NEGATIVE_INFINITY }

    override fun copy() = SpikingMatrixData(size).also {
        commonCopy(it)
    }

    fun commonCopy(toCopy: SpikingMatrixData) {
        spikes.copyInto(toCopy.spikes)
        lastSpikeTimes.copyInto(toCopy.lastSpikeTimes)
    }

    context(Network)
    fun setHasSpiked(i: Int, hasSpiked: Boolean) {
        spikes[i] = hasSpiked
        if (hasSpiked) {
            lastSpikeTimes[i] = time
        }
    }

    context(Network)
    fun setHasSpiked(spikes: BooleanArray) {
        spikes.forEachIndexed { index, hasSpiked -> setHasSpiked(index, hasSpiked) }
    }
}

/**
 * Holders for scalar data used in scalar update rules, like [NeuronUpdateRule] and [SynapseUpdateRule].
 */
interface ScalarDataHolder : CopyableObject {
    override fun copy(): ScalarDataHolder
    fun clear() {}
}

object EmptyScalarData : ScalarDataHolder {
    override fun copy(): EmptyScalarData {
        return this
    }

    override fun clear() {}

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
    context(Network)
    fun setHasSpiked(hasSpiked: Boolean) {
        spiked = hasSpiked
        if (spiked) {
            lastSpikeTime = time
        }
    }

    override fun copy(): SpikingScalarData {
        return SpikingScalarData(spiked, lastSpikeTime)
    }

}
