/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;

import javax.swing.JMenuBar;
import javax.swing.JPanel;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.GenericFrame;


/**
 * Network frame.
 */
public final class NetworkDesktopComponent extends GuiComponent<NetworkComponent> {

    private static final long serialVersionUID = 1L;

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Container for toolbars. */
    private JPanel toolbars = new JPanel();

    /**
     * Create a new network frame.
     */
    public NetworkDesktopComponent(GenericFrame frame, NetworkComponent component) {
        super(frame, component);
        this.setPreferredSize(new Dimension(450,400));

        networkPanel = new NetworkPanel(component.getRootNetwork(), this);
        
        // Place networkPanel in a buffer so that toolbars don't get in the way of canvas elements
        setLayout(new BorderLayout());

        // Construct toolbar pane
        FlowLayout flow = new FlowLayout(FlowLayout.LEFT);
        flow.setHgap(0);
        flow.setVgap(0);
        toolbars.setLayout(flow);
        toolbars.add(networkPanel.getMainToolBar());
        toolbars.add(networkPanel.getEditToolBar());
        toolbars.add(networkPanel.getClampToolBar());

        // Put it all together
        add("North", toolbars);
        add("Center", networkPanel);
        createAndAttachMenus();
    }
    
    

    /**
     * Create and attach the menus for this network frame.
     */
    private void createAndAttachMenus() {

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(networkPanel.createFileMenu());
        menuBar.add(networkPanel.createEditMenu());
        menuBar.add(networkPanel.createInsertMenu());
        menuBar.add(networkPanel.createViewMenu());
        menuBar.add(networkPanel.createHelpMenu());
        getParentFrame().setJMenuBar(menuBar);
    }
    
    @Override
    public void postAddInit() {
        networkPanel.getLayer().removeAllChildren(); // Maybe in a reset method?
        if (networkPanel.getRootNetwork() != this.getWorkspaceComponent().getRootNetwork()) {
            networkPanel.setRootNetwork(this.getWorkspaceComponent().getRootNetwork().getRootNetwork());            
        }
        networkPanel.getRootNetwork().addListener(networkPanel);
        networkPanel.syncToModel();
        networkPanel.repaint();
        networkPanel.clearSelection();
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
        // TODO Auto-generated method stub
    }

}