package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.PointNeuronScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.roundToString
import org.simbrain.util.stats.ProbabilityDistribution
import java.util.*
import kotlin.math.abs

/**
 * PointNeuron from O'Reilly and Munakata, Computational Explorations in
 * Cognitive Neuroscience, chapter 2.
 * 
 * For conductances, there is a max conductance, and then a time varying 
 * conductance which should be between 0 and 1, a proportion of open channels.
 * 
 * Reversal potential are equilibrium potentials. The steady state with no inputs 
 * should be the leak reversal.
 * 
 */
class PointNeuronRule : NeuronUpdateRule<PointNeuronScalarData, EmptyMatrixData>() {

    // TODO: main tab first
    // TODO: Keep time varying conductances between 0 and 1, how to compute these
    // TODO: separate value for exponential decay or decays
    // TODO: How to clear data
    // TODO: Why does main diffeq have minus sign
    // TOOD: Conditional enablings below

    @UserParameter(
        label = "Max Excitatory Conductance",
        description = "Max excitatory conductance field. Conductance if all channels are open.",
        minimumValue = 0.0,
        order = 10,
        tab = "Conductances"
    )
    var excitatoryMaxConductance: Double = 0.4

    @UserParameter(
        label = "Max Inhibitory Conductance",
        description = "Maximal inhibitory conductance.",
        order = 20,
        tab = "Conductances"
    )
    var inhibitoryMaxConductance: Double = 1.0

    @UserParameter(
        label = "Excitatory Reversal",
        description = "Excitatory reversal potential field.",
        minimumValue = 0.0,
        order = 10,
    )
    var excitatoryReversal: Double = 1.0

    @UserParameter(
        label = "Leak Reversal",
        description = "Determines the resting membrane potential.",
        minimumValue = 0.0,
        order = 20,
    )
    var leakReversal: Double = 0.15

    @UserParameter(
        label = "Leak Conductance",
        description = "Leak conductance, which remains constant. Determines how quickly it returns to resting.",
        minimumValue = 0.0,
        order = 30,
        tab = "Conductances"
    )
    var leakConductance: Double = 2.8

    @UserParameter(
        label = "Inhibitory Reversal",
        description = "Inhibitory reversal field.",
        minimumValue = 0.0,
        order = 25
    )
    var inhibitoryReversal: Double = 0.15

    @UserParameter(
        label = "Output Function",
        description = "Current output function.",
        order = 100
    )
    var outputFunction: OutputFunction = OutputFunction.NONE

    // TODO: Conditionally enable
    @UserParameter(
        label = "Gain",
        description = "Gain factor for output function.",
        minimumValue = 0.0,
        order = 90
    )
    var gain: Double = 1.0

    //  TODO: Conditionally enable
    @UserParameter(
        label = "Threshold Potential",
        description = "Threshold of excitation field.",
        minimumValue = 0.0,
        order = 80
    )
    var thresholdPotential: Double = 0.25

    // TODO: Status for tooltip
    var statusString = ""

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
                return "None (Membrane Potential)"
            }
        };

        abstract override fun toString(): String
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun copy(): PointNeuronRule {
        val cn = PointNeuronRule()
        cn.excitatoryMaxConductance = excitatoryMaxConductance
        cn.excitatoryReversal = excitatoryReversal
        cn.leakReversal = leakReversal
        cn.leakConductance = leakConductance
        cn.inhibitoryReversal = inhibitoryReversal
        cn.outputFunction = outputFunction
        cn.gain = gain
        cn.thresholdPotential = thresholdPotential
        return cn
    }

    override fun clear(neuron: Neuron) {
        super.clear(neuron)
    }

    context(Network)
    override fun apply(neuron: Neuron, data: PointNeuronScalarData) {


        // Calculate the excitatory conductance (p. 44, eq. 2.16)
        data.excitatoryConductance =
            (1 - timeStep) * data.excitatoryConductance + timeStep * (getExcitatoryInputs(neuron))

        // Calculate the excitatory current (p. 37 equation 2.5)
        val excitatoryCurrent = data.excitatoryConductance * excitatoryMaxConductance * (data.membranePotential - excitatoryReversal)

        // Calculate the excitatory conductance using time averaging constant.
        data.inhibitoryConductance =  (1 - timeStep) * data.inhibitoryConductance + timeStep * (getInhibitoryInputs(neuron))

        // Calculate the inhibitory current.
        val inhibitoryCurrent = data.inhibitoryConductance * inhibitoryMaxConductance * (data.membranePotential - inhibitoryReversal)

        // Calculate the leak current (p. 37 eq. 2.5)
        val leakCurrent = leakConductance * (data.membranePotential - leakReversal)

        // Calculate the net current (p. 37 eq. 2.6)
        val netCurrent = leakCurrent + excitatoryCurrent + inhibitoryCurrent

        // Calculate the membrane potential given net current. (p.37 eq. 2.7)
        data.membranePotential += timeStep * -netCurrent

        statusString = """
            -----
            membrane potential ${data.membranePotential.roundToString(2)}
            excitatory conductance ${data.excitatoryConductance.roundToString(2)}
            inhibitory conductance ${data.inhibitoryConductance.roundToString(2)}
        """.trimIndent()

        println(statusString)

        // Apply output function. (p. 45-48)
        if (outputFunction === OutputFunction.DISCRETE_SPIKING) {
            if (data.membranePotential > thresholdPotential) {
                neuron.activation = 1.0
                // data.membranePotential = refractoryPotential
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

    private fun getExcitatoryInputs(neuron: Neuron): Double {
        return neuron.fanIn.filter { it.strength > 0.0 }.sumOf { it.psr }
    }

    private fun getInhibitoryInputs(neuron: Neuron): Double {
        return neuron.fanIn.filter { it.strength < 0.0 }.sumOf { it.psr }
    }

    override fun getToolTipText(neuron: Neuron): String? {
        return statusString
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
