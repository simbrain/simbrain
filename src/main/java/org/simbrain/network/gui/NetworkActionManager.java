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

import org.simbrain.network.connections.*;
import org.simbrain.network.gui.actions.*;
import org.simbrain.network.gui.actions.connection.ApplyConnectionAction;
import org.simbrain.network.gui.actions.connection.ClearSourceNeurons;
import org.simbrain.network.gui.actions.connection.SetSourceNeurons;
import org.simbrain.network.gui.actions.edit.*;
import org.simbrain.network.gui.actions.modelgroups.AddGroupAction;
import org.simbrain.network.gui.actions.modelgroups.NeuronCollectionAction;
import org.simbrain.network.gui.actions.network.*;
import org.simbrain.network.gui.actions.neuron.NewNeuronAction;
import org.simbrain.network.gui.actions.neuron.SetNeuronPropertiesAction;
import org.simbrain.network.gui.actions.neuron.ShowPrioritiesAction;
import org.simbrain.network.gui.actions.selection.*;
import org.simbrain.network.gui.actions.synapse.*;
import org.simbrain.network.gui.actions.toolbar.ShowEditToolBarAction;
import org.simbrain.network.gui.actions.toolbar.ShowMainToolBarAction;
import org.simbrain.network.gui.actions.toolbar.ShowRunToolBarAction;
import org.simbrain.network.gui.dialogs.group.NeuronGroupDialog;
import org.simbrain.network.gui.dialogs.network.*;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Network action manager.
 * <p>
 * This class contains references to all the actions for a NetworkPanel. In some
 * cases, related actions are grouped together, see e.g.
 * <code>getNetworkModeActions()</code>.
 * </p>
 */
public final class NetworkActionManager {

    /**
     * Selection edit mode action.
     */
    private final Action selectionEditModeAction;

    /**
     * Text edit mode action.
     */
    private final Action textEditModeAction;

    /**
     * Wand edit mode action.
     */
    private final Action wandEditModeAction;

    /**
     * Auto-zoom action.
     */
    private final Action zoomToFitPageAction;

    /**
     * New neuron action.
     */
    private final Action newNeuronAction;

    /**
     * Clear neurons action.
     */
    private final Action clearNodesAction;

    /**
     * Randomize objects action.
     */
    private final Action randomizeObjectsAction;

    /**
     * Select all action.
     */
    private final Action selectAllAction;

    /**
     * Clear action.
     */
    private final Action deleteAction;

    /**
     * Copy action.
     */
    private final Action copyAction;

    /**
     * Cut action.
     */
    private final Action cutAction;

    /**
     * Paste action.
     */
    private final Action pasteAction;

    /**
     * Iterate network action.
     */
    private final Action iterateNetworkAction;

    /**
     * Run network action.
     */
    private final Action runNetworkAction;

    /**
     * Stop network action.
     */
    private final Action stopNetworkAction;

    /**
     * Show debug.
     */
    private final Action showDebugAction;

    /**
     * Show network preferences action.
     */
    private final Action showNetworkPreferencesAction;

    /**
     * Align vertical action.
     */
    private final AlignVerticalAction alignVerticalAction;

    /**
     * Align horizontal action.
     */
    private final Action alignHorizontalAction;

    /**
     * Space vertical action.
     */
    private final Action spaceVerticalAction;

    /**
     * Space horizontal action.
     */
    private final Action spaceHorizontalAction;

    /**
     * Set auto zoom action.
     */
    private final JToggleButton setAutoZoomAction;

    /**
     * Set neuron properties action.
     */
    private final Action setNeuronPropertiesAction;

    /**
     * Set synapse properties action.
     */
    private final Action setSynapsePropertiesAction;

    /**
     * Select all weights action.
     */
    private final Action selectAllWeightsAction;

    /**
     * Select all neurons action.
     */
    private final Action selectAllNeuronsAction;

    /**
     * Determines if main tool bar is to be shown.
     */
    private final Action showMainToolBarAction;

    /**
     * Determines if edit tool bar is to be shown.
     */
    private final Action showEditToolBarAction;

    /**
     * Determines if run tool bar is to be shown.
     */
    private final Action showRunToolBarAction;

