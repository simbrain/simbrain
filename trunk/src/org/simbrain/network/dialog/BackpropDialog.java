/*
 * Part of HDV (High-Dimensional-Visualizer), a tool for visualizing high
 * dimensional datasets.
 * 
 * Copyright (C) 2004 Scott Hotton <http://www.math.smith.edu/~zeno/> and 
 * Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.dialog;

import javax.swing.JTextField;

import org.simbrain.network.NetworkPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;


/**
 * <b>DialogNetwork</b> is a dialog box for setting the properties of the 
 * Network GUI.
 */
public class BackpropDialog extends StandardDialog {

	private LabelledItemPanel mainPanel = new LabelledItemPanel();
	
	private JTextField numberOfInputUnits = new JTextField();
	private JTextField numberOfOutputUnits = new JTextField();
	private JTextField numberOfHiddenUnits = new JTextField();
	
	/**
	  * This method is the default constructor.
	  */
	 public BackpropDialog(NetworkPanel np) 
	 {
	 	init();
	 }

	 /**
	  * This method initialises the components on the panel.
	  */
	 private void init()
	 {
	 	
	 	//Initialize Dialog
		setTitle("New Backprop Network");
		//fillFieldValues();
		this.setLocation(500, 0); //Sets location of network dialog

		numberOfHiddenUnits.setColumns(3);
		
		//Set up grapics panel
		mainPanel.addItem("Number of Input Units", numberOfInputUnits);
		mainPanel.addItem("Number of Hidden Units", numberOfHiddenUnits);
		mainPanel.addItem("Number of Output Units", numberOfOutputUnits);

		setContentPane(mainPanel);

	 }
		
	 
	 /**
	 * Populate fields with current data
	 */
	 public void fillFieldValues() {
	}
   
   public int getNumInputs() {
   	return Integer.parseInt(numberOfInputUnits.getText());
   }

   public int getNumHidden() {
   	return Integer.parseInt(numberOfHiddenUnits.getText());
   }
   public int getNumOutputs() {
   	return Integer.parseInt(numberOfOutputUnits.getText());
   }


  

}
