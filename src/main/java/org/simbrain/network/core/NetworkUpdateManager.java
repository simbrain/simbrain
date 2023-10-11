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
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.matrix.NeuronArray;
import org.simbrain.network.matrix.WeightMatrix;
import org.simbrain.network.update_actions.BufferedUpdate;
import org.simbrain.network.update_actions.PriorityUpdate;
import org.simbrain.network.update_actions.UpdateNetworkModel;
import org.simbrain.workspace.updater.UpdateAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    private final List<UpdateAction> actionList = new ArrayList<>();

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
     * Perform any initialization required after opening a network from xml.
     * UpdateManager will have been created from a default no argument
     * constructor ands its fields populated using xstream.
     */
    public void postOpenInit() {
        // Iterator<NetworkUpdateAction> actions = actionList.iterator();
        // // TODO: Hack-y solution. Revisit this.
        // while (actions.hasNext()) {
        //     NetworkUpdateAction action = actions.next();
        //     if (action instanceof ConcurrentBufferedUpdate) {
        //         actions.remove();
        //         actionList.add(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(network));
        //         break;
        //     }
        // }
        //
        // for (NetworkUpdateAction action : getActionList()) {
        //     if (action instanceof CustomUpdate) {
        //         ((CustomUpdate) action).init();
        //     }
        // }
    }

    /**
     * This is the list of actions that are available to be added manually.
     */
    public List<UpdateAction> getAvailableActionList() {
        final List<UpdateAction> availableActionList = new ArrayList<>();

        // By default these actions are always available
        availableActionList.add(new BufferedUpdate(network));
        availableActionList.add(new PriorityUpdate(network));

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

    public List<UpdateAction> getActionList() {
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
        network.getEvents().getUpdateActionsChanged().fireAndForget();
    }

    /**
     * Add the specified action to the update manager.
     */
    public void addAction(UpdateAction action) {
        actionList.add(action);
        network.getEvents().getUpdateActionsChanged().fireAndForget();
    }

    /**
     * Add an action at a specified position, i.e. update order.
     */
    public void addAction(int index, UpdateAction action) {
        actionList.add(index, action);
    }

    /**
     * Remove the specified action from the update manager.
     */
    public void removeAction(UpdateAction action) {
        actionList.remove(action);
        network.getEvents().getUpdateActionsChanged().fireAndForget();
    }

    /**
     * Remove all actions completely.
     */
    public void clear() {
        actionList.clear();
        network.getEvents().getUpdateActionsChanged().fireAndForget();
    }

}
