package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.APETabOder
import org.simbrain.util.roundToString
import org.simbrain.util.stats.ProbabilityDistribution
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
@APETabOder( "Main", "Conductances")
class PointNeuronRule : NeuronUpdateRule<PointNeuronScalarData, EmptyMatrixData>() {

    // TODO: Keep time varying conductances between 0 and 1, how to compute these
    // TOOD: Conditional enablings below

    @UserParameter(
        label = "Max Excitatory Conductance",
        description = "Higher values -> faster approach to excitatory reversal.",
        minimumValue = 0.0,
        increment = .1,
        order = 10,
        tab = "Conductances"
    )
    var excitatoryMaxConductance: Double = 0.4

    @UserParameter(
        label = "Max Inhibitory Conductance",
        description = "Higher values -> faster approach to inhibitory reversal.",
        order = 20,
        increment = .1,
        minimumValue = 0.0,
        tab = "Conductances"
    )
    var inhibitoryMaxConductance: Double = 1.0

    @UserParameter(
        label = "Leak Conductance",
        description = "Higher values -> approaches leak reversal quicker.",
        minimumValue = 0.0,
        increment = .1,
        order = 30,
        tab = "Conductances"
    )
    var leakConductance: Double = 2.8

    @UserParameter(
        label = "Excitatory Reversal",
        description = "Equilibrium potential for excitatory currents.",
        minimumValue = 0.0,
        increment = 0.1,
        order = 10,
    )
    var excitatoryReversal: Double = 1.0

    @UserParameter(
        label = "Inhibitory Reversal",
        description = "Inhibitory reversal field.",
        minimumValue = 0.0,
        increment = 0.1,
        order = 20
    )
    var inhibitoryReversal: Double = 0.15

    @UserParameter(
        label = "Leak Reversal",
        description = "Equilibrium resting potential. With no inputs the voltage will approaches this.",
        minimumValue = 0.0,
        increment = 0.1,
        order = 25,
    )
    var leakReversal: Double = 0.15


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

    var toolTipString = ""

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
        cn.inhibitoryMaxConductance = inhibitoryMaxConductance
        cn.excitatoryReversal = excitatoryReversal
        cn.leakReversal = leakReversal
        cn.leakConductance = leakConductance
        cn.inhibitoryReversal = inhibitoryReversal
        cn.outputFunction = outputFunction
        cn.gain = gain
        cn.thresholdPotential = thresholdPotential
        return cn
    }

    context(Network)
    override fun apply(neuron: Neuron, data: PointNeuronScalarData) {


        // Calculate the excitatory conductance (p. 44, eq. 2.16)
        data.excitatoryConductance =
            (1 - timeStep) * data.excitatoryConductance + timeStep * (getExcitatoryInputs(neuron))

        // Calculate the excitatory current (p. 37 equation 2.5)v
        val excitatoryCurrent = (data.excitatoryConductance * excitatoryMaxConductance) * (excitatoryReversal - data.membranePotential )

        // Calculate the inhibitory conductance using time averaging constant.
        data.inhibitoryConductance =  (1 - timeStep) * data.inhibitoryConductance + timeStep * (getInhibitoryInputs(neuron))

        // Calculate the inhibitory current.
        val inhibitoryCurrent = (data.inhibitoryConductance * inhibitoryMaxConductance) * (inhibitoryReversal - data.membranePotential )

        // Calculate the leak current (p. 37 eq. 2.5)
        val leakCurrent = leakConductance * (leakReversal - data.membranePotential )

        // Calculate the net current (p. 37 eq. 2.6)
        val netCurrent = leakCurrent + excitatoryCurrent + inhibitoryCurrent

        // Calculate the membrane potential given net current. (p.37 eq. 2.7)
        data.membranePotential += timeStep * netCurrent

        toolTipString = """
            membrane potential ${data.membranePotential.roundToString(2)}<br>
            excitatory conductance ${data.excitatoryConductance.roundToString(2)}<br>
            inhibitory conductance ${data.inhibitoryConductance.roundToString(2)}<br>
            leak current ${leakCurrent.roundToString(2)}
        """.trimIndent()

        println(toolTipString)

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

    private fun getExcitatoryInputs(neuron: Neuron): Double {
        return max(0.0, neuron.fanIn.filter { it.strength > 0.0 }.sumOf { it.psr })
    }

    private fun getInhibitoryInputs(neuron: Neuron): Double {
        return min(0.0, neuron.fanIn.filter { it.strength < 0.0 }.sumOf { it.psr })
    }

    override fun getToolTipText(neuron: Neuron): String? {
        return toolTipString
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

class PointNeuronScalarData(

    @UserParameter(
        label = "Membrane potential",
        minimumValue = 0.0
    )
    var membranePotential: Double = .15,

    @UserParameter(
        label = "Excitatory Conductance",
        description = "Current excitatory conductance.Proportion of channels open",
        minimumValue = 0.0
    )
    var excitatoryConductance: Double = 0.0,

    @UserParameter(
        label = "Inhibitory Conductance",
        description = "Current inhibitory conductance. Proportion of channels open",
        minimumValue = 0.0
    )
    var inhibitoryConductance: Double = 0.0,

    ) : ScalarDataHolder {
    override fun copy(): PointNeuronScalarData {
        return PointNeuronScalarData(membranePotential, excitatoryConductance, inhibitoryConductance)
    }

    override fun clear() {
        membranePotential = 0.0
        excitatoryConductance = 0.0
        inhibitoryConductance = 0.0
    }
}