    /**
     * Clears the source neurons for neuron connections.
     */
    private Action clearSourceNeuronsAction;

    /**
     * Sets the source neurons for neuron connections.
     */
    private Action setSourceNeuronsAction;

    /**
     * Create a neuron group.
     */
    private final Action neuronCollectionAction;

    /**
     * Connection types.
     */
    private Action allToAll, oneToOne, radial, radialSimple, radialSelf, sparse;

    /**
     * Layout types.
     */
    private Action layoutNeurons;

    /**
     * Whether weights should be shown or not.
     */
    private final JCheckBoxMenuItem showWeightsAction;

    /**
     * Whether neuron priorities should be shown or not.
     */
    private final JCheckBoxMenuItem showPrioritiesAction;

    /**
     * Select all incoming synapses.
     */
    private final Action selectIncomingWeightsAction;

    /**
     * Select all outgoing synapses.
     */
    private final Action selectOutgoingWeightsAction;

    /**
     * Sets the text object properties.
     */
    private final Action setTextPropertiesAction;

    /**
     * Groups selected nodes together.
     */
    private final Action groupAction;

    /**
     * Ungroups selected nodes.
     */
    private final Action ungroupAction;

    /**
     * Shows a weight matrix panel.
     */
    private final Action showWeightMatrixAction;

    /**
     * Shows a trainer dialog.
     */
    private final Action showTrainerAction;

    /**
     * Shows dialog to adjust group of synapses.
     */
    private final Action showAdjustSynapsesDialog;

    /**
     * Shows dialog to adjust connectivity.
     */
    private final Action showAdjustConnectivityDialog;

    /**
     * Shows dialog to adjust updater.
     */
    private final Action showUpdaterDialog;

    /**
     * Test inputs to selected neurons.
     */
    private Action testInputAction;

    /**
     * Add a synapse group between neuron groups.
     */
    private final Action addSynapseGroupAction;

    /**
     * Reference to NetworkPanel.
     */
    private final NetworkPanel networkPanel;

    private transient Map<String, AbstractAction> actions = new HashMap<>();

