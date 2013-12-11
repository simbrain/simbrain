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

import org.simbrain.world.odorworld.effectors.Speech;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Panel to add a straight movement effector to an entity or to modify and existing one.
 *
 * @author Lam Nguyen
 *
 */
public class StraightEffectorPanel extends AbstractEffectorPanel {

    /** Text field to edit label. */
    private JTextField label = new JTextField();

    /** Text field to edit the base movement rate. */
    private JTextField bma = new JTextField();

    /** Entity to which a straight movement effector is being added. */
    private RotatingEntity entity;

    /**
     * Reference to straight movement effector. Initially null if this is a creation panel.
     */
    private StraightMovement straightEffector;

    /** If true this is a creation panel. Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * Constructor for the case where an effector is being created.
     *
     * @param entity the entity to which a straight movement effector is added.
     */
    public StraightEffectorPanel(OdorWorldEntity entity) {
        this.entity = (RotatingEntity) entity;
        addItem("Label", label);
        addItem("Base movement amount", bma); // TODO: Better name?
        isCreationPanel = true;
        fillFieldValues();
    }

    /**
     * Constructor for the case where an effector is being edited.
     *
     * @param entity the entity to which a straight movement effector is added.
     */
    public StraightEffectorPanel(OdorWorldEntity entity, StraightMovement effector) {
        this.entity = (RotatingEntity) entity;
        this.straightEffector = effector;
        addItem("Label", label);
        addItem("Base movement amount", bma);
        isCreationPanel = false;
        fillFieldValues();
    }

    @Override
    public void commitChanges() {
        if (isCreationPanel) {
            entity.addEffector(new StraightMovement(entity, bma.getText()));
        } else {
            straightEffector.setLabel(label.getText());
            straightEffector.setScalingFactor(Double.parseDouble(bma.getText()));
        }
    }

    @Override
    public void fillFieldValues() {
        if (isCreationPanel) {
            label.setText("" + StraightMovement.DEFAULT_LABEL);
            bma.setText("" + StraightMovement.DEFAULT_SCALING_FACTOR);
        } else {
            label.setText("" + straightEffector.getLabel());
            bma.setText("" + straightEffector.getScalingFactor());
        }
    }
}
