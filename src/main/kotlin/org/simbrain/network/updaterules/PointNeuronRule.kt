package org.simbrain.network.updaterules

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.util.BiasedMatrixData
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.math.SimbrainMath
import java.util.*

/**
 * **PointNeuron** from O'Reilley and Munakata, Computational Explorations in
 * Cognitive Neuroscience, chapter 2. All page references below are are to this
 * book.
 */
class PointNeuronRule : NeuronUpdateRule<BiasedScalarData, BiasedMatrixData>() {
    /**
     * Excitatory inputs for connected Synapses.
     */
    private var excitatoryInputs = ArrayList<Synapse>()

    /**
     * Inhibitory inputs for connected Synapses.
     */
    private val inhibitoryInputs = ArrayList<Synapse>()

    /**
     * Time average constant for updating the net current field. (p. 43-44)
     */
    var netTimeConstant: Double = 0.7

    /**
     * Max excitatory conductance field. Conductance if all channels are open.
     * (p. 49)
     */
    var excitatoryMaxConductance: Double = 0.4

    /**
     * Excitatory conductance field. Proportion of channels open.
     */
    var excitatoryConductance: Double = 0.0

    /**
     * Current inhibitory conductance.
     */
    var inhibitoryConductance: Double = 0.0

    /**
     * Maximal inhibitory conductance.
     */
    var inhibitoryMaxConductance: Double = 1.0

    /**
     * Membrane potential field. (p. 45)
     */
    var membranePotential: Double = DEFAULT_MEMBRANE_POTENTIAL

    /**
     * Excitatory reversal potential field. (p. 45)
     */
    var excitatoryReversal: Double = 1.0

    /**
     * Leak reversal potential field. (p. 45)
     */
    var leakReversal: Double = 0.15

    /**
     * Max leak conductance field. Conductance if all channels are open. (p. 49)
     */
    var leakMaxConductance: Double = 2.8

    /**
     * Leak Conductance field. Proportion of channels open. (p. 49)
     */
    var leakConductance: Double = 1.0

    /**
     * Net current field. Sum of all currents.
     */
    private var netCurrent = 0.0

    /**
     * Time averaging constant for updating the membrane potential field. (p.
     * 37, Equation 2.7)
     */
    var potentialTimeConstant: Double = 0.1

    /**
     * Excitatory current field.
     */
    var excitatoryCurrent: Double = 0.0

    /**
     * Leak current field.
     */
    private var leakCurrent = 0.0

    /**
     * Inhibitory current field.
     */
    private var inhibitoryCurrent = 0.0

    /**
     * Inhibitory reversal field.
     */
    var inhibitoryReversal: Double = 0.15

    /**
     * Current output function.
     */
    var outputFunction: OutputFunction = OutputFunction.DISCRETE_SPIKING

    /**
     * Gain factor for output function. (p. 46)
     */
    var gain: Double = 600.0

    /**
     * Threshold of excitation field. (p. 45)
     */
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
            /** {@inheritDoc}  */
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

