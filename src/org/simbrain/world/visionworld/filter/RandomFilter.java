/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.visionworld.filter;

import java.awt.image.BufferedImage;

import java.util.Random;

import org.simbrain.world.visionworld.Filter;

/**
 * Random filter.
 */
public final class RandomFilter
    implements Filter {

    /** Display name. */
    private static final String DISPLAY_NAME = "Random filter";

    /** Minimum value. */
    private final double minimumValue;

    /** Maximum value. */
    private final double maximumValue;

    /** Difference between minimum and maximum values. */
    private final double difference;

    /** Source of randomness. */
    private final Random random;


    /**
     * Create a new random filter with the specified minimum and
     * maximum values.
     *
     * @param minimumValue minimum value for this random filter
     * @param maximumValue maximum value for this random filter
     * @throws IllegalArgumentException if <code>minimumValue</code>
     *    is greater than <code>maximumValue</code>
     */
    public RandomFilter(final double minimumValue, final double maximumValue) {
        if (minimumValue > maximumValue) {
            throw new IllegalArgumentException("maximumValue must be >= minimumValue");
        }
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
        difference = Math.abs(this.maximumValue - this.minimumValue);
        random = new Random();
    }


    /**
     * Return the minimum value for this random filter.
     *
     * @return the minimum value for this random filter
     */
    public double getMinimumValue() {
        return minimumValue;
    }

    /**
     * Return the maximum value for this random filter.
     *
     * @return the maximum value for this random filter
     */
    public double getMaximumValue() {
        return maximumValue;
    }

    /** {@inheritDoc} */
    public double filter(final BufferedImage image) {
        double r = random.nextDouble();
        double value = minimumValue + (r * difference);
        return value;
    }


    public String getDescription() {
        return DISPLAY_NAME + ", min=" + minimumValue + ", max=" + maximumValue;
    }
}
