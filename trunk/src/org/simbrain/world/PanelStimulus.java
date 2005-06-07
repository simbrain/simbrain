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

package org.simbrain.world;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.simbrain.util.ComboBoxRenderer;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>PanelStimulus</b> is a panel used to adjust the "smell signatures" 
 * (arrays of doubles representing the effect an object has on the input nodes
 * of the network of non-creature entities in the world. 
 * 
 */

public class PanelStimulus extends LabelledItemPanel implements ActionListener{
	private ImageIcon images[];
	private WorldEntity entityRef = new WorldEntity();
	
	private double[] val_array = null;
	private JTextField[] stimulusVals = null;
	JPanel stimulusPanel = new JPanel();
	JScrollPane stimScroller = new JScrollPane(stimulusPanel);

	private JTextField tfEntityName = new JTextField();
	private JComboBox cbImageName = new JComboBox(WorldEntity.imagesRenderer());
	private ComboBoxRenderer cbRenderer = new ComboBoxRenderer();
	private JComboBox cbDecayFunction = new JComboBox(Stimulus.getDecayFunctions());
	private JTextField tfDispersion = new JTextField();
	private JSlider jsNoiseLevel = new JSlider(0,100,50);
	private JRadioButton rbAddNoise = new JRadioButton();
	
    /**
     * Create and populate the stimulus panel
     * 
     * @param we reference to the world entity whoes smell signature is being adjusted.
     */
    public PanelStimulus(WorldEntity we) {
        
        entityRef = we;

		//Handle stimulus scroller
		val_array = entityRef.getStimulus().getStimulusVector();
		stimulusVals = new JTextField[val_array.length];
		stimulusPanel.setLayout(new GridLayout(val_array.length, 1));
		stimScroller.setPreferredSize(new Dimension(100,125));

		//Turn on labels at major tick marks.
		jsNoiseLevel.setMajorTickSpacing(25);
		jsNoiseLevel.setPaintTicks(true);
		jsNoiseLevel.setPaintLabels(true); 
		
		rbAddNoise.addActionListener(this);
		
		fillFieldValues();

		//myContentPane.addItem("Entity name", entityName);
		this.addItem("Image name", cbImageName);
		this.addItem("Decay function", cbDecayFunction);
		this.addItem("Dispersion", tfDispersion);
		this.addItem("Add noise", rbAddNoise);
		this.addItem("Noise level", jsNoiseLevel);
		this.addItem("Stimulus values", stimScroller);
		
        cbRenderer.setPreferredSize(new Dimension(35, 35));
		cbImageName.setRenderer(cbRenderer);
	}
    
	/**
	* Populate fields with current data
	*/
	public void fillFieldValues() {
		
		cbImageName.setSelectedIndex(entityRef.getImageNameIndex(entityRef.getImageName()));
		cbDecayFunction.setSelectedIndex(entityRef.getStimulus().getDecayFunctionIndex(entityRef.getStimulus().getDecayFunction()));
		tfDispersion.setText(Double.toString(entityRef.getStimulus().getDispersion()));
		
		rbAddNoise.setSelected(entityRef.getStimulus().isAddNoise());
		if(entityRef.getStimulus().isAddNoise() == true) {
			jsNoiseLevel.setEnabled(true);
			jsNoiseLevel.setValue((int)(entityRef.getStimulus().getNoiseLevel() * 100));
		} else jsNoiseLevel.setEnabled(false);
		
		//Create stimulus panel
		for (int i = 0; i < val_array.length; i++) {
			stimulusVals[i] = new JTextField("" + val_array[i]);
			stimulusVals[i].setToolTipText("Index:" + (i + 1));
			stimulusPanel.add(stimulusVals[i]);
		}

	}
	
	/**
	* Set values based on fields 
	*/
	public void commitChanges() {

		entityRef.setImageName(cbImageName.getSelectedItem().toString());
		if (entityRef instanceof Agent) {
			((Agent)entityRef).setOrientation(((Agent)entityRef).getOrientation());
		}
		entityRef.getStimulus().setStimulusVector(val_array);
		entityRef.getStimulus().setDispersion(Double.parseDouble(tfDispersion.getText()));
		entityRef.getStimulus().setDecayFunction(cbDecayFunction.getSelectedItem().toString());
		
		entityRef.getStimulus().setAddNoise(rbAddNoise.isSelected());
		if(rbAddNoise.isSelected()) {
			entityRef.getStimulus().setNoiseLevel((double)jsNoiseLevel.getValue()/100);
		}
		
		for (int i = 0; i < entityRef.getStimulus().getStimulusVector().length; i++) {
			val_array[i] = Double.parseDouble(stimulusVals[i].getText());
		}
	}
    
	public void actionPerformed(ActionEvent e) {
		if(rbAddNoise.isSelected()) {
			jsNoiseLevel.setEnabled(true);
		} else jsNoiseLevel.setEnabled(false);
	}
}