    /**
     * Create a new network action manager for the specified network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    NetworkActionManager(final NetworkPanel networkPanel) {

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        actions.put("NewGroup", new AddGroupAction(networkPanel,
                NeuronGroupDialog.class, "Add Neuron Group"));

        selectionEditModeAction = new SelectionEditModeAction(networkPanel);
        textEditModeAction = new TextEditModeAction(networkPanel);
        wandEditModeAction = new WandEditModeAction(networkPanel);

        newNeuronAction = new NewNeuronAction(networkPanel);
        clearNodesAction = new ClearNodeActivationsAction(networkPanel);
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

        showNetworkPreferencesAction = new ShowNetworkPreferencesAction(networkPanel);

        alignVerticalAction = new AlignVerticalAction(networkPanel);
        alignHorizontalAction = new AlignHorizontalAction(networkPanel);
        spaceVerticalAction = new SpaceVerticalAction(networkPanel);
        spaceHorizontalAction = new SpaceHorizontalAction(networkPanel);

        showMainToolBarAction = new ShowMainToolBarAction(networkPanel);
        showEditToolBarAction = new ShowEditToolBarAction(networkPanel);
        showRunToolBarAction = new ShowRunToolBarAction(networkPanel);

        showWeightsAction = new JCheckBoxMenuItem(new ShowWeightsAction(networkPanel));
        showPrioritiesAction = new JCheckBoxMenuItem(new ShowPrioritiesAction(networkPanel));

        setAutoZoomAction = new ToggleAutoZoom(networkPanel);

        selectAllWeightsAction = new SelectAllWeightsAction(networkPanel);
        selectAllNeuronsAction = new SelectAllNeuronsAction(networkPanel);
        selectIncomingWeightsAction = new SelectIncomingWeightsAction(networkPanel);
        selectOutgoingWeightsAction = new SelectOutgoingWeightsAction(networkPanel);

        setNeuronPropertiesAction = new SetNeuronPropertiesAction(networkPanel);
        setSynapsePropertiesAction = new SetSynapsePropertiesAction(networkPanel);
        setTextPropertiesAction = new SetTextPropertiesAction(networkPanel);

        allToAll = new ApplyConnectionAction(networkPanel, new AllToAll(), "All to all");
        oneToOne = new ApplyConnectionAction(networkPanel, new OneToOne(), "One-to-one");
        radial = new ApplyConnectionAction(networkPanel, new RadialGaussian(), "Radial (Gaussian)");
        radialSimple = new ApplyConnectionAction(networkPanel, new RadialSimple(), "Radial (Simple)");
        sparse = new ApplyConnectionAction(networkPanel, new Sparse(), "Sparse");

        layoutNeurons = new ShowLayoutDialogAction(networkPanel);

        setSourceNeuronsAction = new SetSourceNeurons(networkPanel);
        clearSourceNeuronsAction = new ClearSourceNeurons(networkPanel);

        groupAction = new GroupAction(networkPanel);
        ungroupAction = new UngroupAction(networkPanel, networkPanel.getViewGroupNode());

        neuronCollectionAction = new NeuronCollectionAction(networkPanel);

        showWeightMatrixAction = new ShowWeightMatrixAction(networkPanel);
        showTrainerAction = new ShowTrainerAction(networkPanel);

        showAdjustSynapsesDialog = new ShowAdjustSynapsesDialog(networkPanel);
        showAdjustConnectivityDialog = new ShowAdjustConnectivityDialog(networkPanel);
        showUpdaterDialog = new ShowNetworkUpdaterDialog(networkPanel);
        testInputAction = new TestInputAction(networkPanel);
        addSynapseGroupAction = new AddSynapseGroupAction(networkPanel);

    }

    public Action getTextEditModeAction() {
        return textEditModeAction;
    }

    public Action getWandEditModeAction() {
        return wandEditModeAction;
    }

    public List<Action> getNetworkModeActions() {
        return Arrays.asList(new Action[] {selectionEditModeAction, textEditModeAction, wandEditModeAction});
    }

    public List<Action> getNetworkControlActions() {
        return Arrays.asList(new Action[] {runNetworkAction, stopNetworkAction});
    }

    public List<Action> getClipboardActions() {
        return Arrays.asList(new Action[] {copyAction, cutAction, pasteAction});
    }

    public List<Action> getNetworkEditingActions() {
        return Arrays.asList(new Action[] {newNeuronAction, deleteAction});
    }

    public JMenuItem getNeuronCollectionAction() {
        return new JMenuItem(neuronCollectionAction);
    }

    public List<Action> getConnectionActions() {
        return Arrays.asList(new Action[] {allToAll, oneToOne, radial, radialSimple, sparse});
    }

    public JMenu getConnectionMenu() {
        // Connection menu
        JMenu connectionsMenu = new JMenu("Connect Neurons");
        for (Action action : getConnectionActions()) {
            connectionsMenu.add(action);
        }
        return connectionsMenu;
    }

    public List<Action> getNewNetworkActions() {
        return Arrays.asList(new Action[] {new AddGroupAction(networkPanel, BackpropCreationDialog.class, "Backprop"),
                new AddGroupAction(networkPanel, CompetitiveCreationDialog.class, "Competitive Network"),
                new AddGroupAction(networkPanel, FeedForwardCreationDialog.class, "Feed Forward Network"),
                new AddGroupAction(networkPanel, HopfieldCreationDialog.class, "Hopfield"),
                new AddGroupAction(networkPanel, LMSCreationDialog.class, "LMS (Least Mean Squares)"),
                new AddGroupAction(networkPanel, SOMCreationDialog.class, "SOM Network"),
                new AddGroupAction(networkPanel, SRNCreationDialog.class, "SRN (Simple Recurrent Network)")});
    }

    public JMenu getNewNetworkMenu() {
        JMenu ret = new JMenu("Insert Network");
        for (Action action : getNewNetworkActions()) {
            ret.add(action);
        }
        return ret;
    }

    public Action getNewNeuronAction() {
        return newNeuronAction;
    }

    public Action getClearNodesAction() {
        return clearNodesAction;
    }

    public Action getRandomizeObjectsAction() {
        return randomizeObjectsAction;
    }

    public Action getSelectAllAction() {
        return selectAllAction;
    }

    public Action getIterateNetworkAction() {
        return iterateNetworkAction;
    }

    public Action getShowDebugAction() {
        return showDebugAction;
    }

    public Action getRunNetworkAction() {
        return runNetworkAction;
    }

    public Action getStopNetworkAction() {
        return stopNetworkAction;
    }

    public Action getShowNetworkPreferencesAction() {
        return showNetworkPreferencesAction;
    }

    public Action getDeleteAction() {
        return deleteAction;
    }

    public Action getCopyAction() {
        return copyAction;
    }

    public Action getCutAction() {
        return cutAction;
    }

    public Action getPasteAction() {
        return pasteAction;
    }

    public Action getAlignHorizontalAction() {
        return alignHorizontalAction;
    }

    public Action getAlignVerticalAction() {
        return alignVerticalAction;
    }

    public Action getSpaceHorizontalAction() {
        return spaceHorizontalAction;
    }

    public Action getSpaceVerticalAction() {
        return spaceVerticalAction;
    }

    public JToggleButton getSetAutoZoomToggleButton() {
        return setAutoZoomAction;
    }

    public Action getSetNeuronPropertiesAction() {
        return setNeuronPropertiesAction;
    }

    public Action getSetSynapsePropertiesAction() {
        return setSynapsePropertiesAction;
    }

    public Action getSelectAllNeuronsAction() {
        return selectAllNeuronsAction;
    }

    public Action getSelectAllWeightsAction() {
        return selectAllWeightsAction;
    }

    public JCheckBoxMenuItem getShowEditToolBarMenuItem() {
        JCheckBoxMenuItem actionWrapper = new JCheckBoxMenuItem(showEditToolBarAction);
        actionWrapper.setSelected(networkPanel.getEditToolBar().isVisible());
        return actionWrapper;
    }

    public JCheckBoxMenuItem getShowMainToolBarMenuItem() {
        JCheckBoxMenuItem actionWrapper = new JCheckBoxMenuItem(showMainToolBarAction);
        actionWrapper.setSelected(networkPanel.getMainToolBar().isVisible());
        return actionWrapper;
    }

    public JCheckBoxMenuItem getShowRunToolBarMenuItem() {
        JCheckBoxMenuItem actionWrapper = new JCheckBoxMenuItem(showRunToolBarAction);
        actionWrapper.setSelected(networkPanel.getRunToolBar().isVisible());
        return actionWrapper;
    }

    public Action getSetSourceNeuronsAction() {
        return setSourceNeuronsAction;
    }

    public Action getClearSourceNeuronsAction() {
        return clearSourceNeuronsAction;
    }

    public JCheckBoxMenuItem getShowWeightsAction() {
        showWeightsAction.setSelected(networkPanel.getWeightsVisible());
        return showWeightsAction;
    }

    public Action getSelectIncomingWeightsAction() {
        return selectIncomingWeightsAction;
    }

    public Action getSelectOutgoingWeightsAction() {
        return selectOutgoingWeightsAction;
    }

    public Action getSetTextPropertiesAction() {
        return setTextPropertiesAction;
    }

    public Action getUngroupAction() {
        return ungroupAction;
    }

    public Action getGroupAction() {
        return groupAction;
    }

    public JCheckBoxMenuItem getShowPrioritiesAction() {
        return showPrioritiesAction;
    }

    public Action getShowWeightMatrixAction() {
        return showWeightMatrixAction;
    }

    public Action getShowTrainerAction() {
        return showTrainerAction;
    }

    public Action getSelectionEditModeAction() {
        return selectionEditModeAction;
    }

    public Action getShowAdjustSynapsesDialog() {
        return showAdjustSynapsesDialog;
    }

    public Action getShowUpdaterDialog() {
        return showUpdaterDialog;
    }

    public Action getShowAdjustConnectivityDialog() {
        return showAdjustConnectivityDialog;
    }

    public Action getTestInputAction() {
        return testInputAction;
    }

    public Action getAddSynapseGroupAction() {
        return addSynapseGroupAction;
    }

    public Action getZoomToFitPageAction() {
        return zoomToFitPageAction;
    }

    public Action getLayoutNeuronsAction() {
        return layoutNeurons;
    }

    /**
     * Get action based on name
     */
    public Action getAction(String name) {
        return actions.get(name);
    }


}
