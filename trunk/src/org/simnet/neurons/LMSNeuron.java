/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.neurons;

import java.util.Iterator;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.synapses.SignalSynapse;


/**
 * <b>LMS Neuron</b>. A way of implementing the delta rule (AKA Widrow Hoff) rule in a single neuron.  It is
 * assumed that one of the synapses attaching to this neuron is a signal synapse, which carries the target
 * value used in learning.
 */
public class LMSNeuron extends Neuron {

    /** Learning rate. */
    private double learningRate = .01;

    /** Signal synapse. */
    private SignalSynapse targetValueSynapse = null;

    /** Target value for learning. */
    private double targetVal;

    /** Current error. */
    private double error;

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public LMSNeuron() {
    }

    /**
     * @return time type.
     */
    public int getTimeType() {
        return org.simnet.interfaces.RootNetwork.DISCRETE;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to make the type
     */
    public LMSNeuron(final Neuron n) {
        super(n);
    }

    /**
     * Returns a duplicate ClampedNeuron (used, e.g., in copy/paste).
     * @return Duplicated neuron
     */
    public Neuron duplicate() {
        LMSNeuron cn = new LMSNeuron();
        cn = (LMSNeuron) super.duplicate(cn);

        return cn;
    }

    /**
     * Update neuron.
     */
    public void update() {
        activation = getWeightedInputs();

        // Find signal neuron
        if (targetValueSynapse == null) {
            targetValueSynapse = findSignalSynapse();
        }

        adjustIncomingWeights();

        setBuffer(clip(activation));
    }

    /**
     * Iterate through incoming weights (besides the signal weight) and update according
     * to LMS rule.
     */
    private void adjustIncomingWeights() {
        if (targetValueSynapse != null && (!this.getParentNetwork().getRootNetwork().getClampWeights())) {
            targetVal = targetValueSynapse.getSource().getActivation();
            error =   targetVal - this.getWeightedInputs();
            for (Iterator incomingSynapses = this.fanIn.iterator(); incomingSynapses.hasNext(); ) {
                Synapse synapse = (Synapse) incomingSynapses.next();
                if (synapse != targetValueSynapse) {
                    synapse.setStrength(synapse.getStrength()
                            + (learningRate * error * synapse.getSource().getActivation()));
                }
            }
        }

    }

    /**
     * Returns the first signal synapse discovered, null if there are none.
     *
     * @return the first signal synapse discovered, null if there are none.
     */
    private SignalSynapse findSignalSynapse() {
        SignalSynapse ret = null;
        for (Iterator incomingSynapses = this.fanIn.iterator(); incomingSynapses.hasNext(); ) {
            Synapse synapse = (Synapse) incomingSynapses.next();
            if (synapse instanceof SignalSynapse) {
                return (SignalSynapse) synapse;
            }
        }
        return ret;
    }

    /**
     * Overrides superclass implementation by ignoring inputs from targetValue synapses.
     *
     *
     * @return weighted input to this node
     */
    public double getWeightedInputs() {
        double wtdSum = this.getInputValue();
        if (fanIn.size() > 0) {
            for (int j = 0; j < fanIn.size(); j++) {
                Synapse w = (Synapse) fanIn.get(j);
                if (w != targetValueSynapse) {
                    wtdSum += w.getValue();
                }
            }
        }

        return wtdSum;
    }

    /**
     * @return Name of neuron type.
     */
    public static String getName() {
        return "LMS";
    }

    /**
     * @return Learning rate.
     */
    public double getLearningRate() {
        return learningRate;
    }

    /**
     * Sets the learning rate.
     * @param learningRate Learning rate to set
     */
    public void setLearningRate(final double learningRate) {
        this.learningRate = learningRate;
    }
}
