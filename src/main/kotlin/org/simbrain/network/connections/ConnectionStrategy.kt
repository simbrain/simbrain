package org.simbrain.network.connections

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.ConnectionStrategyPanel
import org.simbrain.util.UserParameter
import org.simbrain.util.displayInDialog
import org.simbrain.util.propertyeditor.CopyableObject
import kotlin.random.Random

/**
 * Maintains a specific strategy for creating connections between two groups of neurons. Subclasses correspond to
 * specific types of connection strategy. Methods for creating free synapses are generally distinct from those for
 * creating them in synapse groups. Another distinction is between strategies that use polarity and those that do not.
 *
 * Note that connections are generally made in the following order.
 * 1) Synapses are created using this class
 * 2) Their excitatory / inhibitory ratio is set using [percentExcitatory]
 * 3) Weights are initialized using [weightInitializer]
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
abstract class ConnectionStrategy(seed: Long = Random.nextLong()) : CopyableObject {

    /**
     * Strategy for initializing synapse weights after connections are created.
     */
    @UserParameter(label = "Weight Initializer", description = "How to initialize synapse weights", order = 100, showDetails = false)
    var weightInitializer: WeightInitializer = RandomWeightInitializer(seed)

    /**
     * If true, then separately store [percentExcitatory]. If false, the connection strategy itself determines how
     * many excitatory vs. inhibitory weights there are.
     */
    open val usesPolarity = true

    /**
     * If uses polarity, store the percent excitatory. Otherwise ignore.
     */
    var percentExcitatory: Double = 50.0

    /**
     * A random object that uses the strategy's [seed] that can be passed to different functions (such as shuffle) to ensure deterministic results
     */
    var random = Random(seed)

    fun commonCopy(toCopy: ConnectionStrategy) {
        toCopy.weightInitializer = weightInitializer.copy()
        toCopy.percentExcitatory = percentExcitatory
    }

    abstract override fun copy(): ConnectionStrategy

    /**
     * Apply connection to a set of free neurons.
     *
     * @param network parent network free neuron
     * @param source  source neurons
     * @param target  target neurons
     * @param addToNetwork if true, add the synapses to the network
     * @return the resulting list of synapses, which are sometimes needed for
     * other operations
     */
    abstract fun connectNeurons(
        source: List<Neuron>,
        target: List<Neuron>
    ): List<Synapse>

    override fun getTypeList() = connectionTypes

    /**
     * Provides tooltip text for display in GUI components.
     * Subclasses can override to provide strategy-specific information.
     */
    open fun tooltipText(): String = name

}

val connectionTypes = listOf(
    AllToAll::class.java,
    DistanceBased::class.java,
    OneToOne::class.java,
    FixedDegree::class.java,
    Sparse::class.java
)

fun main() {
    ConnectionStrategyPanel(Sparse()).displayInDialog()
}