        /**
         * @return the name of the output function.
         */
        abstract override fun toString(): String
    }

    fun init(neuron: Neuron) {
        setInputLists(neuron)
    }

    /**
     * Update the lists of excitatory and inhibitory currents based on synapse
     * values.
     *
     * @param neuron the neuron to set the input list for
     */
    private fun setInputLists(neuron: Neuron) {
        excitatoryInputs.clear()
        inhibitoryInputs.clear()

        for (synapse in neuron.fanIn) {
            addSynapseToList(synapse)
        }
    }

    /**
     * Adds a synapse to the appropriate internal list.
     *
     * @param synapse synapse to add.
     */
    private fun addSynapseToList(synapse: Synapse) {
        if (excitatoryInputs.contains(synapse)) {
            excitatoryInputs.remove(synapse)
        }
        if (inhibitoryInputs.contains(synapse)) {
            inhibitoryInputs.remove(synapse)
        }
        if (synapse.strength > 0) {
            excitatoryInputs.add(synapse)
        } else {
            inhibitoryInputs.add(synapse)
        }
    }

    override val timeType: Network.TimeType
        get() = Network.TimeType.DISCRETE

    override fun deepCopy(): PointNeuronRule {
        val cn = PointNeuronRule()
        // TODO
        return cn
    }

    override fun clear(neuron: Neuron) {
        membranePotential = DEFAULT_MEMBRANE_POTENTIAL
        neuron.activation = 0.0
        excitatoryConductance = 0.0
        inhibitoryConductance = 0.0
        leakConductance = 0.0
        excitatoryCurrent = 0.0
        leakCurrent = 0.0
        inhibitoryCurrent = 0.0
        netCurrent = 0.0
        setInputLists(neuron) // Temporary hack to allow input lists to be
        // updated by pressing "clear"
    }

    context(Network)
    override fun apply(neuron: Neuron, data: BiasedScalarData) {
        // Calculate the excitatory conductance (p. 44, eq. 2.16)

        excitatoryConductance =
            (1 - netTimeConstant) * excitatoryConductance + netTimeConstant * (getExcitatoryInputs())

        // Calculate the excitatory current (p. 37 equation 2.5)
        excitatoryCurrent = excitatoryConductance * excitatoryMaxConductance * (membranePotential - excitatoryReversal)

        // Calculate the excitatory conductance using time averaging constant.
        inhibitoryConductance =
            (1 - netTimeConstant) * inhibitoryConductance + netTimeConstant * (getInhibitoryInputs())

        // Calculate the inhibitory current.
        inhibitoryCurrent = inhibitoryConductance * inhibitoryMaxConductance * (membranePotential - inhibitoryReversal)

        // Calculate the leak current (p. 37 eq. 2.5)
        leakCurrent = leakConductance * leakMaxConductance * (membranePotential - leakReversal)

        // Calculate the net current (p. 37 eq. 2.6)
        netCurrent = leakCurrent + excitatoryCurrent + inhibitoryCurrent

        // Calculate the membrane potential given net current. (p.37 eq. 2.7)
        membranePotential += -potentialTimeConstant * netCurrent

        // Apply output function. (p. 45-48)
        if (outputFunction === OutputFunction.DISCRETE_SPIKING) {
            if (membranePotential > thresholdPotential) {
                neuron.activation = 1.0
                membranePotential = refractoryPotential
            } else {
                neuron.activation = 0.0
            }
        } else if (outputFunction === OutputFunction.RATE_CODE) {
            val `val` =
                (gain * getPositiveComponent(membranePotential - thresholdPotential)) / (gain * getPositiveComponent(
                    membranePotential - thresholdPotential
                ) + 1)
            // TODO: Correct way to bias for this rule?
            neuron.activation = `val` + data.bias
        } else if (outputFunction === OutputFunction.LINEAR) {
            val `val` = gain * getPositiveComponent(membranePotential - thresholdPotential)
            // TODO: Correct way to bias for this rule?
            neuron.activation = `val` + data.bias
        } else if (outputFunction === OutputFunction.NOISY_RATE_CODE) {
            neuron.activation = 1.0 // TODO: Complete this implementation
        } else if (outputFunction === OutputFunction.NONE) {
            neuron.activation = membranePotential
        }

        // Display current values of variables for diagnostics.
        // printState(neuron);
    }

    override fun createScalarData(): BiasedScalarData {
        return BiasedScalarData()
    }

    override val randomValue: Double
        get() {
            val rand = Random()
            return if (outputFunction === OutputFunction.DISCRETE_SPIKING) {
                if (rand.nextBoolean()) 1.0 else 0.0
            } else if (outputFunction === OutputFunction.RATE_CODE) {
                rand.nextDouble()
            } else if (outputFunction === OutputFunction.LINEAR) {
                // TODO: better value for this?
                gain * thresholdPotential * rand.nextDouble()
            } else if (outputFunction === OutputFunction.NOISY_RATE_CODE) {
                0.0 // TODO: COmplete implementation
            } else {
                rand.nextDouble() // TODO: Better value for this?
            }
        }

    val inhibitoryThresholdConductance: Double
        /**
         * Returns the inhibitory conductance that would set this point neuron's
         * voltage at its threshold potential. See M/R p. 101, equation 3.2
         *
         * @return the value of that equation
         */
        get() {
            val excitatoryTerm =
                excitatoryConductance * excitatoryMaxConductance * (excitatoryReversal - thresholdPotential)
            val leakTerm = leakConductance * leakMaxConductance * (leakReversal - thresholdPotential)

            return (excitatoryTerm + leakTerm) / (thresholdPotential - inhibitoryReversal)
        }

    override fun getToolTipText(neuron: Neuron): String? {
        return """Activation: ${neuron.activation}

Membrane Potential: ${SimbrainMath.roundDouble(membranePotential, 2)}

Net Current: ${SimbrainMath.roundDouble(netCurrent, 2)}

Excitatory current:  ${SimbrainMath.roundDouble(excitatoryCurrent, 2)}
 
Leak current: ${SimbrainMath.roundDouble(leakCurrent, 2)}"""
    }

    // TODO: Never Used Locally: Schedule for removal?
    // /**
    // * Print debugging information.
    // */
    // private void printState(Neuron neuron) {
    // // System.out.println("\nNeuron: " + this.getId());
    // System.out.println("excitatoryConductance:"
    // + SimbrainMath.roundDouble(excitatoryConductance, 2));
    // System.out.println("excitatoryCurrent:"
    // + SimbrainMath.roundDouble(excitatoryCurrent, 2));
    // System.out.println("inhibitoryCurrent:"
    // + SimbrainMath.roundDouble(inhibitoryCurrent, 2));
    // System.out.println("leakCurrent:"
    // + SimbrainMath.roundDouble(leakCurrent, 2));
    // System.out.println("netCurrent:"
    // + SimbrainMath.roundDouble(netCurrent, 2));
    // System.out.println("membranePotential:"
    // + SimbrainMath.roundDouble(membranePotential, 2));
    // System.out.println("output:" + neuron.getActivation());
    // }
    /**
     * Returns net input to this neuron (source activations times weights), from
     * excitatory sources only.
     *
     * @return net input
     */
    private fun getExcitatoryInputs(): Double {
        var retVal = 0.0
        if (excitatoryInputs.size > 0) {
            for (synapse in excitatoryInputs) {
                val source = synapse.source
                // Will not work with spiking, or negative activations?
                retVal += source.activation * synapse.strength
            }
        }
        return retVal
    }

    /**
     * Returns net input to this neuron (source activations times weights), from
     * inhibitory sources only.
     *
     * @return net input
     */
    private fun getInhibitoryInputs(): Double {
        var retVal = 0.0
        if (inhibitoryInputs.size > 0) {
            for (synapse in inhibitoryInputs) {
                val source = synapse.source
                // Will not work with spiking, or negative activations?
                retVal += source.activation * synapse.strength
            }
        }
        return retVal
    }

    /**
     * Returns the positive component of a number.
     *
     * @param val value to consider
     * @return positive component
     */
    private fun getPositiveComponent(`val`: Double): Double {
        return if (`val` > 0) {
            `val`
        } else {
            0.0
        }
    }

    fun setExcitatoryInputs(excitatoryInputs: ArrayList<Synapse>) {
        this.excitatoryInputs = excitatoryInputs
    }


    override val name: String
        // TODO
        get() = "Point Neuron" //
    //    @Override
    //    public double getUpperBound() {
    //        if (outputFunction == OutputFunction.DISCRETE_SPIKING) {
    //            return 1.0;
    //        } else if (outputFunction == OutputFunction.RATE_CODE) {
    //            return 1.0;
    //        } else if (outputFunction == OutputFunction.LINEAR) {
    //            return gain; // TODO: better value for this?
    //        } else if (outputFunction == OutputFunction.NOISY_RATE_CODE) {
    //            return 0; // TODO: COmplete implementation
    //        } else {
    //            return 1.0;
    //        }
    //    }
    //
    //    @Override
    //    public double getLowerBound() {
    //        return 0;
    //    }

    companion object {
        /**
         * Default value for membrane potential.
         */
        private const val DEFAULT_MEMBRANE_POTENTIAL = .15
    }
}