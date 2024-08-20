package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SpikingNeuronUpdateRule
import org.simbrain.network.updaterules.LinearRule.ClippingType
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.UserParameter
import org.simbrain.util.clip
import org.simbrain.util.propertyeditor.APETabOder
import org.simbrain.util.propertyeditor.GuiEditable
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
 * Leabra assumes values between 0 and 1 both for weights and activations. Nothing has been done to ensure this
 * is done in Simbrain beyond clipping, so it is up to the user to ensure this constraint is met.
 *
 * Reversal potential are equilibrium potentials. The steady state with no inputs 
 * should be the leak reversal.
 * 
 */
@APETabOder( "Main", "Conductances")
class PointNeuronRule : SpikingNeuronUpdateRule<PointNeuronScalarData, SpikingMatrixData>() {

    @UserParameter(
        label = "Output Function",
        description = "Current output function.",
        order = 5
    )
    var outputFunction: OutputFunction = OutputFunction.RATE_CODE

    @UserParameter(
        label = "Excitatory Reversal",
        description = "Equilibrium potential for excitatory currents.",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = 0.1,
        order = 10,
    )
    var excitatoryReversal: Double = 1.0

    @UserParameter(
        label = "Inhibitory Reversal",
        description = "Inhibitory reversal field.",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = 0.1,
        order = 20
    )
    var inhibitoryReversal: Double = 0.25

    @UserParameter(
        label = "Leak Reversal",
        description = "Equilibrium resting potential. With no inputs the voltage will approaches this.",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = 0.1,
        order = 25,
    )
    var leakReversal: Double = 0.3

    @UserParameter(
        label = "Reset potential",
        description = "Membrane potential to reset to after a spike",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = 0.1,
        order = 30,
    )
    var resetPotential: Double = 0.1

    var gain: Double by GuiEditable(
        initValue = 1.0,
        label = "Gain",
        description = "Gain factor for output function.",
        min = 0.0,
        max = 1.0,
        order = 90,
        onUpdate = {
            enableWidget(widgetValue(::outputFunction) == OutputFunction.RATE_CODE)
        }
    )

    var thresholdPotential by GuiEditable(
        initValue = .5,
        label = "Threshold Potential",
        description = "Threshold of excitation field.",
        increment = .1,
        min = 0.0,
        max = 1.0,
        order = 80
    )


    @UserParameter(
        label = "Max Excitatory Conductance",
        description = "Higher values -> faster approach to excitatory reversal.",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = .1,
        order = 10,
        tab = "Conductances"
    )
    var excitatoryMaxConductance: Double = 1.0

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
    var leakConductance: Double = .1


    var toolTipString = ""

    /**
     * Output functions. (p. 45-48)
     */
    enum class OutputFunction {

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

        MEMBRANE_POTENTIAL {
            override fun toString(): String {
                return "Membrane potential"
            }
        },

        /**
         * Spiking with membrane potential as activation
         */
        SPIKING {
            override fun toString(): String {
                return "Spiking"
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
        cn.resetPotential = resetPotential
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

//        println(toolTipString)

        if (outputFunction === OutputFunction.RATE_CODE) {
            // "XX1" or "X over X+1" activation function
            val inhibTheta = (data.inhibitoryConductance * inhibitoryMaxConductance) * (inhibitoryReversal - thresholdPotential )
            val leakTheta = leakConductance * (leakReversal - thresholdPotential)
            val gETheta =  (inhibTheta + leakTheta) /(thresholdPotential - excitatoryReversal)
            val x = gain * abs(excitatoryCurrent - gETheta)
            neuron.activation = x/(x+1)
        } else if (outputFunction === OutputFunction.SPIKING) {
            if(data.membranePotential > thresholdPotential) {
                neuron.isSpike = true
                data.membranePotential = resetPotential
            } else {
                neuron.isSpike= false
            }
            neuron.activation = data.membranePotential
        } else if (outputFunction === OutputFunction.MEMBRANE_POTENTIAL)  {
            // Membrane potential mode
            neuron.activation = data.membranePotential
        }

    }

    override fun createScalarData(): PointNeuronScalarData {
        return PointNeuronScalarData()
    }

    override fun getRandomValue(randomizer: ProbabilityDistribution?): Double {
        return Random().nextDouble()
    }


    /**
     * Excitatory inputs correspond to weights with strengths above 0. More notes at [#getInhibitoryInputs]
     */
    private fun getExcitatoryInputs(neuron: Neuron): Double {
        return neuron.fanIn.filter { it.strength > 0.0 }.sumOf { it.psr }.clip(0.0..1.0)
    }

    /**
     * Negative weights are assumed to correspond to inhibitory inputs. Absolute value of psr is used to be consistent with Leabra framework.
     * Example: source node is .5, weight is -1, then psr is normally -.5 in Simbrain but treated as .5 here, which is
     * interpreted as percentage of open inhibitory channels. g_l.
     *
     * Leabra assumes values between 0 and 1 so inputs are clipped.
     *
     */
    private fun getInhibitoryInputs(neuron: Neuron): Double {
        return neuron.fanIn.filter { it.strength < 0.0 }.sumOf { abs(it.psr) }.clip(0.0..1.0)
    }

    override fun getToolTipText(neuron: Neuron): String? {
        return toolTipString
    }

    override val name: String
        get() = "Point Neuron"

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

    ) : SpikingScalarData() {
    override fun copy(): PointNeuronScalarData {
        return PointNeuronScalarData(membranePotential, excitatoryConductance, inhibitoryConductance)
    }

    override fun clear() {
        membranePotential = 0.0
        excitatoryConductance = 0.0
        inhibitoryConductance = 0.0
    }
}