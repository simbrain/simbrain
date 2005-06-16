/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;
import javax.swing.JCheckBox;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;


	
/**
 * <b>DialogWorld</b> is used to set the enivronment's parameters, 
 * in particular, the way stimuli are constructed to be sent the network, and the way 
 * outputs from the network are expressed in the world.
 */
public class DialogOdorWorld extends StandardDialog implements ActionListener {


	private OdorWorld theWorld;
	private LabelledItemPanel myContentPane = new LabelledItemPanel();
	private JTextField worldWidth = new JTextField();
	private JTextField worldHeight = new JTextField();
	private JCheckBox initiateMovement = new JCheckBox();
	private JCheckBox inhibitMovement = new JCheckBox();
	private JCheckBox useLocalBounds = new JCheckBox();
	private JCheckBox updateDrag = new JCheckBox();
	
		
	public DialogOdorWorld(OdorWorld wp)
	{
		theWorld = wp;
		init();
	}

	/**
	 * This method initialises the components on the panel.
	 */
	private void init()
	{
	   setTitle("World Dialog");
		
	   fillFieldValues();
	   
	   worldWidth.setColumns(5);
	  
	   myContentPane.addItem("World Width", worldWidth);
	   myContentPane.addItem("World Height", worldHeight);
	   myContentPane.addItem("Moving objects initiates creature movement", initiateMovement);
	   myContentPane.addItem("Objects block movement", inhibitMovement);
	   myContentPane.addItem("Enable boundaries (if not, agents wrap around)", useLocalBounds);		 
	   myContentPane.addItem("Update network while dragging objects", updateDrag);		  

	   setContentPane(myContentPane);


	}
	 
	/**
	* Populate fields with current data
	*/
   public void fillFieldValues() {
       worldWidth.setText(Integer.toString(theWorld.getWorldWidth()));
       worldHeight.setText(Integer.toString(theWorld.getWorldHeight()));
   	   updateDrag.setSelected(theWorld.isUpdateWhileDragging());
   	   useLocalBounds.setSelected(theWorld.getUseLocalBounds());
   	   initiateMovement.setSelected((theWorld.getObjectDraggingInitiatesMovement()));
   	   inhibitMovement.setSelected(theWorld.isObjectInhibitsMovement());
   	   
   	   
	}
	 
   /**
   * Set projector values based on fields 
   */
  public void getValues() {
      theWorld.setWorldWidth(Integer.parseInt(worldWidth.getText()));
      theWorld.setWorldHeight(Integer.parseInt(worldHeight.getText()));
      theWorld.resize();
      theWorld.setUseLocalBounds(useLocalBounds.isSelected());
      theWorld.setUpdateWhileDragging(updateDrag.isSelected());
      theWorld.setObjectDraggingInitiatesMovement(initiateMovement.isSelected());
      theWorld.setObjectInhibitsMovement(inhibitMovement.isSelected());
  }

  public void actionPerformed(ActionEvent e) {
  }
}
