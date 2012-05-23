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
package org.simbrain.workspace.updater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.CouplingListener;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;

/**
 * Maintain a list of workspace update actions. When the workspace is iterated
 * once (in the GUI, when the step button is clicked), the actions contained
 * here are invoked, in a specific order. By default a single action, buffered
 * update, is invoked. But custom update orders can be created by manipulating
 * the main update action list here.
 * 
 * @author jyoshimi
 */
public class UpdateActionManager {
    
    /**
     * The list of update actions, in a specific order. One run through these
     * actions constitutes a single iteration of the workspace.
     */
    private final List<UpdateAction> actionList = new CopyOnWriteArrayList<UpdateAction>();

    /**
     * The list of possible update actions that can be added to the main list.
     */
    private final List<UpdateAction> availableActionList = new CopyOnWriteArrayList<UpdateAction>();

    /**
     * List of listeners on this update manager
     */
    private final List<UpdateManagerListener> listeners = new ArrayList<UpdateManagerListener>();

    /**
     * Reference to workspace.
     */
    private final WorkspaceUpdater workspaceUpdater;
    
	/**
	 * Keep track of relations between coupling and coupling actions so they can
	 * be cleaned up.
	 */
	private HashMap<Coupling<?>, UpdateCoupling> couplingActionMap = new HashMap<Coupling<?>, UpdateCoupling>();

	/**
	 * Keep track of relations between component and component actions so they
	 * can be cleaned up.
	 */
	private HashMap<WorkspaceComponent, UpdateComponent> componentActionMap = new HashMap<WorkspaceComponent, UpdateComponent>();
        
    /**
     * Construct a new update manager
     *
     * @param workspace reference to workspace updater.
     */
    public UpdateActionManager(final WorkspaceUpdater workspace) {
        this.workspaceUpdater = workspace;
        // Default update method
        addAction(new UpdateAllBuffered(workspace));
        addListeners();

    }

    
    /**
     * Perform initialization after deserializing.
     */
	public void postAddInit() {
        //addListeners();
		for(UpdateAction action : actionList) {
	        System.out.println(action.getLongDescription());
			
		}
	}


    /**
     * Update manager should listen for relevant changes in workspace.
     */
    private void addListeners() {
    	
        // Add / remove component actions as needed
    	workspaceUpdater.getWorkspace().addListener(new WorkspaceListener() {

			@Override
			public void workspaceCleared() {
			}

			@Override
			public void newWorkspaceOpened() {
			}

			@Override
			public void componentAdded(WorkspaceComponent component) {
				UpdateComponent componentAction = new UpdateComponent(workspaceUpdater, component);
				addAvailableAction(componentAction);
				componentActionMap.put(component, componentAction);
				//System.out.println("Added component " + componentActionMap.size());
			}

			@Override
			public void componentRemoved(WorkspaceComponent component) {
				removeAction(componentActionMap
						.remove(component));
				//System.out.println("Removed component " + couplingActionMap.size());
			}
			
    	});
    	
    	
        // Add / remove coupling actions as needed
		workspaceUpdater.getWorkspace().getCouplingManager()
				.addCouplingListener(new CouplingListener() {

					@Override
					public void couplingAdded(Coupling<?> coupling) {
						UpdateCoupling couplingAction = new UpdateCoupling(coupling);
						addAvailableAction(couplingAction);
						couplingActionMap.put(coupling, couplingAction);
						//System.out.println("Added coupling " + couplingActionMap.size());
					}

					@Override
					public void couplingRemoved(Coupling<?> coupling) {
						removeAction(couplingActionMap
								.remove(coupling));
						//System.out.println("Removed coupling " + couplingActionMap.size());
					}

				});
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
    public void moveActionToAvailableList(UpdateAction action) {
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
    public void removeAction(UpdateAction action) {
        actionList.remove(action);
        availableActionList.remove(action);
        for(UpdateManagerListener listener : listeners) {
            listener.actionRemoved(action);
            listener.availableActionRemoved(action);
        }
    }
    
    /**
     * Add an action to the list.
     *
     * @param action the action to add.
     */
    public void addAvailableAction(UpdateAction action) {
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


	public void clear() {
		for(UpdateAction action : actionList) {
			removeAction(action);
		}
		for(UpdateAction action : availableActionList) {
			removeAction(action);
		}
		
	}

}
