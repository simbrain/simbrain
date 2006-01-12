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
package org.simnet.synapses;

import org.simnet.interfaces.Synapse;
import org.simnet.util.RandomSource;


/**
 * <b>RandomSynapse</b>.
 */
public class RandomSynapse extends Synapse {
    /** Randomizer. */
    private RandomSource randomizer = new RandomSource();

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public RandomSynapse() {
        randomizer.setUpperBound(this.getUpperBound());
        randomizer.setLowerBound(this.getLowerBound());
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Synapse to be made of type
     */
    public RandomSynapse(final Synapse n) {
        super(n);
        randomizer.setUpperBound(this.getUpperBound());
        randomizer.setLowerBound(this.getLowerBound());
    }

    /**
     * @return duplicate RandomSynapse (used, e.g., in copy/paste).
     */
    public Synapse duplicate() {
        RandomSynapse rs = new RandomSynapse();
        rs = (RandomSynapse) super.duplicate(rs);
        rs.randomizer = randomizer.duplicate(randomizer);

        return rs;
    }

    /**
     * Updates the synapse.
     */
    public void update() {
        randomizer.setUpperBound(this.getUpperBound());
        randomizer.setLowerBound(this.getLowerBound());
        strength = randomizer.getRandom();
        strength = clip(strength);
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Random";
    }

    /**
     * @return Returns the randomizer.
     */
    public RandomSource getRandomizer() {
        return randomizer;
    }

    /**
     * @param randomizer The randomizer to set.
     */
    public void setRandomizer(final RandomSource randomizer) {
        this.randomizer = randomizer;
        this.setUpperBound(randomizer.getUpperBound());
        this.setLowerBound(randomizer.getLowerBound());
    }
}
