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
import org.simbrain.network.interfaces.SynapseUpdateRule;
import org.simbrain.network.util.RandomSource;


/**
 * <b>RandomSynapse</b>.
 */
public class RandomSynapse extends SynapseUpdateRule {

    /** Randomizer. */
    private RandomSource randomizer = new RandomSource();

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getDescription() {
        return "Random";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        RandomSynapse rs = new RandomSynapse();
        rs.randomizer = new RandomSource(randomizer);
        return rs;
    }

    @Override
    public void update(Synapse synapse) {
        randomizer.setUpperBound(synapse.getUpperBound());
        randomizer.setLowerBound(synapse.getLowerBound());
        synapse.setStrength(synapse.clip(randomizer.getRandom()));
    }

    /**
     * @return Returns the randomizer.
     */
    public RandomSource getRandomizer() {
        return randomizer;
    }
}
