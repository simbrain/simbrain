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

import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.EditMode;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.actions.neuron.AddNeuronsAction;
import org.simbrain.network.gui.dialogs.NetworkDialog;
import org.simbrain.network.gui.nodes.NeuronGroupNode;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.SynapseGroupNodeFull;
import org.simbrain.network.gui.nodes.SynapseGroupNodeSimple;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.SimbrainPreferences.PropertyNotFoundException;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.genericframe.GenericJInternalFrame;

/**
 * Extension of Network Panel with functions used in a desktop setting. This is
 * separate mainly so that network panel can also be used in applets, which do
 * not talk to other workspace components. Thus in this subclass static values
 * are updated using simbrain preferences, and special menus with component
 * level references are built.
 * <p>
 * Applet menus are handled in {@link org.simbrain.network.gui.NetworkMenuBar}
 *
 * @author Jeff Yoshimi
 */
public class NetworkPanelDesktop extends NetworkPanel {

    /** Reference to Desktop Component. */
    NetworkDesktopComponent component;

    /**
     * Construct the desktop extension of network panel. The main thing
     *
     * @param component the component level representation of the desktop
     * @param Network the neural network model
     */
    public NetworkPanelDesktop(final NetworkDesktopComponent component,
            final Network Network) {
        super(Network);
        this.component = component;

        // By default the network's run toolbar is not shown in the context of a
        // workspace,because it is confusingly similar to the workspace run /
        // stop buttons
        this.getRunToolBar().setVisible(false);

        // Set relevant network settings to the user's current preferences
        applyUserPrefsToNetwork();

    }

