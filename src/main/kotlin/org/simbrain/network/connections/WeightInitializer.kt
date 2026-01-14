package org.simbrain.network.connections

import org.simbrain.util.propertyeditor.CopyableObject

/**
 * Strategy for initializing synapse weights after connections are created.
 *
 * Implementations determine how weights are assigned based on different criteria:
 * - [ConstantWeightInitializer]: Uses fixed values for excitatory/inhibitory weights
 * - [RandomWeightInitializer]: Uses probability distributions for excitatory/inhibitory weights
 * - [DistanceBasedWeightInitializer]: Scales weights based on distance between neurons
 *
 * Used by [ConnectionStrategy] to set initial weights after synapses are created.
 */
abstract class WeightInitializer : CopyableObject {

    /**
     * Initialize weights for synapses that have already been split by polarity.
     *
     * @param polarizedSynapses The synapses pre-split into excitatory and inhibitory lists
     */
    abstract fun initializeWeights(polarizedSynapses: PolarizedSynapseCollection)

    abstract override fun copy(): WeightInitializer

    override fun getTypeList() = weightInitializerTypes

}

val weightInitializerTypes = listOf(
    ConstantWeightInitializer::class.java,
    RandomWeightInitializer::class.java,
    DistanceBasedWeightInitializer::class.java
)
