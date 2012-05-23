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
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;

/**
 * Lay neurons out in successive layers. Assumes a complex network with
 * subnetworks.
 *
 * Confusing to use and hence deprecated. Superseded by LayeredNetworkBuilder
 * (which uses other Layout objects)
 *
 * @author jyoshimi
 */
@Deprecated public class LayersLayout implements Layout {

    /** Lay neurons out vertically. */
    public static final int VERTICAL = 0;

    /** Lay neurons out horizontally. */
    public static final int HORIZONTAL = 1;

    /** Initial x position of line of neurons. */
    private double initialX;

    /** Initial y position of line of neuorns. */
    private double initialY;

    /** Horizontal spacing between neurons. */
    private double hSpacing;

    /** Vertical spacing between neurons. */
    private double vSpacing;

    /** What layout to use: vertical or horizontal. */
    private int layout;

    /**
     * Lay out a complex network into layers.
     *
     * @param hspacing
     * @param vspacing
     * @param layout
     */
    public LayersLayout(final double hspacing, final double vspacing, final int layout) {
        this.vSpacing = vspacing;
        this.hSpacing = hspacing;
        this.layout = layout;
    }

    /** 
     * {@inheritDoc}
     */
    public void layoutNeurons(final Network network) {

        ArrayList<Network> layers = network.getNetworkList();

        int baseCount = (layers.get(0)).getNeuronCount();
        double y = initialY + layers.size() * vSpacing;

        for (int i = 0; i < layers.size(); i++) {
            Network currentLayer = layers.get(i);
            List<Neuron> neurons = currentLayer.getFlatNeuronList();
            for (int j = 0; j < neurons.size(); j++) {
                double hOffset = ((baseCount - neurons.size()) * hSpacing) / 2;
                Neuron neuron = (Neuron) neurons.get(j);
                neuron.setX(initialX + hOffset + (j * hSpacing));
                neuron.setY(y - (i * vSpacing));
            }
        }
    }

    /** 
     * {@inheritDoc}
     */
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    /** 
     * {@inheritDoc}
     */
    public String getLayoutName() {
        return "Layers";
    }

    /** 
     * {@inheritDoc}
     */
    public void layoutNeurons(List<Neuron> neurons) {
        //  TODO: No implementation yet; this will requires
        //      specifying number of layers and a size for each.
    }
}
