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
package org.simbrain.network.gui.dialogs.network;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.MainLayoutPanel;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.subnetworks.SOM;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>SOMDialog</b> is used as an assistant to create SOM networks.
 * 
 */
public class SOMCreationDialog extends StandardDialog implements
		ActionListener {
	/** Tabbed pane. */
	private JTabbedPane tabbedPane = new JTabbedPane();

	/** Logic tab panel. */
	private JPanel tabLogic = new JPanel();

	/** Layout tab panel. */
	private JPanel tabLayout = new JPanel();

	/** Logic panel. */
	private LabelledItemPanel logicPanel = new LabelledItemPanel();

	/** Layout panel. */
	private MainLayoutPanel layoutPanel;

	/** Number of neurons field. */
	private JTextField tfNumNeurons = new JTextField();

	/** Alpha field. */
	private JTextField tfAlpha = new JTextField();

	/** NeighborhoodSize value field. */
	private JTextField tfNeighborhoodSize = new JTextField();

	/** NumInputVectors value field. */
	private JTextField tfNumInputVectors = new JTextField();

	/** AlphaDecayRate value field. */
	private JTextField tfAlphaDecayRate = new JTextField();

	/** NeighborhoodDecayAmount value field. */
	private JTextField tfNeigborhoodDecayAmount = new JTextField();

	/** Network Panel. */
	private NetworkPanel networkPanel;

	/**
	 * This method is the default constructor.
	 * 
	 * @param networkPanel
	 *            Network panel
	 */
	public SOMCreationDialog(final NetworkPanel networkPanel) {
		this.networkPanel = networkPanel;
		layoutPanel = new MainLayoutPanel(false, this);
		init();
	}

	/**
	 * Called when dialog closes.
	 */
	protected void closeDialogOk() {

		SOM som =
				new SOM(networkPanel.getNetwork(),
						Integer.parseInt(tfNumNeurons.getText()));
		som.setInitAlpha(Double.parseDouble(tfAlpha.getText()));
		som.setInitNeighborhoodSize(Double.parseDouble(tfNeighborhoodSize
				.getText()));
		som.setNumInputVectors(Integer.parseInt(tfNumInputVectors
				.getText()));
		som.setAlphaDecayRate(Double.parseDouble(tfAlphaDecayRate
				.getText()));
		som.setNeighborhoodDecayAmount(Double
				.parseDouble(tfNeigborhoodDecayAmount.getText()));

		layoutPanel.commitChanges();
		Layout layout = layoutPanel.getCurrentLayout();
		layout.setInitialLocation(networkPanel.getLastClickedPosition());
		layout.layoutNeurons(som.getNeuronList());
		networkPanel.getNetwork().addGroup(som);
		networkPanel.getNetwork().fireNetworkChanged(); // Force interaction box
		// to update
		networkPanel.repaint();
		super.closeDialogOk();

	}

	/**
	 * Initializes all components used in dialog.
	 */
	private void init() {
		// Initializes dialog
		setTitle("New SOM Network");

		fillFieldValues();

		tfNumNeurons.setColumns(5);

		// Set up logic panel
		logicPanel.addItem("Number of Neurons", tfNumNeurons);
		logicPanel.addItem("Initial Learning Rate", tfAlpha);
		logicPanel.addItem("Initial Neighborhood Size",
				tfNeighborhoodSize);
		// logicPanel.addItem("Number of Input Vectors", tfNumInputVectors);
		logicPanel.addItem("Learning Decay Rate", tfAlphaDecayRate);
		logicPanel.addItem("Neighborhood Decay Amount",
				tfNeigborhoodDecayAmount);

		// Set up tab panels
		tabLogic.add(logicPanel);
		tabLayout.add(layoutPanel);
		tabbedPane.addTab("Logic", tabLogic);
		tabbedPane.addTab("Layout", layoutPanel);
		setContentPane(tabbedPane);

	}

	/**
	 * @see java.awt.event.ActionListener
	 */
	public void actionPerformed(final ActionEvent e) {
		/* no implementation */
	}

	/**
	 * Populate fields with current data.
	 */
	private void fillFieldValues() {
		tfAlpha.setText("" + SOM.DEFAULT_ALPHA);
		tfNeighborhoodSize.setText("" + SOM.DEFAULT_INIT_NSIZE);
		tfNumNeurons.setText("" + 25);
		tfNumInputVectors.setText("" + 10);
		tfAlphaDecayRate.setText("" + SOM.DEFAULT_DECAY_RATE);
		tfNeigborhoodDecayAmount.setText(""
				+ SOM.DEFAULT_NEIGHBORHOOD_DECAY_AMOUNT);
	}

}
