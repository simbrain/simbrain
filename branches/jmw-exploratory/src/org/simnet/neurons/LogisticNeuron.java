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
package org.simnet.neurons;

import org.simnet.interfaces.Neuron;


/**
 * <b>LogisticNeuron</b>.
 */
public class LogisticNeuron extends Neuron {

    /** Growth rate. */
    private double growthRate = 3.9;

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public LogisticNeuron() {
    }

    /**
     * @return Time type.
     */
    public int getTimeType() {
        return org.simnet.interfaces.RootNetwork.DISCRETE;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to be created
     */
    public LogisticNeuron(final Neuron n) {
        super(n);
    }

    /**
     * @return duplicate LogisticNeuron (used, e.g., in copy/paste).
     */
    public LogisticNeuron duplicate() {
        LogisticNeuron ln = new LogisticNeuron();
        ln = (LogisticNeuron) super.duplicate(ln);
        ln.setGrowthRate(getGrowthRate());

        return ln;
    }

    /**
     * TODO: Note that the inputs have to be within the neuron's bounds for behavior to be reasonable.
     */
    public void update() {
        double x = getActivation();

        double y = (x - lowerBound) / (upperBound - lowerBound);
        y = growthRate * y * (1 - y);
        x = ((upperBound - lowerBound) * y) + lowerBound;

        setBuffer(clip(x));
    }

    /**
     * @return Returns the firingProbability.
     */
    public double getGrowthRate() {
        return growthRate;
    }

    /**
     * @param growthRate The growthRate to set.
     */
    public void setGrowthRate(final double growthRate) {
        this.growthRate = growthRate;
    }

    /**
     * @return Name of neuron type.
     */
    public static String getName() {
        return "Logistic";
    }
}
