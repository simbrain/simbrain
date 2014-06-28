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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.simbrain.network.core.Neuron;

/**
 * Lay neurons out in a grid.
 *
 * @author Jeff Yoshimi
 */
public class GridLayout implements Layout {

    /** The default number of columns if manual columns are allowed. */
    public static final int DEFAULT_NUM_COLUMNS = 3;

    /** The default horizontal spacing. */
    public static final double DEFAULT_H_SPACING = 50;

    /** The default vertical spacing. */
    public static final double DEFAULT_V_SPACING = 50;

    /** The default allowed state for manual cols. */
    public static final boolean DEFAULT_MANUAL_COLS = false;

    /** Initial x position of line of neurons. */
    private double initialX;

    /** Initial y position of line of neurons. */
    private double initialY;

    /** Number of columns in the layout. */
    private int numColumns = DEFAULT_NUM_COLUMNS;

    /** Horizontal spacing between neurons. */
    private double hSpacing = DEFAULT_H_SPACING;

    /** Vertical spacing between neurons. */
    private double vSpacing = DEFAULT_V_SPACING;

    /** Manually set number of columns in grid. */
    private boolean manualColumns = DEFAULT_MANUAL_COLS;

    /**
     * Create a layout.
     *
     * @param hSpacing horizontal spacing between neurons
     * @param vSpacing vertical spacing between neurons
     * @param numColumns number of columns of neurons
     */
    public GridLayout(final double hSpacing, final double vSpacing,
            final int numColumns) {
        this.hSpacing = hSpacing;
        this.vSpacing = vSpacing;
        this.numColumns = numColumns;
        //this.manualColumns = true; // else why set numColumns?
    }

    /**
     * Default constructor.
     */
    public GridLayout() {
    }

    /**
     * {@inheritDoc}
     */
    public void layoutNeurons(final List<Neuron> neurons) {
        int numCols = numColumns;
        if (!manualColumns) {
            numCols = (int) Math.sqrt(neurons.size());
        }

        int rowNum = -1;
        for (int i = 0; i < neurons.size(); i++) {
            Neuron neuron = neurons.get(i);
            if (i % numCols == 0) {
                rowNum++;
            }
            neuron.setX(initialX + (i % numCols) * hSpacing);
            neuron.setY(initialY + rowNum * vSpacing);
        }
    }

    /**
     * Returns a list of columns corresponding to a set of neurons assumed to be
     * in the form of a grid. Utility method currently used in scripts.
     *
     * TODO: Possibly move this method to a utility class for determining the
     * locations of neurons and other network elements.
     *
     *
     * @param neurons the list of neurons
     * @param numRows the number of rows in the grid
     * @return the list of columns, ordered by x-direction
     */
    public static List<List<Neuron>> getColumnList(final List<Neuron> neurons,
            int numRows) {
        // Sort by x value
        ArrayList<Neuron> neuronList = new ArrayList<Neuron>(neurons);
        Collections.sort(neuronList, new Comparator<Neuron>() {
            public int compare(Neuron n1, Neuron n2) {
                return Double.compare(n1.getX(), n2.getX());
            }
        });

        List<List<Neuron>> grid = new ArrayList<List<Neuron>>();
        List<Neuron> currentColumn = new ArrayList<Neuron>();
        for (int i = 1; i < neuronList.size() + 1; i++) {
            Neuron neuron = neuronList.get(i - 1);
            currentColumn.add(neuron);
            if ((i > 0) && (i % numRows) == 0) {
                // Sort by y value
                Collections.sort(currentColumn, new Comparator<Neuron>() {
                    public int compare(Neuron n1, Neuron n2) {
                        return Double.compare(n1.getY(), n2.getY());
                    }
                });
                grid.add(currentColumn);
                currentColumn = new ArrayList<Neuron>();
            }
        }
        return grid;
    }

    @Override
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    @Override
    public String getDescription() {
        return "Grid";
    }

    /**
     * @return the numColumns
     */
    public int getNumColumns() {
        return numColumns;
    }

    /**
     * @param numColumns the numColumns to set
     */
    public void setNumColumns(final int numColumns) {
        this.numColumns = numColumns;
    }

    /**
     * @return the hSpacing
     */
    public double getHSpacing() {
        return hSpacing;
    }

    /**
     * @param spacing the hSpacing to set
     */
    public void setHSpacing(final double spacing) {
        hSpacing = spacing;
    }

    /**
     * @return the vSpacing
     */
    public double getVSpacing() {
        return vSpacing;
    }

    /**
     * @param spacing the vSpacing to set
     */
    public void setVSpacing(final double spacing) {
        vSpacing = spacing;
    }

    @Override
    public String toString() {
        return "Grid Layout";
    }

    /**
     * @return the manualColumns
     */
    public boolean isManualColumns() {
        return manualColumns;
    }

    /**
     * @param manualColumns the manualColumns to set
     */
    public void setManualColumns(boolean manualColumns) {
        this.manualColumns = manualColumns;
    }
}
