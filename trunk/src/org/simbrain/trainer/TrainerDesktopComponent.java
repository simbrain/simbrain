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
package org.simbrain.trainer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Desktop component for trainer.
 */
public class TrainerDesktopComponent extends GuiComponent<TrainerComponent> {

    /** Trainer panel. */
    private final TrainerPanel trainerPanel;

    /** Current network. */
    private RootNetwork currentNetwork;

    /** Default height. */
    private static final int DEFAULT_HEIGHT = 550;

    /** Default width. */
    private static final int DEFAULT_WIDTH = 600;

    /** Network selection combo box. */
    private JComboBox cbNetworkChooser = new JComboBox();

    /**
     * Listen to the workspace. When components are added update the network
     * selection combo box.
     */
    private WorkspaceListener workspaceListener = new WorkspaceListener() {

        /**
         * Clear the Simbrain desktop.
         */
        public void workspaceCleared() {
        }

        @SuppressWarnings("unchecked")
        public void componentAdded(final WorkspaceComponent workspaceComponent) {
            if (workspaceComponent instanceof NetworkComponent) {
                cbNetworkChooser.addItem((NetworkComponent) workspaceComponent);
            }
        }

        @SuppressWarnings("unchecked")
        public void componentRemoved(final WorkspaceComponent workspaceComponent) {
            if (workspaceComponent instanceof NetworkComponent) {
                if (((NetworkComponent) workspaceComponent).getRootNetwork() == currentNetwork) {
                    trainerPanel.getTrainer().setNetwork(null);
                }
                cbNetworkChooser
                        .removeItem((NetworkComponent) workspaceComponent);
            }
        }
    };

    /**
     * Construct the GUI Bar Chart.
     *
     * @param frame Generic frame
     * @param component Bar chart component
     */
    public TrainerDesktopComponent(final GenericFrame frame,
            final TrainerComponent component) {
        super(frame, component);
        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

        trainerPanel = new TrainerPanel(getWorkspaceComponent().getTrainer());

        component.getWorkspace().addListener(workspaceListener);
        // component.setCurrentFile(currentFile);

        // Place trainerPanel in a buffer so that toolbars don't get in the way
        // of canvas elements
        setLayout(new BorderLayout());

        // Put it all together
        add("Center", trainerPanel);
        createAndAttachMenus();

        // Initialize combo box action listeners
        cbNetworkChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                // If there are no networks in the workspace, return.
                Object object = cbNetworkChooser.getSelectedItem();
                if (object instanceof NetworkComponent) {
                    trainerPanel.getTrainer().setNetwork(
                            ((NetworkComponent) object).getRootNetwork());
                }
            }
        });
        trainerPanel.addTopItem(cbNetworkChooser);
        JLabel netSelectLabel = new JLabel("Select Root Network");
        trainerPanel.addTopItem(netSelectLabel);

        // Initialize selection box with existing components, if any
        for (WorkspaceComponent currentComponent : getWorkspaceComponent()
                .getWorkspace().getComponentList(NetworkComponent.class)) {
            cbNetworkChooser.addItem(currentComponent);
        }
        if (cbNetworkChooser.getSelectedItem() != null) {
            if (cbNetworkChooser.getSelectedItem() instanceof Network) {
                trainerPanel.getTrainer().setNetwork(
                        (Network) cbNetworkChooser.getSelectedItem());
            }
        }
    }

    /**
     * Create and attach the menus for this network frame.
     */
    private void createAndAttachMenus() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(TrainerDesktopActions.getOpenAction(this));
        fileMenu.add(TrainerDesktopActions.getSaveAction(this));
        fileMenu.add(TrainerDesktopActions.getSaveAsAction(this));
        menuBar.add(fileMenu);

        // Build Menu
        JMenu buildMenu = new JMenu("Build");
        JMenuItem threeLayerItem = new JMenuItem(
                TrainerDesktopActions.getBuildThreeLayerAction(this));
        buildMenu.add(threeLayerItem);
        JMenuItem multiLayerItem = new JMenuItem(
                TrainerDesktopActions.getBuildMultiLayerAction(this));
        buildMenu.add(multiLayerItem);
        menuBar.add(buildMenu);

        getParentFrame().setJMenuBar(menuBar);
        getParentFrame().pack();
    }

    @Override
    public void postAddInit() {

        // TODO: This all assumes the trainer's network name names a root
        // network. Need to separately store the root network name and the
        // network name.
        String networkId = trainerPanel.getTrainer().getNetworkName();
        NetworkComponent component = (NetworkComponent) getWorkspaceComponent()
                .getWorkspace().getComponent(networkId);
        if (component != null) {
            trainerPanel.getTrainer().setNetwork(component.getRootNetwork());
            trainerPanel.getTrainer().postAddInit();
        }
        if (this.getParentFrame().getJMenuBar() == null) {
            createAndAttachMenus();
        }
    }

    @Override
    protected void closing() {
        this.getWorkspaceComponent().getWorkspace()
                .removeListener(workspaceListener);
    }

    /**
     * @return the trainerPanel
     */
    public TrainerPanel getTrainerPanel() {
        return trainerPanel;
    }

}
