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
import org.simbrain.network.connectors.WeightMatrix;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.matrix.NeuronArray;
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

        // By default, only do a buffered update
        addAction(new BufferedUpdate(network));

    }

    /**
     * Invokes the actions in the action list consecutively. By default {@link BufferedUpdate} is
     * all that is called.
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
                Stream.of(  network.getModels(NeuronGroup.class),
                            network.getModels(NeuronCollection.class),
                            network.getModels(Subnetwork.class),
                            network.getModels(SynapseGroup.class),
                            network.getModels(NeuronArray.class),
                            network.getModels(WeightMatrix.class))
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
