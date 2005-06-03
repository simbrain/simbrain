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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.UserPreferences;
import org.simbrain.network.pnodes.PNodeLine;
import org.simbrain.network.pnodes.*;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>DialogNetwork</b> is a dialog box for setting the properties of the 
 * Network GUI.
 */
public class NetworkDialog extends StandardDialog implements ActionListener, ChangeListener {

        
        private NetworkPanel netPanel;
        
        private JTabbedPane tabbedPane = new JTabbedPane();
        private JPanel tabGraphics = new JPanel();
        private JPanel tabLogic = new JPanel();
        private JPanel tabMisc = new JPanel();
        private LabelledItemPanel graphicsPanel = new LabelledItemPanel();
        private LabelledItemPanel logicPanel = new LabelledItemPanel();
        private LabelledItemPanel miscPanel = new LabelledItemPanel();
        private JButton defaultButton = new JButton ("Set as default");
        
        private JButton backgroundColorButton = new JButton("Set");
        private JButton lineColorButton = new JButton("Set");
        private JButton nodeHotButton = new JButton("Set");
        private JButton nodeCoolButton = new JButton("Set");
        private JButton weightExcitatoryButton = new JButton("Set");
        private JButton weightInhibitoryButton = new JButton("Set");
        private JSlider weightSizeMaxSlider = new JSlider(JSlider.HORIZONTAL,5, 50, 10);
        private JSlider weightSizeMinSlider = new JSlider(JSlider.HORIZONTAL,5, 50, 10);
        private JTextField precisionField = new JTextField();
        private JCheckBox showWeightValuesBox = new JCheckBox();
        private JCheckBox isRoundingBox= new JCheckBox();
        private JCheckBox indentNetworkFilesBox = new JCheckBox();
        private JTextField nudgeAmountField = new JTextField();
        
        /**
          * This method is the default constructor.
          */
         public NetworkDialog(NetworkPanel np)
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
                precisionField.setColumns(3);
                nudgeAmountField.setColumns(3);
                this.setLocation(500, 0); //Sets location of network dialog
                
                //Set up sliders
                weightSizeMaxSlider.setMajorTickSpacing(25);
                weightSizeMaxSlider.setPaintTicks(true);
                weightSizeMaxSlider.setPaintLabels(true); 
                weightSizeMinSlider.setMajorTickSpacing(25);
                weightSizeMinSlider.setPaintTicks(true);
                weightSizeMinSlider.setPaintLabels(true);
                
                //Add Action Listeners
                defaultButton.addActionListener(this);
                backgroundColorButton.addActionListener(this);
                isRoundingBox.addActionListener(this);
                lineColorButton.addActionListener(this);
                nodeHotButton.addActionListener(this);
                nodeCoolButton.addActionListener(this);
                weightExcitatoryButton.addActionListener(this);
                weightInhibitoryButton.addActionListener(this);
                weightSizeMaxSlider.addChangeListener(this);
                weightSizeMinSlider.addChangeListener(this);
                showWeightValuesBox.addActionListener(this);

                //Set up grapics panel
                graphicsPanel.addItem("Set background color", backgroundColorButton);
                graphicsPanel.addItem("Set line color", lineColorButton);
                graphicsPanel.addItem("Set hot node color", nodeHotButton);
                graphicsPanel.addItem("Set cool node color", nodeCoolButton);
                graphicsPanel.addItem("Set excitatory weight color", weightExcitatoryButton);           
                graphicsPanel.addItem("Set inhibitory weight color", weightInhibitoryButton);
                graphicsPanel.addItem("Weight size Max", weightSizeMaxSlider);
                graphicsPanel.addItem("Weight size Min", weightSizeMinSlider);
                graphicsPanel.addItem("Show weight values", showWeightValuesBox);
                
                //Set up logic panel
                logicPanel.addItem("Round off neuron values", isRoundingBox);
                logicPanel.addItem("Precision of round-off", precisionField);
                
                //Set up Misc Panel
                miscPanel.addItem("Indent network files", indentNetworkFilesBox);
                miscPanel.addItem("Nudge Amount", nudgeAmountField);
                
