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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
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

/**
 * <b>DialogWeight</b> is used to set a PNodeWeight' parameters
 * including its learning function.
 */
public class DialogWeight extends JDialog implements ActionListener {

	//Overall Panel
	private JPanel thePanel = new JPanel();

	//General stuff
	private JPanel generalPanel = new JPanel();
	private JPanel generalSettings = new JPanel();
	private JPanel generalInfo = new JPanel();
	private JTextArea generalText = new JTextArea();
	private JTextField tfValue = new JTextField();
	private JTextField tfUpBound = new JTextField();
	private JTextField tfLowBound = new JTextField();
	private JTextField tfIncrement = new JTextField();

	//Learning stuff
	private JPanel learningPanel = new JPanel();
	private JPanel learningInfo = new JPanel();
	private JTextArea learningText = new JTextArea();
	private JPanel learningSettings = new JPanel();
	private JComboBox cbLearningRule =
		new JComboBox(LearningRule.getList());
	private JTextField tfMomentum = new JTextField();

	//Buttons at the bottom
	private JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	private JButton btnOK = new JButton("OK");
	private JButton btnCancel = new JButton("Cancel");
	private JButton btnSetDefaults = new JButton("Set as default");

	// User when multiply selected weights are diferent in some field
	private static String null_string = "...";

	//Reference to network panel
	private NetworkPanel parentPanel = null;

	//Reference to weights on the network panel
	private ArrayList weight_list;

