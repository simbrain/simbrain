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
import org.simbrain.network.gui.dialogs.network.CompetitivePropertiesPanel.CompetitivePropsPanelType;
import org.simbrain.network.subnetworks.CompetitiveGroup;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;

/**
 * <b>CompetitiveDialog</b>. Create competitive networks.
 */
public class CompetitiveGroupCreationDialog extends StandardDialog {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Logic tab panel. */
    private JPanel tabLogic = new JPanel();

    /** Layout tab panel. */
    private JPanel tabLayout = new JPanel();

    /** Logic panel. */
    private CompetitivePropertiesPanel compPropertiesPanel;

    /** Layout panel. */
    private MainLayoutPanel layoutPanel;

    /** Network Panel. */
    private NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     *
     * @param networkPanel Network panel
     */
    public CompetitiveGroupCreationDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        init();
    }

    /**
     * Initializes all components used in dialog.
     */
    private void init() {

        setTitle("New Competitive Group");
        compPropertiesPanel = new CompetitivePropertiesPanel(networkPanel,
                CompetitivePropsPanelType.CREATE_GROUP);

        // Set up tab panels
        tabLogic.add(compPropertiesPanel);
        layoutPanel = new MainLayoutPanel(false, this);
        layoutPanel.setCurrentLayout(CompetitiveGroup.DEFAULT_LAYOUT);
        tabLayout.add(layoutPanel);
        tabbedPane.addTab("Logic", tabLogic);
        tabbedPane.addTab("Layout", layoutPanel);
        setContentPane(tabbedPane);

        // Help action
        Action helpAction = new ShowHelpAction(
                compPropertiesPanel.getHelpPath());
        addButton(new JButton(helpAction));

    }

    /**
     * Called when dialog closes.
     */
    @Override
    protected void closeDialogOk() {
        compPropertiesPanel.commitChanges();
        CompetitiveGroup competitive =
                (CompetitiveGroup) compPropertiesPanel.getGroup();
        networkPanel.getNetwork().addGroup(competitive);
        layoutPanel.commitChanges();
        competitive.setLayout(layoutPanel.getCurrentLayout());
        competitive.applyLayout();
        networkPanel.repaint();
        super.closeDialogOk();
    }

}
