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
import java.util.*;

/**
 * Network action manager.
 * <p>
 * This class contains references to all the actions for a NetworkPanel. In some cases, related actions are grouped
 * together, see e.g.
 * <code>getNetworkModeActions()</code>.
 * </p>
 */
public final class NetworkActionManager {

    /**
     * Reference to NetworkPanel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Map storing string -> action associations
     */
    private transient Map<String, AbstractAction> actions = new HashMap<>();

    /**
     * Create a new network action manager for the specified network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    NetworkActionManager(final NetworkPanel networkPanel) {

        this.networkPanel = networkPanel;

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        actions.put("zoomToFitPage", new ZoomToFitPageAction(networkPanel));
        actions.put("newNeuron", new NewNeuronAction(networkPanel));
        actions.put("clearNodes", new ClearNodeActivationsAction(networkPanel));
        actions.put("randomizeObjects", new RandomizeObjectsAction(networkPanel));
        actions.put("selectAll", new SelectAllAction(networkPanel));
        actions.put("delete", new DeleteAction(networkPanel));
        actions.put("copy", new CopyAction(networkPanel));
        actions.put("cut", new CutAction(networkPanel));
        actions.put("paste", new PasteAction(networkPanel));
        actions.put("iterateNetwork", new IterateNetworkAction(networkPanel));
        actions.put("runNetwork", new RunNetworkAction(networkPanel));
        actions.put("stopNetwork", new StopNetworkAction(networkPanel));
        actions.put("showDebug", new ShowDebugAction(networkPanel));
        actions.put("showNetworkPreferences", new ShowNetworkPreferencesAction(networkPanel));
        actions.put("alignHorizontal", new AlignHorizontalAction(networkPanel));
        actions.put("alignVertical", new AlignVerticalAction(networkPanel));
        actions.put("spaceVertical", new SpaceVerticalAction(networkPanel));
        actions.put("spaceHorizontal", new SpaceHorizontalAction(networkPanel));
        actions.put("setNeuronProperties", new SetNeuronPropertiesAction(networkPanel));
        actions.put("setSynapseProperties", new SetSynapsePropertiesAction(networkPanel));
        actions.put("selectAllWeights", new SelectAllWeightsAction(networkPanel));
        actions.put("selectAllNeurons", new SelectAllNeuronsAction(networkPanel));
        actions.put("selectAll", new SelectAllAction(networkPanel));
        actions.put("showMainToolBar", new ShowMainToolBarAction(networkPanel));
        actions.put("showEditToolBar", new ShowEditToolBarAction(networkPanel));
        actions.put("showRunToolBar", new ShowRunToolBarAction(networkPanel));
        actions.put("clearSourceNeurons", new ClearSourceNeurons(networkPanel));
        actions.put("setSourceNeurons", new SetSourceNeurons(networkPanel));
        actions.put("selectIncomingWeights", new SelectIncomingWeightsAction(networkPanel));
        actions.put("selectOutgoingWeights", new SelectOutgoingWeightsAction(networkPanel));
        actions.put("setTextProperties", new SetTextPropertiesAction(networkPanel));
        actions.put("showWeightMatrix", new ShowWeightMatrixAction(networkPanel));
        actions.put("showAdjustSynapsesDialog", new ShowAdjustSynapsesDialog(networkPanel));
        actions.put("showAdjustConnectivityDialog", new ShowAdjustConnectivityDialog(networkPanel));
        actions.put("showUpdaterDialog", new ShowNetworkUpdaterDialog(networkPanel));
        actions.put("testInput", new TestInputAction(networkPanel));
        actions.put("showLayoutDialog", new ShowLayoutDialogAction(networkPanel));
        actions.put("setSourceNeurons", new SetSourceNeurons(networkPanel));
        actions.put("clearSourceNeurons", new ClearSourceNeurons(networkPanel));
        actions.put("showPriorities", new ShowPrioritiesAction(networkPanel));

        actions.put("conn_allToAll", new ApplyConnectionAction(networkPanel, new AllToAll(), "All to all"));
        actions.put("conn_oneToOne", new ApplyConnectionAction(networkPanel, new OneToOne(), "One-to-one"));
        actions.put("conn_radial", new ApplyConnectionAction(networkPanel, new RadialGaussian(), "Radial (Gaussian)"));
        actions.put("conn_radialSimple", new ApplyConnectionAction(networkPanel, new RadialSimple(), "Radial (Simple)"));
        actions.put("conn_sparse", new ApplyConnectionAction(networkPanel, new Sparse(), "Sparse"));
        actions.put("showWeights", new ShowWeightsAction(networkPanel));

        actions.put("newNeuronGroup", new AddGroupAction(networkPanel,
                NeuronGroupDialog.class, "Add Neuron Group"));
        actions.put("addSynapseGroup", new AddSynapseGroupAction(networkPanel));
        actions.put("addNeuronCollection", new NeuronCollectionAction(networkPanel));

        actions.put("selectionEditMode", new SelectionEditModeAction(networkPanel));
        actions.put("textEditMode", new TextEditModeAction(networkPanel));
        actions.put("wandEditMode", new WandEditModeAction(networkPanel));

    }


    public List<Action> getNetworkModeActions() {
        return Arrays.asList(new Action[]{actions.get("selectionEditMode"), actions.get("textEditMode"),
                actions.get("wandEditMode")});
    }

    public List<Action> getNetworkControlActions() {
        return Arrays.asList(new Action[]{actions.get("runNetworkAction"), actions.get("stopNetworkAction")});
    }

    public List<Action> getClipboardActions() {
        return Arrays.asList(new Action[]{actions.get("copy"), actions.get("cut"), actions.get("paste")});
    }

    public List<Action> getNetworkEditingActions() {
        return Arrays.asList(new Action[]{actions.get("newNeuron"), actions.get("delete")});
    }

    public JMenu getConnectionMenu() {
        JMenu cm = new JMenu("Connect Neurons");
        cm.add(getMenuItem("conn_allToAll"));
        cm.add(getMenuItem("conn_oneToOne"));
        cm.add(getMenuItem("conn_radial"));
        cm.add(getMenuItem("conn_radialSimple"));
        cm.add(getMenuItem("conn_sparse"));
        return cm;
    }

    public List<Action> getNewNetworkActions() {
        return Arrays.asList(new Action[]{new AddGroupAction(networkPanel, BackpropCreationDialog.class, "Backprop"),
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

    /**
     * Returns on action based on its associated name (see above)
     */
    public Action getAction(String name) {
        return actions.get(name);
    }

    /**
     * Gets an action within a {@link JCheckBoxMenuItem}
     */
    public JCheckBoxMenuItem getMenuItem(String name) {
        return new JCheckBoxMenuItem(getAction(name));
    }

    /**
     * Gets an action within a {@link JCheckBoxMenuItem}.
     *
     * @param name    name of action
     * @param checked whether it should initially be checked.
     */
    public JCheckBoxMenuItem getMenuItem(String name, boolean checked) {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(getAction(name));
        menuItem.setSelected(checked);
        return menuItem;
    }

}
