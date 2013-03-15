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
import javax.swing.JTextField;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

/**
 * Panel to add a smell sensor to an entity.
 *
 * @author Lam Nguyen
 *
 */
public class SmellSensorPanel extends AbstractSensorPanel {

    /** Text field to edit label. */
    private JTextField label = new JTextField("SmellSensor");

    /** Text field to edit theta. */
    private JTextField theta = new JTextField("" + Math.PI/4);

    /** Text field to edit radius. */
    private JTextField radius = new JTextField("" + 23);

    /** Entity to which a smell sensor is being added. */
    private OdorWorldEntity entity;

    /**
     * Default constructor.
     *
     * @param entity the entity to which a smell sensor is added.
     */
    public SmellSensorPanel(OdorWorldEntity entity) {
        this.entity = entity;
        addItem("Label", label);
        addItem("Sensor angle", theta);
        addItem("Sensor length", radius);
        setVisible(true);
    }

    @Override
    public void commitChanges() {
        entity.addSensor(new SmellSensor(entity, label.getText(), Double.parseDouble(theta.getText()), Double.parseDouble(radius.getText()))); // todo: label

    }

    /** Save changes to an edited smell sensor. */
    public void commitChanges(SmellSensor sensor) {
        sensor.setLabel(label.getText());
        sensor.setTheta(Double.parseDouble(theta.getText()));
        sensor.setRadius(Double.parseDouble(radius.getText()));
        sensor.getParent().getParentWorld()
        .fireEntityChanged(sensor.getParent());
    }

    /** Fill in appropriate text fields when smell sensor is being modified. */
    public void fillFieldValues(SmellSensor sensor) {
        label.setText("" + sensor.getLabel());
        theta.setText("" + sensor.getTheta());
        radius.setText("" + sensor.getRadius());
    }
}
