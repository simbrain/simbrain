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
 * <b>Hebbian</b>.
 */
public class Hebbian extends Synapse {

    /** Learning rate. */
    private double learningRate = 1;

    /**
     * Creates a weight of some value connecting two neurons.
     *
     * @param src source neuron
     * @param tar target neuron
     * @param val initial weight value
     * @param theId Id of the synapse
     */
    public Hebbian(final Neuron src, final Neuron tar, final double val, final String theId) {
        source = src;
        target = tar;
        strength = val;
        id = theId;
    }

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public Hebbian() {
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param s Synapse to make of the type
     */
    public Hebbian(final Synapse s) {
        super(s);
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Hebbian";
    }

    /**
     * @return duplicate Hebbian (used, e.g., in copy/paste).
     */
    public Synapse duplicate() {
        Hebbian h = new Hebbian();
        h.setLearningRate(getLearningRate());

        return super.duplicate(h);
    }

    /**
     * Creates a weight connecting source and target neurons.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public Hebbian(final Neuron source, final Neuron target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Updates the synapse.
     */
    public void update() {
        double input = getSource().getActivation();
        double output = getTarget().getActivation();

        strength += (learningRate * input * output);

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
}
