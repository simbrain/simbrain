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

package org.simbrain.network;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.simnet.NetworkPreferences;
import org.simnet.interfaces.*;
import org.simnet.interfaces.Neuron;

/**
 * <b>DialogNeuron</b> is used to set a PNodeNeuron's parameters, including its
 *  activation and output functions.
 */
public class DialogNeuron extends JDialog implements ActionListener {

	//Overall Panel
	private JPanel thePanel = new JPanel();

	//General stuff
	private JPanel generalPanel = new JPanel();
	private JPanel generalSettings = new JPanel();
	private JPanel generalInfo = new JPanel();
	private JTextArea generalText = new JTextArea();
	private JTextField tfActivation = new JTextField();
	private JTextField tfIncrement = new JTextField();
	private JTextField tfDecay = new JTextField();
	private JTextField tfBias = new JTextField();
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();

	//Activation Rules
	private JPanel activationPanel = new JPanel();
	private JPanel activationInfo = new JPanel();
	private JTextArea activationText = new JTextArea();
	private JPanel activationSettings = new JPanel();
	private JComboBox cbActivationRule =
		new JComboBox(ActivationRule.getList());
	private JTextField tfActivationThreshold = new JTextField();

	//Output Rules
	private JPanel outputPanel = new JPanel();
	private JPanel outputInfo = new JPanel();
	private JTextArea outputText = new JTextArea();
	private JPanel outputSettings = new JPanel();
	private JTextField tfOutputSignal = new JTextField();
	private JTextField tfOutputThreshold = new JTextField();

	//Buttons at the bottom
	private JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	private JButton btnOK = new JButton("OK");
	private JButton btnCancel = new JButton("Cancel");
	private JButton btnSetDefaults = new JButton("Set as default");

	//Misc
	private static String null_string = "...";
	private ArrayList neuron_list; // The neurons being modified

