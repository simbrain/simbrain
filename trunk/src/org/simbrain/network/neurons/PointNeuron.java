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
package org.simbrain.network.neurons;

import java.util.ArrayList;

import org.simbrain.network.core.BiasedNeuron;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.core.RootNetwork.TimeType;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.SynapseListener;
import org.simbrain.util.SimbrainMath;

/**
 * <b>PointNeuron</b> from O'Reilley and Munakata, Computational Explorations in
 * Cognitive Neuroscience, chapter 2. All page references below are are to this
 * book.
 */
public class PointNeuron extends NeuronUpdateRule implements SynapseListener,
        BiasedNeuron {

    /** Excitatory inputs for connected Synapses. */
    private ArrayList<Synapse> excitatoryInputs = new ArrayList<Synapse>();

    /** Inhibitory inputs for connected Synapses. */
    private ArrayList<Synapse> inhibitoryInputs = new ArrayList<Synapse>();

    /** Time average constant for updating the net current field. (p. 43-44) */
    private double netTimeConstant = 0.7;

    /**
     * Max excitatory conductance field. Conductance if all channels are open.
     * (p. 49)
     */
    private double excitatoryMaxConductance = 0.4;

    /** Excitatory conductance field. Proportion of channels open. */
    private double excitatoryConductance;

    /** Current inhibitory conductance. */
    private double inhibitoryConductance;

    /** Maximal inhibitory conductance. */
    private double inhibitoryMaxConductance = 1;

    /** Default value for membrane potential. */
    private static final double DEFAULT_MEMBRANE_POTENTIAL = .15;

    /** Membrane potential field. (p. 45) */
    private double membranePotential = DEFAULT_MEMBRANE_POTENTIAL;

    /** Excitatory reversal potential field. (p. 45) */
    private double excitatoryReversal = 1;

    /** Leak reversal potential field. (p. 45) */
    private double leakReversal = 0.15;

    /**
     * Max leak conductance field. Conductance if all channels are open. (p. 49)
     */
    private double leakMaxConductance = 2.8;

    /** Leak Conductance field. Proportion of channels open. (p. 49) */
    private double leakConductance = 1;

    /** Net current field. Sum of all currents. */
    private double netCurrent;

    /**
     * Time averaging constant for updating the membrane potential field. (p.
     * 37, Equation 2.7)
     */
    private double potentialTimeConstant = 0.1;

    /** Excitatory current field. */
    private double excitatoryCurrent;

    /** Leak current field. */
    private double leakCurrent;

    /** Inhibitory current field. */
    private double inhibitoryCurrent;

    /** Inhibitory reversal field. */
    private double inhibitoryReversal = 0.15;

    /** Current output function. */
    private OutputFunction outputFunction = OutputFunction.DISCRETE_SPIKING;

    /** Gain factor for output function. (p. 46) */
    private double gain = 600;

    /** Threshold of excitation field. (p. 45) */
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

    /** Bias term. */
    private double bias;

    /** Output functions. (p. 45-48) */
    public enum OutputFunction {
        DISCRETE_SPIKING {
            public String toString() {
                return "Discrete Spiking";
            }
        },
        LINEAR {
            public String toString() {
                return "Linear";
            }
        },
        RATE_CODE {
            public String toString() {
                return "Rate Code";
            }
        },
        NOISY_RATE_CODE {
            public String toString() {
                return "Noisy Rate Code";
            }
        },
        NONE {
            public String toString() {
                return "None (activation = membrane potential)";
            }
        }
    };

    /**
     * {@inheritDoc}
     */
    public void init(Neuron neuron) {
        neuron.setLowerBound(0);
        setInputLists(neuron);
        if (neuron.getParentNetwork() != null) {
            neuron.getParentNetwork().getRootNetwork().addSynapseListener(this);
        }

    }

    /**
     * Update the lists of excitatory and inhibitory currents based on synapse
     * values.
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

    /**
     * {@inheritDoc}
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    /**
     * {@inheritDoc}
     */
    public PointNeuron deepCopy() {
        PointNeuron cn = new PointNeuron();
        cn.setBias(bias);
        // TODO
        return cn;
    }

    @Override
    public void clear(final Neuron neuron) {
        membranePotential = DEFAULT_MEMBRANE_POTENTIAL;
        neuron.setActivation(0);
        neuron.setBuffer(0);
        excitatoryConductance = 0;
        inhibitoryConductance = 0;
        leakConductance = 0;
        excitatoryCurrent = 0;
        leakCurrent = 0;
        inhibitoryCurrent = 0;
        netCurrent = 0;
        setInputLists(neuron); // Temporary hack to allow input lists to be updated by pressing "clear"
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {

        // Calculate the excitatory conductance (p. 44, eq. 2.16)
        excitatoryConductance = (1 - netTimeConstant) * excitatoryConductance
                + netTimeConstant * (getExcitatoryInputs());

        // Calculate the excitatory current (p. 37 equation 2.5)
        excitatoryCurrent = excitatoryConductance * excitatoryMaxConductance
                * (membranePotential - excitatoryReversal);

        // Calculate the excitatory conductance using time averaging constant.
        inhibitoryConductance = (1 - netTimeConstant) * inhibitoryConductance
                + netTimeConstant * (getInhibitoryInputs());

        // Calculate the inhibitory current.
        inhibitoryCurrent = inhibitoryConductance * inhibitoryMaxConductance
                * (membranePotential - inhibitoryReversal);

        // Calculate the leak current (p. 37 eq. 2.5)
        leakCurrent = leakConductance * leakMaxConductance
                * (membranePotential - leakReversal);

        // Calculate the net current (p. 37 eq. 2.6)
        netCurrent = leakCurrent + excitatoryCurrent + inhibitoryCurrent;

        // Calculate the membrane potential given net current. (p.37 eq. 2.7)
        membranePotential += -potentialTimeConstant * netCurrent;

        // Apply output function. (p. 45-48)
        if (outputFunction == OutputFunction.DISCRETE_SPIKING) {
            if (membranePotential > thresholdPotential) {
                neuron.setBuffer(1);
                membranePotential = refractoryPotential;
            } else {
                neuron.setBuffer(0);
            }
        } else if (outputFunction == OutputFunction.RATE_CODE) {
            double val = (gain * getPositiveComponent(membranePotential
                    - thresholdPotential))
                    / (gain
                            * getPositiveComponent(membranePotential
                                    - thresholdPotential) + 1);
            neuron.setBuffer(neuron.clip(val));
        } else if (outputFunction == OutputFunction.LINEAR) {
            double val = gain
                    * getPositiveComponent(membranePotential
                            - thresholdPotential);
            neuron.setBuffer(neuron.clip(val));
        } else if (outputFunction == OutputFunction.NOISY_RATE_CODE) {
            neuron.setBuffer(1); // TODO: Complete this implementation
        } else if (outputFunction == OutputFunction.NONE) {
            neuron.setBuffer(membranePotential);
        }

        // Display current values of variables for diagnostics.
        //printState(neuron);
    }

    /**
     * Returns the inhibitory conductance that would set this point neuron's
     * voltage at its threshold potential. See M/R p. 101, equation 3.2
     *
     * @return the value of that equation
     */
    public double getInhibitoryThresholdConductance() {
        double excitatoryTerm = excitatoryConductance
                * excitatoryMaxConductance
                * (excitatoryReversal - thresholdPotential);
        double leakTerm = leakConductance * leakMaxConductance
                * (leakReversal - thresholdPotential);

        return (excitatoryTerm + leakTerm)
                / (thresholdPotential - inhibitoryReversal);
    }

    @Override
    public String getToolTipText(final Neuron neuron) {
        return "Activation: " + neuron.getActivation() +
                "\n\nMembrane Potential: " +  SimbrainMath.roundDouble(membranePotential, 2) +
                "\n\nNet Current: " +  SimbrainMath.roundDouble(netCurrent, 2) + 
                "\n\nExcitatory current:  "  +SimbrainMath.roundDouble(excitatoryCurrent, 2) +   
                "\n \nLeak current: " + SimbrainMath.roundDouble(leakCurrent, 2);
    }

    /**
     * Print debugging information.
     */
    private void printState(Neuron neuron) {
        // System.out.println("\nNeuron: " + this.getId());
        System.out.println("excitatoryConductance:"
                + SimbrainMath.roundDouble(excitatoryConductance, 2));
        System.out.println("excitatoryCurrent:"
                + SimbrainMath.roundDouble(excitatoryCurrent, 2));
        System.out.println("inhibitoryCurrent:"
                + SimbrainMath.roundDouble(inhibitoryCurrent, 2));
        System.out.println("leakCurrent:"
                + SimbrainMath.roundDouble(leakCurrent, 2));
        System.out.println("netCurrent:"
                + SimbrainMath.roundDouble(netCurrent, 2));
        System.out.println("membranePotential:"
                + SimbrainMath.roundDouble(membranePotential, 2));
        System.out.println("output:" + neuron.getActivation());
    }

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

    /**
     * @return the netTimeConstant
     */
    public double getNetTimeConstant() {
        return netTimeConstant;
    }

    /**
     * @param netTimeConstant the netTimeConstant to set
     */
    public void setNetTimeConstant(double netTimeConstant) {
        this.netTimeConstant = netTimeConstant;
    }

    /**
     * @return the excitatoryMaxConductance
     */
    public double getExcitatoryMaxConductance() {
        return excitatoryMaxConductance;
    }

    /**
     * @param excitatoryMaxConductance the excitatoryMaxConductance to set
     */
    public void setExcitatoryMaxConductance(double excitatoryMaxConductance) {
        this.excitatoryMaxConductance = excitatoryMaxConductance;
    }

    /**
     * @return the excitatoryConductance
     */
    public double getExcitatoryConductance() {
        return excitatoryConductance;
    }

    /**
     * @param excitatoryConductance the excitatoryConductance to set
     */
    public void setExcitatoryConductance(double excitatoryConductance) {
        this.excitatoryConductance = excitatoryConductance;
    }

    /**
     * @return the membranePotential
     */
    public double getMembranePotential() {
        return membranePotential;
    }

    /**
     * @param membranePotential the membranePotential to set
     */
    public void setMembranePotential(double membranePotential) {
        this.membranePotential = membranePotential;
    }

    /**
     * @return the excitatoryReversal
     */
    public double getExcitatoryReversal() {
        return excitatoryReversal;
    }

    /**
     * @param excitatoryReversal the excitatoryReversal to set
     */
    public void setExcitatoryReversal(double excitatoryReversal) {
        this.excitatoryReversal = excitatoryReversal;
    }

    /**
     * @return the leakReversal
     */
    public double getLeakReversal() {
        return leakReversal;
    }

    /**
     * @param leakReversal the leakReversal to set
     */
    public void setLeakReversal(double leakReversal) {
        this.leakReversal = leakReversal;
    }

    /**
     * @return the leakMaxConductance
     */
    public double getLeakMaxConductance() {
        return leakMaxConductance;
    }

    /**
     * @param leakMaxConductance the leakMaxConductance to set
     */
    public void setLeakMaxConductance(double leakMaxConductance) {
        this.leakMaxConductance = leakMaxConductance;
    }

    /**
     * @return the leakConductance
     */
    public double getLeakConductance() {
        return leakConductance;
    }

    /**
     * @param leakConductance the leakConductance to set
     */
    public void setLeakConductance(double leakConductance) {
        this.leakConductance = leakConductance;
    }

    /**
     * @return the potentialTimeConstant
     */
    public double getPotentialTimeConstant() {
        return potentialTimeConstant;
    }

    /**
     * @param potentialTimeConstant the potentialTimeConstant to set
     */
    public void setPotentialTimeConstant(double potentialTimeConstant) {
        this.potentialTimeConstant = potentialTimeConstant;
    }

    /**
     * @return the currentOutputFunction
     */
    public OutputFunction getOutputFunction() {
        return outputFunction;
    }

    /**
     * @param currentOutputFunction the currentOutputFunction to set
     */
    public void setOutputFunction(OutputFunction currentOutputFunction) {
        this.outputFunction = currentOutputFunction;
    }

    /**
     * @return the gain
     */
    public double getGain() {
        return gain;
    }

    /**
     * @param gain the gain to set
     */
    public void setGain(double gain) {
        this.gain = gain;
    }

    /**
     * @return the threshold
     */
    public double getThresholdPotential() {
        return thresholdPotential;
    }

    /**
     * @param threshold the threshold to set
     */
    public void setThresholdPotential(double threshold) {
        this.thresholdPotential = threshold;
    }

    /**
     * @return the refractoryPotential
     */
    public double getRefractoryPotential() {
        return refractoryPotential;
    }

    /**
     * @param refractoryPotential the refractoryPotential to set
     */
    public void setRefractoryPotential(double refractoryPotential) {
        this.refractoryPotential = refractoryPotential;
    }

    /**
     * @param excitatoryInputs the excitatoryInputs to set
     */
    public void setExcitatoryInputs(ArrayList<Synapse> excitatoryInputs) {
        this.excitatoryInputs = excitatoryInputs;
    }

    /**
     * @return the inhibitoryReversal
     */
    public double getInhibitoryReversal() {
        return inhibitoryReversal;
    }

    /**
     * @param inhibitoryReversal the inhibitoryReversal to set
     */
    public void setInhibitoryReversal(double inhibitoryReversal) {
        this.inhibitoryReversal = inhibitoryReversal;
    }

    /**
     * @return the excitatoryCurrent
     */
    public double getExcitatoryCurrent() {
        return excitatoryCurrent;
    }

    /**
     * @param excitatoryCurrent the excitatoryCurrent to set
     */
    public void setExcitatoryCurrent(double excitatoryCurrent) {
        this.excitatoryCurrent = excitatoryCurrent;
    }

    /**
     * @return the duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * @return the inhibitoryConductance
     */
    public double getInhibitoryConductance() {
        return inhibitoryConductance;
    }

    /**
     * @param inhibitoryConductance the inhibitoryConductance to set
     */
    public void setInhibitoryConductance(double inhibitoryConductance) {
        this.inhibitoryConductance = inhibitoryConductance;
    }

    /**
     * @return the inhibitoryMaxConductance
     */
    public double getInhibitoryMaxConductance() {
        return inhibitoryMaxConductance;
    }

    /**
     * @param inhibitoryMaxConductance the inhibitoryMaxConductance to set
     */
    public void setInhibitoryMaxConductance(double inhibitoryMaxConductance) {
        this.inhibitoryMaxConductance = inhibitoryMaxConductance;
    }

    /**
     * @return the bias
     */
    public double getBias() {
        return bias;
    }

    /**
     * @param bias the bias to set
     */
    public void setBias(double bias) {
        this.bias = bias;
    }

    /**
     * {@inheritDoc}
     */
    public void synapseAdded(NetworkEvent<Synapse> networkEvent) {
        Synapse synapse = networkEvent.getObject();
        if (synapse.getTarget().getUpdateRule() == this) {
            addSynapseToList(synapse);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void synapseChanged(NetworkEvent<Synapse> networkEvent) {
        Synapse synapse = networkEvent.getObject();
        if (synapse.getTarget().getUpdateRule() == this) {
            addSynapseToList(synapse);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void synapseRemoved(NetworkEvent<Synapse> networkEvent) {
        Synapse synapse = networkEvent.getObject();
        if (synapse.getTarget().getUpdateRule() == this) {
            if (excitatoryInputs.contains(synapse)) {
                excitatoryInputs.remove(synapse);
            }
            if (inhibitoryInputs.contains(synapse)) {
                inhibitoryInputs.remove(synapse);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void synapseTypeChanged(NetworkEvent<SynapseUpdateRule> networkEvent) {
        // No implementation
    }

    @Override
    public String getDescription() {
        return "Point neuron (Leabra)";
    }


}