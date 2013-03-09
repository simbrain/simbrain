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

import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

public class TurningEffectorPanel extends AbstractEffectorPanel {

	private JFormattedTextField label = new JFormattedTextField("Turn");

    private JFormattedTextField direction = new JFormattedTextField(0);

    private JFormattedTextField amount = new JFormattedTextField(0);

    private RotatingEntity entity;
    /**
     * Default constructor.
     */
    public TurningEffectorPanel(OdorWorldEntity entity) {
    	this.entity = (RotatingEntity) entity;
    	addItem("Label", label);
    	addItem("Turning direction", direction);
    	addItem("Turning amount", amount);
    	setVisible(true);
    }

	@Override
	public void commitChanges() {
		entity.addEffector(new Turning(entity, label.getText(), Double.parseDouble(direction.getText()))); // todo: label
	}

	public void commitChanges(Turning effector) {
		effector.setDirection(Double.parseDouble(direction.getText()));
		effector.setAmount(Double.parseDouble(amount.getText()));
	}

	public void fillFieldValues(Turning effector) {
		label.setText("" + effector.getLabel()); // Label cannot be edited. Change to Title?
		direction.setText("" + effector.getDirection());
		amount.setText("" + effector.getAmount());
	}
}
