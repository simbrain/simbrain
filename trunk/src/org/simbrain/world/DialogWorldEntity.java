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

package org.simbrain.world;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>WorldEntityDialog</b> is a small dialog box used to adjust the "smell signatures" 
 * (arrays of doubles representing the effect an object has on the input nodes
 * of the network) of non-creature entities in the world.
 */
public class DialogWorldEntity extends StandardDialog implements ActionListener {

	private WorldEntity entity_ref = null;
	private LabelledItemPanel myContentPane = new LabelledItemPanel();
	
	private double[] val_array = null;
	private JTextField[] stimulusVals = null;
	JPanel stimulusPanel = new JPanel();
	JScrollPane stimScroller = new JScrollPane(stimulusPanel);

	private JTextField entityName = new JTextField();
	private JComboBox imageName = new JComboBox(WorldEntity.getImageNames());
	private JComboBox decayFunction = new JComboBox(WorldEntity.getDecayFunctions());
	private JTextField dispersion = new JTextField();
	private JSlider noiseLevel = new JSlider(0,100,50);
	private JRadioButton addNoise = new JRadioButton();

	/**
	 * Create and show the world entity dialog box
	 * 
	 * @param we reference to the world entity whose smell signature is being adjusted
	 */
	public DialogWorldEntity(WorldEntity we) {

		entity_ref = we;
		init();
	}

	/**
	 * This method initialises the components on the panel.
	 */
	private void init() {
		setTitle("Entity Dialog");

		//Handle stimulus scroller
		val_array = entity_ref.getObjectVector();
		stimulusVals = new JTextField[val_array.length];
		stimulusPanel.setLayout(new GridLayout(val_array.length, 1));
		stimScroller.setPreferredSize(new Dimension(100,125));

		//Turn on labels at major tick marks.
		noiseLevel.setMajorTickSpacing(25);
		noiseLevel.setPaintTicks(true);
		noiseLevel.setPaintLabels(true); 
		
		
		addNoise.addActionListener(this);
		
		fillFieldValues();

		//myContentPane.addItem("Entity name", entityName);
		myContentPane.addItem("Image name", imageName);
		myContentPane.addItem("Decay function", decayFunction);
		myContentPane.addItem("Dispersion", dispersion);
		myContentPane.addItem("Add noise", addNoise);
		myContentPane.addItem("Noise level", noiseLevel);
		myContentPane.addItem("Stimulus values", stimScroller);
		

		setContentPane(myContentPane);
	}

	/**
	* Populate fields with current data
	*/
	public void fillFieldValues() {
		
		entityName.setText(entity_ref.getName());
		imageName.setSelectedIndex(entity_ref.getImageNameIndex(entity_ref.getImageName()));
		decayFunction.setSelectedIndex(entity_ref.getDecayFunctionIndex(entity_ref.getDecayFunction()));
		dispersion.setText(Double.toString(entity_ref.getDispersion()));
		
		addNoise.setSelected(entity_ref.isAddNoise());
		if(entity_ref.isAddNoise() == true) {
			noiseLevel.setEnabled(true);
			noiseLevel.setValue((int)(entity_ref.getNoiseLevel() * 100));
		} else noiseLevel.setEnabled(false);
		
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
	public void getValues() {

		entity_ref.setName(entityName.getText());
		entity_ref.setImageName(imageName.getSelectedItem().toString());
		entity_ref.setObjectVector(val_array);
		entity_ref.setDispersion(Double.parseDouble(dispersion.getText()));
		entity_ref.setDecayFunction(decayFunction.getSelectedItem().toString());
		
		entity_ref.setAddNoise(addNoise.isSelected());
		if(addNoise.isSelected()) {
			entity_ref.setNoiseLevel((double)noiseLevel.getValue()/100);
		}
		
		for (int i = 0; i < entity_ref.getObjectVector().length; i++) {
			val_array[i] = Double.parseDouble(stimulusVals[i].getText());
		}
	}

	public void actionPerformed(ActionEvent e) {
		if(addNoise.isSelected()) {
			noiseLevel.setEnabled(true);
		} else noiseLevel.setEnabled(false);
	}
}
