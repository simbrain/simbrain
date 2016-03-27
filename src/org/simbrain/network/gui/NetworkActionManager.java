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

import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JToggleButton;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.OneToOne;
import org.simbrain.network.connections.Radial;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.gui.actions.GroupAction;
import org.simbrain.network.gui.actions.SetTextPropertiesAction;
import org.simbrain.network.gui.actions.ShowDebugAction;
import org.simbrain.network.gui.actions.ShowLayoutDialogAction;
import org.simbrain.network.gui.actions.ShowTrainerAction;
import org.simbrain.network.gui.actions.TestInputAction;
import org.simbrain.network.gui.actions.UngroupAction;
import org.simbrain.network.gui.actions.connection.ApplyConnectionAction;
import org.simbrain.network.gui.actions.connection.ClearSourceNeurons;
import org.simbrain.network.gui.actions.connection.SetSourceNeurons;
import org.simbrain.network.gui.actions.edit.AlignHorizontalAction;
import org.simbrain.network.gui.actions.edit.AlignVerticalAction;
import org.simbrain.network.gui.actions.edit.CopyAction;
import org.simbrain.network.gui.actions.edit.CutAction;
import org.simbrain.network.gui.actions.edit.DeleteAction;
import org.simbrain.network.gui.actions.edit.EditRandomizerPropertiesAction;
import org.simbrain.network.gui.actions.edit.PasteAction;
import org.simbrain.network.gui.actions.edit.RandomizeObjectsAction;
import org.simbrain.network.gui.actions.edit.SelectionEditModeAction;
import org.simbrain.network.gui.actions.edit.SetAutoZoomAction;
import org.simbrain.network.gui.actions.edit.SpaceHorizontalAction;
import org.simbrain.network.gui.actions.edit.SpaceVerticalAction;
import org.simbrain.network.gui.actions.edit.TextEditModeAction;
import org.simbrain.network.gui.actions.edit.WandEditModeAction;
import org.simbrain.network.gui.actions.edit.ZeroSelectedObjectsAction;
import org.simbrain.network.gui.actions.edit.ZoomToFitPageAction;
import org.simbrain.network.gui.actions.modelgroups.AddGroupAction;
import org.simbrain.network.gui.actions.modelgroups.NewNeuronGroupAction;
import org.simbrain.network.gui.actions.network.IterateNetworkAction;
import org.simbrain.network.gui.actions.network.RunNetworkAction;
import org.simbrain.network.gui.actions.network.ShowNetworkHierarchyPanel;
import org.simbrain.network.gui.actions.network.ShowNetworkPreferencesAction;
import org.simbrain.network.gui.actions.network.ShowNetworkUpdaterDialog;
import org.simbrain.network.gui.actions.network.StopNetworkAction;
import org.simbrain.network.gui.actions.neuron.NewActivityGeneratorAction;
import org.simbrain.network.gui.actions.neuron.NewNeuronAction;
import org.simbrain.network.gui.actions.neuron.SetNeuronPropertiesAction;
import org.simbrain.network.gui.actions.neuron.ShowPrioritiesAction;
import org.simbrain.network.gui.actions.selection.SelectAllAction;
import org.simbrain.network.gui.actions.selection.SelectAllNeuronsAction;
import org.simbrain.network.gui.actions.selection.SelectAllWeightsAction;
import org.simbrain.network.gui.actions.selection.SelectIncomingWeightsAction;
import org.simbrain.network.gui.actions.selection.SelectOutgoingWeightsAction;
import org.simbrain.network.gui.actions.synapse.AddSynapseGroupAction;
import org.simbrain.network.gui.actions.synapse.SetSynapsePropertiesAction;
import org.simbrain.network.gui.actions.synapse.ShowAdjustConnectivityDialog;
import org.simbrain.network.gui.actions.synapse.ShowAdjustSynapsesDialog;
import org.simbrain.network.gui.actions.synapse.ShowWeightMatrixAction;
import org.simbrain.network.gui.actions.synapse.ShowWeightsAction;
import org.simbrain.network.gui.actions.toolbar.ShowEditToolBarAction;
import org.simbrain.network.gui.actions.toolbar.ShowMainToolBarAction;
import org.simbrain.network.gui.actions.toolbar.ShowRunToolBarAction;
import org.simbrain.network.gui.dialogs.group.NeuronGroupCreationDialog;
import org.simbrain.network.gui.dialogs.network.BackpropCreationDialog;
import org.simbrain.network.gui.dialogs.network.CompetitiveGroupCreationDialog;
import org.simbrain.network.gui.dialogs.network.CompetitiveNetworkCreationDialog;
import org.simbrain.network.gui.dialogs.network.ESNCreationDialog;
import org.simbrain.network.gui.dialogs.network.FeedForwardCreationDialog;
import org.simbrain.network.gui.dialogs.network.HopfieldCreationDialog;
import org.simbrain.network.gui.dialogs.network.LMSCreationDialog;
import org.simbrain.network.gui.dialogs.network.SOMGroupCreationDialog;
import org.simbrain.network.gui.dialogs.network.SOMNetworkCreationDialog;
import org.simbrain.network.gui.dialogs.network.SRNCreationDialog;
import org.simbrain.network.gui.dialogs.network.WTACreationDialog;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.resource.ResourceManager;

