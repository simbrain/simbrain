/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.dialog.network;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.layout.AbstractLayoutPanel;
import org.simbrain.network.dialog.network.layout.GridLayoutPanel;
import org.simbrain.network.dialog.network.layout.HexagonalGridLayoutPanel;
import org.simbrain.network.dialog.network.layout.LayoutPanel;
import org.simbrain.network.dialog.network.layout.LineLayoutPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.layouts.Layout;
import org.simnet.networks.StandardNetwork;

/**
 * <b>CompetitiveDialog</b> is used as an assistant to create competitive networks.
 *
 */
public class StandardNetworkDialog extends StandardDialog {
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

    /** Number of neurons field. */
    private JTextField tfNumNeurons = new JTextField();

    /** Network Panel. */
    private NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     *
     * @param networkPanel Network panel
     */
    public StandardNetworkDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        layoutPanel = new LayoutPanel(this, new AbstractLayoutPanel[]{new LineLayoutPanel(),
                           new HexagonalGridLayoutPanel(), new GridLayoutPanel()});
        init();
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        Layout layout = layoutPanel.getNeuronLayout();
        layout.setInitialLocation(networkPanel.getLastClickedPosition());
        StandardNetwork standard = new StandardNetwork(Integer.parseInt(tfNumNeurons.getText()), layout);
        networkPanel.getNetwork().addNetwork(standard);
        networkPanel.repaint();
        super.closeDialogOk();
    }

    /**
     * Initializes all components used in dialog.
     */
    private void init() {
        // Initializes dialog
        setTitle("New Standard Netwok");

        fillFieldValues();

        tfNumNeurons.setColumns(5);

        // Set up logic panel
        logicPanel.addItem("Number of Neurons", tfNumNeurons);

        // Set up tab panels
        tabLogic.add(logicPanel);
        tabLayout.add(layoutPanel);
        tabbedPane.addTab("Logic", tabLogic);
        tabbedPane.addTab("Layout", layoutPanel);
        setContentPane(tabbedPane);
    }


    /**
     * Populate fields with current data.
     */
    private void fillFieldValues() {
        StandardNetwork st = new StandardNetwork();
        tfNumNeurons.setText(Integer.toString(st.getNumNeurons()));

    }

}
