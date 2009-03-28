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
package org.simbrain.network.gui;

import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

//TODO: Add file menu, fix help menu
/**
 * Menu bar that appears inside of jpanel; for use in Applets
 */
public class NetworkMenuBar {

    public static JMenuBar getAppletMenuBar(final NetworkPanel networkPanel) {

        ArrayList<JCheckBoxMenuItem> checkBoxes = new ArrayList<JCheckBoxMenuItem>();
        NetworkActionManager actionManager = networkPanel.getActionManager();

        JMenuBar returnMenu = new JMenuBar();
        
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(actionManager.getCutAction());
        editMenu.add(actionManager.getCopyAction());
        editMenu.add(actionManager.getPasteAction());
        editMenu.addSeparator();
        editMenu.add(actionManager.getClearAction());
        JMenu selectionMenu = new JMenu("Select");
        selectionMenu.add(actionManager.getSelectAllAction());
        selectionMenu.add(actionManager.getSelectAllWeightsAction());
        selectionMenu.add(actionManager.getSelectAllNeuronsAction());
        selectionMenu.add(actionManager.getSelectIncomingWeightsAction());
        selectionMenu.add(actionManager.getSelectOutgoingWeightsAction());
        editMenu.add(selectionMenu);
        editMenu.addSeparator();
        editMenu.add(actionManager.getGroupAction());
        editMenu.add(actionManager.getUngroupAction());
        editMenu.addSeparator();
        editMenu.add(networkPanel.createAlignMenu());
        editMenu.add(networkPanel.createSpacingMenu());
        editMenu.addSeparator();
        JMenu clampMenu = new JMenu("Clamp");
        JCheckBoxMenuItem cbW = actionManager.getClampWeightsMenuItem();
        checkBoxes.add(cbW);
        clampMenu.add(cbW);
        JCheckBoxMenuItem cbN = actionManager.getClampNeuronsMenuItem();
        checkBoxes.add(cbN);
        clampMenu.add(cbN);
        editMenu.add(clampMenu);
        editMenu.addSeparator();
        editMenu.add(actionManager.getShowIOInfoMenuItem());
        editMenu.add(actionManager.getSetAutoZoomMenuItem());
        editMenu.addSeparator();
        editMenu.add(actionManager.getSetNeuronPropertiesAction());
        editMenu.add(actionManager.getSetSynapsePropertiesAction());
        returnMenu.add(editMenu);

        JMenu insertMenu = new JMenu("Insert");
        insertMenu.add(networkPanel.createNewNetworkMenu());
        insertMenu.add(actionManager.getNewNeuronAction());
        returnMenu.add(insertMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.add(actionManager.getShowEditToolBarMenuItem());
        viewMenu.add(actionManager.getShowMainToolBarMenuItem());
        viewMenu.add(actionManager.getShowClampToolBarMenuItem());
        viewMenu.add(actionManager.getShowGUIAction());
        viewMenu.add(actionManager.getShowNodesAction());
        returnMenu.add(viewMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(actionManager.getShowHelpAction());
        returnMenu.add(helpMenu);
        
        return returnMenu;
    }


}
