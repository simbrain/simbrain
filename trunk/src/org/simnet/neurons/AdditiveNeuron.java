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

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.util.RandomSource;


/**
 * <b>AdditiveNeuron</b>  See haykin (2002), section 14.5.  Used with continnuous Hopfield networks.
 */
public class AdditiveNeuron extends Neuron {
    /** Lambda. */
    private double lambda = 1.4;
    /** Resistance. */
    private double resistance = 1;
    /** Clipping. */
    private boolean clipping = false;
    /** Noise dialog. */
    private RandomSource noiseGenerator = new RandomSource();
    /** For adding noise to the neuron. */
    private boolean addNoise = false;

    /**
     * @return the time type.
     */
    public int getTimeType() {
        return org.simnet.interfaces.RootNetwork.CONTINUOUS;
    }

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public AdditiveNeuron() {
        this.setUpperBound(1);
        this.setLowerBound(-1);
        this.setIncrement(.1);
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to make of the type
     */
    public AdditiveNeuron(final Neuron n) {
        super(n);
    }

    /**
     * @return duplicate AdditiveNeuron (used, e.g., in copy/paste).
     */
    public Neuron duplicate() {
        AdditiveNeuron an = new AdditiveNeuron();
        an = (AdditiveNeuron) super.duplicate(an);
        an.setLambda(getLambda());
        an.setResistance(getResistance());
        an.setClipping(getClipping());
        an.setAddNoise(getAddNoise());
        an.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);

        return an;
    }

    /**
     * Update buffer of additive neuron using Euler's method.
     */
    public void update() {

        // external input, if any
        double externalInput = 0;
        if (this.isInput()) {
            externalInput = g(this.getInputValue());
        }
        double wtdSum = externalInput;

        if (getFanIn().size() > 0) {
            for (int j = 0; j < getFanIn().size(); j++) {
                Synapse w = (Synapse) getFanIn().get(j);
                Neuron source = w.getSource();
                wtdSum += (w.getStrength() *  g(source.getActivation()));
            }
        }

        double val =  getActivation()
                        + super.getParentNetwork().getTimeStep() * (-getActivation() / resistance + wtdSum);

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        if (clipping) {
            val = clip(val);
        }

        setBuffer(val);
        this.setInputValue(0);
    }

    /**
     * Implements a Hopfield type sigmoidal function.
     *
     * @param x input to function
     * @return  output of function
     */
    private double g(final double x) {
        return 2 / Math.PI * Math.atan((Math.PI * lambda * x) / 2);
    }

    /**
     * @return Name of neuron type.
     */
    public static String getName() {
        return "Additive";
    }

    /**
     * @return Returns the lambda.
     */
    public double getLambda() {
        return lambda;
    }

    /**
     * @param lambda The lambda to set.
     */
    public void setLambda(final double lambda) {
        this.lambda = lambda;
    }

    /**
     * @return Returns the resistance.
     */
    public double getResistance() {
        return resistance;
    }

    /**
     * @param resistance The resistance to set.
     */
    public void setResistance(final double resistance) {
        this.resistance = resistance;
    }

    /**
     * @return Noise generator dialog.
     */
    public RandomSource getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noise The noise to set.
     */
    public void setNoiseGenerator(final RandomSource noise) {
        this.noiseGenerator = noise;
    }

    /**
     * @return Returns the addNoise.
     */
    public boolean getAddNoise() {
        return addNoise;
    }

    /**
     * @param addNoise The addNoise to set.
     */
    public void setAddNoise(final boolean addNoise) {
        this.addNoise = addNoise;
    }

    /**
     * @return Returns the clipping.
     */
    public boolean getClipping() {
        return clipping;
    }

    /**
     * @param clipping The clipping to set.
     */
    public void setClipping(final boolean clipping) {
        this.clipping = clipping;
    }
}
