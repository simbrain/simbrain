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
package org.simnet.synapses;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.neurons.TraceNeuron;


/**
 * <b>TraceSynapse</b>.
 */
public class TraceSynapse extends Synapse {

    /** Learning rate. */
    public static final double DEFAULT_LEARNING_RATE = .5;
    
    /** Learning rate. */
    private double learningRate = DEFAULT_LEARNING_RATE;

    /**
     * Creates a weight of some value connecting two neurons.
     *
     * @param src source neuron
     * @param tar target neuron
     * @param val initial weight value
     * @param theId Id of synapse
     */
    public TraceSynapse(final Neuron src, final Neuron tar, final double val, final String theId) {
    	super(src, tar);
//        setSource(src);
//        setTarget(tar);
        strength = val;
        id = theId;
    }

    /**
     * Creates a weight connecting source and target neurons.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public TraceSynapse(final Neuron source, final Neuron target) {
        super(source, target);
//    	  setSource(source);
//        setTarget(target);
    }

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public TraceSynapse() {
        super();
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param s Synapse to make of the type
     */
    public TraceSynapse(final Synapse s) {
        super(s);
    }

    /**
     * @return duplicate TraceSynapse (used, e.g., in copy/paste).
     */
    public Synapse duplicate() {
        TraceSynapse ts = new TraceSynapse(this.getSource(), this.getTarget());
        ts = (TraceSynapse) super.duplicate(ts);
        ts.setLearningRate(getLearningRate());

        return ts;
    }

    /**
     * Update the synapse.
     */
    public void update() {
        // Assumes source and target neurons are trace neurons; otherwise does nothing.

        // Use "history" of trace and difference because the nodes are updated before the weights, so this must be done
        // to get the values prior to update.
        if ((getSource() instanceof TraceNeuron) && (getTarget() instanceof TraceNeuron)) {
        	double delta = learningRate
                    * (((TraceNeuron) getSource()).getTraceHistory() * ((TraceNeuron) getTarget())
                            .getDifferenceHistory());
            this.setStrength(strength + delta);
            checkBounds();
        }
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Trace";
    }

    /**
     * @return the momentum.
     */
    public double getLearningRate() {
        return learningRate;
    }

    /**
     * Sets the momentum value.
     *
     * @param momentum the momentum to set
     */
    public void setLearningRate(final double momentum) {
        this.learningRate = momentum;
    }

}
