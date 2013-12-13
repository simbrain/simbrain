/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.world.odorworld.dialogs;

import javax.swing.JTextField;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.TileSensor;

/**
 * Panel to add a single tile sensor to an entity or to modify an existing one.
 *
 * @author Lam Nguyen
 *
 */
public class TileSensorPanel extends AbstractSensorPanel {

    /** Text field to edit value. */
    private JTextField activation = new JTextField();

    /** Text field to edit x. */
    private JTextField x = new JTextField();

    /** Text field to edit y. */
    private JTextField y = new JTextField();

    /** Text field to edit width. */
    private JTextField width = new JTextField();

    /** Text field to edit height. */
    private JTextField height = new JTextField();

    /** Entity to which a tile sensor is being added. */
    private OdorWorldEntity entity;

    /**
     * Reference to straight movement effector. Initially null if this is a
     * creation panel.
     */
    private TileSensor tileSensor;

    /** If true this is a creation panel. Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * Constructor for the case where a sensor is being created.
     *
     * @param entity the entity to which a tile sensor is added.
     */
    public TileSensorPanel(final OdorWorldEntity entity) {
        this.entity = entity;
        isCreationPanel = true;
        addItem("Activation amount", activation);
        addItem("X", x);
        addItem("Y", y);
        addItem("Width", width);
        addItem("Height", height);
        fillFieldValues();
    }

    /**
     * Constructor for the case where an effector is being edited.
     *
     * @param entity parent entity
     * @param sensor sensor to edit
     */
    public TileSensorPanel(final OdorWorldEntity entity, TileSensor sensor) {
        this.entity = entity;
        this.tileSensor = sensor;
        isCreationPanel = false;
        addItem("Activation amount", activation);
        addItem("X", x);
        addItem("Y", y);
        addItem("Width", width);
        addItem("Height", height);
        fillFieldValues();
    }

    @Override
    public void commitChanges() {
        if (isCreationPanel) {
            TileSensor sensor = new TileSensor(entity, Integer.parseInt(x
                    .getText()), Integer.parseInt(y.getText()),
                    Integer.parseInt(width.getText()), Integer.parseInt(height
                            .getText()));
            sensor.setActivationAmount(Double.parseDouble(activation.getText()));
            entity.addSensor(sensor);
        } else {
            tileSensor.setActivationAmount(Double.parseDouble(activation
                    .getText()));
            tileSensor.setX(Integer.parseInt(x.getText()));
            tileSensor.setY(Integer.parseInt(y.getText()));
            tileSensor.setWidth(Integer.parseInt(width.getText()));
            tileSensor.setHeight(Integer.parseInt(height.getText()));
            tileSensor.getParent().getParentWorld()
                    .fireEntityChanged(tileSensor.getParent());
        }
    }

    /** Fill in appropriate text fields when tile sensor is being modified. */
    public void fillFieldValues() {
        if (isCreationPanel) {
            activation.setText("" + TileSensor.DEFAULT_ACTIVATION);
            x.setText("" + TileSensor.DEFAULT_X);
            y.setText("" + TileSensor.DEFAULT_Y);
            width.setText("" + TileSensor.DEFAULT_WIDTH);
            height.setText("" + TileSensor.DEFAULT_HEIGHT);
        } else {
            activation.setText(""
                    + Double.toString(tileSensor.getActivationAmount()));
            x.setText("" + Integer.toString(tileSensor.getX()));
            y.setText("" + Integer.toString(tileSensor.getY()));
            width.setText("" + Integer.toString(tileSensor.getWidth()));
            height.setText("" + Integer.toString(tileSensor.getHeight()));
        }
    }
}
