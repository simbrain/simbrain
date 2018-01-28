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

import org.simbrain.network.groups.Group;
import org.simbrain.network.listeners.GroupAdapter;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.update_actions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Manage network updates. Maintains a list of actions that are updated in the
 * order in which they appear in the list when the network is iterated once (in
 * the GUI, when the step button is clicked).
 *
 * @author Jeff Yoshimi
 */
public class NetworkUpdateManager {

    /**
     * Listener for changes to a NetworkUpdateManager.
     */
    public interface Listener {
        /**
         * Fired when an action is added to the update manager.
         */
        void actionAdded(NetworkUpdateAction action);

        /**
         * Fired when an action is removed from the update manager.
         */
        void actionRemoved(NetworkUpdateAction action);

        /**
         * Fired when the update action order is changed.
         */
        void actionOrderChanged();
    }

    /**
     * The list of update actions, in a specific order. One run through these
     * actions constitutes a single "update" in the network.
     */
    private final List<NetworkUpdateAction> actionList = new ArrayList<>();

    /**
     * List of listeners on this update manager.
     */
    private List<Listener> listeners = new ArrayList<>();

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
        addListeners();
    }

    public void invokeAllUpdates() {
        actionList.forEach(NetworkUpdateAction::invoke);
    }

    /**
     * Perform any initialization required after opening a network from xml.
     * UpdateManager will have been created from a default no argument
     * constructor ands its fields populated using xstream.
     */
    public void postUnmarshallingInit() {
        listeners = new ArrayList<>();
        addListeners();
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
     * Update manager listen for relevant changes in network. In particular
     * group update actions are added or removed as groups are added or removed.
     */
    private void addListeners() {
        network.addGroupListener(new GroupAdapter() {

            public void groupAdded(NetworkEvent<Group> e) {
                if (e.getObject().isTopLevelGroup()) {
                    addAction(new UpdateGroup(e.getObject()));
                }
            }

            public void groupRemoved(NetworkEvent<Group> e) {
                // Find corresponding group update action and remove it
                removeGroupAction(e.getObject());
            }

        });
    }

    /**
     * Returns a list of network update actions that can be added.
     *
     * @return available action list
     */
    public List<NetworkUpdateAction> getAvailableActionList() {
        final List<NetworkUpdateAction> availableActionList = new ArrayList<>();

        // By default these guys are always available
        availableActionList.add(new BufferedUpdate(network));
        availableActionList.add(new PriorityUpdate(network));
        availableActionList.add(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(network));

        // Add update actions for all groups available
        for (Group group : network.getGroupList()) {
            if (group.isTopLevelGroup()) {
                availableActionList.add(new UpdateGroup(group));
            }
        }

        return availableActionList;
    }

    /**
     * Remove action (if one exists) associated with the provided group.
     *
     * @param group the group being removed
     */
    private void removeGroupAction(Group group) {
        NetworkUpdateAction toDelete = null;
        for (NetworkUpdateAction action : actionList) {
            if (action instanceof UpdateGroup) {
                if (((UpdateGroup) action).getGroup() == group) {
                    toDelete = action;
                }
            }
        }
        if (toDelete != null) {
            removeAction(toDelete);
        }
    }

    /**
     * Listen for updates to the update manager.
     *
     * @param listener the listener to add
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Return the list of update actions.
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
        for (Listener listener : listeners) {
            listener.actionOrderChanged();
        }
    }

    /**
     * Add the specified action to the update manager.
     */
    public void addAction(NetworkUpdateAction action) {
        actionList.add(action);
        for (Listener listener : listeners) {
            listener.actionAdded(action);
        }
    }

    /**
     * Remove the specified action from the update manager.
     */
    public void removeAction(NetworkUpdateAction action) {
        actionList.remove(action);
        for (Listener listener : listeners) {
            listener.actionRemoved(action);
        }
    }

    /**
     * Remove all actions completely.
     */
    public void clear() {
        for (NetworkUpdateAction action : actionList) {
            for (Listener l : listeners) {
                l.actionRemoved(action);
            }
        }
        actionList.clear();
    }

}