/**
 * Network action manager.
 * <p>
 * This class contains references to all the actions for a NetworkPanel. In some
 * cases, related actions are grouped together, see e.g.
 * <code>getNetworkModeActions()</code>.
 * </p>
 */
public final class NetworkActionManager {

    /** Selection edit mode action. */
    private final Action selectionEditModeAction;

    /** Text edit mode action. */
    private final Action textEditModeAction;

    /** Wand edit mode action. */
    private final Action wandEditModeAction;

    /** Auto-zoom action. */
    private final Action zoomToFitPageAction;

    /** New neuron action. */
    private final Action newNeuronAction;

    /** Clear neurons action. */
    private final Action zeroSelectedObjectsAction;

    /** Randomize objects action. */
    private final Action randomizeObjectsAction;

    /** Select all action. */
    private final Action selectAllAction;

    /** Clear action. */
    private final Action deleteAction;

    /** Copy action. */
    private final Action copyAction;

    /** Cut action. */
    private final Action cutAction;

    /** Paste action. */
    private final Action pasteAction;

    /** Iterate network action. */
    private final Action iterateNetworkAction;

    /** Run network action. */
    private final Action runNetworkAction;

    /** Stop network action. */
    private final Action stopNetworkAction;

    /** Show debug. */
    private final Action showDebugAction;

    /** Show network preferences action. */
    private final Action showNetworkPreferencesAction;

    /** Align vertical action. */
    private final Action alignVerticalAction;

    /** Align horizontal action. */
    private final Action alignHorizontalAction;

    /** Space vertical action. */
    private final Action spaceVerticalAction;

    /** Space horizontal action. */
    private final Action spaceHorizontalAction;

    /** Set auto zoom action. */
    private final JToggleButton setAutoZoomAction;

    /** Set neuron properties action. */
    private final Action setNeuronPropertiesAction;

    /** Set synapse properties action. */
    private final Action setSynapsePropertiesAction;

    /** Select all weights action. */
    private final Action selectAllWeightsAction;

    /** Select all neurons action. */
    private final Action selectAllNeuronsAction;

    /** Determines if main tool bar is to be shown. */
    private final Action showMainToolBarAction;

    /** Determines if edit tool bar is to be shown. */
    private final Action showEditToolBarAction;

    /** Determines if run tool bar is to be shown. */
    private final Action showRunToolBarAction;

    /** Clears the source neurons for neuron connections. */
    private Action clearSourceNeuronsAction;

    /** Sets the source neurons for neuron connections. */
    private Action setSourceNeuronsAction;

    /** Create a neuron group. */
    private final Action neuronGroupAction;

    /** Create an activity generator. */
    private final Action activityGeneratorAction;

    /** Connection types. */
    private Action allToAll, allToAllSelf, fixedFanout, fixedFanoutSelf,
            oneToOne, oneToOneSelf, radial, radialSelf, sparse, sparseSelf;

