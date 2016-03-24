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
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>IACNeuron</b> implements an Interactive Activation and Competition neuron.
 */
public class IACRule extends NeuronUpdateRule implements BoundedUpdateRule,
        ClippableUpdateRule, NoisyUpdateRule {

    /** The Default upper bound. */
    private static final double DEFAULT_CEILING = 1.0;

    /** The Default lower bound. */
    private static final double DEFAULT_FLOOR = -.2;

    /** Neuron decay. */
    private double decay = 0.05;

    /** Rest. */
    private double rest = .1;

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

    /** Local variables as class variables for a minor performance gain. */
    private double effect, netInput, act;

    /**
     * @return
     * {@inheritDoc}
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    /**
     * @return
     * {@inheritDoc}
     */
    public IACRule deepCopy() {
        IACRule iac = new IACRule();
        iac.setDecay(getDecay());
        iac.setRest(getRest());
        iac.setClipped(isClipped());
        iac.setUpperBound(getUpperBound());
        iac.setLowerBound(getLowerBound());
        iac.setAddNoise(getAddNoise());
        iac.noiseGenerator = new Randomizer(noiseGenerator);
        return iac;
    }

    @Override
    public void update(Neuron neuron) {

        // Notation and algorithm from McClelland 1981, Proceedings of the third
        // annual cog-sci meeting

        // Sum of the "active excitors" and "active inhibitors"
        netInput = neuron.getInputValue();
        for (Synapse w : neuron.getFanIn()) {
            if (w.getSource().getActivation() > 0) {
                netInput += (w.getStrength() * w.getSource().getActivation());
            }
        }

        // Determine "effect" value.
        effect = 0;
        if (netInput >= 0) {
            effect = (getUpperBound() - neuron.getActivation()) * netInput;
        } else {
            effect = (neuron.getActivation() - getLowerBound()) * netInput;
        }

        // Update activation using Euler integration of main ODE
        act = neuron.getActivation() + neuron.getNetwork().getTimeStep()
                * (effect - decay * (neuron.getActivation() - rest));

        if (addNoise) {
            act += noiseGenerator.getRandom();
        }

        if (clipping) {
            act = clip(act);
        }

        neuron.setBuffer(act);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double clip(double val) {
        if (val > getUpperBound()) {
            return getUpperBound();
        } else if (val < getLowerBound()) {
            return getLowerBound();
        } else {
            return val;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextualIncrement(Neuron n) {
        double act = n.getActivation();
        if (act >= getUpperBound() && isClipped()) {
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
        if (act <= getLowerBound() && isClipped()) {
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
    public double getUpperBound() {
        return ceiling;
    }

    @Override
    public double getLowerBound() {
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
