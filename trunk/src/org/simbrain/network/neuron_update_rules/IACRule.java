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

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.util.RandomSource;

/**
 * <b>IACNeuron</b> implements an Interactive Activation and Competition neuron.
 */
public class IACRule extends NeuronUpdateRule {

    /** Neuron decay. */
    private double decay = 0;

    /** Rest. */
    private double rest = 0;

    /** Noise dialog box. */
    private RandomSource noiseGenerator = new RandomSource();

    /** Add noise to the neuron. */
    private boolean addNoise = false;

    /** Clipping. */
    private boolean clipping = true;

    /**
     * @{inheritDoc}
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    /**
     * @{inheritDoc}
     */
    public IACRule deepCopy() {
        IACRule iac = new IACRule();
        iac.setDecay(getDecay());
        iac.setRest(getRest());
        iac.setClipping(getClipping());
        iac.setAddNoise(getAddNoise());
        iac.noiseGenerator = new RandomSource(noiseGenerator);
        return iac;
    }

    /**
     * @{inheritDoc}
     */
    public void init(Neuron neuron) {
        // No implementation
    }

    /**
     * @{inheritDoc}
     */
    public void update(Neuron neuron) {
        double val = neuron.getActivation();
        double wtdSum = 0;

        for (Synapse w : neuron.getFanIn()) {
            Neuron source = w.getSource();

            if (source.getActivation() > 0) {
                wtdSum += (w.getStrength() * source.getActivation());
            }
        }

        if (wtdSum > 0) {
            val += ((wtdSum * (neuron.getUpperBound() - val)) - (decay * (val - rest)));
        } else {
            val += ((wtdSum * (val - neuron.getLowerBound())) - (decay * (val - rest)));
        }

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        if (clipping) {
            val = neuron.clip(val);
        }

        neuron.setBuffer(val);
    }

    /**
     * @return Returns the decay.
     */
    public double getDecay() {
        return decay;
    }

    /**
     * @param decay The decay to set.
     */
    public void setDecay(final double decay) {
        this.decay = decay;
    }

    /**
     * @return Returns the rest.
     */
    public double getRest() {
        return rest;
    }

    /**
     * @param rest The rest to set.
     */
    public void setRest(final double rest) {
        this.rest = rest;
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

    /**
     * @return Returns the noiseGenerator.
     */
    public RandomSource getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noiseGenerator The noiseGenerator to set.
     */
    public void setNoiseGenerator(final RandomSource noiseGenerator) {
        this.noiseGenerator = noiseGenerator;
    }

    @Override
    public String getDescription() {
        return "IAC";
    }
}
