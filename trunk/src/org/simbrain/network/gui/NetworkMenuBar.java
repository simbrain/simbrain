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

/**
 * Menu bar that appears inside of JPanel; for use in Applets.
 *
 * TODO: Add file menu; fix help menu.
 * TODO: Reduce duplicated code between this and NetworkPanelDesktop.
 * TODO: Rename so that the fact that this is only used in applets is clear.
 */
public class NetworkMenuBar {

    public static JMenuBar getAppletMenuBar(final NetworkPanel networkPanel) {

        ArrayList<JCheckBoxMenuItem> checkBoxes = new ArrayList<JCheckBoxMenuItem>();
        NetworkActionManager actionManager = networkPanel.getActionManager();

        JMenuBar returnMenu = new JMenuBar();

// TODO: The code below works locally but won't work in an unsigned applet.
//          I have not figured out how to sign applets properly yet so I'm not adding this for now
        
//        JMenu fileMenu = new JMenu("File");
//        JMenuItem openItem = new JMenuItem("Open");
//        openItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                SFileChooser chooser = new SFileChooser(".", "Open Network");
//                chooser.addExtension("xml");
//                File theFile = chooser.showOpenDialog();
//                if (theFile != null) {
//                    RootNetwork newNetwork;
//                    try {
//                        newNetwork = (RootNetwork) RootNetwork.getXStream().fromXML(new FileInputStream(theFile));
//                        newNetwork.setParent(networkPanel.getRootNetwork().getParent());
//                        networkPanel.clearPanel();
//                        networkPanel.setRootNetwork(newNetwork);
//                        networkPanel.syncToModel();
//                        networkPanel.repaint();
//                    } catch (FileNotFoundException e1) {
//                        e1.printStackTrace();
//                    }
//                }
//            }
//        });
//        fileMenu.add(openItem);
//        JMenuItem saveItem = new JMenuItem("Save");
//        saveItem.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                SFileChooser chooser = new SFileChooser(".", "Save Network");
//                chooser.addExtension("xml");
//                File theFile = chooser.showSaveDialog();
//                if (theFile != null) {
//                    try {
//                        RootNetwork.getXStream().toXML(networkPanel.getRootNetwork(), new FileOutputStream(theFile));
//                    } catch (FileNotFoundException e1) {
//                        e1.printStackTrace();
//                    }                    
//                }
//            }
//        });
//        fileMenu.add(saveItem);
//        returnMenu.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.add(actionManager.getCutAction());
        editMenu.add(actionManager.getCopyAction());
        editMenu.add(actionManager.getPasteAction());
        editMenu.addSeparator();
        editMenu.add(actionManager.getDeleteAction());
        JMenu selectionMenu = new JMenu("Select");
        selectionMenu.add(actionManager.getSelectAllAction());
        selectionMenu.add(actionManager.getSelectAllWeightsAction());
        selectionMenu.add(actionManager.getSelectAllNeuronsAction());
        selectionMenu.add(actionManager.getSelectIncomingWeightsAction());
        selectionMenu.add(actionManager.getSelectOutgoingWeightsAction());
        editMenu.add(selectionMenu);
        editMenu.addSeparator();
        editMenu.add(actionManager.getZeroSelectedObjectsAction());
        editMenu.addSeparator();
        editMenu.addSeparator();
        editMenu.add(actionManager.getGroupAction());
        editMenu.add(actionManager.getUngroupAction());
        editMenu.addSeparator();
        editMenu.add(networkPanel.createAlignMenu());
        editMenu.add(networkPanel.createSpacingMenu());
        editMenu.addSeparator();
        editMenu.add(networkPanel.createClampMenu());
        editMenu.addSeparator();
        //editMenu.add(actionManager.getShowIOInfoMenuItem());
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
        JMenu toolbarMenu = new JMenu("Toolbars");
        toolbarMenu.add(actionManager.getShowMainToolBarMenuItem());
        toolbarMenu.add(actionManager.getShowRunToolBarMenuItem());
        toolbarMenu.add(actionManager.getShowEditToolBarMenuItem());
        toolbarMenu.add(actionManager.getShowClampToolBarMenuItem());
        viewMenu.add(toolbarMenu);
        viewMenu.addSeparator();
        viewMenu.add(actionManager.getShowGUIAction());
        viewMenu.add(actionManager.getShowNodesAction());
        returnMenu.add(viewMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(actionManager.getShowHelpAction());
        returnMenu.add(helpMenu);

        return returnMenu;
    }

}
