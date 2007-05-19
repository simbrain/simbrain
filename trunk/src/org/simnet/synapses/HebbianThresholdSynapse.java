/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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


/**
 * <b>HebbianThresholdSynapse</b>.
 */
public class HebbianThresholdSynapse extends Synapse {

    /** Learning rate. */
    private double learningRate = .1;

    /** Output threshold momentum. */
    private double outputThresholdMomentum = .1;

    /** Output threshold. */
    private double outputThreshold = .5;

    /** Use sliding output threshold. */
    private boolean useSlidingOutputThreshold = false;

    /**
     * Creates a weight of some value connecting two neurons.
     *
     * @param src source neuron
     * @param tar target neuron
     * @param val initial weight value
     * @param theId Id of the synapse
     */
    public HebbianThresholdSynapse(final Neuron src, final Neuron tar, final double val, final String theId) {
        source = src;
        target = tar;
        strength = val;
        id = theId;
    }

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public HebbianThresholdSynapse() {
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param s Synapse to make of the type
     */
    public HebbianThresholdSynapse(final Synapse s) {
        super(s);
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Hebbian threshold";
    }

    /**
     * @return Duplicate synapse.
     */
    public Synapse duplicate() {
        HebbianThresholdSynapse h = new HebbianThresholdSynapse();
        h.setLearningRate(getLearningRate());
        h.setOutputThreshold(this.getOutputThreshold());
        h.setOutputThresholdMomentum(this.getOutputThresholdMomentum());
        h.setUseSlidingOutputThreshold(this.getUseSlidingOutputThreshold());

        return super.duplicate(h);
    }

    /**
     * Creates a weight connecting source and target neurons.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public HebbianThresholdSynapse(final Neuron source, final Neuron target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Update the synapse.
     */
    public void update() {
        double input = getSource().getActivation();
        double output = getTarget().getActivation();

        if (useSlidingOutputThreshold) {
            outputThreshold += (outputThresholdMomentum * ((output * output) - outputThreshold));
        }

        strength += (learningRate * input * output * (output - outputThreshold));

        strength = clip(strength);
    }

    /**
     * @return Returns the momentum.
     */
    public double getLearningRate() {
        return learningRate;
    }

    /**
     * @param momentum The momentum to set.
     */
    public void setLearningRate(final double momentum) {
        this.learningRate = momentum;
    }

    /**
     * @return Returns the outputThreshold.
     */
    public double getOutputThreshold() {
        return outputThreshold;
    }

    /**
     * @param outputThreshold The outputThreshold to set.
     */
    public void setOutputThreshold(final double outputThreshold) {
        this.outputThreshold = outputThreshold;
    }

    /**
     * @return Returns the useSlidingOutputThreshold.
     */
    public boolean getUseSlidingOutputThreshold() {
        return useSlidingOutputThreshold;
    }

    /**
     * @param useSlidingOutputThreshold The useSlidingOutputThreshold to set.
     */
    public void setUseSlidingOutputThreshold(final boolean useSlidingOutputThreshold) {
        this.useSlidingOutputThreshold = useSlidingOutputThreshold;
    }

    /**
     * @return Returns the outputThresholdMomentum.
     */
    public double getOutputThresholdMomentum() {
        return outputThresholdMomentum;
    }

    /**
     * @param outputThresholdMomentum The outputThresholdMomentum to set.
     */
    public void setOutputThresholdMomentum(final double outputThresholdMomentum) {
        this.outputThresholdMomentum = outputThresholdMomentum;
    }
}
