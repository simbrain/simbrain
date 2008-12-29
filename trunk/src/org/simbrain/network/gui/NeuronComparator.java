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
package org.simbrain.network.gui;

import org.simbrain.network.interfaces.Neuron;

/**
 * <b>Comparator</b> is used to compare positions of PNodes.
 */
public class NeuronComparator implements java.util.Comparator<Neuron> {

    public enum Type {
        COMPARE_X, COMPARE_Y
    }
    
    /** How to compare the PNodes. */
    private Type comparisonType;

    /**
     * Constructor for Comparator.
     *
     * @param comparison whether to compare by x or y dimension.
     */
    public NeuronComparator(final Type comparison) {
        if (comparison != null) throw new RuntimeException("comparison type cannot be null");
        
        comparisonType = comparison;
    }

    /**
     * @see NeuronComparator
     */
    public int compare(final Neuron p1, final Neuron p2) {
        double d1;
        double d2;
        
        switch (comparisonType) {
        case COMPARE_X:
            d1 = p1.getX();
            d2 = p2.getX();
            break;
        case COMPARE_Y:
            d1 = p1.getX();
            d2 = p2.getX();
            break;
        default:
            throw new IllegalArgumentException("unknown: " + comparisonType);
        }

        return Double.valueOf(d1).compareTo(d2);
    }
}