/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.neuron_update_rules;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.util.BiasedMatrixData;
import org.simbrain.network.util.BiasedScalarData;
import org.simbrain.util.math.SimbrainMath;

import java.util.ArrayList;
import java.util.Random;

/**
 * <b>PointNeuron</b> from O'Reilley and Munakata, Computational Explorations in
 * Cognitive Neuroscience, chapter 2. All page references below are are to this
 * book.
 */
public class PointNeuronRule extends NeuronUpdateRule<BiasedScalarData, BiasedMatrixData> {

    /**
     * Excitatory inputs for connected Synapses.
     */
    private ArrayList<Synapse> excitatoryInputs = new ArrayList<Synapse>();

    /**
     * Inhibitory inputs for connected Synapses.
     */
    private ArrayList<Synapse> inhibitoryInputs = new ArrayList<Synapse>();

    /**
     * Time average constant for updating the net current field. (p. 43-44)
     */
    private double netTimeConstant = 0.7;

    /**
     * Max excitatory conductance field. Conductance if all channels are open.
     * (p. 49)
     */
    private double excitatoryMaxConductance = 0.4;

    /**
     * Excitatory conductance field. Proportion of channels open.
     */
    private double excitatoryConductance;

    /**
     * Current inhibitory conductance.
     */
    private double inhibitoryConductance;

    /**
     * Maximal inhibitory conductance.
     */
    private double inhibitoryMaxConductance = 1;

    /**
     * Default value for membrane potential.
     */
    private static final double DEFAULT_MEMBRANE_POTENTIAL = .15;

    /**
     * Membrane potential field. (p. 45)
     */
    private double membranePotential = DEFAULT_MEMBRANE_POTENTIAL;

    /**
     * Excitatory reversal potential field. (p. 45)
     */
    private double excitatoryReversal = 1;

    /**
     * Leak reversal potential field. (p. 45)
     */
    private double leakReversal = 0.15;

    /**
     * Max leak conductance field. Conductance if all channels are open. (p. 49)
     */
    private double leakMaxConductance = 2.8;

    /**
     * Leak Conductance field. Proportion of channels open. (p. 49)
     */
    private double leakConductance = 1;

    /**
     * Net current field. Sum of all currents.
     */
    private double netCurrent;

    /**
     * Time averaging constant for updating the membrane potential field. (p.
     * 37, Equation 2.7)
     */
    private double potentialTimeConstant = 0.1;

    /**
     * Excitatory current field.
     */
    private double excitatoryCurrent;

    /**
     * Leak current field.
     */
    private double leakCurrent;

    /**
     * Inhibitory current field.
     */
    private double inhibitoryCurrent;

    /**
     * Inhibitory reversal field.
     */
    private double inhibitoryReversal = 0.15;

    /**
     * Current output function.
     */
    private OutputFunction outputFunction = OutputFunction.DISCRETE_SPIKING;

    /**
     * Gain factor for output function. (p. 46)
     */
    private double gain = 600;

    /**
     * Threshold of excitation field. (p. 45)
     */
    private double thresholdPotential = 0.25;

    /**
     * Duration of spike for DISCRETE_SPIKING output function. Used to extend
     * spike across multiple cycles (p. 46).
     */
    private int duration = 1; // TODO: Implement and verify against Emergent

    /**
     * Membrane potential after spike for DISCRETE_SPIKING output function. (p.
     * 46)
     */
    private double refractoryPotential;

    /**
     * Output functions. (p. 45-48)
     */
    public enum OutputFunction {

        /**
         * The spikes themselves are the output.
         */
        DISCRETE_SPIKING {
            /** {@inheritDoc} */
            @Override
            public String toString() {
                return "Discrete Spiking";
            }
        },

        /**
         * The number of spikes over a given time is translated into a
         * continuous rate value without being passed through any other
         * function.
         */
        LINEAR {
            @Override
            public String toString() {
                return "Linear";
            }
        },

        /**
         * The number of spikes over a given time is translated into a
         * continuous rate value which is then put through a squashing function
         * to represent saturation or a max and min firing rate.
         */
        RATE_CODE {
            @Override
            public String toString() {
                return "Rate Code";
            }
        },

