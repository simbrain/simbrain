/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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

/**
 *<b>PanelAgent</b> is a panel used to adjust the "detectors" 
 * of creature entities in the world.
 *
 */
public class PanelAgent extends LabelledItemPanel {
	private OdorWorldAgent entityRef = null;
	
	private JTextField tfWhiskerAngle = new JTextField();
	private JTextField tfWhiskerLength = new JTextField();
	private JTextField tfTurnIncrement = new JTextField();
	private JTextField tfStraightMovementIncrement= new JTextField();
	
	/**
	 * Create and populate creature panel
	 * 
	 * @param we reference to the creature entity whoes detection 
	 * parameters are being adjusted
	 */
	public PanelAgent(OdorWorldAgent we){
	    
	    entityRef = we;
    
	    fillFieldValues();
    
	    this.addItem("Whisker angle", this.tfWhiskerAngle);
	    this.addItem("Whisker length", this.tfWhiskerLength);
	    this.addItem("Turn Increment", this.tfTurnIncrement);
	    this.addItem("Straight movement increment", this.tfStraightMovementIncrement);
    	    
	}

    /**
	 * Populate fields with current data
	 */
	public void fillFieldValues(){
    
	    tfWhiskerAngle.setText(Double.toString(entityRef.getWhiskerAngle() * 180 / Math.PI));
	    tfWhiskerLength.setText(Double.toString(entityRef.getWhiskerLength()));
	    tfTurnIncrement.setText(Double.toString(entityRef.getTurnIncrement()));
	    tfStraightMovementIncrement.setText(Double.toString(entityRef.getMovementIncrement()));
    
	}

	/**
	* Set values based on fields 
	*/
	public void commitChanges(){
    
	    entityRef.setWhiskerAngle(Double.parseDouble(tfWhiskerAngle.getText()) * Math.PI / 180);
	    entityRef.setWhiskerLength(Double.parseDouble(tfWhiskerLength.getText()));
	    entityRef.setTurnIncrement(Double.parseDouble(tfTurnIncrement.getText()));
	    entityRef.setMovementIncrement(Double.parseDouble(tfStraightMovementIncrement.getText()));
    
	}
}
