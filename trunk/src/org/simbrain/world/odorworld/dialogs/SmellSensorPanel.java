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
import org.simbrain.world.odorworld.sensors.SmellSensor;

/**
 * Panel to add a smell sensor to an entity.
 *
 * @author Lam Nguyen
 *
 */
public class SmellSensorPanel extends AbstractSensorPanel {

    /** Text field to edit label. */
    private JTextField label = new JTextField();

    /** Text field to edit theta. */
    private JTextField theta = new JTextField();

    /** Text field to edit radius. */
    private JTextField radius = new JTextField();

    /** Entity to which a smell sensor is being added. */
    private OdorWorldEntity entity;

    /**
     * Reference to smell sensor. Initially null if this is a creation panel.
     */
    private SmellSensor smellSensor;

    /** If true this is a creation panel. Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * Constructor for the case where a sensor is being created.
     *
     * @param entity the entity to which a smell sensor is added.
     */
    public SmellSensorPanel(OdorWorldEntity entity) {
        this.entity = entity;
        isCreationPanel = true;
        addItem("Label", label);
        addItem("Sensor angle", theta);
        addItem("Sensor length", radius);
        fillFieldValues();
    }

    /**
     * Constructor for the case where a sensor is being edited.
     *
     * @param entity the entity to which a smell sensor is added.
     * @param sensor sensor to edit
     */
    public SmellSensorPanel(OdorWorldEntity entity, SmellSensor sensor) {
        this.entity = entity;
        this.smellSensor = sensor;
        isCreationPanel = false;
        addItem("Label", label);
        addItem("Sensor angle", theta);
        addItem("Sensor length", radius);
        fillFieldValues();
    }

    @Override
    public void commitChanges() {
        if (isCreationPanel) {
            entity.addSensor(new SmellSensor(entity, label.getText(), Double
                    .parseDouble(theta.getText()), Double.parseDouble(radius
                    .getText())));
        } else {
            smellSensor.setLabel(label.getText());
            smellSensor.setTheta(Double.parseDouble(theta.getText()));
            smellSensor.setRadius(Double.parseDouble(radius.getText()));
            smellSensor.getParent().getParentWorld()
                    .fireEntityChanged(smellSensor.getParent());
        }
    }

    @Override
    public void fillFieldValues() {
        if (isCreationPanel) {
            label.setText("" + SmellSensor.DEFAULT_LABEL);
            theta.setText("" + SmellSensor.DEFAULT_THETA);
            radius.setText("" + SmellSensor.DEFAULT_RADIUS);
        } else {
            label.setText("" + smellSensor.getLabel());
            theta.setText("" + smellSensor.getTheta());
            radius.setText("" + smellSensor.getRadius());
        }
    }
}
