package org.simbrain.network.learningrules

import org.simbrain.network.core.*
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.UserParameter

/**
 * **ShortTermPlasticitySynapse**.
 */
class ShortTermPlasticityRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {

    @UserParameter(label = "Plasticity Type", description = "Plasticity Type", increment = 1.0, order = 1)
    var plasticityType: Int = STD

    /**
     * Pseudo spike threshold.
     */
    @UserParameter(label = "Spike Threshold", description = "Pseudo Spike Threshold", increment = .1, order = 2)
    var firingThreshold: Double = DEFAULT_FIRING_THRESHOLD

    @UserParameter(label = "Line Strength", description = "Base line strength", increment = .1, order = 3)
    var baseLineStrength: Double = DEFAULT_BASE_LINE_STRENGTH

    @UserParameter(label = "Input Threshold", description = "Input threshold", increment = .1, order = 4)
    var inputThreshold: Double = DEFAULT_INPUT_THRESHOLD

    @UserParameter(label = "Bump rate", description = "Bump Rate", increment = .1, order = 5)
    var bumpRate: Double = DEFAULT_BUMP_RATE

    @UserParameter(
        label = "Decay Rate",
        description = "Rate at which the synapse will decay",
        increment = .1,
        order = 6
    )
    var decayRate: Double = DEFAULT_DECAY_RATE

    @UserParameter(label = "Activated", description = "Activated", increment = .1, order = 7)
    private var activated = DEFAULT_ACTIVATED

    override fun init(synapse: Synapse) {
    }

    override val name: String
        get() = "Short Term Plasticity"

    override fun copy(): SynapseUpdateRule<*, *> {
        val stp = ShortTermPlasticityRule()
        stp.baseLineStrength = baseLineStrength
        stp.bumpRate = bumpRate
        stp.decayRate = decayRate
        stp.inputThreshold = inputThreshold
        stp.plasticityType = plasticityType
        stp.firingThreshold = firingThreshold
        return stp
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        // Determine whether to activate short term dynamics

        activated = if (synapse.source.updateRule is SpikingNeuronUpdateRule<*, *>) {
            synapse.source.isSpike
        } else {
            if (synapse.source.activation > firingThreshold) {
                true
            } else {
                false
            }
        }
        var strength = synapse.strength
        if (activated) {
            if (plasticityType == STD) {
                strength -= (bumpRate * (strength - synapse.lowerBound))
            } else {
                strength += (bumpRate * (synapse.upperBound - strength))
            }
        } else {
            strength -= (decayRate * (strength - baseLineStrength))
        }
        synapse.strength = strength
    }

    // Note: This function has not been tested.
    context(Network)
    override fun apply(connector: Connector, dataHolder: EmptyMatrixData) {
        if (connector is WeightMatrix) {
            val wm = connector.weights
            val sourceArray = connector.source as NeuronArray
            
            // Check if source array has spiking data for more accurate plasticity
            val sourceSpikingData = sourceArray.dataHolder as? SpikingMatrixData
            
            for (i in 0 until wm.nrow()) {
                for (j in 0 until wm.ncol()) {
                    val activated = if (sourceSpikingData != null && sourceArray.updateRule.isSpikingRule) {
                        // Use spike data if available
                        sourceSpikingData.spikes[j]
                    } else {
                        // Fall back to activation-based threshold
                        sourceArray.activations[j, 0] > firingThreshold
                    }
                    
                    var strength = wm[i, j]
                    if (activated) {
                        if (plasticityType == STD) {
                            // Assuming default bounds for matrix version
                            strength -= (bumpRate * (strength - (-1.0))) // Using -1.0 as lower bound
                        } else {
                            // Assuming default bounds for matrix version  
                            strength += (bumpRate * (1.0 - strength)) // Using 1.0 as upper bound
                        }
                    } else {
                        strength -= (decayRate * (strength - baseLineStrength))
                    }
                    wm[i, j] = strength
                }
            }
        }
    }

    companion object {
        // TODO: Enum
        /**
         * STD.
         */
        private const val STD = 0

        /**
         * Plasticity type.
         */
        const val DEFAULT_PLASTICITY_TYPE: Int = STD

        /**
         * Pseudo spike threshold.
         */
        const val DEFAULT_FIRING_THRESHOLD: Double = 0.0

        /**
         * Base line strength.
         */
        const val DEFAULT_BASE_LINE_STRENGTH: Double = 1.0

        /**
         * Input threshold.
         */
        const val DEFAULT_INPUT_THRESHOLD: Double = 0.0

        /**
         * Bump rate.
         */
        const val DEFAULT_BUMP_RATE: Double = .5

        /**
         * Rate at which the synapse will decay.
         */
        const val DEFAULT_DECAY_RATE: Double = .2

        /**
         * Activated.
         */
        const val DEFAULT_ACTIVATED: Boolean = false
    }
}
