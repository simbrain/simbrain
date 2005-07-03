/*
 * Part of HiSee, a tool for visualizing high dimensional datasets
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
package org.simbrain.gauge.graphics;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JTextField;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>DialogGraphics</b> is a dialog box for setting the properties of the 
 * GUI.
 */
public class DialogGraphics extends StandardDialog implements ActionListener{
	
	private GaugePanel theGaugePanel;
	
	private JCheckBox colorPoints = new JCheckBox();
	private JCheckBox showError = new JCheckBox();
	private JCheckBox showStatus = new JCheckBox();
	private JTextField minimumPointSize = new JTextField();
	private JTextField numberIterations = new JTextField();
	private JTextField scale = new JTextField();
	private JButton colorButton = new JButton("Set");
	private LabelledItemPanel myContentPane = new LabelledItemPanel();
	
	/**
	  * This method is the default constructor.
	  */
	 public DialogGraphics(GaugePanel gp)
	 {
		theGaugePanel = gp;
		 checkDatasets();
	 }

	 /**
	  * This method initialises the components on the panel.
	  */
	 private void checkDatasets()
	 {
		 setTitle("Graphics Dialog");

		 fillFieldValues();
		 myContentPane.setBorder(BorderFactory.createEtchedBorder());
		numberIterations.setColumns(3);
		
		colorButton.setActionCommand("changeColor");
		colorButton.addActionListener(this);

		myContentPane.addItem("Show Error ", showError);
		myContentPane.addItem("Show the Status Bar", showStatus);
		myContentPane.addItem("Color the data points", colorPoints);
		myContentPane.addItem("Minimum Point Size", minimumPointSize);
		myContentPane.addItem("Number of iterations between graphics updates", numberIterations);
		myContentPane.addItem("Margin size", scale);
		myContentPane.addItem("Background Color", colorButton);

		 
		 
		 setContentPane(myContentPane);
	 }
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		colorPoints.setSelected(theGaugePanel.isColorMode());
		showError.setSelected(theGaugePanel.isShowError());
		showStatus.setSelected(theGaugePanel.isShowStatus());
		minimumPointSize.setText(Double.toString(theGaugePanel.getGauge().getGp().getMinimumPointSize()));		
		numberIterations.setText(Integer.toString(theGaugePanel.getNumIterationsBetweenUpdate()));		
		scale.setText(Double.toString(theGaugePanel.getScale()));	
	 }
	 
	/**
	* Set projector values based on fields 
	*/
   public void getValues() {
		theGaugePanel.setColorMode(colorPoints.isSelected());
		theGaugePanel.setShowError(showError.isSelected());
		theGaugePanel.setShowStatus(showStatus.isSelected());
   		theGaugePanel.getGauge().getGp().setMinimumPointSize(Double.valueOf(minimumPointSize.getText()).doubleValue());
		theGaugePanel.setNumIterationsBetweenUpdate(Integer.valueOf(numberIterations.getText()).intValue());
		theGaugePanel.setScale(Double.valueOf(scale.getText()).doubleValue());
   }
   
   private Color getColor() {
        JColorChooser colorChooser = new JColorChooser();
        Color theColor = JColorChooser.showDialog(this, "Choose Color", Color.BLACK);
        colorChooser.setLocation(200, 200); //Set location of color chooser
        return theColor;
  }

   public void actionPerformed(ActionEvent e){
       
       String cmd = e.getActionCommand();
       if(cmd.equals("changeColor")){
		    Color theColor = getColor();
		    theGaugePanel.setBackgroundColor(theColor);
		}
   }

}
