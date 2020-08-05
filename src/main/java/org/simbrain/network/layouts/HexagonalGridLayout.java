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
import java.util.List;

/**
 * Lay neurons out in a hexagonal grid.
 *
 * @author wstclair
 */
public class HexagonalGridLayout implements Layout {

    //TODO: Extend GridLayout?

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
    @UserParameter(label = "Horizontal Spacing", description = "Horizontal spacing between neurons", order = 10)
    private double hSpacing = DEFAULT_H_SPACING;

    /**
     * Vertical spacing between neurons.
     */
    @UserParameter(label = "Vertical Spacing", description = "Vertical spacing between neurons", order = 20)
    private double vSpacing = DEFAULT_V_SPACING;

    /**
     * Manually set number of columns in grid.
     */
    @UserParameter(label = "Auto Columns",
        description = "If true, set the number of columns automatically to get a square or nearly-square grid",
        order = 30)
    private boolean autoColumns = DEFAULT_AUTO_COLS;

    /**
     * Number of columns in the layout.
     */
    @UserParameter(label = "Number of Columns", description = "Number of columns in the grid", order = 40)
    private int numColumns = DEFAULT_NUM_COLUMNS;

    /**
     * Create a layout.
     *
     * @param hSpacing   horizontal spacing between neurons
     * @param vSpacing   vertical spacing between neurons
     * @param numColumns number of columns of neurons
     */
    public HexagonalGridLayout(final double hSpacing, final double vSpacing, final int numColumns) {
        this.hSpacing = hSpacing;
        this.vSpacing = vSpacing;
        this.numColumns = numColumns;
    }

    /**
     * Default Constructor.
     */
    public HexagonalGridLayout() {
    }

    @Override
    public void layoutNeurons(final List<Neuron> neurons) {

        if(neurons.size() == 0) {
            return;
        }

        if (autoColumns) {
            numColumns = (int) Math.sqrt(neurons.size());
        }
        int rowNum = -1;
        for (int i = 0; i < neurons.size(); i++) {
            Neuron neuron = neurons.get(i);
            if (i % numColumns == 0) {
                rowNum++;
            }
            if (rowNum % 2 == 0) {
                neuron.setLocation(initialX + hSpacing / 2 + (i % numColumns) * hSpacing,
                        initialY + rowNum * vSpacing, false);
            } else {
                neuron.setLocation(initialX + (i % numColumns) * hSpacing, initialY + rowNum * vSpacing);
            }
        }
    }

    /**
     * Utility to layout a list of neurons with a hexagonal layout.
     *
     * @param neurons neurons to layout
     * @param hSpacing horizontal spacing
     * @param vSpacing vertical spacing
     */
    public static void layoutNeurons(List<Neuron> neurons, int hSpacing, int vSpacing) {
        new HexagonalGridLayout(hSpacing, vSpacing, (int) Math.sqrt(neurons.size())).layoutNeurons(neurons);
    }

    @Override
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    @Override
    public String getDescription() {
        return "Hex Grid";
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
        return "Hexagonal Grid Layout";
    }

    @Override
    public HexagonalGridLayout copy() {
        HexagonalGridLayout layout = new HexagonalGridLayout(hSpacing, vSpacing, numColumns);
        layout.setAutoColumns(this.autoColumns);
        return layout;
    }

}
