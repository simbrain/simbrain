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
package org.simbrain.network;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.*;

import java.awt.Graphics;
import java.awt.Color;

import javax.swing.*;


import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>DialogNetwork</b> is a dialog box for setting the properties of the 
 * Network GUI.
 */
public class DialogNetwork extends StandardDialog implements ActionListener, ChangeListener {

	
	private NetworkPanel netPanel;
	
	private JTabbedPane tabbedPane = new JTabbedPane();
	private JPanel tabGraphics = new JPanel();
	private JPanel tabLogic = new JPanel();
	private LabelledItemPanel graphicsPanel = new LabelledItemPanel();
	private LabelledItemPanel logicPanel = new LabelledItemPanel();
	
	private JButton backgroundColor = new JButton("Set");
	private JButton lineColor = new JButton("Set");
	private JButton nodeHotButton = new JButton("Hot");
	private JButton nodeCoolButton = new JButton("Cool");
	private JButton weightExcitatoryButton = new JButton("Excite");
	private JButton weightInhibitoryButton = new JButton("Inhibit");
	
	private JSlider weightSizeMax = new JSlider(JSlider.HORIZONTAL,5, 50, 10);
	private JSlider weightSizeMin = new JSlider(JSlider.HORIZONTAL,5, 50, 10);
	
	private JCheckBox showWeightValues = new JCheckBox();
	private JTextField precision = new JTextField();
	private JCheckBox isRounding= new JCheckBox();

	/**
	  * This method is the default constructor.
	  */
	 public DialogNetwork(NetworkPanel np)
	 {
	 	 netPanel = np;
		 init();
	 }

	 /**
	  * This method initialises the components on the panel.
	  */
	 private void init()
	 {
	 	//Initialize Dialog
		setTitle("Network Dialog");
		fillFieldValues();
		checkRounding();
		graphicsPanel.setBorder(BorderFactory.createEtchedBorder());
		precision.setColumns(3);
		this.setLocation(500, 0); //Sets location of network dialog
		
		//Set up sliders
		weightSizeMax.setMajorTickSpacing(25);
		weightSizeMax.setPaintTicks(true);
		weightSizeMax.setPaintLabels(true); 
		weightSizeMin.setMajorTickSpacing(25);
		weightSizeMin.setPaintTicks(true);
		weightSizeMin.setPaintLabels(true);
		
		//Add Action Listeners
		backgroundColor.addActionListener(this);
		isRounding.addActionListener(this);
		lineColor.addActionListener(this);
		nodeHotButton.addActionListener(this);
		nodeCoolButton.addActionListener(this);
		weightExcitatoryButton.addActionListener(this);
		weightInhibitoryButton.addActionListener(this);
		weightSizeMax.addChangeListener(this);
		weightSizeMin.addChangeListener(this);
		showWeightValues.addActionListener(this);

		//Set up grapics panel
		graphicsPanel.addItem("Set background color", backgroundColor);
		graphicsPanel.addItem("Set line color", lineColor);
		graphicsPanel.addItem("Set hot node color", nodeHotButton);
		graphicsPanel.addItem("Set cool node color", nodeCoolButton);
		graphicsPanel.addItem("Set excitetory weight color", weightExcitatoryButton);		
		graphicsPanel.addItem("Set inhibitory weight color", weightInhibitoryButton);
		graphicsPanel.addItem("Weight size Max", weightSizeMax);
		graphicsPanel.addItem("Weight size Min", weightSizeMin);
		graphicsPanel.addItem("Show weight values", showWeightValues);
		
		//Set up logic panel
		logicPanel.addItem("Round off neuron values", isRounding);
		logicPanel.addItem("Precision of round-off", precision);	
		
		//Set up tab panels
		tabGraphics.add(graphicsPanel);
		tabLogic.add(logicPanel);
		tabbedPane.addTab("Graphics", tabGraphics);
		tabbedPane.addTab("Logic", tabLogic);
		 
		setContentPane(tabbedPane);
	 }
	 
	 /**
	 * Populate fields with current data
	 */
	public void fillFieldValues() {
		precision.setText(Integer.toString(netPanel.getNetwork().getPrecision()));
		isRounding.setSelected(netPanel.getNetwork().isRoundingOff());
	 }
	 
	/**
	* Set projector values based on fields 
	*/
   public void getValues() {
   		netPanel.getNetwork().setRoundingOff(isRounding.isSelected());
		netPanel.getNetwork().setPrecision(Integer.valueOf(precision.getText()).intValue());
   }
   
   /**
    * Respond to button pressing events
    */
   public void actionPerformed(ActionEvent e) {
   	
   		Object o = e.getSource(); 
   	
	   	if (o == isRounding) {
	   		checkRounding();
	   	} else if (o == backgroundColor) {	
	   		Color theColor = getColor();
	   		if (theColor != null) {
				netPanel.setBackgroundColor(theColor);
	   		}
		} else if (o == lineColor){
	   		Color theColor = getColor();
	   		if (theColor != null) {
				PNodeLine.setLineColor(theColor);
	   		}			
	   		netPanel.resetGraphics();

		} else if (o == nodeHotButton){
	   		System.out.println("Hot Node Color");
		} else if (o == nodeCoolButton){
			System.out.println("Cool Node Color");
		} else if (o == weightExcitatoryButton){
			Color theColor = getColor();
	   		if (theColor != null) {
				PNodeWeight.setExcitatoryColor(theColor);
	   		}			
	   		netPanel.renderObjects();

		} else if (o == weightInhibitoryButton){
			Color theColor = getColor();
	   		if (theColor != null) {
				PNodeWeight.setInhibitoryColor(theColor);
	   		}	
	   		netPanel.renderObjects();

		} else if (o == showWeightValues){
			System.out.println("Show Weight Values");
		}
			
		}
   
   /**
	* Show the color pallette and get a color
	* 
	* @return selected color
	*/
   public Color getColor() {
	   JColorChooser colorChooser = new JColorChooser();
	   System.out.println("Test One");
	   Color theColor = JColorChooser.showDialog(this, "Choose Color", Color.BLACK);
	   colorChooser.setLocation(200, 200); //Set location of color chooser
	   System.out.println("Test Two");
	   return theColor;
   }

   
   /**
    * Enable or disable the precision field depending on state of rounding button
    *
    */
   private void checkRounding() {
	if (isRounding.isSelected() == false) {
		precision.setEnabled(false);
	} else {
		precision.setEnabled(true);
	} 
   }

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent e) {
		JSlider j = (JSlider)e.getSource();
		if (j == weightSizeMax) {
			PNodeWeight.setMaxRadius(j.getValue());
			netPanel.renderObjects();
		} else if (j == weightSizeMin) {
			PNodeWeight.setMinRadius(j.getValue());
			netPanel.renderObjects();
		} 
	}


}
