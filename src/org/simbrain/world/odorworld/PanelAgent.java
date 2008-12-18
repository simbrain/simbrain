/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.odorworld;

import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.world.odorworld.entities.MovingEntity;


/**
 * <b>PanelAgent</b> is a panel used to adjust the "detectors"  of creature entities in the world.
 */
public class PanelAgent extends LabelledItemPanel {
    
    private static final long serialVersionUID = 1L;
    
    /** Entity referenced. */
    private MovingEntity entityRef = null;
    /** Whisker angle field. */
    private JTextField tfWhiskerAngle = new JTextField();
    /** Whisker lenght field. */
    private JTextField tfWhiskerLength = new JTextField();
    /** Turn increment field. */
    private JTextField tfTurnIncrement = new JTextField();
    /** Straight movement increment field. */
    private JTextField tfStraightMovementIncrement = new JTextField();
    /** Half circle degrees. */
    private final int halfCircleDeg = 180;

    /**
     * Create and populate creature panel.
     *
     * @param we reference to the creature entity whoes detection  parameters are being adjusted
     */
    public PanelAgent(final MovingEntity we) {
        entityRef = we;

        fillFieldValues();

        this.addItem("Whisker angle", this.tfWhiskerAngle);
        this.addItem("Whisker length", this.tfWhiskerLength);
        this.addItem("Turn Increment", this.tfTurnIncrement);
        this.addItem("Straight movement increment", this.tfStraightMovementIncrement);
    }

    // TODO!
    
    
    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
//        tfWhiskerAngle.setText(Double.toString((OdorWorldAgent.WHISKER_ANGLE * halfCircleDeg) / Math.PI));
//        tfWhiskerLength.setText(Double.toString(entityRef.getWhiskerLength()));
//        tfTurnIncrement.setText(Double.toString(entityRef.getTurnIncrement()));
//        tfStraightMovementIncrement.setText(Double.toString(entityRef.getMovementIncrement()));
    }

    /**
     * Set values based on fields.
     */
    public void commitChanges() {
//        entityRef.setWhiskerAngle((Double.parseDouble(tfWhiskerAngle.getText()) * Math.PI) / halfCircleDeg);
//        entityRef.setWhiskerLength(Double.parseDouble(tfWhiskerLength.getText()));
//        entityRef.setTurnIncrement(Double.parseDouble(tfTurnIncrement.getText()));
//        entityRef.setMovementIncrement(Double.parseDouble(tfStraightMovementIncrement.getText()));
    }
}
