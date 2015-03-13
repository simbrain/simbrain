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
package org.simbrain.network.neuron_update_rules.activity_generators;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ActivityGenerator;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ClippableUpdateRule;

/**
 * <b>LogisticNeuron</b> updates using the logistic equation, which is chaotic
 * for the default growth rate. Does not use inputs from other neurons.
 *
 * TODO: This should be an input generator
 */
public class LogisticRule extends NeuronUpdateRule implements
        BoundedUpdateRule, ClippableUpdateRule, ActivityGenerator {

    public static final double DEFAULT_FLOOR = 0.0;

    /** Growth rate. */
    private double growthRate = 3.9;

    private double ceiling = 1.0;

    private double floor = -1.0;

    public LogisticRule(LogisticRule lr, Neuron n) {
        super();
        this.ceiling = lr.getUpperBound();
        this.floor = lr.getLowerBound();
        this.growthRate = lr.getGrowthRate();
        init(n);
    }

    public LogisticRule() {
        super();
    }

    /**
     * @{inheritDoc
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    /**
     * @{inheritDoc
     */
    @Override
    public void init(Neuron neuron) {
        neuron.setGenerator(true);
    }

    /**
     * @{inheritDoc <b>Unsafe for activity generators</b>. If copied across a
     *              set of neurons, {@link #init(Neuron) init} must be called to
     *              ensure rational behavior for an activity generator. The
     *              {@link #RandomNeuronRule(RandomNeuronRule, Neuron) copy
     *              constructor} is the preferred method of copying because
     *              {@link #init(Neuron) init} is called on the neuron parameter
     *              automatically.
     */
    public LogisticRule deepCopy() {
        LogisticRule ln = new LogisticRule();
        ln.setGrowthRate(getGrowthRate());
        ln.setUpperBound(ceiling);
        ln.setLowerBound(floor);
        return ln;
    }

    /**
     * @{inheritDoc
     */
    public void update(Neuron neuron) {

        // TODO: Note that the inputs have to be within the neuron's bounds for
        // behavior to be reasonable.

        double x = neuron.getActivation();

        double y = (x - getLowerBound()) / (getUpperBound() - getLowerBound());
        y = growthRate * y * (1 - y);
        x = ((getUpperBound() - getLowerBound()) * y) + getLowerBound();

        neuron.setBuffer(clip(x));
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

    @Override
    public String getDescription() {
        return "Logistic";
    }

    @Override
    public double clip(double val) {
        if (val < getLowerBound()) {
            return getLowerBound();
        }
        if (val > getUpperBound()) {
            return getUpperBound();
        }
        return val;
    }

    @Override
    public boolean isClipped() {
        return true;
    }

    @Override
    public void setClipped(boolean clipping) {
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
    public double getUpperBound() {
        return ceiling;
    }

    @Override
    public double getLowerBound() {
        return floor;
    }

}
