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
package org.simbrain.network.gui.dialogs.synapse;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;

/**
 * The <b>SynapseDialog</b> is initialized with a list of synapses. When the
 * dialog is closed the synapses are changed based on the state of the dialog.
 */
public class SynapseDialog extends StandardDialog {

	/** The default serial version id. */
	private static final long serialVersionUID = 1L;

	/** Null string. */
	public static final String NULL_STRING = "...";

	/** Main panel. */
	private final Box mainPanel = Box.createVerticalBox();

	/**
	 * Top panel. Contains fields for displaying/editing basic synapse
	 * information.
	 * 
	 * @see org.simbrain.network.gui.dialogs.synapse.BasicSynapseInfoPanel.java
	 */
	private final BasicSynapseInfoPanel topPanel;

	/**
	 * Bottom panel. Contains fields for displaying/editing synapse update rule
	 * parameters.
	 * 
	 * @see org.simbrain.network.gui.dialogs.synapse.SynapseUpdateSettingsPanel.java
	 */
	private final SynapseUpdateSettingsPanel bottomPanel;

	/**
	 * Help Button. Links to information about the currently selected synapse
	 * update rule.
	 */
	private final JButton helpButton = new JButton("Help");

	/** Show Help Action. The action executed by the help button */
	private ShowHelpAction helpAction;

	/** The synapses being modified. */
	private final ArrayList<Synapse> synapseList;

	/**
	 * @param selectedSynapses
	 *            the pnode_synapses being adjusted
	 */
	public SynapseDialog(final Collection<SynapseNode> selectedSynapses) {
		this(getSynapses(selectedSynapses));
	}

	/**
	 * @param synapseList
	 *            the logical synapses being adjusted
	 */
	public SynapseDialog(final List<Synapse> synapseList) {
		this.synapseList = (ArrayList<Synapse>) synapseList;
		topPanel = new BasicSynapseInfoPanel(synapseList, this);
		bottomPanel = new SynapseUpdateSettingsPanel(synapseList, this);
		init();
		addListeners();
		updateHelp();
	}

	/**
	 * Get the logical synapses from the SynapseNodes.
	 */
	private static ArrayList<Synapse> getSynapses(
			final Collection<SynapseNode> selectedSynapses) {
		ArrayList<Synapse> sl = new ArrayList<Synapse>();
		for (SynapseNode s : selectedSynapses) {
			sl.add(s.getSynapse());
		}
		return sl;
	}

	/**
	 * Initializes the components on the panel.
	 */
	private void init() {
		setTitle("Synapse Dialog");
		mainPanel.add(topPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		mainPanel.add(bottomPanel);
		setContentPane(mainPanel);
		this.addButton(helpButton);
	}

	/**
	 * Add listeners to the components of the dialog
	 */
	private void addListeners() {
		bottomPanel.getCbSynapseType().addActionListener(
				new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {

						updateHelp();

					}

				});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void closeDialogOk() {
		super.closeDialogOk();
		commitChanges();
	}

	/**
	 * Set the help page based on the currently selected synapse type.
	 */
	public void updateHelp() {
		if (bottomPanel.getCbSynapseType().getSelectedItem() == NULL_STRING) {
			helpAction = new ShowHelpAction("Pages/Network/synapse.html");
		} else {
			String name =
					(String) bottomPanel.getCbSynapseType()
							.getSelectedItem();
			helpAction =
					new ShowHelpAction("Pages/Network/synapse/" + name
							+ ".html");
		}
		helpButton.setAction(helpAction);
	}

	/**
	 * Called externally when the dialog is closed, to commit any changes made.
	 */
	public void commitChanges() {

		topPanel.commitChanges();

		// Now commit changes specific to the synapse type
		bottomPanel.getSynapsePanel().commitChanges(synapseList);

		// Notify the network that changes have been made
		synapseList.get(0).getNetwork().fireNetworkChanged();

	}

	public ArrayList<Synapse> getSynapseList() {
		return synapseList;
	}

	/**
	 * Test Main: For fast prototyping
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Network net = new Network();
		NetworkPanel np = new NetworkPanel(net);
		Neuron n = new Neuron(net, new LinearRule());
		NeuronNode nn = new NeuronNode(np, n);

		Synapse s = new Synapse(n, n);
		ArrayList<SynapseNode> arr = new ArrayList<SynapseNode>();
		arr.add(new SynapseNode(np, nn, nn, s));
		SynapseDialog nd = new SynapseDialog(arr);

		nd.pack();
		nd.setVisible(true);

	}

}
