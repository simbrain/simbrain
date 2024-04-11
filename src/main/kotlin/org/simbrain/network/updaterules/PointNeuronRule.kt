package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.PointNeuronScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import java.util.*
import kotlin.math.abs

/**
 * PointNeuron from O'Reilly and Munakata, Computational Explorations in
 * Cognitive Neuroscience, chapter 2.
 */
class PointNeuronRule : NeuronUpdateRule<PointNeuronScalarData, EmptyMatrixData>() {

    // TODO: organize params into tabs

    // TODO: Use simbrain time constant?
    @UserParameter(
        label = "Net Time Constant",
        description = "Time average constant for updating the net current field",
        minimumValue = 0.0
    )
    var netTimeConstant: Double = 0.7

    @UserParameter(
        label = "Max Excitatory Conductance",
        description = "Max excitatory conductance field. Conductance if all channels are open.",
        minimumValue = 0.0
    )
    var excitatoryMaxConductance: Double = 0.4

    @UserParameter(
        label = "Max Inhibitory Conductance",
        description = "Maximal inhibitory conductance.",
        minimumValue = 0.0
    )
    var inhibitoryMaxConductance: Double = 1.0

    @UserParameter(
        label = "Excitatory Reversal",
        description = "Excitatory reversal potential field.",
        minimumValue = 0.0
    )
    var excitatoryReversal: Double = 1.0

    @UserParameter(
        label = "Leak Reversal",
        description = "Leak reversal potential field.",
        minimumValue = 0.0
    )
    var leakReversal: Double = 0.15

    @UserParameter(
        label = "Max Leak Conductance",
        description = "Max leak conductance field. Conductance if all channels are open.",
        minimumValue = 0.0
    )
    var leakMaxConductance: Double = 2.8

    @UserParameter(
        label = "Potential Time Constant",
        description = "Time averaging constant for updating the membrane potential field.",
        minimumValue = 0.0
    )
    var potentialTimeConstant: Double = 0.1

    @UserParameter(
        label = "Inhibitory Reversal",
        description = "Inhibitory reversal field.",
        minimumValue = 0.0
    )
    var inhibitoryReversal: Double = 0.15

    @UserParameter(
        label = "Output Function",
        description = "Current output function."
    )
    var outputFunction: OutputFunction = OutputFunction.LINEAR

    @UserParameter(
        label = "Gain",
        description = "Gain factor for output function.",
        minimumValue = 0.0
    )
    var gain: Double = 1.0

    @UserParameter(
        label = "Threshold Potential",
        description = "Threshold of excitation field.",
        minimumValue = 0.0
    )
    var thresholdPotential: Double = 0.25

    /**
     * Duration of spike for DISCRETE_SPIKING output function. Used to extend
     * spike across multiple cycles (p. 46).
     */
    var duration: Int = 1 // TODO: Implement and verify against Emergent

    /**
     * Membrane potential after spike for DISCRETE_SPIKING output function. (p.
     * 46)
     */
    var refractoryPotential: Double = 0.0

    /**
     * Output functions. (p. 45-48)
     */
    enum class OutputFunction {
        /**
         * The spikes themselves are the output.
         */
        DISCRETE_SPIKING {
            override fun toString(): String {
                return "Discrete Spiking"
            }
        },

        /**
         * The number of spikes over a given time is translated into a
         * continuous rate value without being passed through any other
         * function.
         */
        LINEAR {
            override fun toString(): String {
                return "Linear"
            }
        },

        /**
         * The number of spikes over a given time is translated into a
         * continuous rate value which is then put through a squashing function
         * to represent saturation or a max and min firing rate.
         */
        RATE_CODE {
            override fun toString(): String {
                return "Rate Code"
            }
        },

        /**
         * TODO: No implementation.
         */
        NOISY_RATE_CODE {
            override fun toString(): String {
                return "Noisy Rate Code"
            }
        },

        /**
         * The membrane potential is the output.
         */
        NONE {
            override fun toString(): String {
                return "Membrane Potential"
            }
        };

        abstract override fun toString(): String
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun copy(): PointNeuronRule {
        val cn = PointNeuronRule()
        cn.netTimeConstant = netTimeConstant
        cn.excitatoryMaxConductance = excitatoryMaxConductance
        cn.excitatoryReversal = excitatoryReversal
        cn.leakReversal = leakReversal
        cn.leakMaxConductance = leakMaxConductance
        cn.potentialTimeConstant = potentialTimeConstant
        cn.inhibitoryReversal = inhibitoryReversal
        cn.outputFunction = outputFunction
        cn.gain = gain
        cn.thresholdPotential = thresholdPotential
        cn.duration = duration
        cn.refractoryPotential = refractoryPotential
        return cn
    }

    override fun clear(neuron: Neuron) {
        super.clear(neuron)
    }