    /** Layout types. */
    private Action gridLayout, hexagonalLayout, lineLayout;

    /** Whether weights should be shown or not. */
    private final JCheckBoxMenuItem showWeightsAction;

    /** Whether neuron priorities should be shown or not. */
    private final JCheckBoxMenuItem showPrioritiesAction;

    /** Whether network hierarchy inspector should be shown or not. */
    private final JCheckBoxMenuItem showNetworkHierarchyAction;

    /** Select all incoming synapses. */
    private final Action selectIncomingWeightsAction;

    /** Select all outgoing synapses. */
    private final Action selectOutgoingWeightsAction;

    /** Sets the text object properties. */
    private final Action setTextPropertiesAction;

    /** Groups selected nodes together. */
    private final Action groupAction;

    /** Ungroups selected nodes. */
    private final Action ungroupAction;

    /** Shows a weight matrix panel. */
    private final Action showWeightMatrixAction;

    /** Shows a trainer dialog. */
    private final Action showTrainerAction;

    /** Shows dialog to edit randomizer properties. */
    private final Action setRandomizerPropertiesAction;

    /** Shows dialog to adjust group of synapses. */
    private final Action showAdjustSynapsesDialog;

    /** Shows dialog to adjust connectivity. */
    private final Action showAdjustConnectivityDialog;

    /** Shows dialog to adjust updater. */
    private final Action showUpdaterDialog;

    /** Test inputs to selected neurons. */
    private Action testInputAction;

    /** Add a synapse group between neuron groups. */
    private final Action addSynapseGroupAction;

    /** Reference to NetworkPanel. */
    private final NetworkPanel networkPanel;

