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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.simbrain.workspace.updator.TaskSynchronizationManager;
import org.simbrain.workspace.updator.UpdateController;
import org.simbrain.workspace.updator.WorkspaceUpdator;
import org.simbrain.workspace.updator.WorkspaceUpdatorListener;

/**
 * A collection of components which interact via couplings. Neural networks,
 * data-tables, gauges, and scripts are examples of components in a Simbrain
 * workspace. Essentially, an instance of a workspace corresponds to a single
 * simulation (though at some point it will be possible to link multiple
 * workspaces on different machines together).
 *
 * A workspace can be visualized via a {@link org.simbrain.workspace.gui.SimbrainDesktop}.
 *
 * @see org.simbrain.workspace.Coupling
 *
 */
public class Workspace {

    /** The default serial version ID. */
    private static final long serialVersionUID = 1L;

    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Workspace.class);

    /** The coupling manager for this workspace. */
    private final CouplingManager manager = new CouplingManager();

    /** List of workspace components. */
    private List<WorkspaceComponent> componentList = Collections
        .synchronizedList(new ArrayList<WorkspaceComponent>());

    /** Sentinel for determining if workspace has been changed since last save. */
    private boolean workspaceChanged = false;

    /** Current workspace file. */
    private File currentFile = null;

    /**
     * Current directory. So when re-opening this type of component the
     * app remembers where to look.
     */
    private String currentDirectory = WorkspacePreferences.getCurrentDirectory();

    /**
     * Listeners on this workspace. The CopyOnWriteArrayList is not a problem because
     * writes to this list are uncommon.
     */
    private CopyOnWriteArrayList<WorkspaceListener> listeners =
        new CopyOnWriteArrayList<WorkspaceListener>();

    /**
     * Mapping from workspace component types to integers which show how many have been added.
     * For naming.
     */
    private Hashtable<Class<?>, Integer> componentNameIndices = new Hashtable<Class<?>, Integer>();

    /**
     * The updator used to manage component updates.
     */
    private Object updatorLock = new Object();

    /**
     * Delay in milliseconds between update cycles. Used to artificially slow down
     * simulation (sometimes useful in teaching).
     */
    private int updateDelay = 0;

    /**
     * The updator used to manage component updates.
     */
    private WorkspaceUpdator updator = new WorkspaceUpdator(this, manager);

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


    /**
     * Couple each source attribute to all target attributes.
     *
     * @param sourceAttributes source producing attributes
     * @param targetAttributes target consuming attributes
     */
    @SuppressWarnings("unchecked")
    public void coupleOneToMany(
            final List<PotentialAttribute> sourceAttributes,
            final List<PotentialAttribute> targetAttributes) {
        for (PotentialAttribute producingAttribute : sourceAttributes) {
            for (PotentialAttribute  consumingAttribute : targetAttributes) {
                Coupling<?> coupling = new Coupling(producingAttribute
                        .createProducer(), consumingAttribute.createConsumer());
                getCouplingManager().addCoupling(coupling);
            }
        }
    }

    /**
     * Couple each source attribute to one target attribute, as long as there
     * are target attributes to couple to.
     *
     * @param sourceAttributes source producing attributes
     * @param targetAttributes target consuming attributes
     */
    @SuppressWarnings("unchecked")
    public void coupleOneToOne(
            final List<PotentialAttribute> producerKeys, final List<PotentialAttribute> consumerKeys) {

        Iterator<PotentialAttribute> consumerIterator = consumerKeys.iterator();

        for (PotentialAttribute  producerID : producerKeys) {
            if (consumerIterator.hasNext()) {
                Producer producer = producerID.createProducer();
                Consumer consumer = consumerIterator.next().createConsumer();
                Coupling<?> coupling = new Coupling(producer, consumer);
                getCouplingManager().addCoupling(coupling);
            }
        }
    }


    /**
     * Adds a workspace component to the workspace.
     *
     * @param component The component to add.
     */
    public void addWorkspaceComponent(final WorkspaceComponent component) {
        LOGGER.debug("adding component: " + component);
        componentList.add(component);
        component.setWorkspace(this);
        component.setChangedSinceLastSave(false);
        this.setWorkspaceChanged(true);

        /*
         * Handle component naming.
         *
         * If the component has not yet been named, name as follows:
         *      (ClassName - "Component") + index
         * where index iterates as new components are added.
         * e.g. Network 1, Network 2, etc.
         */
        if (component.getName().equalsIgnoreCase("")) {
            if (componentNameIndices.get(component.getClass()) == null) {
                componentNameIndices.put(component.getClass(), 1);
            } else {
                int index = componentNameIndices.get(component.getClass());
                componentNameIndices.put(component.getClass(), index + 1);
            }
            component.setName(component.getSimpleName()
                    + componentNameIndices.get(component.getClass()));
        }

        for (WorkspaceListener listener : listeners) {
            listener.componentAdded(component);
        }
    }

    /**
     * Remove the specified component.
     *
     * @param component The component to remove.
     */
    public void removeWorkspaceComponent(final WorkspaceComponent component) {
        LOGGER.debug("removing component: " + component);
        for (WorkspaceListener listener : listeners) {
            listener.componentRemoved(component);
        }
        /* Remove all couplings associated with this component */
        this.getCouplingManager().removeCouplings(component);
        componentList.remove(component);
        this.setWorkspaceChanged(true);
    }

    /**
     * Should be called when updating is stopped.
     */
    void updateStopped() {
        synchronized (componentList) {
            for (WorkspaceComponent component : componentList) {
                component.doStopped();
            }
        }
    }

    /**
     * Convenience method (with an easily-recognized name) forwarded to
     * singleUpdate.
     */
    public void iterate() {
        singleUpdate();
    }

    /**
     * Iterate for a specified number of steps.
     *
     * @param numIterations
     */
    public void iterate(final int numIterations) {
        updator.iterate(numIterations);
    }

    /**
     * Update all couplings on all components once.
     */
    public void singleUpdate() {
        globalUpdate();
        updateStopped();
    }

    /**
     * Update all couplings on all components. Currently use a buffering method.
     */
    public void globalUpdate() {
        synchronized (updatorLock) {
            updator.runOnce();
        }
    }

    /**
     * Iterates all couplings on all components until halted by user.
     */
    public void globalRun() {
        synchronized (updatorLock) {
            updator.run();
        }
    }

    /**
     * Stops iteration of all couplings on all components.
     */
    public void globalStop() {
        synchronized (updatorLock) {
            updator.stop();
        }
    }

    /**
     * Remove all components (networks, worlds, etc.) from this workspace.
     */
    public void clearWorkspace() {
        removeAllComponents();
        resetTime();
        this.setWorkspaceChanged(false);
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
        ArrayList<WorkspaceComponent> toRemove = new ArrayList<WorkspaceComponent>();
        synchronized (componentList) {
            for (WorkspaceComponent component : componentList) {
                toRemove.add(component);
            }
            for (WorkspaceComponent component : toRemove) {
                removeWorkspaceComponent(component);
            }
        }
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
            synchronized (componentList) {
                for (WorkspaceComponent component : componentList) {
                    //System.out.println(component.getName() + ":" + component.hasChangedSinceLastSave());
                    if (component.hasChangedSinceLastSave()) {
                        hasChanged = true;
                    }
                }
            }
            return hasChanged;
        }
    }

    /**
     * Sets whether the workspace has been changed.
     *
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
        //WorkspacePreferences.setDefaultFile(currentFile.getAbsolutePath());
    }

    /**
     * @return the componentList
     */
    public List<? extends WorkspaceComponent> getComponentList() {
        return Collections.unmodifiableList(componentList);
    }

    /**
     * Get a component using its name id.  Used in terminal mode.
     *
     * @param id name of component
     * @return Workspace Component
     */
    public WorkspaceComponent getComponent(final String id) {
        synchronized (componentList) {
            for (WorkspaceComponent component : componentList) {
                if (component.getName().equalsIgnoreCase(id)) {
                    return component;
                }
            }
        }
        return null;
    }

    /** The lock used to lock calls on syncAllComponents. */
    private final Object componentLock = new Object();

    public void setTaskSynchronizationManager(final TaskSynchronizationManager manager) {
        updator.setTaskSynchronizationManager(manager);
    }

    /**
     * Synchronizes on all components and executes task, returning the
     * result of that callable.
     *
     * @param <E> The return type of task.
     * @param task The task to synchronize.
     * @return The result of task.
     * @throws Exception If an exception occurs.
     */
    public <E> E syncOnAllComponents(final Callable<E> task) throws Exception {
        synchronized (componentLock) {
            Iterator<Object> locks = new Iterator<Object>() {
                Iterator<? extends WorkspaceComponent> components
                    = getComponentList().iterator();
                Iterator<? extends Object> current = null;

                public boolean hasNext() {
                    if (current == null || !current.hasNext()) {
                        return components.hasNext();
                    } else {
                        return true;
                    }
                }

                public Object next() {
                    if (current == null || !current.hasNext()) {
                        if (components.hasNext()) {
                            current = components.next().getLocks().iterator();
                        } else {
                            throw new IllegalStateException("no more elements");
                        }
                    }
                    
                    return current.next();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
            
            return syncRest(locks, task);
        }
    }
    
    /**
     * Recursively synchronizes on the next component in the iterator and executes
     * task if there are no more components.
     * 
     * @param <E> The return type of task.
     * @param iterator The iterator of the remaining components to synchronize on.
     * @param task The task to synchronize.
     * @return The result of task.
     * @throws Exception If an exception occurs.
     */
    public static <E> E syncRest(final Iterator<? extends Object> iterator, final Callable<E> task)
            throws Exception {
        if (iterator.hasNext()) {
            synchronized (iterator.next()) {
                return syncRest(iterator, task);
            }
        } else {
            return task.call();
        }
    }

    /**
     * Returns the coupling manager for this workspace.
     *
     * @return The coupling manager for this workspace.
     */
    public CouplingManager getCouplingManager() {
        return manager;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder builder =
               new StringBuilder("Number of components: " + componentList.size() + "\n");
        int i = 0;
        synchronized (componentList) {
            for (WorkspaceComponent component : componentList) {
                builder.append("Component " + ++i + ":" + component.getName() + "\n");
            }
        }
        return builder.toString();
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
     * By default, the workspace is updated as followed: 1) Update couplings 2)
     * Call "update" on all workspacecomponents
     * 
     * Sometimes this way of updating is not sufficient, and the user will want
     * updates (in the GUI, presses of the iterate and play buttons) to update
     * components and couplings in a different way.
     *
     * For an example, see the script in
     * {SimbrainDir}/scripts/scriptmenu/addBackpropSim.bsh
     *
     * @param controller The update controller to use.
     * @param threads The number of threads for the component updates.
     */
    public void setCustomUpdateController(final UpdateController controller, final int threads) {
        synchronized (updatorLock) {
            if (updator.isRunning()) {
                throw new RuntimeException(
                        "Cannot change updator while running.");
            }
            WorkspaceUpdator updator = new WorkspaceUpdator(this, manager,
                    controller, threads);
            swapUpdators(updator);
        }
    }

    /**
     * Sets a custom controller with the default number of threads.
     *
     * @param controller The number of threads to use.
     */
    public void setCustomUpdateController(final UpdateController controller) {
        synchronized (updatorLock) {
            if (updator.isRunning()) {
                throw new RuntimeException(
                        "Cannot change updator while running.");
            }
            WorkspaceUpdator updator = new WorkspaceUpdator(this, manager, controller);
            swapUpdators(updator);
        }
    }

    /**
     * Change out updator.
     *
     * @param newUpdator the new updator
     */
    private void swapUpdators(final WorkspaceUpdator newUpdator) {

        // Move old listeners over to new updator
        for(WorkspaceUpdatorListener listener : updator.getUpdatorListeners()) {
            newUpdator.addUpdatorListener(listener);
        }
        updator = newUpdator;
        for(WorkspaceUpdatorListener listener : updator.getUpdatorListeners()) {
            listener.changedUpdateController();
        }
    }

    /**
     * Sets a custom controller with the default number of threads.
     *
     * @param controller The number of threads to use.
     */
    public void resetUpdateController() {
        setCustomUpdateController(WorkspaceUpdator.DEFAULT_CONTROLLER);
    }

    /**
     * Returns global time.
     *
     * @return the time
     */
    public Number getTime() {
        return updator.getTime();
    }

    /**
     * Reset time.
     */
    public void resetTime() {
        updator.resetTime();
    }

    /**
     * Returns a reference to the workspace updator.
     *
     * @return reference to workspace updator.
     */
    public WorkspaceUpdator getWorkspaceUpdator() {
        return updator;
    }

    /**
     * @return the updateDelay
     */
    public int getUpdateDelay() {
        return updateDelay;
    }

    /**
     * @param updateDelay the updateDelay to set
     */
    public void setUpdateDelay(int updateDelay) {
        this.updateDelay = updateDelay;
    }

}