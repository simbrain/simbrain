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
package org.simnet.util;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;

/**
 * <b>SimnetUtils</b> provides utility classes.
 */
public class SimnetUtils {

    /**
     * Return the upper left corner of a list of objects, based on neurons.
     *
     * @param objects list of objects
     * @return the point corresponding to the upper left corner of the objects
     */
    public static Point2D getUpperLeft(final ArrayList objects) {
        double x = Double.MAX_VALUE;
        double y = Double.MAX_VALUE;

        for (final Object object : objects) {
            if (object instanceof Neuron) {
                Neuron neuron = (Neuron) object;
                if (neuron.getX() < x) {
                    x = neuron.getX();
                }
                if (neuron.getY() < y) {
                    y = neuron.getY();
                }
            } else if (object instanceof Network) {
                for (Neuron neuron : ((Network) object).getFlatNeuronList()) {
                    if (neuron.getX() < x) {
                        x = neuron.getX();
                    }
                    if (neuron.getY() < y) {
                        y = neuron.getY();
                    }
                }
            }
        }
        if (x == Double.MAX_VALUE) {
            x = 0;
        }
        if (y == Double.MAX_VALUE) {
            y = 0;
        }
        return new Point2D.Double(x, y);
    }

    /**
     * Translate a set of objects.
     *
     * @param objects list of network objects to translate
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    public static void translate(final ArrayList objects, final double offsetX, final double offsetY) {
        for (Object object : objects) {
            if (object instanceof Neuron) {
                Neuron neuron = (Neuron) object;
                neuron.setX(neuron.getX() + offsetX);
                neuron.setY(neuron.getY() + offsetY);
            }
            if (object instanceof Network) {
                for (Neuron neuron : ((Network) object).getFlatNeuronList()) {
                    neuron.setX(neuron.getX() + offsetX);
                    neuron.setY(neuron.getY() + offsetY);
                }
            }
        }
    }
}
