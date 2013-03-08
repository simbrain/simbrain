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

import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

public class StraightEffectorPanel extends AbstractEffectorPanel {

	private JFormattedTextField label = new JFormattedTextField("Go-Straight");

    private JFormattedTextField amount = new JFormattedTextField(0);

    private RotatingEntity entity;
    /**
     * Default constructor.
     */
    public StraightEffectorPanel(OdorWorldEntity entity) {
    	this.entity = (RotatingEntity)entity;
    	addItem("Label", label);
    	addItem("Amount", amount);
    	setVisible(true);
    }

	@Override
	public void commitChanges() {
		entity.addEffector(new StraightMovement(entity, label.getText())); // todo: label

	}

	public void commitChanges(StraightMovement effector) {
		effector.setAmount(Double.parseDouble(amount.getText()));
	}

	public void fillFieldValues(StraightMovement effector) {
		label.setText("" + effector.getLabel()); // Label cannot be edited. Change to Title?
		amount.setText("" + effector.getAmount());
	}
}
