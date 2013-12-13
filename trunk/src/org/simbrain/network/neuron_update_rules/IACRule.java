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
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ClippableUpdateRule;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>IACNeuron</b> implements an Interactive Activation and Competition neuron.
 */
public class IACRule extends NeuronUpdateRule implements BoundedUpdateRule,
        ClippableUpdateRule {

    /** The Default upper bound. */
    private static final double DEFAULT_CEILING = 1.0;

    /** The Default lower bound. */
    private static final double DEFAULT_FLOOR = -1.0;

    /** Neuron decay. */
    private double decay = 0.1;

    /** Rest. */
    private double rest = 0;

    /** Noise dialog box. */
    private Randomizer noiseGenerator = new Randomizer();

    /** Add noise to the neuron. */
    private boolean addNoise = false;

    /** Clipping. */
    private boolean clipping = true;

    /** The upper bound of the activity if clipping is used. */
    private double ceiling = DEFAULT_CEILING;

    /** The lower bound of the activity if clipping is used. */
    private double floor = DEFAULT_FLOOR;

    /**
     * @{inheritDoc
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    /**
     * @{inheritDoc
     */
    public IACRule deepCopy() {
        IACRule iac = new IACRule();
        iac.setDecay(getDecay());
        iac.setRest(getRest());
        iac.setClipped(isClipped());
        iac.setUpperBound(getCeiling());
        iac.setLowerBound(getFloor());
        iac.setAddNoise(getAddNoise());
        iac.noiseGenerator = new Randomizer(noiseGenerator);
        return iac;
    }

    /**
     * @{inheritDoc
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
            val += ((wtdSum * (getCeiling() - val)) - (decay * (val - rest)));
        } else {
            val += ((wtdSum * (val - getFloor())) - (decay * (val - rest)));
        }

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        if (clipping) {
            val = clip(val);
        }

        neuron.setBuffer(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double clip(double val) {
        if (val > getCeiling()) {
            return getCeiling();
        } else if (val < getFloor()) {
            return getFloor();
        } else {
            return val;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRandomValue() {
        return (getCeiling() - getFloor()) * Math.random() - getFloor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextualIncrement(Neuron n) {
        double act = n.getActivation();
        if (act >= getCeiling() && isClipped()) {
            return;
        } else {
            if (isClipped()) {
                act = clip(act + increment);
            } else {
                act = act + increment;
            }
            n.setActivation(act);
            n.getNetwork().fireNeuronChanged(n);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextualDecrement(Neuron n) {
        double act = n.getActivation();
        if (act <= getFloor() && isClipped()) {
            return;
        } else {
            if (isClipped()) {
                act = clip(act - increment);
            } else {
                act = act - increment;
            }
            n.setActivation(act);
            n.getNetwork().fireNeuronChanged(n);
        }
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
        return "IAC";
    }

    @Override
    public double getCeiling() {
        return ceiling;
    }

    @Override
    public double getFloor() {
        return floor;
    }

    @Override
    public void setUpperBound(double ceiling) {
        this.ceiling = ceiling;
    }

    @Override
    public void setLowerBound(double floor) {
        this.floor = floor;
    }

    @Override
    public boolean isClipped() {
        return clipping;
    }

    @Override
    public void setClipped(boolean clipping) {
        this.clipping = clipping;
    }

}
