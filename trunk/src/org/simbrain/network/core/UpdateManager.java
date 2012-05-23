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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simbrain.network.groups.Group;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.update_actions.BufferedUpdate;
import org.simbrain.network.update_actions.CustomUpdate;
import org.simbrain.network.update_actions.PriorityUpdate;
import org.simbrain.network.update_actions.UpdateGroup;

/**
 * Manage network updates. Maintain a list of actions. When the network is
 * iterated once (in the GUI, when the step button is clicked), the actions
 * contained here are invoked, in a specific order.
 */
public class UpdateManager {

    /**
     * The list of update actions, in a specific order. One run through these
     * actions constitutes a single "update" in the network.
     */
    private final List<UpdateAction> actionList = new ArrayList<UpdateAction>();

    /**
     * The list of possible update actions that can be added to the main list.
     */
    private final List<UpdateAction> availableActionList = new ArrayList<UpdateAction>();

    /**
     * List of listeners on this update manager.
     */
    private List<UpdateManagerListener> listeners = new ArrayList<UpdateManagerListener>();

    /** Reference to parent network. */
    private final RootNetwork network;

    /**
     * Construct a new update manager.
     *
     * @param network
     */
    public UpdateManager(RootNetwork network) {
        this.network = network;
        // Default update method
        addAction(new BufferedUpdate(network));
        addAvailableAction(new PriorityUpdate(network));        
        addListeners();

    }
    
    /**
     * Perform any initialization required after opening a network from xml.
     * UpdateManager will have been created from a default no argument constructor
     * ands its fields populated using xstream.
     */
	public void postUnmarshallingInit() {
		listeners = new ArrayList<UpdateManagerListener>();
        addListeners();
        for(UpdateAction action : getActionList()) {
        	if (action instanceof CustomUpdate) {
        		((CustomUpdate)action).init();
        	}
        }
        for(UpdateAction action : getAvailableActionList()) {
        	if (action instanceof CustomUpdate) {
        		((CustomUpdate)action).init();
        	}
        }
	}

    /**
     * Update manager should listen for relevant changes in network.
     */
    private void addListeners() {
        // Group are automatically listened for, and added.  Possibly
        //  Make it possible to override this default behavior.
        network.addGroupListener(new GroupListener() {

            public void groupAdded(NetworkEvent<Group> e) {
                if (e.getObject().isTopLevelGroup()) {
                    addAction(new UpdateGroup(e.getObject()));                    
                }
            }

            public void groupRemoved(NetworkEvent<Group> e) {
                // Find corresponding group update action and remove it
                removeGroupAction(e.getObject());
            }

            public void groupChanged(NetworkEvent<Group> networkEvent,
                    String changeDescription) {
                
            }

            public void groupParameterChanged(NetworkEvent<Group> networkEvent) {
            }
            
        });    }
    
    /**
     * Remove action (if one exists) associated with the provided group.
     *
     * @param group 
     */
    private void removeGroupAction(Group group) {
        UpdateAction toDelete = null;
        for (UpdateAction action : actionList) {
            if (action instanceof UpdateGroup) {
                if (((UpdateGroup)action).getGroup() == group) {
                    toDelete = action;                                                        
                }
            }
        }
        if (toDelete != null) {
            removeActionCompletely(toDelete);
        }

    }

    /**
     * Listen for updates to the update manager.
     *
     * @param listener the listener to add
     */
    public void addListener(UpdateManagerListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(UpdateManagerListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * @return the actionList
     */
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
        //TODO: Handle case where indices don't make sense
        Collections.swap(actionList, index1, index2);
        for(UpdateManagerListener listener : listeners) {
            listener.actionOrderChanged();
        }
    }
    
    /**
     * Add an action to the list.  (Takes it off the available list)
     *
     * @param action the action to add.
     */
    public void addAction(UpdateAction action) {
        actionList.add(action);
        for(UpdateManagerListener listener : listeners) {
            listener.actionAdded(action);
        }
        removeAvailableAction(action);
    }
    
    /**
     * Remove an action from the list. (But adds it it to the available list)
     *
     * @param action the action to remove
     */
    public void removeAction(UpdateAction action) {
        actionList.remove(action);
        for(UpdateManagerListener listener : listeners) {
            listener.actionRemoved(action);
        }
        addAvailableAction(action);
    }
    
    /**
     * Completely remove an action (from both current and available lists).
     *
     * @param action the action to completely remove
     */
    public void removeActionCompletely(UpdateAction action) {
        actionList.remove(action);
        availableActionList.remove(action);
        for(UpdateManagerListener listener : listeners) {
            listener.actionRemoved(action);
        }
    }
    
    /**
     * Add an action to the list.
     *
     * @param action the action to add.
     */
    protected void addAvailableAction(UpdateAction action) {
        availableActionList.add(action);
        for(UpdateManagerListener listener : listeners) {
            listener.availableActionAdded(action);
        }
    }
    
    /**
     * Remove a potential action from the list.
     *
     * @param action the action to remove
     */
    public void removeAvailableAction(UpdateAction action) {
        availableActionList.remove(action);
        for(UpdateManagerListener listener : listeners) {
            listener.availableActionRemoved(action);
        }
    }
    
    /**
     * Listen from changes to update manager.
     */
    public interface UpdateManagerListener {

        /** An action was added. */
        public void actionAdded(UpdateAction action);

        /** An action was removed. */
        public void actionRemoved(UpdateAction action);

        /** A potential action was added. */
        public void availableActionAdded(UpdateAction action);

        /** An potential action was removed. */
        public void availableActionRemoved(UpdateAction action);

        /** The action order was changed. */
        public void actionOrderChanged();
    }

    /**
     * @return the potentialActionList
     */
    public List<UpdateAction> getAvailableActionList() {
        return availableActionList;
    }

}
