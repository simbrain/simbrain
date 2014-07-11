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
import java.util.List;

import org.simbrain.network.core.Neuron;

/**
 * Comparators for using in sorting various kinds of org.simbrain.network lists.
 *
 * @author Zach Tosi
 * @author jyoshimi
 */
public enum OrientationComparator implements Comparator<Neuron> {

    X_ORDER {

        @Override
        public String toString() {
            return "Horizontal (L to R)";
        }

        @Override
        public int compare(final Neuron neuron1, final Neuron neuron2) {
            return Double.compare(neuron1.getX(), neuron2.getX());
        }

    },
    Y_ORDER {

        @Override
        public String toString() {
            return "Vertical (U to D)";
        }

        @Override
        public int compare(final Neuron neuron1, final Neuron neuron2) {
            return Double.compare(neuron1.getY(), neuron2.getY());
        }

    },
    X_REVERSE {

        @Override
        public String toString() {
            return "Horizontal (R to L)";
        }

        @Override
        public int compare(final Neuron neuron1, final Neuron neuron2) {
            return -Double.compare(neuron1.getX(), neuron2.getX());
        }

    },
    Y_REVERSE {

        @Override
        public String toString() {
            return "Vertical (D to U)";
        }

        @Override
        public int compare(final Neuron neuron1, final Neuron neuron2) {
            return -Double.compare(neuron1.getY(), neuron2.getY());
        }

    };

    /**
     *
     * @param neuron1
     * @param neuron2
     * @return
     */
    public abstract int compare(final Neuron neuron1, final Neuron neuron2);

    /**
     *
     * @param neurons
     * @return
     */
    public static double findMinX(List<Neuron> neurons) {
        double minX = Double.MAX_VALUE;
        for (Neuron n : neurons) {
            if (n.getX() < minX) {
                minX = n.getX();
            }
        }
        return minX;
    }

    public static double findMaxX(List<Neuron> neurons) {
        double maxX = Double.MIN_VALUE;
        for (Neuron n : neurons) {
            if (n.getX() > maxX) {
                maxX = n.getX();
            }
        }
        return maxX;
    }

    public static double findMinY(List<Neuron> neurons) {
        double minY = Double.MAX_VALUE;
        for (Neuron n : neurons) {
            if (n.getY() < minY) {
                minY = n.getY();
            }
        }
        return minY;
    }

    public static double findMaxY(List<Neuron> neurons) {
        double maxY = Double.MIN_VALUE;
        for (Neuron n : neurons) {
            if (n.getY() > maxY) {
                maxY = n.getY();
            }
        }
        return maxY;
    }

    public static double findMidpointX(List<Neuron> neurons) {
        double sumX = 0;
        for (Neuron n : neurons) {
            sumX += n.getX();
        }
        return sumX / neurons.size();
    }

    public static double findMidpointY(List<Neuron> neurons) {
        double sumY = 0;
        for (Neuron n : neurons) {
            sumY += n.getY();
        }
        return sumY / neurons.size();
    }

}