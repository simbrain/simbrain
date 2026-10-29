package org.simbrain.network.spikeresponders

import org.simbrain.network.core.Connector
import org.simbrain.network.core.Network
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.Synapse
import org.simbrain.network.spikeresponders.*
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.keyedUnitDouble
import java.util.*

/**
 * **SpikeResponder** is a superclass for objects that respond to pre-synaptic spikes with a response that is sent
 * as input to the post-synaptic neuron.
 */
abstract class SpikeResponder : CopyableObject {


    /**
     * If true, only spike with some probability specified by [spikeProbability]
     */
    var useSpikeProbability = false

    var spikeProbability by GuiEditable(
        initValue = 1.0,
        description = "Probability of spiking; must be between 0 and 1.",
        min = 0.0,
        max = 1.0,
        increment = 0.1,
        order = 1,
        useCheckboxFrom = SpikeResponder::useSpikeProbability,
        setter = { value ->
            useSpikeProbability = value != 1.0
            field = value.coerceIn(0.0, 1.0)
        }
    )

    override fun getTypeList(): List<Class<out CopyableObject?>>? {
        return responderList
    }

    /**
     * Defines a spike responder for scalar data.
     *
     * @param synapse           a reference to a parent synapse whose spikes we respond to. Contains reference to source
     * and target neuron, weight, strength, spike time, etc which can be used to defined the
     * response rule.
     * @param responderData data holder for spike responder
     */
    context(Network)
    abstract fun apply(synapse: Synapse, responderData: ScalarDataHolder)

    /**
     * Override to define a spike responder for matrix data.
     */
    context(Network)
    open fun apply(connector: Connector, responderData: MatrixDataHolder) {}

    /**
     * For a responder whose response at each synapse of a weight matrix is that synapse's weight times a factor that
     * depends only on the source neuron, returns those per-source factors. The weight matrix then sums responses from
     * its weights directly, visiting only sources with a nonzero factor, instead of having [apply] write every entry
     * of its PSR matrix. Called once per update of the weight matrix, so a responder may advance per-source state
     * here. Null, the default, means [apply] writes the PSR matrix.
     */
    context(Network)
    open fun sourceFactors(connector: WeightMatrix): DoubleArray? = null

    /**
     * Whether a spike reaching [synapse] gets through, given [spikeProbability]. The draw is a hash of the network's
     * seed, the synapse's id, this responder's type and the iteration, so free synapses, which update in parallel,
     * get the same draws whatever order they update in.
     */
    context(Network)
    fun spikeGetsThrough(synapse: Synapse): Boolean {
        if (!useSpikeProbability) return true
        // Rounded, since time accumulates floating point error
        val iteration = Math.round(time / timeStep)
        val draw = keyedUnitDouble(
            randomSeed,
            (synapse.id?.hashCode() ?: 0).toLong(),
            javaClass.name.hashCode().toLong(),
            iteration
        )
        return draw < spikeProbability
    }

    /**
     * Whether a spike gets through a weight matrix entry, given [spikeProbability]. Weight matrices update serially,
     * so the draw comes from the network's generator in the order entries are visited.
     */
    context(Network)
    fun spikeGetsThroughInOrder(): Boolean {
        if (!useSpikeProbability) return true
        return synchronized(random) { random.nextDouble() } < spikeProbability
    }

    /**
     * Override to return an appropriate data holder for a given responder.
     */
    open fun createResponderData(): ScalarDataHolder {
        return EmptyScalarData
    }

    /**
     * Override to return an appropriate data holder for a given responder.
     */
    open fun createMatrixData(rows: Int, cols: Int): MatrixDataHolder {
        return EmptyMatrixData
    }

    /**
     * @return Spike responder to duplicate.
     */
    abstract override fun copy(): SpikeResponder

    /**
     * @return the name of the spike responder
     */
    abstract val description: String?

    val type: String
        /**
         * @return the name of the class of this synapse
         */
        get() = this.javaClass.name.substring(this.javaClass.name.lastIndexOf('.') + 1)

}

/**
 * Spike responders for drop-down list used by
 * [org.simbrain.util.propertyeditor.ObjectTypeEditor]
 * to set the spike responder on a synapse.
 */
var responderList: List<Class<out CopyableObject?>> = listOf<Class<out CopyableObject?>>(
    NonResponder::class.java, JumpAndDecay::class.java,
    RiseAndDecay::class.java, StepResponder::class.java, ShortTermPlasticity::class.java
)