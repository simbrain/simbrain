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

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.MainLayoutPanel;
import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * <b>WTADialog</b> is a dialog box for setting the properties of the Network
 * GUI.
 */
public class WTACreationDialog extends StandardDialog {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Logic tab panel. */
    private JPanel tabLogic = new JPanel();

    /** Layout tab panel. */
    private JPanel tabLayout = new JPanel();

    /** Logic panel. */
    private WTAPropertiesPanel wtaPanel;

    /** Layout panel. */
    private MainLayoutPanel layoutPanel;

    /** Network panel. */
    private NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     *
     * @param np Network panel
     */
    public WTACreationDialog(final NetworkPanel np) {
        networkPanel = np;
        layoutPanel = new MainLayoutPanel(false, this);
        init();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        // Initialize Dialog
        setTitle("New WTA Network");

        // Set up tab panels
        wtaPanel = new WTAPropertiesPanel(networkPanel);
        tabLogic.add(wtaPanel);
        layoutPanel = new MainLayoutPanel(false, this);
        layoutPanel.setCurrentLayout(WinnerTakeAll.DEFAULT_LAYOUT);
        tabLayout.add(layoutPanel);
        tabbedPane.addTab("Logic", tabLogic);
        tabbedPane.addTab("Layout", tabLayout);
        setContentPane(tabbedPane);

        // Help action
        Action helpAction = new ShowHelpAction(wtaPanel.getHelpPath());
        addButton(new JButton(helpAction));
    }

    /**
     * Called when dialog closes.
     */
    @Override
    protected void closeDialogOk() {
        wtaPanel.commitChanges();
        WinnerTakeAll wta = (WinnerTakeAll) wtaPanel.getGroup();
        layoutPanel.commitChanges();
        wta.setLayout(layoutPanel.getCurrentLayout());
        wta.applyLayout();
        wta.offset(networkPanel.getWhereToAdd().getX(), networkPanel
                .getWhereToAdd().getY());
        networkPanel.getNetwork().addGroup(wta);
        networkPanel.repaint();
        super.closeDialogOk();
    }

}
