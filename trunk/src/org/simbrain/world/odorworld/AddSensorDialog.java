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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * SensorDialog is a dialog box for adding Sensors to Odor World.
 *
 * @author Lam Nguyen
 *
 */

public class AddSensorDialog extends StandardDialog implements ActionListener {

	/** String of Sensor types. */
	private String[] sensors = {"SmellSensor", "TileSensor", "Tile Set"};

	/** Entity to which sensor is being added. */
	private OdorWorldEntity entity;

	/** Select sensor type. */
	private JComboBox sensorType = new JComboBox(sensors);

	/** Panel that changes to a specific sensor panel. */
	private AbstractSensorPanel currentSensorPanel;

	/** Main dialog box. */
	private Box mainPanel = Box.createVerticalBox();

	/** Panel for setting sensor type. */
	private LabelledItemPanel typePanel = new LabelledItemPanel();

	/** Sensor Dialog add sensor constructor. */
	public AddSensorDialog(OdorWorldEntity entity) {
		this.entity = entity;
		init("Add Sensor");
	}

	/**
	 * Initialize default constructor.
	 */
	private void init(String title) {
		setTitle(title);
		sensorType.addActionListener(this);
        typePanel.addItem("Sensor Type", sensorType);
        sensorType.setSelectedItem("SmellSensor");
        ShowHelpAction helpAction = new ShowHelpAction(
                "Pages/Worlds/OdorWorld/OdorWorld.html"); // todo: put in specific page
        addButton(new JButton(helpAction));
        initPanel();
        mainPanel.add(typePanel);
        mainPanel.add(currentSensorPanel);
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
		if (sensorType.getSelectedItem() == "TileSensor") {
			clearSensorPanel();
            setTitle("Add a tile sensor");
			currentSensorPanel = new TileSensorPanel(entity);
			mainPanel.add(currentSensorPanel);
		} else if (sensorType.getSelectedItem() == "SmellSensor") {
			clearSensorPanel();
            setTitle("Add a smell sensor");
			currentSensorPanel = new SmellSensorPanel(entity);
			mainPanel.add(currentSensorPanel);
		} else if (sensorType.getSelectedItem() == "Tile Set") {
            clearSensorPanel();
            setTitle("Add a grid of tile sensors");
            currentSensorPanel = new TileSetPanel(entity);
            mainPanel.add(currentSensorPanel);
        }
		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * Remove current panel, if any.
	 */
	private void clearSensorPanel() {
		if (currentSensorPanel != null) {
			mainPanel.remove(currentSensorPanel);
		}
	}

	/**
	 *
	 * @param e Action event.
	 */
	public void actionPerformed(final ActionEvent e) {
		initPanel();
	}

	/**
	 * Called externally when the dialog is closed, to commit any changes made.
	 */
	public void commitChanges() {
		currentSensorPanel.commitChanges();
	}
}
