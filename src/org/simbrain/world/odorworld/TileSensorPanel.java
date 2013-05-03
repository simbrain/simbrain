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
package org.simbrain.world.odorworld;

import javax.swing.JTextField;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.TileSensor;

/**
 * Panel to add a single tile sensor to an entity.
 *
 * @author Lam Nguyen
 *
 */
public class TileSensorPanel extends AbstractSensorPanel {

    /** Text field to edit value. */
    private JTextField activation = new JTextField("" + 1);

    /** Text field to edit x. */
    private JTextField x = new JTextField("" + 0);

    /** Text field to edit y. */
    private JTextField y = new JTextField("" + 0);

    /** Text field to edit width. */
    private JTextField width = new JTextField("" + 20);

    /** Text field to edit height. */
    private JTextField height = new JTextField("" + 20);

    /** Entity to which a tile sensor is being added. */
    private OdorWorldEntity entity;

    /**
     * Default constructor.
     *
     * @param entity the entity to which a tile sensor is added.
     */
    public TileSensorPanel(final OdorWorldEntity entity) {
        this.entity = entity;
        addItem("Activation amount", activation);
        addItem("X", x);
        addItem("Y", y);
        addItem("Width", width);
        addItem("Height", height);
    }

    @Override
    public void commitChanges() {
        TileSensor sensor = new TileSensor(entity,
                Integer.parseInt(x.getText()), Integer.parseInt(y.getText()),
                Integer.parseInt(width.getText()), Integer.parseInt(height
                        .getText()));
        sensor.setActivationAmount(Double.parseDouble(activation.getText()));
        entity.addSensor(sensor);
    }

    /** Fill in appropriate text fields when tile sensor is being modified. */
    public void fillFieldValues(TileSensor sensor) {
        activation.setText("" + Double.toString(sensor.getActivationAmount()));
        x.setText("" + Integer.toString(sensor.getX()));
        y.setText("" + Integer.toString(sensor.getY()));
        width.setText("" + Integer.toString(sensor.getWidth()));
        height.setText("" + Integer.toString(sensor.getHeight()));
    }

    /** Save changes to an edited straight movement effector. */
    public void commitChanges(TileSensor sensor) {
        sensor.setActivationAmount(Double.parseDouble(activation.getText()));
        sensor.setX(Integer.parseInt(x.getText()));
        sensor.setY(Integer.parseInt(y.getText()));
        sensor.setWidth(Integer.parseInt(width.getText()));
        sensor.setHeight(Integer.parseInt(height.getText()));
        sensor.getParent().getParentWorld()
                .fireEntityChanged(sensor.getParent());
    }
}
