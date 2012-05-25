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
package org.simbrain.network.layouts;

import java.awt.geom.Point2D;
import java.util.List;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;

/**
 * Interface for all neuron layout managers, which arrange a set of neurons in
 * different ways.
 *
 * @author jyoshimi
 */
public interface Layout {

    /**
     * Layout a list of neurons.
     *
     * @param neurons the list of neurons
     */
    void layoutNeurons(List<Neuron> neurons);

    /**
     * @return the name of this layout type
     */
    String getLayoutName();

    /**
     * Set the initial position.
     *
     * @param initialPoint initial position
     */
    void setInitialLocation(final Point2D initialPoint);

}
