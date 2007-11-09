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
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * <b>Workspace</b> is the container for all Simbrain windows--network, world, and gauge.
 */
public class Workspace {
    
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(Workspace.class);
    
    private final CouplingManager manager = new CouplingManager();
    
    /** List of workspace components. */
    private ArrayList<WorkspaceComponent<?>> componentList = new ArrayList<WorkspaceComponent<?>>();

    /** Sentinel for determining if workspace has been changed since last save. */
    private boolean workspaceChanged = false;
    
    /** Current workspace file. */
    private File currentFile = new File(WorkspacePreferences.getDefaultFile());

    /** Whether network has been updated yet; used by thread. */
    private volatile boolean updateCompleted;

    /** Thread which runs workspace. */
    private WorkspaceThread workspaceThread;

    private Set<WorkspaceListener> listeners = new HashSet<WorkspaceListener>();
    
    /**
     * Default constructor.
     */
    public Workspace() {
        
    }
    
    public void addListener(WorkspaceListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(WorkspaceListener listener) {
        listeners.remove(listener);
    }

    public void addWorkspaceComponent(WorkspaceComponent<?> component)
    {
        LOGGER.debug("adding component: " + component);
        componentList.add(component);
        component.setWorkspace(this);
        workspaceChanged = true;
        
        for (WorkspaceListener listener : listeners) {
            listener.componentAdded(component);
        }
    }
    
    /**
     * Remove the specified window.
     *
     * @param window
     */
    public void removeWorkspaceComponent(final WorkspaceComponent<?> component) {
        LOGGER.debug("removing component: " + component);
        
        for (WorkspaceListener listener : listeners) {
            listener.componentRemoved(component);
        }
        componentList.remove(component);
        this.setWorkspaceChanged(true);
    }
    
    /**
     * Update all couplings on all components.  Currently use a buffering method.
     * TODO: Add other update methods.
     */
    public void globalUpdate() {
        manager.updateAllCouplings();
        
        for (WorkspaceComponent<?> component : componentList) {
            component.doUpdate();
        }
        
        updateCompleted = true;
    }

    public void addCoupling(Coupling<?> coupling) {
        manager.addCoupling(coupling);
    }
    
//    private boolean hasCouplings(WorkspaceComponent component) {
//        if (component.getCouplingContainer() == null) {
//            return false;
//        } else if (component.getCouplingContainer().getCouplings() == null) {
//            return false;
//        }
//        
//        return true;
//    }

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
                if (!listener.clearWorkspace()) return;
            }
        }
        workspaceChanged = false;
        removeAllComponents();
        currentFile = null;
        for (WorkspaceListener listener : listeners) {
            listener.workspaceCleared();
        }
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
                if (window.isChangedSinceLastSave()) {
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
     * @return the workspaceThread.
     */
    public WorkspaceThread getWorkspaceThread() {
        if (workspaceThread == null) workspaceThread = new WorkspaceThread(this);
        
        return workspaceThread;
    }
    
    private void clearWorkspaceThread() {
        workspaceThread = null;
    }
}
