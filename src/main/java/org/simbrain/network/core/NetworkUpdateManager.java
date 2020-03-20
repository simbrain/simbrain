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
package org.simbrain.network.core;

import org.simbrain.network.NetworkModel;
import org.simbrain.network.update_actions.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manage network updates. Maintains a list of actions that are updated in the
 * order in which they appear in the list when the network is iterated once (in
 * the GUI, when the step button is clicked).
 *
 * @author Jeff Yoshimi
 */
public class NetworkUpdateManager {

    /**
     * The list of update actions, in a specific order. One run through these
     * actions constitutes a single "update" in the network.
     */
    private final List<NetworkUpdateAction> actionList = new ArrayList<>();

    /**
     * Reference to parent network.
     */
    private final Network network;

    /**
     * Construct a new update manager.
     */
    public NetworkUpdateManager(Network network) {
        this.network = network;

        // Default update method
        addAction(new BufferedUpdate(network));

        //  These are the items that are automatically wrapped in an update action
        network.getEvents().onNeuronGroupAdded(ng -> {
            UpdateNetworkModel updateAction = new UpdateNetworkModel(ng);
            addAction(updateAction);
            ng.getEvents().onDelete(ng2 -> removeAction(updateAction));
        });
        network.getEvents().onSynapseGroupAdded(sg -> {
            UpdateNetworkModel updateAction = new UpdateNetworkModel(sg);
            addAction(updateAction);
            sg.getEvents().onDelete(sg2 -> removeAction(updateAction));
        });
        network.getEvents().onSubnetworkAdded(sn -> {
            UpdateNetworkModel updateAction = new UpdateNetworkModel(sn);
            addAction(updateAction);
            sn.getEvents().onDelete(sn2 -> removeAction(updateAction));
        });
        network.getEvents().onNeuronArrayAdded(na -> {
            UpdateNetworkModel updateAction = new UpdateNetworkModel(na);
            addAction(updateAction);
            na.getEvents().onDelete(na2 -> removeAction(updateAction));
        });
        network.getEvents().onWeightMatrixAdded(wm -> {
            UpdateNetworkModel updateAction = new UpdateNetworkModel(wm);
            addAction(updateAction);
            wm.getEvents().onDelete(wm2 -> removeAction(updateAction));
        });
        network.getEvents().onMultiLayerNetworkAdded(mln -> {
            UpdateNetworkModel updateNetworkModelAction = new UpdateNetworkModel(mln);
            addAction(updateNetworkModelAction);
            //mln.getEvents().onDelete(mln2 -> removeAction(updateGroupAction)); // TODO
        });

    }

    /**
     * Invokes the actions in the action list consecutively.
     */
    public void invokeAllUpdates() {
        actionList.forEach(NetworkUpdateAction::invoke);
    }

    /**
     * Perform any initialization required after opening a network from xml.
     * UpdateManager will have been created from a default no argument
     * constructor ands its fields populated using xstream.
     */
    public void postUnmarshallingInit() {
        Iterator<NetworkUpdateAction> actions = actionList.iterator();
        // TODO: Hack-y solution. Revisit this.
        while (actions.hasNext()) {
            NetworkUpdateAction action = actions.next();
            if (action instanceof ConcurrentBufferedUpdate) {
                actions.remove();
                actionList.add(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(network));
                break;
            }
        }

        for (NetworkUpdateAction action : getActionList()) {
            if (action instanceof CustomUpdate) {
                ((CustomUpdate) action).init();
            }
        }
    }

    /**
     * This is the list of actions that are available to be added manually.
     */
    public List<NetworkUpdateAction> getAvailableActionList() {
        final List<NetworkUpdateAction> availableActionList = new ArrayList<>();

        // By default these actions are always available
        availableActionList.add(new BufferedUpdate(network));
        availableActionList.add(new PriorityUpdate(network));
        availableActionList.add(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(network));

        // TODO: If added, these should be removed when any corresponding object is removed

        List<NetworkModel> actionableModels =
                Stream.of(  network.getNeuronGroups(),
                            network.getNeuronCollectionSet(),
                            network.getSubnetworks(),
                            network.getSynapseGroups(),
                            network.getNeuronArrays(),
                            network.getWeightMatrices(),
                            network.getMultiLayerNetworks())
                        .flatMap(Collection::stream).collect(Collectors.toList());

        for (NetworkModel nm : actionableModels) {
            availableActionList.add(new UpdateNetworkModel(nm));
        }

        return availableActionList;
    }
    /**
     * Return the list of current update actions.
     */
    public List<NetworkUpdateAction> getActionList() {
        return actionList;
    }

    /**
     * Swap elements at the specified location.
     *
     * @param index1 index of first element
     * @param index2 index of second element
     */
    public void swapElements(final int index1, final int index2) {
        Collections.swap(actionList, index1, index2);
        network.getEvents().fireUpdateActionsChanged();
    }

    /**
     * Add the specified action to the update manager.
     */
    public void addAction(NetworkUpdateAction action) {
        actionList.add(action);
        network.getEvents().fireUpdateActionsChanged();
    }

    /**
     * Remove the specified action from the update manager.
     */
    public void removeAction(NetworkUpdateAction action) {
        actionList.remove(action);
        network.getEvents().fireUpdateActionsChanged();
    }

    /**
     * Remove all actions completely.
     */
    public void clear() {
        actionList.clear();
        network.getEvents().fireUpdateActionsChanged();
    }

}
