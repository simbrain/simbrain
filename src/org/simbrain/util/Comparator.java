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
package org.simbrain.util;

import org.simbrain.network.interfaces.Neuron;

/**
 * <b>Comparator</b> is used to compare positions of PNodes.
 */
public class Comparator implements java.util.Comparator {

    /** Compare PNodes by their x dimension. */
    public static final int COMPARE_X = 1;

    /** Compare PNodes by their y dimension. */
    public static final int COMPARE_Y = 2;

    /** How to compare the PNodes. */
    private int comparisonType;

    /**
     * Constructor for Comparator.
     *
     * @param comparison whether to compare by x or y dimension.
     */
    public Comparator(final int comparison) {
        comparisonType = comparison;
    }

    /**
     * @see Comparator
     */
    public int compare(final Object o1, final Object o2) {
        Neuron p1 = (Neuron) o1;
        Neuron p2 = (Neuron) o2;
        Double d1, d2;
        if (comparisonType == COMPARE_X) {
            d1 = new Double(p1.getX());
            d2 = new Double(p2.getX());
        } else {
            d1 = new Double(p1.getY());
            d2 = new Double(p2.getY());
        }

        return d1.compareTo(d2);
    }
}