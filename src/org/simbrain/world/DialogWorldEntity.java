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

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.simbrain.util.ComboBoxRenderer;
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
	private ImageIcon images[];
	
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
		val_array = entity_ref.getStimulusObject().getStimulusVector();
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
		myContentPane.addItem("Image name", cbImageName);
		myContentPane.addItem("Decay function", cbDecayFunction);
		myContentPane.addItem("Dispersion", tfDispersion);
		myContentPane.addItem("Add noise", rbAddNoise);
		myContentPane.addItem("Noise level", jsNoiseLevel);
		myContentPane.addItem("Stimulus values", stimScroller);
		
        cbRenderer.setPreferredSize(new Dimension(35, 35));
		cbImageName.setRenderer(cbRenderer);
		

		setContentPane(myContentPane);
	}

	/**
	* Populate fields with current data
	*/
	public void fillFieldValues() {
		
		cbImageName.setSelectedIndex(entity_ref.getImageNameIndex(entity_ref.getImageName()));
		cbDecayFunction.setSelectedIndex(entity_ref.getStimulusObject().getDecayFunctionIndex(entity_ref.getStimulusObject().getDecayFunction()));
		tfDispersion.setText(Double.toString(entity_ref.getStimulusObject().getDispersion()));
		
		rbAddNoise.setSelected(entity_ref.getStimulusObject().isAddNoise());
		if(entity_ref.getStimulusObject().isAddNoise() == true) {
			jsNoiseLevel.setEnabled(true);
			jsNoiseLevel.setValue((int)(entity_ref.getStimulusObject().getNoiseLevel() * 100));
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
	public void getValues() {

		entity_ref.setImageName(cbImageName.getSelectedItem().toString());
		if (entity_ref instanceof Agent) {
			((Agent)entity_ref).setOrientation(((Agent)entity_ref).getOrientation());
		}
		entity_ref.getStimulusObject().setStimulusVector(val_array);
		entity_ref.getStimulusObject().setDispersion(Double.parseDouble(tfDispersion.getText()));
		entity_ref.getStimulusObject().setDecayFunction(cbDecayFunction.getSelectedItem().toString());
		
		entity_ref.getStimulusObject().setAddNoise(rbAddNoise.isSelected());
		if(rbAddNoise.isSelected()) {
			entity_ref.getStimulusObject().setNoiseLevel((double)jsNoiseLevel.getValue()/100);
		}
		
		for (int i = 0; i < entity_ref.getStimulusObject().getStimulusVector().length; i++) {
			val_array[i] = Double.parseDouble(stimulusVals[i].getText());
		}
	}

	public void actionPerformed(ActionEvent e) {
		if(rbAddNoise.isSelected()) {
			jsNoiseLevel.setEnabled(true);
		} else jsNoiseLevel.setEnabled(false);
	}
}
