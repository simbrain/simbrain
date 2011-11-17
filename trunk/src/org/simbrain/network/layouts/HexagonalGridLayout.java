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
 * Lay neurons out in a hexagonal grid.
 *
 * @author wstclair
 */
public class HexagonalGridLayout implements Layout {

    /** Initial x position of line of neurons. */
    private double initialX;

    /** Initial y position of line of neurons. */
    private double initialY;

    /** Number of columns in the layout. */
    private static int numColumns = 3;

    /** Horizontal spacing between neurons. */
    private static double hSpacing = 50;

    /** Vertical spacing between neurons. */
    private static double vSpacing = 50;

    /** Manually set number of columns in grid. */
    private static boolean manualColumns = false;

    /**
     * Create a layout.
     *
     * @param hSpacing horizontal spacing between neurons
     * @param vSpacing vertical spacing between neurons
     * @param numColumns number of columns of neurons
     */
    public HexagonalGridLayout(final double hSpacing, final double vSpacing,
            final int numColumns) {
        HexagonalGridLayout.hSpacing = hSpacing;
        HexagonalGridLayout.vSpacing = vSpacing;
        HexagonalGridLayout.numColumns = numColumns;
    }

    /**
     * Default Constructor.
     */
    public HexagonalGridLayout() { }

    /**
     * {@inheritDoc}
     */
    public void layoutNeurons(final Network network) {
        ArrayList<Neuron> neurons = network.getFlatNeuronList();
        layoutNeurons(neurons);
    }

    /**
     * {@inheritDoc}
     */
    public void layoutNeurons(final List<Neuron> neurons) {
        int rowNum = 0;
        int numCols = numColumns;
        if (!manualColumns) {
            numCols = (int) Math.sqrt(neurons.size());
        }
        for (int i = 0; i < neurons.size(); i++) {
            Neuron neuron = (Neuron) neurons.get(i);
            if (i % numCols == 0) {
                rowNum++;
            }
            if (rowNum % 2 == 0) {
                neuron.setX(initialX + hSpacing / 2 + (i % numCols) * hSpacing);
            } else {
                neuron.setX(initialX + (i % numCols) * hSpacing);
            }
            neuron.setY(initialY + rowNum * vSpacing);
        }
    }

    /**
     *  {@inheritDoc}
     */
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    /**
     * {@inheritDoc}
     */
    public String getLayoutName() {
        return "Hexagonal Grid";
    }

    /**
     * @return the numColumns
     */
    public static int getNumColumns() {
        return numColumns;
    }

    /**
     * @param numColumns the numColumns to set
     */
    public static void setNumColumns(final int numColumns) {
        HexagonalGridLayout.numColumns = numColumns;
    }

    /**
     * @return the hSpacing
     */
    public static double getHSpacing() {
        return hSpacing;
    }

    /**
     * @param spacing the hSpacing to set
     */
    public static void setHSpacing(final double spacing) {
        hSpacing = spacing;
    }

    /**
     * @return the vSpacing
     */
    public static double getVSpacing() {
        return vSpacing;
    }

    /**
     * @param spacing the vSpacing to set
     */
    public static void setVSpacing(final double spacing) {
        vSpacing = spacing;
    }

    /** @override */
    public String toString() {
        return "Hexagonal Grid Layout";
    }

    /**
     * @return the manualColumns
     */
    public static boolean isManualColumns() {
        return manualColumns;
    }

    /**
     * @param manualColumns the manualColumns to set
     */
    public static void setManualColumns(boolean manualColumns) {
        HexagonalGridLayout.manualColumns = manualColumns;
    }

}
