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
package org.simnet.neurons;

import org.simnet.interfaces.Neuron;


/**
 * <b>TemporalDifferenceNeuron</b>.
 */
public class TemporalDifferenceNeuron extends Neuron {

    /** Alpha variable. */
    private double alpha = 0;

    /** Beta variable. */
    private double beta = 0;

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public TemporalDifferenceNeuron() {
    }

    /**
     * TODO: Not really true...
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
    public TemporalDifferenceNeuron(final Neuron n) {
        super(n);
    }

    /**
     * Returns a duplicate TemporalDifference (used, e.g., in copy/paste).
     * @return Duplicated neuron
     */
    public Neuron duplicate() {
        TemporalDifferenceNeuron cn = new TemporalDifferenceNeuron();
        cn = (TemporalDifferenceNeuron) super.duplicate(cn);
        cn.setAlpha(getAlpha());
        cn.setBeta(getBeta());

        return cn;
    }

    /**
     * Update neuron.
     */
    public void update() {
    }

    /**
     * @return Name of neuron type.
     */
    public static String getName() {
        return "Temporal Difference";
    }

    /**
     * @return alpha.
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * @param alpha Parameter to be set.
     */
    public void setAlpha(final double alpha) {
        this.alpha = alpha;
    }

    /**
     * @return beta.
     */
    public double getBeta() {
        return beta;
    }

    /**
     * @param beta Parameter to be set.
     */
    public void setBeta(final double beta) {
        this.beta = beta;
    }
}
