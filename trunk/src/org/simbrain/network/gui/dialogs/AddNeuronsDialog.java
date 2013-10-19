/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.MainLayoutPanel;
import org.simbrain.network.gui.dialogs.neuron.ExtendedNeuronInfoPanel;
import org.simbrain.network.gui.dialogs.neuron.NeuronUpdateSettingsPanel;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.DropDownTriangle;
import org.simbrain.util.DropDownTriangle.UpDirection;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.TristateDropDown;

/**
 * A dialog for adding multiple neurons to the network. User can specify a
 * neuron type and a layout.
 *
 * @author ztosi
 * @author jyoshimi
 */
public class AddNeuronsDialog extends StandardDialog {

	/** Default. */
	private static final long serialVersionUID = 1L;

	/** The default layout. */
	private static final Layout DEFAULT_LAYOUT = new GridLayout();

	/** The default neuron. */
	private static final NeuronUpdateRule DEFAULT_NEURON =
			new LinearRule();

	/** Default number of neurons. */
	private static final int DEFAULT_NUM_NEURONS = 25;

	/** The network panel neurons will be added to. */
	private final NetworkPanel networkPanel;

	/** The base neuron to copy. */
	private Neuron baseNeuron;

	/** Item panel where options will be displayed. */
	private Box addNeuronsPanel = Box.createVerticalBox();

	/** Text field where desired number of neurons is entered. */
	private JTextField numNeurons = new JTextField(""
			+ DEFAULT_NUM_NEURONS);

	/** A triangle widget for displaying/hiding extra neuron settings. */
	private DropDownTriangle extraDataTriangle = new DropDownTriangle(
			UpDirection.LEFT, false, "More", "Less", this);

	/** A panel for editing more neuron variables. */
	private ExtendedNeuronInfoPanel moreSettingsPanel;

	/** Button allowing selection of type of neuron to add. **/
	private NeuronUpdateSettingsPanel selectNeuronType;

	/** A panel where layout settings can be edited. */
	private MainLayoutPanel selectLayout;

	/**
	 * A panel for editing whether or not the neurons will be added as a group.
	 */
	private NeuronGroupPanel groupPanel;

	/** A List of the neurons being added to the network. */
	private final List<Neuron> addedNeurons = new ArrayList<Neuron>();

	/**
	 * Constructs the dialog.
	 *
	 * @param networkPanel
	 *            the panel the neurons are being added to.
	 */
	public AddNeuronsDialog(final NetworkPanel networkPanel) {
		this.networkPanel = networkPanel;
		baseNeuron =
				new Neuron(networkPanel.getNetwork(), DEFAULT_NEURON);
		networkPanel.clearSelection();
		init();
		addListeners();

	}

	/**
	 * Initializes the add neurons panel with default settings.
	 */
	private void init() {

		setTitle("Add Neurons...");

		JPanel topPanel = new JPanel(new BorderLayout());
		JPanel basicsPanel = new JPanel(new GridBagLayout());
		basicsPanel
				.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 0, 0, 0);
		gbc.weightx = 0.8;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		basicsPanel.add(new JLabel("Number of Neurons:"), gbc);

		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(5, 3, 0, 0);
		gbc.weightx = 0.2;
		gbc.gridx = 1;
		basicsPanel.add(numNeurons, gbc);

		gbc.gridwidth = 1;
		int lgap = extraDataTriangle.isDown() ? 10 : 0;
		gbc.insets = new Insets(10, 5, lgap, 5);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 0.2;
		gbc.gridwidth = 2;
		basicsPanel.add(extraDataTriangle, gbc);
		topPanel.add(basicsPanel, BorderLayout.NORTH);
		gbc.gridwidth = 1;

		moreSettingsPanel =
				new ExtendedNeuronInfoPanel(
						Collections.singletonList(baseNeuron));
		moreSettingsPanel.setAlignmentX(CENTER_ALIGNMENT);
		moreSettingsPanel.setVisible(extraDataTriangle.isDown());
		topPanel.add(moreSettingsPanel, BorderLayout.SOUTH);

		topPanel.setAlignmentX(CENTER_ALIGNMENT);
		topPanel.setBorder(BorderFactory.createTitledBorder("Basic"));
		addNeuronsPanel.add(topPanel);

		addNeuronsPanel.add(Box.createVerticalStrut(10));

		selectNeuronType =
				new NeuronUpdateSettingsPanel(
						Collections.singletonList(baseNeuron), this, true);
		selectNeuronType.setAlignmentX(CENTER_ALIGNMENT);
		addNeuronsPanel.add(selectNeuronType);