	/**
	 * Constructs a neuron dialog
	 * 
	 * @param owner reference to the network panel
	 * @param theNeuron reference to the neuron being modified
	 */
	public DialogNeuron(Frame owner, ArrayList theList) {

		//Basic setup
		super(owner);
		neuron_list = theList;
		this.getContentPane().setLayout(new BorderLayout());
		thePanel.setPreferredSize(new Dimension(490, 430));
		thePanel.setMinimumSize(new Dimension(490, 430));
		thePanel.setLayout(new BoxLayout(thePanel, BoxLayout.Y_AXIS));
		
		getRootPane().setDefaultButton(btnOK); 

		//Set title and fields depending on whether one or many weights have been selected
		String title;
		if (neuron_list.size() == 1) {
			setDialogForSingleNeuron();
			title = new String("Set Single Neuron");
		} else {
			setDialogForMultipleNeurons();
			title = new String("Set Multiple Neurons");
		}
		this.setTitle("" + title);

		//General stuff
		generalSettings.setLayout(
			new BoxLayout(generalSettings, BoxLayout.Y_AXIS));
		generalSettings.add(Box.createVerticalGlue());
		generalSettings.add(
			Utils.createRow(
				"   Activation",
				"Current activation of this neuron. Represents (roughly) firing rate of a neuron",
				tfActivation));
		generalSettings.add(Box.createRigidArea(new Dimension(0, 3)));
		generalSettings.add(
			Utils.createRow(
				"   Upper Bound",
				"Highest attainable value.",
				tfUpBound));
		generalSettings.add(Box.createRigidArea(new Dimension(0, 3)));
		generalSettings.add(
			Utils.createRow(
				"   Lower Bound",
				"Lowest attainable value.",
				tfLowBound));
		generalSettings.add(Box.createRigidArea(new Dimension(0, 3)));
		generalSettings.add(
			Utils.createRow(
				"   Increment",
				"When incrementing neuron up or down, increment by this amount.",
				tfIncrement));
		generalSettings.add(Box.createRigidArea(new Dimension(0, 3)));
		generalSettings.add(
			Utils.createRow(
				"   Decay",
				"Add or remove specified amount of activation at each time-step until activation is 0.",
				tfDecay));
		generalSettings.add(Box.createRigidArea(new Dimension(0, 3)));
		generalSettings.add(
			Utils.createRow(
				"   Bias                          ", // The extra spaces force this panel to line-up with the others
				"Add specified amount of activation to neuron at each time-step.",
				tfBias));
		generalSettings.add(Box.createVerticalGlue());

		generalInfo.setLayout(new BoxLayout(generalInfo, BoxLayout.Y_AXIS));
		generalText.setBackground(null);
		generalText.setWrapStyleWord(true);
		generalText.setLineWrap(true);
		generalText.setText(
			"General neuron parameters which are independent of the selected activation and output rules (thought those rules may use values here in their calculations).   Linger on the text-labels to the left for more info on each setting");
		generalInfo.add(generalText);

		generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.X_AXIS));
		generalPanel.setBorder(
			BorderFactory.createTitledBorder("General Settings"));
		generalPanel.add(Box.createHorizontalGlue());
		generalPanel.add(generalSettings);
		generalPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		generalPanel.add(generalInfo);
		generalPanel.add(Box.createHorizontalGlue());
		thePanel.add(generalPanel);

		//Activation Rule Stuff
		activationSettings.setLayout(
			new BoxLayout(activationSettings, BoxLayout.Y_AXIS));
		cbActivationRule.addActionListener(this);
		activationSettings.add(Box.createVerticalGlue());
		activationSettings.add(
			Utils.createRow(
				"   Activation Function",
				"Rule used to update activation at each time step.Often calls the output function of source neurons",
				cbActivationRule));
		activationSettings.add(Box.createRigidArea(new Dimension(0, 3)));
		activationSettings.add(
			Utils.createRow(
				"   Threshold",
				"Used with threshold activation function.",
				tfActivationThreshold));
		activationSettings.add(Box.createVerticalGlue());

		activationInfo.setLayout(
			new BoxLayout(activationInfo, BoxLayout.Y_AXIS));
		activationText.setWrapStyleWord(true);
		activationText.setLineWrap(true);
		activationText.setBackground(null);
		activationText.setEditable(false);
		activationText.setMargin(new Insets(3,3,3,5));
		activationInfo.add(activationText);

		activationPanel.setLayout(
			new BoxLayout(activationPanel, BoxLayout.X_AXIS));
		activationPanel.setBorder(
			BorderFactory.createTitledBorder("Activation Function"));
		activationPanel.add(Box.createHorizontalGlue());
		activationPanel.add(activationSettings);
		activationPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		activationPanel.add(activationInfo);
		activationPanel.add(Box.createHorizontalGlue());
		thePanel.add(activationPanel);

		//Bottom Stuff
		btnOK.addActionListener(this);
		btnCancel.addActionListener(this);
		btnSetDefaults.addActionListener(this);
		bottomPanel.add(btnSetDefaults);
		bottomPanel.add(btnCancel);
		bottomPanel.add(btnOK);

		//Final construction of dialog
		this.getContentPane().add("Center", thePanel);
		this.getContentPane().add("South", bottomPanel);
		setVisible(true);
		pack();
		this.repaint();

	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JComboBox) {
			
			if (((String) cbActivationRule.getSelectedItem()).equals(null_string)== false) {
				//NeuronActivationFunction tempAR = NeuronActivationFunction.getByName((String) cbActivationRule.getSelectedItem());
				//activationText.setText(tempAR.getHelp());
				
				//tfActivationThreshold.setEnabled(tempAR.usesThreshold());
	
			}
		} else {
			JButton btemp = (JButton) e.getSource();
			if (btemp == btnOK) {
				for (int i = 0; i < neuron_list.size(); i++) {
					Neuron neuron_ref = (Neuron) neuron_list.get(i);

					if (cbActivationRule.getSelectedItem().equals(null_string)
						== false) {
//						neuron_ref.setActivationFunction(
//							(String) cbActivationRule.getSelectedItem());
					}
					if (tfActivation.getText().equals(null_string) == false) {
						neuron_ref.setActivation(
							Double.parseDouble(tfActivation.getText()));
					}
					if (tfUpBound.getText().equals(null_string) == false) {
						neuron_ref.setUpperBound(
							Double.parseDouble(tfUpBound.getText()));
					}
					if (tfLowBound.getText().equals(null_string) == false) {
						neuron_ref.setLowerBound(
							Double.parseDouble(tfLowBound.getText()));
					}
					if (tfOutputSignal.getText().equals(null_string)
						== false) {
						neuron_ref.setOutputSignal(
							Double.parseDouble(tfOutputSignal.getText()));
					}
					if (tfOutputThreshold.getText().equals(null_string)
						== false) {
						neuron_ref.setOutputThreshold(
							Double.parseDouble(tfOutputThreshold.getText()));
					}
					if (tfActivationThreshold.getText().equals(null_string)
						== false) {
						neuron_ref.setActivationThreshold(
							Double.parseDouble(
								tfActivationThreshold.getText()));
					}
					if (tfIncrement.getText().equals(null_string) == false) {
						neuron_ref.setIncrement(
							Double.parseDouble(tfIncrement.getText()));
					}
					if (tfDecay.getText().equals(null_string) == false) {
						neuron_ref.setDecay(
							Double.parseDouble(tfDecay.getText()));
					}
					if (tfBias.getText().equals(null_string) == false) {
						neuron_ref.setBias(
							Double.parseDouble(tfBias.getText()));
					}

				}

				this.dispose();
			} else if (btemp == btnCancel) {
				this.dispose();
			} else if (btemp == btnSetDefaults) {

				if (cbActivationRule.getSelectedItem().equals(null_string)
					== false) {
					NetworkPreferences.setActivationFunction(
						(String) cbActivationRule.getSelectedItem());
				}
				if (tfActivation.getText().equals(null_string) == false) {
					NetworkPreferences.setActivation(
						Double.parseDouble(tfActivation.getText()));
				}
				if (tfUpBound.getText().equals(null_string) == false) {
					NetworkPreferences.setNrnUpperBound(
						Double.parseDouble(tfUpBound.getText()));

				}
				if (tfLowBound.getText().equals(null_string) == false) {
					NetworkPreferences.setNrnLowerBound(
						Double.parseDouble(tfLowBound.getText()));
				}
				if (tfOutputSignal.getText().equals(null_string) == false) {
					NetworkPreferences.setOutputSignal(
						Double.parseDouble(tfOutputSignal.getText()));
				}
				if (tfOutputThreshold.getText().equals(null_string) == false) {
					NetworkPreferences.setOutputThreshold(
						Double.parseDouble(tfOutputThreshold.getText()));
				}
				if (tfActivationThreshold.getText().equals(null_string)
					== false) {
					NetworkPreferences.setActivationThreshold(
						Double.parseDouble(tfActivationThreshold.getText()));
				}
				if (tfIncrement.getText().equals(null_string) == false) {
					NetworkPreferences.setNrnIncrement(
						Double.parseDouble(tfIncrement.getText()));
				}
				if (tfDecay.getText().equals(null_string) == false) {
					NetworkPreferences.setDecay(
						Double.parseDouble(tfDecay.getText()));
				}
				if (tfBias.getText().equals(null_string) == false) {
					NetworkPreferences.setBias(
						Double.parseDouble(tfBias.getText()));
				}

				NetworkPreferences.saveAll();
			}

		}
	}

	/**
	 * Set the dialog box for the case of a single neuron. 
	 * Use that neuron's current settings	 
	 */
	private void setDialogForSingleNeuron() {

		Neuron neuron_ref = (Neuron) neuron_list.get(0);

		cbActivationRule.setSelectedItem(
			neuron_ref.getActivationFunction().getName());
		activationText.setText(neuron_ref.getActivationFunction().getHelp());
		tfActivation.setText("" + neuron_ref.getActivation());
		tfUpBound.setText("" + neuron_ref.getUpperBound());
		tfLowBound.setText("" + neuron_ref.getLowerBound());
		tfOutputSignal.setText("" + neuron_ref.getOutputSignal());
		tfOutputThreshold.setText("" + neuron_ref.getOutputThreshold());
		tfActivationThreshold.setText("" + neuron_ref.getActivationThreshold());
		tfActivationThreshold.setEnabled(neuron_ref.getActivationFunction().usesThreshold());
		tfIncrement.setText("" + neuron_ref.getIncrement());
		tfDecay.setText("" + neuron_ref.getDecay());
		tfBias.setText("" + neuron_ref.getBias());

	}

	/**
	 * Set the dialog box for the case of multiple neurons 
	 * Use the default prefernces for neurons	 
	 */
	private void setDialogForMultipleNeurons() {
		
		Neuron neuron_ref = (Neuron) neuron_list.get(0);

		if (Utils
			.isConsistent(neuron_list, Neuron.class, "getActivationFunction")) {
			cbActivationRule.setSelectedItem(
				neuron_ref.getActivationFunction().getName());
			activationText.setText(
				neuron_ref.getActivationFunction().getHelp());
		} else {
			cbActivationRule.addItem(null_string);
			cbActivationRule.setSelectedItem(null_string);
			activationText.setText(
				"Selected neurons have different activation functions");
		}
		if (Utils.isConsistent(neuron_list, Neuron.class, "getActivation")) {
			tfActivation.setText("" + neuron_ref.getActivation());
		} else {
			tfActivation.setText(null_string);
		}
		if (Utils.isConsistent(neuron_list, Neuron.class, "getUpperBound")) {
			tfUpBound.setText("" + neuron_ref.getUpperBound());
		} else {
			tfUpBound.setText(null_string);
		}
		if (Utils.isConsistent(neuron_list, Neuron.class, "getLowerBound")) {
			tfLowBound.setText("" + neuron_ref.getLowerBound());
		} else {
			tfLowBound.setText(null_string);
		}
		if (Utils.isConsistent(neuron_list, Neuron.class, "getOutputSignal")) {
			tfOutputSignal.setText("" + neuron_ref.getOutputSignal());
		} else {
			tfOutputSignal.setText(null_string);
		}
		if (Utils
			.isConsistent(neuron_list, Neuron.class, "getOutputThreshold")) {
			tfOutputThreshold.setText("" + neuron_ref.getOutputThreshold());
		} else {
			tfOutputThreshold.setText(null_string);
		}
		if (Utils
			.isConsistent(
				neuron_list,
				Neuron.class,
				"getActivationThreshold")) {
			tfActivationThreshold.setText(
				"" + neuron_ref.getActivationThreshold());
		} else {
			tfActivationThreshold.setText(null_string);
		}
		if (Utils.isConsistent(neuron_list, Neuron.class, "getIncrement")) {
			tfIncrement.setText("" + neuron_ref.getIncrement());
		} else {
			tfIncrement.setText(null_string);
		}
		if (Utils.isConsistent(neuron_list, Neuron.class, "getDecay")) {
			tfDecay.setText("" + neuron_ref.getDecay());
		} else {
			tfDecay.setText(null_string);
		}
		if (Utils.isConsistent(neuron_list, Neuron.class, "getBias")) {
			tfBias.setText("" + neuron_ref.getBias());
		} else {
			tfBias.setText(null_string);
		}

	}

}