    /**
     * Push settings from user preferences to Simbrain objects. Note that the
     * relevant static fields have default values (e.g in NetworkPanel) for
     * cases where NetworkPanel is used by itself.
     */
    public void applyUserPrefsToNetwork() {
        try {
            NetworkPanel.setBackgroundColor(new Color(SimbrainPreferences
                    .getInt("networkBackgroundColor")));
            EditMode.setWandRadius(SimbrainPreferences
                    .getInt("networkWandRadius"));
            NetworkPanel.setNudgeAmount(SimbrainPreferences
                    .getDouble("networkNudgeAmount"));
            Network.setSynapseVisibilityThreshold(SimbrainPreferences
                    .getInt("networkSynapseVisibilityThreshold"));
            NeuronNode.setHotColor(SimbrainPreferences
                    .getFloat("networkHotNodeColor"));
            NeuronNode.setCoolColor(SimbrainPreferences
                    .getFloat("networkCoolNodeColor"));
            NeuronNode.setSpikingColor(new Color(SimbrainPreferences
                    .getInt("networkSpikingColor")));
            SynapseNode.setExcitatoryColor(new Color(SimbrainPreferences
                    .getInt("networkExcitatorySynapseColor")));
            SynapseNode.setInhibitoryColor(new Color(SimbrainPreferences
                    .getInt("networkInhibitorySynapseColor")));
            SynapseNode.setZeroWeightColor(new Color(SimbrainPreferences
                    .getInt("networkZeroWeightColor")));
            SynapseNode.setMaxDiameter(SimbrainPreferences
                    .getInt("networkSynapseMaxSize"));
            SynapseNode.setMinDiameter(SimbrainPreferences
                    .getInt("networkSynapseMinSize"));
            resetColors();
        } catch (PropertyNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create and return a new Edit menu for this Network panel.
     *
     * @return a new Edit menu for this Network panel
     */
    JMenu createEditMenu() {

        JMenu editMenu = new JMenu("Edit");

        editMenu.add(actionManager.getCutAction());
        editMenu.add(actionManager.getCopyAction());
        editMenu.add(actionManager.getPasteAction());
        editMenu.add(actionManager.getDeleteAction());
        editMenu.addSeparator();
        editMenu.add(actionManager.getClearSourceNeuronsAction());
        editMenu.add(actionManager.getSetSourceNeuronsAction());
        editMenu.add(actionManager.getConnectionMenu());
        editMenu.add(actionManager.getAddSynapseGroupAction());
        editMenu.addSeparator();
        editMenu.add(actionManager.getRandomizeObjectsAction());
        editMenu.add(actionManager.getShowAdjustSynapsesDialog());
        editMenu.addSeparator();
        editMenu.add(actionManager.getLayoutMenu());
        editMenu.add(actionManager.getGroupMenu());
        editMenu.addSeparator();
        editMenu.add(createAlignMenu());
        editMenu.add(createSpacingMenu());
        editMenu.addSeparator();
        editMenu.add(actionManager.getSetNeuronPropertiesAction());
        editMenu.add(actionManager.getSetSynapsePropertiesAction());
        editMenu.addSeparator();
        editMenu.add(createSelectionMenu());

        return editMenu;
    }

    /**
     * Create and return a new Insert menu for this Network panel.
     *
     * @return a new Insert menu for this Network panel
     */
    JMenu createInsertMenu() {

        JMenu insertMenu = new JMenu("Insert");
        insertMenu.add(actionManager.getNewNeuronAction());
        insertMenu.addSeparator();
        insertMenu.add(new AddNeuronsAction(this));
        insertMenu.addSeparator();
        insertMenu.add(actionManager.getNewGroupMenu());
        insertMenu.add(actionManager.getNewNetworkMenu());
        insertMenu.addSeparator();
        insertMenu.add(actionManager.getTestInputAction());
        insertMenu.add(actionManager.getShowWeightMatrixAction());
        return insertMenu;
    }

    /**
     * Create and return a new View menu for this Network panel.
     *
     * @return a new View menu for this Network panel
     */
    JMenu createViewMenu() {
        JMenu viewMenu = new JMenu("View");
        JMenu toolbarMenu = new JMenu("Toolbars");
        toolbarMenu.add(actionManager.getShowMainToolBarMenuItem());
        toolbarMenu.add(actionManager.getShowRunToolBarMenuItem());
        toolbarMenu.add(actionManager.getShowEditToolBarMenuItem());
        viewMenu.add(toolbarMenu);
        viewMenu.addSeparator();
        viewMenu.add(actionManager.getSetAutoZoomMenuItem());
        viewMenu.addSeparator();
        // viewMenu.add(actionManager.getShowGUIAction());
        viewMenu.add(actionManager.getShowPrioritiesAction());
        // viewMenu.add(actionManager.getShowNetworkHierarchyPanel());
        viewMenu.add(actionManager.getShowWeightsAction());

        return viewMenu;
    }

    /**
     * Create a selection JMenu.
     *
     * @return the selection menu.
     */
    public JMenu createSelectionMenu() {
        JMenu selectionMenu = new JMenu("Select");
        selectionMenu.add(actionManager.getSelectAllAction());
        selectionMenu.add(actionManager.getSelectAllWeightsAction());
        selectionMenu.add(actionManager.getSelectAllNeuronsAction());
        selectionMenu.add(actionManager.getSelectIncomingWeightsAction());
        selectionMenu.add(actionManager.getSelectOutgoingWeightsAction());
        return selectionMenu;
    }

    /**
     * Create and return a new Help menu for this Network panel.
     *
     * @return a new Help menu for this Network panel
     */
    public JMenu createHelpMenu() {
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Network.html");
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(helpAction);
        return helpMenu;
    }

    /**
     * This version of network dialog allows user to set User Preferences.
     *
     * @param networkPanel
     *
     * @return superclass version of network dialog, with User Preferences
     */
    public NetworkDialog getNetworkDialog(final NetworkPanel networkPanel) {
        return new DesktopNetworkDialog(networkPanel);
    }

    /**
     * This version of a NeuronNode has a coupling context menu.
     *
     * @param net network panel
     * @param neuron neuron to show in Gui
     * @return desktop version of NeuronNode, with context menu
     */
    @Override
    public NeuronNode createNeuronNode(final NetworkPanel net,
            final Neuron neuron) {
        return new NeuronNodeDesktop(component.getWorkspaceComponent(), net,
                neuron);
    }

    /**
     * This version of a NeuronGroupNode has a coupling context menu.
     */
    @Override
    public NeuronGroupNode createNeuronGroupNode(final NeuronGroup neuronGroup) {
        NeuronGroupNode node = super.createNeuronGroupNode(neuronGroup);
        return new NeuronGroupNodeDesktop(component.getWorkspaceComponent(),
                this, node);
    }

    /**
     * This version of a NeuronGroupNode has a coupling context menu.
     */
    @Override
    public SynapseGroupNodeFull createSynapseGroupFull(
            final SynapseGroup synapseGroup) {
        return new SynapseGroupNodeDesktopFull(
                component.getWorkspaceComponent(), this, synapseGroup);
    }

    /**
     * This version of a NeuronGroupNode has a coupling context menu.
     */
    @Override
    public SynapseGroupNodeSimple createSynapseGroupSimple(
            final SynapseGroup synapseGroup) {
        return new SynapseGroupNodeDesktopSimple(
                component.getWorkspaceComponent(), this, synapseGroup);
    }

    /**
     * This version adds the script menu.
     *
     * @return the context menu with script menu added
     */
    public JPopupMenu createContextMenu() {
        JPopupMenu contextMenu = super.createContextMenu();

        // Add script menus
        contextMenu.addSeparator();
        contextMenu.add(NetworkScriptMenu.getNetworkScriptMenu(this));

        return contextMenu;
    }

    @Override
    public GenericFrame displayPanel(JPanel panel, String title) {
        GenericJInternalFrame frame = new GenericJInternalFrame();
        frame.setContentPane(panel);
        component.getDesktop().addInternalFrame(frame);
        frame.pack();
        frame.setResizable(true);
        frame.setMaximizable(true);
        frame.setIconifiable(true);
        frame.setClosable(true);
        frame.setTitle(title);
        frame.setVisible(true);
        return frame;
    }

}