		addNeuronsPanel.add(Box.createVerticalStrut(10));

		selectLayout =
				new MainLayoutPanel(DEFAULT_LAYOUT.getDescription(),
						true, this);
		selectLayout.setAlignmentX(CENTER_ALIGNMENT);
		addNeuronsPanel.add(selectLayout);

		addNeuronsPanel.add(Box.createVerticalStrut(10));
		groupPanel = new NeuronGroupPanel(networkPanel);
		groupPanel.setAlignmentX(CENTER_ALIGNMENT);
		addNeuronsPanel.add(groupPanel);

		setContentPane(addNeuronsPanel);
	}

	/**
	 * Set buttons' action listeners.
	 */
	private void addListeners() {

		extraDataTriangle.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				moreSettingsPanel.setVisible(extraDataTriangle.isDown());
				pack();
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

	}

	/**
	 * Adds the neurons to the panel.
	 */
	private void addNeurons() {
		int number = Integer.parseInt(numNeurons.getText());

		Network net = networkPanel.getNetwork();
		for (int i = 0; i < number; i++) {
			addedNeurons.add(new Neuron(net, baseNeuron));
		}

		networkPanel.addNeuronsToPanel(addedNeurons,
				selectLayout.getCurrentLayout());

	}

	/**
	 * Adds the neurons to the panel as a group.
	 */
	private void addGroup() {
		addNeurons();
		NeuronGroup ng = groupPanel.generateNeuronGroup();
		if (ng != null) {
			networkPanel.getNetwork().transferNeuronsToGroup(
					addedNeurons, ng);
			networkPanel.getNetwork().addGroup(ng);
			networkPanel.repaint();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void closeDialogOk() {
		super.closeDialogOk();
		moreSettingsPanel.commitChanges();
		selectNeuronType.getNeuronPanel().commitChanges(baseNeuron);
		boolean clamp =
				selectNeuronType.getClamped().getSelectedIndex() == TristateDropDown
						.getTRUE();
		baseNeuron.setClamped(clamp);
		selectLayout.commitChanges();
		if (groupPanel.getAddToGroup().isSelected()) {
			addGroup();
		} else {
			addNeurons();
		}
		dispose();
	}

	/**
	 * {@inheritDoc}
	 */
	protected void closeDialogCancel() {
		super.closeDialogCancel();
		dispose();
	}

	/**
	 * A sub-panel which allows a user to put newly created neurons into a
	 * neuron group. Options include a new neuron group, already existing neuron
	 * group, or no neuron group (loose). The user can also change a group's
	 * name from here.
	 *
	 * @author ztosi
	 *
	 */
	private class NeuronGroupPanel extends JPanel {

		/** Select whether or not to add the neurons in a neuron group. */
		private JCheckBox addToGroup = new JCheckBox();

		/** A label for the neuron group name. */
		private JLabel tfNameLabel = new JLabel("Name: ");

		/**
		 * A text box for naming a new neuron group or renaming an existing one.
		 */
		private JTextField tfGroupName = new JTextField();

		/**
		 * Creates the neuron group sub-panel
		 *
		 * @param np
		 *            a reference to the network panel.
		 */
		public NeuronGroupPanel(NetworkPanel np) {
			addListeners();
			setLayout(new BorderLayout());

			JPanel subPanel = new JPanel();
			subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.X_AXIS));
			subPanel.add(addToGroup);
			subPanel.add(Box.createHorizontalStrut(20));
			subPanel.add(tfNameLabel);
			tfGroupName.setEnabled(addToGroup.isSelected());
			subPanel.add(tfGroupName);
			subPanel.setBorder(BorderFactory
					.createEmptyBorder(5, 5, 5, 5));
			this.add(subPanel, BorderLayout.CENTER);
			setBorder(BorderFactory.createTitledBorder("Group"));

		}

		/**
		 * Adds (internal) listeners to the panel.
		 */
		private void addListeners() {
			addToGroup.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {

					tfGroupName.setEnabled(addToGroup.isSelected());

				}

			});

		}

		/**
		 * Generates the neuron group with the attributes from the panel.
		 * Returns null if the {@link #addToGroup addToGroup} check-box is not
		 * selected.
		 *
		 * @return
		 */
		public NeuronGroup generateNeuronGroup() {
			if (addToGroup.isSelected()) {
				NeuronGroup ng =
						new NeuronGroup(networkPanel.getNetwork());
				ng.setLabel(tfGroupName.getText());
				return ng;
			} else {
				return null;
			}
		}

		/**
		 * @return the addToGroup check-box
		 */
		public JCheckBox getAddToGroup() {
			return addToGroup;
		}

	}

}
