/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.network.gui.dialogs.neuron;

import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ClippableUpdateRule;
import org.simbrain.util.widgets.TristateDropDown;

/**
 *
 * A panel containing more detailed generic information about neurons. Generally
 * speaking, this panel is not meant to exist in a dialog by itself, it is a set
 * of commonly used (hence generic) neuron value fields which is shared by
 * multiple complete dialogs.
 *
 * Values included are: Activation ceiling and floor, label, priority and
 * increment.
 *
 * @author ztosi
 *
 */
public class ExtendedNeuronInfoPanel extends JPanel {

	/** Null string. */
	public static final String NULL_STRING = "...";

	/** Upper bound field. */
	private final JTextField tfCeiling = new JTextField();

	/** Lower bound field. */
	private final JTextField tfFloor = new JTextField();

	private final TristateDropDown clipping = new TristateDropDown();

	{
		clipping.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setBoundsEnabled(clipping.getSelectedIndex() == TristateDropDown
						.getTRUE());
			}

		});
	}

	/**
	 * Label for ceiling text field. Is a class variable so that its visibility
	 * can be set alongside the visibility of the text field.
	 */
	private final JLabel ceilL = new JLabel("Ceiling: ");

	/**
	 * Label for floor text field. Is a class variable so that its visibility
	 * can be set alongside the visibility of the text field.
	 */
	private final JLabel floorL = new JLabel("Floor: ");

	/**
	 * Label for clipping field.
	 */
	private final JLabel clipL = new JLabel("Clipping: ");

	/** Increment field. */
	private final JTextField tfIncrement = new JTextField();

	/** Priority Field. */
	private final JTextField tfPriority = new JTextField();

	/** Are upper and lower bounds visible? */
	private boolean boundsVisible;

	/** Are upper and lower bounds enabled? */
	private boolean boundsEnabled;

	/** Bounds panel. */
	private final JPanel boundsPanel = new JPanel();

	/**
	 * Whether or not the neuron is clamped (i.e. will not update/change its
	 * activation once set).
	 */
	private final TristateDropDown clamped = new TristateDropDown();

	/** Parent reference so pack can be called. */
	private final Window parent;

	/** The neurons being modified. */
	private List<Neuron> neuronList;

	    /**
     * Construct the panel representing the provided neurons.
     *
     * @param neuronList list of neurons to represent.
     * @param parent parent window so pack can be called
     */
    public ExtendedNeuronInfoPanel(final List<Neuron> neuronList,
            final Window parent) {
        this.neuronList = neuronList;
        this.parent = parent;
        fillFieldValues();
        initializeLayout();
    }

	/**
	 * Lays out the panel
	 */
	private void initializeLayout() {

		BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(layout);

		GridLayout gl = new GridLayout(0, 2);
		gl.setVgap(5);

		JPanel clampP = new JPanel(gl);
		clampP.add(new JLabel("Clamped: "));
		clampP.add(clamped);
		clampP.setAlignmentX(CENTER_ALIGNMENT);
		this.add(clampP);

		this.add(Box.createVerticalStrut(5));

		boundsPanel
				.setLayout(new BoxLayout(boundsPanel, BoxLayout.Y_AXIS));
		JPanel sbp1 = new JPanel(gl);
		sbp1.add(clipL);
		sbp1.add(clipping);
		sbp1.setAlignmentX(CENTER_ALIGNMENT);
		boundsPanel.add(sbp1);
		boundsPanel.add(Box.createVerticalStrut(5));
		JPanel sbp2 = new JPanel(gl);
		sbp2.add(ceilL);
		sbp2.add(tfCeiling);
		sbp2.add(floorL);
		sbp2.add(tfFloor);
		sbp2.setAlignmentX(CENTER_ALIGNMENT);
		boundsPanel.add(sbp2);
		boundsPanel.add(Box.createVerticalStrut(5));
		boundsPanel.setAlignmentX(CENTER_ALIGNMENT);
		this.add(boundsPanel);

		JPanel subP = new JPanel(gl);
		subP.add(new JLabel("Increment: "));
		subP.add(tfIncrement);
		subP.add(new JLabel("Priority:"));
		subP.add(tfPriority);
		subP.setAlignmentX(CENTER_ALIGNMENT);
		this.add(subP);

		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	}

	/**
	 * Fills the values of the text fields based on the corresponding values of
	 * the neurons to be edited. Called before {@link #initializeLayout()}.
	 */
	public void fillFieldValues() {

		Neuron neuronRef = neuronList.get(0);
		List<NeuronUpdateRule> ruleList = Neuron.getRuleList(neuronList);
		boolean h = true;
		for (NeuronUpdateRule r : ruleList) {
			if (!(r instanceof ClippableUpdateRule)) {
				h = false;
				break;
			}
		}

		setBoundsVisible(h);

		if (boundsVisible) {

			// Clipping
			if (NetworkUtils.isConsistent(ruleList,
					ClippableUpdateRule.class, "isClipped")) {

				setBoundsEnabled(((ClippableUpdateRule) neuronRef
						.getUpdateRule()).isClipped());

				// Handle Ceiling
				if (!NetworkUtils.isConsistent(ruleList,
						NeuronUpdateRule.class, "getCeiling"))
					tfCeiling.setText(NULL_STRING);
				else
					tfCeiling.setText(Double.toString(neuronRef
							.getUpdateRule().getCeiling()));

				// Handle Floor
				if (!NetworkUtils.isConsistent(ruleList,
						NeuronUpdateRule.class, "getFloor"))
					tfFloor.setText(NULL_STRING);
				else
					tfFloor.setText(Double.toString(neuronRef
							.getUpdateRule().getFloor()));

			} else {
				clipping.setNull();
				setBoundsEnabled(false);
				tfCeiling.setText(NULL_STRING);
				tfFloor.setText(NULL_STRING);
			}
		}

		// Handle Increment
		if (!NetworkUtils.isConsistent(Neuron.getRuleList(neuronList),
				NeuronUpdateRule.class, "getIncrement"))
			tfIncrement.setText(NULL_STRING);
		else
			tfIncrement.setText(Double.toString(neuronRef.getUpdateRule()
					.getIncrement()));

		// Handle Priority
		if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
				"getUpdatePriority"))
			tfPriority.setText(NULL_STRING);
		else
			tfPriority.setText(Integer.toString(neuronRef
					.getUpdatePriority()));

		// Handle Clamped
		if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
				"isClamped"))
			clamped.setNull();
		else
			clamped.setSelected(neuronList.get(0).isClamped());

	}

	public void fillDefaultValues(NeuronUpdateRule rule) {
		setBoundsVisible(rule instanceof BoundedUpdateRule
				&& rule instanceof ClippableUpdateRule);
		if (boundsVisible) {
			tfCeiling.setText(Double.toString(rule.getCeiling()));
			tfFloor.setText(Double.toString(rule.getFloor()));
			setBoundsEnabled(((ClippableUpdateRule) rule).isClipped());
		}
		tfIncrement.setText(Double.toString(rule.getIncrement()));
		tfPriority.setText(Integer.toString(0));
	}

	/**
	 * Uses the values from text fields to alter corresponding values in the
	 * neuron(s) being edited. Called externally to apply changes.
	 */
	public void commitChanges() {
		int numNeurons = neuronList.size();

		if (boundsVisible) {
			// Clipping?
			if (!clipping.isNull()) {
				boolean clip =
						clipping.getSelectedIndex() == TristateDropDown
								.getTRUE();
				for (int i = 0; i < numNeurons; i++) {
					((ClippableUpdateRule) neuronList.get(i)
							.getUpdateRule()).setClipped(clip);
				}

				if (clip) {
					// Upper Bound
					double ceiling =
							AbstractNeuronPanel.doubleParsable(tfCeiling);
					if (!Double.isNaN(ceiling)) {
						for (int i = 0; i < numNeurons; i++) {
							((BoundedUpdateRule) neuronList.get(i)
									.getUpdateRule()).setCeiling(ceiling);
						}
					}
					// Lower Bound
					double floor =
							AbstractNeuronPanel.doubleParsable(tfFloor);
					if (!Double.isNaN(floor)) {
						for (int i = 0; i < numNeurons; i++) {
							((BoundedUpdateRule) neuronList.get(i)
									.getUpdateRule()).setFloor(floor);
						}
					}
				}

			}
		}

		// Increment
		double increment =
				AbstractNeuronPanel.doubleParsable(tfIncrement);
		if (!Double.isNaN(increment)) {
			for (int i = 0; i < numNeurons; i++) {
				neuronList.get(i).getUpdateRule().setIncrement(increment);
			}
		}

		// Priority
		double priority = AbstractNeuronPanel.doubleParsable(tfPriority);
		if (!Double.isNaN(priority)) {
			int p = (int) priority; // Cast to integer (there is no NaN value
			// for integers to use as a flag).
			for (int i = 0; i < numNeurons; i++) {
				neuronList.get(i).setUpdatePriority(p);
			}
		}

		// Clamped
		if (!clamped.isNull()) {
			boolean clamp =
					clamped.getSelectedIndex() == TristateDropDown
							.getTRUE();
			for (int i = 0; i < numNeurons; i++) {
				neuronList.get(i).setClamped(clamp);
			}
		}

	}

	/**
	 * @return the TristateDropDown menu controlling whether or not the neurons'
	 *         activation(s) are clamped
	 */
	public TristateDropDown getClamped() {
		return clamped;
	}

	public void setBoundsEnabled(boolean enabled) {
		boundsEnabled = enabled;
		int t = TristateDropDown.getTRUE();
		int f = TristateDropDown.getFALSE();
		clipping.setSelectedIndex(isBoundsEnabled() ? t : f);
		tfCeiling.setEnabled(enabled);
		tfFloor.setEnabled(enabled);
		repaint();
	}

	public void setBoundsVisible(boolean visible) {
		boundsVisible = visible;
		boundsPanel.setVisible(visible);
		repaint();
		parent.pack();
	}

	public boolean isBoundsVisible() {
		return boundsVisible;
	}

	public boolean isBoundsEnabled() {
		return boundsEnabled;
	}

}