        /**
         * TODO: No implementation.
         */
        NOISY_RATE_CODE {
            @Override
            public String toString() {
                return "Noisy Rate Code";
            }
        },

        /**
         * The membrane potential is the output.
         */
        NONE {
            @Override
            public String toString() {
                return "Membrane Potential";
            }
        };

        /**
         * @return the name of the output function.
         */
        public abstract String toString();

    }

    public void init(Neuron neuron) {
        setInputLists(neuron);
    }

    /**
     * Update the lists of excitatory and inhibitory currents based on synapse
     * values.
     *
     * @param neuron the neuron to set the input list for
     */
    private void setInputLists(Neuron neuron) {
        excitatoryInputs.clear();
        inhibitoryInputs.clear();

        for (Synapse synapse : neuron.getFanIn()) {
            addSynapseToList(synapse);
        }
    }

    /**
     * Adds a synapse to the appropriate internal list.
     *
     * @param synapse synapse to add.
     */
    private void addSynapseToList(Synapse synapse) {
        if (excitatoryInputs.contains(synapse)) {
            excitatoryInputs.remove(synapse);
        }
        if (inhibitoryInputs.contains(synapse)) {
            inhibitoryInputs.remove(synapse);
        }
        if (synapse.getStrength() > 0) {
            excitatoryInputs.add(synapse);
        } else {
            inhibitoryInputs.add(synapse);
        }
    }

    @Override
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    @Override
    public PointNeuronRule deepCopy() {
        PointNeuronRule cn = new PointNeuronRule();
        // TODO
        return cn;
    }

    @Override
    public void clear(final Neuron neuron) {
        membranePotential = DEFAULT_MEMBRANE_POTENTIAL;
        neuron.setActivation(0);
        neuron.setActivation(0);
        excitatoryConductance = 0;
        inhibitoryConductance = 0;
        leakConductance = 0;
        excitatoryCurrent = 0;
        leakCurrent = 0;
        inhibitoryCurrent = 0;
        netCurrent = 0;
        setInputLists(neuron); // Temporary hack to allow input lists to be
        // updated by pressing "clear"
    }

    @Override
    public void apply(Neuron neuron, BiasedScalarData data) {

        // Calculate the excitatory conductance (p. 44, eq. 2.16)
        excitatoryConductance = (1 - netTimeConstant) * excitatoryConductance + netTimeConstant * (getExcitatoryInputs());

        // Calculate the excitatory current (p. 37 equation 2.5)
        excitatoryCurrent = excitatoryConductance * excitatoryMaxConductance * (membranePotential - excitatoryReversal);

        // Calculate the excitatory conductance using time averaging constant.
        inhibitoryConductance = (1 - netTimeConstant) * inhibitoryConductance + netTimeConstant * (getInhibitoryInputs());

        // Calculate the inhibitory current.
        inhibitoryCurrent = inhibitoryConductance * inhibitoryMaxConductance * (membranePotential - inhibitoryReversal);

        // Calculate the leak current (p. 37 eq. 2.5)
        leakCurrent = leakConductance * leakMaxConductance * (membranePotential - leakReversal);

        // Calculate the net current (p. 37 eq. 2.6)
        netCurrent = leakCurrent + excitatoryCurrent + inhibitoryCurrent;

        // Calculate the membrane potential given net current. (p.37 eq. 2.7)
        membranePotential += -potentialTimeConstant * netCurrent;

        // Apply output function. (p. 45-48)
        if (outputFunction == OutputFunction.DISCRETE_SPIKING) {
            if (membranePotential > thresholdPotential) {
                neuron.setActivation(1);
                membranePotential = refractoryPotential;
            } else {
                neuron.setActivation(0);
            }
        } else if (outputFunction == OutputFunction.RATE_CODE) {
            double val = (gain * getPositiveComponent(membranePotential - thresholdPotential)) / (gain * getPositiveComponent(membranePotential - thresholdPotential) + 1);
            // TODO: Correct way to bias for this rule?
            neuron.setActivation(val + data.getBias());
        } else if (outputFunction == OutputFunction.LINEAR) {
            double val = gain * getPositiveComponent(membranePotential - thresholdPotential);
            // TODO: Correct way to bias for this rule?
            neuron.setActivation(val + data.getBias());
        } else if (outputFunction == OutputFunction.NOISY_RATE_CODE) {
            neuron.setActivation(1); // TODO: Complete this implementation
        } else if (outputFunction == OutputFunction.NONE) {
            neuron.setActivation(membranePotential);
        }

        // Display current values of variables for diagnostics.
        // printState(neuron);
    }

