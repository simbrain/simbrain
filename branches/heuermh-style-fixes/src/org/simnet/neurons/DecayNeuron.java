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

import org.simnet.util.RandomSource;


/**
 * <b>DecayNeuron</b>
 */
public class DecayNeuron extends Neuron {
    private static final int RELATIVE = 0;
    private static final int ABSOLUTE = 1;
    private int relAbs = RELATIVE;
    private double decayAmount = .1;
    private double decayFraction = .1;
    private double baseLine = 0;
    private boolean clipping = true;
    private RandomSource noiseGenerator = new RandomSource();
    private boolean addNoise = false;

    public DecayNeuron() {
    }

    public int getTimeType() {
        return org.simnet.interfaces.Network.DISCRETE;
    }

    public DecayNeuron(Neuron n) {
        super(n);
    }

    public Neuron duplicate() {
        DecayNeuron dn = new DecayNeuron();
        dn = (DecayNeuron) super.duplicate(dn);
        dn.setRelAbs(getRelAbs());
        dn.setDecayAmount(getDecayAmount());
        dn.setDecayFraction(getDecayFraction());
        dn.setClipping(getClipping());
        dn.setAddNoise(getAddNoise());
        dn.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);

        return dn;
    }

    public void update() {
        double val = activation + this.weightedInputs();
        double decayVal = 0;

        if (relAbs == RELATIVE) {
            decayVal = decayFraction * Math.abs(val - baseLine);
        } else if (relAbs == ABSOLUTE) {
            decayVal = decayAmount;
        }

        // Here's where the action happens
        if (val < baseLine) {
            val += decayVal;

            if (val > baseLine) {
                val = baseLine;
            }
        } else if (val > baseLine) {
            val -= decayVal;

            if (val < baseLine) {
                val = baseLine;
            }
        }

        if (addNoise == true) {
            val += noiseGenerator.getRandom();
        }

        if (clipping == true) {
            val = clip(val);
        }

        setBuffer(val);
    }

    public static String getName() {
        return "Decay";
    }

    /**
     * @return Returns the decayAmount.
     */
    public double getDecayAmount() {
        return decayAmount;
    }

    /**
     * @param decayAmount The decayAmount to set.
     */
    public void setDecayAmount(double decayAmount) {
        this.decayAmount = decayAmount;
    }

    /**
     * @return Returns the dedayPercentage.
     */
    public double getDecayFraction() {
        return decayFraction;
    }

    /**
     * @param dedayPercentage The dedayPercentage to set.
     */
    public void setDecayFraction(double decayFraction) {
        this.decayFraction = decayFraction;
    }

    /**
     * @return Returns the relAbs.
     */
    public int getRelAbs() {
        return relAbs;
    }

    /**
     * @param relAbs The relAbs to set.
     */
    public void setRelAbs(int relAbs) {
        this.relAbs = relAbs;
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

    /**
     * @return Returns the noiseGenerator.
     */
    public RandomSource getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noiseGenerator The noiseGenerator to set.
     */
    public void setNoiseGenerator(RandomSource noiseGenerator) {
        this.noiseGenerator = noiseGenerator;
    }

    /**
     * @return Returns the baseLine.
     */
    public double getBaseLine() {
        return baseLine;
    }

    /**
     * @param baseLine The baseLine to set.
     */
    public void setBaseLine(double baseLine) {
        this.baseLine = baseLine;
    }
}
