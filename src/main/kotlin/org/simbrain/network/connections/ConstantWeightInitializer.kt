package org.simbrain.network.connections

import org.simbrain.util.UserParameter

/**
 * Initializes all synapse weights to constant values.
 *
 * Excitatory synapses are set to [excitatoryStrength] and
 * inhibitory synapses are set to [inhibitoryStrength].
 */
class ConstantWeightInitializer : WeightInitializer() {

    /**
     * The constant strength for excitatory synapses.
     */
    @UserParameter(label = "Excitatory Strength", description = "Strength for excitatory synapses", order = 1)
    var excitatoryStrength: Double = DEFAULT_EXCITATORY_STRENGTH

    /**
     * The constant strength for inhibitory synapses.
     */
    @UserParameter(label = "Inhibitory Strength", description = "Strength for inhibitory synapses", order = 2)
    var inhibitoryStrength: Double = DEFAULT_INHIBITORY_STRENGTH

    override fun initializeWeights(polarizedSynapses: PolarizedSynapseCollection) {
        polarizedSynapses.excitatory.forEach { synapse ->
            synapse.forceSetStrength(excitatoryStrength)
        }

        polarizedSynapses.inhibitory.forEach { synapse ->
            synapse.forceSetStrength(inhibitoryStrength)
        }
    }

    override fun copy(): ConstantWeightInitializer {
        return ConstantWeightInitializer().apply {
            excitatoryStrength = this@ConstantWeightInitializer.excitatoryStrength
            inhibitoryStrength = this@ConstantWeightInitializer.inhibitoryStrength
        }
    }

    override val name = "Constant"

}
