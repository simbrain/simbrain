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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.gauge.GaugePreferences;
import org.simbrain.network.MouseEventHandler;
import org.simbrain.network.NetworkPreferences;
import org.simbrain.network.SelectionHandle;
import org.simbrain.network.pnodes.PNodeLine;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.network.pnodes.PNodeWeight;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>DialogGraphics</b> is a dialog box for setting the properties of the 
 * GUI.
 */
public class DialogGraphics extends StandardDialog implements ActionListener{
	
	private GaugePanel theGaugePanel;
	
	private String[] list = {"Background", "Hot Point", "Points"};
	
	private JCheckBox colorPoints = new JCheckBox();
	private JCheckBox showError = new JCheckBox();
	private JCheckBox showStatus = new JCheckBox();
	private JTextField pointSize = new JTextField();
	private JTextField numberIterations = new JTextField();
	private JComboBox cbChangeColor = new JComboBox(list);
	private JButton setButton = new JButton("Set");
	private LabelledItemPanel myContentPane = new LabelledItemPanel();
	private JPanel colorPanel = new JPanel();
	private JPanel colorIndicator = new JPanel();
	
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
		
		setButton.setActionCommand("changeColor");
		setButton.addActionListener(this);
		
		cbChangeColor.addActionListener(this);
		cbChangeColor.setActionCommand("moveSelector");
		colorPanel.add(cbChangeColor);
		colorIndicator.setSize(20,20);
		colorPanel.add(colorIndicator);
		colorPanel.add(setButton);
		setIndicatorColor();
		

		myContentPane.addItem("Show Error ", showError);
		myContentPane.addItem("Show the Status Bar", showStatus);
		myContentPane.addItem("Color the data points", colorPoints);
		myContentPane.addItem("Point Size", pointSize);
		myContentPane.addItem("Number of iterations between graphics updates", numberIterations);
		myContentPane.addItem("Change Colors", colorPanel);		 
		 
		setContentPane(myContentPane);
	 }
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		colorPoints.setSelected(theGaugePanel.isColorMode());
		showError.setSelected(theGaugePanel.isShowError());
		showStatus.setSelected(theGaugePanel.isShowStatus());
		pointSize.setText(Double.toString(theGaugePanel.getPointSize()));		
		numberIterations.setText(Integer.toString(theGaugePanel.getNumIterationsBetweenUpdate()));		
	 }
	 
	/**
	* Set projector values based on fields 
	*/
   public void commit() {
		theGaugePanel.updateColors(colorPoints.isSelected());
		theGaugePanel.setShowError(showError.isSelected());
		theGaugePanel.setShowStatus(showStatus.isSelected());
   		theGaugePanel.setPointSize(Double.valueOf(pointSize.getText()).doubleValue());
		theGaugePanel.setNumIterationsBetweenUpdate(Integer.valueOf(numberIterations.getText()).intValue());
		theGaugePanel.update();
   }
   

   /**
    * For dialog behavior that require immediate results
    */
   public void actionPerformed(ActionEvent e){
       
   	   if (e.getActionCommand().equals("changeColor")) {   	   	
	       Color theColor = getColor();
	       switch(cbChangeColor.getSelectedIndex()){
	       		case 0:
	       		    if(theColor != null){
	       		        theGaugePanel.setBackgroundColor(theColor.getRGB());
	       		    }
	       		    break;
	   		    case 1:
	   		        if(theColor != null){
	   		        		theGaugePanel.setHotColor(theColor.getRGB());
	   		        }
	   		        break;
		        case 2:
		            if(theColor != null){
		                theGaugePanel.setDefaultColor(theColor.getRGB());
		            }
		            break;
	       };
	       setIndicatorColor();
   	   } else if (e.getActionCommand().equals("moveSelector")) {
   	   		setIndicatorColor();
   	   }
   }
   
   /**
    * Restores the changed fields to their previous values
    * Used when user cancels out of the dialog to undo whatever changes were made in actionPerformed
    */
   public void returnToCurrentPrefs() {
	   theGaugePanel.setBackgroundColor(GaugePreferences.getBackgroundColor());
	   theGaugePanel.setHotColor(GaugePreferences.getHotColor());
	   theGaugePanel.setDefaultColor(GaugePreferences.getDefaultColor());
       setIndicatorColor();
   }
   
   /**
    * Sets selected preferences as user defaults to be used each time program is launched
    * Called when "ok" is pressed
    *
    */
   public void setAsDefault() {
       GaugePreferences.setBackgroundColor(theGaugePanel.getBackgroundColor());
       GaugePreferences.setHotColor(theGaugePanel.getHotColor());
       GaugePreferences.setDefaultColor(theGaugePanel.getDefaultColor());
   }

   
   
   /**
    * Set the color indicator based on the current selection 
    * in the combo box
    */
   private void setIndicatorColor() {
		switch (cbChangeColor.getSelectedIndex()) {
		case 0:
			colorIndicator.setBackground(theGaugePanel.getBackground());
			break;
		case 1:
			colorIndicator.setBackground(new Color(theGaugePanel.getHotColor()));
			break;
		case 2:
			colorIndicator.setBackground(new Color(theGaugePanel.getDefaultColor()));
			break;
		};
	}
   
   private Color getColor() {
       JColorChooser colorChooser = new JColorChooser();
       Color theColor = JColorChooser.showDialog(this, "Choose Color", Color.BLACK);
       colorChooser.setLocation(200, 200); //Set location of color chooser
       return theColor;
 }

}