    @Override
    public BiasedScalarData createScalarData() {
        return new BiasedScalarData();
    }

    @Override
    public double getRandomValue() {
        Random rand = new Random();
        if (outputFunction == OutputFunction.DISCRETE_SPIKING) {
            return rand.nextBoolean() ? 1.0 : 0.0;
        } else if (outputFunction == OutputFunction.RATE_CODE) {
            return rand.nextDouble();
        } else if (outputFunction == OutputFunction.LINEAR) {
            // TODO: better value for this?
            return gain * thresholdPotential * rand.nextDouble();
        } else if (outputFunction == OutputFunction.NOISY_RATE_CODE) {
            return 0; // TODO: COmplete implementation
        } else {
            return rand.nextDouble(); // TODO: Better value for this?
        }
    }

    /**
     * Returns the inhibitory conductance that would set this point neuron's
     * voltage at its threshold potential. See M/R p. 101, equation 3.2
     *
     * @return the value of that equation
     */
    public double getInhibitoryThresholdConductance() {
        double excitatoryTerm = excitatoryConductance * excitatoryMaxConductance * (excitatoryReversal - thresholdPotential);
        double leakTerm = leakConductance * leakMaxConductance * (leakReversal - thresholdPotential);

        return (excitatoryTerm + leakTerm) / (thresholdPotential - inhibitoryReversal);
    }

