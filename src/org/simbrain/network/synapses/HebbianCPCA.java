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
package org.simbrain.network.synapses;

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;


/**
 * <b>HebbianCPCA</b>.
 */
public class HebbianCPCA extends Synapse {

    /** Default Learning rate. */
    public static final double DEFAULT_LEARNING_RATE = .005;
    /**
     * Default Maximum weight value (see equation 4.19 in O'Reilly and
     * Munakata).
     */
    public static final double DEFAULT_M = .5 / .15;
    /** default Weight offset. */
    public static final double DEFAULT_THETA = 1;
    /** default Sigmoidal function. */
    public static final double DEFAULT_LAMBDA = 1;

    /** Learning rate. */
    private double learningRate = DEFAULT_LEARNING_RATE;
    /** Maximum weight value (see equation 4.19 in O'Reilly and Munakata). */
    private double m = DEFAULT_M;
    /** Weight offset. */
    private double theta = DEFAULT_THETA;
    /** Sigmoidal function. */
    private double lambda = DEFAULT_LAMBDA;

    /**
     * Creates a weight of some value connecting two neurons.
     *
     * @param src source neuron
     * @param tar target neuron
     * @param val initial weight value
     * @param theId Id of the synapse
     */
    public HebbianCPCA(final Neuron src, final Neuron tar, final double val, final String theId) {
//    	  setSource(src);
//        setTarget(tar);
        super(src, tar);
        this.setStrength(val);
        id = theId;
    }

    /**
     * This constructor is used when creating a neuron of one type from another
     * neuron of another type Only values common to different types of neuron
     * are copied.
     * 
     * @param s Synapse to make of the type
     */
    public HebbianCPCA(final Synapse s) {
        super(s);
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "HebbianCPCA";
    }

    /**
     * @return duplicate Hebbian (used, e.g., in copy/paste).
     */
    public Synapse duplicate() {
        HebbianCPCA h = new HebbianCPCA(this.getSource(), this.getTarget());
        h.setLearningRate(getLearningRate());
        h.setM(getM());
        h.setTheta(getTheta());
        h.setLambda(getLambda());

        return super.duplicate(h);
    }

    /**
     * Creates a weight connecting source and target neurons.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public HebbianCPCA(final Neuron source, final Neuron target) {
        super(source, target);
    }

    /**
     * Updates the synapse (see equation 4.18 in O'Reilly and Munakata).
     */
    public void update() {
        double input = getSource().getActivation();
        double output = getTarget().getActivation();

        double deltaW = learningRate * ((output * input) - (output * getStrength())); // Equation 4.12
        //deltaW = learningRate * (output * input * (m - strength) + output * (1 - input) * (-strength));
        //strength = sigmoidal(strength);
        //strength = clip(strength + deltaW);
        setStrength(getStrength() + deltaW);
    }

    /**
     * Sigmoidal Function (see equation 4.23 in O'Reilly and Munakata).
     *
     * @param arg value to send to sigmoidal
     * @return value of sigmoidal
     */
    private double sigmoidal(final double arg) {
        return 1 / (1 + Math.pow(theta * (arg / (1 - arg)), -lambda));

    }

    /**
     * @return Returns the momentum.
     */
    public double getLearningRate() {
        return learningRate;
    }

    /**
     * @return Returns the maximum weight.
     */
    public double getM() {
        return m;
    }

    /**
     * @return Returns the weight offset.
     */
    public double getTheta() {
        return theta;
    }

    /**
     * @return Returns sigmoidal function.
     */
    public double getLambda() {
        return lambda;
    }


    /**
     * @param momentum The momentum to set.
     */
    public void setLearningRate(final double momentum) {
        this.learningRate = momentum;
    }

    /**
     * @param m is maximum weight The maximum weight to set
     */
    public void setM(final double m) {
        this.m = m;
    }

    /**
     * @param theta is weight offset The weight offset to set.
     */
    public void setTheta(final double theta) {
        this.theta = theta;
        }

    /**
     * @param lambda is The sigmoidal to set.
     */
    public void setLambda(final double lambda) {
        this.lambda = lambda;
    }

}
