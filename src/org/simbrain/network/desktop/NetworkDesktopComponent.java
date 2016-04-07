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
package org.simbrain.network.desktop;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.actions.network.ShowNetworkPreferencesAction;
import org.simbrain.network.gui.actions.network.ShowNetworkUpdaterDialog;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Network desktop component. An extension of the Gui component for this class
 * which is used in the Simbrain desktop.
 */
public final class NetworkDesktopComponent extends
        GuiComponent<NetworkComponent> {

    /** Network panel. */
    private final NetworkPanelDesktop networkPanel;

    /** Menu bar. */
    private JMenuBar menuBar;

    /** Default height. */
    private static final int DEFAULT_HEIGHT = 450;

    /** Default width. */
    private static final int DEFAULT_WIDTH = 450;

    /**
     * If a synapse group has more than this many synapses and does not have
     * "compression" turned on, show user a warning.
     */
    private static final int saveWarningThreshold = 200;

    /**
     * Create a new network frame.
     * @param frame frame of network
     * @param component network component
     */
    public NetworkDesktopComponent(final GenericFrame frame,
            final NetworkComponent component) {
        super(frame, component);
        this.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

        networkPanel = new NetworkPanelDesktop(this, component.getNetwork());

        // component.setCurrentFile(currentFile);

        // Place networkPanel in a buffer so that toolbars don't get in the way
        // of canvas elements
        setLayout(new BorderLayout());

        // Put it all together
        add("Center", networkPanel);
        createAndAttachMenus();

        // Toggle the network panel's visiblity if the workspace component is
        // set to "gui off"
        component
                .addWorkspaceComponentListener(new WorkspaceComponentListener() {

                    @Override
                    public void componentUpdated() {
                    }

                    @Override
                    public void guiToggled() {
                        networkPanel
                                .setGuiOn(getWorkspaceComponent().isGuiOn());
                    }

                    @Override
                    public void componentOnOffToggled() {
                    }

                });
    }

    /**
     * Create and attach the menus for this network frame.
     */
    private void createAndAttachMenus() {

        menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(networkPanel.createEditMenu());
        menuBar.add(networkPanel.createInsertMenu());
        menuBar.add(networkPanel.createViewMenu());
        menuBar.add(NetworkScriptMenu.getNetworkScriptMenu(this
                .getNetworkPanel()));
        // menuBar.add(createAttributeMenu());
        menuBar.add(networkPanel.createHelpMenu());
        getParentFrame().setJMenuBar(menuBar);
    }

    /**
     * Create and return a new File menu for this Network panel.
     * 
     * @return a new File menu for this Network panel
     */
    JMenu createFileMenu() {

        JMenu fileMenu = new JMenu("File");

        fileMenu.add(new OpenAction(this));
        fileMenu.add(new SaveAction(this));
        fileMenu.add(new SaveAsAction(this));
        fileMenu.addSeparator();
        fileMenu.add(new ShowNetworkUpdaterDialog(networkPanel));
        fileMenu.add(new ShowNetworkPreferencesAction(networkPanel));
        fileMenu.addSeparator();

        fileMenu.add(new CloseAction(this.getWorkspaceComponent()));

        return fileMenu;
    }

    @Override
    public void postAddInit() {
        if (this.getParentFrame().getJMenuBar() == null) {
            createAndAttachMenus();
        }
        networkPanel.getNetwork().setName(this.getName());

        // TODO: Below only needs to happen when opening; but currently it
        // happens also when creating a new network
        networkPanel.clearPanel();
        if (networkPanel.getNetwork() != this.getWorkspaceComponent()
                .getNetwork()) {
            networkPanel.setNetwork(this.getWorkspaceComponent().getNetwork());
        }
        networkPanel.syncToModel();
        networkPanel.initGui();
    }

    /**
     * Return the network panel for this network frame.
     * 
     * @return the network panel for this network frame
     */
    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    @Override
    public void closing() {
    }

    @Override
    public void showSaveFileDialog() {
        if (showUncompressedSynapseGroupWarning()) {
            super.showSaveFileDialog();
        }

    }

    @Override
    public void save() {
        if (showUncompressedSynapseGroupWarning()) {
            super.save();
        }
    }

    /**
     * If at least one synapse group has a large number of synapses that are not
     * going to be saved using compression, show the user a warning.
     * 
     * @return true if the save operation should proceed, false if the save
     *         operation should be cancelled.
     */
    private boolean showUncompressedSynapseGroupWarning() {
        boolean showPanel = false;
        for (SynapseGroup group : networkPanel.getNetwork().getSynapseGroups()) {
            if (group.getAllSynapses().size() > saveWarningThreshold) {
                if (!group.isUseGroupLevelSettings()) {
                    showPanel = true;
                }
            }
        }
        if (showPanel) {
            int n = JOptionPane
                    .showConfirmDialog(
                            null,
                            "<html><body><p style='width: 200px;'>You are saving at least one large synapse group without compression. "
                                    + "It is reccomended that you enable 'optimize as group' in all large synapse groups so that "
                                    + "their weight matrices are compressed.   Otherwise the save will take a "
                                    + "long time and the saved file will be large. Click Cancel to go ahead with the save, "
                                    + "and OK to return to the network and change settings.</body></html>",
                            "Save Warning", JOptionPane.OK_CANCEL_OPTION);
            if (n == JOptionPane.OK_OPTION) {
                return false;
            }
        }
        return true;
    }

}