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

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.MainLayoutPanel;
import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * <b>DiscreteHopfieldDialog</b> is a dialog box for creating discrete Hopfield
 * networks.
 */
public class HopfieldCreationDialog extends StandardDialog {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Logic tab panel. */
    private JPanel tabLogic = new JPanel();

    /** Layout tab panel. */
    private JPanel tabLayout = new JPanel();

    /** Logic panel. */
    private HopfieldPropertiesPanel hopPropertiesPanel;

    /** Layout panel. */
    private MainLayoutPanel layoutPanel;

    /** Network Panel. */
    private NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     *
     * @param networkPanel Network panel
     */
    public HopfieldCreationDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        layoutPanel = new MainLayoutPanel(false, this);
        init();
    }

    /**
     * Initializes all components used in dialog.
     */
    private void init() {
        // Initializes dialog
        setTitle("New Hopfield Network");

        // Logic Panel
        hopPropertiesPanel = new HopfieldPropertiesPanel(networkPanel);
        hopPropertiesPanel.fillFieldValues();
        tabLogic.setLayout(new FlowLayout());
        tabLogic.add(hopPropertiesPanel);

        // Layout panel
        tabLayout.add(layoutPanel);
        layoutPanel = new MainLayoutPanel(false, this);

        // Set it all up
        tabbedPane.addTab("Logic", tabLogic);
        tabbedPane.addTab("Layout", layoutPanel);
        setContentPane(tabbedPane);

        // Help action
        Action helpAction = new ShowHelpAction(hopPropertiesPanel.getHelpPath());
        addButton(new JButton(helpAction));

    }

    /**
     * Called when dialog closes.
     */
    @Override
    protected void closeDialogOk() {
        hopPropertiesPanel.commitChanges();
        Hopfield hopfield = (Hopfield) hopPropertiesPanel.getGroup();
        layoutPanel.commitChanges();
        layoutPanel.getCurrentLayout().setInitialLocation(
                networkPanel.getWhereToAdd());
        hopfield.getNeuronGroup().setLayout(layoutPanel.getCurrentLayout());
        hopfield.getNeuronGroup().applyLayout();
        networkPanel.getNetwork().addGroup(hopfield);
        networkPanel.repaint();
        super.closeDialogOk();
    }
}
