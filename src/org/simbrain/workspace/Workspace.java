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
package org.simbrain.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * <b>Workspace</b> is the container for all Simbrain windows--network, world, and gauge.
 */
public class Workspace {
    
    /** The default serial version ID. */
    private static final long serialVersionUID = 1L;

    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Workspace.class);
    
    /** The coupling manager for this workspace. */
    private final CouplingManager manager = new CouplingManager();
    
    /** List of workspace components. */
    private ArrayList<WorkspaceComponent<?>> componentList = new ArrayList<WorkspaceComponent<?>>();

    /** Sentinel for determining if workspace has been changed since last save. */
    private boolean workspaceChanged = false;
    
    /** Current workspace file. */
    private File currentFile = new File(WorkspacePreferences.getDefaultFile());

    /** Current directory. So when re-opening this type of component the app remembers where to look. */
    private String currentDirectory = WorkspacePreferences.getCurrentDirectory();

    /** Whether workspace has been updated yet; used by thread. */
    private volatile boolean updateCompleted;

    /** Thread which runs workspace. */
    private WorkspaceThread workspaceThread;

    /** Listeners on this workspace. */
    private Set<WorkspaceListener> listeners = new HashSet<WorkspaceListener>();
    
    private boolean fireEvents = true;

    
    /**
     * Adds a listener to the workspace.
     * 
     * @param listener the Listener to add.
     */
    public void addListener(final WorkspaceListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes the listener from the workspace.
     * 
     * @param listener The listener to remove.
     */
    public void removeListener(final WorkspaceListener listener) {
        listeners.remove(listener);
    }

    
    void toggleEvents(final boolean on) {
        this.fireEvents = on;
    }
    
    /**
     * Couple producing attributes in the source component to consuming attributes in the target component.
     *
     * @param source reference to source component.
     * @param target reference to target component.
     */
    public void couple(final WorkspaceComponent source, final WorkspaceComponent target) {        
        //TODO: Add strategies
        // Below is 1-1
        Iterator<ConsumingAttribute> consumer = target.getCustomListOfConsumingAttributes().iterator();
        for (ProducingAttribute producer : (ArrayList<ProducingAttribute>)source.getCustomListOfProducingAttributes()) {
            if (consumer.hasNext()) {
                this.addCoupling(new Coupling(producer, consumer.next()));
            }
        }
    }
    
    
    /**
     * Couple a specified set of producing attributes to the consuming attributes in a target component.
     */
    public void coupleSpecific(final ArrayList<ProducingAttribute> sourceProducers, final WorkspaceComponent<?> targetComponent) {
        
        Iterator consumerIterator =  ((ArrayList<ConsumingAttribute>)targetComponent.getCustomListOfConsumingAttributes()).iterator();
        for (ProducingAttribute producingAttribute : sourceProducers) {
            if (consumerIterator.hasNext()) {
                Coupling coupling = new Coupling(producingAttribute, (ConsumingAttribute) consumerIterator.next());
                addCoupling(coupling);
            } else {
                break;
            }
        }
        
    }
    /**
     * Couple producing attributes in a source component with specified consuming attributes.
     */
    public void coupleSpecific(final WorkspaceComponent<?> sourceComponent, final ArrayList<ConsumingAttribute> targetConsumers) {
        Iterator consumerIterator = targetConsumers.iterator();
        for (ProducingAttribute producingAttribute :  ((ArrayList<ProducingAttribute>)sourceComponent.getCustomListOfProducingAttributes())) {
            if (consumerIterator.hasNext()) {
                Coupling coupling = new Coupling(producingAttribute, (ConsumingAttribute) consumerIterator.next());
                addCoupling(coupling);
            } else {
                break;
            }
        }
    }

    /**
     * Adds a workspace component to the workspace.
     * 
     * @param component The component to add.
     */
    public void addWorkspaceComponent(final WorkspaceComponent<?> component) {
        LOGGER.debug("adding component: " + component);
        componentList.add(component);
        component.setWorkspace(this);
        workspaceChanged = true;
        
        if (fireEvents) {
            for (WorkspaceListener listener : listeners) {
                listener.componentAdded(component);
            }
        }
    }
    
    /**
     * Remove the specified component.
     *
     * @param component The component to remove.
     */
    public void removeWorkspaceComponent(final WorkspaceComponent<?> component) {
        LOGGER.debug("removing component: " + component);
        
        for (WorkspaceListener listener : listeners) {
            listener.componentRemoved(component);
        }
        componentList.remove(component);
        this.setWorkspaceChanged(true);
    }
    
    // TODO: Add other update methods.
    
    /**
     * Update all couplings on all components.  Currently use a buffering method.
     */
    public void globalUpdate() {
        manager.updateAllCouplings();
        
        for (WorkspaceComponent<?> component : componentList) {
            component.doUpdate();
        }
        
        updateCompleted = true;
    }
    
    /**
     * Should be called when updating is stopped.
     */
    void updateStopped() {
        for (WorkspaceComponent<?> component : componentList) {
            component.doStopped();
        }
    }
    
    /**
     * Update all couplings on all components once.
     */
    public void singleUpdate() {
        globalUpdate();
        updateStopped();
    }
    
    /**
     * Adds a coupling to the CouplingManager.
     * 
     * @param coupling The coupling to add.
     */
    public void addCoupling(final Coupling<?> coupling) {
        manager.addCoupling(coupling);
    }
    
    /**
     * Removes a coupling from the CouplingManager.
     * 
     * @param coupling The coupling to remove.
     */
    public void removeCoupling(final Coupling<?> coupling) {
        manager.removeCoupling(coupling);
    }

    
    /**
     * Iterates all couplings on all components until halted by user.
     */
    public void globalRun() {
        WorkspaceThread workspaceThread = getWorkspaceThread();

        if (!workspaceThread.isRunning()) {
            workspaceThread.setRunning(true);
            workspaceThread.start();
        } else {
            workspaceThread.setRunning(false);
        }
    }

    /**
     * Stops iteration of all couplings on all components.
     */
    public void globalStop() {
        WorkspaceThread workspaceThread = getWorkspaceThread();

        workspaceThread.setRunning(false);
        clearWorkspaceThread();
    }

    /**
     * Remove all items (networks, worlds, etc.) from this workspace.
     */
    public void clearWorkspace() {
        if (changesExist()) {
            for (WorkspaceListener listener : listeners) {
                if (!listener.clearWorkspace()) { return; }
            }
        }
        workspaceChanged = false;
        removeAllComponents();
        currentFile = null;
        for (WorkspaceListener listener : listeners) {
            listener.workspaceCleared();
        }
        manager.clearCouplings();
    }

    /**
     * Disposes all Simbrain Windows.
     */
    public void removeAllComponents() {
        for (WorkspaceComponent<?> component : componentList) {
            for (WorkspaceListener listener : listeners) {
                listener.componentRemoved(component);
            }
        }
        
        componentList.clear();
    }

    /**
     * Check whether there have been changes in the workspace or its components.
     *
     * @return true if changes exist, false otherwise
     */
    public boolean changesExist() {
        if (workspaceChanged) {
            return true;
        } else {
            boolean hasChanged = false;
            for (WorkspaceComponent<?> window : componentList) {
                if (window.hasChangedSinceLastSave()) {
                    hasChanged = true;
                }
            }
            return hasChanged;
        }
    }

    /**
     * Sets whether the workspace has been changed.
     * @param workspaceChanged Has workspace been changed value
     */
    public void setWorkspaceChanged(final boolean workspaceChanged) {
        this.workspaceChanged = workspaceChanged;
    }

    /**
     * @return the currentDirectory
     */
    public String getCurrentDirectory() {
        return currentDirectory;
    }

    /**
     * @param currentDirectory the currentDirectory to set
     */
    public void setCurrentDirectory(final String currentDirectory) {
        this.currentDirectory = currentDirectory;
        WorkspacePreferences.setCurrentDirectory(currentDirectory);
    }

    /**
     * @return Returns the currentFile.
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * @param currentFile The current_file to set.
     */
    public void setCurrentFile(final File currentFile) {
        this.currentFile = currentFile;
    }

    /**
     * @return the componentList
     */
    public List<? extends WorkspaceComponent<?>> getComponentList() {
        return Collections.unmodifiableList(componentList);
    }
    
    /**
     * Get a component using its name id.  Used in terminal mode.
     *
     * @param id name of component
     * @return Workspace Component
     */
    public WorkspaceComponent getComponent(final String id) {
        for (WorkspaceComponent component : componentList) {
            if (component.getName().equalsIgnoreCase(id)) {
                return component;
            }
        }
        return null;
    }

    
    /**
     * Used by Network thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @return whether the network has been updated or not
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @param b whether the network has been updated or not.
     */
    public void setUpdateCompleted(final boolean b) {
        updateCompleted = b;
    }

    /**
     * Returns the workspaceThread.
     * 
     * @return The workspaceThread.
     */
    public WorkspaceThread getWorkspaceThread() {
        if (workspaceThread == null) { workspaceThread = new WorkspaceThread(this); }
        
        return workspaceThread;
    }
    
    /**
     * Clears the workspace thread.
     */
    private void clearWorkspaceThread() {
        workspaceThread = null;
    }

    /**
     * Returns the coupling manager for this workspace.
     * 
     * @return The coupling manager for this workspace.
     */
    public CouplingManager getManager() {
        return manager;
    }
}
