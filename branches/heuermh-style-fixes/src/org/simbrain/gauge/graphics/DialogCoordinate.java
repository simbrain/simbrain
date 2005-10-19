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
package org.simbrain.gauge.graphics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.simbrain.gauge.GaugePreferences;
import org.simbrain.gauge.core.Gauge;
import org.simbrain.gauge.core.ProjectCoordinate;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>DialogCoordinate</b> is a dialog box for setting the properties of the 
 * coordinate projection algorithm
 */
public class DialogCoordinate extends StandardDialog implements ActionListener{
	
	private Gauge theGauge;
	
	private JTextField firstDimField = new JTextField();
	private JTextField secondDimField= new JTextField();
	private JCheckBox autoFind = new JCheckBox();
    private JButton defaultButton = new JButton ("Restore defaults");
	
	private LabelledItemPanel myContentPane = new LabelledItemPanel();
	
	/**
	  * This method is the default constructor.
	  */
	 public DialogCoordinate(Gauge gauge)
	 {
	 	 theGauge = gauge;
		 init();
	 }

	 /**
	  * This method initialises the components on the panel.
	  */
	 private void init()
	 {
		 setTitle("Coordinate Dialog");

		fillFieldValues();
		myContentPane.setBorder(BorderFactory.createEtchedBorder());

		firstDimField.setColumns(4);
		myContentPane.addItem("First dimension to project", firstDimField);
		myContentPane.addItem("Second dimension to project", secondDimField);
		myContentPane.addItem("Automatically use most variant dimensions",
				autoFind);

        defaultButton.addActionListener(this);
        addButton(defaultButton);
		setContentPane(myContentPane);
	 }
     
     /**
      * Respond to button pressing events
      */
     public void actionPerformed(ActionEvent e) {
         
         Object o = e.getSource();
         if (o == defaultButton) {
             GaugePreferences.restoreCoordinateDefaults();
             this.returnToCurrentPrefs();
             fillFieldValues();
         }
     }
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		ProjectCoordinate gauge = (ProjectCoordinate)theGauge.getCurrentProjector();
		firstDimField.setText(Integer.toString(gauge.getHi_d1()+1));
		secondDimField.setText(Integer.toString(gauge.getHi_d2()+1));
		autoFind.setSelected(((ProjectCoordinate)theGauge.getCurrentProjector()).isAutoFind());
	 }
	 
	/**
	* Set projector values based on fields 
	*/
   public void commit() {
		((ProjectCoordinate)theGauge.getCurrentProjector()).setHi_d1(Integer.valueOf(firstDimField.getText()).intValue()-1);
		((ProjectCoordinate)theGauge.getCurrentProjector()).setHi_d2(Integer.valueOf(secondDimField.getText()).intValue()-1);
		((ProjectCoordinate)theGauge.getCurrentProjector()).setAutoFind(autoFind.isSelected());
	}

   /**
    * Restores the changed fields to their previous values
    * Used when user cancels out of the dialog to undo whatever changes were made in actionPerformed
    */
   public void returnToCurrentPrefs() {
       ((ProjectCoordinate)theGauge.getCurrentProjector()).setHi_d1(GaugePreferences.getHiDim1());
       ((ProjectCoordinate)theGauge.getCurrentProjector()).setHi_d2(GaugePreferences.getHiDim2());
       ((ProjectCoordinate)theGauge.getCurrentProjector()).setAutoFind(GaugePreferences.getAutoFind());
   }
   
   /**
    * Sets selected preferences as user defaults to be used each time program is launched
    * Called when "ok" is pressed
    *
    */
   public void setAsDefault() {
       
       GaugePreferences.setHiDim1(Integer.valueOf(firstDimField.getText()).intValue()-1);
       GaugePreferences.setHiDim2(Integer.valueOf(secondDimField.getText()).intValue()-1);
       GaugePreferences.setAutoFind(autoFind.isSelected());
   }
}
