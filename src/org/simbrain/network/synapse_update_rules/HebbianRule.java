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
package org.simbrain.network.synapse_update_rules;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;



/**
 * <b>Hebbian</b> implements a standard Hebbian learning rule.
 */
public class HebbianRule extends SynapseUpdateRule {

    /** Default learning rate. */
    public static final double DEFAULT_LEARNING_RATE = .1;

    /** Learning rate. */
    private double learningRate = DEFAULT_LEARNING_RATE;


    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getDescription() {
        return "Hebbian";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        HebbianRule h = new HebbianRule();
        h.setLearningRate(getLearningRate());
        return h;
    }

    @Override
    public void update(Synapse synapse) {
        double input = synapse.getSource().getActivation();
        double output = synapse.getTarget().getActivation();
        double strength = synapse.clip(synapse.getStrength()
                + (learningRate * input * output));
        synapse.setStrength(strength);
    }

    /**
     * @return Returns the momentum.
     */
    public double getLearningRate() {
        return learningRate;
    }

    /**
     * @param rate The rate to set.
     */
    public void setLearningRate(final double rate) {
        this.learningRate = rate;
    }

}
