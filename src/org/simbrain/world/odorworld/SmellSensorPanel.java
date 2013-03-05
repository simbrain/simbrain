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

import javax.swing.JFormattedTextField;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

public class SmellSensorPanel extends AbstractSensorPanel {

	private JFormattedTextField label = new JFormattedTextField("SmellSensor");

    private JFormattedTextField theta = new JFormattedTextField(Math.PI / 4);

    private JFormattedTextField radius = new JFormattedTextField(23);

    private OdorWorldEntity entity;
    /**
     * Default constructor.
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

	public void commitChanges(SmellSensor sensor) {
		sensor.setTheta(Double.parseDouble(theta.getText()));
		sensor.setRadius(Double.parseDouble(radius.getText()));
	}

	public void fillFieldValues(SmellSensor sensor) {
		theta.setText("" + sensor.getTheta());
		radius.setText("" + sensor.getRadius());
	}
}
