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
package org.simbrain.network;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.gauge.GaugeComponent;
import org.simbrain.network.actions.AddGaugeAction;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.CouplingContainer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Network frame.
 */
public final class NetworkComponent extends WorkspaceComponent {

    private static int windowIndex = 1;

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Container for toolbars. */
    private JPanel toolbars = new JPanel();

    /**
     * Create a new network frame.
     */
    public NetworkComponent() {

        super();
        
        this.setPreferredSize(new Dimension(450,400));

        networkPanel = new NetworkPanel();

        // place networkPanel in a buffer so that toolbars don't get in the way of canvas elements
        JPanel buffer = new JPanel();
        buffer.setLayout(new BorderLayout());

        // Construct toolbar pane
        FlowLayout flow = new FlowLayout(FlowLayout.LEFT);
        flow.setHgap(0);
        flow.setVgap(0);
        toolbars.setLayout(flow);
        toolbars.add(networkPanel.getMainToolBar());
        toolbars.add(networkPanel.getEditToolBar());
        toolbars.add(networkPanel.getClampToolBar());

        // Put it all together
        buffer.add("North", toolbars);
        buffer.add("Center", networkPanel);
        setContentPane(buffer);
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
        setJMenuBar(menuBar);
    }

    /**
     * Returns a refrence to the root network, which contains all couplings.
     */
    public CouplingContainer getCouplingContainer() {
        return networkPanel.getRootNetwork();
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
    public String getFileExtension() {
        return "net";
    }

    @Override
    public boolean isChangedSinceLastSave() {
        return false;
    }

    @Override
    public void save(File saveFile) {
        networkPanel.saveNetwork(saveFile);
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }


    @Override
    public void open(File openFile) {
        this.setName(openFile.getName());
        this.getNetworkPanel().openNetwork(openFile);
    }

    @Override
    public int getWindowIndex() {
        // TODO Auto-generated method stub
        return windowIndex++;
    }
    
    @Override
    public void updateComponent() {
        super.updateComponent();
        this.getNetworkPanel().getRootNetwork().updateRootNetwork();
    }


}