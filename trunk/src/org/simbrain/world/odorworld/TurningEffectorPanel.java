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

import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Panel to add a turning effector to an entity.
 *
 * @author Lam Nguyen
 *
 */
public class TurningEffectorPanel extends AbstractEffectorPanel {

    /** Text field to edit label. */
    private JTextField label = new JTextField("Turn");

    /** Text field to edit direction. */
    private JTextField direction = new JTextField("" + 0);

    /** Text field to edit amount. */
    private JTextField amount = new JTextField("" + 0);

    /** Entity to which a turning effector is being added. */
    private RotatingEntity entity;

    /**
     * Default constructor.
     *
     * @param entity the entity to which a turning effector is added.
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

    /** Save changes to an edited turning effector. */
    public void commitChanges(Turning effector) {
        effector.setLabel(label.getText());
        effector.setDirection(Double.parseDouble(direction.getText()));
        effector.setAmount(Double.parseDouble(amount.getText()));
    }

    /** Fill in appropriate text fields when turning effector is being modified. */
    public void fillFieldValues(Turning effector) {
        label.setText("" + effector.getLabel());
        direction.setText("" + effector.getDirection());
        amount.setText("" + effector.getAmount());
    }
}
