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
package org.simbrain.network.neuron_update_rules.interfaces;

import org.simbrain.util.randomizer.Randomizer;

/**
 * An interface which should be implemented by any neuron update rule that uses
 * a randomizer to provide noise.
 *
 * @author zach
 *
 */
public interface NoisyUpdateRule {

    /**
     * Return the noise generator.
     *
     * @return the noise generator.
     */
    Randomizer getNoiseGenerator();

    /**
     * Set the noise generator.
     *
     * @param rand the generator to set
     */
    void setNoiseGenerator(Randomizer rand);

    /**
     * Return true if add noise is turned on.
     *
     * @return true if add noise is on; false otherwise.
     */
    boolean getAddNoise();

    /**
     * Set whether noise should be used.
     *
     * @param noise true if noise should be used; false otherwise.
     */
    void setAddNoise(boolean noise);

}
