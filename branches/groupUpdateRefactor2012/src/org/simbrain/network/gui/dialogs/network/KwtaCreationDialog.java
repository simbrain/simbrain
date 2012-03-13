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

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.subnetworks.KWTA;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.layout.AbstractLayoutPanel;
import org.simbrain.network.gui.dialogs.network.layout.GridLayoutPanel;
import org.simbrain.network.gui.dialogs.network.layout.HexagonalGridLayoutPanel;
import org.simbrain.network.gui.dialogs.network.layout.LayoutPanel;
import org.simbrain.network.gui.dialogs.network.layout.LineLayoutPanel;
import org.simbrain.network.layouts.Layout;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>KwtaDialog</b> is used as an assistant to create Kwta networks.
 *
 */
public class KwtaCreationDialog extends StandardDialog {

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

    //TODO: Separate this from number of neurons!  Add a second field.
    /** K field. */
    private JTextField tfK = new JTextField("5");

    /** Network Panel. */
    private NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     *
     * @param networkPanel Network panel
     */
    public KwtaCreationDialog(final NetworkPanel networkPanel) {
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
        KWTA kWTA = new KWTA(networkPanel.getRootNetwork(), Integer.parseInt(tfK.getText()), layout);
        networkPanel.getRootNetwork().addGroup(kWTA);
        networkPanel.repaint();
        super.closeDialogOk();
    }

    /**
     * Initializes all components used in dialog.
     */
    private void init() {
        // Initializes dialog
        setTitle("New K Winner Take All Network");

        fillFieldValues();

        tfK.setColumns(5);

        // Set up logic panel
        logicPanel.addItem("Number of Neurons", tfK);

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
        //KWTA kw = new KWTA();
        tfK.setText("5");

    }

}