    @Override
    public String getToolTipText(final Neuron neuron) {
        return "Activation: " + neuron.getActivation() + "\n\nMembrane Potential: " + SimbrainMath.roundDouble(membranePotential, 2) + "\n\nNet Current: " + SimbrainMath.roundDouble(netCurrent, 2) + "\n\nExcitatory current:  " + SimbrainMath.roundDouble(excitatoryCurrent, 2) + "\n \nLeak current: " + SimbrainMath.roundDouble(leakCurrent, 2);
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
    private double getExcitatoryInputs() {

        double retVal = 0;
        if (excitatoryInputs.size() > 0) {
            for (Synapse synapse : excitatoryInputs) {
                Neuron source = synapse.getSource();
                // Will not work with spiking, or negative activations?
                retVal += source.getActivation() * synapse.getStrength();
            }
        }
        return retVal;
    }

    /**
     * Returns net input to this neuron (source activations times weights), from
     * inhibitory sources only.
     *
     * @return net input
     */
    private double getInhibitoryInputs() {

        double retVal = 0;
        if (inhibitoryInputs.size() > 0) {
            for (Synapse synapse : inhibitoryInputs) {
                Neuron source = synapse.getSource();
                // Will not work with spiking, or negative activations?
                retVal += source.getActivation() * synapse.getStrength();
            }
        }
        return retVal;
    }

    /**
     * Returns the positive component of a number.
     *
     * @param val value to consider
     * @return positive component
     */
    private double getPositiveComponent(double val) {

        if (val > 0) {
            return val;
        } else {
            return 0;
        }
    }

    public double getNetTimeConstant() {
        return netTimeConstant;
    }

    public void setNetTimeConstant(double netTimeConstant) {
        this.netTimeConstant = netTimeConstant;
    }

    public double getExcitatoryMaxConductance() {
        return excitatoryMaxConductance;
    }

    public void setExcitatoryMaxConductance(double excitatoryMaxConductance) {
        this.excitatoryMaxConductance = excitatoryMaxConductance;
    }

    public double getExcitatoryConductance() {
        return excitatoryConductance;
    }

    public void setExcitatoryConductance(double excitatoryConductance) {
        this.excitatoryConductance = excitatoryConductance;
    }

    public double getMembranePotential() {
        return membranePotential;
    }

    public void setMembranePotential(double membranePotential) {
        this.membranePotential = membranePotential;
    }

    public double getExcitatoryReversal() {
        return excitatoryReversal;
    }

    public void setExcitatoryReversal(double excitatoryReversal) {
        this.excitatoryReversal = excitatoryReversal;
    }

    public double getLeakReversal() {
        return leakReversal;
    }

    public void setLeakReversal(double leakReversal) {
        this.leakReversal = leakReversal;
    }

    public double getLeakMaxConductance() {
        return leakMaxConductance;
    }

    public void setLeakMaxConductance(double leakMaxConductance) {
        this.leakMaxConductance = leakMaxConductance;
    }

    public double getLeakConductance() {
        return leakConductance;
    }

    public void setLeakConductance(double leakConductance) {
        this.leakConductance = leakConductance;
    }

    public double getPotentialTimeConstant() {
        return potentialTimeConstant;
    }

    public void setPotentialTimeConstant(double potentialTimeConstant) {
        this.potentialTimeConstant = potentialTimeConstant;
    }

    public OutputFunction getOutputFunction() {
        return outputFunction;
    }

    public void setOutputFunction(OutputFunction currentOutputFunction) {
        this.outputFunction = currentOutputFunction;
    }

    public double getGain() {
        return gain;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    public double getThresholdPotential() {
        return thresholdPotential;
    }

    public void setThresholdPotential(double threshold) {
        this.thresholdPotential = threshold;
    }

    public double getRefractoryPotential() {
        return refractoryPotential;
    }

    public void setRefractoryPotential(double refractoryPotential) {
        this.refractoryPotential = refractoryPotential;
    }

    public void setExcitatoryInputs(ArrayList<Synapse> excitatoryInputs) {
        this.excitatoryInputs = excitatoryInputs;
    }

    public double getInhibitoryReversal() {
        return inhibitoryReversal;
    }

    public void setInhibitoryReversal(double inhibitoryReversal) {
        this.inhibitoryReversal = inhibitoryReversal;
    }

    public double getExcitatoryCurrent() {
        return excitatoryCurrent;
    }

    public void setExcitatoryCurrent(double excitatoryCurrent) {
        this.excitatoryCurrent = excitatoryCurrent;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getInhibitoryConductance() {
        return inhibitoryConductance;
    }

    public void setInhibitoryConductance(double inhibitoryConductance) {
        this.inhibitoryConductance = inhibitoryConductance;
    }

    public double getInhibitoryMaxConductance() {
        return inhibitoryMaxConductance;
    }

    public void setInhibitoryMaxConductance(double inhibitoryMaxConductance) {
        this.inhibitoryMaxConductance = inhibitoryMaxConductance;
    }

    // TODO
    // public void synapseAdded(NetworkEvent<Synapse> networkEvent) {
    //     Synapse synapse = networkEvent.getObject();
    //     if (synapse.getTarget().getUpdateRule() == this) {
    //         addSynapseToList(synapse);
    //     }
    // }
    //
    // public void synapseChanged(NetworkEvent<Synapse> networkEvent) {
    //     Synapse synapse = networkEvent.getObject();
    //     if (synapse.getTarget().getUpdateRule() == this) {
    //         addSynapseToList(synapse);
    //     }
    // }
    //
    // public void synapseRemoved(NetworkEvent<Synapse> networkEvent) {
    //     Synapse synapse = networkEvent.getObject();
    //     if (synapse.getTarget().getUpdateRule() == this) {
    //         if (excitatoryInputs.contains(synapse)) {
    //             excitatoryInputs.remove(synapse);
    //         }
    //         if (inhibitoryInputs.contains(synapse)) {
    //             inhibitoryInputs.remove(synapse);
    //         }
    //     }
    // }


    @Override
    public String getName() {
        return "Point Neuron";
    }
    //
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

}