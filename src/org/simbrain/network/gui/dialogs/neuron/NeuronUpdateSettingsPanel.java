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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.AbstractNeuronPanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.ClampedNeuronRulePanel;
import org.simbrain.util.DropDownTriangle;
import org.simbrain.util.DropDownTriangle.UpDirection;

/**
 * 
 * A panel for setting the neuron type and changing the parameters of the
 * selected update rule.
 * 
 * @author ztosi
 * @author jyoshimi
 * 
 */
public class NeuronUpdateSettingsPanel extends JPanel {

	/** Null string. */
	public static final String NULL_STRING = "...";

	/**
	 * The default display state of the neuron panel. Currently, True, that is,
	 * by default, the neuron panel corresponding to the rule in the combo box
	 * is visible.
	 */
	private static final boolean DEFAULT_NP_DISPLAY_STATE = true;

	/** Neuron type combo box. */
	private final JComboBox<String> cbNeuronType = new JComboBox<String>(
			Neuron.getRulelist());

	/** The neurons being modified. */
	private final List<Neuron> neuronList;

	/** Neuron panel. */
	private AbstractNeuronPanel neuronPanel;

	/** For showing/hiding the neuron panel. */
	private final DropDownTriangle displayNPTriangle;

	private final Window parent;

	/**
	 * 
	 * @param neuronList
	 */
	public NeuronUpdateSettingsPanel(List<Neuron> neuronList,
			Window parent) {
		this(neuronList, parent, DEFAULT_NP_DISPLAY_STATE);
	}

	/**
	 * 
	 * @param neuronList
	 */
	public NeuronUpdateSettingsPanel(List<Neuron> neuronList,
			Window parent, boolean startingState) {
		this.neuronList = neuronList;
		this.parent = parent;
		displayNPTriangle =
				new DropDownTriangle(UpDirection.LEFT, startingState,
						"Settings", "Settings", parent);
		initNeuronType();
		initializeLayout();
		addListeners();
	}

	/**
	 * Lays out this panel.
	 * 
	 * @return
	 */
	private void initializeLayout() {

		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

		JPanel tPanel = new JPanel();
		tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.X_AXIS));
		tPanel.add(cbNeuronType);
		int horzStrut =
				neuronPanel.getPreferredSize().width
						- cbNeuronType.getPreferredSize().width
						- displayNPTriangle.getPreferredSize().width;
		horzStrut = (horzStrut > 30 && displayNPTriangle.isDown()) ?
				horzStrut : 30;
		tPanel.add(Box.createHorizontalStrut(horzStrut));

		JPanel supP = new JPanel(new FlowLayout());
		supP.add(displayNPTriangle);

		tPanel.add(supP);
		tPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		tPanel.setBorder(padding);
		this.add(tPanel);

		this.add(Box.createRigidArea(new Dimension(0, 5)));

		neuronPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		neuronPanel.setBorder(padding);
		neuronPanel.setVisible(displayNPTriangle.isDown());
		this.add(neuronPanel);

		TitledBorder tb2 =
				BorderFactory.createTitledBorder("Update Rule");
		this.setBorder(tb2);

	}

	/**
	 * Adds the listeners to this dialog.
	 */
	private void addListeners() {
		displayNPTriangle.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {

				neuronPanel.setVisible(displayNPTriangle.isDown());
				repaintPanel();
				firePropertyChange("", null, null);
				parent.pack();

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}

		});

		cbNeuronType.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				neuronPanel =
						Neuron.RULE_MAP.get(cbNeuronType
								.getSelectedItem());
				try {
					neuronPanel.fillFieldValues(Neuron
							.getRuleList(neuronList));
				} catch (Exception e) {
					neuronPanel.fillDefaultValues();
				}
				repaintPanel();
				parent.pack();
			}

		});

	}

	/**
	 * Called to repaint the panel based on changes in the to the selected
	 * neuron type.
	 */
	public void repaintPanel() {
		removeAll();
		initializeLayout();
		repaint();
	}

	/**
	 * Initialize the main neuron panel based on the type of the selected
	 * neurons.
	 */
	private void initNeuronType() {

		if (!NetworkUtils.isConsistent(neuronList, Neuron.class,
				"getType")) {
			cbNeuronType.addItem(AbstractNeuronPanel.NULL_STRING);
			cbNeuronType
					.setSelectedIndex(cbNeuronType.getItemCount() - 1);
			// Simply to serve as an empty panel
			neuronPanel = new ClampedNeuronRulePanel();
		} else {
			String neuronName =
					neuronList.get(0).getUpdateRule().getDescription();
			neuronPanel = Neuron.RULE_MAP.get(neuronName);
			neuronPanel.fillFieldValues(Neuron.getRuleList(neuronList));
			cbNeuronType.setSelectedItem(neuronName);
		}
	}

	public JComboBox<String> getCbNeuronType() {
		return cbNeuronType;
	}

	public AbstractNeuronPanel getNeuronPanel() {
		return neuronPanel;
	}

	public void setNeuronPanel(AbstractNeuronPanel neuronPanel) {
		this.neuronPanel = neuronPanel;
	}

}
