/*
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
package org.simbrain.network.util;

import java.util.Comparator;

import org.simbrain.network.core.Neuron;

/**
 * Comparators for using in sorting various kinds of org.simbrain.network lists.
 *
 * @author jyoshimi
 */
public class Comparators {

    /**
     * Comparator which orders by X coordinate.
     */
    public static final Comparator<Neuron> X_ORDER = new Comparator<Neuron>() {
        public String toString() {
            return "Vertical";
        }

        public int compare(final Neuron neuron1, final Neuron neuron2) {
            return Double.compare(neuron1.getX(), neuron2.getX());
        }
    };

    /**
     * Comparator which orders by X coordinate.
     */
    public static final Comparator<Neuron> Y_ORDER = new Comparator<Neuron>() {
        public String toString() {
            return "Horizontal";
        }

        public int compare(final Neuron neuron1, final Neuron neuron2) {
            return Double.compare(neuron1.getY(), neuron2.getY());
        }
    };
}
