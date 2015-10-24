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
package org.simbrain.network.neuron_update_rules;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.randomizer.Randomizer;
import java.lang.Math;


public class FitzhughNagumo extends SpikingNeuronUpdateRule implements
    NoisyUpdateRule {


    /** W. - recovery variable */
    private double w;

    /** V. - membrane potential */
    private double v;

    /** Constant background current. KEEP */
    private double iBg = 1;

    /** Threshold value to signal a spike. KEEP */
    private double threshold = 1.9;

    /** Noise dialog. */
    private Randomizer noiseGenerator = new Randomizer();

    /** Add noise to the neuron. */
    private boolean addNoise;
    
    /** Recovery rate */
    private double a = 0.08;
    
    /** Recovery dependence on voltage. */
    private double b = 1;
    
    /** Recovery self-dependence. */
    private double c = 0.8;


    /**
     * {@inheritDoc}
     */
    public FitzhughNagumo deepCopy() {
        FitzhughNagumo in = new FitzhughNagumo();
        in.setW(getW());
        in.setV(getV());
        in.setAddNoise(getAddNoise());
        in.noiseGenerator = new Randomizer(noiseGenerator);

        return in;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Neuron neuron) {
        double timeStep = neuron.getNetwork().getTimeStep();
//        final boolean refractory = getLastSpikeTime() + refractoryPeriod
//                >= neuron.getNetwork().getTime();
        final double activation = neuron.getActivation();
        double inputs = 0;
        inputs = inputType.getInput(neuron);
        if (addNoise) {
            inputs += noiseGenerator.getRandom();
        }
        inputs += iBg;
        w += (timeStep * (a*(b*v+0.7-(c*w))));

        v = activation + (timeStep * (activation - (Math.pow(activation, 3)/3) - w + inputs) );
        // You want this
        if (v >= threshold) {
            neuron.setSpkBuffer(true);
            setHasSpiked(true, neuron);
        } else {
            neuron.setSpkBuffer(false);
            setHasSpiked(false, neuron);
        }
        //till here
        neuron.setBuffer(v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRandomValue() {
        // Equal chance of spiking or not spiking, taking on any value between
        // the resting potential and the threshold if not.
        return 2 * (threshold - c) * Math.random() + c;
    }

    /**
     * @return Returns the w.
     */
    public double getW() {
        return w;
    }

    /**
     * @param w The w to set.
     */
    public void setW(final double w) {
        this.w = w;
    }

    /**
     * @return Returns the v.
     */
    public double getV() {
        return v;
    }

    /**
     * @param v The v to set.
     */
    public void setV(final double v) {
        this.v = v;
    }

    public double getiBg() {
		return iBg;
	}

	public void setiBg(double iBg) {
		this.iBg = iBg;
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
     * @return Returns the noiseGenerator.
     */
    public Randomizer getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noiseGenerator The noiseGenerator to set.
     */
    public void setNoiseGenerator(final Randomizer noiseGenerator) {
        this.noiseGenerator = noiseGenerator;
    }

    @Override
    public String getDescription() {
        return "FitzhughNagumo";
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public double getA() {
        return a;
    }
    
    public double getB() {
        return b;
    }
    
    public double getC(){
        return c;
    }
    
    public void setA(double a) {
        this.a = a;
    }
    
    public void setB(double b) {
        this.b = b;
    }
    
    public void setC(double c) {
        this.c = c;
    }
}