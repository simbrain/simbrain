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
package org.simbrain.world.odorworld.sensors;

import org.simbrain.util.UserParameter;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * A grid of sensors which detects whether the entity is in a given cell of
 * this grid. Broadly inspired by "place cells" (https://en.wikipedia.org/wiki/Place_cell)
 *
 * @author jyoshimi
 */
public class GridSensor extends Sensor implements VisualizableEntityAttribute {

    /**
     * Default Activation Amount.
     */
    public static final double DEFAULT_ACTIVATION = 1;

    /**
     * Default X coordinate of the upper left corner.
     */
    public static final int DEFAULT_X = 0;

    /**
     * Default Y coordinate of the upper left corner.
     */
    public static final int DEFAULT_Y = 0;

    /**
     * Default width.
     */
    public static final int DEFAULT_WIDTH = 32;

    /**
     * Default height.
     */
    public static final int DEFAULT_HEIGHT = 32;

    /**
     * Current value of the sensor; activationAmount if "active", 0 otherwise.
     */
    private double[] values;

    /**
     * Value to return when the tile sensor is activated.
     */
    @UserParameter(label = "Activation amount",
            description = "Amount of activation that a neuron coupled with the tile sensor receives "
                    + "when the tile sensor is activated. ",
            order = 3)
    private double activationAmount = DEFAULT_ACTIVATION;

    /**
     * Upper left corner.
     */
    @UserParameter(label = "Start X",
            description = "x coordinates for the location of the top-left corner of the tile sensor.",
            order = 4)
    private int x;


    /**
     * Upper left corner.
     */
    @UserParameter(label = "Start Y",
            description = "y coordinates for the location of the top-left corner of the tile sensor.",
            order = 5)
    private int y;

    @UserParameter(
            label = "Num Columns",
            description = "Number of columns of the sensor grid",
            order = 8
    )
    private int columns = 5;

    @UserParameter(
            label = "Num Rows",
            description = "Number of columns of the sensor grid",
            order = 9
    )
    private int rows = 5;

    /**
     * Width of the sensor.
     */
    @UserParameter(label = "Cell Width",
        description = "Determines the size of the tile. Width specifies the horizontal length of the tile sensor.",
        order = 11)
    private int width = DEFAULT_WIDTH;

    /**
     * Height of the sensor.
     */
    @UserParameter(label = "Cell Height",
        description = "Determines the size of the tile. Height specifies the vertical length.",
        order = 12)
    private int height = DEFAULT_HEIGHT;

    /**
     * Whether to draw the sensor grid in GridSensorNode.
     */
    @UserParameter(
            label = "Grid Visibility",
            description = "Show sensor grid when this is set to true, hide when set to false false",
            order = 13
    )
    private boolean gridVisibility = true;

    /**
     * Construct a tile sensor.
     *
     * @param parent parent entity
     * @param x      upper left
     * @param y      upper right
     * @param width  width in pixels
     * @param height height
     */
    public GridSensor(OdorWorldEntity parent, int x, int y, int width, int height) {
        super(parent, "Grid (" + x + "," + y + "):" + width + "x" + height);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.values = new double[columns * rows];
        super.setTheta(0);
        super.setRadius(25);
    }

    /**
     * Construct a copy of a location sensor.
     *
     * @param gridSensor the location sensor to copy
     */
    public GridSensor(GridSensor gridSensor) {
        super(gridSensor);
        this.activationAmount = gridSensor.activationAmount;
        this.x = gridSensor.x;
        this.y = gridSensor.y;
        this.width = gridSensor.width;
        this.height = gridSensor.height;
        this.rows = gridSensor.rows;
        this.columns = gridSensor.columns;
    }

    /**
     * Default constructor for {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     *
     * NOTE:
     * {@link org.simbrain.world.odorworld.dialogs.AddSensorDialog} handles the set up of {@link #parent}.
     * When calling this directly, remember to set up the required field {@link #parent} accordingly.
     */
    public GridSensor() {
        super();
    }

    @Override
    public void update() {
        values = new double[columns * rows];
        int gridX = (int) (getLocation()[0] / width - x);
        int gridY = (int) (getLocation()[1] / height - y);
        if (gridX < columns && gridY < rows && gridX >= 0 && gridY >= 0) {
            values[gridX + gridY * rows] = DEFAULT_ACTIVATION;
        }
    }

    /**
     * @return value associated with this sensor, 0 if occupied,
     */
    @Producible(customDescriptionMethod = "getAttributeDescription")
    public double[] getValues() {
        return values;
    }

    public double getActivationAmount() {
        return activationAmount;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public void setActivationAmount(double amount) {
        activationAmount = amount;
    }

    @Override
    public void setParent(OdorWorldEntity parent) {
        this.parent = parent;
    }

    @Override
    public GridSensor copy() {
        return new GridSensor(this);
    }

    @Override
    public String getLabel() {
        if (super.getLabel().isEmpty()) {
            return getDirectionString() + "Grid Sensor";
        } else {
            return super.getLabel();
        }
    }

    public boolean getGridVisibility() {
        return gridVisibility;
    }

    @Override
    public String getName() {
        return "Grid Sensor";
    }
}