    context(Network)
    override fun apply(neuron: Neuron, data: PointNeuronScalarData) {

        // Calculate the excitatory conductance (p. 44, eq. 2.16)
        data.excitatoryConductance =
            (1 - netTimeConstant) * data.excitatoryConductance + netTimeConstant * (getExcitatoryInputs(neuron))

        // Calculate the excitatory current (p. 37 equation 2.5)
        val excitatoryCurrent = data.excitatoryConductance * excitatoryMaxConductance * (data.membranePotential - excitatoryReversal)

        // Calculate the excitatory conductance using time averaging constant.
        data.inhibitoryConductance =
            (1 - netTimeConstant) * data.inhibitoryConductance + netTimeConstant * (getInhibitoryInputs(neuron))

        // Calculate the inhibitory current.
        val inhibitoryCurrent = data.inhibitoryConductance * inhibitoryMaxConductance * (data.membranePotential - inhibitoryReversal)

        // Calculate the leak current (p. 37 eq. 2.5)
        val leakCurrent = data.leakConductance * leakMaxConductance * (data.membranePotential - leakReversal)

        // Calculate the net current (p. 37 eq. 2.6)
        val netCurrent = leakCurrent + excitatoryCurrent + inhibitoryCurrent

        // Calculate the membrane potential given net current. (p.37 eq. 2.7)
        data.membranePotential += -potentialTimeConstant * netCurrent

        // Apply output function. (p. 45-48)
        if (outputFunction === OutputFunction.DISCRETE_SPIKING) {
            if (data.membranePotential > thresholdPotential) {
                neuron.activation = 1.0
                data.membranePotential = refractoryPotential
            } else {
                neuron.activation = 0.0
            }
        } else if (outputFunction === OutputFunction.RATE_CODE) {
            neuron.activation =
                (gain * abs(data.membranePotential - thresholdPotential)) / (gain * abs(
                    data.membranePotential - thresholdPotential
                ) + 1)
        } else if (outputFunction === OutputFunction.LINEAR) {
            neuron.activation = gain * abs(data.membranePotential - thresholdPotential)
        } else if (outputFunction === OutputFunction.NOISY_RATE_CODE) {
            neuron.activation = 1.0 // TODO: Complete this implementation
        } else if (outputFunction === OutputFunction.NONE) {
            neuron.activation = data.membranePotential
        }

        // Display current values of variables for diagnostics.
        // printState(neuron);
    }

    override fun createScalarData(): PointNeuronScalarData {
        return PointNeuronScalarData()
    }

    override fun getRandomValue(randomizer: ProbabilityDistribution?): Double {
        val rand = Random()
        return if (outputFunction === OutputFunction.DISCRETE_SPIKING) {
            if (rand.nextBoolean()) 1.0 else 0.0
        } else if (outputFunction === OutputFunction.RATE_CODE) {
            rand.nextDouble()
        } else if (outputFunction === OutputFunction.LINEAR) {
            // TODO: better value for this?
            gain * thresholdPotential * rand.nextDouble()
        } else if (outputFunction === OutputFunction.NOISY_RATE_CODE) {
            0.0 // TODO: Complete implementation
        } else {
            rand.nextDouble() // TODO: Better value for this?
        }
    }

    // val inhibitoryThresholdConductance: Double
    //     /**
    //      * Returns the inhibitory conductance that would set this point neuron's
    //      * voltage at its threshold potential. See M/R p. 101, equation 3.2
    //      *
    //      * @return the value of that equation
    //      */
    //     get() {
    //         val excitatoryTerm =
    //             excitatoryConductance * excitatoryMaxConductance * (excitatoryReversal - thresholdPotential)
    //         val leakTerm = leakConductance * leakMaxConductance * (leakReversal - thresholdPotential)
    //
    //         return (excitatoryTerm + leakTerm) / (thresholdPotential - inhibitoryReversal)
    //     }


    //  TODO: Store text during update
    // override fun getToolTipText(neuron: Neuron): String? {
    //     return """
    //         Activation: ${neuron.activation}
    //         Membrane Potential: ${SimbrainMath.roundDouble(membranePotential, 2)}
    //         Excitatory current:  ${SimbrainMath.roundDouble(excitatoryCurrent, 2)}
    //         Leak current: ${SimbrainMath.roundDouble(leakCurrent, 2)}
    //         """
    // }

    private fun getExcitatoryInputs(neuron: Neuron): Double {
        return neuron.fanIn.filter { it.strength > 0.0 }.sumOf { it.psr }
    }

    private fun getInhibitoryInputs(neuron: Neuron): Double {
        return neuron.fanIn.filter { it.strength < 0.0 }.sumOf { it.psr }
    }

    override val name: String
        get() = "Point Neuron"

    //    @Override
    //    public double getUpperBound() {
    //        if (outputFunction == OutputFunction.DISCRETE_SPIKING) {
    //            return 1.0;
    //        } else if (outputFunction == OutputFunction.RATE_CODE) {
    //            return 1.0;
    //        } else if (outputFunction == OutputFunction.LINEAR) {
    //            return gain; // TODO: better value for this?
    //        } else if (outputFunction == OutputFunction.NOISY_RATE_CODE) {
    //            return 0; // TODO: Complete implementation
    //        } else {
    //            return 1.0;
    //        }
    //    }
    //
    //    @Override
    //    public double getLowerBound() {
    //        return 0;
    //    }


}
