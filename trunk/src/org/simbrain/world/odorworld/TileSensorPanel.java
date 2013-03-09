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
import org.simbrain.world.odorworld.sensors.TileSensor;

public class TileSensorPanel extends AbstractSensorPanel {

    private JFormattedTextField x = new JFormattedTextField(0);

    private JFormattedTextField y = new JFormattedTextField(0);

    private JFormattedTextField width = new JFormattedTextField(20);

    private JFormattedTextField height = new JFormattedTextField(20);

    private OdorWorldEntity entity;

    /**
     * Default constructor.
     */
    public TileSensorPanel(final OdorWorldEntity entity) {
    	this.entity = entity;
    	addItem("X", x);
    	addItem("Y", y);
    	addItem("Width", width);
    	addItem("Height", height);
    }

    @Override
	public void commitChanges() {
		entity.addSensor(new TileSensor(entity, Integer.parseInt(x.getText()), Integer.parseInt(y.getText()), Integer.parseInt(width.getText()), Integer.parseInt(height.getText())));

	}

	public void fillFieldValues(TileSensor sensor) {
		x.setText("" + Integer.toString(sensor.getX()));
		y.setText("" + Integer.toString(sensor.getY()));
		width.setText("" + Integer.toString(sensor.getWidth()));
		height.setText("" + Integer.toString(sensor.getHeight()));
	}

	public void commitChanges(TileSensor sensor) {
		sensor.setX(Integer.parseInt(x.getText()));
		sensor.setY(Integer.parseInt(y.getText()));
		sensor.setWidth(Integer.parseInt(width.getText()));
		sensor.setHeight(Integer.parseInt(height.getText()));
        sensor.getParent().getParentWorld()
                .fireEntityChanged(sensor.getParent());
	}
}