	/**
	 * Constructs a weight dialog
	 * 
	 * @param owner reference to the frame which contains the network panel
	 * @param np reference to the network panel.  Used to update PNodeWeight when the weight values are changed
	 * @param theWeight refrence to the weight being represented
	 */
	public DialogWeight(Frame owner, NetworkPanel np, ArrayList weights) {

		//Basic setup
		super(owner);
		parentPanel = np;
		weight_list = weights;
		this.getContentPane().setLayout(new BorderLayout());
		thePanel.setPreferredSize(new Dimension(480, 260));
		thePanel.setMinimumSize(new Dimension(480, 260));
		thePanel.setLayout(new BoxLayout(thePanel, BoxLayout.Y_AXIS));
		
		getRootPane().setDefaultButton(btnOK); 

		//Set title and fields depending on whether one or many weights have been selected
		String title;
		if (weight_list.size() == 1) {
			setDialogForSingleWeight();
			title = new String("Set Single Weight");
		} else {
			setDialogForMultipleWeights();
			title = new String("Set Multiple Weights");
		}
		this.setTitle("" + title);

		//General stuff
		generalSettings.setLayout(
			new BoxLayout(generalSettings, BoxLayout.Y_AXIS));
		generalSettings.add(Box.createVerticalGlue());
		generalSettings.add(
			Utils.createRow(
				"   Value",
				"Current strength of weight(s)",
				tfValue));
		generalSettings.add(Box.createRigidArea(new Dimension(0, 3)));
		generalSettings.add(
			Utils.createRow(
				"   Upper Bound",
				"Highest value a weight can attain",
				tfUpBound));
		generalSettings.add(Box.createRigidArea(new Dimension(0, 3)));
		generalSettings.add(
			Utils.createRow(
				"   Lower Bound",
				"Lowest value a weight can attain",
				tfLowBound));
		generalSettings.add(Box.createRigidArea(new Dimension(0, 3)));
		generalSettings.add(
			Utils.createRow(
				"   Increment",
				"Degree to which to increment a weight",
				tfIncrement));
		generalSettings.add(Box.createVerticalGlue());

		generalInfo.setLayout(new BoxLayout(generalInfo, BoxLayout.Y_AXIS));
		generalText.setBackground(null);
		generalText.setWrapStyleWord(true);
		generalText.setLineWrap(true);
		generalText.setText(
			"General weight parameters which are independent of the selected learning rule.   Linger on the text-labels to the left for more info on each setting");
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

		//Learning Stuff
		learningSettings.setLayout(
			new BoxLayout(learningSettings, BoxLayout.Y_AXIS));
		cbLearningRule.addActionListener(this);
		learningSettings.add(Box.createVerticalGlue());
		learningSettings.add(
			Utils.createRow(
				"   Learning Rule",
				"The learning rule used by this /these weight(s)",
				cbLearningRule));
		learningSettings.add(Box.createRigidArea(new Dimension(0, 3)));
		learningSettings.add(
			Utils.createRow(
				"   Momentum",
				"The degree to which weights change at each time step",
				tfMomentum));
		learningSettings.add(Box.createVerticalGlue());

		learningInfo.setLayout(new BoxLayout(learningInfo, BoxLayout.Y_AXIS));
		learningText.setWrapStyleWord(true);
		learningText.setLineWrap(true);
		learningText.setBackground(null);
		learningInfo.add(learningText);

		learningPanel.setLayout(new BoxLayout(learningPanel, BoxLayout.X_AXIS));
		learningPanel.setBorder(
			BorderFactory.createTitledBorder("Learning Rule"));
		learningPanel.add(Box.createHorizontalGlue());
		learningPanel.add(learningSettings);
		learningPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		learningPanel.add(learningInfo);
		learningPanel.add(Box.createHorizontalGlue());
		thePanel.add(learningPanel);

		//Bottom Stuff
		btnOK.addActionListener(this);
		btnOK.setSelected(true);
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
//			if (((String) cbLearningRule.getSelectedItem()).equals(null_string) == false) {
//				WeightLearningRule temp =
//					WeightLearningRule.getByName(
//						(String) cbLearningRule.getSelectedItem());
//				learningText.setText(temp.getHelp());
//			}
		} else {
			JButton btemp = (JButton) e.getSource();

			if (btemp == btnOK) {
				for (int i = 0; i < weight_list.size(); i++) {

					Synapse weight_ref = ((Synapse) weight_list.get(i));

//					if (cbLearningRule.getSelectedItem().equals(null_string)
//						== false) {
//						weight_ref.setLearningRule(
//							(String) cbLearningRule.getSelectedItem());
//					}
					if (tfValue.getText().equals(null_string) == false) {
						weight_ref.setStrength(
							Double.parseDouble(tfValue.getText()));
					}
					if (tfUpBound.getText().equals(null_string) == false) {
						weight_ref.setUpperBound(
							Double.parseDouble(tfUpBound.getText()));
					}
					if (tfLowBound.getText().equals(null_string) == false) {
						weight_ref.setLowerBound(
							Double.parseDouble(tfLowBound.getText()));
					}
					if (tfIncrement.getText().equals(null_string) == false) {
						weight_ref.setIncrement(
							Double.parseDouble(tfIncrement.getText()));
					}
					if (tfMomentum.getText().equals(null_string) == false) {
						weight_ref.setMomentum(
							Double.parseDouble(tfMomentum.getText()));
					}
				}
				parentPanel.renderObjects();

				this.dispose();
			} else if (btemp == btnCancel) {
				this.dispose();
			} else if (btemp == btnSetDefaults) {

				if (cbLearningRule.getSelectedItem().equals(null_string)
					== false) {
					NetworkPreferences.setLearningRule(
						(String) cbLearningRule.getSelectedItem());

				}
				if (tfValue.getText().equals(null_string) == false) {
					NetworkPreferences.setStrength(
						Double.parseDouble(tfValue.getText()));
				}
				if (tfUpBound.getText().equals(null_string) == false) {
					NetworkPreferences.setWtUpperBound(
						Double.parseDouble(tfUpBound.getText()));
				}
				if (tfLowBound.getText().equals(null_string) == false) {
					NetworkPreferences.setWtLowerBound(
						Double.parseDouble(tfLowBound.getText()));
				}
				if (tfIncrement.getText().equals(null_string) == false) {
					NetworkPreferences.setWtIncrement(
						Double.parseDouble(tfIncrement.getText()));
				}
				if (tfMomentum.getText().equals(null_string) == false) {
					NetworkPreferences.setMomentum(
						Double.parseDouble(tfMomentum.getText()));
				}
			}

			parentPanel.renderObjects();
			NetworkPreferences.saveAll();
		}

	}

	/**
	 * Set the dialog box for the case of a single weight. 
	 * Use that weight's current settings	 
	 */
	private void setDialogForSingleWeight() {

		Synapse weight_ref = (Synapse) weight_list.get(0);
//		cbLearningRule.setSelectedItem(weight_ref.getLearningRule().getName());
//		learningText.setText(weight_ref.getLearningRule().getHelp());
		tfValue.setText("" + weight_ref.getStrength());
		tfUpBound.setText("" + weight_ref.getUpperBound());
		tfLowBound.setText("" + weight_ref.getLowerBound());
		tfIncrement.setText("" + weight_ref.getIncrement());
		tfMomentum.setText("" + weight_ref.getMomentum());
	}

	/**
	 * Set the dialog box for the case of multiple weights 
	 * Use the default prefernces for neurons.
	 * Must check for each field to see if it is consistent across selected weights.
	 * For example, if all selected weights have the same value, then show it, 
	 * otherwise just show "..."
	 */
	private void setDialogForMultipleWeights() {

		Synapse weight_ref = ((Synapse) weight_list.get(0));

		if (Utils.isConsistent(weight_list, Synapse.class, "getLearningRule")) {
//			cbLearningRule.setSelectedItem(
//				weight_ref.getLearningRule().getName());
//			learningText.setText(weight_ref.getLearningRule().getHelp());
		} else {
			cbLearningRule.addItem(null_string);
			cbLearningRule.setSelectedItem(null_string);
			cbLearningRule.setToolTipText(
				"Selected weights have different learning rules");
		}

		if (Utils.isConsistent(weight_list, Synapse.class, "getStrength")) {
			tfValue.setText("" + weight_ref.getStrength());
		} else {
			tfValue.setText(null_string);
		}

		if (Utils.isConsistent(weight_list, Synapse.class, "getUpperBound")) {
			tfUpBound.setText("" + weight_ref.getUpperBound());
		} else {
			tfUpBound.setText(null_string);
		}

		if (Utils.isConsistent(weight_list, Synapse.class, "getLowerBound")) {
			tfLowBound.setText("" + weight_ref.getLowerBound());
		} else {
			tfLowBound.setText(null_string);
		}

		if (Utils.isConsistent(weight_list, Synapse.class, "getIncrement")) {
			tfIncrement.setText("" + weight_ref.getIncrement());
		} else {
			tfIncrement.setText(null_string);
		}

		if (Utils.isConsistent(weight_list, Synapse.class, "getMomentum")) {
			tfMomentum.setText("" + weight_ref.getMomentum());
		} else {
			tfMomentum.setText(null_string);
		}

	}
}
