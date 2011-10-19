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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.layout.LayoutPanel;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.networks.Hopfield;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>DiscreteHopfieldDialog</b> is a dialog box for creating discrete Hopfield
 * networks.
 */
public class HopfieldDialog extends StandardDialog {

    /** File system separator. */
    private static final String FS = System.getProperty("file.separator");

    /** Sequential network update order. */
    public static final int SEQUENTIAL = 0;

    /** Random network update order. */
    public static final int RANDOM = 1;

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Logic tab panel. */
    private JPanel tabLogic = new JPanel();

    /** Layout tab panel. */
    private JPanel tabLayout = new JPanel();

    /** Logic panel. */
    private LabelledItemPanel logicPanel = new LabelledItemPanel();

    /** Layout panel. */
    private LayoutPanel layoutPanel;

    /** Number of units field. */
    private JTextField numberOfUnits = new JTextField();

    /** Network type combo box. */
    private JComboBox cbUpdateOrder = new JComboBox(new String[] {
            "Sequential", "Random" });

    /** Open training file button. */
    private JButton trainingFile = new JButton("Set");

    /** Network panel. */
    private NetworkPanel networkPanel;

    /**
     * Default constructor.
     *
     * @param net Network panel
     */
    public HopfieldDialog(final NetworkPanel net) {
        networkPanel = net;
        // layoutPanel = new LayoutPanel(this, new AbstractLayoutPanel[] {
        // new GridLayoutPanel(), new HexagonalGridLayoutPanel(),
        // new LineLayoutPanel() });
        init();
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {

        int numUnits = Integer.parseInt(numberOfUnits.getText());
        // Layout layout = layoutPanel.getNeuronLayout();
        GridLayout layout = new GridLayout(50, 50, (int) Math.sqrt(numUnits));
        layout.setInitialLocation(networkPanel.getLastClickedPosition());
        Hopfield hop = new Hopfield(networkPanel.getRootNetwork(), numUnits,
                layout);
        networkPanel.getRootNetwork().addNetwork(hop);
        networkPanel.repaint();
        super.closeDialogOk();
    }

    /**
     * Initializes the panel.
     */
    private void init() {
        // Initialize Dialog
        setTitle("New Hopfield Network");

        fillFieldValues();

        // Set up graphics panel
        logicPanel.addItem("Update order", cbUpdateOrder);
        logicPanel.addItem("Number of Units", numberOfUnits);
        // logicPanel.addItem("Set training file", trainingFile);

        // Set up tab panel
        // tabLogic.add(logicPanel);
        // tabLayout.add(layoutPanel);
        // tabbedPane.addTab("Logic", logicPanel);
        // tabbedPane.addTab("Layout", layoutPanel);
        setContentPane(logicPanel);

    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        Hopfield dh = new Hopfield();
        numberOfUnits.setText(Integer.toString(dh.getNumUnits()));
    }

}