    /**
     * Create a new network action manager for the specified network panel.
     *
     * @param networkPanel
     *            networkPanel, must not be null
     */
    NetworkActionManager(final NetworkPanel networkPanel) {

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        selectionEditModeAction = new SelectionEditModeAction(networkPanel);
        textEditModeAction = new TextEditModeAction(networkPanel);
        wandEditModeAction = new WandEditModeAction(networkPanel);

        newNeuronAction = new NewNeuronAction(networkPanel);
        activityGeneratorAction = new NewActivityGeneratorAction(networkPanel);
        zeroSelectedObjectsAction = new ZeroSelectedObjectsAction(networkPanel);
        randomizeObjectsAction = new RandomizeObjectsAction(networkPanel);

        selectAllAction = new SelectAllAction(networkPanel);

        zoomToFitPageAction = new ZoomToFitPageAction(networkPanel);

        deleteAction = new DeleteAction(networkPanel);
        copyAction = new CopyAction(networkPanel);
        cutAction = new CutAction(networkPanel);
        pasteAction = new PasteAction(networkPanel);

        iterateNetworkAction = new IterateNetworkAction(networkPanel);
        runNetworkAction = new RunNetworkAction(networkPanel);
        stopNetworkAction = new StopNetworkAction(networkPanel);

        showDebugAction = new ShowDebugAction(networkPanel);

        showNetworkPreferencesAction = new ShowNetworkPreferencesAction(
                networkPanel);

        alignVerticalAction = new AlignVerticalAction(networkPanel);
        alignHorizontalAction = new AlignHorizontalAction(networkPanel);
        spaceVerticalAction = new SpaceVerticalAction(networkPanel);
        spaceHorizontalAction = new SpaceHorizontalAction(networkPanel);

        showMainToolBarAction = new ShowMainToolBarAction(networkPanel);
        showEditToolBarAction = new ShowEditToolBarAction(networkPanel);
        showRunToolBarAction = new ShowRunToolBarAction(networkPanel);

        showWeightsAction = new JCheckBoxMenuItem(new ShowWeightsAction(
                networkPanel));
        showPrioritiesAction = new JCheckBoxMenuItem(new ShowPrioritiesAction(
                networkPanel));
        showNetworkHierarchyAction = new JCheckBoxMenuItem(
                new ShowNetworkHierarchyPanel(networkPanel));

        setAutoZoomAction = new JToggleButton(new SetAutoZoomAction(networkPanel));
        setAutoZoomAction.setSelectedIcon(ResourceManager.getImageIcon("AutoZoomOn.png"));
        setAutoZoomAction.setIcon(ResourceManager.getImageIcon("AutoZoomOff.png"));
        setAutoZoomAction.setBorder(BorderFactory.createEtchedBorder());
        setAutoZoomAction.setSelected(networkPanel.getAutoZoomMode());

        selectAllWeightsAction = new SelectAllWeightsAction(networkPanel);
        selectAllNeuronsAction = new SelectAllNeuronsAction(networkPanel);
        selectIncomingWeightsAction = new SelectIncomingWeightsAction(
                networkPanel);
        selectOutgoingWeightsAction = new SelectOutgoingWeightsAction(
                networkPanel);

        setNeuronPropertiesAction = new SetNeuronPropertiesAction(networkPanel);
        setSynapsePropertiesAction = new SetSynapsePropertiesAction(
                networkPanel);
        setTextPropertiesAction = new SetTextPropertiesAction(networkPanel);

        allToAll = new ApplyConnectionAction(networkPanel, new AllToAll(),
                "All to all");
        oneToOne = new ApplyConnectionAction(networkPanel, new OneToOne(),
                "One-to-one");
        radial = new ApplyConnectionAction(networkPanel, new Radial(), "Radial");
        sparse = new ApplyConnectionAction(networkPanel, new Sparse(), "Sparse");

        gridLayout = new ShowLayoutDialogAction(new GridLayout(), networkPanel);
        hexagonalLayout = new ShowLayoutDialogAction(new HexagonalGridLayout(),
                networkPanel);
        lineLayout = new ShowLayoutDialogAction(new LineLayout(), networkPanel);

        setSourceNeuronsAction = new SetSourceNeurons(networkPanel);
        clearSourceNeuronsAction = new ClearSourceNeurons(networkPanel);

        groupAction = new GroupAction(networkPanel);
        ungroupAction = new UngroupAction(networkPanel,
                networkPanel.getViewGroupNode());

        neuronGroupAction = new NewNeuronGroupAction(networkPanel);

        showWeightMatrixAction = new ShowWeightMatrixAction(networkPanel);
        showTrainerAction = new ShowTrainerAction(networkPanel);

        setRandomizerPropertiesAction = new EditRandomizerPropertiesAction(
                networkPanel);
        showAdjustSynapsesDialog = new ShowAdjustSynapsesDialog(networkPanel);
        showAdjustConnectivityDialog = new ShowAdjustConnectivityDialog(
                networkPanel);
        showUpdaterDialog = new ShowNetworkUpdaterDialog(networkPanel);
        testInputAction = new TestInputAction(networkPanel);
        addSynapseGroupAction = new AddSynapseGroupAction(networkPanel);
    }

    /**
     * Return the text edit mode action.
     *
     * @return the text edit mode action
     */
    public Action getTextEditModeAction() {
        return textEditModeAction;
    }

    /**
     * Return the wand edit mode action.
     *
     * @return the wand edit mode action
     */
    public Action getWandEditModeAction() {
        return wandEditModeAction;
    }

    /**
     * Return a list of network mode actions.
     *
     * @return a list of network mode actions
     */
    public List<Action> getNetworkModeActions() {
        return Arrays.asList(new Action[] { selectionEditModeAction,
                textEditModeAction, wandEditModeAction });
    }

    /**
     * Return a list of network control actions.
     *
     * @return a list of network control actions
     */
    public List<Action> getNetworkControlActions() {
        return Arrays
                .asList(new Action[] { runNetworkAction, stopNetworkAction });
    }

    /**
     * Return clipboard actions.
     *
     * @return a list of clipboard actions
     */
    public List<Action> getClipboardActions() {
        return Arrays
                .asList(new Action[] { copyAction, cutAction, pasteAction });
    }

    /**
     * Return a list of network editing actions.
     *
     * @return a list of network editing actions
     */
    public List<Action> getNetworkEditingActions() {
        return Arrays.asList(new Action[] { newNeuronAction, deleteAction });
    }