                //Set up tab panels
                tabGraphics.add(graphicsPanel);
                tabLogic.add(logicPanel);
                tabMisc.add(miscPanel);
                tabbedPane.addTab("Graphics", tabGraphics);
                tabbedPane.addTab("Logic", tabLogic);
                tabbedPane.addTab("Misc.", tabMisc);
                addButton(defaultButton);
                setContentPane(tabbedPane);
         }

   
   /**
    * Respond to button pressing events
    */
   public void actionPerformed(ActionEvent e) {
        
                Object o = e.getSource(); 
        
                if (o == isRoundingBox) {
                        checkRounding();
                        netPanel.getNetwork().setRoundingOff(isRoundingBox.isSelected());
                } else if (o == precisionField) {
                        netPanel.getNetwork().setPrecision(Integer.valueOf(precisionField.getText()).intValue());
                } else if (o == backgroundColorButton) {        
                        Color theColor = getColor();
                        if (theColor != null) {
                                netPanel.setBackgroundColor(theColor);
                        }
                } else if (o == lineColorButton){
                        Color theColor = getColor();
                        if (theColor != null) {
                                PNodeLine.setLineColor(theColor);
                                PNodeNeuron.setEdgeColor(theColor);
                        }                       
                        netPanel.resetGraphics();

                } else if (o == nodeHotButton){
                        Color theColor = getColor();
                        if (theColor != null) {
                                PNodeNeuron.setHotColor(Color.RGBtoHSB(theColor.getRed(),theColor.getGreen(),theColor.getBlue(), null)[0]);
                        }       
                        netPanel.renderObjects();
                } else if (o == nodeCoolButton){
                        Color theColor = getColor();
                        if (theColor != null) {
                                PNodeNeuron.setCoolColor(Color.RGBtoHSB(theColor.getRed(),theColor.getGreen(),theColor.getBlue(), null)[0]);
                        }       
                        netPanel.renderObjects();
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

                } else if (o == showWeightValuesBox){
                        System.out.println("Show Weight Values");
                } else if (o == defaultButton) {
                        setAsDefault();
                }
                        
        }
         
         /**
         * Populate fields with current data
         */
        public void fillFieldValues() {
                precisionField.setText(Integer.toString(netPanel.getNetwork().getPrecision()));
                nudgeAmountField.setText(Double.toString(netPanel.getNudgeAmount()));
                isRoundingBox.setSelected(netPanel.getNetwork().isRoundingOff());
                weightSizeMaxSlider.setValue(PNodeWeight.getMaxRadius());
                weightSizeMinSlider.setValue(PNodeWeight.getMinRadius());
                indentNetworkFilesBox.setSelected(netPanel.getSerializer().isUsingTabs());
         }
         
   
        /** (non-Javadoc)
         * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
         */
        public void stateChanged(ChangeEvent e) {
                JSlider j = (JSlider)e.getSource();
                if (j == weightSizeMaxSlider) {
                        PNodeWeight.setMaxRadius(j.getValue());
                        netPanel.renderObjects();
                } else if (j == weightSizeMinSlider) {
                        PNodeWeight.setMinRadius(j.getValue());
                        netPanel.renderObjects();
                } 
        }
   
   /**
        * Show the color pallette and get a color
        * 
        * @return selected color
        */
   public Color getColor() {
           JColorChooser colorChooser = new JColorChooser();
           Color theColor = JColorChooser.showDialog(this, "Choose Color", Color.BLACK);
           colorChooser.setLocation(200, 200); //Set location of color chooser
           return theColor;
   }

   
   /**
    * Enable or disable the precision field depending on state of rounding button
    *
    */
   private void checkRounding() {
        if (isRoundingBox.isSelected() == false) {
                precisionField.setEnabled(false);
        } else {
                precisionField.setEnabled(true);
        } 
   }


        
        /**
         * Restores the changed fields to their previous values
         *
         */
        public void returnToDefault() {
                netPanel.setBackgroundColor(new Color(UserPreferences.getBackgroundColor()));
                PNodeLine.setLineColor(new Color(UserPreferences.getLineColor()));
                PNodeNeuron.setHotColor(UserPreferences.getHotColor());
                PNodeNeuron.setCoolColor(UserPreferences.getCoolColor());               
                PNodeWeight.setExcitatoryColor(new Color(UserPreferences.getExcitatoryColor()));
                PNodeWeight.setInhibitoryColor(new Color(UserPreferences.getInhibitoryColor()));
                PNodeWeight.setMaxRadius(UserPreferences.getMaxRadius());
                PNodeWeight.setMinRadius(UserPreferences.getMinRadius());
                netPanel.getNetwork().setPrecision(UserPreferences.getPrecision());
                netPanel.getNetwork().setRoundingOff(UserPreferences.getRounding());
                netPanel.resetGraphics();
                netPanel.renderObjects();
                
        }

        /**
         * Sets selected preferences as user defaults to be used each time program is launched
         *
         */
        public void setAsDefault() {
                UserPreferences.setBackgroundColor(netPanel.getBackground().getRGB());
                UserPreferences.setLineColor(PNodeLine.getLineColor().getRGB());
                UserPreferences.setHotColor(PNodeNeuron.getHotColor());
                UserPreferences.setCoolColor(PNodeNeuron.getCoolColor());
                UserPreferences.setExcitatoryColor(PNodeWeight.getExcitatoryColor().getRGB());
                UserPreferences.setInhibitoryColor(PNodeWeight.getInhibitoryColor().getRGB());
                UserPreferences.setMaxRadius(PNodeWeight.getMaxRadius());
                UserPreferences.setMinRadius(PNodeWeight.getMinRadius());
                UserPreferences.setPrecision(netPanel.getNetwork().getPrecision());
                UserPreferences.setRounding(netPanel.getNetwork().isRoundingOff());
        }
        
        public boolean isUsingIndent() {
                return indentNetworkFilesBox.isSelected();
        }
        
        /**
         * Gets the value for nudge
         * 
         * @return 
         */
        public double getNudgeAmountField() {
                return Double.valueOf(nudgeAmountField.getText()).doubleValue();
        }
}