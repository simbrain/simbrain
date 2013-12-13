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

import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Panel to add a turning effector to an entity or to modify an existing one.
 *
 * @author Lam Nguyen
 *
 */
public class TurningEffectorPanel extends AbstractEffectorPanel {

    /** Text field to edit label. */
    private JTextField label = new JTextField();

    /** Text field to edit direction. */
    private JTextField direction = new JTextField();

    /** Text field to edit amount. */
    private JTextField amount = new JTextField();

    /** Entity to which a turning effector is being added. */
    private RotatingEntity entity;

    /**
     * Reference to straight movement effector. Initially null if this is a
     * creation panel.
     */
    private Turning turningEffector;

    /** If true this is a creation panel. Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * Constructor for the case where an effector is being created.
     *
     * @param entity the entity to which a straight movement effector is added.
     */
    public TurningEffectorPanel(OdorWorldEntity entity) {
        this.entity = (RotatingEntity) entity;
        isCreationPanel = true;
        addItem("Label", label);
        addItem("Turning direction", direction);
        addItem("Turning amount", amount);
        fillFieldValues();
    }

    /**
     * Constructor for the case where an effector is being edited.
     *
     * @param entity parent entity
     * @param effector effector to edit
     */
    public TurningEffectorPanel(OdorWorldEntity entity, Turning effector) {
        this.entity = (RotatingEntity) entity;
        this.turningEffector = effector;
        isCreationPanel = false;
        addItem("Label", label);
        addItem("Turning direction", direction);
        addItem("Turning amount", amount);
        fillFieldValues();
    }

    @Override
    public void commitChanges() {
        if (isCreationPanel) {
            entity.addEffector(new Turning(entity, label.getText(), Double
                    .parseDouble(direction.getText())));
        } else {
            turningEffector.setLabel(label.getText());
            turningEffector
                    .setDirection(Double.parseDouble(direction.getText()));
            turningEffector.setAmount(Double.parseDouble(amount.getText()));
        }
    }

    @Override
    protected void fillFieldValues() {
        if (isCreationPanel) {
            label.setText("" + Turning.DEFAULT_LABEL);
            direction.setText("" + Turning.DEFAULT_DIRECTION);
            amount.setText("" + Turning.DEFAULT_AMOUNT);
        } else {
            label.setText("" + turningEffector.getLabel());
            direction.setText("" + turningEffector.getDirection());
            amount.setText("" + turningEffector.getAmount());
        }
    }
}