    /**
     * @return a list of layout actions
     */
    public List<Action> getLayoutActions() {
        return Arrays.asList(new Action[] { gridLayout, hexagonalLayout,
                lineLayout });
    }

    /**
     * Returns a menu of model group actions.
     *
     * @return the group menu
     */
    public JMenu getGroupMenu() {
        // TODO: Fix menu. Alone with one right now.
        JMenu groupMenu = new JMenu("Group");
        groupMenu.add(neuronGroupAction);
        return groupMenu;
    }

    /**
     * Returns a menu for setting neuron connections.
     *
     * @return the connection menu
     */
    public JMenu getLayoutMenu() {
        JMenu layoutMenu = new JMenu("Layout");
        for (Action action : getLayoutActions()) {
            layoutMenu.add(action);
        }
        return layoutMenu;
    }

    /**
     * @return connection actions
     */
    public List<Action> getConnectionActions() {
        return Arrays
                .asList(new Action[] { allToAll, oneToOne, sparse });
    }

    /**
     * (Not current used).
     *
     * @return self connect actions
     */
    public List<Action> getSelfConnectionActions() {
        return Arrays.asList(new Action[] { allToAllSelf, fixedFanoutSelf,
                oneToOneSelf, sparseSelf });
    }

    /**
     * Returns a menu for setting neuron connections.
     *
     * @return the connection menu
     */
    public JMenu getConnectionMenu() {
        // Connection menu
        JMenu connectionsMenu = new JMenu("Connect Neurons");
        for (Action action : getConnectionActions()) {
            connectionsMenu.add(action);
        }
        return connectionsMenu;
    }

    /**
     * @return a list of new networks that can be inserted
     */
    public List<Action> getNewNetworkActions() {
        return Arrays
                .asList(new Action[] {
                        new AddGroupAction(networkPanel,
                                BackpropCreationDialog.class, "Backprop"),
                        new AddGroupAction(networkPanel,
                                CompetitiveNetworkCreationDialog.class,
                                "Competitive Network"),
                        new AddGroupAction(networkPanel,
                                ESNCreationDialog.class, "Echo State Network"),
                        new AddGroupAction(networkPanel,
                                FeedForwardCreationDialog.class,
                                "Feed Forward Network"),
                        new AddGroupAction(networkPanel,
                                HopfieldCreationDialog.class, "Hopfield"),
                        new AddGroupAction(networkPanel,
                                LMSCreationDialog.class,
                                "LMS (Least Mean Squares)"),
                        new AddGroupAction(networkPanel,
                                SOMNetworkCreationDialog.class, "SOM Network"),
                        new AddGroupAction(networkPanel,
                                SRNCreationDialog.class,
                                "SRN (Simple Recurrent Network)") });
    }

    /**
     * @return a list of the new neuron groups that can be inserted
     */
    public List<Action> getNewGroupActions() {
        return Arrays
                .asList(new Action[] {
                        new AddGroupAction(networkPanel,
                                NeuronGroupCreationDialog.class,
                                "(Bare) Neuron Group"),
                        new AddGroupAction(networkPanel,
                                CompetitiveGroupCreationDialog.class,
                                "Competitive (Group only)"),
                        new AddGroupAction(networkPanel,
                                SOMGroupCreationDialog.class,
                                "SOM (Group only)"),
                        new AddGroupAction(networkPanel,
                                WTACreationDialog.class,
                                "WTA (Winner take all)") });
    }

    /**
     * Return a JMenu for creating new networks.
     *
     * @return the new JMenu.
     */
    public JMenu getNewNetworkMenu() {
        JMenu ret = new JMenu("Insert Network");
        for (Action action : getNewNetworkActions()) {
            ret.add(action);
        }
        return ret;
    }

    /**
     * Return a JMenu for creating new neuron groups.
     *
     * @return the new JMenu.
     */
    public JMenu getNewGroupMenu() {
        JMenu ret = new JMenu("Insert Neuron Group");
        for (Action action : getNewGroupActions()) {
            ret.add(action);
        }
        return ret;
    }

