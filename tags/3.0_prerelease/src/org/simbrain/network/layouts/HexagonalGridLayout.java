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

import org.simbrain.network.core.Neuron;

/**
 * Lay neurons out in a hexagonal grid.
 *
 * @author wstclair
 */
public class HexagonalGridLayout implements Layout {

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
    public HexagonalGridLayout(final double hSpacing, final double vSpacing,
            final int numColumns) {
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
            if (rowNum % 2 == 0) {
                neuron.setX(initialX + hSpacing / 2 + (i % numCols) * hSpacing);
            } else {
                neuron.setX(initialX + (i % numCols) * hSpacing);
            }
            neuron.setY(initialY + rowNum * vSpacing);
        }
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
        return "Hexagonal Grid Layout";
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
