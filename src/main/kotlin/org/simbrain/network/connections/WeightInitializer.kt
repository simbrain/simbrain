package org.simbrain.network.connections

import org.simbrain.network.core.Synapse
import org.simbrain.util.propertyeditor.CopyableObject

/**
 * Strategy for initializing synapse weights after connections are created.
 *
 * Implementations determine how weights are assigned based on different criteria:
 * - [RandomWeightInitializer]: Uses probability distributions for excitatory/inhibitory weights
 * - [DistanceBasedWeightInitializer]: Scales weights based on distance between neurons
 *
 * Used by [ConnectionStrategy] to set initial weights after synapses are created.
 */
abstract class WeightInitializer : CopyableObject {

    /**
     * Initialize weights for the given synapses.
     *
     * @param synapses The synapses whose weights should be initialized
     */
    abstract fun initializeWeights(synapses: List<Synapse>)

    abstract override fun copy(): WeightInitializer

    override fun getTypeList() = weightInitializerTypes

}

val weightInitializerTypes = listOf(
    RandomWeightInitializer::class.java,
    DistanceBasedWeightInitializer::class.java
)
