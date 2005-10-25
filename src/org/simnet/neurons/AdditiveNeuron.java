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
import org.simnet.util.SMath;


/**
 * <b>AdditiveNeuron</b>  See haykin (2002), section 14.5.  Used with continnuous Hopfield networks.
 */
public class AdditiveNeuron extends Neuron {
    private double lambda = 1.4;
    private double resistance = 1;
    private boolean clipping = false;
    private RandomSource noiseGenerator = new RandomSource();
    private boolean addNoise = false;

    public int getTimeType() {
        return org.simnet.interfaces.Network.CONTINUOUS;
    }

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters
     */
    public AdditiveNeuron() {
        this.setUpperBound(1);
        this.setLowerBound(-1);
        this.setIncrement(.1);
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied
     */
    public AdditiveNeuron(Neuron n) {
        super(n);
    }

    /**
     * Returns a duplicate AdditiveNeuron (used, e.g., in copy/paste)
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
     * Update buffer of additive neuron using Euler's method
     */
    public void update() {
        double wtdSum = 0;

        if (getFanIn().size() > 0) {
            for (int j = 0; j < getFanIn().size(); j++) {
                Synapse w = (Synapse) getFanIn().get(j);
                Neuron source = w.getSource();
                wtdSum += (w.getStrength() * SMath.arctan(source.getActivation(), lambda));
            }
        }

        double val = getActivation() + ((super.getParentNetwork().getTimeStep() * -getActivation()) / resistance)
                     + wtdSum + getInputValue();

        if (addNoise == true) {
            val += noiseGenerator.getRandom();
        }

        if (clipping == true) {
            val = clip(val);
        }

        setBuffer(val);
    }

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
    public void setLambda(double lambda) {
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
    public void setResistance(double resistance) {
        this.resistance = resistance;
    }

    public RandomSource getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noise The noise to set.
     */
    public void setNoiseGenerator(RandomSource noise) {
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
    public void setAddNoise(boolean addNoise) {
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
    public void setClipping(boolean clipping) {
        this.clipping = clipping;
    }
}
