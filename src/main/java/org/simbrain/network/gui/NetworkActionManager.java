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
//package org.simbrain.network.gui;
//
//import org.simbrain.network.connections.*;
//import org.simbrain.network.groups.NeuronCollection;
//import org.simbrain.network.groups.NeuronGroup;
//import org.simbrain.network.groups.SynapseGroup;
//import org.simbrain.network.gui.actions.*;
//import org.simbrain.network.gui.actions.connection.ApplyConnectionAction;
//import org.simbrain.network.gui.actions.connection.ClearSourceNeurons;
//import org.simbrain.network.gui.actions.connection.SetSourceNeurons;
//import org.simbrain.network.gui.actions.edit.*;
//import org.simbrain.network.gui.actions.modelgroups.AddGroupAction;
//import org.simbrain.network.gui.actions.modelgroups.NeuronCollectionAction;
//import org.simbrain.network.gui.actions.network.*;
//import org.simbrain.network.gui.actions.neuron.NewNeuronAction;
//import org.simbrain.network.gui.actions.neuron.SetNeuronPropertiesAction;
//import org.simbrain.network.gui.actions.neuron.ShowPrioritiesAction;
//import org.simbrain.network.gui.actions.selection.*;
//import org.simbrain.network.gui.actions.synapse.*;
//import org.simbrain.network.gui.actions.toolbar.ShowEditToolBarAction;
//import org.simbrain.network.gui.actions.toolbar.ShowMainToolBarAction;
//import org.simbrain.network.gui.actions.toolbar.ShowRunToolBarAction;
//import org.simbrain.network.gui.dialogs.group.NeuronGroupDialog;
//import org.simbrain.network.gui.dialogs.network.*;
//
//import javax.swing.*;
//import java.util.*;
//
///**
// * Contains instances of actions, some of them which maintain state, for common
// * reference across Simbrain. They are accessed using the name of the action class
// * or a related class. Simply inspect the code if not sure.
// */
//public final class NetworkActionManager {
//
//    /**
//     * Reference to NetworkPanel.
//     */
//    private final NetworkPanel networkPanel;
//
//    /**
//     * Map storing class -> action associations
//     */
//    private transient Map<Class, AbstractAction> actions = new HashMap<>();
//
//    /**
//     * Create a new network action manager for the specified network panel.
//     *
//     * @param networkPanel networkPanel, must not be null
//     */
//    NetworkActionManager(final NetworkPanel networkPanel) {
//
//        this.networkPanel = networkPanel;
//
//        if (networkPanel == null) {
//            throw new IllegalArgumentException("networkPanel must not be null");
//        }
//
//        // Could have used reflection but it would only make things simpler on this page and the code below is easy to
//        // follow, and this class is easy to use externally
//
//        actions.put(ZoomToFitPageAction.class, new ZoomToFitPageAction(networkPanel));
//        actions.put(NewNeuronAction.class, new NewNeuronAction(networkPanel));
//        actions.put(ClearNodeActivationsAction.class, new ClearNodeActivationsAction(networkPanel));
//        actions.put(RandomizeObjectsAction.class, new RandomizeObjectsAction(networkPanel));
//        actions.put(SelectAllAction.class, new SelectAllAction(networkPanel));
//        actions.put(DeleteAction.class, new DeleteAction(networkPanel));
//        actions.put(CopyAction.class, new CopyAction(networkPanel));
//        actions.put(CutAction.class, new CutAction(networkPanel));
//        actions.put(PasteAction.class, new PasteAction(networkPanel));
//        actions.put(IterateNetworkAction.class, new IterateNetworkAction(networkPanel));
//        actions.put(RunNetworkAction.class, new RunNetworkAction(networkPanel));
//        actions.put(StopNetworkAction.class, new StopNetworkAction(networkPanel));
//        actions.put(ShowDebugAction.class, new ShowDebugAction(networkPanel));
//        actions.put(ShowNetworkPreferencesAction.class, new ShowNetworkPreferencesAction(networkPanel));
//        actions.put(AlignHorizontalAction.class, new AlignHorizontalAction(networkPanel));
//        actions.put(AlignVerticalAction.class, new AlignVerticalAction(networkPanel));
//        actions.put(SpaceVerticalAction.class, new SpaceVerticalAction(networkPanel));
//        actions.put(SpaceHorizontalAction.class, new SpaceHorizontalAction(networkPanel));
//        actions.put(SetNeuronPropertiesAction.class, new SetNeuronPropertiesAction(networkPanel));
//        actions.put(SetSynapsePropertiesAction.class, new SetSynapsePropertiesAction(networkPanel));
//        actions.put(SelectAllWeightsAction.class, new SelectAllWeightsAction(networkPanel));
//        actions.put(SelectAllNeuronsAction.class, new SelectAllNeuronsAction(networkPanel));
//        actions.put(ShowMainToolBarAction.class, new ShowMainToolBarAction(networkPanel));
//        actions.put(ShowEditToolBarAction.class, new ShowEditToolBarAction(networkPanel));
//        actions.put(ShowRunToolBarAction.class, new ShowRunToolBarAction(networkPanel));
//        actions.put(SelectIncomingWeightsAction.class, new SelectIncomingWeightsAction(networkPanel));
//        actions.put(SelectOutgoingWeightsAction.class, new SelectOutgoingWeightsAction(networkPanel));
//        actions.put(SetTextPropertiesAction.class, new SetTextPropertiesAction(networkPanel));
//        actions.put(ShowWeightMatrixAction.class, new ShowWeightMatrixAction(networkPanel));
//        actions.put(ShowAdjustSynapsesDialog.class, new ShowAdjustSynapsesDialog(networkPanel));
//        actions.put(ShowAdjustConnectivityDialog.class, new ShowAdjustConnectivityDialog(networkPanel));
//        actions.put(ShowNetworkUpdaterDialog.class, new ShowNetworkUpdaterDialog(networkPanel));
//        actions.put(TestInputAction.class, new TestInputAction(networkPanel));
//        actions.put(ShowLayoutDialogAction.class, new ShowLayoutDialogAction(networkPanel));
//        actions.put(SetSourceNeurons.class, new SetSourceNeurons(networkPanel));
//        actions.put(ClearSourceNeurons.class, new ClearSourceNeurons(networkPanel));
//        actions.put(ShowPrioritiesAction.class, new ShowPrioritiesAction(networkPanel));
//        actions.put(ShowWeightsAction.class, new ShowWeightsAction(networkPanel));
//        actions.put(SelectionEditModeAction.class, new SelectionEditModeAction(networkPanel));
//        actions.put(TextEditModeAction.class, new TextEditModeAction(networkPanel));
//        actions.put(WandEditModeAction.class, new WandEditModeAction(networkPanel));
//
//        actions.put(AllToAll.class, new ApplyConnectionAction(networkPanel, new AllToAll(), "All to all"));
//        actions.put(OneToOne.class, new ApplyConnectionAction(networkPanel, new OneToOne(), "One-to-one"));
//        actions.put(RadialGaussian.class, new ApplyConnectionAction(networkPanel, new RadialGaussian(), "Radial (Gaussian)"));
//        actions.put(RadialSimple.class, new ApplyConnectionAction(networkPanel, new RadialSimple(), "Radial (Simple)"));
//        actions.put(Sparse.class, new ApplyConnectionAction(networkPanel, new Sparse(), "Sparse"));
//
//        actions.put(NeuronGroup.class, new AddGroupAction(networkPanel,
//                NeuronGroupDialog.class, "Add Neuron Group"));
//        actions.put(AddSynapseGroupAction.class, new AddSynapseGroupAction(networkPanel));
//        actions.put(NeuronCollectionAction.class, new NeuronCollectionAction(networkPanel));
//
//
//    }
//
//    public List<Action> getNetworkModeActions() {
//        return Arrays.asList(new Action[]{actions.get(SelectionEditModeAction.class), actions.get(TextEditModeAction.class),
//                actions.get(WandEditModeAction.class)});
//    }
//
//    public List<Action> getNetworkControlActions() {
//        return Arrays.asList(new Action[]{actions.get(RunNetworkAction.class), actions.get(StopNetworkAction.class)});
//    }
//
//    public List<Action> getClipboardActions() {
//        return Arrays.asList(new Action[]{actions.get(CopyAction.class), actions.get(CutAction.class),
//                actions.get(PasteAction.class)});
//    }
//
//    public List<Action> getNetworkEditingActions() {
//        return Arrays.asList(new Action[]{actions.get(NewNeuronAction.class), actions.get(DeleteAction.class)});
//    }
//
//    public JMenu getConnectionMenu() {
//        JMenu cm = new JMenu("Connect Neurons");
//        cm.add(getMenuItem(AllToAll.class));
//        cm.add(getMenuItem(OneToOne.class));
//        cm.add(getMenuItem(RadialGaussian.class));
//        cm.add(getMenuItem(RadialSimple.class));
//        cm.add(getMenuItem(Sparse.class));
//        return cm;
//    }
//
//    public List<Action> getNewNetworkActions() {
//        return Arrays.asList(new Action[]{new AddGroupAction(networkPanel, BackpropCreationDialog.class, "Backprop"),
//                new AddGroupAction(networkPanel, CompetitiveCreationDialog.class, "Competitive Network"),
//                new AddGroupAction(networkPanel, FeedForwardCreationDialog.class, "Feed Forward Network"),
//                new AddGroupAction(networkPanel, HopfieldCreationDialog.class, "Hopfield"),
//                new AddGroupAction(networkPanel, LMSCreationDialog.class, "LMS (Least Mean Squares)"),
//                new AddGroupAction(networkPanel, SOMCreationDialog.class, "SOM Network"),
//                new AddGroupAction(networkPanel, SRNCreationDialog.class, "SRN (Simple Recurrent Network)")});
//    }
//
//    public JMenu getNewNetworkMenu() {
//        JMenu ret = new JMenu("Insert Network");
//        for (Action action : getNewNetworkActions()) {
//            ret.add(action);
//        }
//        return ret;
//    }
//
//    /**
//     * Returns on action based on its associated name (see above)
//     */
//    public Action getAction(Class clazz) {
//        return actions.get(clazz);
//    }
//
//    /**
//     * Gets an action within a {@link JCheckBoxMenuItem}.  Can just be
//     * used as a menu item without using the checkbox.
//     */
//    public JCheckBoxMenuItem getMenuItem(Class clazz) {
//        return new JCheckBoxMenuItem(getAction(clazz));
//    }
//
//    /**
//     * Gets an action within a {@link JCheckBoxMenuItem}.
//     *
//     * @param clazz    class associated with action
//     * @param checked whether it should initially be checked.
//     */
//    public JCheckBoxMenuItem getMenuItem(Class clazz, boolean checked) {
//        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(getAction(clazz));
//        menuItem.setSelected(checked);
//        return menuItem;
//    }
//
//}