    /**
     * Return the new neuron action.
     *
     * @return the new neuron action
     */
    public Action getNewNeuronAction() {
        return newNeuronAction;
    }
    
    /**
     * Return the new activity generator
     *
     * @return the new activity generator
     */
    public Action getNewActivityGeneratorAction() {
        return activityGeneratorAction;
    }

    /**
     * Return the clear neurons action.
     *
     * @return the clear neurons action
     */
    public Action getZeroSelectedObjectsAction() {
        return zeroSelectedObjectsAction;
    }

    /**
     * Return the randomize objects action.
     *
     * @return the randomize objects action
     */
    public Action getRandomizeObjectsAction() {
        return randomizeObjectsAction;
    }

    /**
     * Return the select all action.
     *
     * @return the select all action
     */
    public Action getSelectAllAction() {
        return selectAllAction;
    }

    /**
     * Return the iterate network action.
     *
     * @return the iterate network action
     */
    public Action getIterateNetworkAction() {
        return iterateNetworkAction;
    }

    /**
     * @return Returns the showDebugAction.
     */
    public Action getShowDebugAction() {
        return showDebugAction;
    }

    /**
     * Return the run network action.
     *
     * @return the run network action
     */
    public Action getRunNetworkAction() {
        return runNetworkAction;
    }

    /**
     * Return the stop network action.
     *
     * @return the stop network action
     */
    public Action getStopNetworkAction() {
        return stopNetworkAction;
    }

    /**
     * Return the show network preferences action.
     *
     * @return the network preferences action
     */
    public Action getShowNetworkPreferencesAction() {
        return showNetworkPreferencesAction;
    }

    /**
     * Return the clear action.
     *
     * @return the clear action
     */
    public Action getDeleteAction() {
        return deleteAction;
    }

    /**
     * Return the copy action.
     *
     * @return the copy action
     */
    public Action getCopyAction() {
        return copyAction;
    }

    /**
     * Return the cut action.
     *
     * @return the cut action
     */
    public Action getCutAction() {
        return cutAction;
    }

    /**
     * Return the paste action.
     *
     * @return the paste action
     */
    public Action getPasteAction() {
        return pasteAction;
    }

    /**
     * Return the align horizontal action.
     *
     * @return the align horizontal action
     */
    public Action getAlignHorizontalAction() {
        return alignHorizontalAction;
    }

    /**
     * Return the align vertical action.
     *
     * @return the align vertical action
     */
    public Action getAlignVerticalAction() {
        return alignVerticalAction;
    }

    /**
     * Return the space horizontal action.
     *
     * @return the space horizontal action
     */
    public Action getSpaceHorizontalAction() {
        return spaceHorizontalAction;
    }

    /**
     * Return the space vertical action.
     *
     * @return the space vertical action
     */
    public Action getSpaceVerticalAction() {
        return spaceVerticalAction;
    }

    /**
     * Return the set auto zoom check box menu item.
     *
     * @return the set auto zoom check box menu item
     */
    public JToggleButton getSetAutoZoomToggleButton() {
        return setAutoZoomAction;
    }

    /**
     * Return the neuron properties action.
     *
     * @return the neuron properties action
     */
    public Action getSetNeuronPropertiesAction() {
        return setNeuronPropertiesAction;
    }

    /**
     * Return the synapse properties action.
     *
     * @return the synapse properties action
     */
    public Action getSetSynapsePropertiesAction() {
        return setSynapsePropertiesAction;
    }

    /**
     * Return the select all neurons action.
     *
     * @return the select all neurons action
     */
    public Action getSelectAllNeuronsAction() {
        return selectAllNeuronsAction;
    }

    /**
     * Return the select all weights action.
     *
     * @return the select all weights action.
     */
    public Action getSelectAllWeightsAction() {
        return selectAllWeightsAction;
    }

    /**
     * Return the show edit tool bar menu item.
     *
     * @return the show edit tool bar menu item
     */
    public JCheckBoxMenuItem getShowEditToolBarMenuItem() {
        JCheckBoxMenuItem actionWrapper = new JCheckBoxMenuItem(
                showEditToolBarAction);
        actionWrapper.setSelected(networkPanel.getEditToolBar().isVisible());
        return actionWrapper;
    }

