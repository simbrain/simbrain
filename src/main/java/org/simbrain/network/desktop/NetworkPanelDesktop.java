///*
// * Part of Simbrain--a java-based neural network kit
// * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 2 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package org.simbrain.network.desktop;
//
//import org.simbrain.network.core.Network;
//import org.simbrain.network.core.Neuron;
//import org.simbrain.network.core.Synapse;
//import org.simbrain.network.groups.NeuronCollection;
//import org.simbrain.network.groups.NeuronGroup;
//import org.simbrain.network.gui.EditMode;
//import org.simbrain.network.gui.NetworkPanel;
//import org.simbrain.network.gui.actions.ShowLayoutDialogAction;
//import org.simbrain.network.gui.actions.TestInputAction;
//import org.simbrain.network.gui.actions.connection.ClearSourceNeurons;
//import org.simbrain.network.gui.actions.connection.SetSourceNeurons;
//import org.simbrain.network.gui.actions.dl4j.AddMultiLayerNet;
//import org.simbrain.network.gui.actions.edit.*;
//import org.simbrain.network.gui.actions.dl4j.AddNeuronArrayAction;
//import org.simbrain.network.gui.actions.modelgroups.NeuronCollectionAction;
//import org.simbrain.network.gui.actions.neuron.AddNeuronsAction;
//import org.simbrain.network.gui.actions.neuron.NewNeuronAction;
//import org.simbrain.network.gui.actions.neuron.SetNeuronPropertiesAction;
//import org.simbrain.network.gui.actions.neuron.ShowPrioritiesAction;
//import org.simbrain.network.gui.actions.selection.*;
//import org.simbrain.network.gui.actions.synapse.*;
//import org.simbrain.network.gui.actions.toolbar.ShowEditToolBarAction;
//import org.simbrain.network.gui.actions.toolbar.ShowMainToolBarAction;
//import org.simbrain.network.gui.actions.toolbar.ShowRunToolBarAction;
//import org.simbrain.network.gui.dialogs.NetworkDialog;
//import org.simbrain.network.gui.dialogs.group.NeuronGroupDialog;
//import org.simbrain.network.gui.dialogs.group.SynapseGroupDialog;
//import org.simbrain.network.gui.dialogs.neuron.NeuronDialog;
//import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;
//import org.simbrain.network.gui.nodes.NeuronGroupNode;
//import org.simbrain.network.gui.nodes.NeuronNode;
//import org.simbrain.network.gui.nodes.SynapseGroupInteractionBox;
//import org.simbrain.network.gui.nodes.SynapseNode;
//import org.simbrain.util.SimbrainPreferences;
//import org.simbrain.util.StandardDialog;
//import org.simbrain.util.genericframe.GenericFrame;
//import org.simbrain.util.genericframe.GenericJInternalFrame;
//import org.simbrain.util.widgets.ShowHelpAction;
//import org.simbrain.workspace.AttributeContainer;
//import org.simbrain.workspace.Workspace;
//import org.simbrain.workspace.gui.CouplingMenu;
//import org.simbrain.workspace.gui.SimbrainDesktop;
//
//import javax.swing.*;
//import java.awt.*;
//import java.util.Collection;
//
///**
// * Extension of Network Panel with functions used in a desktop setting. This is
// * separate mainly so that network panel can also be used in applets, which do
// * not talk to other workspace components. Thus in this subclass static values
// * are updated using simbrain preferences, and special menus with component
// * level references are built.
// * <p>
// * Applet menus are handled in {@link org.simbrain.network.gui.NetworkMenuBar}
// *
// * @author Jeff Yoshimi
// */
//public class NetworkPanelDesktop extends NetworkPanel {
//
//    /**
//     * Reference to Desktop Component.
//     */
//    NetworkDesktopComponent component;
//
//    /**
//     * Construct the desktop extension of network panel. The main thing
//     *
//     * @param component the component level representation of the desktop
//     * @param Network   the neural network model
//     */
//    public NetworkPanelDesktop(final NetworkDesktopComponent component, final Network Network) {
//        super(Network);
//        this.component = component;
//
//        // By default the network's run toolbar is not shown in the context of a
//        // workspace,because it is confusingly similar to the workspace run /
//        // stop buttons
//        this.getRunToolBar().setVisible(false);
//
//        // Set relevant network settings to the user's current preferences
//        applyUserPrefsToNetwork();
//
//    }
//
//    /**
//     * Push settings from user preferences to Simbrain objects. Note that the
//     * relevant static fields have default values (e.g in NetworkPanel) for
//     * cases where NetworkPanel is used by itself.
//     */
//    public void applyUserPrefsToNetwork() {
//        NetworkPanel.setBackgroundColor(new Color(SimbrainPreferences.getInt("networkBackgroundColor")));
//        EditMode.setWandRadius(SimbrainPreferences.getInt("networkWandRadius"));
//        NetworkPanel.setNudgeAmount(SimbrainPreferences.getDouble("networkNudgeAmount"));
//        Network.setSynapseVisibilityThreshold(SimbrainPreferences.getInt("networkSynapseVisibilityThreshold"));
//        NeuronNode.setHotColor(SimbrainPreferences.getFloat("networkHotNodeColor"));
//        NeuronNode.setCoolColor(SimbrainPreferences.getFloat("networkCoolNodeColor"));
//        NeuronNode.setSpikingColor(new Color(SimbrainPreferences.getInt("networkSpikingColor")));
//        SynapseNode.setExcitatoryColor(new Color(SimbrainPreferences.getInt("networkExcitatorySynapseColor")));
//        SynapseNode.setInhibitoryColor(new Color(SimbrainPreferences.getInt("networkInhibitorySynapseColor")));
//        SynapseNode.setZeroWeightColor(new Color(SimbrainPreferences.getInt("networkZeroWeightColor")));
//        SynapseNode.setMaxDiameter(SimbrainPreferences.getInt("networkSynapseMaxSize"));
//        SynapseNode.setMinDiameter(SimbrainPreferences.getInt("networkSynapseMinSize"));
//        resetColors();
//    }
//
//    /**
//     * Create and return a new Edit menu for this Network panel.
//     *
//     * @return a new Edit menu for this Network panel
//     */
//    JMenu createEditMenu() {
//
//        JMenu editMenu = new JMenu("Edit");
//        editMenu.add(actionManager.getAction(CutAction.class));
//        editMenu.add(actionManager.getAction(CopyAction.class));
//        editMenu.add(actionManager.getAction(PasteAction.class));
//        editMenu.add(actionManager.getAction(DeleteAction.class));
//        editMenu.addSeparator();
//        editMenu.add(actionManager.getAction(ClearSourceNeurons.class));
//        editMenu.add(actionManager.getAction(SetSourceNeurons.class));
//        editMenu.add(actionManager.getConnectionMenu());
//        editMenu.add(actionManager.getAction(AddSynapseGroupAction.class));
//        editMenu.addSeparator();
//        editMenu.add(actionManager.getAction(RandomizeObjectsAction.class));
//        editMenu.add(actionManager.getAction(ShowAdjustSynapsesDialog.class));
//        editMenu.addSeparator();
//        editMenu.add(actionManager.getAction(ShowLayoutDialogAction.class));
//        editMenu.addSeparator();
//        editMenu.add(actionManager.getAction(NeuronCollectionAction.class));
//        editMenu.addSeparator();
//        editMenu.add(createAlignMenu());
//        editMenu.add(createSpacingMenu());
//        editMenu.addSeparator();
//        editMenu.add(actionManager.getAction(SetNeuronPropertiesAction.class));
//        editMenu.add(actionManager.getAction(SetSynapsePropertiesAction.class));
//        editMenu.addSeparator();
//        editMenu.add(createSelectionMenu());
//
//        return editMenu;
//    }
//
//    /**
//     * Create and return a new Insert menu for this Network panel.
//     *
//     * @return a new Insert menu for this Network panel
//     */
//    JMenu createInsertMenu() {
//
//        JMenu insertMenu = new JMenu("Insert");
//        insertMenu.add(actionManager.getAction(NewNeuronAction.class));
//        insertMenu.add(actionManager.getAction(NeuronGroup.class));
//        insertMenu.addSeparator();
//        insertMenu.add(new AddNeuronsAction(this));
//        insertMenu.add(new AddNeuronArrayAction(this));
//        insertMenu.add(new AddMultiLayerNet(this));
//        insertMenu.addSeparator();
//        insertMenu.add(actionManager.getNewNetworkMenu());
//        insertMenu.addSeparator();
//        insertMenu.add(actionManager.getAction(TestInputAction.class));
//        insertMenu.add(actionManager.getAction(ShowWeightMatrixAction.class));
//        return insertMenu;
//    }
//
//    /**
//     * Create and return a new View menu for this Network panel.
//     *
//     * @return a new View menu for this Network panel
//     */
//    JMenu createViewMenu() {
//        JMenu viewMenu = new JMenu("View");
//        JMenu toolbarMenu = new JMenu("Toolbars");
//        toolbarMenu.add(actionManager.getMenuItem(ShowRunToolBarAction.class,
//                getRunToolBar().isVisible()));
//        toolbarMenu.add(actionManager.getMenuItem(ShowMainToolBarAction.class,
//                getMainToolBar().isVisible()));
//        toolbarMenu.add(actionManager.getMenuItem(ShowEditToolBarAction.class,
//                getEditToolBar().isVisible()));
//
//        viewMenu.add(toolbarMenu);
//        viewMenu.addSeparator();
//        viewMenu.add(actionManager.getMenuItem(ShowPrioritiesAction.class,
//                getPrioritiesVisible()));
//        viewMenu.add(actionManager.getMenuItem(ShowWeightsAction.class,
//                getWeightsVisible()));
//
//        return viewMenu;
//    }
//
//    /**
//     * Create a selection JMenu.
//     *
//     * @return the selection menu.
//     */
//    public JMenu createSelectionMenu() {
//        JMenu selectionMenu = new JMenu("Select");
//        selectionMenu.add(actionManager.getAction(SelectAllAction.class));
//        selectionMenu.add(actionManager.getAction(SelectAllWeightsAction.class));
//        selectionMenu.add(actionManager.getAction(SelectAllNeuronsAction.class));
//        selectionMenu.add(actionManager.getAction(SelectIncomingWeightsAction.class));
//        selectionMenu.add(actionManager.getAction(SelectOutgoingWeightsAction.class));
//        return selectionMenu;
//    }
//
//    /**
//     * Create and return a new Help menu for this Network panel.
//     *
//     * @return a new Help menu for this Network panel
//     */
//    public JMenu createHelpMenu() {
//        ShowHelpAction helpAction = new ShowHelpAction("Pages/Network.html");
//        JMenu helpMenu = new JMenu("Help");
//        helpMenu.add(helpAction);
//        return helpMenu;
//    }
//
//    /**
//     * This version of network dialog allows user to set User Preferences.
//     *
//     * @param networkPanel network panel
//     * @return superclass version of network dialog, with User Preferences
//     */
//    public NetworkDialog getNetworkDialog(final NetworkPanel networkPanel) {
//        return new DesktopNetworkDialog(networkPanel);
//    }
//
//    /**
//     * This version adds the script menu.
//     *
//     * @return the context menu with script menu added
//     */
//    public JPopupMenu createNetworkContextMenu() {
//        JPopupMenu contextMenu = super.createNetworkContextMenu();
//
//        // Add script menus
//        contextMenu.addSeparator();
//        contextMenu.add(NetworkScriptMenu.getNetworkScriptMenu(this));
//
//        return contextMenu;
//    }
//
//    @Override
//    public GenericFrame displayPanel(JPanel panel, String title) {
//        GenericJInternalFrame frame = new GenericJInternalFrame();
//        frame.setContentPane(panel);
//        component.getDesktop().addInternalFrame(frame);
//        frame.pack();
//        frame.setResizable(true);
//        frame.setMaximizable(true);
//        frame.setIconifiable(true);
//        frame.setClosable(true);
//        frame.setTitle(title);
//        frame.setVisible(true);
//        return frame;
//    }
//
//    @Override
//    public JPopupMenu getNeuronContextMenu(Neuron neuron) {
//        JPopupMenu contextMenu = super.getNeuronContextMenu(neuron);
//        // Add coupling menus
//        Workspace workspace = component.getWorkspaceComponent().getWorkspace();
//        if (getSelectedNodes(NeuronNode.class).size() == 1) {
//            contextMenu.addSeparator();
//            CouplingMenu menu = new CouplingMenu(component.getWorkspaceComponent(), neuron);
//            contextMenu.add(menu);
//        }
//        return contextMenu;
//    }
//
//    @Override
//    public JPopupMenu getSynapseContextMenu(Synapse synapse) {
//        JPopupMenu contextMenu = new JPopupMenu();
//
//        contextMenu.add(new CutAction(this));
//        contextMenu.add(new CopyAction(this));
//        contextMenu.add(new PasteAction(this));
//        contextMenu.addSeparator();
//
//        contextMenu.add(new DeleteAction(this));
//        contextMenu.addSeparator();
//
//        contextMenu.add(new SetSynapsePropertiesAction(this));
//        // Add coupling menus
//        Workspace workspace = component.getWorkspaceComponent().getWorkspace();
//        if (getSelectedNodes(SynapseNode.class).size() == 1) {
//            contextMenu.addSeparator();
//            CouplingMenu menu = new CouplingMenu(component.getWorkspaceComponent(), synapse);
//            contextMenu.add(menu);
//        }
//        return contextMenu;
//    }
//
//    /**
//     * Creates a modeless version of the neuron group dialog relative to the
//     * SimbrainDesktop.
//     */
//    @Override
//    public StandardDialog getNeuronGroupDialog(final NeuronGroupNode node) {
//        // TODO: Why is there this map in SimbrainDesktop? Shouldn't there
//        // only ever be one Simbrain Desktop?
//        SimbrainDesktop current = SimbrainDesktop.getInstances().values().iterator().next();
//        NeuronGroupDialog dialog = new NeuronGroupDialog(this, node.getNeuronGroup());
//        dialog.setAsDoneDialog();
//        dialog.setModalityType(Dialog.ModalityType.MODELESS);
//        return dialog;
//    }
//
//    /**
//     * Creates a modeless version of the synapse group dialog relative to the
//     * SimbrainDesktop.
//     */
//    @Override
//    public StandardDialog getSynapseGroupDialog(SynapseGroupInteractionBox sgib) {
//        // TODO: Why is there this map in SimbrainDesktop? Shouldn't there
//        // only ever be one Simbrain Desktop?
//        SimbrainDesktop current = SimbrainDesktop.getInstances().values().iterator().next();
//        SynapseGroupDialog sgd = SynapseGroupDialog.createSynapseGroupDialog(this, sgib.getSynapseGroup());
//        sgd.setModalityType(Dialog.ModalityType.MODELESS);
//        return sgd;
//    }
//
//    @Override
//    public StandardDialog getSynapseDialog(Collection<SynapseNode> sns) {
//        SimbrainDesktop current = SimbrainDesktop.getInstances().values().iterator().next();
//        SynapseDialog dialog = SynapseDialog.createSynapseDialog(sns, current.getFrame());
//        dialog.setModalityType(Dialog.ModalityType.MODELESS);
//        return dialog;
//    }
//
//    /**
//     * Creates and displays the neuron properties dialog.
//     */
//    @Override
//    public void showSelectedNeuronProperties() {
//        NeuronDialog dialog = getNeuronDialog();
//        dialog.pack();
//        dialog.setLocationRelativeTo(null);
//        dialog.setVisible(true);
//    }
//
//    /**
//     * Creates and displays the synapse properties dialog.
//     */
//    @Override
//    public void showSelectedSynapseProperties() {
//        StandardDialog dialog = getSynapseDialog(getSelectedNodes(SynapseNode.class));
//        dialog.pack();
//        dialog.setLocationRelativeTo(null);
//        dialog.setVisible(true);
//    }
//
//    @Override
//    public JMenu getCouplingMenu(AttributeContainer container) {
//        if (component.getWorkspaceComponent() != null) {
//            CouplingMenu menu = new CouplingMenu(component.getWorkspaceComponent(), container);
//            return menu;
//        }
//        return null;
//    }
//
//}
