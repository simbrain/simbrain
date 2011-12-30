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
package org.simbrain.network.interfaces;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.UpdatableGroup;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
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

    /** Reference to parent network. */
    private final RootNetwork network;
    
    /**
     * Construct a new update manager
     *
     * @param network
     */
    public UpdateManager(RootNetwork network) {
        this.network = network;
        
        // Group are automatically listened for, and added.  Possibly
        //  Make it possible to override this default behavior.
        network.addGroupListener(new GroupListener() {

            public void groupAdded(NetworkEvent<Group> e) {
                if (e.getObject() instanceof UpdatableGroup) {
                    addAction(new UpdateGroup(e.getObject()));
                }            
            }

            public void groupRemoved(NetworkEvent<Group> e) {
                if (e.getObject() instanceof UpdatableGroup) {
                    removeAction(new UpdateGroup(e.getObject()));
                }            
            }

            public void groupChanged(NetworkEvent<Group> networkEvent,
                    String changeDescription) {
                
            }

            public void groupParameterChanged(NetworkEvent<Group> networkEvent) {
            }
            
        });
    }

    /**
     * @return the actionList
     */
    public List<UpdateAction> getActionList() {
        return actionList;
    }
    
    /**
     * Add an action to the list.
     *
     * @param action the action to add.
     */
    protected void addAction(UpdateAction action) {
        actionList.add(action);
    }
    
    /**
     * Remove an action from the list.
     *
     * @param action the action to remove
     */
    protected void removeAction(UpdateAction action) {
        actionList.remove(action);
    }

}
