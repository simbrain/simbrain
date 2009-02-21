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

import java.awt.Color;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import org.simbrain.network.gui.NetworkGuiSettings;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.NetworkDialog;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;

/**
 * Extension of Network Panel with functions used in a desktop setting.
 * 
 * @author jyoshimi
 */
public class NetworkPanelDesktop extends NetworkPanel {

    NetworkDesktopComponent component;
    
    public NetworkPanelDesktop(final NetworkDesktopComponent component, final RootNetwork rootNetwork) {
        super(rootNetwork);
        this.component = component;
        
        // TODO: Finish this and clean it up
        NetworkGuiSettings.setLineColor(new Color(NetworkGuiPreferences.getLineColor()));
    }
    
    
    /**
     * Create and return a new Edit menu for this rootNetwork panel.
     *
     * @return a new Edit menu for this rootNetwork panel
     */
    JMenu createEditMenu() {

        JMenu editMenu = new JMenu("Edit");

        editMenu.add(actionManager.getCutAction());
        editMenu.add(actionManager.getCopyAction());
        editMenu.add(actionManager.getPasteAction());
        editMenu.addSeparator();
        editMenu.add(actionManager.getClearAction());
        editMenu.add(createSelectionMenu());
        editMenu.addSeparator();
        editMenu.add(actionManager.getGroupAction());
        editMenu.add(actionManager.getUngroupAction());
        editMenu.addSeparator();
        editMenu.add(createAlignMenu());
        editMenu.add(createSpacingMenu());
        editMenu.addSeparator();
        editMenu.add(createClampMenu());
        editMenu.addSeparator();
        editMenu.add(actionManager.getShowIOInfoMenuItem());
        editMenu.add(actionManager.getSetAutoZoomMenuItem());
        editMenu.addSeparator();
        editMenu.add(actionManager.getSetNeuronPropertiesAction());
        editMenu.add(actionManager.getSetSynapsePropertiesAction());

        return editMenu;
    }

    /**
     * Create and return a new Insert menu for this rootNetwork panel.
     *
     * @return a new Insert menu for this rootNetwork panel
     */
     JMenu createInsertMenu() {

        JMenu insertMenu = new JMenu("Insert");

        insertMenu.add(createNewNetworkMenu());
        insertMenu.add(actionManager.getNewNeuronAction());

        return insertMenu;
    }

    /**
     * Create and return a new View menu for this rootNetwork panel.
     *
     * @return a new View menu for this rootNetwork panel
     */
    JMenu createViewMenu() {
        JMenu viewMenu = new JMenu("View");

        viewMenu.add(actionManager.getShowEditToolBarMenuItem());
        viewMenu.add(actionManager.getShowMainToolBarMenuItem());
        viewMenu.add(actionManager.getShowClampToolBarMenuItem());
        viewMenu.add(actionManager.getShowGUIAction());
        viewMenu.add(actionManager.getShowNodesAction());

        return viewMenu;
    }

    /**
     * Create clamp JMenu.
     *
     * @return the clamp JMenu
     */
     JMenu createClampMenu() {
        JMenu clampMenu = new JMenu("Clamp");
        JCheckBoxMenuItem cbW = actionManager.getClampWeightsMenuItem();
        checkBoxes.add(cbW);
        clampMenu.add(cbW);
        JCheckBoxMenuItem cbN = actionManager.getClampNeuronsMenuItem();
        checkBoxes.add(cbN);
        clampMenu.add(cbN);
        return clampMenu;
    }

    /**
     * Create a selection JMenu.
     *
     * @return the selection menu.
     */
     JMenu createSelectionMenu() {
        JMenu selectionMenu = new JMenu("Select");
        selectionMenu.add(actionManager.getSelectAllAction());
        selectionMenu.add(actionManager.getSelectAllWeightsAction());
        selectionMenu.add(actionManager.getSelectAllNeuronsAction());
        selectionMenu.add(actionManager.getSelectIncomingWeightsAction());
        selectionMenu.add(actionManager.getSelectOutgoingWeightsAction());
        return selectionMenu;
    }

    /**
     * Create and return a new Help menu for this rootNetwork panel.
     *
     * @return a new Help menu for this rootNetwork panel
     */
    public JMenu createHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(actionManager.getShowHelpAction());
        return helpMenu;
    }
    
    /*
     * This version of network dialog allows user to set User Preferences.
     *
     * @param networkPanel
     * @return superclass version of network dialog, with User Preferences
     */
    public NetworkDialog getNetworkDialog(final NetworkPanel networkPanel) {
        return new DesktopNetworkDialog(networkPanel);
    }
    
    /**
     * This version of a NeuronNode has a coupling context menu.
     *
     * @param networkPanel network panel
     * @param neuron neuron to show in Gui
     * @return desktop version of NeuronNode, with context menu
     */
    public NeuronNode getNeuronNode(final NetworkPanel net, final Neuron neuron) {
        return new NeuronNodeDesktop(component, net, neuron);
    }

}
