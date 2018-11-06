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

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.*;

import javax.swing.*;

/**
 * SensorDialog is a dialog box for adding Sensors to Odor World.
 *
 * @author Lam Nguyen
 */

public class AddSensorDialog extends StandardDialog {

    //TODO: Replace with annotated property editor

    /**
     * String of Sensor types.
     */
    private String[] sensors = {"Bump Sensor", "Hearing Sensor", "Object Sensor", "Smell Sensor", "Tile Sensor" };

    /**
     * Entity to which sensor is being added.
     */
    private OdorWorldEntity entity;

    /**
     * Select sensor type.
     */
    private JComboBox sensorType = new JComboBox(sensors);

    /**
     * Main dialog box.
     */
    private Box mainPanel = Box.createVerticalBox();

    /**
     * Panel for setting sensor type.
     */
    private LabelledItemPanel typePanel = new LabelledItemPanel();

    /**
     * Sensor Dialog add sensor constructor.
     *
     * @param entity
     */
    public AddSensorDialog(OdorWorldEntity entity) {
        this.entity = entity;
        init("Add Sensor");
    }

    /**
     * Initialize default constructor.
     */
    private void init(String title) {
        setTitle(title);
        typePanel.addItem("Sensor Type", sensorType);
        sensorType.setSelectedItem("Smell Sensor");
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/OdorWorld/sensors.html");
        addButton(new JButton(helpAction));
        initPanel();
        mainPanel.add(typePanel);
        setContentPane(mainPanel);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Initialize the Sensor Dialog Panel based upon the current sensor type.
     */
    private void initPanel() {
        if (sensorType.getSelectedItem() == "Tile Sensor") {
            setTitle("Add a tile sensor");
        } else if (sensorType.getSelectedItem() == "Smell Sensor") {
            setTitle("Add a smell sensor");
        } else if (sensorType.getSelectedItem() == "Tile Set") {
            setTitle("Add a grid of tile sensors");
        } else if (sensorType.getSelectedItem() == "Hearing Sensor") {
            setTitle("Add a hearing sensor");
        } else if (sensorType.getSelectedItem().equals("Object Sensor")) {
            setTitle("Add an object sensor");
        } else if (sensorType.getSelectedItem().equals("Bump Sensor")) {
            setTitle("Add a bump sensor");
        }

    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        if (sensorType.getSelectedItem() == "Tile Sensor") {
            entity.addSensor(new TileSensor(entity));
        } else if (sensorType.getSelectedItem() == "Smell Sensor") {
            entity.addSensor(new SmellSensor(entity));
        } else if (sensorType.getSelectedItem() == "Tile Set") {
            entity.addSensor(new LocationSensor(entity, 0, 0, 0, 0));
        } else if (sensorType.getSelectedItem() == "Hearing Sensor") {
            entity.addSensor(new Hearing(entity));
        } else if (sensorType.getSelectedItem().equals("Object Sensor")) {
            entity.addSensor(new ObjectSensor(entity));
        } else if (sensorType.getSelectedItem().equals("Bump Sensor")) {
            entity.addSensor(new BumpSensor(entity));
        }
    }
}