    /**
     * Return the show main tool bar menu item.
     *
     * @return the show main tool bar menu item
     */
    public JCheckBoxMenuItem getShowMainToolBarMenuItem() {
        JCheckBoxMenuItem actionWrapper = new JCheckBoxMenuItem(
                showMainToolBarAction);
        actionWrapper.setSelected(networkPanel.getMainToolBar().isVisible());
        return actionWrapper;
    }

    /**
     * Return the run clamp tool bar menu item.
     *
     * @return the run clamp tool bar menu item
     */
    public JCheckBoxMenuItem getShowRunToolBarMenuItem() {
        JCheckBoxMenuItem actionWrapper = new JCheckBoxMenuItem(
                showRunToolBarAction);
        actionWrapper.setSelected(networkPanel.getRunToolBar().isVisible());
        return actionWrapper;
    }

    /**
     * Return the set source neurons action.
     *
     * @return set source neurons action
     */
    public Action getSetSourceNeuronsAction() {
        return setSourceNeuronsAction;
    }

    /**
     * @return the clearSourceNeuronsAction
     */
    public Action getClearSourceNeuronsAction() {
        return clearSourceNeuronsAction;
    }

    /**
     * @return the showNodesAction
     */
    public JCheckBoxMenuItem getShowWeightsAction() {
        showWeightsAction.setSelected(networkPanel.getWeightsVisible());
        return showWeightsAction;
    }

    /**
     * @return the selectIncomingWeightsAction
     */
    public Action getSelectIncomingWeightsAction() {
        return selectIncomingWeightsAction;
    }

    /**
     * @return the selectOutgoingWeightsAction
     */
    public Action getSelectOutgoingWeightsAction() {
        return selectOutgoingWeightsAction;
    }

    /**
     * @return the setTextPropertiesAction.
     */
    public Action getSetTextPropertiesAction() {
        return setTextPropertiesAction;
    }

    /**
     * @return the ungroupAction
     */
    public Action getUngroupAction() {
        return ungroupAction;
    }

    /**
     * @return the groupAction
     */
    public Action getGroupAction() {
        return groupAction;
    }

    /**
     * @return the showPrioritiesAction
     */
    public JCheckBoxMenuItem getShowPrioritiesAction() {
        return showPrioritiesAction;
    }

    /**
     * @return the showNetworkHierarchyAction
     */
    public JCheckBoxMenuItem getShowNetworkHierarchyPanel() {
        return showNetworkHierarchyAction;
    }

    /**
     * @return the show weight matrix action
     */
    public Action getShowWeightMatrixAction() {
        return showWeightMatrixAction;
    }

    /**
     * @return the show weight matrix action
     */
    public Action getShowTrainerAction() {
        return showTrainerAction;
    }

    /**
     * @return the selectionEditModeAction
     */
    public Action getSelectionEditModeAction() {
        return selectionEditModeAction;
    }

    /**
     * @return the getSetRandomPropsAction
     */
    public Action getSetRandomizerPropsAction() {
        return setRandomizerPropertiesAction;
    }

    /**
     * @return the showAdjustSynapsesDialog
     */
    public Action getShowAdjustSynapsesDialog() {
        return showAdjustSynapsesDialog;
    }

    /**
     * @return the showUpdaterDialog
     */
    public Action getShowUpdaterDialog() {
        return showUpdaterDialog;
    }

    /**
     * @return the showAdjustConnectivityDialog
     */
    public Action getShowAdjustConnectivityDialog() {
        return showAdjustConnectivityDialog;
    }

    /**
     * @return the testInputAction
     */
    public Action getTestInputAction() {
        return testInputAction;
    }

    /**
     * @return the addSynapseGroupAction
     */
    public Action getAddSynapseGroupAction() {
        return addSynapseGroupAction;
    }

    /**
     * @return the zoomToFitPageAction
     */
    public Action getZoomToFitPageAction() {
        return zoomToFitPageAction;
    }
}
