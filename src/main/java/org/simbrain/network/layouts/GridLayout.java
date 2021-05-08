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

import org.simbrain.network.core.Neuron;
import org.simbrain.util.UserParameter;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lay neurons out in a grid.
 *
 * @author Jeff Yoshimi
 */
public class GridLayout extends Layout {

    /**
     * The default number of columns if manual columns are allowed.
     */
    public static final int DEFAULT_NUM_COLUMNS = 3;

    /**
     * The default horizontal spacing.
     */
    public static final double DEFAULT_H_SPACING = 50;

    /**
     * The default vertical spacing.
     */
    public static final double DEFAULT_V_SPACING = 50;

    /**
     * The default allowed state for manual cols.
     */
    public static final boolean DEFAULT_AUTO_COLS = true;

    /**
     * Initial x position of line of neurons.
     */
    private double initialX;

    /**
     * Initial y position of line of neurons.
     */
    private double initialY;

    /**
     * Horizontal spacing between neurons.
     */
    @UserParameter(label = "Horizontal Spacing", description = "Horizontal spacing between neurons")
    private double hSpacing = DEFAULT_H_SPACING;

    /**
     * Vertical spacing between neurons.
     */
    @UserParameter(label = "Vertical Spacing", description = "Vertical spacing between neurons")
    private double vSpacing = DEFAULT_V_SPACING;

    /**
     * Manually set number of columns in grid.
     */
    @UserParameter(label = "Auto Columns",
        description = "If true, set the number of columns automatically to get a square or nearly-square grid",
        order = 10)
    private boolean autoColumns = DEFAULT_AUTO_COLS;

    /**
     * Number of columns in the layout, when auto columns is false.
     */
    @UserParameter(label = "Number of Columns",
        description = "Number of columns in the grid",
        order = 20)
    private int numColumns = DEFAULT_NUM_COLUMNS;

    /**
     * Default constructor.
     */
    public GridLayout() {
    }

    /**
     * Create a grid layout that automatically produces a square
     * grid based on number of neurons.
     *
     * @param hSpacing horizontal spacing between neurons
     * @param vSpacing vertical spacing between neurons
     */
    public GridLayout(final double hSpacing, final double vSpacing) {
        this.hSpacing = hSpacing;
        this.vSpacing = vSpacing;
        this.autoColumns = true;
    }

    /**
     * Create a grid layout with a specific number of columns.
     *
     * @param hSpacing   horizontal spacing between neurons
     * @param vSpacing   vertical spacing between neurons
     * @param numColumns number of columns of neurons
     */
    public GridLayout(final double hSpacing, final double vSpacing, final int numColumns) {
        this.hSpacing = hSpacing;
        this.vSpacing = vSpacing;
        this.numColumns = numColumns;
        this.autoColumns = false; //TODO else why set numColumns?
    }

    @Override
    public void layoutNeurons(final List<Neuron> neurons) {

        if(neurons.size() == 0) {
            return;
        }

        int numCols = numColumns;

        // If auto-columns set numcolumns automatically
        if (autoColumns) {
            if (neurons.size() > 3) {
                numCols = (int) Math.sqrt(neurons.size());
            } else {
                // Better-looking results for 3 or fewer neurons
                numCols = 2;
            }
        }

        int rowNum = -1;
        for (int i = 0; i < neurons.size(); i++) {
            Neuron neuron = neurons.get(i);
            if (i % numCols == 0) {
                rowNum++;
            }
            neuron.setLocation(initialX + (i % numCols) * hSpacing, initialY + rowNum * vSpacing);
        }
    }

    /**
     * Returns a list of columns corresponding to a set of neurons assumed to be
     * in the form of a grid. Utility method currently used in scripts.
     * <p>
     * TODO: Possibly move this method to a utility class for determining the
     * locations of neurons and other network elements.
     *
     * @param neurons the list of neurons
     * @param numRows the number of rows in the grid
     * @return the list of columns, ordered by x-direction
     */
    public static List<List<Neuron>> getColumnList(final List<Neuron> neurons, int numRows) {
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

    public int getNumColumns() {
        return numColumns;
    }

    public void setNumColumns(final int numColumns) {
        this.numColumns = numColumns;
    }

    public double getHSpacing() {
        return hSpacing;
    }

    public void setHSpacing(final double spacing) {
        hSpacing = spacing;
    }

    public double getVSpacing() {
        return vSpacing;
    }

    public void setVSpacing(final double spacing) {
        vSpacing = spacing;
    }

    public boolean isAutoColumns() {
        return autoColumns;
    }

    public void setAutoColumns(boolean autoColumns) {
        this.autoColumns = autoColumns;
    }

    @Override
    public String toString() {
        return "Grid Layout";
    }

    @Override
    public GridLayout copy() {
        GridLayout layout = new GridLayout(hSpacing, vSpacing, numColumns);
        layout.setAutoColumns(autoColumns);
        return layout;
    }


}
